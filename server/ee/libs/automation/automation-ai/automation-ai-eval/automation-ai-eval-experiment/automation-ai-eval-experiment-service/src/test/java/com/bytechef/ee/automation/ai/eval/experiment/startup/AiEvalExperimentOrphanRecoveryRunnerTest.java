/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.eval.experiment.startup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.ai.eval.experiment.domain.AiEvalExperiment;
import com.bytechef.ee.platform.ai.eval.experiment.domain.AiEvalExperimentRun;
import com.bytechef.ee.platform.ai.eval.experiment.service.AiEvalExperimentRunService;
import com.bytechef.ee.platform.ai.eval.experiment.service.AiEvalExperimentService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Ivica Cardic
 * @version ee
 */
class AiEvalExperimentOrphanRecoveryRunnerTest {

    @Test
    void testRecoversOrphanedExperiments() {
        AiEvalExperimentService experimentService = mock(AiEvalExperimentService.class);
        AiEvalExperimentRunService runService = mock(AiEvalExperimentRunService.class);

        AiEvalExperiment orphan = new AiEvalExperiment(7L);
        ReflectionTestUtils.setField(orphan, "id", 100L);

        AiEvalExperimentRun pendingRun = new AiEvalExperimentRun(100L, 1L);
        ReflectionTestUtils.setField(pendingRun, "id", 501L);

        AiEvalExperimentRun runningRun = new AiEvalExperimentRun(100L, 2L);
        ReflectionTestUtils.setField(runningRun, "id", 502L);
        runningRun.markRunning();

        AiEvalExperimentRun completedRun = new AiEvalExperimentRun(100L, 3L);
        ReflectionTestUtils.setField(completedRun, "id", 503L);
        completedRun.markRunning();
        completedRun.complete(9001L, 100, new BigDecimal("0.01"));

        when(experimentService.findRunningOlderThan(any())).thenReturn(List.of(orphan));
        when(runService.findAllByExperiment(100L)).thenReturn(List.of(pendingRun, runningRun, completedRun));

        AiEvalExperimentOrphanRecoveryRunner runner = new AiEvalExperimentOrphanRecoveryRunner(
            experimentService, runService, AiEvalExperimentOrphanRecoveryRunner.DEFAULT_ORPHAN_THRESHOLD_MINUTES);

        runner.recoverOrphanedExperiments();

        verify(runService).fail(501L, "Orphaned by JVM restart");
        verify(runService).fail(502L, "Orphaned by JVM restart");
        verify(runService, never()).fail(eq(503L), any());
        verify(experimentService).markFinished(100L, true);
    }

    /**
     * Pins the inner per-run try/catch in {@code recoverOne}: a single failing {@code runService.fail(501L,...)} MUST
     * NOT abort the recovery loop. The next run (502L) must still be failed and the experiment-level
     * {@code markFinished} must still execute. Without this guard, a self-perpetuating orphan loop forms — the
     * experiment stays in RUNNING and the next JVM start re-discovers it indefinitely.
     */
    @Test
    void testContinuesLoopWhenOneRunFailFails() {
        AiEvalExperimentService experimentService = mock(AiEvalExperimentService.class);
        AiEvalExperimentRunService runService = mock(AiEvalExperimentRunService.class);

        AiEvalExperiment orphan = new AiEvalExperiment(7L);
        ReflectionTestUtils.setField(orphan, "id", 100L);

        AiEvalExperimentRun firstRun = new AiEvalExperimentRun(100L, 1L);
        ReflectionTestUtils.setField(firstRun, "id", 501L);

        AiEvalExperimentRun secondRun = new AiEvalExperimentRun(100L, 2L);
        ReflectionTestUtils.setField(secondRun, "id", 502L);

        when(experimentService.findRunningOlderThan(any())).thenReturn(List.of(orphan));
        when(runService.findAllByExperiment(100L)).thenReturn(List.of(firstRun, secondRun));
        doThrow(new RuntimeException("DB blip on first run.fail()")).when(runService)
            .fail(eq(501L), any());

        AiEvalExperimentOrphanRecoveryRunner runner = new AiEvalExperimentOrphanRecoveryRunner(
            experimentService, runService, AiEvalExperimentOrphanRecoveryRunner.DEFAULT_ORPHAN_THRESHOLD_MINUTES);

        runner.recoverOrphanedExperiments();

        verify(runService).fail(501L, "Orphaned by JVM restart");
        verify(runService).fail(502L, "Orphaned by JVM restart");
        verify(experimentService).markFinished(100L, true);
    }

    /**
     * Pins the markFinished resilience contract: if {@code markFinished} throws, the orphan recovery loop logs the
     * failure rather than silently re-leaving the experiment in RUNNING. The recovery is still incomplete — the next
     * JVM restart will re-discover the same orphan — but the operator gets a distinct error log signaling that the
     * recovery itself failed (vs. the per-run failures which carry their own warn logs).
     */
    @Test
    void testRecoveryContinuesWhenMarkFinishedFails() {
        AiEvalExperimentService experimentService = mock(AiEvalExperimentService.class);
        AiEvalExperimentRunService runService = mock(AiEvalExperimentRunService.class);

        AiEvalExperiment orphan = new AiEvalExperiment(7L);
        ReflectionTestUtils.setField(orphan, "id", 200L);

        AiEvalExperimentRun pendingRun = new AiEvalExperimentRun(200L, 1L);
        ReflectionTestUtils.setField(pendingRun, "id", 601L);

        when(experimentService.findRunningOlderThan(any())).thenReturn(List.of(orphan));
        when(runService.findAllByExperiment(200L)).thenReturn(List.of(pendingRun));
        doThrow(new RuntimeException("optimistic-lock contention")).when(experimentService)
            .markFinished(200L, true);

        AiEvalExperimentOrphanRecoveryRunner runner = new AiEvalExperimentOrphanRecoveryRunner(
            experimentService, runService, AiEvalExperimentOrphanRecoveryRunner.DEFAULT_ORPHAN_THRESHOLD_MINUTES);

        // MUST NOT throw — markFinished's failure is caught and logged at error so the experiment-level
        // failure is distinct from per-run failures, and so the recovery loop completes cleanly even
        // when markFinished blows up.
        runner.recoverOrphanedExperiments();

        verify(runService).fail(601L, "Orphaned by JVM restart");
        verify(experimentService).markFinished(200L, true);
    }

    @Test
    void testNoOpWhenNoOrphans() {
        AiEvalExperimentService experimentService = mock(AiEvalExperimentService.class);
        AiEvalExperimentRunService runService = mock(AiEvalExperimentRunService.class);

        when(experimentService.findRunningOlderThan(any())).thenReturn(List.of());

        AiEvalExperimentOrphanRecoveryRunner runner = new AiEvalExperimentOrphanRecoveryRunner(
            experimentService, runService, AiEvalExperimentOrphanRecoveryRunner.DEFAULT_ORPHAN_THRESHOLD_MINUTES);

        runner.recoverOrphanedExperiments();

        verifyNoInteractions(runService);
    }

    /**
     * Pins the opt-in contract: the recovery runner is gated by both {@code bytechef.ai.gateway.enabled} AND
     * {@code bytechef.ai.gateway.experiment.orphan-recovery.enabled} with no {@code matchIfMissing} default. A
     * regression that re-introduces {@code matchIfMissing = true} would silently re-enable the runner on every
     * multi-instance node — instance B would force-fail RUNNING experiments still in flight on instance A. CE installs
     * also rely on the default-disabled behavior to keep the runner out of CE contexts.
     */
    @Test
    void testConditionalOnPropertyIsOptIn() {
        ConditionalOnProperty annotation = AiEvalExperimentOrphanRecoveryRunner.class
            .getAnnotation(ConditionalOnProperty.class);

        assertThat(annotation)
            .as("AiEvalExperimentOrphanRecoveryRunner must be @ConditionalOnProperty-gated")
            .isNotNull();

        assertThat(annotation.matchIfMissing())
            .as("matchIfMissing must remain false so the runner stays opt-in (multi-instance / CE safety)")
            .isFalse();

        assertThat(annotation.havingValue())
            .as("havingValue must be 'true' to require explicit opt-in")
            .isEqualTo("true");

        assertThat(annotation.name())
            .as("Both bytechef.ai.gateway.enabled AND .experiment.orphan-recovery.enabled must be set")
            .containsExactlyInAnyOrder(
                "bytechef.ai.gateway.enabled",
                "bytechef.ai.gateway.experiment.orphan-recovery.enabled");
    }
}
