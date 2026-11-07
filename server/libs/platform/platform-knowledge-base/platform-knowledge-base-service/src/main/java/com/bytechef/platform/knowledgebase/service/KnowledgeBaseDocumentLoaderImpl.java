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

import static com.bytechef.platform.knowledgebase.constant.KnowledgeBaseConstants.METADATA_ENVIRONMENT_ID;
import static com.bytechef.platform.knowledgebase.constant.KnowledgeBaseConstants.METADATA_KNOWLEDGE_BASE_DOCUMENT_CHUNK_ID;
import static com.bytechef.platform.knowledgebase.constant.KnowledgeBaseConstants.METADATA_KNOWLEDGE_BASE_DOCUMENT_ID;
import static com.bytechef.platform.knowledgebase.constant.KnowledgeBaseConstants.METADATA_KNOWLEDGE_BASE_ID;

import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBase;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseDocument;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseDocumentChunk;
import com.bytechef.platform.knowledgebase.file.storage.KnowledgeBaseFileStorage;
import com.bytechef.platform.knowledgebase.vectorstore.KnowledgeBaseVectorStoreWrapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;

/**
 * @author Ivica Cardic
 */
public class KnowledgeBaseDocumentLoaderImpl implements KnowledgeBaseDocumentLoader {

    private final KnowledgeBaseDocumentChunkService knowledgeBaseDocumentChunkService;
    private final KnowledgeBaseDocumentService knowledgeBaseDocumentService;
    private final KnowledgeBaseFileStorage knowledgeBaseFileStorage;
    private final KnowledgeBaseService knowledgeBaseService;
    private final VectorStore vectorStore;

    @SuppressFBWarnings("EI")
    public KnowledgeBaseDocumentLoaderImpl(
        KnowledgeBaseDocumentChunkService knowledgeBaseDocumentChunkService,
        KnowledgeBaseDocumentService knowledgeBaseDocumentService, KnowledgeBaseFileStorage knowledgeBaseFileStorage,
        KnowledgeBaseService knowledgeBaseService, VectorStore vectorStore) {

        this.knowledgeBaseDocumentChunkService = knowledgeBaseDocumentChunkService;
        this.knowledgeBaseDocumentService = knowledgeBaseDocumentService;
        this.knowledgeBaseFileStorage = knowledgeBaseFileStorage;
        this.knowledgeBaseService = knowledgeBaseService;
        this.vectorStore = vectorStore;
    }

    @Override
    public long load(
        long knowledgeBaseId, @Nullable Long knowledgeBaseDocumentId, String documentName, List<Chunk> chunks,
        Map<String, Object> additionalMetadata) {

        return load(knowledgeBaseId, knowledgeBaseDocumentId, documentName, chunks, additionalMetadata, null);
    }

    private long load(
        long knowledgeBaseId, @Nullable Long knowledgeBaseDocumentId, String documentName, List<Chunk> chunks,
        Map<String, Object> additionalMetadata, @Nullable Long replacedChunkBytes) {

        KnowledgeBase knowledgeBase = knowledgeBaseService.getKnowledgeBase(knowledgeBaseId);

        VectorStore knowledgeBaseVectorStore = new KnowledgeBaseVectorStoreWrapper(vectorStore, knowledgeBaseId);

        long contentBytes = getContentBytes(chunks);

        KnowledgeBaseDocument knowledgeBaseDocument;

        if (knowledgeBaseDocumentId != null) {
            knowledgeBaseDocument = knowledgeBaseDocumentService.getKnowledgeBaseDocument(knowledgeBaseDocumentId);

            long documentSize = contentBytes;

            if (replacedChunkBytes != null) {
                long previousDocumentSize = Objects.requireNonNullElse(knowledgeBaseDocument.getDocumentSize(), 0L);

                documentSize += Math.max(0, previousDocumentSize - replacedChunkBytes);
            }

            knowledgeBaseDocument.setDocumentSize(documentSize);
        } else {
            knowledgeBaseDocument = new KnowledgeBaseDocument();

            knowledgeBaseDocument.setKnowledgeBaseId(knowledgeBaseId);
            knowledgeBaseDocument.setName(documentName);
            knowledgeBaseDocument.setDocumentSize(contentBytes);
        }

        knowledgeBaseDocument.setStatus(KnowledgeBaseDocument.STATUS_PROCESSING);

        knowledgeBaseDocument = knowledgeBaseDocumentService.saveKnowledgeBaseDocument(knowledgeBaseDocument);

        long savedKnowledgeBaseDocumentId = Objects.requireNonNull(knowledgeBaseDocument.getId());

        try {
            for (Chunk chunk : chunks) {
                storeChunk(
                    knowledgeBase, knowledgeBaseVectorStore, savedKnowledgeBaseDocumentId, chunk, additionalMetadata);
            }

            knowledgeBaseDocument.setStatus(KnowledgeBaseDocument.STATUS_READY);

            knowledgeBaseDocumentService.saveKnowledgeBaseDocument(knowledgeBaseDocument);
        } catch (RuntimeException exception) {
            knowledgeBaseDocument.setStatus(KnowledgeBaseDocument.STATUS_ERROR);

            knowledgeBaseDocumentService.saveKnowledgeBaseDocument(knowledgeBaseDocument);

            throw exception;
        }

        return savedKnowledgeBaseDocumentId;
    }

    @Override
    public long update(
        long knowledgeBaseId, @Nullable Long knowledgeBaseDocumentId, @Nullable Long knowledgeBaseDocumentChunkId,
        List<Chunk> chunks, Map<String, Object> additionalMetadata) {

        VectorStore knowledgeBaseVectorStore = new KnowledgeBaseVectorStoreWrapper(vectorStore, knowledgeBaseId);

        Map<String, Object> loadMetadata = new HashMap<>();

        Long replacedChunkBytes = null;

        if (knowledgeBaseDocumentChunkId != null) {
            List<Document> chunkDocuments = getChunkDocuments(knowledgeBaseVectorStore, knowledgeBaseDocumentChunkId);

            loadMetadata.putAll(getInheritedMetadata(chunkDocuments));

            replacedChunkBytes = getDocumentContentBytes(chunkDocuments);
        }

        loadMetadata.putAll(additionalMetadata);

        FilterExpressionBuilder filterExpressionBuilder = new FilterExpressionBuilder();

        if (knowledgeBaseDocumentChunkId != null) {
            knowledgeBaseDocumentChunkService.deleteKnowledgeBaseDocumentChunk(knowledgeBaseDocumentChunkId);

            knowledgeBaseVectorStore.delete(
                filterExpressionBuilder.eq(METADATA_KNOWLEDGE_BASE_DOCUMENT_CHUNK_ID, knowledgeBaseDocumentChunkId)
                    .build());
        } else if (knowledgeBaseDocumentId != null) {
            knowledgeBaseDocumentChunkService.deleteKnowledgeBaseDocumentChunks(
                knowledgeBaseDocumentChunkService.getKnowledgeBaseDocumentChunksByDocumentId(knowledgeBaseDocumentId));

            knowledgeBaseVectorStore.delete(
                filterExpressionBuilder.eq(METADATA_KNOWLEDGE_BASE_DOCUMENT_ID, knowledgeBaseDocumentId)
                    .build());
        }

        return load(
            knowledgeBaseId, knowledgeBaseDocumentId,
            KnowledgeBaseDocumentLoader.deriveDocumentName(chunks, loadMetadata),
            chunks, loadMetadata, replacedChunkBytes);
    }

    private static List<Document> getChunkDocuments(
        VectorStore knowledgeBaseVectorStore, long knowledgeBaseDocumentChunkId) {

        FilterExpressionBuilder filterExpressionBuilder = new FilterExpressionBuilder();

        Filter.Expression chunkFilterExpression = filterExpressionBuilder
            .eq(METADATA_KNOWLEDGE_BASE_DOCUMENT_CHUNK_ID, knowledgeBaseDocumentChunkId)
            .build();

        return knowledgeBaseVectorStore.similaritySearch(
            SearchRequest.builder()
                .query(" ")
                .topK(1)
                .similarityThreshold(0.0)
                .filterExpression(chunkFilterExpression)
                .build());
    }

    private static long getContentBytes(List<Chunk> chunks) {
        long contentBytes = 0;

        for (Chunk chunk : chunks) {
            contentBytes += getTextBytes(chunk.text());
        }

        return contentBytes;
    }

    private static long getDocumentContentBytes(List<Document> documents) {
        long contentBytes = 0;

        for (Document document : documents) {
            contentBytes += getTextBytes(document.getText());
        }

        return contentBytes;
    }

    private static Map<String, Object> getInheritedMetadata(List<Document> existingDocuments) {
        if (existingDocuments.isEmpty()) {
            return Map.of();
        }

        Document existingDocument = existingDocuments.getFirst();

        Map<String, Object> inheritedMetadata = new HashMap<>(existingDocument.getMetadata());

        inheritedMetadata.remove(METADATA_KNOWLEDGE_BASE_DOCUMENT_CHUNK_ID);

        return inheritedMetadata;
    }

    private static long getTextBytes(@Nullable String text) {
        if (text == null) {
            return 0;
        }

        String sanitizedText = text.replace("\0", "");

        return sanitizedText.getBytes(StandardCharsets.UTF_8).length;
    }

    private void storeChunk(
        KnowledgeBase knowledgeBase, VectorStore knowledgeBaseVectorStore, long knowledgeBaseDocumentId, Chunk chunk,
        Map<String, Object> additionalMetadata) {

        KnowledgeBaseDocumentChunk knowledgeBaseDocumentChunk = new KnowledgeBaseDocumentChunk();

        knowledgeBaseDocumentChunk.setKnowledgeBaseDocumentId(knowledgeBaseDocumentId);

        knowledgeBaseDocumentChunk = knowledgeBaseDocumentChunkService.saveKnowledgeBaseDocumentChunk(
            knowledgeBaseDocumentChunk);

        long knowledgeBaseDocumentChunkId = Objects.requireNonNull(knowledgeBaseDocumentChunk.getId());

        Map<String, Object> metadata = new HashMap<>(chunk.metadata());

        metadata.put(METADATA_ENVIRONMENT_ID, knowledgeBase.getEnvironmentId());
        metadata.put(METADATA_KNOWLEDGE_BASE_ID, knowledgeBase.getId());
        metadata.put(METADATA_KNOWLEDGE_BASE_DOCUMENT_ID, knowledgeBaseDocumentId);
        metadata.put(METADATA_KNOWLEDGE_BASE_DOCUMENT_CHUNK_ID, knowledgeBaseDocumentChunkId);
        metadata.putAll(additionalMetadata);

        String text = chunk.text();

        if (text != null) {
            text = text.replace("\0", "");
        }

        Document document = chunk.id() == null
            ? new Document(text, metadata) : new Document(chunk.id(), text, metadata);

        knowledgeBaseVectorStore.add(List.of(document));

        knowledgeBaseDocumentChunk.setVectorStoreId(document.getId());

        FileEntry chunkContent = knowledgeBaseFileStorage.storeChunkContent(knowledgeBaseDocumentChunkId, text);

        knowledgeBaseDocumentChunk.setContent(chunkContent);

        knowledgeBaseDocumentChunkService.saveKnowledgeBaseDocumentChunk(knowledgeBaseDocumentChunk);
    }
}
