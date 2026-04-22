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
 * Direct coverage of {@link OtelTraceId} construction. The validation is also exercised transitively via
 * {@link OtelGenAiSpanTest}, but a dedicated test pins the invariants of the type itself so a refactor that
 * accidentally moves validation off the constructor is caught.
 *
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
        // Catches a regression that mixes up the OtelTraceId / OtelSpanId length constants.
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
        // 16 chars — span-id length, not trace-id length. This is the swap a future refactor might
        // introduce; the type system can't catch the cross-call swap so the runtime check must.
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
        // OTel spec defines the all-zero id as "invalid id". Accepting it would let a zeroed protobuf bytes
        // field slip through validation as if it were a real id; downstream lookups would then collapse every
        // "invalid" span across workspaces onto the same row.
        assertThatThrownBy(() -> OtelTraceId.of("00000000000000000000000000000000"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("invalid-id sentinel");
    }
}
