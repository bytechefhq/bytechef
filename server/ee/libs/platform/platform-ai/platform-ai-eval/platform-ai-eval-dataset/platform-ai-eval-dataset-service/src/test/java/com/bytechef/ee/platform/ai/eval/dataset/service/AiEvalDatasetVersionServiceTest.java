/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.eval.dataset.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.ai.eval.dataset.domain.AiEvalDatasetVersion;
import com.bytechef.ee.platform.ai.eval.dataset.repository.AiEvalDatasetVersionRepository;
import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DuplicateKeyException;

/**
 * @author Ivica Cardic
 * @version ee
 */
@ExtendWith(MockitoExtension.class)
class AiEvalDatasetVersionServiceTest {

    @Mock
    private AiEvalDatasetVersionRepository aiEvalDatasetVersionRepository;

    private AiEvalDatasetVersionServiceImpl aiEvalDatasetVersionService;

    @BeforeEach
    void setUp() {
        SelfReferenceProvider selfProvider = new SelfReferenceProvider();

        aiEvalDatasetVersionService = new AiEvalDatasetVersionServiceImpl(aiEvalDatasetVersionRepository, selfProvider);

        selfProvider.setTarget(aiEvalDatasetVersionService);
    }

    @Test
    void testCreateFirstVersionIncrementsToOne() {
        long datasetId = 42L;

        when(aiEvalDatasetVersionRepository.findAllByDatasetId(datasetId)).thenReturn(List.of());
        when(aiEvalDatasetVersionRepository.save(any(AiEvalDatasetVersion.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        AiEvalDatasetVersion created = aiEvalDatasetVersionService.createVersion(datasetId, "seed", false);

        assertThat(created.getVersionNumber()).isEqualTo(1);
        assertThat(created.getLabel()).isEqualTo("seed");
        assertThat(created.isFrozen()).isFalse();
        assertThat(created.getDatasetId()).isEqualTo(datasetId);
    }

    @Test
    void testCreateSubsequentVersionIncrements() {
        long datasetId = 42L;

        AiEvalDatasetVersion existingV1 = new AiEvalDatasetVersion(datasetId, 1);
        AiEvalDatasetVersion existingV3 = new AiEvalDatasetVersion(datasetId, 3);
        AiEvalDatasetVersion existingV2 = new AiEvalDatasetVersion(datasetId, 2);

        when(aiEvalDatasetVersionRepository.findAllByDatasetId(datasetId))
            .thenReturn(List.of(existingV1, existingV3, existingV2));
        when(aiEvalDatasetVersionRepository.save(any(AiEvalDatasetVersion.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        AiEvalDatasetVersion created = aiEvalDatasetVersionService.createVersion(datasetId, null, true);

        assertThat(created.getVersionNumber()).isEqualTo(4);
        assertThat(created.isFrozen()).isTrue();
    }

    @Test
    void testFreezeRejectsAlreadyFrozen() {
        AiEvalDatasetVersion frozenVersion = seedVersion(99L, 42L, 1);

        frozenVersion.freeze();

        when(aiEvalDatasetVersionRepository.findById(99L)).thenReturn(Optional.of(frozenVersion));

        assertThatThrownBy(() -> aiEvalDatasetVersionService.freeze(99L))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("already frozen");

        verify(aiEvalDatasetVersionRepository, never()).save(any());
    }

    @Test
    void testGetOrCreateUnfrozenReturnsExistingWhenPresent() {
        long datasetId = 42L;

        AiEvalDatasetVersion existingUnfrozen = seedVersion(77L, datasetId, 5);

        when(aiEvalDatasetVersionRepository.findFirstByDatasetIdAndFrozenFalseOrderByVersionNumberDesc(datasetId))
            .thenReturn(Optional.of(existingUnfrozen));

        AiEvalDatasetVersion result = aiEvalDatasetVersionService.getOrCreateUnfrozenVersion(datasetId);

        assertThat(result).isSameAs(existingUnfrozen);

        verify(aiEvalDatasetVersionRepository, never()).save(any());
        verify(aiEvalDatasetVersionRepository, never()).findAllByDatasetId(any());
    }

    @Test
    void testGetOrCreateUnfrozenCreatesWhenAllFrozen() {
        long datasetId = 42L;

        AiEvalDatasetVersion frozenV1 = new AiEvalDatasetVersion(datasetId, 1);
        AiEvalDatasetVersion frozenV2 = new AiEvalDatasetVersion(datasetId, 2);

        frozenV1.freeze();
        frozenV2.freeze();

        when(aiEvalDatasetVersionRepository.findFirstByDatasetIdAndFrozenFalseOrderByVersionNumberDesc(datasetId))
            .thenReturn(Optional.empty());
        when(aiEvalDatasetVersionRepository.findAllByDatasetId(datasetId)).thenReturn(List.of(frozenV1, frozenV2));
        when(aiEvalDatasetVersionRepository.save(any(AiEvalDatasetVersion.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        AiEvalDatasetVersion created = aiEvalDatasetVersionService.getOrCreateUnfrozenVersion(datasetId);

        assertThat(created.isFrozen()).isFalse();
        assertThat(created.getVersionNumber()).isEqualTo(3);
        assertThat(created.getDatasetId()).isEqualTo(datasetId);
        assertThat(created.getLabel()).isNull();
    }

    @Test
    void testGetOrCreateUnfrozenRefetchesWinnerOnDuplicateKey() {
        long datasetId = 42L;

        AiEvalDatasetVersion winnerVersion = seedVersion(101L, datasetId, 1);

        when(aiEvalDatasetVersionRepository.findFirstByDatasetIdAndFrozenFalseOrderByVersionNumberDesc(datasetId))
            .thenReturn(Optional.empty(), Optional.of(winnerVersion));
        when(aiEvalDatasetVersionRepository.findAllByDatasetId(datasetId)).thenReturn(List.of());
        when(aiEvalDatasetVersionRepository.save(any(AiEvalDatasetVersion.class)))
            .thenThrow(new DuplicateKeyException("ux_ai_eval_dataset_version_unfrozen_per_dataset"));

        AiEvalDatasetVersion result = aiEvalDatasetVersionService.getOrCreateUnfrozenVersion(datasetId);

        assertThat(result).isSameAs(winnerVersion);
    }

    @Test
    void testGetOrCreateUnfrozenRefetchesWinnerOnVersionNumberIndexRace() {
        long datasetId = 42L;

        AiEvalDatasetVersion winnerVersion = seedVersion(101L, datasetId, 1);

        when(aiEvalDatasetVersionRepository.findFirstByDatasetIdAndFrozenFalseOrderByVersionNumberDesc(datasetId))
            .thenReturn(Optional.empty(), Optional.of(winnerVersion));
        when(aiEvalDatasetVersionRepository.findAllByDatasetId(datasetId)).thenReturn(List.of());
        when(aiEvalDatasetVersionRepository.save(any(AiEvalDatasetVersion.class)))
            .thenThrow(new DuplicateKeyException("uk_ai_eval_dataset_version_number"));

        AiEvalDatasetVersion result = aiEvalDatasetVersionService.getOrCreateUnfrozenVersion(datasetId);

        assertThat(result).isSameAs(winnerVersion);
    }

    @Test
    void testGetOrCreateUnfrozenPropagatesUnknownDuplicateKey() {
        long datasetId = 42L;

        when(aiEvalDatasetVersionRepository.findFirstByDatasetIdAndFrozenFalseOrderByVersionNumberDesc(datasetId))
            .thenReturn(Optional.empty());
        when(aiEvalDatasetVersionRepository.findAllByDatasetId(datasetId)).thenReturn(List.of());
        when(aiEvalDatasetVersionRepository.save(any(AiEvalDatasetVersion.class)))
            .thenThrow(new DuplicateKeyException("ux_some_future_unique_index_not_yet_known"));

        assertThatThrownBy(() -> aiEvalDatasetVersionService.getOrCreateUnfrozenVersion(datasetId))
            .isInstanceOf(DuplicateKeyException.class)
            .hasMessageContaining("ux_some_future_unique_index_not_yet_known");
    }

    @Test
    void testGetOrCreateUnfrozenSurfacesIllegalStateWhenRefetchEmpty() {
        long datasetId = 42L;

        when(aiEvalDatasetVersionRepository.findFirstByDatasetIdAndFrozenFalseOrderByVersionNumberDesc(datasetId))
            .thenReturn(Optional.empty());
        when(aiEvalDatasetVersionRepository.findAllByDatasetId(datasetId)).thenReturn(List.of());
        when(aiEvalDatasetVersionRepository.save(any(AiEvalDatasetVersion.class)))
            .thenThrow(new DuplicateKeyException("ux_ai_eval_dataset_version_unfrozen_per_dataset"));

        assertThatThrownBy(() -> aiEvalDatasetVersionService.getOrCreateUnfrozenVersion(datasetId))
            .isInstanceOf(IllegalStateException.class)
            .hasCauseInstanceOf(DuplicateKeyException.class)
            .hasMessageContaining(String.valueOf(datasetId));
    }

    @Test
    void testGetOrCreateUnfrozenRetriesCreateAfterFreezeRacedTheRecoveryWindow() {
        long datasetId = 42L;

        AiEvalDatasetVersion retryWinnerVersion = seedVersion(202L, datasetId, 2);

        when(aiEvalDatasetVersionRepository.findFirstByDatasetIdAndFrozenFalseOrderByVersionNumberDesc(datasetId))
            .thenReturn(Optional.empty(), Optional.empty());
        when(aiEvalDatasetVersionRepository.findAllByDatasetId(datasetId)).thenReturn(List.of());

        when(aiEvalDatasetVersionRepository.save(any(AiEvalDatasetVersion.class)))
            .thenThrow(new DuplicateKeyException("ux_ai_eval_dataset_version_unfrozen_per_dataset"))
            .thenReturn(retryWinnerVersion);

        AiEvalDatasetVersion result = aiEvalDatasetVersionService.getOrCreateUnfrozenVersion(datasetId);

        assertThat(result).isSameAs(retryWinnerVersion);
    }

    @Test
    void testGetOrCreateUnfrozenPropagatesUnknownDuplicateKeyOnRetry() {
        long datasetId = 42L;

        when(aiEvalDatasetVersionRepository.findFirstByDatasetIdAndFrozenFalseOrderByVersionNumberDesc(datasetId))
            .thenReturn(Optional.empty(), Optional.empty());
        when(aiEvalDatasetVersionRepository.findAllByDatasetId(datasetId)).thenReturn(List.of());

        when(aiEvalDatasetVersionRepository.save(any(AiEvalDatasetVersion.class)))
            .thenThrow(new DuplicateKeyException("ux_ai_eval_dataset_version_unfrozen_per_dataset"))
            .thenThrow(new DuplicateKeyException("ux_some_future_constraint_added_by_migration"));

        assertThatThrownBy(() -> aiEvalDatasetVersionService.getOrCreateUnfrozenVersion(datasetId))
            .isInstanceOf(DuplicateKeyException.class)
            .hasMessageContaining("ux_some_future_constraint_added_by_migration");
    }

    private static final class SelfReferenceProvider implements ObjectProvider<AiEvalDatasetVersionService> {

        private AiEvalDatasetVersionService target;

        void setTarget(AiEvalDatasetVersionService target) {
            this.target = target;
        }

        @Override
        public AiEvalDatasetVersionService getObject() throws BeansException {
            return target;
        }

        @Override
        public AiEvalDatasetVersionService getObject(Object... args) throws BeansException {
            return target;
        }

        @Override
        public AiEvalDatasetVersionService getIfAvailable() throws BeansException {
            return target;
        }

        @Override
        public AiEvalDatasetVersionService getIfUnique() throws BeansException {
            return target;
        }

        @Override
        public Iterator<AiEvalDatasetVersionService> iterator() {
            return target == null ? List.<AiEvalDatasetVersionService>of()
                .iterator()
                : List.of(target)
                    .iterator();
        }

        @Override
        public void forEach(Consumer<? super AiEvalDatasetVersionService> action) {
            if (target != null) {
                action.accept(target);
            }
        }
    }

    private static AiEvalDatasetVersion seedVersion(long id, long datasetId, int versionNumber) {
        AiEvalDatasetVersion version = new AiEvalDatasetVersion(datasetId, versionNumber);

        try {
            Field idField = AiEvalDatasetVersion.class.getDeclaredField("id");

            idField.setAccessible(true);
            idField.set(version, id);
        } catch (ReflectiveOperationException reflectiveOperationException) {
            throw new AssertionError("failed to seed id", reflectiveOperationException);
        }

        return version;
    }
}
