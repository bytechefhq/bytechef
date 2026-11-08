/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.ee.platform.ai.eval.domain.AiEvalScoreDataType;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 * @version ee
 */
class AiExternalScoreBatchRequestTest {

    @Test
    void testBatchItemRejectsBothTraceAndSpanIds() {
        assertThatThrownBy(() -> new AiExternalScoreBatchItem(
            1L, 2L, "faithfulness", BigDecimal.ONE, AiEvalScoreDataType.NUMERIC,
            null, "ragas", Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testBatchItemRejectsNeitherTraceNorSpanId() {
        assertThatThrownBy(() -> new AiExternalScoreBatchItem(
            null, null, "faithfulness", BigDecimal.ONE, AiEvalScoreDataType.NUMERIC,
            null, "ragas", Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testBatchItemRejectsBlankName() {
        assertThatThrownBy(() -> new AiExternalScoreBatchItem(
            1L, null, "", BigDecimal.ONE, AiEvalScoreDataType.NUMERIC,
            null, "ragas", Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testBatchItemRejectsNullValue() {
        assertThatThrownBy(() -> new AiExternalScoreBatchItem(
            1L, null, "faithfulness", null, AiEvalScoreDataType.NUMERIC,
            null, "ragas", Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("value");
    }

    @Test
    void testBatchItemRejectsNumericDataTypeWithStringValue() {
        assertThatThrownBy(() -> new AiExternalScoreBatchItem(
            1L, null, "faithfulness", "not-a-number", AiEvalScoreDataType.NUMERIC,
            null, "ragas", Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testConstructorRejectsBatchExceedingAbsoluteCap() {
        List<AiExternalScoreBatchItem> oversizedScores =
            new ArrayList<>(AiExternalScoreBatchRequest.MAX_BATCH_SIZE + 1);

        for (int i = 0; i < AiExternalScoreBatchRequest.MAX_BATCH_SIZE + 1; i++) {
            oversizedScores.add(new AiExternalScoreBatchItem(
                (long) (i + 1), null, "faithfulness", BigDecimal.ONE, AiEvalScoreDataType.NUMERIC,
                null, "ragas", Map.of()));
        }

        assertThatThrownBy(() -> new AiExternalScoreBatchRequest(oversizedScores))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining(String.valueOf(AiExternalScoreBatchRequest.MAX_BATCH_SIZE))
            .hasMessageContaining("absolute cap");
    }

    @Test
    void testConstructorAcceptsBatchAtAbsoluteCap() {
        List<AiExternalScoreBatchItem> atCapScores = new ArrayList<>(AiExternalScoreBatchRequest.MAX_BATCH_SIZE);

        for (int i = 0; i < AiExternalScoreBatchRequest.MAX_BATCH_SIZE; i++) {
            atCapScores.add(new AiExternalScoreBatchItem(
                (long) (i + 1), null, "faithfulness", BigDecimal.ONE, AiEvalScoreDataType.NUMERIC,
                null, "ragas", Map.of()));
        }

        AiExternalScoreBatchRequest request = new AiExternalScoreBatchRequest(atCapScores);

        assertThat(request.scores()).hasSize(AiExternalScoreBatchRequest.MAX_BATCH_SIZE);
    }
}
