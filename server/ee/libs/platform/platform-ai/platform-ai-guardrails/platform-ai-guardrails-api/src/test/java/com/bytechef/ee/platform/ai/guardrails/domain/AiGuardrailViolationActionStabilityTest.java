/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.guardrails.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class AiGuardrailViolationActionStabilityTest {

    @Test
    void testActionOrdinalsArePinned() {
        assertThat(AiGuardrailViolationAction.BLOCKED.ordinal()).isEqualTo(0);
        assertThat(AiGuardrailViolationAction.REDACTED.ordinal()).isEqualTo(1);
        assertThat(AiGuardrailViolationAction.ALLOWED.ordinal()).isEqualTo(2);
        assertThat(AiGuardrailViolationAction.values()).hasSize(3);
    }
}
