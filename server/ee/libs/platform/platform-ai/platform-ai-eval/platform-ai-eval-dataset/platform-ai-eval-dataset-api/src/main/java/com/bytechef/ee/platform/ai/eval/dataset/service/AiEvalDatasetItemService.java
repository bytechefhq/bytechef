/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.eval.dataset.service;

import com.bytechef.ee.platform.ai.eval.dataset.domain.AiEvalDatasetItem;
import java.util.List;

/**
 * Item-level operations on a dataset. All insert paths go through
 * {@link AiEvalDatasetVersionService#getOrCreateUnfrozenVersion(long)} so that frozen versions remain untouched and a
 * new unfrozen version is auto-created if needed.
 *
 * <p>
 * Workspace/cross-tenant trace lookups are NOT done here — the platform module has no compile-time dependency on the
 * gateway observability stack. Callers that promote a production trace into a dataset (e.g. the REST controller in
 * automation-ai-gateway-dataset-public-rest) are responsible for loading the trace, asserting workspace ownership, and
 * passing the resolved {@code input} string + {@code traceId} into {@link #addItemFromTrace}.
 *
 * @author Ivica Cardic
 * @version ee
 */
public interface AiEvalDatasetItemService {

    /**
     * Adds one item to the given dataset. If the latest version is frozen (or no version exists yet), auto-creates a
     * new unfrozen version and attaches the item to it.
     */
    AiEvalDatasetItem addItem(long datasetId, String input, String expectedOutput, String metadata);

    /**
     * Adds many items as a single batch to the dataset. Resolves the unfrozen version once for the whole batch and then
     * persists every item against it (auto-creating an unfrozen version if the latest is frozen).
     */
    List<AiEvalDatasetItem> addItems(long datasetId, List<AddItem> items);

    /**
     * Adds an item whose payload was sourced from an observability trace. Stores {@code sourceTraceId} for provenance
     * so the item can be traced back to the production invocation it was derived from. The caller is responsible for
     * loading the trace from the gateway, asserting cross-tenant ownership, and supplying the resolved {@code input}
     * string — keeping the platform module decoupled from the gateway observability types.
     */
    AiEvalDatasetItem addItemFromTrace(
        long datasetId, long traceId, String input, String expectedOutput, String metadata);

    List<AiEvalDatasetItem> getItemsByVersion(Long versionId);

    long countByVersion(Long versionId);

    /**
     * DTO for the {@link #addItems(long, List)} batch input.
     */
    record AddItem(String input, String expectedOutput, String metadata) {
    }
}
