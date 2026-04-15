/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.audit.job;

import com.bytechef.ee.platform.audit.repository.PersistenceAuditEventRepository;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Deletes audit events older than {@code bytechef.audit.retention-days} (default 365). Runs daily at 02:00 local time
 * to reclaim space and limit growth of {@code persistent_audit_event}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class AuditEventRetentionJob {

    private static final Logger logger = LoggerFactory.getLogger(AuditEventRetentionJob.class);

    private final PersistenceAuditEventRepository persistenceAuditEventRepository;
    private final long retentionDays;

    @SuppressFBWarnings("EI")
    public AuditEventRetentionJob(
        PersistenceAuditEventRepository persistenceAuditEventRepository,
        @Value("${bytechef.audit.retention-days:365}") long retentionDays) {

        this.persistenceAuditEventRepository = persistenceAuditEventRepository;
        this.retentionDays = retentionDays;
    }

    @Scheduled(cron = "${bytechef.audit.retention-cron:0 0 2 * * *}")
    public void purgeExpiredEvents() {
        LocalDateTime cutoff = LocalDateTime.now()
            .minusDays(retentionDays);

        int deleted = persistenceAuditEventRepository.deleteByEventDateBefore(cutoff);

        if (deleted > 0) {
            logger.info("Audit retention: deleted {} event(s) older than {}", deleted, cutoff);
        } else {
            logger.debug("Audit retention: no events older than {}", cutoff);
        }
    }
}
