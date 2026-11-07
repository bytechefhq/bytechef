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
public class MultiSessionToolIndex implements ToolIndex {

    private static final String METADATA_SESSION_ID = "sessionId";

    private static final double SIMILARITY_THRESHOLD = 0.2;

    private final ToolIndex delegate;
    private final VectorStore vectorStore;
    private final Set<String> additionalSessionIds;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public MultiSessionToolIndex(ToolIndex delegate, VectorStore vectorStore, Set<String> additionalSessionIds) {
        this.delegate = delegate;
        this.vectorStore = vectorStore;
        this.additionalSessionIds = Set.copyOf(additionalSessionIds);
    }

    @Override
    public void indexTool(String sessionId, ToolReference toolReference) {
        delegate.indexTool(sessionId, toolReference);
    }

    @Override
    public void indexTools(String sessionId, List<ToolReference> toolReferences) {
        delegate.indexTools(sessionId, toolReferences);
    }

    @Override
    public void clearIndex(String sessionId) {
        delegate.clearIndex(sessionId);
    }

    @Override
    public ToolSearchResponse search(ToolSearchRequest toolSearchRequest) {
        List<String> sessionIds = new ArrayList<>(additionalSessionIds);

        String requestSessionId = toolSearchRequest.sessionId();

        if (requestSessionId != null && !requestSessionId.isBlank() && !sessionIds.contains(requestSessionId)) {
            sessionIds.add(requestSessionId);
        }

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
