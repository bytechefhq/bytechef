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

package com.bytechef.automation.knowledgebase.worker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.knowledgebase.domain.KnowledgeBase;
import com.bytechef.automation.knowledgebase.domain.KnowledgeBaseDocument;
import com.bytechef.automation.knowledgebase.domain.KnowledgeBaseDocumentChunk;
import com.bytechef.automation.knowledgebase.event.KnowledgeBaseDocumentChunkEvent;
import com.bytechef.automation.knowledgebase.event.KnowledgeBaseDocumentEvent;
import com.bytechef.automation.knowledgebase.file.storage.KnowledgeBaseFileStorage;
import com.bytechef.automation.knowledgebase.service.KnowledgeBaseDocumentChunkService;
import com.bytechef.automation.knowledgebase.service.KnowledgeBaseDocumentService;
import com.bytechef.automation.knowledgebase.service.KnowledgeBaseService;
import com.bytechef.automation.knowledgebase.worker.etl.KnowledgeBaseEtlPipeline;
import com.bytechef.file.storage.domain.FileEntry;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.core.io.Resource;

/**
 * Integration tests for {@link KnowledgeBaseDocumentProcessWorker}.
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class KnowledgeBaseDocumentProcessWorkerIntTest {

    @Mock
    private KnowledgeBaseDocumentChunkService knowledgeBaseDocumentChunkService;

    @Mock
    private KnowledgeBaseDocumentService knowledgeBaseDocumentService;

    @Mock
    private KnowledgeBaseEtlPipeline knowledgeBaseEtlPipeline;

    @Mock
    private KnowledgeBaseFileStorage knowledgeBaseFileStorage;

    @Mock
    private KnowledgeBaseService knowledgeBaseService;

    private KnowledgeBaseDocumentProcessWorker worker;

    @BeforeEach
    void beforeEach() {
        worker = new KnowledgeBaseDocumentProcessWorker(
            knowledgeBaseDocumentChunkService,
            knowledgeBaseDocumentService,
            knowledgeBaseEtlPipeline,
            knowledgeBaseFileStorage,
            knowledgeBaseService);
    }

    @Test
    void testOnKnowledgeBaseDocumentEvent() {
        Long documentId = 1L;
        Long knowledgeBaseId = 1L;

        KnowledgeBaseDocument document = createMockDocument(documentId, knowledgeBaseId);
        KnowledgeBase knowledgeBase = createMockKnowledgeBase(knowledgeBaseId);

        byte[] documentBytes = "Document content".getBytes(StandardCharsets.UTF_8);
        FileEntry fileEntry = document.getDocument();

        Document aiDocument1 = new Document("Chunk 1 content");
        Document aiDocument2 = new Document("Chunk 2 content");

        List<Document> processedDocuments = List.of(aiDocument1, aiDocument2);

        when(knowledgeBaseDocumentService.getKnowledgeBaseDocument(documentId)).thenReturn(document);
        when(knowledgeBaseService.getKnowledgeBase(knowledgeBaseId)).thenReturn(knowledgeBase);
        when(knowledgeBaseFileStorage.readDocumentToBytes(fileEntry)).thenReturn(documentBytes);
        when(knowledgeBaseEtlPipeline.process(
            any(Resource.class), eq(fileEntry.getMimeType()),
            eq(knowledgeBase.getMinChunkSizeChars()), eq(knowledgeBase.getMaxChunkSize()),
            eq(knowledgeBase.getOverlap())))
                .thenReturn(processedDocuments);

        KnowledgeBaseDocumentChunk savedChunk1 = createMockChunk(1L, documentId);
        KnowledgeBaseDocumentChunk savedChunk2 = createMockChunk(2L, documentId);

        when(knowledgeBaseDocumentChunkService.saveKnowledgeBaseDocumentChunk(any(KnowledgeBaseDocumentChunk.class)))
            .thenReturn(savedChunk1, savedChunk1, savedChunk2, savedChunk2);

        when(knowledgeBaseEtlPipeline.writeChunkToVectorStore(
            any(Document.class), eq(knowledgeBaseId), eq(documentId), anyLong(), any())).thenReturn("vector-store-id-1",
                "vector-store-id-2");

        FileEntry chunkFileEntry1 = new FileEntry("1.txt", "file://test/chunks/1.txt");
        FileEntry chunkFileEntry2 = new FileEntry("2.txt", "file://test/chunks/2.txt");

        when(knowledgeBaseFileStorage.storeChunkContent(eq(1L), eq("Chunk 1 content"))).thenReturn(chunkFileEntry1);
        when(knowledgeBaseFileStorage.storeChunkContent(eq(2L), eq("Chunk 2 content"))).thenReturn(chunkFileEntry2);

        List<Integer> capturedStatuses = new ArrayList<>();

        doAnswer(invocation -> {
            KnowledgeBaseDocument doc = invocation.getArgument(0);

            capturedStatuses.add(doc.getStatus());

            return null;
        }).when(knowledgeBaseDocumentService)
            .saveKnowledgeBaseDocument(any(KnowledgeBaseDocument.class));

        KnowledgeBaseDocumentEvent event = new KnowledgeBaseDocumentEvent(documentId);

        worker.onKnowledgeBaseDocumentEvent(event);

        verify(knowledgeBaseDocumentService, times(2)).saveKnowledgeBaseDocument(any(KnowledgeBaseDocument.class));

        assertThat(capturedStatuses).hasSize(2);
        assertThat(capturedStatuses.get(0)).isEqualTo(KnowledgeBaseDocument.STATUS_PROCESSING);
        assertThat(capturedStatuses.get(1)).isEqualTo(KnowledgeBaseDocument.STATUS_READY);

        verify(knowledgeBaseDocumentChunkService, times(4))
            .saveKnowledgeBaseDocumentChunk(any(KnowledgeBaseDocumentChunk.class));
        verify(knowledgeBaseFileStorage).storeChunkContent(eq(1L), eq("Chunk 1 content"));
        verify(knowledgeBaseFileStorage).storeChunkContent(eq(2L), eq("Chunk 2 content"));
    }

    @Test
    void testOnKnowledgeBaseDocumentEventWithError() {
        Long documentId = 1L;
        Long knowledgeBaseId = 1L;

        KnowledgeBaseDocument document = createMockDocument(documentId, knowledgeBaseId);

        when(knowledgeBaseDocumentService.getKnowledgeBaseDocument(documentId)).thenReturn(document);
        when(knowledgeBaseService.getKnowledgeBase(knowledgeBaseId))
            .thenThrow(new RuntimeException("KnowledgeBase not found"));

        List<Integer> capturedStatuses = new ArrayList<>();

        doAnswer(invocation -> {
            KnowledgeBaseDocument doc = invocation.getArgument(0);

            capturedStatuses.add(doc.getStatus());

            return null;
        }).when(knowledgeBaseDocumentService)
            .saveKnowledgeBaseDocument(any(KnowledgeBaseDocument.class));

        KnowledgeBaseDocumentEvent event = new KnowledgeBaseDocumentEvent(documentId);

        worker.onKnowledgeBaseDocumentEvent(event);

        verify(knowledgeBaseDocumentService, times(2)).saveKnowledgeBaseDocument(any(KnowledgeBaseDocument.class));

        assertThat(capturedStatuses).hasSize(2);
        assertThat(capturedStatuses.get(0)).isEqualTo(KnowledgeBaseDocument.STATUS_PROCESSING);
        assertThat(capturedStatuses.get(1)).isEqualTo(KnowledgeBaseDocument.STATUS_ERROR);
    }

    @Test
    void testOnKnowledgeBaseDocumentEventWithEmptyDocumentList() {
        Long documentId = 1L;
        Long knowledgeBaseId = 1L;

        KnowledgeBaseDocument document = createMockDocument(documentId, knowledgeBaseId);
        KnowledgeBase knowledgeBase = createMockKnowledgeBase(knowledgeBaseId);

        byte[] documentBytes = "Empty content".getBytes(StandardCharsets.UTF_8);
        FileEntry fileEntry = document.getDocument();

        List<Document> emptyDocuments = List.of();

        when(knowledgeBaseDocumentService.getKnowledgeBaseDocument(documentId)).thenReturn(document);
        when(knowledgeBaseService.getKnowledgeBase(knowledgeBaseId)).thenReturn(knowledgeBase);
        when(knowledgeBaseFileStorage.readDocumentToBytes(fileEntry)).thenReturn(documentBytes);
        when(knowledgeBaseEtlPipeline.process(
            any(Resource.class), eq(fileEntry.getMimeType()),
            anyInt(), anyInt(), anyInt())).thenReturn(emptyDocuments);

        List<Integer> capturedStatuses = new ArrayList<>();

        doAnswer(invocation -> {
            KnowledgeBaseDocument doc = invocation.getArgument(0);

            capturedStatuses.add(doc.getStatus());

            return null;
        }).when(knowledgeBaseDocumentService)
            .saveKnowledgeBaseDocument(any(KnowledgeBaseDocument.class));

        KnowledgeBaseDocumentEvent event = new KnowledgeBaseDocumentEvent(documentId);

        worker.onKnowledgeBaseDocumentEvent(event);

        verify(knowledgeBaseDocumentChunkService, never()).saveKnowledgeBaseDocumentChunk(any());
        verify(knowledgeBaseFileStorage, never()).storeChunkContent(anyLong(), anyString());

        verify(knowledgeBaseDocumentService, times(2)).saveKnowledgeBaseDocument(any(KnowledgeBaseDocument.class));

        assertThat(capturedStatuses).hasSize(2);
        assertThat(capturedStatuses.get(1)).isEqualTo(KnowledgeBaseDocument.STATUS_READY);
    }

    @Test
    void testOnKnowledgeBaseDocumentChunkEvent() {
        Long chunkId = 1L;
        Long documentId = 1L;
        Long knowledgeBaseId = 1L;
        String newContent = "Updated chunk content";

        KnowledgeBaseDocumentChunk chunk = createMockChunk(chunkId, documentId);
        KnowledgeBaseDocument document = createMockDocument(documentId, knowledgeBaseId);

        when(knowledgeBaseDocumentChunkService.getKnowledgeBaseDocumentChunk(chunkId)).thenReturn(chunk);
        when(knowledgeBaseDocumentService.getKnowledgeBaseDocument(documentId)).thenReturn(document);

        KnowledgeBaseDocumentChunkEvent event = new KnowledgeBaseDocumentChunkEvent(chunkId, newContent);

        worker.onKnowledgeBaseDocumentChunkEvent(event);

        verify(knowledgeBaseEtlPipeline).processChunkUpdate(
            eq(newContent), eq(knowledgeBaseId), eq(documentId), eq(chunkId), any());
    }

    @Test
    void testOnKnowledgeBaseDocumentChunkEventWithError() {
        Long chunkId = 1L;
        String newContent = "Updated chunk content";

        when(knowledgeBaseDocumentChunkService.getKnowledgeBaseDocumentChunk(chunkId))
            .thenThrow(new RuntimeException("Chunk not found"));

        KnowledgeBaseDocumentChunkEvent event = new KnowledgeBaseDocumentChunkEvent(chunkId, newContent);

        worker.onKnowledgeBaseDocumentChunkEvent(event);

        verify(knowledgeBaseEtlPipeline, never()).processChunkUpdate(anyString(), anyLong(), anyLong(), anyLong(),
            any());
    }

    @Test
    void testOnKnowledgeBaseDocumentEventWithTags() {
        Long documentId = 1L;
        Long knowledgeBaseId = 1L;
        List<Long> tagIds = List.of(1L, 2L, 3L);

        KnowledgeBaseDocument document = createMockDocument(documentId, knowledgeBaseId);

        document.setTagIds(tagIds);

        KnowledgeBase knowledgeBase = createMockKnowledgeBase(knowledgeBaseId);

        byte[] documentBytes = "Document content".getBytes(StandardCharsets.UTF_8);
        FileEntry fileEntry = document.getDocument();

        Document aiDocument = new Document("Chunk content");
        List<Document> processedDocuments = List.of(aiDocument);

        when(knowledgeBaseDocumentService.getKnowledgeBaseDocument(documentId)).thenReturn(document);
        when(knowledgeBaseService.getKnowledgeBase(knowledgeBaseId)).thenReturn(knowledgeBase);
        when(knowledgeBaseFileStorage.readDocumentToBytes(fileEntry)).thenReturn(documentBytes);
        when(knowledgeBaseEtlPipeline.process(any(Resource.class), anyString(), anyInt(), anyInt(), anyInt()))
            .thenReturn(processedDocuments);

        KnowledgeBaseDocumentChunk savedChunk = createMockChunk(1L, documentId);

        when(knowledgeBaseDocumentChunkService.saveKnowledgeBaseDocumentChunk(any(KnowledgeBaseDocumentChunk.class)))
            .thenReturn(savedChunk);
        when(knowledgeBaseEtlPipeline.writeChunkToVectorStore(any(), anyLong(), anyLong(), anyLong(), eq(tagIds)))
            .thenReturn("vector-store-id");
        when(knowledgeBaseFileStorage.storeChunkContent(anyLong(), anyString()))
            .thenReturn(new FileEntry("1.txt", "file://test/1.txt"));

        KnowledgeBaseDocumentEvent event = new KnowledgeBaseDocumentEvent(documentId);

        worker.onKnowledgeBaseDocumentEvent(event);

        verify(knowledgeBaseEtlPipeline).writeChunkToVectorStore(any(), eq(knowledgeBaseId), eq(documentId), eq(1L),
            eq(tagIds));
    }

    private KnowledgeBaseDocument createMockDocument(Long id, Long knowledgeBaseId) {
        KnowledgeBaseDocument document = new KnowledgeBaseDocument();

        document.setId(id);
        document.setKnowledgeBaseId(knowledgeBaseId);
        document.setName("Test Document");
        document.setDocument(new FileEntry("test.txt", "file://test/test.txt"));
        document.setStatus(KnowledgeBaseDocument.STATUS_UPLOADED);

        return document;
    }

    private KnowledgeBase createMockKnowledgeBase(Long id) {
        KnowledgeBase knowledgeBase = new KnowledgeBase();

        knowledgeBase.setId(id);
        knowledgeBase.setName("Test KnowledgeBase");
        knowledgeBase.setMaxChunkSize(1000);
        knowledgeBase.setMinChunkSizeChars(100);
        knowledgeBase.setOverlap(50);

        return knowledgeBase;
    }

    private KnowledgeBaseDocumentChunk createMockChunk(Long id, Long documentId) {
        KnowledgeBaseDocumentChunk chunk = new KnowledgeBaseDocumentChunk();

        chunk.setId(id);
        chunk.setKnowledgeBaseDocumentId(documentId);
        chunk.setVectorStoreId("vector-store-id-" + id);

        return chunk;
    }
}
