/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.tool.invocation.log;

import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Deletes tool invocation log rows older than {@code bytechef.tool-invocation-log.retention-days} (default 90). Runs
 * daily at 02:30 local time to limit growth of {@code tool_invocation_log}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class ToolInvocationLogRetentionJob {

    private static final Logger log = LoggerFactory.getLogger(ToolInvocationLogRetentionJob.class);

    private final long retentionDays;
    private final ToolInvocationLogService toolInvocationLogService;

    @SuppressFBWarnings("EI")
    public ToolInvocationLogRetentionJob(
        @Value("${bytechef.tool-invocation-log.retention-days:90}") long retentionDays,
        ToolInvocationLogService toolInvocationLogService) {

        this.retentionDays = retentionDays;
        this.toolInvocationLogService = toolInvocationLogService;
    }

    @Scheduled(cron = "${bytechef.tool-invocation-log.retention-cron:0 30 2 * * *}")
    public void purgeExpiredLogs() {
        Instant cutoff = Instant.now()
            .minus(retentionDays, ChronoUnit.DAYS);

        int deleted = toolInvocationLogService.deleteToolInvocationLogsCreatedBefore(cutoff);

        if (deleted > 0) {
            log.info("Tool invocation log retention: deleted {} row(s) older than {}", deleted, cutoff);
        } else {
            log.debug("Tool invocation log retention: no rows older than {}", cutoff);
        }
    }
}
