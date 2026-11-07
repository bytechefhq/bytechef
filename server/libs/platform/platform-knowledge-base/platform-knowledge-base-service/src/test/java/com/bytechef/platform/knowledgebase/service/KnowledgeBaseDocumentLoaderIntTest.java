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

package com.bytechef.platform.knowledgebase.service;

import static com.bytechef.platform.knowledgebase.constant.KnowledgeBaseConstants.METADATA_KNOWLEDGE_BASE_DOCUMENT_CHUNK_ID;
import static com.bytechef.platform.knowledgebase.constant.KnowledgeBaseConstants.METADATA_KNOWLEDGE_BASE_DOCUMENT_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.platform.knowledgebase.config.KnowledgeBaseDocumentLoaderIntTestConfiguration;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBase;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseDocument;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseDocumentChunk;
import com.bytechef.platform.knowledgebase.repository.KnowledgeBaseDocumentChunkRepository;
import com.bytechef.platform.knowledgebase.repository.KnowledgeBaseDocumentRepository;
import com.bytechef.platform.knowledgebase.repository.KnowledgeBaseRepository;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseDocumentLoader.Chunk;
import com.bytechef.platform.knowledgebase.vectorstore.KnowledgeBaseVectorStoreWrapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = KnowledgeBaseDocumentLoaderIntTestConfiguration.class,
    properties = {
        "spring.ai.vectorstore.pgvector.dimensions=1536", "spring.ai.vectorstore.pgvector.index-type=hnsw",
        "spring.ai.vectorstore.pgvector.distance-type=cosine_distance"
    })
class KnowledgeBaseDocumentLoaderIntTest {

    @Autowired
    private KnowledgeBaseDocumentChunkRepository knowledgeBaseDocumentChunkRepository;

    @Autowired
    private KnowledgeBaseDocumentChunkService knowledgeBaseDocumentChunkService;

    @Autowired
    private KnowledgeBaseDocumentLoader knowledgeBaseDocumentLoader;

    @Autowired
    private KnowledgeBaseDocumentRepository knowledgeBaseDocumentRepository;

    @Autowired
    private KnowledgeBaseDocumentService knowledgeBaseDocumentService;

    @Autowired
    private KnowledgeBaseRepository knowledgeBaseRepository;

    @Autowired
    @Qualifier("knowledgeBasePgVectorStore")
    private VectorStore vectorStore;

    private long knowledgeBaseId;

    @AfterEach
    void afterEach() {
        knowledgeBaseDocumentChunkRepository.deleteAll();
        knowledgeBaseDocumentRepository.deleteAll();
        knowledgeBaseRepository.deleteAll();
    }

    @BeforeEach
    void beforeEach() {
        KnowledgeBase knowledgeBase = new KnowledgeBase();

        knowledgeBase.setName("Loader KnowledgeBase");

        knowledgeBaseId = knowledgeBaseRepository.save(knowledgeBase)
            .getId();
    }

    @Test
    void testLoadStoresTheDocumentItsChunksVectorsAndContentAndMarksItReady() {
        long knowledgeBaseDocumentId = knowledgeBaseDocumentLoader.load(
            knowledgeBaseId, null, "guide.txt",
            List.of(
                new Chunk(null, "first chunk about invoices", Map.of("page", 1)),
                new Chunk(null, "second chunk about refunds", Map.of("page", 2))),
            Map.of());

        KnowledgeBaseDocument knowledgeBaseDocument = knowledgeBaseDocumentService.getKnowledgeBaseDocument(
            knowledgeBaseDocumentId);

        assertThat(knowledgeBaseDocument.getStatus()).isEqualTo(KnowledgeBaseDocument.STATUS_READY);
        assertThat(knowledgeBaseDocument.getName()).isEqualTo("guide.txt");

        List<KnowledgeBaseDocumentChunk> knowledgeBaseDocumentChunks =
            knowledgeBaseDocumentChunkService.getKnowledgeBaseDocumentChunksByDocumentId(knowledgeBaseDocumentId);

        assertThat(knowledgeBaseDocumentChunks).hasSize(2)
            .allSatisfy(knowledgeBaseDocumentChunk -> {
                assertThat(knowledgeBaseDocumentChunk.getVectorStoreId()).isNotBlank();
                assertThat(knowledgeBaseDocumentChunk.getContent()).isNotNull();
            });

        List<Document> documents = search("first chunk about invoices");

        assertThat(documents.getFirst()
            .getText()).isEqualTo("first chunk about invoices");
        assertThat(((Number) documents.getFirst()
            .getMetadata()
            .get(METADATA_KNOWLEDGE_BASE_DOCUMENT_ID)).longValue()).isEqualTo(knowledgeBaseDocumentId);
    }

    @Test
    void testLoadMarksTheDocumentErrorWhenEmbeddingFailsAndRethrows() {
        assertThatThrownBy(() -> knowledgeBaseDocumentLoader.load(
            knowledgeBaseId, null, "broken.txt",
            List.of(new Chunk(null, KnowledgeBaseDocumentLoaderIntTestConfiguration.FAILING_TEXT, Map.of())),
            Map.of())).isInstanceOf(RuntimeException.class);

        List<KnowledgeBaseDocument> knowledgeBaseDocuments =
            knowledgeBaseDocumentService.getKnowledgeBaseDocuments(knowledgeBaseId);

        assertThat(knowledgeBaseDocuments).singleElement()
            .satisfies(knowledgeBaseDocument -> assertThat(knowledgeBaseDocument.getStatus())
                .isEqualTo(KnowledgeBaseDocument.STATUS_ERROR));
    }

    @Test
    void testUpdateOfAChunkInheritsItsMetadataAndReplacesIt() {
        long knowledgeBaseDocumentId = knowledgeBaseDocumentLoader.load(
            knowledgeBaseId, null, "notes.txt",
            List.of(
                new Chunk(null, "original chunk text", Map.of()),
                new Chunk(null, "untouched chunk text", Map.of())),
            Map.of("source", "a"));

        KnowledgeBaseDocumentChunk originalChunk = knowledgeBaseDocumentChunkService
            .getKnowledgeBaseDocumentChunksByDocumentId(knowledgeBaseDocumentId)
            .stream()
            .filter(knowledgeBaseDocumentChunk -> search("original chunk text").stream()
                .anyMatch(document -> document.getId()
                    .equals(knowledgeBaseDocumentChunk.getVectorStoreId())))
            .findFirst()
            .orElseThrow();

        knowledgeBaseDocumentLoader.update(
            knowledgeBaseId, knowledgeBaseDocumentId, originalChunk.getId(),
            List.of(new Chunk(null, "replacement chunk text", Map.of())), Map.of());

        List<String> texts = search("replacement chunk text").stream()
            .map(Document::getText)
            .toList();

        assertThat(texts).containsExactlyInAnyOrder("replacement chunk text", "untouched chunk text");

        Document replacementDocument = search("replacement chunk text").stream()
            .filter(document -> "replacement chunk text".equals(document.getText()))
            .findFirst()
            .orElseThrow();

        assertThat(replacementDocument.getMetadata()).containsEntry("source", "a");
        assertThat(((Number) replacementDocument.getMetadata()
            .get(METADATA_KNOWLEDGE_BASE_DOCUMENT_CHUNK_ID)).longValue()).isNotEqualTo(originalChunk.getId());
        assertThat(
            knowledgeBaseDocumentChunkService.getKnowledgeBaseDocumentChunksByDocumentId(knowledgeBaseDocumentId))
                .hasSize(2);
    }

    @Test
    void testLoadSetsTheDocumentSizeFromTheChunkTextInUtf8Bytes() {
        long knowledgeBaseDocumentId = knowledgeBaseDocumentLoader.load(
            knowledgeBaseId, null, "sizes.txt",
            List.of(new Chunk(null, "hello", Map.of()), new Chunk(null, "w\u00f6rld", Map.of())), Map.of());

        assertThat(getDocumentSize(knowledgeBaseDocumentId)).isEqualTo(11L);
    }

    @Test
    void testLoadIntoAnExistingDocumentReplacesItsDocumentSize() {
        long knowledgeBaseDocumentId = loadDocumentOfSize("0123456789", 1_000L);

        knowledgeBaseDocumentLoader.load(
            knowledgeBaseId, knowledgeBaseDocumentId, "sizes.txt", List.of(new Chunk(null, "hello", Map.of())),
            Map.of());

        assertThat(getDocumentSize(knowledgeBaseDocumentId)).isEqualTo(5L);
    }

    @Test
    void testUpdateOfAChunkReplacesOnlyThatChunksSize() {
        long knowledgeBaseDocumentId = loadDocumentOfSize("0123456789", 100L);

        KnowledgeBaseDocumentChunk knowledgeBaseDocumentChunk = knowledgeBaseDocumentChunkService
            .getKnowledgeBaseDocumentChunksByDocumentId(knowledgeBaseDocumentId)
            .getFirst();

        knowledgeBaseDocumentLoader.update(
            knowledgeBaseId, knowledgeBaseDocumentId, knowledgeBaseDocumentChunk.getId(),
            List.of(new Chunk(null, "hello", Map.of())), Map.of());

        assertThat(getDocumentSize(knowledgeBaseDocumentId)).isEqualTo(95L);
    }

    private Long getDocumentSize(long knowledgeBaseDocumentId) {
        KnowledgeBaseDocument knowledgeBaseDocument = knowledgeBaseDocumentService.getKnowledgeBaseDocument(
            knowledgeBaseDocumentId);

        return knowledgeBaseDocument.getDocumentSize();
    }

    private long loadDocumentOfSize(String text, long documentSize) {
        long knowledgeBaseDocumentId = knowledgeBaseDocumentLoader.load(
            knowledgeBaseId, null, "sizes.txt", List.of(new Chunk(null, text, Map.of())), Map.of());

        KnowledgeBaseDocument knowledgeBaseDocument = knowledgeBaseDocumentService.getKnowledgeBaseDocument(
            knowledgeBaseDocumentId);

        knowledgeBaseDocument.setDocumentSize(documentSize);

        knowledgeBaseDocumentService.saveKnowledgeBaseDocument(knowledgeBaseDocument);

        return knowledgeBaseDocumentId;
    }

    private List<Document> search(String query) {
        return new KnowledgeBaseVectorStoreWrapper(vectorStore, knowledgeBaseId).similaritySearch(
            SearchRequest.builder()
                .query(query)
                .topK(10)
                .similarityThreshold(0.0)
                .build());
    }
}
