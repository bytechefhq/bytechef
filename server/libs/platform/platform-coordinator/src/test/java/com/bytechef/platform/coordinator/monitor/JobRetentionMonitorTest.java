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
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.platform.data.storage.DataStorage;
import com.bytechef.platform.data.storage.domain.DataStorageScope;
import com.bytechef.platform.plan.domain.PlanLimits;
import com.bytechef.platform.plan.domain.PlanTier;
import com.bytechef.platform.plan.provider.PlanLimitsProvider;
import com.bytechef.tenant.service.TenantService;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

/**
 * Pins the retention-purge semantics: a terminal job past the effective retention window is deleted through the
 * cascading facade, subflow child jobs are left for their parent's cascade, no configured retention means no-op, and a
 * plan-provided logRetentionDays takes precedence over the operator default.
 *
 * @author Ivica Cardic
 */
public class JobRetentionMonitorTest {

    private final DataStorage dataStorage = mock(DataStorage.class);
    private final JobFacade jobFacade = mock(JobFacade.class);
    private final JobService jobService = mock(JobService.class);
    private final TenantService tenantService = mock(TenantService.class);

    private Job job;

    @BeforeEach
    public void beforeEach() {
        job = mock(Job.class);

        when(job.getId()).thenReturn(1L);
        // Mockito returns 0 (not null) for unstubbed wrapper-returning methods; a 0 parent task execution id would
        // make every mock job look like a subflow child, so the null must be stubbed explicitly.
        when(job.getParentTaskExecutionId()).thenReturn(null);

        when(tenantService.getTenantIds()).thenReturn(List.of("public"));
    }

    @Test
    public void testExpiredJobIsPurged() {
        when(jobService.getEndedJobs(any())).thenReturn(List.of(job));

        JobRetentionMonitor jobRetentionMonitor = getJobRetentionMonitor(30, null);

        jobRetentionMonitor.purgeExpiredJobs();

        verify(jobFacade).deleteJob(1L);
        verify(dataStorage).deleteScopeData(DataStorageScope.CURRENT_EXECUTION, "1");
    }

    @Test
    public void testSubflowChildJobIsSkipped() {
        when(job.getParentTaskExecutionId()).thenReturn(5L);

        when(jobService.getEndedJobs(any())).thenReturn(List.of(job));

        JobRetentionMonitor jobRetentionMonitor = getJobRetentionMonitor(30, null);

        jobRetentionMonitor.purgeExpiredJobs();

        verify(jobFacade, never()).deleteJob(1L);
    }

    @Test
    public void testNoConfiguredRetentionIsANoOp() {
        JobRetentionMonitor jobRetentionMonitor = getJobRetentionMonitor(null, null);

        jobRetentionMonitor.purgeExpiredJobs();

        verify(jobService, never()).getEndedJobs(any());
    }

    @Test
    public void testPlanRetentionTakesPrecedenceOverDefault() {
        // Plan says 7 days, operator default says 30: the cutoff must sit ~7 days back, not ~30.
        PlanLimits planLimits = new PlanLimits(
            PlanTier.PRO, null, null, null, null, PlanLimits.DEFAULT_BURST_MULTIPLIER, null, null, null, null, null,
            7, null);

        when(jobService.getEndedJobs(any())).thenReturn(List.of());

        JobRetentionMonitor jobRetentionMonitor = getJobRetentionMonitor(30, tenantId -> planLimits);

        jobRetentionMonitor.purgeExpiredJobs();

        verify(jobService).getEndedJobs(
            argThat(cutoff -> cutoff.isAfter(
                Instant.now()
                    .minus(Duration.ofDays(8)))));
    }

    private JobRetentionMonitor getJobRetentionMonitor(
        Integer defaultRetentionDays, PlanLimitsProvider planLimitsProvider) {

        @SuppressWarnings("unchecked")
        ObjectProvider<DataStorage> dataStorageObjectProvider = (ObjectProvider<DataStorage>) mock(
            ObjectProvider.class);

        lenient()
            .when(dataStorageObjectProvider.getIfAvailable())
            .thenReturn(dataStorage);

        @SuppressWarnings("unchecked")
        ObjectProvider<PlanLimitsProvider> planLimitsProviderObjectProvider =
            (ObjectProvider<PlanLimitsProvider>) mock(ObjectProvider.class);

        when(planLimitsProviderObjectProvider.getIfAvailable()).thenReturn(planLimitsProvider);

        return new JobRetentionMonitor(
            dataStorageObjectProvider, defaultRetentionDays, jobFacade, jobService, planLimitsProviderObjectProvider,
            tenantService);
    }
}
