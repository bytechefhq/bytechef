/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.otlp.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * Direct coverage of {@link OtelSpanId} construction. Mirrors {@link OtelTraceIdTest} so the two id types stay in
 * lock-step under future refactors.
 *
 * @author Ivica Cardic
 * @version ee
 */
class OtelSpanIdTest {

    @Test
    void testAcceptsCanonical16CharLowercaseHex() {
        OtelSpanId spanId = OtelSpanId.of("0123456789abcdef");

        assertThat(spanId.hex()).isEqualTo("0123456789abcdef");
    }

    @Test
    void testHexLengthIs16() {
        // Catches a regression that mixes up the OtelTraceId / OtelSpanId length constants.
        assertThat(OtelSpanId.HEX_LENGTH).isEqualTo(16);
    }

    @Test
    void testRejectsBlank() {
        assertThatThrownBy(() -> OtelSpanId.of("   "))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("spanId")
            .hasMessageContaining("must not be blank");
    }

    @Test
    void testRejectsNull() {
        assertThatThrownBy(() -> OtelSpanId.of(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("spanId");
    }

    @Test
    void testRejectsTraceIdLength() {
        // 32 chars — trace-id length, not span-id length. Direct symmetry with OtelTraceIdTest's wrong-length
        // case so a refactor that swaps the HEX_LENGTH constants is caught from both sides.
        assertThatThrownBy(() -> OtelSpanId.of("0123456789abcdef0123456789abcdef"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("spanId")
            .hasMessageContaining("16");
    }

    @Test
    void testRejectsUppercaseHex() {
        assertThatThrownBy(() -> OtelSpanId.of("0123456789ABCDEF"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("lowercase hex");
    }

    @Test
    void testRejectsNonHexCharacter() {
        assertThatThrownBy(() -> OtelSpanId.of("0123456789abcdex"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("lowercase hex");
    }

    @Test
    void testRejectsAllZeroSentinel() {
        assertThatThrownBy(() -> OtelSpanId.of("0000000000000000"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("invalid-id sentinel");
    }
}
