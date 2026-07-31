/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.guardrails.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.platform.ai.guardrails.domain.AiGuardrailsWorkspaceSettings.BlockingMode;
import org.junit.jupiter.api.Test;

/**
 * Pins {@link BlockingMode}'s ordinals. The value is currently persisted by {@link BlockingMode#name()}, not ordinal,
 * but this test keeps the enum append-only regardless, matching every other persisted enum in this codebase and
 * guarding against a future switch to ordinal-based storage.
 *
 * @version ee
 */
class BlockingModeStabilityTest {

    @Test
    void testBlockingModeOrdinalsArePinned() {
        assertThat(BlockingMode.BLOCK.ordinal()).isEqualTo(0);
        assertThat(BlockingMode.REDACT_AND_CONTINUE.ordinal()).isEqualTo(1);
        assertThat(BlockingMode.values()).hasSize(2);
    }
}
