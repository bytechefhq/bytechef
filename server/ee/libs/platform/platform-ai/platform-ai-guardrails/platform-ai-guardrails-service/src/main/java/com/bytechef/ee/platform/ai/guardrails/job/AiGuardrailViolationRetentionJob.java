/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.guardrails.job;

import com.bytechef.ee.platform.ai.guardrails.repository.AiGuardrailViolationRepository;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class AiGuardrailViolationRetentionJob {

    private static final Logger log = LoggerFactory.getLogger(AiGuardrailViolationRetentionJob.class);

    private final AiGuardrailViolationRepository aiGuardrailViolationRepository;
    private final long retentionDays;

    @SuppressFBWarnings("EI")
    public AiGuardrailViolationRetentionJob(
        AiGuardrailViolationRepository aiGuardrailViolationRepository,
        @Value("${bytechef.ai.guardrails.violation.retention-days:30}") long retentionDays) {

        this.aiGuardrailViolationRepository = aiGuardrailViolationRepository;
        this.retentionDays = retentionDays;
    }

    @Scheduled(cron = "${bytechef.ai.guardrails.violation.retention-cron:0 30 2 * * *}")
    public void purgeExpiredViolations() {
        Instant cutoff = Instant.now()
            .minus(Duration.ofDays(retentionDays));

        int deleted = aiGuardrailViolationRepository.deleteByCreatedDateBefore(cutoff);

        if (deleted > 0) {
            log.info("Guardrail violation retention: deleted {} record(s) older than {}", deleted, cutoff);
        } else {
            log.debug("Guardrail violation retention: no records older than {}", cutoff);
        }
    }
}
