/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.eval.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 * @version ee
 */
class AiEvalScoreValueTest {

    @Test
    void testFromColumnsRoundTripsNumeric() {
        AiEvalScoreValue value = AiEvalScoreValue.fromColumns(
            42L, AiEvalScoreDataType.NUMERIC, new BigDecimal("0.87"), null);

        assertThat(value)
            .isInstanceOfSatisfying(AiEvalScoreValue.Numeric.class,
                numeric -> assertThat(numeric.value()).isEqualByComparingTo("0.87"));
    }

    @Test
    void testFromColumnsRoundTripsBooleanFromNumericColumn() {
        AiEvalScoreValue value = AiEvalScoreValue.fromColumns(
            42L, AiEvalScoreDataType.BOOLEAN, BigDecimal.ONE, null);

        assertThat(value)
            .isInstanceOfSatisfying(AiEvalScoreValue.Bool.class,
                bool -> assertThat(bool.value()).isTrue());
    }

    @Test
    void testFromColumnsRoundTripsBooleanFromStringColumn() {
        AiEvalScoreValue value = AiEvalScoreValue.fromColumns(
            42L, AiEvalScoreDataType.BOOLEAN, null, "true");

        assertThat(value)
            .isInstanceOfSatisfying(AiEvalScoreValue.Bool.class,
                bool -> assertThat(bool.value()).isTrue());
    }

    @Test
    void testFromColumnsPrefersNumericColumnForBoolean() {
        AiEvalScoreValue value = AiEvalScoreValue.fromColumns(
            42L, AiEvalScoreDataType.BOOLEAN, BigDecimal.ZERO, "true");

        assertThat(value)
            .isInstanceOfSatisfying(AiEvalScoreValue.Bool.class,
                bool -> assertThat(bool.value()).isFalse());
    }

    @Test
    void testFromColumnsRoundTripsCategorical() {
        AiEvalScoreValue value = AiEvalScoreValue.fromColumns(
            42L, AiEvalScoreDataType.CATEGORICAL, null, "high");

        assertThat(value)
            .isInstanceOfSatisfying(AiEvalScoreValue.Categorical.class,
                categorical -> assertThat(categorical.label()).isEqualTo("high"));
    }

    @Test
    void testFromColumnsThrowsOnNumericWithNullValueColumn() {
        assertThatThrownBy(() -> AiEvalScoreValue.fromColumns(42L, AiEvalScoreDataType.NUMERIC, null, "stuff"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("id=42")
            .hasMessageContaining("NUMERIC")
            .hasMessageContaining("value column is null");
    }

    @Test
    void testFromColumnsThrowsOnBooleanWithBothColumnsNull() {
        assertThatThrownBy(() -> AiEvalScoreValue.fromColumns(42L, AiEvalScoreDataType.BOOLEAN, null, null))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("id=42")
            .hasMessageContaining("BOOLEAN");
    }

    @Test
    void testFromColumnsThrowsOnCategoricalWithNullStringColumn() {
        assertThatThrownBy(() -> AiEvalScoreValue.fromColumns(42L, AiEvalScoreDataType.CATEGORICAL, null, null))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("id=42")
            .hasMessageContaining("CATEGORICAL");
    }

    @Test
    void testFromColumnsThrowsOnCategoricalWithBlankStringColumn() {
        assertThatThrownBy(() -> AiEvalScoreValue.fromColumns(42L, AiEvalScoreDataType.CATEGORICAL, null, "   "))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("id=42")
            .hasMessageContaining("CATEGORICAL");
    }

    @Test
    void testNumericRecordRejectsNullValue() {
        assertThatThrownBy(() -> new AiEvalScoreValue.Numeric(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("numeric value");
    }

    @Test
    void testCategoricalRecordRejectsBlankLabel() {
        assertThatThrownBy(() -> new AiEvalScoreValue.Categorical(" "))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("categorical label");
    }
}
