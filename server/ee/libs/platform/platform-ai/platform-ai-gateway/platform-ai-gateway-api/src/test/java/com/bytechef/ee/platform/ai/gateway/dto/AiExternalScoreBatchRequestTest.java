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
        // Validation parity with the singleton AiExternalScoreRequest: name must be non-blank. Without this
        // guard, a row with blank name slips DTO validation and fails deeper in the facade as a generic 500.
        // Validate.isTrue with a false predicate throws IllegalArgumentException.
        assertThatThrownBy(() -> new AiExternalScoreBatchItem(
            1L, null, "", BigDecimal.ONE, AiEvalScoreDataType.NUMERIC,
            null, "ragas", Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testBatchItemRejectsNullValue() {
        // Explicit IllegalArgumentException so the failure threads through handleBadRequest as HTTP 400.
        // Without this targeted check, commons-lang3 Validate.notNull throws NPE, which falls through to the
        // generic 500 path and mis-shapes a clear validation failure as an internal-error response.
        assertThatThrownBy(() -> new AiExternalScoreBatchItem(
            1L, null, "faithfulness", null, AiEvalScoreDataType.NUMERIC,
            null, "ragas", Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("value");
    }

    @Test
    void testBatchItemRejectsNumericDataTypeWithStringValue() {
        // assertCompatible runs unconditionally on the batch item; a NUMERIC dataType with a non-numeric
        // String value must fail at construction, not deep in the facade.
        assertThatThrownBy(() -> new AiExternalScoreBatchItem(
            1L, null, "faithfulness", "not-a-number", AiEvalScoreDataType.NUMERIC,
            null, "ragas", Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testConstructorRejectsBatchExceedingAbsoluteCap() {
        // The defense-in-depth ceiling MAX_BATCH_SIZE=10_000 fires even when the operator-tunable
        // bytechef.ai.gateway.external-scores.maxBatchSize is bypassed (direct DTO construction by tests, internal
        // facades, future SDK clients). Without this assertion a regression that loosens the constructor cap
        // (e.g., dropping the size check during refactor) would surface only as a connection-pool starvation
        // incident in production — too late.
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
        // Pin the inclusive boundary: a batch of exactly MAX_BATCH_SIZE is accepted. Off-by-one regressions on
        // the > vs >= comparison would silently flip this to a 400 surfaced as Jackson rejection at the wire.
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
