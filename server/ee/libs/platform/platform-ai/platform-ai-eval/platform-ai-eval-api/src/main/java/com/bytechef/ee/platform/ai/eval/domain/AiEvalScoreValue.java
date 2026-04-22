/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.eval.domain;

import java.math.BigDecimal;
import org.apache.commons.lang3.Validate;
import org.jspecify.annotations.Nullable;

/**
 * Typed eval-score value paired with its {@link AiEvalScoreDataType}. Discriminated view over the flat
 * {@code (BigDecimal value, String stringValue)} columns that {@link AiEvalScore} persists to JDBC — callers that
 * construct or read scores should prefer this view so the compiler rules out illegal combinations like "NUMERIC score
 * with only a string value" at the type level.
 *
 * @author Ivica Cardic
 * @version ee
 */
public sealed interface AiEvalScoreValue {

    AiEvalScoreDataType dataType();

    /**
     * Numeric score (e.g. rating, similarity). Mapped to the {@code value} column; {@code stringValue} is left null.
     */
    record Numeric(BigDecimal value) implements AiEvalScoreValue {

        public Numeric {
            Validate.notNull(value, "numeric value must not be null");
        }

        @Override
        public AiEvalScoreDataType dataType() {
            return AiEvalScoreDataType.NUMERIC;
        }
    }

    /**
     * Boolean score (pass/fail). Mapped to both columns for query convenience: {@code value} stores 0/1 so SQL
     * aggregation works, {@code stringValue} stores {@code "true"} / {@code "false"} for human-readable export.
     */
    record Bool(boolean value) implements AiEvalScoreValue {

        @Override
        public AiEvalScoreDataType dataType() {
            return AiEvalScoreDataType.BOOLEAN;
        }

        public BigDecimal asNumeric() {
            return value ? BigDecimal.ONE : BigDecimal.ZERO;
        }

        public String asString() {
            return Boolean.toString(value);
        }
    }

    /**
     * Categorical score (label from a discrete set). Mapped to {@code stringValue}; {@code value} is left null.
     */
    record Categorical(String label) implements AiEvalScoreValue {

        public Categorical {
            Validate.notBlank(label, "categorical label must not be blank");
        }

        @Override
        public AiEvalScoreDataType dataType() {
            return AiEvalScoreDataType.CATEGORICAL;
        }
    }

    /**
     * Reconstructs the typed value from the flat JDBC columns. Throws {@link IllegalStateException} (rather than
     * returning {@code null}) when the row's columns cannot materialize the declared {@code dataType} — e.g. a
     * CATEGORICAL row with a null {@code stringValue}, or a NUMERIC row with a null {@code value}. The strictness
     * mirrors {@code applyTypedValue} on the write path and {@code AiEvalScore.getDataType()} on the read path: a
     * silent {@code null} return forced callers to handle a tri-state {parse-failure / valid / type-mismatch} that
     * never had a meaningful default, and the resulting NPE typically manifested far from the corrupted row. Callers
     * that need to surface "corrupt row" without crashing the response should catch the exception explicitly.
     * {@code rowId} is included in the diagnostic so operators can locate and remediate the row.
     */
    static AiEvalScoreValue fromColumns(
        @Nullable Long rowId, AiEvalScoreDataType dataType, @Nullable BigDecimal numeric, @Nullable String string) {

        return switch (dataType) {
            case NUMERIC -> {
                if (numeric == null) {
                    throw corrupt(rowId, dataType, "value column is null");
                }

                yield new Numeric(numeric);
            }
            case BOOLEAN -> {
                if (numeric != null) {
                    yield new Bool(numeric.signum() != 0);
                }

                if (string != null) {
                    yield new Bool(Boolean.parseBoolean(string));
                }

                throw corrupt(rowId, dataType, "both value and stringValue columns are null");
            }
            case CATEGORICAL -> {
                if (string == null || string.isBlank()) {
                    throw corrupt(rowId, dataType, "stringValue column is null or blank");
                }

                yield new Categorical(string);
            }
        };
    }

    private static IllegalStateException corrupt(
        @Nullable Long rowId, AiEvalScoreDataType dataType, String detail) {

        return new IllegalStateException(
            "AiEvalScore id=" + rowId + " has corrupt " + dataType + " columns (" + detail + ")");
    }
}
