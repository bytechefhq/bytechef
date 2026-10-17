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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.coordinator.event.JobStatusApplicationEvent;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.platform.workflow.execution.event.JobApprovalClosedEvent;
import com.bytechef.tenant.service.TenantService;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.TransientDataAccessResourceException;

/**
 * @author Ivica Cardic
 */
@ExtendWith({
    MockitoExtension.class, ObjectMapperSetupExtension.class
})
class ApprovalExpiryMonitorTest {

    private static final long JOB_ID = 42L;
    private static final long TASK_EXECUTION_ID = 7L;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private JobService jobService;

    @Mock
    private TaskExecutionService taskExecutionService;

    @Mock
    private com.bytechef.platform.workflow.execution.service.TaskStateService taskStateService;

    @Mock
    private TenantService tenantService;

    private SimpleMeterRegistry meterRegistry;
    private ApprovalExpiryMonitor monitor;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();

        ObjectProvider<MeterRegistry> meterRegistryObjectProvider = mock(ObjectProvider.class);

        when(meterRegistryObjectProvider.getIfAvailable()).thenReturn(meterRegistry);
        when(tenantService.getTenantIds()).thenReturn(List.of("public"));

        monitor = new ApprovalExpiryMonitor(
            eventPublisher, jobService, meterRegistryObjectProvider, taskExecutionService, taskStateService,
            tenantService);
    }

    @Test
    void testExpiredApprovalFailsTaskAndJobAndCounts() {
        Job job = stoppedJobWithResumeId();

        when(jobService.getStaleJobs(any(Job.Status.class), any(Instant.class))).thenReturn(List.of(job));

        TaskExecution taskExecution = suspendedTaskExecution(Instant.now()
            .minusSeconds(3600));

        when(taskExecutionService.getTaskExecution(TASK_EXECUTION_ID)).thenReturn(taskExecution);

        monitor.sweepExpiredApprovals();

        assertThat(taskExecution.getStatus()).isEqualTo(TaskExecution.Status.FAILED);
        assertThat(job.getStatus()).isEqualTo(Job.Status.FAILED);
        assertThat(job.getError()
            .getMessage()).startsWith("The pending approval expired at ");
        assertThat(job.getMetadata("expiredJobResumeId")).isEqualTo(job.getMetadata("jobResumeId"));
        assertThat(job.getMetadata("consumedJobResumeId")).isEqualTo(job.getMetadata("jobResumeId"));

        verify(jobService).update(job);
        verify(taskExecutionService).update(taskExecution);

        verify(taskStateService).delete(any());

        ArgumentCaptor<JobStatusApplicationEvent> eventCaptor =
            ArgumentCaptor.forClass(JobStatusApplicationEvent.class);

        verify(eventPublisher).publishEvent(eventCaptor.capture());

        assertThat(eventCaptor.getValue()
            .getStatus()).isEqualTo(Job.Status.FAILED);

        verify(eventPublisher).publishEvent(new JobApprovalClosedEvent(JOB_ID));

        assertThat(meterRegistry.counter("bytechef_approval_expired", "source", "sweep")
            .count()).isEqualTo(1.0);
    }

    @Test
    void testUnexpiredApprovalStaysPendingAndFeedsTheGauge() {
        Job job = stoppedJobWithResumeId();

        when(jobService.getStaleJobs(any(Job.Status.class), any(Instant.class))).thenReturn(List.of(job));

        TaskExecution taskExecution = suspendedTaskExecution(Instant.now()
            .plusSeconds(3600));

        when(taskExecutionService.getTaskExecution(TASK_EXECUTION_ID)).thenReturn(taskExecution);

        monitor.sweepExpiredApprovals();

        assertThat(job.getStatus()).isEqualTo(Job.Status.STOPPED);

        verify(jobService, never()).update(any());
        verify(eventPublisher, never()).publishEvent(any());

        io.micrometer.core.instrument.Gauge gauge = meterRegistry.find("bytechef_approval_pending")
            .gauge();

        assertThat(gauge).isNotNull();
        assertThat(gauge.value()).isEqualTo(1.0);
    }

    @Test
    void testExpiredSuspendThatIsNotAnApprovalIsIgnored() {
        Job job = stoppedJobWithResumeId();

        Map<String, Object> metadata = new HashMap<>(job.getMetadata());

        metadata.remove("__bytechef_approval_resume__");

        job.setMetadata(metadata);

        when(jobService.getStaleJobs(any(Job.Status.class), any(Instant.class))).thenReturn(List.of(job));

        monitor.sweepExpiredApprovals();

        assertThat(job.getStatus()).isEqualTo(Job.Status.STOPPED);

        verify(taskExecutionService, never()).getTaskExecution(TASK_EXECUTION_ID);
        verify(jobService, never()).update(any());
    }

    @Test
    void testStoppedJobWithoutResumeIdIsIgnored() {
        Job job = new Job(JOB_ID);

        job.setStatus(Job.Status.STOPPED);

        when(jobService.getStaleJobs(any(Job.Status.class), any(Instant.class))).thenReturn(List.of(job));

        monitor.sweepExpiredApprovals();

        verify(taskExecutionService, never()).getTaskExecution(TASK_EXECUTION_ID);
        verify(jobService, never()).update(any());
    }

    @Test
    void testMissingSuspendedTaskExecutionFailsTheJobAsOrphaned() {
        Job job = stoppedJobWithResumeId();

        when(jobService.getStaleJobs(any(Job.Status.class), any(Instant.class))).thenReturn(List.of(job));
        when(taskExecutionService.getTaskExecution(TASK_EXECUTION_ID))
            .thenThrow(new IllegalStateException("TaskExecution with id '7' not found"));

        monitor.sweepExpiredApprovals();

        assertThat(job.getStatus()).isEqualTo(Job.Status.FAILED);
        assertThat(job.getError()
            .getMessage()).isEqualTo("The suspended task execution of the pending approval no longer exists");

        verify(jobService).update(job);
        verify(taskExecutionService, never()).update(any());
        verify(eventPublisher).publishEvent(any(JobStatusApplicationEvent.class));
    }

    @Test
    void testTransientTaskExecutionLookupFailureKeepsTheJobPending() {
        Job job = stoppedJobWithResumeId();

        when(jobService.getStaleJobs(any(Job.Status.class), any(Instant.class))).thenReturn(List.of(job));
        when(taskExecutionService.getTaskExecution(TASK_EXECUTION_ID))
            .thenThrow(new TransientDataAccessResourceException("connection reset"));

        monitor.sweepExpiredApprovals();

        assertThat(job.getStatus()).isEqualTo(Job.Status.STOPPED);

        verify(jobService, never()).update(any());
    }

    @Test
    void testResumeWinningTheRaceLeavesTheSuspendedTaskExecutionUntouched() {
        Job job = stoppedJobWithResumeId();

        when(jobService.getStaleJobs(any(Job.Status.class), any(Instant.class))).thenReturn(List.of(job));

        TaskExecution taskExecution = suspendedTaskExecution(Instant.now()
            .minusSeconds(3600));

        when(taskExecutionService.getTaskExecution(TASK_EXECUTION_ID)).thenReturn(taskExecution);
        when(jobService.update(job)).thenThrow(new OptimisticLockingFailureException("resumed concurrently"));

        monitor.sweepExpiredApprovals();

        assertThat(taskExecution.getStatus()).isNotEqualTo(TaskExecution.Status.FAILED);

        verify(taskExecutionService, never()).update(any());
        verify(taskStateService, never()).delete(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void testUnsupportedStaleJobFinderSkipsTheTenant() {
        when(jobService.getStaleJobs(any(Job.Status.class), any(Instant.class)))
            .thenThrow(new UnsupportedOperationException());

        monitor.sweepExpiredApprovals();

        verify(jobService, never()).update(any());
    }

    private static Job stoppedJobWithResumeId() {
        Job job = new Job(JOB_ID);

        job.setStatus(Job.Status.STOPPED);

        Map<String, Object> metadata = new HashMap<>();

        metadata.put(
            "jobResumeId",
            com.bytechef.commons.util.EncodingUtils.base64EncodeToString(
                "public:42:123e4567-e89b-12d3-a456-426614174000".getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        metadata.put("taskExecutionResumeId", TASK_EXECUTION_ID);
        metadata.put("__bytechef_approval_resume__", true);

        job.setMetadata(metadata);

        return job;
    }

    private static TaskExecution suspendedTaskExecution(Instant expiresAt) {
        TaskExecution taskExecution = new TaskExecution();

        taskExecution.setId(TASK_EXECUTION_ID);
        taskExecution.putMetadata("suspend", Map.of("expiresAt", expiresAt.toEpochMilli()));

        return taskExecution;
    }
}
