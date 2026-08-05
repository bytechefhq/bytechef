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
import com.bytechef.platform.data.storage.DataStorage;
import com.bytechef.platform.data.storage.domain.DataStorageScope;
import com.bytechef.platform.plan.provider.PlanLimitsProvider;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.service.TenantService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Purges finished jobs older than the retention window: the plan's {@code logRetentionDays} when one is set for the
 * current tenant, otherwise the operator-configured
 * {@code bytechef.workflow.execution.retention.default-retention-days}. With neither configured the monitor is a no-op
 * — the pre-plan behavior where execution history was kept forever.
 *
 * <p>
 * Only terminal jobs carry an end date, so the end-date finder naturally skips in-flight runs. Subflow child jobs are
 * skipped individually — {@link JobFacade#deleteJob(long)} cascades over child jobs, task executions, contexts, and the
 * associated file-storage blobs, so a child is removed together with its expired parent (a parent always ends at or
 * after its children). Like the other monitors the sweep runs per tenant under that tenant's context; in the
 * distributed deployment both the ended-jobs query and the delete route to execution-app through the remote clients,
 * and the defensive {@link UnsupportedOperationException} handling stays for deployments that lack them.
 * </p>
 *
 * @author Ivica Cardic
 */
public class JobRetentionMonitor {

    private static final Logger log = LoggerFactory.getLogger(JobRetentionMonitor.class);

    @Nullable
    private final Integer defaultRetentionDays;

    private final ObjectProvider<DataStorage> dataStorageObjectProvider;
    private final JobFacade jobFacade;
    private final JobService jobService;
    private final ObjectProvider<PlanLimitsProvider> planLimitsProviderObjectProvider;
    private final TenantService tenantService;

    @SuppressFBWarnings("EI2")
    public JobRetentionMonitor(
        ObjectProvider<DataStorage> dataStorageObjectProvider, @Nullable Integer defaultRetentionDays,
        JobFacade jobFacade, JobService jobService,
        ObjectProvider<PlanLimitsProvider> planLimitsProviderObjectProvider, TenantService tenantService) {

        this.dataStorageObjectProvider = dataStorageObjectProvider;
        this.defaultRetentionDays = defaultRetentionDays;
        this.jobFacade = jobFacade;
        this.jobService = jobService;
        this.planLimitsProviderObjectProvider = planLimitsProviderObjectProvider;
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
        Integer retentionDays = resolveRetentionDays();

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

            Long expiredJobId = expiredJob.getId();

            if (expiredJobId == null) {
                continue;
            }

            try {
                jobFacade.deleteJob(expiredJobId);

                deleteExecutionData(expiredJobId);

                purgedJobCount++;
            } catch (UnsupportedOperationException exception) {
                log.debug("Job deletion unsupported in this deployment; skipping retention purge");

                return;
            } catch (RuntimeException exception) {
                log.warn("Failed to purge expired job {}", expiredJobId, exception);
            }
        }

        if (purgedJobCount > 0) {
            log.info("Purged {} job(s) older than the {}-day retention window", purgedJobCount, retentionDays);
        }
    }

    /**
     * Drops the job's {@code CURRENT_EXECUTION} data-storage entries (crash-orphaned agent/agentic checkpoints and
     * anything else components stored at that scope). Best-effort: the rows are already gone, so a data-storage failure
     * — or a provider that cannot enumerate by scope (file-storage) — must not fail the sweep.
     */
    private void deleteExecutionData(long jobId) {
        DataStorage dataStorage = dataStorageObjectProvider.getIfAvailable();

        if (dataStorage == null) {
            return;
        }

        try {
            dataStorage.deleteScopeData(DataStorageScope.CURRENT_EXECUTION, String.valueOf(jobId));
        } catch (UnsupportedOperationException exception) {
            log.debug("Scope-wide data-storage delete unsupported by this provider; skipping execution-data cleanup");
        } catch (RuntimeException exception) {
            log.warn("Failed to delete execution data for purged job {}", jobId, exception);
        }
    }

    @Nullable
    private Integer resolveRetentionDays() {
        PlanLimitsProvider planLimitsProvider = planLimitsProviderObjectProvider.getIfAvailable();

        if (planLimitsProvider != null) {
            Integer logRetentionDays = planLimitsProvider.getPlanLimits(TenantContext.getCurrentTenantId())
                .logRetentionDays();

            if (logRetentionDays != null) {
                return logRetentionDays;
            }
        }

        return defaultRetentionDays;
    }
}
