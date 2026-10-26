/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.guardrails.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * @version ee
 */
class AiGuardrailsWorkspaceSettingsTest {

    @Test
    void testNullMinConfidenceIsValid() {
        AiGuardrailsWorkspaceSettings settings = settingsWithMinConfidence(null);

        assertThat(settings.minConfidence()).isNull();
    }

    @Test
    void testMinConfidenceWithinRangeIsValid() {
        assertThat(settingsWithMinConfidence(0.0)
            .minConfidence()).isEqualTo(0.0);
        assertThat(settingsWithMinConfidence(0.6)
            .minConfidence()).isEqualTo(0.6);
        assertThat(settingsWithMinConfidence(1.0)
            .minConfidence()).isEqualTo(1.0);
    }

    @Test
    void testNegativeMinConfidenceIsRejected() {
        assertThatThrownBy(() -> settingsWithMinConfidence(-1.0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("minConfidence");
    }

    @Test
    void testAboveRangeMinConfidenceIsRejected() {
        assertThatThrownBy(() -> settingsWithMinConfidence(1.5))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("minConfidence");
    }

    @Test
    void testNanMinConfidenceIsRejected() {
        assertThatThrownBy(() -> settingsWithMinConfidence(Double.NaN))
            .isInstanceOf(IllegalArgumentException.class);
    }

    private static AiGuardrailsWorkspaceSettings settingsWithMinConfidence(Double minConfidence) {
        return new AiGuardrailsWorkspaceSettings(
            AiGuardrailsSettingsScope.WORKSPACE, 1L, null, null, null, null, null, null, null, minConfidence, null,
            null);
    }
}
