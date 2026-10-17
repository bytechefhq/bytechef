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

package com.bytechef.platform.coordinator.monitor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.coordinator.event.JobStatusApplicationEvent;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.repository.JobRepository;
import com.bytechef.atlas.execution.repository.TaskExecutionRepository;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.error.ExecutionError;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.coordinator.config.PlatformCoordinatorIntTestConfiguration;
import com.bytechef.platform.workflow.execution.JobResumeId;
import com.bytechef.platform.workflow.execution.event.JobApprovalClosedEvent;
import com.bytechef.platform.workflow.execution.service.TaskStateService;
import com.bytechef.tenant.service.TenantService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.search.Search;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

/**
 * @author Ivica Cardic
 */
@RecordApplicationEvents
@SpringBootTest(classes = PlatformCoordinatorIntTestConfiguration.class)
class ApprovalExpiryMonitorIntTest {

    private static final String EXPIRED_COUNTER = "bytechef_approval_expired";

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    private ApplicationEvents applicationEvents;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JobRepository jobRepository;

    @MockitoSpyBean
    private JobService jobService;

    @Autowired
    private TaskExecutionRepository taskExecutionRepository;

    @MockitoSpyBean
    private TaskExecutionService taskExecutionService;

    @MockitoBean
    private TaskStateService taskStateService;

    @Autowired
    private TenantService tenantService;

    private ApprovalExpiryMonitor approvalExpiryMonitor;
    private SimpleMeterRegistry meterRegistry;

    @BeforeEach
    void beforeEach() {
        jdbcTemplate.update("DELETE FROM task_execution");
        jdbcTemplate.update("DELETE FROM job");

        meterRegistry = new SimpleMeterRegistry();

        StaticListableBeanFactory staticListableBeanFactory = new StaticListableBeanFactory(
            Map.of("meterRegistry", meterRegistry));

        ObjectProvider<MeterRegistry> meterRegistryObjectProvider = staticListableBeanFactory.getBeanProvider(
            MeterRegistry.class);

        approvalExpiryMonitor = new ApprovalExpiryMonitor(
            applicationEventPublisher, jobService, meterRegistryObjectProvider, taskExecutionService,
            taskStateService, tenantService);
    }

    @Test
    void testExpiredApprovalFailsTaskAndJobAndCounts() {
        long jobId = saveApprovalJob(hoursFromNow(-1));

        approvalExpiryMonitor.sweepExpiredApprovals();

        Job job = getJob(jobId);
        TaskExecution taskExecution = getSuspendedTaskExecution(job);
        ExecutionError executionError = Objects.requireNonNull(job.getError());

        assertThat(job.getStatus()).isEqualTo(Job.Status.FAILED);
        assertThat(executionError.getMessage()).startsWith("The pending approval expired at ");
        assertThat(job.getMetadata("expiredJobResumeId")).isEqualTo(job.getMetadata(MetadataConstants.JOB_RESUME_ID));
        assertThat(job.getMetadata("consumedJobResumeId"))
            .isEqualTo(job.getMetadata(MetadataConstants.JOB_RESUME_ID));
        assertThat(taskExecution.getStatus()).isEqualTo(TaskExecution.Status.FAILED);

        verify(taskStateService).delete(any(JobResumeId.class));

        assertThat(applicationEvents.stream(JobStatusApplicationEvent.class))
            .singleElement()
            .extracting(JobStatusApplicationEvent::getStatus)
            .isEqualTo(Job.Status.FAILED);
        assertThat(applicationEvents.stream(JobApprovalClosedEvent.class))
            .containsExactly(new JobApprovalClosedEvent(jobId));
        assertThat(getExpiredCount()).isEqualTo(1.0);
    }

    @Test
    void testUnexpiredApprovalStaysPendingAndFeedsTheGauge() {
        long jobId = saveApprovalJob(hoursFromNow(1));

        approvalExpiryMonitor.sweepExpiredApprovals();

        Job job = getJob(jobId);

        assertThat(job.getStatus()).isEqualTo(Job.Status.STOPPED);
        assertThat(applicationEvents.stream()).noneMatch(JobStatusApplicationEvent.class::isInstance);

        Search search = meterRegistry.find("bytechef_approval_pending");

        Gauge gauge = search.gauge();

        assertThat(gauge).isNotNull();
        assertThat(gauge.value()).isEqualTo(1.0);
    }

    @Test
    void testExpiredSuspendThatIsNotAnApprovalIsIgnored() {
        long jobId = saveApprovalJob(hoursFromNow(-1));

        removeMetadata(jobId, MetadataConstants.APPROVAL_RESUME);

        approvalExpiryMonitor.sweepExpiredApprovals();

        Job job = getJob(jobId);

        assertThat(job.getStatus()).isEqualTo(Job.Status.STOPPED);

        verify(taskExecutionService, never()).getTaskExecution(anyLong());
    }

    @Test
    void testStoppedJobWithoutResumeIdIsIgnored() {
        Job job = new Job();

        job.setStatus(Job.Status.STOPPED);
        job.setWorkflowId("demo:1234");

        job = jobRepository.save(job);

        approvalExpiryMonitor.sweepExpiredApprovals();

        Job reloadedJob = getJob(Objects.requireNonNull(job.getId()));

        assertThat(reloadedJob.getStatus()).isEqualTo(Job.Status.STOPPED);

        verify(taskExecutionService, never()).getTaskExecution(anyLong());
    }

    @Test
    void testMissingSuspendedTaskExecutionFailsTheJobAsOrphaned() {
        long jobId = saveApprovalJob(hoursFromNow(1));

        Job job = getJob(jobId);

        Map<String, Object> metadata = new HashMap<>(job.getMetadata());

        metadata.put(MetadataConstants.TASK_EXECUTION_RESUME_ID, Long.MAX_VALUE);

        job.setMetadata(metadata);

        jobRepository.save(job);

        approvalExpiryMonitor.sweepExpiredApprovals();

        Job failedJob = getJob(jobId);
        ExecutionError executionError = Objects.requireNonNull(failedJob.getError());

        assertThat(failedJob.getStatus()).isEqualTo(Job.Status.FAILED);
        assertThat(executionError.getMessage())
            .isEqualTo("The suspended task execution of the pending approval no longer exists");
        assertThat(applicationEvents.stream(JobStatusApplicationEvent.class)).hasSize(1);

        verify(taskExecutionService, never()).update(any());
    }

    @Test
    void testTransientTaskExecutionLookupFailureKeepsTheJobPending() {
        long jobId = saveApprovalJob(hoursFromNow(-1));

        doThrow(new TransientDataAccessResourceException("connection reset"))
            .when(taskExecutionService)
            .getTaskExecution(anyLong());

        approvalExpiryMonitor.sweepExpiredApprovals();

        Job job = getJob(jobId);

        assertThat(job.getStatus()).isEqualTo(Job.Status.STOPPED);

        verify(jobService, never()).update(any());
    }

    @Test
    void testResumeWinningTheRaceLeavesTheSuspendedTaskExecutionUntouched() {
        long jobId = saveApprovalJob(hoursFromNow(-1));

        doAnswer(invocation -> {
            Object staleJobs = invocation.callRealMethod();

            Job job = getJob(jobId);

            job.setStatus(Job.Status.STARTED);

            jobRepository.save(job);

            return staleJobs;
        }).when(jobService)
            .getStaleJobs(any(), any());

        approvalExpiryMonitor.sweepExpiredApprovals();

        Job job = getJob(jobId);
        TaskExecution taskExecution = getSuspendedTaskExecution(job);

        assertThat(job.getStatus()).isEqualTo(Job.Status.STARTED);
        assertThat(taskExecution.getStatus()).isNotEqualTo(TaskExecution.Status.FAILED);
        assertThat(applicationEvents.stream()).noneMatch(JobStatusApplicationEvent.class::isInstance);

        verify(taskStateService, never()).delete(any());
    }

    @Test
    void testUnsupportedStaleJobFinderSkipsTheTenant() {
        saveApprovalJob(hoursFromNow(-1));

        doThrow(new UnsupportedOperationException())
            .when(jobService)
            .getStaleJobs(any(), any());

        approvalExpiryMonitor.sweepExpiredApprovals();

        verify(jobService, never()).update(any());
    }

    private double getExpiredCount() {
        Counter counter = meterRegistry.counter(EXPIRED_COUNTER, "source", "sweep");

        return counter.count();
    }

    private Job getJob(long jobId) {
        return OptionalUtils.get(jobRepository.findById(jobId));
    }

    private TaskExecution getSuspendedTaskExecution(Job job) {
        Long taskExecutionId = MapUtils.getLong(job.getMetadata(), MetadataConstants.TASK_EXECUTION_RESUME_ID);

        return OptionalUtils.get(taskExecutionRepository.findById(Objects.requireNonNull(taskExecutionId)));
    }

    private void removeMetadata(long jobId, String key) {
        Job job = getJob(jobId);

        Map<String, Object> metadata = new HashMap<>(job.getMetadata());

        metadata.remove(key);

        job.setMetadata(metadata);

        jobRepository.save(job);
    }

    private long saveApprovalJob(Instant expiresAt) {
        Job job = new Job();

        job.setStatus(Job.Status.STOPPED);
        job.setWorkflowId("demo:1234");

        job = jobRepository.save(job);

        long jobId = Objects.requireNonNull(job.getId());

        TaskExecution taskExecution = TaskExecution.builder()
            .jobId(jobId)
            .taskNumber(1)
            .workflowTask(new WorkflowTask(Map.of("name", "approval1", "type", "approval/v1/approve")))
            .build();

        taskExecution.setStatus(TaskExecution.Status.STARTED);
        taskExecution.putMetadata(MetadataConstants.SUSPEND, Map.of("expiresAt", expiresAt.toEpochMilli()));

        taskExecution = taskExecutionRepository.save(taskExecution);

        JobResumeId jobResumeId = JobResumeId.of(jobId);

        job.setMetadata(
            Map.of(
                MetadataConstants.APPROVAL_RESUME, true, MetadataConstants.JOB_RESUME_ID, jobResumeId.toString(),
                MetadataConstants.TASK_EXECUTION_RESUME_ID, Objects.requireNonNull(taskExecution.getId())));

        jobRepository.save(job);

        return jobId;
    }

    private static Instant hoursFromNow(long hours) {
        Instant now = Instant.now();

        return now.plus(Duration.ofHours(hours));
    }
}
