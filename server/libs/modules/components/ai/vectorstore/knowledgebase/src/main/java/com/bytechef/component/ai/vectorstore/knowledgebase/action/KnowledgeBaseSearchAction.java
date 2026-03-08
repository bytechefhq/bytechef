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

package com.bytechef.component.ai.vectorstore.knowledgebase.action;

import static com.bytechef.component.ai.vectorstore.knowledgebase.constant.KnowledgeBaseVectorStoreConstants.KNOWLEDGE_BASE_ID;
import static com.bytechef.component.ai.vectorstore.knowledgebase.constant.KnowledgeBaseVectorStoreConstants.METADATA_TAG_IDS;
import static com.bytechef.component.ai.vectorstore.knowledgebase.constant.KnowledgeBaseVectorStoreConstants.QUERY;
import static com.bytechef.component.ai.vectorstore.knowledgebase.constant.KnowledgeBaseVectorStoreConstants.SIMILARITY_THRESHOLD;
import static com.bytechef.component.ai.vectorstore.knowledgebase.constant.KnowledgeBaseVectorStoreConstants.TAG_IDS;
import static com.bytechef.component.ai.vectorstore.knowledgebase.constant.KnowledgeBaseVectorStoreConstants.TOP_K;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.platform.component.definition.VectorStoreComponentDefinition.SEARCH;

import com.bytechef.automation.knowledgebase.domain.KnowledgeBase;
import com.bytechef.automation.knowledgebase.service.KnowledgeBaseService;
import com.bytechef.component.ai.vectorstore.knowledgebase.cluster.KnowledgeBaseVectorStoreWrapper;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.MultipleConnectionsPerformFunction;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.platform.tag.service.TagService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;

/**
 * Search action for querying the internal knowledge base vector store. Supports three search modes:
 * <ul>
 * <li>Tag-Only Search: Direct retrieval by tags (no vector search)</li>
 * <li>Vector Search Only: Semantic search without tags</li>
 * <li>Combined Tag Filtering + Vector Search: Filter by tags, then semantic search</li>
 * </ul>
 *
 * @author Ivica Cardic
 */
public final class KnowledgeBaseSearchAction {

    private KnowledgeBaseSearchAction() {
    }

    public static ActionDefinition of(
        KnowledgeBaseService knowledgeBaseService, TagService tagService, VectorStore vectorStore) {

        return action(SEARCH)
            .title("Search Data")
            .description(
                "Query data from the knowledge base. Supports three modes: tag-only search, vector search, " +
                    "or combined tag filtering with vector search.")
            .properties(
                integer(KNOWLEDGE_BASE_ID)
                    .label("Knowledge Base")
                    .description("The knowledge base to search.")
                    .options(getKnowledgeBaseOptions(knowledgeBaseService))
                    .required(true),
                string(QUERY)
                    .label("Query")
                    .description(
                        "The search query for semantic similarity search. Leave empty for tag-only search.")
                    .required(false),
                array(TAG_IDS)
                    .label("Tags")
                    .description(
                        "Filter results by tags. Documents with ANY of the selected tags will be returned (OR logic).")
                    .items(integer())
                    .options(getTagOptions(tagService))
                    .required(false),
                integer(TOP_K)
                    .label("Top K")
                    .description("Maximum number of results to return.")
                    .defaultValue(10)
                    .required(false),
                number(SIMILARITY_THRESHOLD)
                    .label("Similarity Threshold")
                    .description(
                        "Minimum similarity score (0.0 to 1.0). Only results with similarity above this " +
                            "threshold will be returned.")
                    .defaultValue(0.0)
                    .required(false))
            .output()
            .perform((MultipleConnectionsPerformFunction) (
                inputParameters, componentConnections, extensions,
                context) -> perform(inputParameters, componentConnections, extensions, context, vectorStore));
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private static Object perform(
        Parameters inputParameters, Map<String, ComponentConnection> componentConnections, Parameters extensions,
        ActionContext context, VectorStore vectorStore) {

        Long knowledgeBaseId = inputParameters.getRequiredLong(KNOWLEDGE_BASE_ID);
        String query = inputParameters.getString(QUERY);
        List<Long> tagIds = inputParameters.getList(TAG_IDS, Long.class);
        int topK = inputParameters.getInteger(TOP_K, 10);
        double similarityThreshold = inputParameters.getDouble(SIMILARITY_THRESHOLD, 0.0);

        KnowledgeBaseVectorStoreWrapper wrappedVectorStore = new KnowledgeBaseVectorStoreWrapper(
            vectorStore, knowledgeBaseId);

        boolean hasQuery = query != null && !query.isBlank();
        boolean hasTags = tagIds != null && !tagIds.isEmpty();

        if (!hasQuery && !hasTags) {
            // Neither query nor tags provided - return empty result
            return List.of();
        }

        if (!hasQuery) {
            // Tag-only search: retrieve documents that match any of the specified tags
            return searchByTagsOnly(wrappedVectorStore, tagIds, topK);
        }

        if (!hasTags) {
            // Vector search only: semantic similarity search without tag filtering
            return searchByVectorOnly(wrappedVectorStore, query, topK, similarityThreshold);
        }

        // Combined: filter by tags, then perform vector search
        return searchWithTagFilter(wrappedVectorStore, query, tagIds, topK, similarityThreshold);
    }

    /**
     * Tag-only search: retrieves documents that match any of the specified tags (OR logic). Uses a dummy query with tag
     * filtering since vector stores require a query.
     */
    private static List<Document> searchByTagsOnly(VectorStore vectorStore, List<Long> tagIds, int topK) {
        Filter.Expression tagFilter = buildTagFilter(tagIds);

        // For tag-only search, we use an empty query but rely on the filter
        // Note: The vector store will still compute embeddings, but filtering will narrow results
        SearchRequest searchRequest = SearchRequest.builder()
            .query("")
            .topK(topK)
            .similarityThreshold(0.0)
            .filterExpression(tagFilter)
            .build();

        return vectorStore.similaritySearch(searchRequest);
    }

    /**
     * Vector search only: performs semantic similarity search without tag filtering.
     */
    private static List<Document> searchByVectorOnly(
        VectorStore vectorStore, String query, int topK, double similarityThreshold) {

        SearchRequest searchRequest = SearchRequest.builder()
            .query(query)
            .topK(topK)
            .similarityThreshold(similarityThreshold)
            .build();

        return vectorStore.similaritySearch(searchRequest);
    }

    /**
     * Combined search: filters by tags (OR logic), then performs semantic similarity search.
     */
    private static List<Document> searchWithTagFilter(
        VectorStore vectorStore, String query, List<Long> tagIds, int topK, double similarityThreshold) {

        Filter.Expression tagFilter = buildTagFilter(tagIds);

        SearchRequest searchRequest = SearchRequest.builder()
            .query(query)
            .topK(topK)
            .similarityThreshold(similarityThreshold)
            .filterExpression(tagFilter)
            .build();

        return vectorStore.similaritySearch(searchRequest);
    }

    /**
     * Builds a filter expression for tags using OR logic. Documents matching ANY of the specified tags will be
     * included.
     */
    private static Filter.Expression buildTagFilter(List<Long> tagIds) {
        FilterExpressionBuilder filterExpressionBuilder = new FilterExpressionBuilder();

        // Build OR expression for tags: tag_ids contains tagId1 OR tag_ids contains tagId2...
        // Since PgVector doesn't support array contains directly, we use IN for each tag
        // and combine with OR
        Filter.Expression[] tagExpressions = tagIds.stream()
            .map(tagId -> filterExpressionBuilder.in(METADATA_TAG_IDS, tagId)
                .build())
            .toArray(Filter.Expression[]::new);

        if (tagExpressions.length == 1) {
            return tagExpressions[0];
        }

        // Combine with OR
        Filter.Expression result = tagExpressions[0];

        for (int i = 1; i < tagExpressions.length; i++) {
            result = new Filter.Expression(Filter.ExpressionType.OR, result, tagExpressions[i]);
        }

        return result;
    }

    private static ActionDefinition.OptionsFunction<Long> getKnowledgeBaseOptions(
        KnowledgeBaseService knowledgeBaseService) {

        return (inputParameters, connectionParameters, dependencyPaths, searchText, context) -> {
            List<Option<Long>> options = new ArrayList<>();

            List<KnowledgeBase> knowledgeBases = knowledgeBaseService.getKnowledgeBases();

            for (KnowledgeBase knowledgeBase : knowledgeBases) {
                String knowledgeBaseName = knowledgeBase.getName();

                String knowledgeBaseNameLowerCase = knowledgeBaseName.toLowerCase();

                if (searchText == null || knowledgeBaseNameLowerCase.contains(searchText.toLowerCase())) {
                    Long knowledgeBaseIdValue = knowledgeBase.getId();

                    options.add(option(knowledgeBaseName, knowledgeBaseIdValue.longValue()));
                }
            }

            return options;
        };
    }

    private static ActionDefinition.OptionsFunction<Long> getTagOptions(TagService tagService) {
        return (inputParameters, connectionParameters, dependencyPaths, searchText, context) -> {
            List<Option<Long>> options = new ArrayList<>();

            List<Tag> tags = tagService.getTags();

            for (Tag tag : tags) {
                String tagName = tag.getName();

                String tagNameLowerCase = tagName.toLowerCase();

                if (searchText == null || tagNameLowerCase.contains(searchText.toLowerCase())) {
                    long tagId = Objects.requireNonNull(tag.getId());

                    options.add(option(tagName, tagId));
                }
            }

            return options;
        };
    }
}
