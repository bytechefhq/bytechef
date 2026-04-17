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

package com.bytechef.component.ai.vectorstore.knowledgebase.cluster;

import static com.bytechef.automation.knowledgebase.constant.KnowledgeBaseConstants.METADATA_KNOWLEDGE_BASE_DOCUMENT_ID;
import static com.bytechef.automation.knowledgebase.constant.KnowledgeBaseConstants.METADATA_KNOWLEDGE_BASE_ID;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;

/**
 * A wrapper around the system's VectorStore that filters queries by knowledge_base_id.
 *
 * @author Ivica Cardic
 */
public class KnowledgeBaseVectorStoreWrapper implements VectorStore {

    private final VectorStore vectorStore;
    private final Long knowledgeBaseId;
    private final List<Long> documentIds;

    public KnowledgeBaseVectorStoreWrapper(VectorStore vectorStore, Long knowledgeBaseId) {
        this(vectorStore, knowledgeBaseId, null);
    }

    @SuppressFBWarnings("EI")
    public KnowledgeBaseVectorStoreWrapper(VectorStore vectorStore, Long knowledgeBaseId, List<Long> documentIds) {
        this.vectorStore = vectorStore;
        this.knowledgeBaseId = knowledgeBaseId;
        this.documentIds = documentIds == null ? null : List.copyOf(documentIds);
    }

    @Override
    public void add(List<Document> documents) {
        List<Document> wrappedDocuments = documents.stream()
            .map(document -> {
                Map<String, Object> metadata = new HashMap<>(document.getMetadata());

                metadata.put(METADATA_KNOWLEDGE_BASE_ID, knowledgeBaseId);

                return new Document(document.getId(), document.getText(), metadata);
            })
            .toList();

        vectorStore.add(wrappedDocuments);
    }

    @Override
    public void delete(List<String> idList) {
        vectorStore.delete(idList);
    }

    @Override
    public void delete(Filter.Expression filterExpression) {
        Filter.Expression combinedExpression = new Filter.Expression(
            Filter.ExpressionType.AND,
            new Filter.Expression(
                Filter.ExpressionType.EQ,
                new Filter.Key(METADATA_KNOWLEDGE_BASE_ID),
                new Filter.Value(knowledgeBaseId)),
            filterExpression);

        vectorStore.delete(combinedExpression);
    }

    @Override
    public List<Document> similaritySearch(SearchRequest request) {
        FilterExpressionBuilder filterExpressionBuilder = new FilterExpressionBuilder();

        Filter.Expression combinedFilter = filterExpressionBuilder
            .eq(METADATA_KNOWLEDGE_BASE_ID, knowledgeBaseId)
            .build();

        if (documentIds != null && !documentIds.isEmpty()) {
            combinedFilter = new Filter.Expression(
                Filter.ExpressionType.AND, combinedFilter, buildDocumentIdFilter(documentIds));
        }

        if (request.getFilterExpression() != null) {
            combinedFilter = new Filter.Expression(
                Filter.ExpressionType.AND, combinedFilter, request.getFilterExpression());
        }

        SearchRequest filteredRequest = SearchRequest.builder()
            .query(request.getQuery())
            .topK(request.getTopK())
            .similarityThreshold(request.getSimilarityThreshold())
            .filterExpression(combinedFilter)
            .build();

        return vectorStore.similaritySearch(filteredRequest);
    }

    /**
     * Builds an IN filter expression on {@code knowledge_base_document_id}. Documents matching ANY of the supplied IDs
     * will satisfy the expression.
     */
    public static Filter.Expression buildDocumentIdFilter(List<Long> documentIds) {
        FilterExpressionBuilder filterExpressionBuilder = new FilterExpressionBuilder();

        return filterExpressionBuilder.in(METADATA_KNOWLEDGE_BASE_DOCUMENT_ID, documentIds.toArray())
            .build();
    }

    @Override
    public String getName() {
        return "KnowledgeBase-" + knowledgeBaseId;
    }
}
