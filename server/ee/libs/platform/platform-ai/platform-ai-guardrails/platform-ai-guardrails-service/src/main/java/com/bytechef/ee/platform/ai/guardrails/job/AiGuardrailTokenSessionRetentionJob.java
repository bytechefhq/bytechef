/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.guardrails.job;

import com.bytechef.ee.platform.ai.guardrails.domain.AiGuardrailTokenSession;
import com.bytechef.ee.platform.ai.guardrails.repository.AiGuardrailTokenSessionRepository;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
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
public class AiGuardrailTokenSessionRetentionJob {

    private static final Logger log = LoggerFactory.getLogger(AiGuardrailTokenSessionRetentionJob.class);

    private final AiGuardrailTokenSessionRepository aiGuardrailTokenSessionRepository;
    private final long retentionDays;

    @SuppressFBWarnings("EI")
    public AiGuardrailTokenSessionRetentionJob(
        AiGuardrailTokenSessionRepository aiGuardrailTokenSessionRepository,
        @Value("${bytechef.ai.guardrails.session.retention-days:30}") long retentionDays) {

        this.aiGuardrailTokenSessionRepository = aiGuardrailTokenSessionRepository;
        this.retentionDays = retentionDays;
    }

    @Scheduled(cron = "${bytechef.ai.guardrails.session.retention-cron:0 0 3 * * *}")
    public void purgeExpiredTokenSessions() {
        Instant cutoff = Instant.now()
            .minus(Duration.ofDays(retentionDays));

        List<AiGuardrailTokenSession> expired = aiGuardrailTokenSessionRepository.findByLastModifiedDateBefore(cutoff);

        if (expired.isEmpty()) {
            log.debug("Guardrail token session retention: no sessions older than {}", cutoff);

            return;
        }

        aiGuardrailTokenSessionRepository.deleteAll(expired);

        log.info("Guardrail token session retention: deleted {} session(s) older than {}", expired.size(), cutoff);
    }
}
