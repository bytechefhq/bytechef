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

package com.bytechef.component.ai.vectorstore.knowledgebase.util;

import static com.bytechef.component.ai.vectorstore.knowledgebase.constant.KnowledgeBaseVectorStoreConstants.KNOWLEDGE_BASE_DOCUMENT_CHUNK_ID;
import static com.bytechef.component.ai.vectorstore.knowledgebase.constant.KnowledgeBaseVectorStoreConstants.KNOWLEDGE_BASE_DOCUMENT_ID;
import static com.bytechef.component.ai.vectorstore.knowledgebase.constant.KnowledgeBaseVectorStoreConstants.KNOWLEDGE_BASE_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.ai.vectorstore.VectorStore;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBase;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseDocument;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseDocumentChunk;
import com.bytechef.platform.knowledgebase.file.storage.KnowledgeBaseFileStorage;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseDocumentChunkService;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseDocumentService;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseService;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.vectorstore.SearchRequest;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class KnowledgeBaseVectorStoreTest {

    private final KnowledgeBaseDocumentChunkService knowledgeBaseDocumentChunkService =
        mock(KnowledgeBaseDocumentChunkService.class);
    private final KnowledgeBaseDocumentService knowledgeBaseDocumentService = mock(KnowledgeBaseDocumentService.class);
    private final KnowledgeBaseFileStorage knowledgeBaseFileStorage = mock(KnowledgeBaseFileStorage.class);
    private final KnowledgeBaseService knowledgeBaseService = mock(KnowledgeBaseService.class);
    private final org.springframework.ai.vectorstore.VectorStore springVectorStore =
        mock(org.springframework.ai.vectorstore.VectorStore.class);

    private VectorStore vectorStore;

    @BeforeEach
    void beforeEach() {
        when(knowledgeBaseService.getKnowledgeBase(anyLong())).thenReturn(new KnowledgeBase());
        when(knowledgeBaseDocumentService.saveKnowledgeBaseDocument(any())).thenAnswer(invocation -> {
            KnowledgeBaseDocument knowledgeBaseDocument = invocation.getArgument(0);

            if (knowledgeBaseDocument.getId() == null) {
                knowledgeBaseDocument.setId(10L);
            }

            return knowledgeBaseDocument;
        });
        when(knowledgeBaseDocumentChunkService.saveKnowledgeBaseDocumentChunk(any())).thenAnswer(invocation -> {
            KnowledgeBaseDocumentChunk knowledgeBaseDocumentChunk = invocation.getArgument(0);

            knowledgeBaseDocumentChunk.setId(20L);

            return knowledgeBaseDocumentChunk;
        });
        when(knowledgeBaseFileStorage.storeChunkContent(anyLong(), anyString())).thenReturn(mock(FileEntry.class));

        vectorStore = KnowledgeBaseVectorStore.createVectorStore(
            knowledgeBaseDocumentChunkService, knowledgeBaseDocumentService, knowledgeBaseFileStorage,
            knowledgeBaseService, springVectorStore);
    }

    @Test
    void testLoadSetsDocumentSizeFromChunkText() {
        vectorStore.load(
            ParametersFactory.create(Map.of(KNOWLEDGE_BASE_ID, 1L)), ParametersFactory.create(Map.of()), null,
            reader("hello", "wörld"), List.of());

        assertThat(getFirstSavedDocument().getDocumentSize()).isEqualTo(11L);
    }

    @Test
    void testLoadIntoExistingDocumentReplacesDocumentSize() {
        when(knowledgeBaseDocumentService.getKnowledgeBaseDocument(5L)).thenReturn(existingDocument(1_000L));

        vectorStore.load(
            ParametersFactory.create(Map.of(KNOWLEDGE_BASE_ID, 1L, KNOWLEDGE_BASE_DOCUMENT_ID, 5L)),
            ParametersFactory.create(Map.of()), null, reader("hello"), List.of());

        assertThat(getFirstSavedDocument().getDocumentSize()).isEqualTo(5L);
    }

    @Test
    void testChunkUpdateReplacesOnlyTheChunkSize() {
        when(knowledgeBaseDocumentService.getKnowledgeBaseDocument(5L)).thenReturn(existingDocument(100L));
        when(springVectorStore.similaritySearch(any(SearchRequest.class)))
            .thenReturn(List.of(new Document("old-chunk", "0123456789", Map.of())));

        vectorStore.update(
            ParametersFactory.create(
                Map.of(KNOWLEDGE_BASE_ID, 1L, KNOWLEDGE_BASE_DOCUMENT_ID, 5L, KNOWLEDGE_BASE_DOCUMENT_CHUNK_ID, 7L)),
            ParametersFactory.create(Map.of()), null, reader("hello"), List.of());

        assertThat(getFirstSavedDocument().getDocumentSize()).isEqualTo(95L);
    }

    private static KnowledgeBaseDocument existingDocument(long documentSize) {
        KnowledgeBaseDocument knowledgeBaseDocument = new KnowledgeBaseDocument();

        knowledgeBaseDocument.setId(5L);
        knowledgeBaseDocument.setKnowledgeBaseId(1L);
        knowledgeBaseDocument.setDocumentSize(documentSize);

        return knowledgeBaseDocument;
    }

    private KnowledgeBaseDocument getFirstSavedDocument() {
        ArgumentCaptor<KnowledgeBaseDocument> captor = ArgumentCaptor.forClass(KnowledgeBaseDocument.class);

        verify(knowledgeBaseDocumentService, atLeastOnce()).saveKnowledgeBaseDocument(captor.capture());

        return captor.getAllValues()
            .getFirst();
    }

    private static DocumentReader reader(String... texts) {
        List<Document> documents = Arrays.stream(texts)
            .map(Document::new)
            .toList();

        return () -> documents;
    }
}
