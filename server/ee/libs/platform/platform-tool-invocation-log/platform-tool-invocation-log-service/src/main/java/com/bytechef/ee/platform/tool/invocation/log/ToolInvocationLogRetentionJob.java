/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.tool.invocation.log;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.service.TenantService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class ToolInvocationLogRetentionJob {

    private static final Logger log = LoggerFactory.getLogger(ToolInvocationLogRetentionJob.class);

    private final long retentionDays;
    private final TenantService tenantService;
    private final ToolInvocationLogService toolInvocationLogService;

    @SuppressFBWarnings("EI")
    public ToolInvocationLogRetentionJob(
        ApplicationProperties applicationProperties, TenantService tenantService,
        ToolInvocationLogService toolInvocationLogService) {

        ApplicationProperties.ToolInvocationLog toolInvocationLog = applicationProperties.getToolInvocationLog();

        this.retentionDays = toolInvocationLog.getRetentionDays();
        this.tenantService = tenantService;
        this.toolInvocationLogService = toolInvocationLogService;
    }

    @Scheduled(cron = "0 30 2 * * ?")
    @SchedulerLock(name = "purgeExpiredToolInvocationLogs", lockAtLeastFor = "PT5M", lockAtMostFor = "PT1H")
    public void purgeExpiredLogs() {
        Instant cutoff = Instant.now()
            .minus(retentionDays, ChronoUnit.DAYS);

        runForEachTenant(() -> purgeLogsCreatedBefore(cutoff));
    }

    private void purgeLogsCreatedBefore(Instant cutoff) {
        int deleted = toolInvocationLogService.deleteToolInvocationLogsCreatedBefore(cutoff);

        if (deleted > 0) {
            log.info("Tool invocation log retention: deleted {} row(s) older than {}", deleted, cutoff);
        } else {
            log.debug("Tool invocation log retention: no rows older than {}", cutoff);
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
                log.error("Tool invocation log retention failed for tenant {}", tenantId, exception);
            }
        }
    }
}
