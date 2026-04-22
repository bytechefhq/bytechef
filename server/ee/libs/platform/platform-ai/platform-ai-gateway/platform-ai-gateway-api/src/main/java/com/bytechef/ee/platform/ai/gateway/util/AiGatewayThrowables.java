/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.util;

/**
 * Shared utilities for rendering {@link Throwable}s into the gateway's per-row response payloads. Centralizing the
 * format here so that the same root cause surfaces with the same shape across the gateway's facades — operators reading
 * a rejection reason should never have to remember "this facade strips the class name and that one prefixes it."
 *
 * @author Ivica Cardic
 * @version ee
 */
public final class AiGatewayThrowables {

    /**
     * Default cap on the rendered string length when the caller has no column-specific limit. Persisted into
     * {@code AiEvalExecution.errorMessage}, {@code AiExperimentRun.errorMessage}, OTLP rejection reasons, etc. — bound
     * the size so a runaway cause chain (e.g., a deeply wrapped Spring {@code DataAccessException} with vendor-specific
     * SQL state) cannot bloat a row past the column limit and trigger a secondary persist failure.
     *
     * <p>
     * Callers that persist into shorter columns should use {@link #summarize(Throwable, int)} with their own column cap
     * rather than relying on this default — for example, OTLP detail rows that fan out to a per-row diagnostic column
     * may want a much tighter limit so a 50-row batch's combined detail payload stays under the response cap.
     */
    public static final int DEFAULT_MAX_RENDERED_LENGTH = 1024;

    private AiGatewayThrowables() {
    }

    /**
     * Renders a {@link Throwable} as {@code SimpleName: message}, walking the cause chain and appending the most
     * specific cause when the wrapper carries no message of its own. Falls back to {@code SimpleName} alone when
     * neither the wrapper nor its causes have a message. The result is truncated to
     * {@link #DEFAULT_MAX_RENDERED_LENGTH}.
     *
     * <p>
     * The walk matters because gateway facades wrap heavily — {@code IllegalStateException("Failed to parse
     * dataset item ...", cause)}, {@code RuntimeException("Failed to compute HMAC", cause)}, the
     * {@code DataAccessException} hierarchy with rich vendor messages on the cause. A wrapper-only render persists
     * literal {@code "RuntimeException"} into operator-facing rows when the wrapper was constructed via
     * {@code new RuntimeException(cause)}, leaving zero diagnostic content. Walking to the deepest non-empty message
     * keeps the persisted row useful while preserving the wrapper's class name as the prefix.
     */
    public static String summarize(Throwable throwable) {
        return summarize(throwable, DEFAULT_MAX_RENDERED_LENGTH);
    }

    /**
     * Renders a {@link Throwable} with a caller-supplied length cap, for callers persisting into a column whose limit
     * differs from {@link #DEFAULT_MAX_RENDERED_LENGTH}. The truncation rule is identical to
     * {@link #summarize(Throwable)}; the only difference is the upper bound.
     *
     * @param throwable the throwable to render
     * @param maxLength hard ceiling on the returned string's length; must be {@code > 0}
     * @throws IllegalArgumentException if {@code maxLength} is {@code <= 0}
     */
    public static String summarize(Throwable throwable, int maxLength) {
        if (maxLength <= 0) {
            throw new IllegalArgumentException("maxLength must be > 0; got " + maxLength);
        }

        String name = throwable.getClass()
            .getSimpleName();
        String message = mostSpecificMessage(throwable);

        String rendered = message == null ? name : name + ": " + message;

        return rendered.length() <= maxLength
            ? rendered
            : rendered.substring(0, maxLength);
    }

    private static String mostSpecificMessage(Throwable throwable) {
        // Bounded walk: 6 levels is enough for any real exception chain and stops dead on a pathological
        // cycle (Spring AOP proxies and reflection wrappers occasionally produce A -> B -> A loops).
        Throwable current = throwable;

        for (int depth = 0; depth < 6 && current != null; depth++) {
            String message = current.getMessage();

            if (message != null && !message.isBlank()) {
                if (current == throwable) {
                    return message;
                }

                return message + " (root cause: " + current.getClass()
                    .getSimpleName() + ")";
            }

            Throwable next = current.getCause();

            if (next == current) {
                break;
            }

            current = next;
        }

        return null;
    }
}
