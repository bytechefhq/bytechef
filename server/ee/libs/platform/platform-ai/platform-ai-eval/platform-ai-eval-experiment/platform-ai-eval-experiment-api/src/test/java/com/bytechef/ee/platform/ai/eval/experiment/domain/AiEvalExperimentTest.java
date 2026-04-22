/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.eval.experiment.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 * @version ee
 */
class AiEvalExperimentTest {

    @Test
    void testConstructorRejectsNullIds() {
        assertThatNullPointerException().isThrownBy(() -> new AiEvalExperiment(null));
    }

    @Test
    void testStartTransitionsStatusAndStampsStartedDate() {
        AiEvalExperiment experiment = new AiEvalExperiment(2L);

        assertThat(experiment.getStatus()).isEqualTo(AiEvalExperimentStatus.PENDING);
        assertThat(experiment.getStartedDate()).isNull();

        experiment.start();

        assertThat(experiment.getStatus()).isEqualTo(AiEvalExperimentStatus.RUNNING);
        assertThat(experiment.getStartedDate()).isNotNull();
        assertThat(experiment.getCompletedDate()).isNull();
    }

    @Test
    void testCompleteTransitionsStatusAndStampsCompletedDate() {
        AiEvalExperiment experiment = new AiEvalExperiment(2L);

        experiment.start();
        experiment.complete();

        assertThat(experiment.getStatus()).isEqualTo(AiEvalExperimentStatus.COMPLETED);
        assertThat(experiment.getCompletedDate()).isNotNull();
    }

    @Test
    void testFailTransitionsStatusAndStampsCompletedDate() {
        AiEvalExperiment experiment = new AiEvalExperiment(2L);

        experiment.start();
        experiment.fail();

        assertThat(experiment.getStatus()).isEqualTo(AiEvalExperimentStatus.FAILED);
        assertThat(experiment.getCompletedDate()).isNotNull();
    }

    @Test
    void testFailFromPendingIsAllowed() {
        AiEvalExperiment experiment = new AiEvalExperiment(2L);

        // PENDING → FAILED is the T2.13 markRunning-failure recovery path: the executor must converge a still-PENDING
        // experiment to FAILED when the markRunning transition itself blew up.
        experiment.fail();

        assertThat(experiment.getStatus()).isEqualTo(AiEvalExperimentStatus.FAILED);
    }

    @Test
    void testCannotCompleteFromPending() {
        AiEvalExperiment experiment = new AiEvalExperiment(2L);

        assertThatIllegalStateException()
            .isThrownBy(experiment::complete)
            .withMessageContaining("PENDING");
    }

    @Test
    void testSetDatasetVersionIdRejectedAfterStart() {
        // Mutating any of the four configuration setters after start() invalidates every recorded
        // AiEvalExperimentRun against the original config — assertPending closes that backdoor. Without these
        // pins, a regression that drops assertPending from one setter would silently corrupt
        // run-vs-experiment alignment downstream.
        AiEvalExperiment experiment = new AiEvalExperiment(2L);

        experiment.start();

        assertThatIllegalStateException().isThrownBy(() -> experiment.setDatasetVersionId(99L));
    }

    @Test
    void testSetMetadataRejectedAfterStart() {
        AiEvalExperiment experiment = new AiEvalExperiment(2L);

        experiment.start();

        assertThatIllegalStateException().isThrownBy(() -> experiment.setMetadata("{\"k\":\"v\"}"));
    }

    @Test
    void testSetModelRejectedAfterStart() {
        AiEvalExperiment experiment = new AiEvalExperiment(2L);

        experiment.start();

        assertThatIllegalStateException().isThrownBy(() -> experiment.setModel("openai/gpt-5"));
    }

    @Test
    void testSetPromptVersionIdRejectedAfterStart() {
        AiEvalExperiment experiment = new AiEvalExperiment(2L);

        experiment.start();

        assertThatIllegalStateException().isThrownBy(() -> experiment.setPromptVersionId(42L));
    }

    @Test
    void testCannotTransitionFromTerminalState() {
        AiEvalExperiment completed = new AiEvalExperiment(2L);

        completed.start();
        completed.complete();

        assertThatIllegalStateException().isThrownBy(completed::start);
        assertThatIllegalStateException().isThrownBy(completed::complete);
        assertThatIllegalStateException().isThrownBy(completed::fail);

        AiEvalExperiment failed = new AiEvalExperiment(2L);

        failed.start();
        failed.fail();

        assertThatIllegalStateException().isThrownBy(failed::start);
        assertThatIllegalStateException().isThrownBy(failed::complete);
        assertThatIllegalStateException().isThrownBy(failed::fail);
    }
}
