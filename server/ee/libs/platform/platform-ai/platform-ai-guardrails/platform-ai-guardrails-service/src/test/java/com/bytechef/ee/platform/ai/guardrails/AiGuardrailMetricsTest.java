/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.guardrails;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
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
}
