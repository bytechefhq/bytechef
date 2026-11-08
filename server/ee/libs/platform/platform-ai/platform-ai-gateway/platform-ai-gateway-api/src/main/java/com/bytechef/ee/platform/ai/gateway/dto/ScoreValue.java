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
 * @author Ivica Cardic
 * @version ee
 */
public sealed interface ScoreValue permits ScoreValue.Numeric, ScoreValue.Bool, ScoreValue.Categorical {

    record Numeric(BigDecimal value) implements ScoreValue {

        public Numeric {
            if (value == null) {
                throw new IllegalArgumentException("Numeric score value must not be null");
            }
        }
    }

    record Bool(boolean value) implements ScoreValue {
    }

    record Categorical(String value) implements ScoreValue {

        public Categorical {
            if (value == null) {
                throw new IllegalArgumentException("Categorical score value must not be null");
            }
        }
    }

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
