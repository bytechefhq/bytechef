/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.guardrails.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.ee.platform.ai.guardrails.config.AiGuardrailViolationIntTestConfiguration;
import com.bytechef.ee.platform.ai.guardrails.domain.AiGuardrailCustomRule;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ActiveProfiles("testint")
@SpringBootTest(classes = AiGuardrailViolationIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
public class AiGuardrailCustomRuleRepositoryIntTest {

    @Autowired
    private AiGuardrailCustomRuleRepository aiGuardrailCustomRuleRepository;

    @Test
    public void testTheChangelogCreatesATableTheEntityCanRoundTrip() {
        AiGuardrailCustomRule saved = aiGuardrailCustomRuleRepository.save(
            new AiGuardrailCustomRule(
                42L, "ACME_ACCOUNT_ID", "\\bACME-\\d{4}-[A-Z]{2}\\b", 0, new BigDecimal("0.90"),
                "acme account,acme id", 40, new BigDecimal("0.95")));

        assertThat(saved.getId()).isNotNull();

        AiGuardrailCustomRule reloaded = aiGuardrailCustomRuleRepository.findById(saved.getId())
            .orElseThrow();

        assertThat(reloaded.getType()).isEqualTo("ACME_ACCOUNT_ID");
        assertThat(reloaded.getPattern()).isEqualTo("\\bACME-\\d{4}-[A-Z]{2}\\b");
        assertThat(reloaded.getWorkspaceId()).isEqualTo(42L);
        assertThat(reloaded.getContextKeywords()).isEqualTo("acme account,acme id");
        assertThat(reloaded.getContextWindow()).isEqualTo(40);

        assertThat(reloaded.getCreatedDate()).isNotNull();
        assertThat(reloaded.isEnabled()).isFalse();
    }

    @Test
    public void testAWorkspaceCannotDefineTheSameTypeTwice() {
        aiGuardrailCustomRuleRepository.save(
            new AiGuardrailCustomRule(
                7L, "ACME_DUPLICATE", "\\bA\\d{4}\\b", 0, new BigDecimal("0.90"), null, null, null));

        assertThatThrownBy(
            () -> aiGuardrailCustomRuleRepository.save(
                new AiGuardrailCustomRule(
                    7L, "ACME_DUPLICATE", "\\bB\\d{4}\\b", 0, new BigDecimal("0.90"), null, null, null)))
                        .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    public void testTwoWorkspacesMayEachDefineTheSameType() {
        aiGuardrailCustomRuleRepository.save(
            new AiGuardrailCustomRule(
                11L, "ACME_SHARED_NAME", "\\bA\\d{4}\\b", 0, new BigDecimal("0.90"), null, null, null));

        AiGuardrailCustomRule other = aiGuardrailCustomRuleRepository.save(
            new AiGuardrailCustomRule(
                12L, "ACME_SHARED_NAME", "\\bA\\d{4}\\b", 0, new BigDecimal("0.90"), null, null, null));

        assertThat(other.getId()).isNotNull();
    }

    @Test
    public void testTheEnabledQueryReturnsOnlyEnabledRules() {
        AiGuardrailCustomRule disabled = aiGuardrailCustomRuleRepository.save(
            new AiGuardrailCustomRule(
                21L, "ACME_OFF", "\\bA\\d{4}\\b", 0, new BigDecimal("0.90"), null, null, null));
        AiGuardrailCustomRule enabled = aiGuardrailCustomRuleRepository.save(
            new AiGuardrailCustomRule(
                21L, "ACME_ON", "\\bB\\d{4}\\b", 0, new BigDecimal("0.90"), null, null, null));

        enabled.setEnabled(true);

        aiGuardrailCustomRuleRepository.save(enabled);

        assertThat(aiGuardrailCustomRuleRepository.findAllByWorkspaceIdAndEnabledTrueOrderByTypeAsc(21L))
            .extracting(AiGuardrailCustomRule::getType)
            .containsExactly("ACME_ON")
            .doesNotContain(disabled.getType());
    }

    @Test
    public void testARuleIdFromAnotherWorkspaceReadsAsAbsent() {
        AiGuardrailCustomRule saved = aiGuardrailCustomRuleRepository.save(
            new AiGuardrailCustomRule(
                31L, "ACME_SCOPED", "\\bA\\d{4}\\b", 0, new BigDecimal("0.90"), null, null, null));

        assertThat(aiGuardrailCustomRuleRepository.findByIdAndWorkspaceId(saved.getId(), 32L)).isEmpty();
        assertThat(aiGuardrailCustomRuleRepository.findByIdAndWorkspaceId(saved.getId(), 31L)).isPresent();
    }
}
