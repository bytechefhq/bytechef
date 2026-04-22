/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.otlp.dto;

/**
 * Canonical OTel trace-id value type — exactly 32 lowercase hex characters (16 protobuf bytes). The validation
 * predicate is shared with {@link OtelSpanId} via {@link OtelHexIds} so a future regex tweak applies to both in
 * lock-step.
 *
 * @author Ivica Cardic
 * @version ee
 */
public record OtelTraceId(String hex) {

    /** 16 protobuf bytes = 32 hex characters per the OTel spec. */
    public static final int HEX_LENGTH = 32;

    public OtelTraceId {
        validate("traceId", hex);
    }

    public static OtelTraceId of(String hex) {
        return new OtelTraceId(hex);
    }

    /**
     * Validates {@code value} as canonical lowercase hex of {@link #HEX_LENGTH} characters. Public so the OTLP mapper
     * can reuse the same predicate when staging a span before record allocation. Throws
     * {@link IllegalArgumentException} on failure with diagnostic context.
     */
    public static void validate(String fieldName, String value) {
        OtelHexIds.validate(fieldName, value, HEX_LENGTH);
    }
}
