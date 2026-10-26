/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.guardrails;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.bytechef.platform.ai.sensitivedata.SensitiveDataMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class AiGuardrailMetricsTest {

    @Test
    void testRecordIncrementsCounterByEventAndSurface() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

        AiGuardrailMetrics metrics = new AiGuardrailMetrics(meterRegistry, "gateway");

        metrics.record("pii_redacted");
        metrics.record("pii_redacted");
        metrics.record("blocked_term");

        assertThat(meterRegistry.counter(
            AiGuardrailMetrics.COUNTER_NAME, "event", "pii_redacted", "surface", "gateway")
            .count()).isEqualTo(2.0);
        assertThat(meterRegistry.counter(
            AiGuardrailMetrics.COUNTER_NAME, "event", "blocked_term", "surface", "gateway")
            .count()).isEqualTo(1.0);
    }

    @Test
    void testRecordIsNoOpWithoutRegistry() {
        AiGuardrailMetrics metrics = new AiGuardrailMetrics((MeterRegistry) null, "gateway");

        assertThatCode(() -> metrics.record("pii_redacted")).doesNotThrowAnyException();
    }

    @Test
    void testRecordDetectorFailureDelegatesToDetectorFailedEventUnderThisInstancesSurface() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

        AiGuardrailMetrics metrics = new AiGuardrailMetrics(meterRegistry, "ai_agent");

        metrics.recordDetectorFailure("opennlp");

        assertThat(meterRegistry.counter(
            AiGuardrailMetrics.COUNTER_NAME, "event", "detector_failed", "surface", "ai_agent")
            .count()).isEqualTo(1.0);
    }

    @Test
    void testRecordToolArgsRestoredDelegatesToToolArgsRestoredEventUnderThisInstancesSurface() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

        AiGuardrailMetrics metrics = new AiGuardrailMetrics(meterRegistry, "ai_agent");

        metrics.recordToolArgsRestored();

        assertThat(meterRegistry.counter(
            AiGuardrailMetrics.COUNTER_NAME, "event", "tool_args_restored", "surface", "ai_agent")
            .count()).isEqualTo(1.0);
    }

    @Test
    void testRecordToolResultTokenizedDelegatesToToolResultTokenizedEventUnderThisInstancesSurface() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

        AiGuardrailMetrics metrics = new AiGuardrailMetrics(meterRegistry, "ai_agent");

        metrics.recordToolResultTokenized();

        assertThat(meterRegistry.counter(
            AiGuardrailMetrics.COUNTER_NAME, "event", "tool_result_tokenized", "surface", "ai_agent")
            .count()).isEqualTo(1.0);
    }

    @Test
    void testRecordTokenUnresolvedDelegatesToTokenUnresolvedEventUnderThisInstancesSurface() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

        AiGuardrailMetrics metrics = new AiGuardrailMetrics(meterRegistry, "ai_agent");

        metrics.recordTokenUnresolved();

        assertThat(meterRegistry.counter(
            AiGuardrailMetrics.COUNTER_NAME, "event", "token_unresolved", "surface", "ai_agent")
            .count()).isEqualTo(1.0);
    }

    @Test
    void testRecordAssistantHistoryRetokenizedDelegatesToAssistantHistoryRetokenizedEventUnderThisInstancesSurface() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

        AiGuardrailMetrics metrics = new AiGuardrailMetrics(meterRegistry, "ai_agent");

        metrics.recordAssistantHistoryRetokenized();

        assertThat(meterRegistry.counter(
            AiGuardrailMetrics.COUNTER_NAME, "event", "assistant_history_retokenized", "surface", "ai_agent")
            .count()).isEqualTo(1.0);
    }

    @Test
    void testEverySensitiveDataMetricsEventIsImplementedRatherThanLeftAsANoOpDefault() {
        List<String> unimplemented = new ArrayList<>();

        for (Method seamMethod : SensitiveDataMetrics.class.getDeclaredMethods()) {
            if (seamMethod.isSynthetic()) {
                continue;
            }

            try {
                Method override =
                    AiGuardrailMetrics.class.getDeclaredMethod(seamMethod.getName(), seamMethod.getParameterTypes());

                if (override.isDefault()) {
                    unimplemented.add(seamMethod.getName());
                }
            } catch (NoSuchMethodException noSuchMethodException) {
                unimplemented.add(seamMethod.getName());
            }
        }

        assertThat(unimplemented)
            .as("AiGuardrailMetrics inherits SensitiveDataMetrics' no-op default for these events, so they are "
                + "recorded nowhere in production. The interface's defaults exist to keep test doubles as single "
                + "method references, not to make the real implementation optional -- give each one a body that "
                + "records through record(String), or delete it from the seam.")
            .isEmpty();
    }
}
