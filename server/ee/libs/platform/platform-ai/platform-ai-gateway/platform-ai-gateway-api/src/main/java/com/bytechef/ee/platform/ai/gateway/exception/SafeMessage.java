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
 * Wire-safe HTTP response message. The value type forces a deliberate decision at the throw site: free-form
 * {@code String} arguments cannot be implicitly passed as the safe payload of {@link BadRequestException}; the caller
 * has to wrap via {@link #of(String)} or {@link #raw(String)}, which is the syntactic affordance reminding the author
 * "this string will reach the client."
 *
 * <p>
 * {@link #of(String)} runs a defense-in-depth heuristic that rejects strings containing common internal-context markers
 * (UUIDs, decimal/hex ids over 8 digits, the {@code workspace_}/{@code tenant_}/{@code account_} prefixes, SQL-shaped
 * fragments, raw exception class names). A genuine ID-bearing message that needs to reach the client (e.g., a UUID
 * public reference) must use {@link #raw(String)} explicitly — the call site then makes the override visible to
 * reviewers. The two factories differ only in this defensive screen.
 *
 * <p>
 * Implemented as a {@code final class} with a {@code private} constructor rather than a {@code record} so the canonical
 * constructor cannot be invoked directly: {@code new SafeMessage("workspace_id=12345")} would have structurally
 * defeated the {@link #of(String)} screening contract. Callers must go through one of the two factories, and the
 * deliberate-decision affordance the type was introduced to enforce holds at compile time.
 *
 * @author Ivica Cardic
 * @version ee
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

    /**
     * Strict factory: rejects messages that look like they leak internal context. Use this at every throw site unless
     * you have a documented reason to surface an ID via {@link #raw(String)}.
     */
    public static SafeMessage of(String message) {
        if (message != null && containsLikelyInternalContext(message)) {
            throw new IllegalArgumentException(
                "SafeMessage.of(...) rejected a message that looks like it carries internal context: \""
                    + message + "\"; use SafeMessage.raw(...) explicitly if you intend to surface this to the client");
        }

        return new SafeMessage(message);
    }

    /**
     * Escape hatch for messages that legitimately surface a public id (e.g., a tenant-visible UUID). Uses an explicit
     * factory name so the override is visible at the throw site and to code reviewers — silently passing a String never
     * works.
     */
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
