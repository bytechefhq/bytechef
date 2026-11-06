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
 * @author Ivica Cardic
 * @version ee
 */
final class OtelHexIds {

    private static final Pattern HEX_PATTERN = Pattern.compile("^[0-9a-f]+$");

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
