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
 * @author Ivica Cardic
 * @version ee
 */
public sealed interface AiEvalScoreValue {

    AiEvalScoreDataType dataType();

    record Numeric(BigDecimal value) implements AiEvalScoreValue {

        public Numeric {
            Validate.notNull(value, "numeric value must not be null");
        }

        @Override
        public AiEvalScoreDataType dataType() {
            return AiEvalScoreDataType.NUMERIC;
        }
    }

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

    record Categorical(String label) implements AiEvalScoreValue {

        public Categorical {
            Validate.notBlank(label, "categorical label must not be blank");
        }

        @Override
        public AiEvalScoreDataType dataType() {
            return AiEvalScoreDataType.CATEGORICAL;
        }
    }

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
