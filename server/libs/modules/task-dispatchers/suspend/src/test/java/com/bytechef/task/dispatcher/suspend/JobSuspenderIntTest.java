/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.task.dispatcher.suspend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.bytechef.atlas.coordinator.event.JobStatusApplicationEvent;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.repository.JobRepository;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.component.definition.ActionContext.Suspend;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.scheduler.TriggerScheduler;
import com.bytechef.platform.workflow.execution.JobResumeId;
import com.bytechef.platform.workflow.execution.facade.InMemoryTaskStateFacade;
import com.bytechef.task.dispatcher.suspend.config.SuspendIntTestConfiguration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = SuspendIntTestConfiguration.class)
class JobSuspenderIntTest {

    private static final Instant FIRST_EXPIRES_AT = Instant.parse("2030-01-01T00:00:00Z");
    private static final long FIRST_TASK_EXECUTION_ID = 1L;
    private static final Instant SECOND_EXPIRES_AT = Instant.parse("2031-01-01T00:00:00Z");
    private static final long SECOND_TASK_EXECUTION_ID = 2L;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private JobService jobService;

    private final List<Object> publishedEvents = new CopyOnWriteArrayList<>();
    private final InMemoryTaskStateFacade taskStateFacade = new InMemoryTaskStateFacade();
    private final TriggerScheduler triggerScheduler = mock(TriggerScheduler.class);

    private JobSuspender jobSuspender;
    private long jobId;

    @BeforeEach
    void beforeEach() {
        jobSuspender = new JobSuspender(publishedEvents::add, jobService, taskStateFacade, triggerScheduler);

        Job job = new Job();

        job.setStatus(Job.Status.STARTED);
        job.setWorkflowId("demo:1234");

        job = jobRepository.save(job);

        jobId = Objects.requireNonNull(job.getId());
    }

    @Test
    void testSuspendStopsTheJobOnTheTaskThatAskedForIt() {
        String jobResumeId = suspend(
            FIRST_TASK_EXECUTION_ID, Map.of(MetadataConstants.APPROVAL_RESUME, true), FIRST_EXPIRES_AT);

        Job job = getJob();

        assertThat(job.getStatus()).isEqualTo(Job.Status.STOPPED);
        assertThat(job.getMetadata(MetadataConstants.JOB_RESUME_ID)).isEqualTo(jobResumeId);
        assertThat(MapUtils.getLong(job.getMetadata(), MetadataConstants.TASK_EXECUTION_RESUME_ID))
            .isEqualTo(FIRST_TASK_EXECUTION_ID);
        assertThat(job.getMetadata(MetadataConstants.APPROVAL_RESUME)).isEqualTo(true);
        assertThat(publishedEvents).singleElement()
            .isInstanceOf(JobStatusApplicationEvent.class);
    }

    @Test
    void testQueuingASuspendRestoresTheTimeoutOfTheSuspendTheJobWaitsOn() {
        String firstJobResumeId = suspend(FIRST_TASK_EXECUTION_ID, Map.of(), FIRST_EXPIRES_AT);
        String secondJobResumeId = suspend(SECOND_TASK_EXECUTION_ID, Map.of(), SECOND_EXPIRES_AT);

        Job job = getJob();

        assertThat(job.getMetadata(MetadataConstants.JOB_RESUME_ID)).isEqualTo(firstJobResumeId);
        assertThat(job.getMetadata(MetadataConstants.QUEUED_SUSPENDS))
            .asList()
            .singleElement()
            .asInstanceOf(InstanceOfAssertFactories.MAP)
            .containsEntry(MetadataConstants.JOB_RESUME_ID, secondJobResumeId);

        verify(triggerScheduler).scheduleOneTimeTask(
            FIRST_EXPIRES_AT, Map.of(JobResumeId.TIMEOUT_TASK_PARAMETER, firstJobResumeId), jobId);
    }

    @Test
    void testPromotingAQueuedSuspendStoredInTheJobMakesItTheJobsSuspend() {
        suspend(FIRST_TASK_EXECUTION_ID, Map.of(), FIRST_EXPIRES_AT);

        String secondJobResumeId = suspend(SECOND_TASK_EXECUTION_ID, Map.of(), SECOND_EXPIRES_AT);

        resumeTheSuspendTheJobWaitsOn();

        jobSuspender.suspendOnNextQueuedSuspend(jobId);

        Job job = getJob();

        assertThat(job.getStatus()).isEqualTo(Job.Status.STOPPED);
        assertThat(job.getMetadata(MetadataConstants.JOB_RESUME_ID)).isEqualTo(secondJobResumeId);
        assertThat(MapUtils.getLong(job.getMetadata(), MetadataConstants.TASK_EXECUTION_RESUME_ID))
            .isEqualTo(SECOND_TASK_EXECUTION_ID);
        assertThat(job.getMetadata()).doesNotContainKey(MetadataConstants.QUEUED_SUSPENDS);

        verify(triggerScheduler).scheduleOneTimeTask(
            SECOND_EXPIRES_AT, Map.of(JobResumeId.TIMEOUT_TASK_PARAMETER, secondJobResumeId), jobId);
    }

    @Test
    void testQueuingASuspendBehindOneWithoutATimeoutCancelsTheQueuedTimeout() {
        suspend(FIRST_TASK_EXECUTION_ID, Map.of(), null);
        suspend(SECOND_TASK_EXECUTION_ID, Map.of(), SECOND_EXPIRES_AT);

        verify(triggerScheduler).cancelOneTimeTask(jobId);
        verify(triggerScheduler, never()).scheduleOneTimeTask(any(), any(), anyLong());
    }

    private Job getJob() {
        return jobRepository.findById(jobId)
            .orElseThrow();
    }

    private void resumeTheSuspendTheJobWaitsOn() {
        jobService.resumeToStatusStarted(jobId);

        Job job = getJob();

        Map<String, Object> jobMetadata = new HashMap<>(job.getMetadata());

        jobMetadata.remove(MetadataConstants.JOB_RESUME_ID);

        job.setMetadata(jobMetadata);

        jobService.update(job);
    }

    private String suspend(long taskExecutionId, Map<String, ?> continueParameters, @Nullable Instant expiresAt) {
        JobResumeId jobResumeId = JobResumeId.of(jobId);
        Suspend suspend = new Suspend(continueParameters, expiresAt);

        taskStateFacade.save(jobResumeId, suspend);

        String jobResumeIdString = jobResumeId.toString();

        jobSuspender.suspend(jobId, taskExecutionId, jobResumeIdString, suspend);

        return jobResumeIdString;
    }
}
