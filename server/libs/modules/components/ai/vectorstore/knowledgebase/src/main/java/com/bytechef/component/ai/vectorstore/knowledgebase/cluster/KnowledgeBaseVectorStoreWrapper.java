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

import static com.bytechef.automation.knowledgebase.constant.KnowledgeBaseConstants.METADATA_KNOWLEDGE_BASE_ID;
import static com.bytechef.automation.knowledgebase.constant.KnowledgeBaseConstants.METADATA_TAG_IDS;

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
    private final List<Long> tagIds;

    public KnowledgeBaseVectorStoreWrapper(VectorStore vectorStore, Long knowledgeBaseId) {
        this(vectorStore, knowledgeBaseId, null);
    }

    @SuppressFBWarnings("EI")
    public KnowledgeBaseVectorStoreWrapper(VectorStore vectorStore, Long knowledgeBaseId, List<Long> tagIds) {
        this.vectorStore = vectorStore;
        this.knowledgeBaseId = knowledgeBaseId;
        this.tagIds = tagIds == null ? null : List.copyOf(tagIds);
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

        if (tagIds != null && !tagIds.isEmpty()) {
            combinedFilter = new Filter.Expression(Filter.ExpressionType.AND, combinedFilter, buildTagFilter(tagIds));
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
     * Builds an OR-combined filter expression over the tag-id metadata keys. Documents matching ANY of the supplied
     * tags will satisfy the expression.
     */
    public static Filter.Expression buildTagFilter(List<Long> tagIds) {
        FilterExpressionBuilder filterExpressionBuilder = new FilterExpressionBuilder();

        Filter.Expression[] tagExpressions = tagIds.stream()
            .map(tagId -> filterExpressionBuilder.eq(METADATA_TAG_IDS + "_" + tagId, true)
                .build())
            .toArray(Filter.Expression[]::new);

        if (tagExpressions.length == 1) {
            return tagExpressions[0];
        }

        Filter.Expression result = tagExpressions[0];

        for (int i = 1; i < tagExpressions.length; i++) {
            result = new Filter.Expression(Filter.ExpressionType.OR, result, tagExpressions[i]);
        }

        return result;
    }

    @Override
    public String getName() {
        return "KnowledgeBase-" + knowledgeBaseId;
    }
}
