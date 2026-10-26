/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.guardrails.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.ai.guardrails.domain.AiGuardrailCustomRule;
import com.bytechef.ee.platform.ai.guardrails.repository.AiGuardrailCustomRuleRepository;
import com.bytechef.platform.ai.sensitivedata.CustomPatternValidator;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class AiGuardrailCustomRuleServiceTest {

    private final AiGuardrailCustomRuleRepository repository = mock(AiGuardrailCustomRuleRepository.class);

    private final AiGuardrailCustomRuleService service = newService(50);

    @Test
    void testCreateRejectsAnUnvalidatablePattern() {
        assertThatThrownBy(() -> service.create(rule("ACME_ID", "\\bACME-\\d+\\b")))
            .isInstanceOf(CustomPatternValidator.CustomPatternRejectedException.class)
            .hasMessageContaining("upper bound");

        verify(repository, never()).save(any());
    }

    @Test
    void testCreateStoresAValidatedRuleDisabled() {
        when(repository.countByWorkspaceId(anyLong())).thenReturn(0L);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.create(rule("ACME_ID", "\\bACME-\\d{4}\\b"));

        ArgumentCaptor<AiGuardrailCustomRule> captor = ArgumentCaptor.forClass(AiGuardrailCustomRule.class);

        verify(repository).save(captor.capture());

        assertThat(captor.getValue()
            .isEnabled())
                .as("a rule is created disabled, whatever the caller asked for")
                .isFalse();
    }

    @Test
    void testCreateOverridesACallerSuppliedEnabledFlag() {
        when(repository.countByWorkspaceId(anyLong())).thenReturn(0L);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AiGuardrailCustomRule customRule = rule("ACME_ID", "\\bACME-\\d{4}\\b");

        customRule.setEnabled(true);

        service.create(customRule);

        ArgumentCaptor<AiGuardrailCustomRule> captor = ArgumentCaptor.forClass(AiGuardrailCustomRule.class);

        verify(repository).save(captor.capture());

        assertThat(captor.getValue()
            .isEnabled()).isFalse();
    }

    @Test
    void testUpdatingAPatternRevalidatesIt() {
        AiGuardrailCustomRule existing = rule("ACME_ID", "\\bACME-\\d{4}\\b");

        when(repository.findByIdAndWorkspaceId(1L, 42L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.updatePattern(1L, 42L, "\\bACME-\\d+\\b"))
            .isInstanceOf(CustomPatternValidator.CustomPatternRejectedException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void testTheCountCapRefusesAFurtherRule() {
        AiGuardrailCustomRuleService capped = newService(2);

        when(repository.countByWorkspaceId(42L)).thenReturn(2L);

        assertThatThrownBy(() -> capped.create(rule("ACME_ID", "\\bACME-\\d{4}\\b")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("at most 2");

        verify(repository, never()).save(any());
    }

    private AiGuardrailCustomRuleService newService(int maxRulesPerWorkspace) {
        @SuppressWarnings("unchecked")
        ObjectProvider<com.bytechef.ee.platform.ai.guardrails.AiGuardrailMetrics> metricsProvider =
            mock(ObjectProvider.class);

        return new AiGuardrailCustomRuleServiceImpl(repository, metricsProvider, maxRulesPerWorkspace);
    }

    private static AiGuardrailCustomRule rule(String type, String pattern) {
        return new AiGuardrailCustomRule(42L, type, pattern, 0, new BigDecimal("0.90"), null, null, null);
    }
}
