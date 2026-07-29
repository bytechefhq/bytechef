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
import com.bytechef.tenant.service.TenantService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

/**
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
        when(job.getParentTaskExecutionId()).thenReturn(null);

        when(tenantService.getTenantIds()).thenReturn(List.of("public"));
    }

    @Test
    public void testExpiredJobIsPurged() {
        when(jobService.getEndedJobs(any())).thenReturn(List.of(job));

        JobRetentionMonitor jobRetentionMonitor = getJobRetentionMonitor(30);

        jobRetentionMonitor.purgeExpiredJobs();

        verify(jobFacade).deleteJob(1L);
        verify(dataStorage).deleteScopeData(DataStorageScope.CURRENT_EXECUTION, "1");
    }

    @Test
    public void testSubflowChildJobIsSkipped() {
        when(job.getParentTaskExecutionId()).thenReturn(5L);

        when(jobService.getEndedJobs(any())).thenReturn(List.of(job));

        JobRetentionMonitor jobRetentionMonitor = getJobRetentionMonitor(30);

        jobRetentionMonitor.purgeExpiredJobs();

        verify(jobFacade, never()).deleteJob(1L);
    }

    @Test
    public void testNoConfiguredRetentionIsANoOp() {
        JobRetentionMonitor jobRetentionMonitor = getJobRetentionMonitor(null);

        jobRetentionMonitor.purgeExpiredJobs();

        verify(jobService, never()).getEndedJobs(any());
    }

    private JobRetentionMonitor getJobRetentionMonitor(Integer defaultRetentionDays) {

        @SuppressWarnings("unchecked")
        ObjectProvider<DataStorage> dataStorageObjectProvider = (ObjectProvider<DataStorage>) mock(
            ObjectProvider.class);

        lenient()
            .when(dataStorageObjectProvider.getIfAvailable())
            .thenReturn(dataStorage);

        return new JobRetentionMonitor(
            dataStorageObjectProvider, defaultRetentionDays, jobFacade, jobService, tenantService);
    }
}
