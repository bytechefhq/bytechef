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

package com.bytechef.automation.knowledgebase.facade;

import com.bytechef.automation.knowledgebase.domain.KnowledgeBaseDocumentChunk;
import com.bytechef.automation.knowledgebase.file.storage.KnowledgeBaseFileStorage;
import com.bytechef.automation.knowledgebase.service.KnowledgeBaseDocumentChunkService;
import com.bytechef.file.storage.domain.FileEntry;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
@ConditionalOnProperty(prefix = "bytechef.knowledge-base", name = "enabled", havingValue = "true")
class KnowledgeBaseFacadeImpl implements KnowledgeBaseFacade {

    private static final String METADATA_KNOWLEDGE_BASE_ID = "knowledge_base_id";

    private final KnowledgeBaseDocumentChunkService knowledgeBaseDocumentChunkService;
    private final KnowledgeBaseFileStorage knowledgeBaseFileStorage;
    private final ObjectMapper objectMapper;
    private final VectorStore vectorStore;

    @SuppressFBWarnings("EI")
    KnowledgeBaseFacadeImpl(
        KnowledgeBaseDocumentChunkService knowledgeBaseDocumentChunkService,
        KnowledgeBaseFileStorage knowledgeBaseFileStorage, ObjectMapper objectMapper,
        @Qualifier("knowledgeBasePgVectorStore") VectorStore vectorStore) {

        this.knowledgeBaseDocumentChunkService = knowledgeBaseDocumentChunkService;
        this.knowledgeBaseFileStorage = knowledgeBaseFileStorage;
        this.objectMapper = objectMapper;
        this.vectorStore = vectorStore;
    }

    @Override
    @Transactional(readOnly = true)
    public List<KnowledgeBaseDocumentChunk> searchKnowledgeBase(
        Long knowledgeBaseId, String query, String metadataFilters) {

        Filter.Expression filterExpression = buildFilterExpression(knowledgeBaseId, metadataFilters);

        SearchRequest searchRequest = SearchRequest.builder()
            .query(query)
            .topK(10)
            .filterExpression(filterExpression)
            .build();

        List<Document> documents = vectorStore.similaritySearch(searchRequest);

        return documents.stream()
            .map(this::toKnowledgeBaseDocumentChunk)
            .filter(chunk -> chunk.getId() != null)
            .toList();
    }

    private Filter.Expression buildFilterExpression(Long knowledgeBaseId, String metadataFilters) {
        FilterExpressionBuilder builder = new FilterExpressionBuilder();

        FilterExpressionBuilder.Op knowledgeBaseFilter = builder.eq(METADATA_KNOWLEDGE_BASE_ID, knowledgeBaseId);

        if (metadataFilters == null || metadataFilters.isBlank()) {
            return knowledgeBaseFilter.build();
        }

        try {
            Map<String, Object> filters = objectMapper.readValue(
                metadataFilters, new TypeReference<>() {});

            if (filters.isEmpty()) {
                return knowledgeBaseFilter.build();
            }

            FilterExpressionBuilder.Op combinedFilter = knowledgeBaseFilter;

            for (Map.Entry<String, Object> entry : filters.entrySet()) {
                combinedFilter = builder.and(combinedFilter, builder.eq(entry.getKey(), entry.getValue()));
            }

            return combinedFilter.build();
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException(
                "Invalid metadata filters JSON: " + metadataFilters, exception);
        }
    }

    private KnowledgeBaseDocumentChunk toKnowledgeBaseDocumentChunk(Document document) {
        KnowledgeBaseDocumentChunk chunk;

        Map<String, Object> metadata = document.getMetadata();

        Number chunkIdNumber = (Number) metadata.get("knowledge_base_document_chunk_id");

        Long chunkId = chunkIdNumber != null ? chunkIdNumber.longValue() : null;

        if (chunkId != null) {
            chunk = knowledgeBaseDocumentChunkService.getKnowledgeBaseDocumentChunk(chunkId);
        } else {
            // Fallback: look up by vector store ID for documents indexed before chunk ID was added to metadata
            chunk = knowledgeBaseDocumentChunkService.getKnowledgeBaseDocumentChunkByVectorStoreId(document.getId())
                .orElse(null);
        }

        if (chunk != null) {
            FileEntry contentFileEntry = chunk.getContent();

            if (contentFileEntry != null) {
                String textContent = knowledgeBaseFileStorage.readChunkContent(contentFileEntry);

                chunk.setTextContent(textContent);
            }
        } else {
            chunk = new KnowledgeBaseDocumentChunk();

            Number documentIdNumber = (Number) metadata.get("knowledge_base_document_id");

            Long documentId = documentIdNumber != null ? documentIdNumber.longValue() : null;

            chunk.setKnowledgeBaseDocumentId(documentId);
            chunk.setTextContent(document.getText());
        }

        chunk.setMetadata(metadata);

        Double score = document.getScore();

        if (score != null) {
            chunk.setScore(score.floatValue());
        }

        return chunk;
    }
}
