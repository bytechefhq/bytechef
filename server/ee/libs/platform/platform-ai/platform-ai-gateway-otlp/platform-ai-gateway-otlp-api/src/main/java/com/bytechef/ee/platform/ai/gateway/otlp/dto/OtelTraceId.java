/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.otlp.dto;

/**
 * @author Ivica Cardic
 * @version ee
 */
public record OtelTraceId(String hex) {

    public static final int HEX_LENGTH = 32;

    public OtelTraceId {
        validate("traceId", hex);
    }

    public static OtelTraceId of(String hex) {
        return new OtelTraceId(hex);
    }

    public static void validate(String fieldName, String value) {
        OtelHexIds.validate(fieldName, value, HEX_LENGTH);
    }
}
