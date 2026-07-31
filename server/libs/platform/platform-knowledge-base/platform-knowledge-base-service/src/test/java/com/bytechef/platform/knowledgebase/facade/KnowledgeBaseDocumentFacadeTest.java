/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.platform.knowledgebase.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBase;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseDocument;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseDocumentChunk;
import com.bytechef.platform.knowledgebase.exception.KnowledgeBaseStorageLimitExceededException;
import com.bytechef.platform.knowledgebase.file.storage.KnowledgeBaseFileStorage;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseDocumentChunkService;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseDocumentService;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseDocumentTagService;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseService;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseStorageService;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseVectorStoreMetadataService;
import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.ApplicationEventPublisher;

/**
 * @author Ivica Cardic
 */
class KnowledgeBaseDocumentFacadeTest {

    private final KnowledgeBaseDocumentChunkService chunkService = mock(KnowledgeBaseDocumentChunkService.class);
    private final KnowledgeBaseFileStorage fileStorage = mock(KnowledgeBaseFileStorage.class);
    private final KnowledgeBaseDocumentService documentService = mock(KnowledgeBaseDocumentService.class);
    private final KnowledgeBaseService knowledgeBaseService = mock(KnowledgeBaseService.class);
    private final KnowledgeBaseStorageService storageService = mock(KnowledgeBaseStorageService.class);
    private final VectorStore vectorStore = mock(VectorStore.class);

    private KnowledgeBaseDocumentFacadeImpl createFacade() {
        return new KnowledgeBaseDocumentFacadeImpl(
            mock(ApplicationEventPublisher.class), chunkService, documentService,
            mock(KnowledgeBaseDocumentTagService.class), fileStorage, knowledgeBaseService,
            mock(KnowledgeBaseVectorStoreMetadataService.class), vectorStore, storageService);
    }

    @Test
    void testCreateBlockedWhenOverLimit() {
        doThrow(new KnowledgeBaseStorageLimitExceededException(2_000L, 1_000L))
            .when(storageService)
            .checkWithinLimit(500);

        assertThatThrownBy(() -> createFacade().createKnowledgeBaseDocument(
            1L, "a.txt", "text/plain", 500, new ByteArrayInputStream(new byte[0])))
                .isInstanceOf(KnowledgeBaseStorageLimitExceededException.class);

        verifyNoInteractions(fileStorage);
    }

    @Test
    void testCreatePersistsDocumentSize() {
        when(fileStorage.storeDocument(eq("a.txt"), any())).thenReturn(mock(FileEntry.class));
        when(documentService.saveKnowledgeBaseDocument(any())).thenAnswer(invocation -> invocation.getArgument(0));

        createFacade().createKnowledgeBaseDocument(
            1L, "a.txt", "text/plain", 500, new ByteArrayInputStream(new byte[0]));

        ArgumentCaptor<KnowledgeBaseDocument> captor = ArgumentCaptor.forClass(KnowledgeBaseDocument.class);

        verify(documentService).saveKnowledgeBaseDocument(captor.capture());

        assertThat(captor.getValue()
            .getDocumentSize())
                .isEqualTo(500L);
    }

    @Test
    void testSweepTombstonedDocumentChunksEvictsVectorsFilesAndRows() {
        KnowledgeBaseDocument documentOne = newTombstonedDocument(10L, 1L);
        KnowledgeBaseDocument documentTwo = newTombstonedDocument(11L, 1L);

        when(documentService.getTombstonedDocuments(5L)).thenReturn(List.of(documentOne, documentTwo));

        FileEntry chunkContent = new FileEntry("chunk-1.txt", "file://chunks/chunk-1.txt");

        KnowledgeBaseDocumentChunk chunkWithVector = newChunk(10L, "vector-1", chunkContent);
        KnowledgeBaseDocumentChunk chunkWithoutVector = newChunk(11L, null, null);

        when(chunkService.getKnowledgeBaseDocumentChunksByDocumentIds(List.of(10L, 11L)))
            .thenReturn(List.of(chunkWithVector, chunkWithoutVector));

        KnowledgeBase knowledgeBase = new KnowledgeBase();

        knowledgeBase.setEnvironment(Environment.DEVELOPMENT);

        when(knowledgeBaseService.getKnowledgeBase(1L)).thenReturn(knowledgeBase);

        int sweptChunks = createFacade().sweepTombstonedDocumentChunks(5L);

        assertThat(sweptChunks).isEqualTo(2);

        verify(vectorStore).delete(List.of("vector-1"));
        verify(fileStorage).deleteChunkContent(chunkContent);
        verify(chunkService).deleteKnowledgeBaseDocumentChunks(List.of(chunkWithVector, chunkWithoutVector));
    }

    @Test
    void testSweepTombstonedDocumentChunksNoTombstonedDocumentsIsNoOp() {
        when(documentService.getTombstonedDocuments(5L)).thenReturn(List.of());

        int sweptChunks = createFacade().sweepTombstonedDocumentChunks(5L);

        assertThat(sweptChunks).isEqualTo(0);

        verifyNoInteractions(chunkService, vectorStore, fileStorage);
    }

    @Test
    void testSweepTombstonedDocumentChunksSkipsVectorStoreWhenNoVectorStoreIds() {
        KnowledgeBaseDocument document = newTombstonedDocument(10L, 1L);

        when(documentService.getTombstonedDocuments(5L)).thenReturn(List.of(document));

        // Chunk rows exist but were never embedded — no vector-store ids to evict.
        KnowledgeBaseDocumentChunk chunk = newChunk(10L, null, null);

        when(chunkService.getKnowledgeBaseDocumentChunksByDocumentIds(List.of(10L))).thenReturn(List.of(chunk));

        int sweptChunks = createFacade().sweepTombstonedDocumentChunks(5L);

        assertThat(sweptChunks).isEqualTo(1);

        verifyNoInteractions(vectorStore, knowledgeBaseService);
        verify(chunkService).deleteKnowledgeBaseDocumentChunks(List.of(chunk));
    }

    private static KnowledgeBaseDocument newTombstonedDocument(long id, long knowledgeBaseId) {
        KnowledgeBaseDocument document = new KnowledgeBaseDocument();

        document.setId(id);
        document.setKnowledgeBaseId(knowledgeBaseId);
        document.setDeletedAt(Instant.parse("2026-07-31T10:00:00Z"));

        return document;
    }

    private static KnowledgeBaseDocumentChunk newChunk(long documentId, String vectorStoreId, FileEntry content) {
        KnowledgeBaseDocumentChunk chunk = new KnowledgeBaseDocumentChunk();

        chunk.setKnowledgeBaseDocumentId(documentId);
        chunk.setVectorStoreId(vectorStoreId);
        chunk.setContent(content);

        return chunk;
    }
}
