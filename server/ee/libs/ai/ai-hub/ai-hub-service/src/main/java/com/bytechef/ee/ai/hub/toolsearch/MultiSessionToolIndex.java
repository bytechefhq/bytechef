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
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.tool.toolsearch.ToolIndex;
import org.springframework.ai.tool.toolsearch.ToolReference;
import org.springframework.ai.tool.toolsearch.ToolSearchRequest;
import org.springframework.ai.tool.toolsearch.ToolSearchResponse;

/**
 * A {@link ToolIndex} that fans a search out across a fixed set of additional sessions (the workspace catalog and a
 * per-mode global-tool session) unioned with the per-request session, then merges, de-duplicates by tool name, and
 * returns the global top-K.
 *
 * <p>
 * This replaces the vendored {@code VectorToolSearcher(VectorStore, Set<String> additionalSessionIds)} multi-session
 * constructor, which Spring AI 2.0.0-RC1's single-session
 * {@link org.springframework.ai.tool.toolsearch.index.vectorstore.VectorToolIndex} no longer offers — RC1 scopes every
 * {@link ToolSearchRequest} to exactly one session. Indexing and clearing delegate straight through to the wrapped
 * index; only {@link #search(ToolSearchRequest)} adds the multi-session fan-out.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class MultiSessionToolIndex implements ToolIndex {

    private final ToolIndex delegate;
    private final Set<String> additionalSessionIds;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public MultiSessionToolIndex(ToolIndex delegate, Set<String> additionalSessionIds) {
        this.delegate = delegate;
        this.additionalSessionIds = Set.copyOf(additionalSessionIds);
    }

    @Override
    public void indexTool(String sessionId, ToolReference toolReference) {
        delegate.indexTool(sessionId, toolReference);
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

        // De-duplicate by tool name, keeping the highest-scoring reference when the same tool surfaces in more than one
        // session (e.g. a task subset that overlaps the workspace catalog).
        Map<String, ToolReference> bestByToolName = new LinkedHashMap<>();

        for (String sessionId : sessionIds) {
            ToolSearchResponse response = delegate.search(
                new ToolSearchRequest(
                    sessionId, toolSearchRequest.query(), toolSearchRequest.maxResults(),
                    toolSearchRequest.categoryFilter()));

            for (ToolReference toolReference : response.toolReferences()) {
                bestByToolName.merge(
                    toolReference.toolName(), toolReference,
                    (existing, candidate) -> score(candidate) > score(existing) ? candidate : existing);
            }
        }

        List<ToolReference> merged = new ArrayList<>(bestByToolName.values());

        merged.sort((left, right) -> Double.compare(score(right), score(left)));

        Integer maxResults = toolSearchRequest.maxResults();

        if (maxResults != null && merged.size() > maxResults) {
            merged = new ArrayList<>(merged.subList(0, maxResults));
        }

        return new ToolSearchResponse(merged, merged.size(), null);
    }

    private static double score(@Nullable ToolReference toolReference) {
        if (toolReference == null) {
            return 0.0;
        }

        Double relevanceScore = toolReference.relevanceScore();

        return relevanceScore == null ? 0.0 : relevanceScore;
    }
}
