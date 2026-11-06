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
 * @author Ivica Cardic
 * @version ee
 */
class OtelTraceIdTest {

    @Test
    void testAcceptsCanonical32CharLowercaseHex() {
        OtelTraceId traceId = OtelTraceId.of("0123456789abcdef0123456789abcdef");

        assertThat(traceId.hex()).isEqualTo("0123456789abcdef0123456789abcdef");
    }

    @Test
    void testHexLengthIs32() {
        assertThat(OtelTraceId.HEX_LENGTH).isEqualTo(32);
    }

    @Test
    void testRejectsBlank() {
        assertThatThrownBy(() -> OtelTraceId.of("   "))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("traceId")
            .hasMessageContaining("must not be blank");
    }

    @Test
    void testRejectsNull() {
        assertThatThrownBy(() -> OtelTraceId.of(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("traceId");
    }

    @Test
    void testRejectsWrongLengthShort() {
        assertThatThrownBy(() -> OtelTraceId.of("0123456789abcdef"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("traceId")
            .hasMessageContaining("32");
    }

    @Test
    void testRejectsWrongLengthLong() {
        assertThatThrownBy(() -> OtelTraceId.of("0123456789abcdef0123456789abcdef00"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("traceId");
    }

    @Test
    void testRejectsUppercaseHex() {
        assertThatThrownBy(() -> OtelTraceId.of("0123456789ABCDEF0123456789ABCDEF"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("lowercase hex");
    }

    @Test
    void testRejectsNonHexCharacter() {
        assertThatThrownBy(() -> OtelTraceId.of("0123456789abcdef0123456789abcdeg"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("lowercase hex");
    }

    @Test
    void testRejectsAllZeroSentinel() {
        assertThatThrownBy(() -> OtelTraceId.of("00000000000000000000000000000000"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("invalid-id sentinel");
    }
}
