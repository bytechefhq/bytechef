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

        if (parentSpanId != null && parentSpanId.equals(spanId)) {
            throw new IllegalArgumentException(
                "parentSpanId must not equal spanId (self-parenting forbidden by OTLP spec): " + spanId.hex());
        }
    }

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

        if (!(value instanceof Number number)) {
            throw new IllegalArgumentException(
                key + " must be a number; got " + value.getClass()
                    .getSimpleName());
        }

        long longValue = number.longValue();

        if (longValue > Integer.MAX_VALUE || longValue < Integer.MIN_VALUE) {
            throw new IllegalArgumentException(
                key + " value " + longValue + " is outside the supported int range");
        }

        return (int) longValue;
    }
}
