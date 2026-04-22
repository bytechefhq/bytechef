/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.eval.experiment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.ai.eval.experiment.domain.AiEvalExperimentRun;
import com.bytechef.ee.platform.ai.eval.experiment.domain.AiEvalExperimentRunStatus;
import com.bytechef.ee.platform.ai.eval.experiment.repository.AiEvalExperimentRunRepository;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link AiEvalExperimentRunServiceImpl}. Covers the per-run lifecycle transitions (markRunning /
 * complete / fail).
 *
 * @author Ivica Cardic
 * @version ee
 */
@ExtendWith(MockitoExtension.class)
class AiEvalExperimentRunServiceTest {

    @Mock
    private AiEvalExperimentRunRepository aiEvalExperimentRunRepository;

    private AiEvalExperimentRunServiceImpl aiEvalExperimentRunService;

    @BeforeEach
    void setUp() {
        aiEvalExperimentRunService = new AiEvalExperimentRunServiceImpl(aiEvalExperimentRunRepository);
    }

    @Test
    void testMarkRunningTransitionsStatus() {
        AiEvalExperimentRun run = seedId(new AiEvalExperimentRun(1L, 2L), 99L);

        when(aiEvalExperimentRunRepository.findById(99L)).thenReturn(Optional.of(run));
        when(aiEvalExperimentRunRepository.save(any(AiEvalExperimentRun.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        AiEvalExperimentRun result = aiEvalExperimentRunService.markRunning(99L);

        assertThat(result.getStatus()).isEqualTo(AiEvalExperimentRunStatus.RUNNING);
    }

    @Test
    void testCompleteSetsTraceLatencyCost() {
        AiEvalExperimentRun run = seedId(new AiEvalExperimentRun(1L, 2L), 99L);

        // The lifecycle guard on AiEvalExperimentRun.complete() requires the run to be RUNNING; transition it first so
        // the
        // service-level happy path mirrors the executor's actual call sequence (markRunning → complete).
        run.markRunning();

        when(aiEvalExperimentRunRepository.findById(99L)).thenReturn(Optional.of(run));
        when(aiEvalExperimentRunRepository.save(any(AiEvalExperimentRun.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        BigDecimal cost = new BigDecimal("0.0042");

        AiEvalExperimentRun result = aiEvalExperimentRunService.complete(99L, 1234L, 567, cost);

        assertThat(result.getStatus()).isEqualTo(AiEvalExperimentRunStatus.COMPLETED);
        assertThat(result.getTraceId()).isEqualTo(1234L);
        assertThat(result.getLatencyMs()).isEqualTo(567);
        assertThat(result.getCost()).isEqualTo(cost);
    }

    @Test
    void testFindAllByExperimentDelegatesToOrderedRepositoryQuery() {
        // Pin the contract: the service must call the *Ordered* repository method, not the implicit
        // findAllByExperimentId. Without ORDER BY, PostgreSQL is free to return rows in physical heap order
        // (which can shuffle on autovacuum / page rewrites), and downstream consumers — the executor's
        // positional alignment loop, the GraphQL comparison view, the public REST list — would observe
        // intermittent reorderings that look like data corruption from the user's perspective.
        AiEvalExperimentRun first = seedId(new AiEvalExperimentRun(1L, 100L), 10L);
        AiEvalExperimentRun second = seedId(new AiEvalExperimentRun(1L, 101L), 11L);

        when(aiEvalExperimentRunRepository.findAllByExperimentIdOrderByIdAsc(1L))
            .thenReturn(List.of(first, second));

        List<AiEvalExperimentRun> result = aiEvalExperimentRunService.findAllByExperiment(1L);

        verify(aiEvalExperimentRunRepository).findAllByExperimentIdOrderByIdAsc(1L);

        assertThat(result).extracting(AiEvalExperimentRun::getId)
            .containsExactly(10L, 11L);
    }

    @Test
    void testFailSetsErrorMessage() {
        AiEvalExperimentRun run = seedId(new AiEvalExperimentRun(1L, 2L), 99L);

        when(aiEvalExperimentRunRepository.findById(99L)).thenReturn(Optional.of(run));
        when(aiEvalExperimentRunRepository.save(any(AiEvalExperimentRun.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        AiEvalExperimentRun result = aiEvalExperimentRunService.fail(99L, "boom");

        assertThat(result.getStatus()).isEqualTo(AiEvalExperimentRunStatus.FAILED);
        assertThat(result.getErrorMessage()).isEqualTo("boom");
    }

    private static AiEvalExperimentRun seedId(AiEvalExperimentRun run, long id) {
        try {
            Field idField = AiEvalExperimentRun.class.getDeclaredField("id");

            idField.setAccessible(true);
            idField.set(run, id);
        } catch (ReflectiveOperationException reflectiveOperationException) {
            throw new AssertionError("failed to seed id", reflectiveOperationException);
        }

        return run;
    }
}
