/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.exception;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class SafeMessage {

    private static final Pattern UUID_LIKE =
        Pattern.compile("\\b[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\\b");

    private static final Pattern LONG_HEX = Pattern.compile("\\b[0-9a-fA-F]{16,}\\b");

    private static final Pattern LONG_DECIMAL = Pattern.compile("\\b\\d{9,}\\b");

    private static final Pattern INTERNAL_PREFIX = Pattern.compile(
        "(?i)\\b(workspace_id|tenant_id|account_id|user_id|trace_id|span_id|connection_id)\\b");

    private static final Pattern SQL_SHAPE =
        Pattern.compile("(?i)\\b(select|insert|update|delete|where|join|from)\\b\\s+\\w+");

    private static final Pattern EXCEPTION_CLASS =
        Pattern.compile("\\b\\w+(Exception|Error)(?:\\s*:|$)");

    private final String value;

    private SafeMessage(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("safe message must not be blank");
        }

        this.value = value;
    }

    public String value() {
        return value;
    }

    public static SafeMessage of(String message) {
        if (message != null && containsLikelyInternalContext(message)) {
            throw new IllegalArgumentException(
                "SafeMessage.of(...) rejected a message that looks like it carries internal context: \""
                    + message + "\"; use SafeMessage.raw(...) explicitly if you intend to surface this to the client");
        }

        return new SafeMessage(message);
    }

    public static SafeMessage raw(String message) {
        return new SafeMessage(message);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof SafeMessage other)) {
            return false;
        }

        return Objects.equals(value, other.value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }

    @Override
    public String toString() {
        return "SafeMessage[value=" + value + "]";
    }

    private static boolean containsLikelyInternalContext(String message) {
        return UUID_LIKE.matcher(message)
            .find()
            || LONG_HEX.matcher(message)
                .find()
            || LONG_DECIMAL.matcher(message)
                .find()
            || INTERNAL_PREFIX.matcher(message)
                .find()
            || SQL_SHAPE.matcher(message)
                .find()
            || EXCEPTION_CLASS.matcher(message)
                .find();
    }
}
