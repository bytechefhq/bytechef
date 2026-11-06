/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.external.invocation.log.job;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.platform.external.invocation.log.service.ExternalInvocationLogService;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.service.TenantService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class ExternalInvocationLogRetentionJob {

    private static final Logger log = LoggerFactory.getLogger(ExternalInvocationLogRetentionJob.class);

    private final long retentionDays;
    private final TenantService tenantService;
    private final ExternalInvocationLogService externalInvocationLogService;

    @SuppressFBWarnings("EI")
    public ExternalInvocationLogRetentionJob(
        ApplicationProperties applicationProperties, TenantService tenantService,
        ExternalInvocationLogService externalInvocationLogService) {

        ApplicationProperties.ExternalInvocationLog externalInvocationLog =
            applicationProperties.getExternalInvocationLog();

        this.retentionDays = externalInvocationLog.getRetentionDays();
        this.tenantService = tenantService;
        this.externalInvocationLogService = externalInvocationLogService;
    }

    @Scheduled(cron = "0 30 2 * * ?")
    @SchedulerLock(name = "purgeExpiredExternalInvocationLogs", lockAtLeastFor = "PT5M", lockAtMostFor = "PT1H")
    public void purgeExpiredLogs() {
        Instant cutoff = Instant.now()
            .minus(retentionDays, ChronoUnit.DAYS);

        runForEachTenant(() -> purgeLogsCreatedBefore(cutoff));
    }

    private void purgeLogsCreatedBefore(Instant cutoff) {
        int deleted = externalInvocationLogService.deleteExternalInvocationLogsCreatedBefore(cutoff);

        if (deleted > 0) {
            log.info("External invocation log retention: deleted {} row(s) older than {}", deleted, cutoff);
        } else {
            log.debug("External invocation log retention: no rows older than {}", cutoff);
        }
    }

    private void runForEachTenant(Runnable purge) {
        if (!tenantService.isMultiTenantEnabled()) {
            purge.run();

            return;
        }

        for (String tenantId : tenantService.getTenantIds()) {
            try {
                TenantContext.runWithTenantId(tenantId, purge::run);
            } catch (RuntimeException exception) {
                log.error("External invocation log retention failed for tenant {}", tenantId, exception);
            }
        }
    }
}
