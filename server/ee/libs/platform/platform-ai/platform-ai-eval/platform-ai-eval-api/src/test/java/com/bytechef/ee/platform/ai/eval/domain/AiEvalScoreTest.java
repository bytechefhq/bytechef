/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.eval.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 * @version ee
 */
class AiEvalScoreTest {

    @Test
    void testMetadataAndSourceIdentifierRoundTrip() {
        AiEvalScore score = AiEvalScore.numeric(
            1L, "accuracy", AiEvalScoreSource.EXTERNAL, BigDecimal.ONE);

        score.setMetadata("{\"foo\":\"bar\"}");
        score.setSourceIdentifier("ragas@0.2.3");

        assertEquals("{\"foo\":\"bar\"}", score.getMetadata());
        assertEquals("ragas@0.2.3", score.getSourceIdentifier());
    }

    @Test
    void testNumericFactoryWithMetadataAndSourceIdentifier() {
        AiEvalScore score = AiEvalScore.numeric(
            1L, "accuracy", AiEvalScoreSource.EXTERNAL, new BigDecimal("0.95"),
            "ragas@0.2.3", "{\"runId\":\"abc\"}");

        // Read via getTypedValue() — getValue() is private, callers must go through the typed view.
        AiEvalScoreValue typed = score.getTypedValue();

        assertThat(typed)
            .isInstanceOf(AiEvalScoreValue.Numeric.class)
            .extracting(value -> ((AiEvalScoreValue.Numeric) value).value())
            .isEqualTo(new BigDecimal("0.95"));

        assertEquals("ragas@0.2.3", score.getSourceIdentifier());
        assertEquals("{\"runId\":\"abc\"}", score.getMetadata());
        assertEquals(AiEvalScoreSource.EXTERNAL, score.getSource());
    }

    @Test
    void testBoolFactoryWithMetadataAndSourceIdentifier() {
        AiEvalScore score = AiEvalScore.bool(
            1L, "passes", AiEvalScoreSource.EXTERNAL, true,
            "internal-judge-v1", "{\"reason\":\"ok\"}");

        assertEquals("internal-judge-v1", score.getSourceIdentifier());
        assertEquals("{\"reason\":\"ok\"}", score.getMetadata());
        assertEquals(AiEvalScoreDataType.BOOLEAN, score.getDataType());
    }

    /**
     * Reflective regression guard for the private demotion of {@code setStringValue} and {@code setValue}. The dataType
     * invariant is only safe if every caller — including same-package code — goes through {@code applyTypedValue};
     * widening either setter back to package-private or higher would re-open the loophole where a future class added to
     * {@code com.bytechef.ee.automation.ai.gateway.domain} could desync {@code (dataType, value, stringValue)} and
     * silently corrupt aggregates. Spring Data JDBC's reflective hydration calls {@code Method.setAccessible(true)} so
     * private visibility does not break row mapping. Mirrors the {@code @PreAuthorize} reflective check in
     * {@code AiExperimentGraphQlControllerTest}.
     */
    @Test
    void testStringValueAndValueSettersArePrivate() throws NoSuchMethodException {
        Method setStringValue = AiEvalScore.class.getDeclaredMethod("setStringValue", String.class);
        Method setValue = AiEvalScore.class.getDeclaredMethod("setValue", BigDecimal.class);

        int stringValueModifiers = setStringValue.getModifiers();
        int valueModifiers = setValue.getModifiers();

        assertThat(Modifier.isPrivate(stringValueModifiers))
            .as("setStringValue must be private — re-widening breaks the dataType invariant")
            .isTrue();

        assertThat(Modifier.isPrivate(valueModifiers))
            .as("setValue must be private — re-widening breaks the dataType invariant")
            .isTrue();
    }

    @Test
    void testCategoricalFactoryWithMetadataAndSourceIdentifier() {
        AiEvalScore score = AiEvalScore.categorical(
            1L, "category", AiEvalScoreSource.EXTERNAL, "good",
            "ragas@0.2.3", null);

        assertEquals("ragas@0.2.3", score.getSourceIdentifier());
        assertNull(score.getMetadata());
        assertEquals(AiEvalScoreDataType.CATEGORICAL, score.getDataType());
    }
}
