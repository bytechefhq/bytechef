/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.contextstore.service;

import com.bytechef.ee.platform.contextstore.domain.ContextStoreRecord;
import com.bytechef.ee.platform.contextstore.dto.ContextStoreQuery;
import com.bytechef.ee.platform.contextstore.dto.ContextStoreQueryFilter;
import com.bytechef.ee.platform.contextstore.dto.ContextStoreSearchResult;
import java.util.List;
import java.util.Optional;

/**
 * Read-side query service for {@link ContextStoreRecord}s. Translates a {@link ContextStoreQuery} into SQL that joins
 * against {@code context_store_record_index} on each filtered field, applies sort/cursor pagination, and materialises
 * full records (payload + metadata) for return.
 *
 * <p>
 * Workspace scoping is enforced by the caller — typically the automation-side tool callback or component action which
 * has already resolved the {@code sourceId} from the workspace's relation table. The platform query service treats
 * {@code sourceId} as the authoritative scope and does not look up workspace ownership itself.
 *
 * @author Ivica Cardic
 * @version ee
 */
public interface ContextStoreQueryService {

    /**
     * Executes the query against {@code context_store_record} and the typed-column index. Pagination uses a base64
     * encoded {@code last record id} cursor; pages of {@code limit + 1} are read internally to compute
     * {@code nextCursor}.
     */
    ContextStoreSearchResult search(ContextStoreQuery query);

    /**
     * Convenience by-key fetch. Returns at most one record — the {@code (sourceId, sourceRecordId)} tuple is unique.
     * Workspace scoping is enforced by the caller via the {@code workspace_context_store_source} relation; the
     * {@code sourceId} alone is sufficient because a source belongs to at most one workspace.
     */
    Optional<ContextStoreRecord> get(Long sourceId, String sourceRecordId);

    /**
     * Returns just the {@code context_store_record.id} values matching the structured filter — id-only projection used
     * by Mode 3 hybrid retrieval to pre-filter the candidate set before similarity search. Avoids the wasteful
     * round-trip of {@code search().getItems().stream().map(getId)} when only ids are needed.
     *
     * <p>
     * The result is bounded by {@link #MAX_HYBRID_CANDIDATES} so the downstream metadata IN clause stays bounded; if
     * the underlying filter yields more matches the result is truncated and the caller's {@code topK} naturally retains
     * only the most relevant ranked subset.
     * </p>
     */
    List<Long> searchRecordIds(long sourceId, ContextStoreQueryFilter filter);

    /**
     * Hard cap on the number of record ids returned by {@link #searchRecordIds(long, ContextStoreQueryFilter)}. Bounds
     * the metadata {@code IN} clause that the semantic-search hybrid path issues against the vector store.
     */
    int MAX_HYBRID_CANDIDATES = 10_000;
}
