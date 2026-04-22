/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.eval.experiment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.ai.eval.experiment.domain.AiEvalExperiment;
import com.bytechef.ee.platform.ai.eval.experiment.domain.AiEvalExperimentStatus;
import com.bytechef.ee.platform.ai.eval.experiment.repository.AiEvalExperimentRepository;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link AiEvalExperimentServiceImpl}. Covers workspace-agnostic create + the lifecycle transitions
 * ({@link AiEvalExperimentServiceImpl#markRunning} / {@link AiEvalExperimentServiceImpl#markFinished} /
 * {@link AiEvalExperimentServiceImpl#requestStop}). Workspace-aware delegation (createInWorkspace, findAllByWorkspace,
 * getWorkspaceId) is owned by
 * {@code com.bytechef.ee.automation.ai.eval.experiment.service.WorkspaceAiEvalExperimentServiceImpl} and tested there.
 *
 * @author Ivica Cardic
 * @version ee
 */
@ExtendWith(MockitoExtension.class)
class AiEvalExperimentServiceTest {

    @Mock
    private AiEvalExperimentRepository aiEvalExperimentRepository;

    private AiEvalExperimentServiceImpl aiEvalExperimentService;

    @BeforeEach
    void setUp() {
        aiEvalExperimentService = new AiEvalExperimentServiceImpl(aiEvalExperimentRepository);
    }

    @Test
    void testCreateReturnsPersistedExperiment() {
        AiEvalExperiment experiment = new AiEvalExperiment(2L);

        AiEvalExperiment persisted = seedId(new AiEvalExperiment(2L), 42L);

        when(aiEvalExperimentRepository.save(any(AiEvalExperiment.class)))
            .thenReturn(persisted);

        AiEvalExperiment saved = aiEvalExperimentService.create(experiment);

        verify(aiEvalExperimentRepository).save(experiment);

        assertThat(saved).isSameAs(persisted);
        assertThat(saved.getStatus()).isEqualTo(AiEvalExperimentStatus.PENDING);
    }

    @Test
    void testMarkRunningTransitionsStatus() {
        AiEvalExperiment experiment = seedId(new AiEvalExperiment(2L), 42L);

        when(aiEvalExperimentRepository.findById(42L)).thenReturn(Optional.of(experiment));
        when(aiEvalExperimentRepository.save(any(AiEvalExperiment.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        AiEvalExperiment result = aiEvalExperimentService.markRunning(42L);

        ArgumentCaptor<AiEvalExperiment> captor = ArgumentCaptor.forClass(AiEvalExperiment.class);

        verify(aiEvalExperimentRepository).save(captor.capture());

        AiEvalExperiment persisted = captor.getValue();

        assertThat(persisted.getStatus()).isEqualTo(AiEvalExperimentStatus.RUNNING);
        assertThat(persisted.getStartedDate()).isNotNull();
        assertThat(result).isSameAs(persisted);
    }

    @Test
    void testMarkFinishedSetsCompletedWhenNoFailures() {
        AiEvalExperiment experiment = seedId(new AiEvalExperiment(2L), 42L);

        experiment.start();

        when(aiEvalExperimentRepository.findById(42L)).thenReturn(Optional.of(experiment));
        when(aiEvalExperimentRepository.save(any(AiEvalExperiment.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        AiEvalExperiment result = aiEvalExperimentService.markFinished(42L, false);

        assertThat(result.getStatus()).isEqualTo(AiEvalExperimentStatus.COMPLETED);
        assertThat(result.getCompletedDate()).isNotNull();
    }

    @Test
    void testMarkFinishedSetsFailedWhenAnyFailed() {
        AiEvalExperiment experiment = seedId(new AiEvalExperiment(2L), 42L);

        experiment.start();

        when(aiEvalExperimentRepository.findById(42L)).thenReturn(Optional.of(experiment));
        when(aiEvalExperimentRepository.save(any(AiEvalExperiment.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        AiEvalExperiment result = aiEvalExperimentService.markFinished(42L, true);

        assertThat(result.getStatus()).isEqualTo(AiEvalExperimentStatus.FAILED);
        assertThat(result.getCompletedDate()).isNotNull();
    }

    @Test
    void testRequestStopFlipsFlag() {
        AiEvalExperiment experiment = seedId(new AiEvalExperiment(2L), 42L);

        experiment.start();

        when(aiEvalExperimentRepository.findById(42L)).thenReturn(Optional.of(experiment));
        when(aiEvalExperimentRepository.save(any(AiEvalExperiment.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        AiEvalExperiment result = aiEvalExperimentService.requestStop(42L);

        ArgumentCaptor<AiEvalExperiment> captor = ArgumentCaptor.forClass(AiEvalExperiment.class);

        verify(aiEvalExperimentRepository).save(captor.capture());

        AiEvalExperiment persisted = captor.getValue();

        assertThat(persisted.isStopRequested()).isTrue();
        assertThat(persisted.getStopRequestedDate()).isNotNull();
        // Status is NOT transitioned here — the executor does it when it observes the flag.
        assertThat(persisted.getStatus()).isEqualTo(AiEvalExperimentStatus.RUNNING);
        assertThat(result).isSameAs(persisted);
    }

    @Test
    void testFindRunningOlderThanDelegatesToRepo() {
        AiEvalExperiment experiment = seedId(new AiEvalExperiment(2L), 42L);

        experiment.start();

        Instant threshold = Instant.now();

        when(aiEvalExperimentRepository.findAllByStatusAndStartedDateBefore(
            eq(AiEvalExperimentStatus.RUNNING.ordinal()), eq(threshold)))
                .thenReturn(List.of(experiment));

        List<AiEvalExperiment> result = aiEvalExperimentService.findRunningOlderThan(threshold);

        assertThat(result).containsExactly(experiment);

        verify(aiEvalExperimentRepository)
            .findAllByStatusAndStartedDateBefore(AiEvalExperimentStatus.RUNNING.ordinal(), threshold);
    }

    private static AiEvalExperiment seedId(AiEvalExperiment experiment, long id) {
        try {
            Field idField = AiEvalExperiment.class.getDeclaredField("id");

            idField.setAccessible(true);
            idField.set(experiment, id);
        } catch (ReflectiveOperationException reflectiveOperationException) {
            throw new AssertionError("failed to seed id", reflectiveOperationException);
        }

        return experiment;
    }
}
