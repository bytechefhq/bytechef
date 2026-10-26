/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.guardrails.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.platform.ai.guardrails.config.AiGuardrailViolationIntTestConfiguration;
import com.bytechef.ee.platform.ai.guardrails.domain.AiGuardrailViolation;
import com.bytechef.ee.platform.ai.guardrails.domain.AiGuardrailViolationAction;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ActiveProfiles("testint")
@SpringBootTest(classes = AiGuardrailViolationIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
public class AiGuardrailViolationRepositoryIntTest {

    @Autowired
    private AiGuardrailViolationRepository aiGuardrailViolationRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void testTheChangelogCreatesATableTheEntityCanRoundTrip() {
        AiGuardrailViolation saved = aiGuardrailViolationRepository.save(
            new AiGuardrailViolation(
                "EMAIL_ADDRESS", 0, 5, 11, new BigDecimal("0.90"), AiGuardrailViolationAction.REDACTED, "ai_agent",
                42L, 1, "admin@localhost.com"));

        assertThat(saved.getId()).isNotNull();

        AiGuardrailViolation reloaded = aiGuardrailViolationRepository.findById(saved.getId())
            .orElseThrow();

        assertThat(reloaded.getCategory()).isEqualTo("EMAIL_ADDRESS");
        assertThat(reloaded.getSpanStart()).isEqualTo(5);
        assertThat(reloaded.getSpanLength()).isEqualTo(11);
        assertThat(reloaded.getAction()).isEqualTo(AiGuardrailViolationAction.REDACTED);
        assertThat(reloaded.getSurface()).isEqualTo("ai_agent");
        assertThat(reloaded.getWorkspaceId()).isEqualTo(42L);

        assertThat(reloaded.getCreatedDate()).isNotNull();
    }

    @Test
    public void testTheSchemaHasNoColumnForAMatchedValue() {
        List<String> columns = jdbcTemplate.queryForList(
            "SELECT column_name FROM information_schema.columns WHERE table_name = 'ai_guardrail_violation'",
            String.class);

        assertThat(columns)
            .containsExactlyInAnyOrder(
                "id", "workspace_id", "environment", "surface", "category", "kind", "span_start", "span_length",
                "confidence", "action", "principal", "created_date");
    }

    @Test
    public void testTheNullWorkspaceBucketIsCountedRatherThanSkipped() {
        aiGuardrailViolationRepository.save(
            new AiGuardrailViolation(
                "US_SSN", 0, 0, 11, new BigDecimal("0.60"), AiGuardrailViolationAction.BLOCKED, "ai_agent",
                null, null, null));

        long counted = aiGuardrailViolationRepository.countForWorkspaceSince(
            null, Instant.now()
                .minus(Duration.ofDays(1)));

        assertThat(counted).isPositive();
    }

    @Test
    public void testRetentionDeletesOnlyRowsOlderThanTheCutoff() {
        aiGuardrailViolationRepository.save(
            new AiGuardrailViolation(
                "EMAIL_ADDRESS", 0, 0, 11, new BigDecimal("0.90"), AiGuardrailViolationAction.REDACTED, "copilot",
                7L, 1, null));

        int deleted = aiGuardrailViolationRepository.deleteByCreatedDateBefore(
            Instant.now()
                .minus(Duration.ofDays(30)));

        assertThat(deleted).isZero();
        assertThat(aiGuardrailViolationRepository.findAllByWorkspaceIdOrderByCreatedDateDesc(7L)).isNotEmpty();
    }
}
