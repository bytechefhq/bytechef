/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayBudgetEnforcementMode;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayProviderType;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityAlertCondition;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilitySpanLevel;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilitySpanStatus;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilitySpanType;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityTraceSource;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityTraceStatus;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityWebhookDeliveryStatus;
import org.junit.jupiter.api.Test;

/**
 * Pins the persisted-INT-ordinal contract for every gateway-api enum that is written to the database. Each enum's
 * Javadoc documents the append-only invariant; this test makes a regression that reorders, removes, or alphabetises a
 * value fail at build time. Without these pins, a cosmetic refactor would silently flip the meaning of every historical
 * row — and {@code safeOrdinalLookup} only surfaces such corruption after damage is written.
 *
 * <p>
 * Add new variants only at the end of the enum, and append a corresponding {@code containsExactly} entry here.
 *
 * @author Ivica Cardic
 * @version ee
 */
class PersistedEnumOrdinalContractTest {

    // Eval enum ordinal contracts moved to platform-ai-eval-api/AiEvalPersistedEnumOrdinalContractTest after
    // AiEvalExecutionStatus / AiEvalExecutionFailureReason / AiEvalScoreDataType / AiEvalScoreSource were
    // relocated to com.bytechef.ee.platform.ai.eval.domain.

    @Test
    void testAiObservabilitySpanLevelOrdinalContract() {
        assertThat(AiObservabilitySpanLevel.values()).containsExactly(
            AiObservabilitySpanLevel.DEBUG,
            AiObservabilitySpanLevel.DEFAULT,
            AiObservabilitySpanLevel.WARNING,
            AiObservabilitySpanLevel.ERROR);
    }

    @Test
    void testAiObservabilitySpanStatusOrdinalContract() {
        assertThat(AiObservabilitySpanStatus.values()).containsExactly(
            AiObservabilitySpanStatus.ACTIVE,
            AiObservabilitySpanStatus.COMPLETED,
            AiObservabilitySpanStatus.ERROR);
    }

    @Test
    void testAiObservabilitySpanTypeOrdinalContract() {
        assertThat(AiObservabilitySpanType.values()).containsExactly(
            AiObservabilitySpanType.GENERATION,
            AiObservabilitySpanType.SPAN,
            AiObservabilitySpanType.EVENT,
            AiObservabilitySpanType.TOOL_CALL,
            AiObservabilitySpanType.RETRIEVAL);
    }

    @Test
    void testAiObservabilityTraceSourceOrdinalContract() {
        // ai_observability_trace.source is persisted as INT (ordinal) — see schema.
        // A reorder here would silently flip the source attribution of every historical row.
        assertThat(AiObservabilityTraceSource.values()).containsExactly(
            AiObservabilityTraceSource.API,
            AiObservabilityTraceSource.PLAYGROUND,
            AiObservabilityTraceSource.OTLP,
            AiObservabilityTraceSource.EXPERIMENT);
    }

    @Test
    void testAiGatewayProviderTypeOrdinalContract() {
        // The enum's own Javadoc carries an explicit APPEND-ONLY comment and exposes fromOrdinal(int) — both
        // strong indicators of ordinal-based persistence. An IDE alphabetize-refactor on this enum would silently
        // flip the provider attribution of every historical workspace_ai_gateway_provider row.
        assertThat(AiGatewayProviderType.values()).containsExactly(
            AiGatewayProviderType.ANTHROPIC,
            AiGatewayProviderType.AZURE_OPENAI,
            AiGatewayProviderType.COHERE,
            AiGatewayProviderType.DEEPSEEK,
            AiGatewayProviderType.GOOGLE_GEMINI,
            AiGatewayProviderType.GROQ,
            AiGatewayProviderType.MISTRAL,
            AiGatewayProviderType.OPENAI);
    }

    @Test
    void testAiObservabilityTraceStatusOrdinalContract() {
        assertThat(AiObservabilityTraceStatus.values()).containsExactly(
            AiObservabilityTraceStatus.ACTIVE,
            AiObservabilityTraceStatus.COMPLETED,
            AiObservabilityTraceStatus.ERROR);
    }


    @Test
    void testAiObservabilityAlertConditionOrdinalContract() {
        assertThat(AiObservabilityAlertCondition.values()).containsExactly(
            AiObservabilityAlertCondition.GREATER_THAN,
            AiObservabilityAlertCondition.LESS_THAN,
            AiObservabilityAlertCondition.EQUALS);
    }

    @Test
    void testAiGatewayBudgetEnforcementModeOrdinalContract() {
        assertThat(AiGatewayBudgetEnforcementMode.values()).containsExactly(
            AiGatewayBudgetEnforcementMode.HARD,
            AiGatewayBudgetEnforcementMode.SOFT);
    }

    @Test
    void testAiObservabilityWebhookDeliveryStatusOrdinalContract() {
        assertThat(AiObservabilityWebhookDeliveryStatus.values()).containsExactly(
            AiObservabilityWebhookDeliveryStatus.PENDING,
            AiObservabilityWebhookDeliveryStatus.SUCCESS,
            AiObservabilityWebhookDeliveryStatus.FAILED,
            AiObservabilityWebhookDeliveryStatus.RETRYING);
    }
}
