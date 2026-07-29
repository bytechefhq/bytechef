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

import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.service.TenantService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * @author Ivica Cardic
 */
public class JobRetentionMonitor {

    private static final Logger log = LoggerFactory.getLogger(JobRetentionMonitor.class);

    @Nullable
    private final Integer defaultRetentionDays;

    private final JobFacade jobFacade;
    private final JobService jobService;
    private final TenantService tenantService;

    @SuppressFBWarnings("EI2")
    public JobRetentionMonitor(
        @Nullable Integer defaultRetentionDays, JobFacade jobFacade, JobService jobService,
        TenantService tenantService) {

        this.defaultRetentionDays = defaultRetentionDays;
        this.jobFacade = jobFacade;
        this.jobService = jobService;
        this.tenantService = tenantService;
    }

    @Scheduled(initialDelayString = "PT10M", fixedDelayString = "PT6H")
    public void purgeExpiredJobs() {
        for (String tenantId : tenantService.getTenantIds()) {
            try {
                TenantContext.runWithTenantId(tenantId, this::purgeExpiredJobsForCurrentTenant);
            } catch (RuntimeException exception) {
                log.warn("Job-retention sweep failed for tenant {}", tenantId, exception);
            }
        }
    }

    private void purgeExpiredJobsForCurrentTenant() {
        Integer retentionDays = defaultRetentionDays;

        if (retentionDays == null) {
            return;
        }

        Instant cutoff = Instant.now()
            .minus(Duration.ofDays(retentionDays));

        List<Job> expiredJobs;

        try {
            expiredJobs = jobService.getEndedJobs(cutoff);
        } catch (UnsupportedOperationException exception) {
            log.debug("Ended-jobs query unsupported in this deployment; skipping retention purge");

            return;
        }

        int purgedJobCount = 0;

        for (Job expiredJob : expiredJobs) {
            if (expiredJob.getParentTaskExecutionId() != null) {
                continue;
            }

            try {
                jobFacade.deleteJob(expiredJob.getId());

                purgedJobCount++;
            } catch (UnsupportedOperationException exception) {
                log.debug("Job deletion unsupported in this deployment; skipping retention purge");

                return;
            } catch (RuntimeException exception) {
                log.warn("Failed to purge expired job {}", expiredJob.getId(), exception);
            }
        }

        if (purgedJobCount > 0) {
            log.info("Purged {} job(s) older than the {}-day retention window", purgedJobCount, retentionDays);
        }
    }

}
