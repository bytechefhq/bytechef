/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.eval.dataset.util;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;

/**
 * Shared helpers for narrowing a {@link DuplicateKeyException} to a specific unique-constraint name. Mirrors
 * {@code AiGatewayConstraintMatchers} in automation-ai-gateway-api but lives in platform-ai-eval-dataset-service so the
 * platform module stays free of any automation/gateway compile-time dependency.
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
public final class AiEvalDatasetConstraintMatchers {

    private static final Logger log = LoggerFactory.getLogger(AiEvalDatasetConstraintMatchers.class);

    private static final AtomicReference<Method> CACHED_GET_SERVER_ERROR_MESSAGE = new AtomicReference<>();
    private static final AtomicReference<Method> CACHED_GET_CONSTRAINT = new AtomicReference<>();

    private static final int MAX_CAUSE_DEPTH = 6;

    private AiEvalDatasetConstraintMatchers() {
    }

    public static Pattern wordBoundaryPattern(String constraintName) {
        return Pattern.compile("\\b" + Pattern.quote(constraintName) + "\\b");
    }

    public static boolean matchesConstraint(
        DuplicateKeyException exception, String constraintName, Pattern wordBoundaryPattern) {

        Throwable cause = exception.getCause();
        int hops = 0;

        while (cause != null && hops < MAX_CAUSE_DEPTH) {
            if (matchesViaServerErrorMessage(cause, constraintName)) {
                return true;
            }

            Throwable next = cause.getCause();

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
