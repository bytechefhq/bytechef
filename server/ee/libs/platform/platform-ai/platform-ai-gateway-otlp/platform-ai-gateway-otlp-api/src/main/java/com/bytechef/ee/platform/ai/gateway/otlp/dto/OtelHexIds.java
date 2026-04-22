/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.otlp.dto;

import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;

/**
 * Shared validation predicate for {@link OtelTraceId} / {@link OtelSpanId}. Centralises blank / length / charset / OTel
 * "invalid-id" sentinel checks so a future tweak applies to both id types in lock-step (e.g., loosening to mixed-case
 * hex would otherwise require touching both classes).
 *
 * @author Ivica Cardic
 * @version ee
 */
final class OtelHexIds {

    /** Anchored so the whole value must consist of lowercase hex digits — no embedded whitespace, prefixes, etc. */
    private static final Pattern HEX_PATTERN = Pattern.compile("^[0-9a-f]+$");

    /** Maximum length of the rejected value embedded into an exception message; longer inputs are truncated. */
    private static final int MAX_VALUE_IN_MESSAGE = 64;

    private OtelHexIds() {
    }

    static void validate(String fieldName, String value, int expectedLength) {
        if (StringUtils.isBlank(value)) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }

        if (value.length() != expectedLength) {
            throw new IllegalArgumentException(
                fieldName + " must be exactly " + expectedLength + " lowercase hex characters; got "
                    + value.length() + " ('" + truncate(value) + "')");
        }

        if (!HEX_PATTERN.matcher(value)
            .matches()) {
            throw new IllegalArgumentException(
                fieldName + " must contain only lowercase hex digits [0-9a-f]; got '" + truncate(value) + "'");
        }

        // The OTel spec defines an all-zero id (00...00) as the "invalid id" sentinel. Accepting it would let
        // a zeroed protobuf bytes field — common when an exporter strips an attribute or fails to populate the
        // span context — slip through validation as if it were a real id, and downstream dedup / lookup
        // queries would silently coalesce every "invalid" span across workspaces onto the same row.
        if (isAllZero(value)) {
            throw new IllegalArgumentException(fieldName + " must not be the all-zero invalid-id sentinel");
        }
    }

    private static boolean isAllZero(String value) {
        for (int index = 0; index < value.length(); index++) {
            if (value.charAt(index) != '0') {
                return false;
            }
        }

        return true;
    }

    private static String truncate(String value) {
        if (value.length() <= MAX_VALUE_IN_MESSAGE) {
            return value;
        }

        return value.substring(0, MAX_VALUE_IN_MESSAGE) + "...";
    }
}
