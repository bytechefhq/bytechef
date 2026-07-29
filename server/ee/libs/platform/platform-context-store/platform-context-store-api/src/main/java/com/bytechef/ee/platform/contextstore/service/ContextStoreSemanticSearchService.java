/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.contextstore.service;

import com.bytechef.ee.platform.contextstore.domain.ContextStoreRecord;
import com.bytechef.ee.platform.contextstore.dto.ContextStoreQueryFilter;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;

/**
 * Semantic search over the Context Store vector-store table populated by
 * {@link com.bytechef.ee.platform.contextstore.listener.ContextStoreSemanticBatchListener}. Exposes Mode 2 (pure
 * semantic) and Mode 3 (structured-prefilter + semantic ranking) queries.
 *
 * <p>
 * The bean is gated on {@code EmbeddingModel}/{@code VectorStore} presence; deployments without the semantic add-on (CE
 * without embeddings) won't have it in the application context. Both single-tenant and multi-tenant deployments
 * register it. Callers that want graceful degradation should resolve via
 * {@link org.springframework.beans.factory.ObjectProvider}.
 * </p>
 *
 * <p>
 * Workspace authorization is the caller's responsibility — the platform-side service treats the {@code sourceId} as the
 * authoritative scope and assumes the caller has already verified ownership through
 * {@code context_store_source.workspace_id}.
 * </p>
 *
 * @author Ivica Cardic
 * @version ee
 */
public interface ContextStoreSemanticSearchService {

    /**
     * Mode 2: pure semantic search. Ranks records by similarity to the natural-language {@code queryText}, with no
     * structured pre-filter. Results are scoped to the given source.
     */
    List<SemanticHit> searchSemantic(long sourceId, String queryText, int k);

    /**
     * Mode 3: hybrid search. Applies the structured {@code prefilter} first (via
     * {@link ContextStoreQueryService#searchRecordIds}) to pre-filter the candidate record set, then ranks the
     * candidates by similarity to {@code queryText}. The filter operations are the same as
     * {@link ContextStoreQueryService#search} (single-field equality + range; no multi-clause OR/NOT in MVP).
     *
     * <p>
     * If the prefilter yields no candidates the method returns immediately with an empty list (no similarity search is
     * performed).
     * </p>
     */
    List<SemanticHit> searchSemantic(long sourceId, String queryText, int k, ContextStoreQueryFilter prefilter);

    /**
     * A single ranked semantic hit — a hydrated {@link ContextStoreRecord} together with the cosine similarity score
     * (in {@code [0, 1]}, higher is more similar). When the underlying vector store does not expose a distance the
     * score is {@link Double#NaN}.
     *
     * @author Ivica Cardic
     * @version ee
     */
    @SuppressFBWarnings({
        "EI_EXPOSE_REP", "EI_EXPOSE_REP2"
    })
    record SemanticHit(ContextStoreRecord record, double similarityScore) {
    }
}
