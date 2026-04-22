/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.domain;

import java.math.BigDecimal;
import org.apache.commons.lang3.Validate;
import org.jspecify.annotations.Nullable;

/**
 * Typed eval-score value paired with its {@link AiEvalScoreDataType}. Replaces the untyped pair of nullable
 * {@code (BigDecimal value, String stringValue)} columns that {@link AiEvalScore} persists to JDBC — the flat columns
 * still exist for persistence, but callers that construct or read scores should prefer this discriminated view so the
 * compiler rules out combinations like "NUMERIC score with only a string value" that the old API quietly accepted.
 *
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
     * Reconstructs the typed value from the flat JDBC columns. Returns {@code null} when neither column carries enough
     * information to materialize the type (e.g. a CATEGORICAL row with a null {@code stringValue} — should not happen
     * in well-formed rows, but persisted data can be inconsistent for historical reasons).
     */
    @Nullable
    static AiEvalScoreValue fromColumns(
        AiEvalScoreDataType dataType, @Nullable BigDecimal numeric, @Nullable String string) {

        return switch (dataType) {
            case NUMERIC -> numeric != null ? new Numeric(numeric) : null;
            case BOOLEAN -> numeric != null ? new Bool(numeric.signum() != 0)
                : string != null ? new Bool(Boolean.parseBoolean(string))
                    : null;
            case CATEGORICAL -> string != null && !string.isBlank() ? new Categorical(string) : null;
        };
    }
}
