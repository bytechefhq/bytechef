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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.ai.eval.dataset.domain.AiEvalDataset;
import com.bytechef.ee.platform.ai.eval.dataset.repository.AiEvalDatasetRepository;
import java.lang.reflect.Field;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Ivica Cardic
 * @version ee
 */
@ExtendWith(MockitoExtension.class)
class AiEvalDatasetServiceTest {

    @Mock
    private AiEvalDatasetRepository aiEvalDatasetRepository;

    private AiEvalDatasetServiceImpl aiEvalDatasetService;

    @BeforeEach
    void setUp() {
        aiEvalDatasetService = new AiEvalDatasetServiceImpl(aiEvalDatasetRepository);
    }

    @Test
    void testCreatePersistsNewDataset() {
        AiEvalDataset dataset = new AiEvalDataset("my-dataset");

        AiEvalDataset persisted = new AiEvalDataset("my-dataset");
        seedDatasetId(persisted, 7L);

        when(aiEvalDatasetRepository.save(any(AiEvalDataset.class)))
            .thenReturn(persisted);

        AiEvalDataset created = aiEvalDatasetService.create(dataset);

        ArgumentCaptor<AiEvalDataset> captor = ArgumentCaptor.forClass(AiEvalDataset.class);

        verify(aiEvalDatasetRepository).save(captor.capture());

        assertThat(captor.getValue()).isSameAs(dataset);
        assertThat(created).isSameAs(persisted);
    }

    @Test
    void testCreateRejectsNullDataset() {
        assertThatThrownBy(() -> aiEvalDatasetService.create(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("dataset");

        verify(aiEvalDatasetRepository, never()).save(any(AiEvalDataset.class));
    }

    @Test
    void testCreateRejectsDatasetWithId() {
        AiEvalDataset preIdDataset = new AiEvalDataset("my-dataset");

        seedDatasetId(preIdDataset, 42L);

        assertThatThrownBy(() -> aiEvalDatasetService.create(preIdDataset))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("id must be null");

        verify(aiEvalDatasetRepository, never()).save(any(AiEvalDataset.class));
    }

    @Test
    void testGetDatasetReturnsRow() {
        AiEvalDataset dataset = new AiEvalDataset("my-dataset");

        seedDatasetId(dataset, 42L);

        when(aiEvalDatasetRepository.findById(42L)).thenReturn(Optional.of(dataset));

        AiEvalDataset loaded = aiEvalDatasetService.getDataset(42L);

        assertThat(loaded).isSameAs(dataset);
    }

    @Test
    void testGetDatasetThrowsWhenMissing() {
        when(aiEvalDatasetRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> aiEvalDatasetService.getDataset(99L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("99");
    }

    @Test
    void testArchiveLoadsTogglesAndSaves() {
        AiEvalDataset dataset = new AiEvalDataset("my-dataset");

        seedDatasetId(dataset, 42L);

        when(aiEvalDatasetRepository.findById(42L)).thenReturn(Optional.of(dataset));
        when(aiEvalDatasetRepository.save(any(AiEvalDataset.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        aiEvalDatasetService.archive(42L);

        ArgumentCaptor<AiEvalDataset> captor = ArgumentCaptor.forClass(AiEvalDataset.class);

        verify(aiEvalDatasetRepository, times(1)).save(captor.capture());

        assertThat(captor.getValue()).isSameAs(dataset);
        assertThat(captor.getValue()
            .getArchivedDate()).isNotNull();
    }

    @Test
    void testArchiveThrowsWhenDatasetMissing() {
        when(aiEvalDatasetRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> aiEvalDatasetService.archive(99L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("99");

        verify(aiEvalDatasetRepository, never()).save(any(AiEvalDataset.class));
    }

    @Test
    void testUpdatePreChecksExistsBeforeSaving() {
        AiEvalDataset dataset = new AiEvalDataset("renamed");

        seedDatasetId(dataset, 42L);

        when(aiEvalDatasetRepository.existsById(42L)).thenReturn(true);
        when(aiEvalDatasetRepository.save(dataset)).thenReturn(dataset);

        AiEvalDataset updated = aiEvalDatasetService.update(dataset);

        assertThat(updated).isSameAs(dataset);

        verify(aiEvalDatasetRepository).existsById(42L);
        verify(aiEvalDatasetRepository).save(dataset);
    }

    @Test
    void testUpdateThrowsWhenDatasetMissing() {
        AiEvalDataset dataset = new AiEvalDataset("renamed");

        seedDatasetId(dataset, 99L);

        when(aiEvalDatasetRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> aiEvalDatasetService.update(dataset))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("99");

        verify(aiEvalDatasetRepository, never()).save(any(AiEvalDataset.class));
    }

    @Test
    void testUpdateRejectsNullDataset() {
        assertThatThrownBy(() -> aiEvalDatasetService.update(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("dataset");

        verify(aiEvalDatasetRepository, never()).save(any(AiEvalDataset.class));
    }

    @Test
    void testUpdateRejectsDatasetWithoutId() {
        AiEvalDataset noIdDataset = new AiEvalDataset("renamed");

        assertThatThrownBy(() -> aiEvalDatasetService.update(noIdDataset))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("id");

        verify(aiEvalDatasetRepository, never()).existsById(any());
        verify(aiEvalDatasetRepository, never()).save(any(AiEvalDataset.class));
    }

    private static void seedDatasetId(AiEvalDataset dataset, long id) {
        try {
            Field idField = AiEvalDataset.class.getDeclaredField("id");

            idField.setAccessible(true);
            idField.set(dataset, id);
        } catch (ReflectiveOperationException reflectiveOperationException) {
            throw new AssertionError("failed to seed id", reflectiveOperationException);
        }
    }
}
