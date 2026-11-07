/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.toolsearch;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.toolsearch.ToolIndex;
import org.springframework.ai.tool.toolsearch.ToolReference;
import org.springframework.ai.tool.toolsearch.ToolSearchRequest;
import org.springframework.ai.tool.toolsearch.ToolSearchResponse;
import org.springframework.ai.tool.toolsearch.index.vectorstore.VectorToolIndex;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class CatalogToolIndex implements ToolIndex {

    static final String METADATA_SESSION_ID = "sessionId";

    private static final Logger log = LoggerFactory.getLogger(CatalogToolIndex.class);

    private static final double SIMILARITY_THRESHOLD = 0.2;

    private final VectorStore vectorStore;
    private final Set<String> catalogSessionIds;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public CatalogToolIndex(VectorStore vectorStore, Set<String> catalogSessionIds) {
        this.vectorStore = vectorStore;
        this.catalogSessionIds = Set.copyOf(catalogSessionIds);
    }

    @Override
    public void indexTool(String sessionId, ToolReference toolReference) {
        indexTools(sessionId, List.of(toolReference));
    }

    @Override
    public void indexTools(String sessionId, List<ToolReference> toolReferences) {
        if (log.isTraceEnabled()) {
            log.trace("Not indexing {} request tools under session {}", toolReferences.size(), sessionId);
        }
    }

    @Override
    public void clearIndex(String sessionId) {
        if (catalogSessionIds.contains(sessionId)) {
            return;
        }

        vectorStore.delete(
            new FilterExpressionBuilder()
                .eq(METADATA_SESSION_ID, sessionId)
                .build());
    }

    @Override
    public ToolSearchResponse search(ToolSearchRequest toolSearchRequest) {
        List<String> sessionIds = new ArrayList<>(catalogSessionIds);

        Integer maxResults = toolSearchRequest.maxResults();
        int perSessionTopK = maxResults != null ? maxResults : 10;

        int topK = perSessionTopK * sessionIds.size();

        SearchRequest searchRequest = SearchRequest.builder()
            .query(toolSearchRequest.query())
            .topK(topK)
            .similarityThreshold(SIMILARITY_THRESHOLD)
            .filterExpression(
                new FilterExpressionBuilder()
                    .in(METADATA_SESSION_ID, sessionIds.toArray())
                    .build())
            .build();

        List<Document> documents = vectorStore.similaritySearch(searchRequest);

        Map<String, ToolReference> bestByToolName = new LinkedHashMap<>();

        if (documents != null) {
            for (Document document : documents) {
                ToolReference toolReference = toToolReference(document);

                bestByToolName.merge(
                    toolReference.toolName(), toolReference,
                    (existing, candidate) -> score(candidate) > score(existing) ? candidate : existing);
            }
        }

        List<ToolReference> merged = new ArrayList<>(bestByToolName.values());

        merged.sort((left, right) -> Double.compare(score(right), score(left)));

        if (maxResults != null && merged.size() > maxResults) {
            merged = new ArrayList<>(merged.subList(0, maxResults));
        }

        return new ToolSearchResponse(merged, merged.size(), null);
    }

    private static ToolReference toToolReference(Document document) {
        Map<String, Object> metadata = document.getMetadata();

        return ToolReference.builder()
            .toolName((String) Objects.requireNonNull(metadata.get(VectorToolIndex.METADATA_TOOL_NAME)))
            .relevanceScore(Objects.requireNonNullElse(document.getScore(), 0.0))
            .summary((String) Objects.requireNonNull(metadata.get(VectorToolIndex.METADATA_TOOL_DESCRIPTION)))
            .build();
    }

    private static double score(@Nullable ToolReference toolReference) {
        if (toolReference == null) {
            return 0.0;
        }

        Double relevanceScore = toolReference.relevanceScore();

        return relevanceScore == null ? 0.0 : relevanceScore;
    }
}
