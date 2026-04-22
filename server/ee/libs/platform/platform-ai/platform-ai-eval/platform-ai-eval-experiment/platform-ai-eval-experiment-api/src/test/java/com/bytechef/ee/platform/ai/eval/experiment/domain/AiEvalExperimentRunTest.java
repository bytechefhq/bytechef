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

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 * @version ee
 */
class AiEvalExperimentRunTest {

    @Test
    void testConstructorRejectsNullIds() {
        assertThatNullPointerException().isThrownBy(() -> new AiEvalExperimentRun(null, 2L));
        assertThatNullPointerException().isThrownBy(() -> new AiEvalExperimentRun(1L, null));
    }

    @Test
    void testMarkRunningTransitionsStatus() {
        AiEvalExperimentRun run = new AiEvalExperimentRun(1L, 2L);

        assertThat(run.getStatus()).isEqualTo(AiEvalExperimentRunStatus.PENDING);

        run.markRunning();

        assertThat(run.getStatus()).isEqualTo(AiEvalExperimentRunStatus.RUNNING);
    }

    @Test
    void testCompleteSetsTraceLatencyAndCost() {
        AiEvalExperimentRun run = new AiEvalExperimentRun(1L, 2L);

        run.markRunning();
        run.complete(99L, 1234, new BigDecimal("0.001234"));

        assertThat(run.getStatus()).isEqualTo(AiEvalExperimentRunStatus.COMPLETED);
        assertThat(run.getTraceId()).isEqualTo(99L);
        assertThat(run.getLatencyMs()).isEqualTo(1234);
        assertThat(run.getCost()).isEqualByComparingTo(new BigDecimal("0.001234"));
        assertThat(run.getErrorMessage()).isNull();
    }

    @Test
    void testFailSetsStatusAndError() {
        AiEvalExperimentRun run = new AiEvalExperimentRun(1L, 2L);

        run.markRunning();
        run.fail("model timeout");

        assertThat(run.getStatus()).isEqualTo(AiEvalExperimentRunStatus.FAILED);
        assertThat(run.getErrorMessage()).isEqualTo("model timeout");
    }

    @Test
    void testFailFromPendingIsAllowed() {
        AiEvalExperimentRun run = new AiEvalExperimentRun(1L, 2L);

        // PENDING → FAILED supports the executor's stop-requested cancellation path: pre-started runs are cancelled
        // without first transitioning them to RUNNING.
        run.fail("Cancelled by user before run started");

        assertThat(run.getStatus()).isEqualTo(AiEvalExperimentRunStatus.FAILED);
    }

    @Test
    void testCannotCompleteFromPending() {
        AiEvalExperimentRun run = new AiEvalExperimentRun(1L, 2L);

        assertThatIllegalStateException()
            .isThrownBy(() -> run.complete(99L, 1234, new BigDecimal("0.001234")))
            .withMessageContaining("PENDING");
    }

    @Test
    void testCannotTransitionFromTerminalState() {
        AiEvalExperimentRun completed = new AiEvalExperimentRun(1L, 2L);

        completed.markRunning();
        completed.complete(99L, 1234, new BigDecimal("0.001234"));

        assertThatIllegalStateException().isThrownBy(completed::markRunning);
        assertThatIllegalStateException()
            .isThrownBy(() -> completed.complete(99L, 1234, new BigDecimal("0.001234")));
        assertThatIllegalStateException().isThrownBy(() -> completed.fail("late"));

        AiEvalExperimentRun failed = new AiEvalExperimentRun(1L, 2L);

        failed.markRunning();
        failed.fail("boom");

        assertThatIllegalStateException().isThrownBy(failed::markRunning);
        assertThatIllegalStateException()
            .isThrownBy(() -> failed.complete(99L, 1234, new BigDecimal("0.001234")));
        assertThatIllegalStateException().isThrownBy(() -> failed.fail("again"));
    }
}
