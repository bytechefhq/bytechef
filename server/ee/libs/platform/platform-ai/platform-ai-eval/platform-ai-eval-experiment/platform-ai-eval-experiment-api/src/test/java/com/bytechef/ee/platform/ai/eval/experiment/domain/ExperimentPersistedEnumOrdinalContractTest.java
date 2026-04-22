/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.eval.experiment.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Pins the persisted-INT-ordinal contract for the experiment-api enums. See {@code PersistedEnumOrdinalContractTest} in
 * gateway-api for the rationale. Each enum's Javadoc documents the append-only invariant; this test makes a regression
 * that reorders, removes, or alphabetises a value fail at build time.
 *
 * @author Ivica Cardic
 * @version ee
 */
class ExperimentPersistedEnumOrdinalContractTest {

    @Test
    void testAiEvalExperimentStatusOrdinalContract() {
        assertThat(AiEvalExperimentStatus.values()).containsExactly(
            AiEvalExperimentStatus.PENDING,
            AiEvalExperimentStatus.RUNNING,
            AiEvalExperimentStatus.COMPLETED,
            AiEvalExperimentStatus.FAILED);
    }

    @Test
    void testAiEvalExperimentRunStatusOrdinalContract() {
        assertThat(AiEvalExperimentRunStatus.values()).containsExactly(
            AiEvalExperimentRunStatus.PENDING,
            AiEvalExperimentRunStatus.RUNNING,
            AiEvalExperimentRunStatus.COMPLETED,
            AiEvalExperimentRunStatus.FAILED);
    }
}
