/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.otlp.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 * @version ee
 */
class OtelGenAiSpanTest {

    private static final String TRACE_ID = "0123456789abcdef0123456789abcdef";
    private static final String SPAN_ID = "0123456789abcdef";
    private static final OtelResourceAttributes EMPTY_RESOURCE = OtelResourceAttributes.empty();
    private static final OtelSpanAttributes EMPTY_SPAN_ATTRS = OtelSpanAttributes.empty();

    @Test
    void testDurationComputedFromStartAndEnd() {
        Instant start = Instant.parse("2026-04-22T10:00:00Z");
        Instant end = Instant.parse("2026-04-22T10:00:01Z");

        OtelGenAiSpan span = OtelGenAiSpan.ofHex(
            TRACE_ID, SPAN_ID, null, "chat", start, end,
            OtelSpanStatus.OK, OtelSpanAttributes.of(Map.of("gen_ai.system", "openai")),
            EMPTY_RESOURCE, null, null);

        assertThat(span.durationMs()).isEqualTo(1000L);
    }

    @Test
    void testRejectsEndBeforeStart() {
        Instant start = Instant.parse("2026-04-22T10:00:01Z");
        Instant end = Instant.parse("2026-04-22T10:00:00Z");

        assertThatThrownBy(() -> OtelGenAiSpan.ofHex(
            TRACE_ID, SPAN_ID, null, "chat", start, end,
            OtelSpanStatus.OK, EMPTY_SPAN_ATTRS, EMPTY_RESOURCE, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("endTime");
    }

    @Test
    void testRejectsBlankTraceId() {
        assertThatThrownBy(() -> OtelGenAiSpan.ofHex(
            "  ", SPAN_ID, null, "chat", Instant.now(), Instant.now(),
            OtelSpanStatus.OK, EMPTY_SPAN_ATTRS, EMPTY_RESOURCE, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("traceId");
    }

    @Test
    void testRejectsNonHexTraceId() {
        assertThatThrownBy(() -> OtelGenAiSpan.ofHex(
            "0123456789abcdef0123456789abcdeg", SPAN_ID, null, "chat", Instant.now(), Instant.now(),
            OtelSpanStatus.OK, EMPTY_SPAN_ATTRS, EMPTY_RESOURCE, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("traceId");
    }

    @Test
    void testRejectsTraceIdWrongLength() {
        assertThatThrownBy(() -> OtelGenAiSpan.ofHex(
            "0123456789abcdef", SPAN_ID, null, "chat", Instant.now(), Instant.now(),
            OtelSpanStatus.OK, EMPTY_SPAN_ATTRS, EMPTY_RESOURCE, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("traceId");
    }

    @Test
    void testRejectsUppercaseHex() {
        assertThatThrownBy(() -> OtelGenAiSpan.ofHex(
            "0123456789ABCDEF0123456789ABCDEF", SPAN_ID, null, "chat", Instant.now(), Instant.now(),
            OtelSpanStatus.OK, EMPTY_SPAN_ATTRS, EMPTY_RESOURCE, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("traceId");
    }

    @Test
    void testRejectsAllZeroTraceIdSentinel() {
        assertThatThrownBy(() -> OtelGenAiSpan.ofHex(
            "00000000000000000000000000000000", SPAN_ID, null, "chat", Instant.now(), Instant.now(),
            OtelSpanStatus.OK, EMPTY_SPAN_ATTRS, EMPTY_RESOURCE, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("invalid-id sentinel");
    }

    @Test
    void testRejectsAllZeroSpanIdSentinel() {
        assertThatThrownBy(() -> OtelGenAiSpan.ofHex(
            TRACE_ID, "0000000000000000", null, "chat", Instant.now(), Instant.now(),
            OtelSpanStatus.OK, EMPTY_SPAN_ATTRS, EMPTY_RESOURCE, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("invalid-id sentinel");
    }

    @Test
    void testNormalizesEmptyParentSpanIdToNull() {
        OtelGenAiSpan empty = OtelGenAiSpan.ofHex(
            TRACE_ID, SPAN_ID, "", "chat", Instant.now(), Instant.now(), OtelSpanStatus.OK, EMPTY_SPAN_ATTRS,
            EMPTY_RESOURCE, null, null);

        OtelGenAiSpan nullParent = OtelGenAiSpan.ofHex(
            TRACE_ID, SPAN_ID, null, "chat", Instant.now(), Instant.now(), OtelSpanStatus.OK, EMPTY_SPAN_ATTRS,
            EMPTY_RESOURCE, null, null);

        OtelGenAiSpan whitespaceParent = OtelGenAiSpan.ofHex(
            TRACE_ID, SPAN_ID, "   ", "chat", Instant.now(), Instant.now(), OtelSpanStatus.OK, EMPTY_SPAN_ATTRS,
            EMPTY_RESOURCE, null, null);

        assertThat(empty.parentSpanId()).isNull();
        assertThat(empty.parentSpanIdHex()).isNull();
        assertThat(nullParent.parentSpanId()).isNull();
        assertThat(whitespaceParent.parentSpanId()).isNull();
    }

    @Test
    void testTypedAccessorsRoundTripHex() {
        String parentSpanIdHex = "fedcba9876543210";

        OtelGenAiSpan span = OtelGenAiSpan.ofHex(
            TRACE_ID, SPAN_ID, parentSpanIdHex, "chat", Instant.now(), Instant.now(), OtelSpanStatus.OK,
            EMPTY_SPAN_ATTRS, EMPTY_RESOURCE, null, null);

        assertThat(span.traceId()
            .hex()).isEqualTo(TRACE_ID);
        assertThat(span.spanId()
            .hex()).isEqualTo(SPAN_ID);
        assertThat(span.traceIdHex()).isEqualTo(TRACE_ID);
        assertThat(span.spanIdHex()).isEqualTo(SPAN_ID);
        assertThat(span.parentSpanIdHex()).isEqualTo(parentSpanIdHex);
    }

    @Test
    void testRejectsSelfParenting() {
        assertThatThrownBy(() -> OtelGenAiSpan.ofHex(
            TRACE_ID, SPAN_ID, SPAN_ID, "chat", Instant.now(), Instant.now(), OtelSpanStatus.OK, EMPTY_SPAN_ATTRS,
            EMPTY_RESOURCE, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("self-parenting forbidden");
    }

    @Test
    void testReadIntAttrRejectsCollectionWithoutLeakingContent() {
        OtelGenAiSpan span = OtelGenAiSpan.ofHex(
            TRACE_ID, SPAN_ID, null, "chat", Instant.now(), Instant.now(), OtelSpanStatus.OK,
            OtelSpanAttributes.of(Map.of("gen_ai.usage.input_tokens", List.of(1, 2, 3))),
            EMPTY_RESOURCE, null, null);

        assertThatThrownBy(span::inputTokensAttr)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("gen_ai.usage.input_tokens")
            .hasMessageContaining("must be a number")
            .hasMessageNotContaining("[1, 2, 3]");
    }

    @Test
    void testReadIntAttrRejectsBooleanWithoutLeakingValue() {
        OtelGenAiSpan span = OtelGenAiSpan.ofHex(
            TRACE_ID, SPAN_ID, null, "chat", Instant.now(), Instant.now(), OtelSpanStatus.OK,
            OtelSpanAttributes.of(Map.of("gen_ai.usage.input_tokens", true)),
            EMPTY_RESOURCE, null, null);

        assertThatThrownBy(span::inputTokensAttr)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must be a number");
    }

    @Test
    void testReadIntAttrRejectsValueAboveIntegerMaxValue() {
        long tooBig = (long) Integer.MAX_VALUE + 1L;

        OtelGenAiSpan span = OtelGenAiSpan.ofHex(
            TRACE_ID, SPAN_ID, null, "chat", Instant.now(), Instant.now(), OtelSpanStatus.OK,
            OtelSpanAttributes.of(Map.of("gen_ai.usage.input_tokens", tooBig)),
            EMPTY_RESOURCE, null, null);

        assertThatThrownBy(span::inputTokensAttr)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("outside the supported int range");
    }

    @Test
    void testReadIntAttrAcceptsLongWithinRange() {
        long withinRange = 12_345L;

        OtelGenAiSpan span = OtelGenAiSpan.ofHex(
            TRACE_ID, SPAN_ID, null, "chat", Instant.now(), Instant.now(), OtelSpanStatus.OK,
            OtelSpanAttributes.of(Map.of("gen_ai.usage.input_tokens", withinRange)),
            EMPTY_RESOURCE, null, null);

        assertThat(span.inputTokensAttr()).isEqualTo(12_345);
    }

    @Test
    void testSpanKindAttrReadsOpenInferenceKind() {
        OtelGenAiSpan span = spanWithAttributes(Map.of("openinference.span.kind", "RETRIEVER"));

        assertThat(span.spanKindAttr()).isEqualTo("RETRIEVER");
    }

    @Test
    void testSpanKindAttrNullWhenAbsent() {
        OtelGenAiSpan span = spanWithAttributes(Map.of());

        assertThat(span.spanKindAttr()).isNull();
    }

    private static OtelGenAiSpan spanWithAttributes(Map<String, Object> attributes) {
        return OtelGenAiSpan.ofHex(
            TRACE_ID, SPAN_ID, null, "chat", Instant.now(), Instant.now(), OtelSpanStatus.OK,
            OtelSpanAttributes.of(attributes), EMPTY_RESOURCE, null, null);
    }
}
