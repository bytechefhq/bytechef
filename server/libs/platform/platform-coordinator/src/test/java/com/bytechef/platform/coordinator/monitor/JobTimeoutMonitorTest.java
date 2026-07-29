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
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.platform.plan.domain.PlanLimits;
import com.bytechef.platform.plan.domain.PlanTier;
import com.bytechef.platform.plan.provider.PlanLimitsProvider;
import com.bytechef.platform.ratelimit.PlanLimitRejectionCounter;
import com.bytechef.tenant.service.TenantService;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Pins the per-run timeout semantics: a STARTED job past the effective limit is failed together with its non-terminal
 * tasks (job-status fan-out fires, no auto-resume), a job within the limit is untouched, no configured limit means
 * no-op, and a plan-provided asyncRunTimeout takes precedence over the operator default.
 *
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

        when(taskExecution.getStatus()).thenReturn(TaskExecution.Status.STARTED);

        when(tenantService.getTenantIds()).thenReturn(List.of("public"));
    }

    @Test
    public void testLongRunningJobIsTimedOut() {
        when(jobService.getLongRunningJobs(any(), any())).thenReturn(List.of(job));
        when(jobService.getJob(1L)).thenReturn(job);
        when(taskExecutionService.getJobTaskExecutions(1L)).thenReturn(List.of(taskExecution));

        JobTimeoutMonitor jobTimeoutMonitor = getJobTimeoutMonitor(Duration.ofHours(1), null);

        jobTimeoutMonitor.timeOutLongRunningJobs();

        verify(taskExecution).setStatus(TaskExecution.Status.FAILED);
        verify(taskExecutionService).update(taskExecution);
        verify(job).setStatus(Job.Status.FAILED);
        verify(jobService).update(job);
        verify(eventPublisher).publishEvent(any(JobStatusApplicationEvent.class));
    }

    @Test
    public void testJobWithinLimitIsUntouched() {
        when(job.getStartDate()).thenReturn(
            Instant.now()
                .minus(Duration.ofMinutes(5)));

        when(jobService.getLongRunningJobs(any(), any())).thenReturn(List.of(job));
        when(jobService.getJob(1L)).thenReturn(job);

        JobTimeoutMonitor jobTimeoutMonitor = getJobTimeoutMonitor(Duration.ofHours(1), null);

        jobTimeoutMonitor.timeOutLongRunningJobs();

        verify(jobService, never()).update(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    public void testNoConfiguredTimeoutIsANoOp() {
        JobTimeoutMonitor jobTimeoutMonitor = getJobTimeoutMonitor(null, null);

        jobTimeoutMonitor.timeOutLongRunningJobs();

        verify(jobService, never()).getLongRunningJobs(any(), any());
    }

    @Test
    public void testPlanTimeoutTakesPrecedenceOverDefault() {
        // Plan says 4h, operator default says 1h: a 3h-old job must NOT be timed out.
        PlanLimits planLimits = new PlanLimits(
            PlanTier.PRO, null, null, null, null, PlanLimits.DEFAULT_BURST_MULTIPLIER, null, null,
            Duration.ofHours(4), null, null, null, null);

        when(jobService.getLongRunningJobs(any(), any())).thenReturn(List.of());

        JobTimeoutMonitor jobTimeoutMonitor = getJobTimeoutMonitor(Duration.ofHours(1), tenantId -> planLimits);

        jobTimeoutMonitor.timeOutLongRunningJobs();

        verify(jobService).getLongRunningJobs(
            any(), org.mockito.ArgumentMatchers.argThat(cutoff -> cutoff.isBefore(
                Instant.now()
                    .minus(Duration.ofHours(3)))));
    }

    @Test
    public void testNonStartedJobIsSkipped() {
        when(job.getStatus()).thenReturn(Job.Status.COMPLETED);

        when(jobService.getLongRunningJobs(any(), any())).thenReturn(List.of(job));
        when(jobService.getJob(1L)).thenReturn(job);

        JobTimeoutMonitor jobTimeoutMonitor = getJobTimeoutMonitor(Duration.ofHours(1), null);

        jobTimeoutMonitor.timeOutLongRunningJobs();

        verify(jobService, never()).update(any());
    }

    private JobTimeoutMonitor getJobTimeoutMonitor(
        Duration defaultTimeout, PlanLimitsProvider planLimitsProvider) {

        @SuppressWarnings("unchecked")
        ObjectProvider<PlanLimitsProvider> planLimitsProviderObjectProvider =
            (ObjectProvider<PlanLimitsProvider>) mock(ObjectProvider.class);

        when(planLimitsProviderObjectProvider.getIfAvailable()).thenReturn(planLimitsProvider);

        @SuppressWarnings("unchecked")
        ObjectProvider<PlanLimitRejectionCounter> planLimitRejectionCounterObjectProvider =
            (ObjectProvider<PlanLimitRejectionCounter>) mock(ObjectProvider.class);

        return new JobTimeoutMonitor(
            defaultTimeout, eventPublisher, jobService, planLimitRejectionCounterObjectProvider,
            planLimitsProviderObjectProvider, taskExecutionService, tenantService);
    }
}
