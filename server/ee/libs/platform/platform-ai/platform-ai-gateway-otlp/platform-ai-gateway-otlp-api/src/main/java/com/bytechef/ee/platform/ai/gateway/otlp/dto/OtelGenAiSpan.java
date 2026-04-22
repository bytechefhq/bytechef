/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.otlp.dto;

import java.time.Duration;
import java.time.Instant;
import org.apache.commons.lang3.Validate;

/**
 * Flattened view of one OTel span after {@code gen_ai.*} semantic-convention attributes are extracted. Upstream
 * converters (from protobuf or JSON) produce this record; downstream facades turn it into persistable entities.
 *
 * <p>
 * Trace and span ids are typed value classes ({@link OtelTraceId} / {@link OtelSpanId}) so the type system prevents
 * trace-vs-span mixups across the pipeline. Hex strings for SQL parameter binding are available via
 * {@link #traceIdHex()} / {@link #spanIdHex()} / {@link #parentSpanIdHex()} — explicit hex extraction makes the
 * boundary between typed handling and raw-string persistence visible at every call site.
 *
 * <p>
 * Per-span and resource attributes are wrapped in distinct value types ({@link OtelSpanAttributes} /
 * {@link OtelResourceAttributes}). Both wrappers carry a {@code Map<String, Object>} internally, but distinct types
 * make a positional swap at the canonical constructor a compile-time error rather than silent data corruption.
 *
 * @author Ivica Cardic
 * @version ee
 */
public record OtelGenAiSpan(
    OtelTraceId traceId,
    OtelSpanId spanId,
    OtelSpanId parentSpanId,
    String name,
    Instant startTime,
    Instant endTime,
    OtelSpanStatus status,
    OtelSpanAttributes attributes,
    OtelResourceAttributes resourceAttributes,
    String inputBody,
    String outputBody) {

    public OtelGenAiSpan {
        Validate.notNull(traceId, "traceId must not be null");
        Validate.notNull(spanId, "spanId must not be null");
        Validate.notNull(startTime, "startTime must not be null");
        Validate.notNull(endTime, "endTime must not be null");
        Validate.notNull(status, "status must not be null");
        Validate.notNull(attributes, "attributes must not be null");
        Validate.notNull(resourceAttributes, "resourceAttributes must not be null");

        if (endTime.isBefore(startTime)) {
            throw new IllegalArgumentException(
                "endTime (" + endTime + ") must not be before startTime (" + startTime + ")");
        }

        // The OTLP spec forbids self-parenting; a span declaring itself as its own parent would cause
        // downstream graph traversal to infinite-loop. Reject at the type boundary so a bug in the upstream
        // exporter cannot poison the trace tree post-persist.
        if (parentSpanId != null && parentSpanId.equals(spanId)) {
            throw new IllegalArgumentException(
                "parentSpanId must not equal spanId (self-parenting forbidden by OTLP spec): " + spanId.hex());
        }
    }

    /**
     * Convenience factory for callers staging from raw hex strings. {@code parentHex} accepts {@code null} or any blank
     * string for "no parent" — all such forms are normalized to a {@code null} {@link OtelSpanId} so the record never
     * carries two valid representations of the same state.
     */
    public static OtelGenAiSpan ofHex(
        String traceHex, String spanHex, String parentHex, String name, Instant startTime, Instant endTime,
        OtelSpanStatus status, OtelSpanAttributes attributes, OtelResourceAttributes resourceAttributes,
        String inputBody, String outputBody) {

        OtelSpanId parentSpanId = (parentHex == null || parentHex.isBlank()) ? null : OtelSpanId.of(parentHex);

        return new OtelGenAiSpan(
            OtelTraceId.of(traceHex), OtelSpanId.of(spanHex), parentSpanId, name, startTime, endTime, status,
            attributes, resourceAttributes, inputBody, outputBody);
    }

    public String traceIdHex() {
        return traceId.hex();
    }

    public String spanIdHex() {
        return spanId.hex();
    }

    public String parentSpanIdHex() {
        return parentSpanId == null ? null : parentSpanId.hex();
    }

    public long durationMs() {
        return Duration.between(startTime, endTime)
            .toMillis();
    }

    public String systemAttr() {
        return readStringAttr("gen_ai.system");
    }

    public String spanKindAttr() {
        return readStringAttr("openinference.span.kind");
    }

    public String requestModelAttr() {
        return readStringAttr("gen_ai.request.model");
    }

    public String responseModelAttr() {
        return readStringAttr("gen_ai.response.model");
    }

    /**
     * Reads a {@link CharSequence}-typed attribute by key. Returns {@code null} if the attribute is absent OR carries a
     * non-CharSequence value (e.g. an exporter mistakenly emits the model identifier as a one-element
     * {@code ARRAY_VALUE} — {@code ["gpt-4o"]}). The previous {@code String.valueOf(value)} path would have stringified
     * such input as {@code "[gpt-4o]"} and persisted that synthetic string into {@code AiObservabilitySpan.model},
     * producing a permanent {@code MODEL_NOT_REGISTERED} miss that operators could not correlate back to a malformed
     * exporter. Returning {@code null} on type mismatch routes the span through the existing {@code REJECTED_NO_SYSTEM}
     * / {@code MODEL_IDENTIFIER_MISSING} buckets — both already alerted-on — instead of silently corrupting downstream
     * lookups.
     */
    private String readStringAttr(String key) {
        Object value = attributes.get(key);

        if (value instanceof CharSequence charSequence) {
            return charSequence.toString();
        }

        return null;
    }

    public Integer inputTokensAttr() {
        return readIntAttr("gen_ai.usage.input_tokens");
    }

    public Integer outputTokensAttr() {
        return readIntAttr("gen_ai.usage.output_tokens");
    }

    private Integer readIntAttr(String key) {
        Object value = attributes.get(key);

        if (value == null) {
            return null;
        }

        // Accept only Number subtypes. Boolean / Character / Iterable / Map / arbitrary Object would all fall
        // through to Integer.parseInt(String.valueOf(...)), embedding the caller-supplied content into the
        // resulting NumberFormatException message — which the ingest facade then surfaces in the OTLP
        // rejection reason returned to the client. The fixed-shape error message keeps the wire response
        // stable regardless of input type.
        if (!(value instanceof Number number)) {
            throw new IllegalArgumentException(
                key + " must be a number; got " + value.getClass()
                    .getSimpleName());
        }

        long longValue = number.longValue();

        // Per-span token counts cannot legitimately exceed Integer.MAX_VALUE (~2.1B tokens in a single LLM
        // call would correspond to thousands of dollars at any commercial provider's rate). A value beyond
        // the int range almost certainly indicates a misencoded attribute (uint64 wraparound, or a metric
        // counter mistakenly stamped onto a span). Reject explicitly rather than silently truncating, which
        // would persist a bogus int and mis-attribute cost on every dashboard.
        if (longValue > Integer.MAX_VALUE || longValue < Integer.MIN_VALUE) {
            throw new IllegalArgumentException(
                key + " value " + longValue + " is outside the supported int range");
        }

        return (int) longValue;
    }
}
