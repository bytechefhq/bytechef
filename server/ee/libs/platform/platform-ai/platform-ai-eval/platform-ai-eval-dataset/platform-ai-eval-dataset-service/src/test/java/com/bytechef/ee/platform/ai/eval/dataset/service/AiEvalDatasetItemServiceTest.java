/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.eval.dataset.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.ai.eval.dataset.domain.AiEvalDatasetItem;
import com.bytechef.ee.platform.ai.eval.dataset.domain.AiEvalDatasetVersion;
import com.bytechef.ee.platform.ai.eval.dataset.repository.AiEvalDatasetItemRepository;
import java.lang.reflect.Field;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link AiEvalDatasetItemServiceImpl}. Verifies the copy-on-freeze auto-create delegation and the
 * provenance-preserving {@code addItemFromTrace} path. Cross-tenant trace lookups are NOT exercised here — the platform
 * service is workspace-agnostic; the REST controller in automation-ai-gateway-dataset-public-rest owns the trace fetch
 * + workspace assertion before calling this service.
 *
 * @author Ivica Cardic
 * @version ee
 */
@ExtendWith(MockitoExtension.class)
class AiEvalDatasetItemServiceTest {

    @Mock
    private AiEvalDatasetItemRepository aiEvalDatasetItemRepository;

    @Mock
    private AiEvalDatasetVersionService aiEvalDatasetVersionService;

    private AiEvalDatasetItemServiceImpl aiEvalDatasetItemService;

    @BeforeEach
    void setUp() {
        aiEvalDatasetItemService = new AiEvalDatasetItemServiceImpl(
            aiEvalDatasetItemRepository, aiEvalDatasetVersionService);
    }

    @Test
    void testAddItemAutoCreatesUnfrozenVersion() {
        long datasetId = 42L;

        AiEvalDatasetVersion unfrozenVersion = seedVersionId(new AiEvalDatasetVersion(datasetId, 1), 7L);

        when(aiEvalDatasetVersionService.getOrCreateUnfrozenVersion(datasetId)).thenReturn(unfrozenVersion);
        when(aiEvalDatasetItemRepository.save(any(AiEvalDatasetItem.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        AiEvalDatasetItem savedItem = aiEvalDatasetItemService.addItem(datasetId, "hello", "world", "{\"tag\":\"t1\"}");

        // Delegation to the version service is the invariant — that's what enforces "never insert into a frozen
        // version". Verify it's called and the saved item points at the returned version's id.
        verify(aiEvalDatasetVersionService).getOrCreateUnfrozenVersion(datasetId);

        ArgumentCaptor<AiEvalDatasetItem> itemCaptor = ArgumentCaptor.forClass(AiEvalDatasetItem.class);

        verify(aiEvalDatasetItemRepository).save(itemCaptor.capture());

        AiEvalDatasetItem persisted = itemCaptor.getValue();

        assertThat(persisted.getDatasetId()).isEqualTo(datasetId);
        assertThat(persisted.getDatasetVersionId()).isEqualTo(7L);
        assertThat(persisted.getInput()).isEqualTo("hello");
        assertThat(persisted.getExpectedOutput()).isEqualTo("world");
        assertThat(persisted.getMetadata()).isEqualTo("{\"tag\":\"t1\"}");
        assertThat(persisted.getSourceTraceId()).isNull();
        assertThat(savedItem).isSameAs(persisted);
    }

    @Test
    void testAddItemFromTraceCopiesInputAndSetsSourceTraceId() {
        long datasetId = 42L;
        long traceId = 555L;
        String traceInput = "{\"messages\":[{\"role\":\"user\",\"content\":\"what is 2+2\"}]}";

        AiEvalDatasetVersion unfrozenVersion = seedVersionId(new AiEvalDatasetVersion(datasetId, 1), 9L);

        when(aiEvalDatasetVersionService.getOrCreateUnfrozenVersion(datasetId)).thenReturn(unfrozenVersion);
        when(aiEvalDatasetItemRepository.save(any(AiEvalDatasetItem.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        AiEvalDatasetItem result = aiEvalDatasetItemService.addItemFromTrace(
            datasetId, traceId, traceInput, "4", "{\"topic\":\"math\"}");

        ArgumentCaptor<AiEvalDatasetItem> itemCaptor = ArgumentCaptor.forClass(AiEvalDatasetItem.class);

        verify(aiEvalDatasetItemRepository).save(itemCaptor.capture());

        AiEvalDatasetItem persisted = itemCaptor.getValue();

        assertThat(persisted.getInput()).isEqualTo(traceInput);
        assertThat(persisted.getExpectedOutput()).isEqualTo("4");
        assertThat(persisted.getMetadata()).isEqualTo("{\"topic\":\"math\"}");
        assertThat(persisted.getDatasetVersionId()).isEqualTo(9L);
        // Provenance must be stamped atomically with the insert — no second save.
        assertThat(persisted.getSourceTraceId()).isEqualTo(traceId);
        assertThat(result).isSameAs(persisted);
    }

    private static AiEvalDatasetVersion seedVersionId(AiEvalDatasetVersion version, long id) {
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
