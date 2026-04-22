/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.otlp.dto;

/**
 * Canonical OTel span-id value type — exactly 16 lowercase hex characters (8 protobuf bytes). The validation predicate
 * is shared with {@link OtelTraceId} via {@link OtelHexIds}.
 *
 * @author Ivica Cardic
 * @version ee
 */
public record OtelSpanId(String hex) {

    /** 8 protobuf bytes = 16 hex characters per the OTel spec. */
    public static final int HEX_LENGTH = 16;

    public OtelSpanId {
        validate("spanId", hex);
    }

    public static OtelSpanId of(String hex) {
        return new OtelSpanId(hex);
    }

    /**
     * Validates {@code value} as canonical lowercase hex of {@link #HEX_LENGTH} characters. Public so callers staging a
     * span before record allocation can reuse the same predicate.
     */
    public static void validate(String fieldName, String value) {
        OtelHexIds.validate(fieldName, value, HEX_LENGTH);
    }
}
