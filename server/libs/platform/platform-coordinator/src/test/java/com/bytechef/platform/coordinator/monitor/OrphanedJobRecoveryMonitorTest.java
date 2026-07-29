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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.coordinator.event.JobStatusApplicationEvent;
import com.bytechef.atlas.coordinator.event.ResumeJobEvent;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.tenant.service.TenantService;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

/**
 * @author Ivica Cardic
 */
public class OrphanedJobRecoveryMonitorTest {

    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final JobService jobService = mock(JobService.class);
    private final TaskExecutionService taskExecutionService = mock(TaskExecutionService.class);
    private final TenantService tenantService = mock(TenantService.class);

    private final Instant staleInstant = Instant.now()
        .minus(Duration.ofMinutes(30));

    private Job job;
    private TaskExecution taskExecution;

    @BeforeEach
    public void beforeEach() {
        job = mock(Job.class);

        when(job.getId()).thenReturn(1L);
        when(job.getStatus()).thenReturn(Job.Status.STARTED);
        when(job.getLastModifiedDate()).thenReturn(staleInstant);
        when(job.getMetadata()).thenReturn(Map.of());
        // Mockito returns 0L (not null) for unstubbed wrapper-typed methods, which would make every
        // mock job look like a subflow and silently disable the auto-resume branch under test.
        when(job.getParentTaskExecutionId()).thenReturn(null);

        taskExecution = mock(TaskExecution.class);

        when(taskExecution.getJobId()).thenReturn(1L);
        when(taskExecution.getStatus()).thenReturn(TaskExecution.Status.STARTED);
        when(taskExecution.getLastModifiedDate()).thenReturn(staleInstant);

        when(taskExecutionService.getStaleTaskExecutions(any(), any())).thenReturn(List.of(taskExecution));
        when(taskExecutionService.getJobTaskExecutions(1L)).thenReturn(List.of(taskExecution));
        when(jobService.getStaleJobs(any(), any())).thenReturn(List.of());
        when(jobService.getJob(1L)).thenReturn(job);
        when(tenantService.getTenantIds()).thenReturn(List.of("public"));
    }

    @Test
    public void testRecoversOrphanedJobWithoutResumeByDefault() {
        OrphanedJobRecoveryMonitor monitor = new OrphanedJobRecoveryMonitor(
            false, eventPublisher, jobService, 3, Duration.ofMinutes(5), taskExecutionService, tenantService);

        monitor.recoverOrphanedJobs();

        verify(taskExecution).setStatus(TaskExecution.Status.FAILED);
        verify(taskExecutionService).update(taskExecution);
        verify(job).setStatus(Job.Status.FAILED);
        verify(jobService).update(job);
        verify(eventPublisher).publishEvent(any(JobStatusApplicationEvent.class));
        verify(eventPublisher, never()).publishEvent(any(ResumeJobEvent.class));
    }

    @Test
    public void testAutoResumePublishesResumeEventAndCountsAttempt() {
        OrphanedJobRecoveryMonitor monitor = new OrphanedJobRecoveryMonitor(
            true, eventPublisher, jobService, 3, Duration.ofMinutes(5), taskExecutionService, tenantService);

        monitor.recoverOrphanedJobs();

        verify(job).setMetadata(Map.of(OrphanedJobRecoveryMonitor.AUTO_RECOVERY_ATTEMPTS, 1));
        verify(eventPublisher).publishEvent(any(ResumeJobEvent.class));
    }

    @Test
    public void testAutoResumeStopsAtAttemptCap() {
        when(job.getMetadata(OrphanedJobRecoveryMonitor.AUTO_RECOVERY_ATTEMPTS)).thenReturn(3);

        OrphanedJobRecoveryMonitor monitor = new OrphanedJobRecoveryMonitor(
            true, eventPublisher, jobService, 3, Duration.ofMinutes(5), taskExecutionService, tenantService);

        monitor.recoverOrphanedJobs();

        verify(job).setStatus(Job.Status.FAILED);
        verify(eventPublisher, never()).publishEvent(any(ResumeJobEvent.class));
    }

    @Test
    public void testSkipsJobWithFreshTaskExecution() {
        TaskExecution freshTaskExecution = mock(TaskExecution.class);

        when(freshTaskExecution.getStatus()).thenReturn(TaskExecution.Status.STARTED);
        when(freshTaskExecution.getLastModifiedDate()).thenReturn(Instant.now());

        when(taskExecutionService.getJobTaskExecutions(1L)).thenReturn(List.of(taskExecution, freshTaskExecution));

        OrphanedJobRecoveryMonitor monitor = new OrphanedJobRecoveryMonitor(
            false, eventPublisher, jobService, 3, Duration.ofMinutes(5), taskExecutionService, tenantService);

        monitor.recoverOrphanedJobs();

        verify(taskExecutionService, never()).update(any());
        verify(jobService, never()).update(any());
    }

    @Test
    public void testSkipsSubflowAutoResume() {
        when(job.getParentTaskExecutionId()).thenReturn(99L);

        OrphanedJobRecoveryMonitor monitor = new OrphanedJobRecoveryMonitor(
            true, eventPublisher, jobService, 3, Duration.ofMinutes(5), taskExecutionService, tenantService);

        monitor.recoverOrphanedJobs();

        verify(job).setStatus(Job.Status.FAILED);
        verify(eventPublisher, never()).publishEvent(any(ResumeJobEvent.class));
    }

    @Test
    public void testSkipsJobNoLongerStarted() {
        when(job.getStatus()).thenReturn(Job.Status.COMPLETED);

        OrphanedJobRecoveryMonitor monitor = new OrphanedJobRecoveryMonitor(
            false, eventPublisher, jobService, 3, Duration.ofMinutes(5), taskExecutionService, tenantService);

        monitor.recoverOrphanedJobs();

        verify(jobService, never()).update(any());
    }
}
