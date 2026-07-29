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
 * <p>
 * <b>Single-embedding fan-out.</b> RC1's {@link ToolIndex#search(ToolSearchRequest)} accepts only a query
 * <i>string</i>, and {@code VectorToolIndex.doSearch} embeds that string inside every {@code similaritySearch} call.
 * Unioning sessions by issuing one {@code delegate.search(...)} per session would therefore re-embed the identical
 * query once per session (catalog + per-mode global + conversation = three embedding round-trips per {@code searchTool}
 * invocation). Instead this class drops to the underlying {@link VectorStore} and issues a <b>single</b>
 * {@code similaritySearch} whose filter is {@code sessionId IN (all sessions)} — one embedding, and pgvector applies
 * the union server-side. The result is also a true global top-K rather than per-session top-K then merge. It replicates
 * {@code VectorToolIndex}'s threshold, id-metadata mapping, and de-dup semantics so the only behavioural change is the
 * removal of the redundant embeddings.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class MultiSessionToolIndex implements ToolIndex {

    /**
     * Session-id metadata key written by {@link VectorToolIndex} when it indexes a tool. Private in the upstream class,
     * so mirrored here — it is the on-the-wire column name the pgvector filter must match.
     */
    private static final String METADATA_SESSION_ID = "sessionId";

    /**
     * Mirrors {@code VectorToolIndex.DEFAULT_SIMILARITY_THRESHOLD} (private upstream). Applied to the union search so a
     * tool must clear the same relevance bar it would have per session.
     */
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

    /**
     * Delegates the whole batch to the wrapped index instead of inheriting the {@link ToolIndex} default, which loops
     * {@link #indexTool} one reference at a time. The default turned the advisor's per-session loop-initialization
     * indexing (the agent's full static tool list under a fresh conversation id) into one single-document
     * {@code vectorStore.add} — and therefore one embedding HTTP request — per tool on the first turn of every new
     * conversation. {@code VectorToolIndex.indexTools} builds all documents and issues one {@code vectorStore.add}, so
     * the store's {@code BatchingStrategy} collapses the batch into a handful of embedding requests.
     */
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

        // Over-fetch to the union's breadth (per-session top-K summed across sessions) so the post-dedup truncation
        // still yields up to maxResults distinct tools even when the same tool ranks highly in several sessions. This
        // matches the candidate breadth the old per-session fan-out considered before merging.
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

        // De-duplicate by tool name, keeping the highest-scoring reference when the same tool surfaces in more than one
        // session (e.g. a task subset that overlaps the workspace catalog).
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
