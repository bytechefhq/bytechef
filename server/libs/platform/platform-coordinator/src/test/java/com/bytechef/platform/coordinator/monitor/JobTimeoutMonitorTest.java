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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.coordinator.event.JobStatusApplicationEvent;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.worker.event.CancelControlTaskEvent;
import com.bytechef.tenant.service.TenantService;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

/**
 * @author Ivica Cardic
 */
public class JobTimeoutMonitorTest {

    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final JobService jobService = mock(JobService.class);
    private final TaskExecutionService taskExecutionService = mock(TaskExecutionService.class);
    private final TenantService tenantService = mock(TenantService.class);

    private Job job;
    private TaskExecution taskExecution;

    @BeforeEach
    public void beforeEach() {
        job = mock(Job.class);

        when(job.getId()).thenReturn(1L);
        when(job.getStatus()).thenReturn(Job.Status.STARTED);
        when(job.getStartDate()).thenReturn(
            Instant.now()
                .minus(Duration.ofHours(3)));

        taskExecution = mock(TaskExecution.class);

        when(taskExecution.getId()).thenReturn(10L);
        when(taskExecution.getStatus()).thenReturn(TaskExecution.Status.STARTED);
        when(taskExecutionService.failIfUnfinished(anyLong(), anyString())).thenReturn(true);

        when(tenantService.getTenantIds()).thenReturn(List.of("public"));
    }

    @Test
    public void testLongRunningJobIsTimedOut() {
        when(jobService.getLongRunningJobs(any(), any())).thenReturn(List.of(job));
        when(jobService.getJob(1L)).thenReturn(job);
        when(taskExecutionService.getJobTaskExecutions(1L)).thenReturn(List.of(taskExecution));

        JobTimeoutMonitor jobTimeoutMonitor = getJobTimeoutMonitor(Duration.ofHours(1));

        jobTimeoutMonitor.timeOutLongRunningJobs();

        verify(taskExecutionService).failIfUnfinished(eq(10L), anyString());
        verify(job).setStatus(Job.Status.FAILED);
        verify(jobService).update(job);
        verify(eventPublisher).publishEvent(any(CancelControlTaskEvent.class));
        verify(eventPublisher).publishEvent(any(JobStatusApplicationEvent.class));
    }

    @Test
    public void testTimedOutJobLeavesATaskThatCompletedAfterItWasRead() {
        when(jobService.getLongRunningJobs(any(), any())).thenReturn(List.of(job));
        when(jobService.getJob(1L)).thenReturn(job);
        when(taskExecutionService.getJobTaskExecutions(1L)).thenReturn(List.of(taskExecution));
        when(taskExecutionService.failIfUnfinished(anyLong(), anyString())).thenReturn(false);

        JobTimeoutMonitor jobTimeoutMonitor = getJobTimeoutMonitor(Duration.ofHours(1));

        jobTimeoutMonitor.timeOutLongRunningJobs();

        verify(taskExecutionService, never()).update(any());
        verify(taskExecution, never()).setStatus(any());
        verify(eventPublisher, never()).publishEvent(any(CancelControlTaskEvent.class));
    }

    @Test
    public void testJobWithinLimitIsUntouched() {
        when(job.getStartDate()).thenReturn(
            Instant.now()
                .minus(Duration.ofMinutes(5)));

        when(jobService.getLongRunningJobs(any(), any())).thenReturn(List.of(job));
        when(jobService.getJob(1L)).thenReturn(job);

        JobTimeoutMonitor jobTimeoutMonitor = getJobTimeoutMonitor(Duration.ofHours(1));

        jobTimeoutMonitor.timeOutLongRunningJobs();

        verify(jobService, never()).update(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    public void testNoConfiguredTimeoutIsANoOp() {
        JobTimeoutMonitor jobTimeoutMonitor = getJobTimeoutMonitor(null);

        jobTimeoutMonitor.timeOutLongRunningJobs();

        verify(jobService, never()).getLongRunningJobs(any(), any());
    }

    @Test
    public void testNonStartedJobIsSkipped() {
        when(job.getStatus()).thenReturn(Job.Status.COMPLETED);

        when(jobService.getLongRunningJobs(any(), any())).thenReturn(List.of(job));
        when(jobService.getJob(1L)).thenReturn(job);

        JobTimeoutMonitor jobTimeoutMonitor = getJobTimeoutMonitor(Duration.ofHours(1));

        jobTimeoutMonitor.timeOutLongRunningJobs();

        verify(jobService, never()).update(any());
    }

    @Test
    public void testJobWaitingOnANonTerminalChildJobIsNotTimedOut() {
        Job childJob = mock(Job.class);

        when(childJob.getStatus()).thenReturn(Job.Status.STOPPED);

        when(jobService.getLongRunningJobs(any(), any())).thenReturn(List.of(job));
        when(jobService.getJob(1L)).thenReturn(job);
        when(jobService.getChildJobIds(1L)).thenReturn(List.of(2L));
        when(jobService.getJobs(List.of(2L))).thenReturn(List.of(childJob));

        JobTimeoutMonitor jobTimeoutMonitor = getJobTimeoutMonitor(Duration.ofHours(1));

        jobTimeoutMonitor.timeOutLongRunningJobs();

        verify(taskExecutionService, never()).failIfUnfinished(anyLong(), anyString());
        verify(jobService, never()).update(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    public void testJobWhoseChildJobsAreAllTerminalIsTimedOut() {
        Job childJob = mock(Job.class);

        when(childJob.getStatus()).thenReturn(Job.Status.COMPLETED);

        when(jobService.getLongRunningJobs(any(), any())).thenReturn(List.of(job));
        when(jobService.getJob(1L)).thenReturn(job);
        when(jobService.getChildJobIds(1L)).thenReturn(List.of(2L));
        when(jobService.getJobs(List.of(2L))).thenReturn(List.of(childJob));
        when(taskExecutionService.getJobTaskExecutions(1L)).thenReturn(List.of(taskExecution));

        JobTimeoutMonitor jobTimeoutMonitor = getJobTimeoutMonitor(Duration.ofHours(1));

        jobTimeoutMonitor.timeOutLongRunningJobs();

        verify(job).setStatus(Job.Status.FAILED);
        verify(jobService).update(job);
    }

    private JobTimeoutMonitor getJobTimeoutMonitor(Duration defaultTimeout) {

        return new JobTimeoutMonitor(
            defaultTimeout, eventPublisher, jobService, taskExecutionService, tenantService);
    }
}
