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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.component.definition.ActionContext.Suspend;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.scheduler.TriggerScheduler;
import com.bytechef.platform.workflow.execution.JobResumeId;
import com.bytechef.platform.workflow.execution.service.TaskStateService;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

/**
 * @author Ivica Cardic
 */
class JobSuspenderTest {

    private static final long JOB_ID = 7L;

    private final JobService jobService = mock(JobService.class);
    private final TaskStateService taskStateService = mock(TaskStateService.class);
    private final TriggerScheduler triggerScheduler = mock(TriggerScheduler.class);

    private JobSuspender jobSuspender;
    private Job job;

    @BeforeEach
    void beforeEach() {
        jobSuspender = new JobSuspender(
            mock(ApplicationEventPublisher.class), jobService, taskStateService, triggerScheduler);

        job = new Job();

        job.setId(JOB_ID);

        when(jobService.getJob(JOB_ID)).thenAnswer(invocation -> job);
        when(jobService.update(any(Job.class))).thenAnswer(invocation -> job = invocation.getArgument(0));
    }

    @Test
    void testQueuingASuspendRestoresTheTimeoutOfTheSuspendTheJobWaitsOn() {
        String activeJobResumeId = JobResumeId.of(JOB_ID)
            .toString();
        Instant activeExpiresAt = Instant.parse("2030-01-01T00:00:00Z");

        job.setMetadata(
            new HashMap<>(
                Map.of(
                    MetadataConstants.JOB_RESUME_ID, activeJobResumeId,
                    MetadataConstants.TASK_EXECUTION_RESUME_ID, 1L)));

        when(taskStateService.fetchValue(JobResumeId.parse(activeJobResumeId)))
            .thenReturn(Optional.of(new Suspend(Map.of(), activeExpiresAt)));

        jobSuspender.suspend(
            JOB_ID, 2L, JobResumeId.of(JOB_ID)
                .toString(),
            new Suspend(Map.of(), Instant.parse("2031-01-01T00:00:00Z")));

        verify(triggerScheduler).scheduleOneTimeTask(
            activeExpiresAt, Map.of(JobResumeId.TIMEOUT_TASK_PARAMETER, activeJobResumeId), JOB_ID);
    }

    @Test
    void testPromotingAQueuedSuspendSchedulesItsTimeout() {
        String queuedJobResumeId = JobResumeId.of(JOB_ID)
            .toString();
        Instant queuedExpiresAt = Instant.parse("2031-01-01T00:00:00Z");

        job.setMetadata(
            new HashMap<>(
                Map.of(
                    MetadataConstants.QUEUED_SUSPENDS,
                    List.of(
                        Map.of(
                            MetadataConstants.JOB_RESUME_ID, queuedJobResumeId,
                            MetadataConstants.TASK_EXECUTION_RESUME_ID, 2L)))));

        when(taskStateService.fetchValue(JobResumeId.parse(queuedJobResumeId)))
            .thenReturn(Optional.of(new Suspend(Map.of(), queuedExpiresAt)));

        jobSuspender.suspendOnNextQueuedSuspend(JOB_ID);

        verify(triggerScheduler).scheduleOneTimeTask(
            queuedExpiresAt, Map.of(JobResumeId.TIMEOUT_TASK_PARAMETER, queuedJobResumeId), JOB_ID);
    }

    @Test
    void testQueuingASuspendBehindOneWithoutATimeoutCancelsTheQueuedTimeout() {
        String activeJobResumeId = JobResumeId.of(JOB_ID)
            .toString();

        job.setMetadata(
            new HashMap<>(
                Map.of(
                    MetadataConstants.JOB_RESUME_ID, activeJobResumeId,
                    MetadataConstants.TASK_EXECUTION_RESUME_ID, 1L)));

        when(taskStateService.fetchValue(JobResumeId.parse(activeJobResumeId)))
            .thenReturn(Optional.of(new Suspend(Map.of(), null)));

        jobSuspender.suspend(
            JOB_ID, 2L, JobResumeId.of(JOB_ID)
                .toString(),
            new Suspend(Map.of(), Instant.parse("2031-01-01T00:00:00Z")));

        verify(triggerScheduler).cancelOneTimeTask(JOB_ID);
        verify(triggerScheduler, never()).scheduleOneTimeTask(any(), any(), anyLong());
    }
}
