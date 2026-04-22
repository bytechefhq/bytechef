/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.dto;

import com.bytechef.ee.platform.ai.eval.domain.AiEvalScoreDataType;
import java.math.BigDecimal;

/**
 * Sealed tagged union of score values, produced by {@link AiExternalScoreRequest} / {@link AiExternalScoreBatchItem}
 * compact constructors via {@link #from(AiEvalScoreDataType, Object)}. The facade dispatches on this sealed type with
 * exhaustive {@code switch} so a future {@link AiEvalScoreDataType} variant added without a corresponding
 * {@code ScoreValue} permit fails at compile time.
 *
 * <p>
 * The wire format stays unchanged ({@code value: Object} + {@code dataType: enum}) for client compatibility; the typed
 * union exists strictly inside the JVM. Tolerant coercion rules (numeric strings as BOOLEAN, {@code 0}/{@code 1} as
 * BOOLEAN, etc.) live in the canonicalisation in {@link #from} so callers never see the raw {@code Object}.
 *
 * @author Ivica Cardic
 * @version ee
 */
public sealed interface ScoreValue permits ScoreValue.Numeric, ScoreValue.Bool, ScoreValue.Categorical {

    /**
     * Numeric score with a {@link BigDecimal} payload — chosen over {@code double} so caller-supplied precision (and
     * scale, for percentile-style scores) round-trips losslessly through Jackson.
     */
    record Numeric(BigDecimal value) implements ScoreValue {

        public Numeric {
            if (value == null) {
                throw new IllegalArgumentException("Numeric score value must not be null");
            }
        }
    }

    /** Boolean score (pass/fail-style metrics from human-eval harnesses). */
    record Bool(boolean value) implements ScoreValue {
    }

    /** Categorical score — caller-controlled label space (e.g. {@code "good"}/{@code "neutral"}/{@code "bad"}). */
    record Categorical(String value) implements ScoreValue {

        public Categorical {
            if (value == null) {
                throw new IllegalArgumentException("Categorical score value must not be null");
            }
        }
    }

    /**
     * Canonicalises a wire-shape {@code (dataType, rawValue)} pair into a {@link ScoreValue}. Throws
     * {@link IllegalArgumentException} for shape mismatches so the request DTO compact constructor surfaces the failure
     * at deserialisation time. Fold-in of the canonical wire-shape rules:
     *
     * <ul>
     * <li>{@code NUMERIC}: {@link Number} or numeric-parseable {@link String}.</li>
     * <li>{@code BOOLEAN}: {@link Boolean}, {@link Number} (signum != 0 → true), or {@code "true"}/{@code "false"}
     * (case-insensitive) / numeric String.</li>
     * <li>{@code CATEGORICAL}: any {@link String}.</li>
     * </ul>
     *
     * Caller-supplied content never appears in the rejection message — only the runtime class name — so a malicious
     * payload cannot leak via the 400 response body.
     */
    static ScoreValue from(AiEvalScoreDataType dataType, Object rawValue) {
        if (dataType == null) {
            throw new IllegalArgumentException("dataType must not be null");
        }

        if (rawValue == null) {
            throw new IllegalArgumentException("value must not be null");
        }

        return switch (dataType) {
            case NUMERIC -> new Numeric(toBigDecimal(rawValue, dataType));
            case BOOLEAN -> new Bool(toBoolean(rawValue, dataType));
            case CATEGORICAL -> new Categorical(toCategorical(rawValue, dataType));
        };
    }

    private static BigDecimal toBigDecimal(Object value, AiEvalScoreDataType dataType) {
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }

        if (value instanceof Number number) {
            return new BigDecimal(number.toString());
        }

        if (value instanceof String stringValue) {
            try {
                return new BigDecimal(stringValue.trim());
            } catch (NumberFormatException numberFormatException) {
                throw rejectShape(dataType, value, "numeric value", numberFormatException);
            }
        }

        throw rejectShape(dataType, value, "numeric value", null);
    }

    private static boolean toBoolean(Object value, AiEvalScoreDataType dataType) {
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }

        if (value instanceof Number number) {
            return new BigDecimal(number.toString())
                .signum() != 0;
        }

        if (value instanceof String stringValue) {
            String trimmed = stringValue.trim();

            if ("true".equalsIgnoreCase(trimmed)) {
                return true;
            }

            if ("false".equalsIgnoreCase(trimmed)) {
                return false;
            }

            try {
                return new BigDecimal(trimmed)
                    .signum() != 0;
            } catch (NumberFormatException numberFormatException) {
                throw rejectShape(dataType, value, "Boolean / Number / 'true'|'false' / numeric String",
                    numberFormatException);
            }
        }

        throw rejectShape(dataType, value, "Boolean / Number / 'true'|'false' / numeric String", null);
    }

    private static String toCategorical(Object value, AiEvalScoreDataType dataType) {
        if (value instanceof String stringValue) {
            return stringValue;
        }

        throw rejectShape(dataType, value, "String", null);
    }

    private static IllegalArgumentException rejectShape(
        AiEvalScoreDataType dataType, Object value, String expected, Throwable cause) {

        // Caller content stays out of the message — only the runtime class name is included.
        String got = value == null ? "null" : value.getClass()
            .getSimpleName();

        IllegalArgumentException illegalArgumentException = new IllegalArgumentException(
            "Score with dataType=" + dataType + " requires " + expected + "; got " + got);

        if (cause != null) {
            illegalArgumentException.initCause(cause);
        }

        return illegalArgumentException;
    }
}
