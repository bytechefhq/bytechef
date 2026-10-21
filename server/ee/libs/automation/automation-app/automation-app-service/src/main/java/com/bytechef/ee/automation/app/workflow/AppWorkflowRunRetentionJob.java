/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.workflow;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.automation.app.service.AppWorkflowService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.service.TenantService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Deletes app workflow run records older than {@code bytechef.app.workflow.run-retention-days}. Runs on
 * {@code bytechef.app.workflow.run-retention-cron} (daily at 02:40 local time by default) to limit growth of
 * {@code app_workflow_run}; a purged run polls as not found. A retention of zero or fewer days disables the sweep.
 *
 * <p>
 * Every tenant is swept under its own context, because the delete runs on the tenant's schema; a tenant whose sweep
 * fails is logged and the remaining tenants are still swept. A deployment without a tenant service sweeps the scheduler
 * thread's current tenant.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class AppWorkflowRunRetentionJob {

    private static final Logger log = LoggerFactory.getLogger(AppWorkflowRunRetentionJob.class);

    private final ApplicationProperties applicationProperties;
    private final AppWorkflowService appWorkflowService;
    private final ObjectProvider<TenantService> tenantServiceObjectProvider;

    @SuppressFBWarnings("EI")
    public AppWorkflowRunRetentionJob(
        ApplicationProperties applicationProperties, AppWorkflowService appWorkflowService,
        ObjectProvider<TenantService> tenantServiceObjectProvider) {

        this.applicationProperties = applicationProperties;
        this.appWorkflowService = appWorkflowService;
        this.tenantServiceObjectProvider = tenantServiceObjectProvider;
    }

    @Scheduled(cron = "${bytechef.app.workflow.run-retention-cron:0 40 2 * * *}")
    public void purgeExpiredRuns() {
        long retentionDays = applicationProperties.getApp()
            .getWorkflow()
            .getRunRetentionDays();

        if (retentionDays <= 0) {
            log.debug("App workflow run retention is disabled");

            return;
        }

        Instant cutoff = Instant.now()
            .minus(retentionDays, ChronoUnit.DAYS);

        TenantService tenantService = tenantServiceObjectProvider.getIfAvailable();

        if (tenantService == null) {
            purgeExpiredRunsForCurrentTenant(cutoff);

            return;
        }

        for (String tenantId : tenantService.getTenantIds()) {
            try {
                TenantContext.runWithTenantId(tenantId, () -> purgeExpiredRunsForCurrentTenant(cutoff));
            } catch (RuntimeException exception) {
                log.warn("App workflow run retention sweep failed for tenant {}", tenantId, exception);
            }
        }
    }

    private void purgeExpiredRunsForCurrentTenant(Instant cutoff) {
        int deleted = appWorkflowService.deleteRunsCreatedBefore(cutoff);

        if (deleted > 0) {
            log.info(
                "App workflow run retention: deleted {} row(s) older than {} for tenant {}", deleted, cutoff,
                TenantContext.getCurrentTenantId());
        } else {
            log.debug(
                "App workflow run retention: no rows older than {} for tenant {}", cutoff,
                TenantContext.getCurrentTenantId());
        }
    }
}
