/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.util;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;

/**
 * Shared helpers for narrowing a {@link DuplicateKeyException} to a specific unique-constraint name.
 *
 * <p>
 * Prefers the structured {@code PSQLException#getServerErrorMessage().getConstraint()} accessor (locale-stable across
 * driver upgrades), accessed reflectively to keep the postgres driver out of the compile classpath. Falls back to
 * word-boundary matching on the exception message when the typed accessor is unavailable or returns no constraint.
 *
 * <p>
 * Word boundaries prevent a future index named with the constraint as a prefix (e.g. an index named
 * {@code <existing_index>_v2}) from silently matching the original — without anchors, a substring match would mis-route
 * a different schema violation as the same constraint hit and silently corrupt dedup bookkeeping.
 *
 * @author Ivica Cardic
 * @version ee
 */
public final class AiGatewayConstraintMatchers {

    private static final Logger log = LoggerFactory.getLogger(AiGatewayConstraintMatchers.class);

    // Cached reflection accessors for org.postgresql.util.PSQLException#getServerErrorMessage() and
    // org.postgresql.util.ServerErrorMessage#getConstraint(). The reflection probe runs on every dedup hit
    // (the dedup path is hot under retry storms); allocating Method objects each call would create GC churn.
    // Resolved lazily on first matching exception class so non-Postgres deployments pay nothing.
    private static final AtomicReference<Method> CACHED_GET_SERVER_ERROR_MESSAGE = new AtomicReference<>();
    private static final AtomicReference<Method> CACHED_GET_CONSTRAINT = new AtomicReference<>();

    private AiGatewayConstraintMatchers() {
    }

    /**
     * Builds a word-boundary {@link Pattern} for the supplied constraint name. Cache the result at call sites — pattern
     * compilation is allocation-heavy and the dedup hot path runs per-row under retry storms.
     */
    public static Pattern wordBoundaryPattern(String constraintName) {
        return Pattern.compile("\\b" + Pattern.quote(constraintName) + "\\b");
    }

    private static final int MAX_CAUSE_DEPTH = 6;

    /**
     * Returns true when {@code exception} arose from the supplied unique constraint. Walks the cause chain trying the
     * typed PSQLException accessor first, then falls back to word-boundary matching on the exception's own message and
     * its most-specific cause.
     *
     * <p>
     * The cause walk is bounded ({@value MAX_CAUSE_DEPTH} hops) and self-cycle-guarded — Spring's exception wrappers do
     * not normally cycle, but the rest of the gateway applies this discipline (see
     * {@link com.bytechef.ee.platform.ai.gateway.util.AiGatewayThrowables#summarize}); closing the asymmetry is cheap
     * and makes the helper safe under any wrapper chain a future Spring/JDBC upgrade introduces.
     */
    public static boolean matchesConstraint(
        DuplicateKeyException exception, String constraintName, Pattern wordBoundaryPattern) {

        Throwable cause = exception.getCause();
        int hops = 0;

        while (cause != null && hops < MAX_CAUSE_DEPTH) {
            if (matchesViaServerErrorMessage(cause, constraintName)) {
                return true;
            }

            Throwable next = cause.getCause();

            // Self-cycle guard: a malformed exception chain (cause points to itself, or to an earlier link via the
            // setCause API) would otherwise spin forever. Bail when next == cause OR when we revisit the head.
            if (next == cause || next == exception) {
                break;
            }

            cause = next;
            hops++;
        }

        if (containsConstraint(exception.getMessage(), wordBoundaryPattern)) {
            return true;
        }

        Throwable mostSpecificCause = exception.getMostSpecificCause();

        return mostSpecificCause != null && containsConstraint(mostSpecificCause.getMessage(), wordBoundaryPattern);
    }

    /**
     * Reflective probe for {@code PSQLException}'s {@code ServerErrorMessage#getConstraint()}. Returns false silently
     * when the runtime class is not Postgres or the structured field is empty — callers fall back to message-substring
     * matching. Reflection failures emit a TRACE-level breadcrumb so a future Postgres driver upgrade that renames the
     * accessor surfaces in diagnostic logs instead of being invisible. {@link Method} references are resolved once and
     * cached so the dedup hot-path does not allocate.
     */
    private static boolean matchesViaServerErrorMessage(Throwable cause, String constraintName) {
        if (!"org.postgresql.util.PSQLException".equals(cause.getClass()
            .getName())) {
            return false;
        }

        try {
            Method getServerErrorMessage = CACHED_GET_SERVER_ERROR_MESSAGE.get();

            if (getServerErrorMessage == null) {
                getServerErrorMessage = cause.getClass()
                    .getMethod("getServerErrorMessage");

                CACHED_GET_SERVER_ERROR_MESSAGE.compareAndSet(null, getServerErrorMessage);
            }

            Object serverErrorMessage = getServerErrorMessage.invoke(cause);

            if (serverErrorMessage == null) {
                return false;
            }

            Method getConstraint = CACHED_GET_CONSTRAINT.get();

            if (getConstraint == null) {
                getConstraint = serverErrorMessage.getClass()
                    .getMethod("getConstraint");

                CACHED_GET_CONSTRAINT.compareAndSet(null, getConstraint);
            }

            Object constraint = getConstraint.invoke(serverErrorMessage);

            return constraintName.equals(constraint);
        } catch (ReflectiveOperationException reflectionFailure) {
            log.atTrace()
                .setMessage(
                    "PSQLException structured-constraint probe failed; falling back to message match. " +
                        "If this fires repeatedly after a Postgres-driver upgrade, the getServerErrorMessage / " +
                        "getConstraint accessors have likely been renamed.")
                .setCause(reflectionFailure)
                .log();

            return false;
        }
    }

    private static boolean containsConstraint(String message, Pattern wordBoundaryPattern) {
        return message != null && wordBoundaryPattern.matcher(message)
            .find();
    }
}
