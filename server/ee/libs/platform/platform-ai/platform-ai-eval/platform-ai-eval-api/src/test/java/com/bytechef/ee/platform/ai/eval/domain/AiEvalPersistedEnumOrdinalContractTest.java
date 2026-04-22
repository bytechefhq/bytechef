/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.eval.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Pins the persisted-INT-ordinal contract for every platform-ai-eval enum that is written to the database. Each enum's
 * Javadoc documents the append-only invariant; this test makes a regression that reorders, removes, or alphabetises a
 * value fail at build time. Without these pins, a cosmetic refactor would silently flip the meaning of every historical
 * row — and {@code safeOrdinalLookup} only surfaces such corruption after damage is written.
 *
 * <p>
 * Add new variants only at the end of the enum, and append a corresponding {@code containsExactly} entry here.
 *
 * @author Ivica Cardic
 * @version ee
 */
class AiEvalPersistedEnumOrdinalContractTest {

    @Test
    void testAiEvalExecutionStatusOrdinalContract() {
        assertThat(AiEvalExecutionStatus.values()).containsExactly(
            AiEvalExecutionStatus.PENDING,
            AiEvalExecutionStatus.COMPLETED,
            AiEvalExecutionStatus.ERROR);
    }

    @Test
    void testAiEvalExecutionFailureReasonOrdinalContract() {
        assertThat(AiEvalExecutionFailureReason.values()).containsExactly(
            AiEvalExecutionFailureReason.UNKNOWN,
            AiEvalExecutionFailureReason.PROVIDER_NOT_FOUND,
            AiEvalExecutionFailureReason.SCORE_CONFIG_NOT_FOUND,
            AiEvalExecutionFailureReason.LLM_RESPONSE_PARSE_FAILED,
            AiEvalExecutionFailureReason.PROVIDER_REQUEST_FAILED,
            AiEvalExecutionFailureReason.PERSISTENCE_FAILED,
            AiEvalExecutionFailureReason.MISSING_MODEL,
            AiEvalExecutionFailureReason.PROVIDER_REQUEST_FAILED_AFTER_RETRIES);
    }

    @Test
    void testAiEvalScoreDataTypeOrdinalContract() {
        assertThat(AiEvalScoreDataType.values()).containsExactly(
            AiEvalScoreDataType.NUMERIC,
            AiEvalScoreDataType.BOOLEAN,
            AiEvalScoreDataType.CATEGORICAL);
    }

    @Test
    void testAiEvalScoreSourceOrdinalContract() {
        assertThat(AiEvalScoreSource.values()).containsExactly(
            AiEvalScoreSource.MANUAL,
            AiEvalScoreSource.API,
            AiEvalScoreSource.LLM_JUDGE,
            AiEvalScoreSource.EXTERNAL);
    }
}
