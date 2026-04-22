/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.eval.dataset.dto;

import com.bytechef.ee.platform.ai.eval.dataset.domain.AiEvalDatasetItem;
import java.time.Instant;

/**
 * Read-only view of an {@link AiEvalDatasetItem} returned by the dataset REST endpoints. Exposes only identifying and
 * provenance fields; the JSON payloads (input/expectedOutput/metadata) are intentionally omitted — callers fetch them
 * through dedicated item-retrieval endpoints.
 *
 * @author Ivica Cardic
 * @version ee
 */
public record ItemView(
    Long id, Long datasetId, Long datasetVersionId, Long sourceTraceId, Instant createdDate) {

    public ItemView {
        // Identity + tenant-scoping fields are non-null by schema contract. {@code datasetVersionId} and
        // {@code sourceTraceId} stay nullable: an item that hasn't yet been promoted into a versioned snapshot has
        // no version, and items added directly via the bulk-add endpoint have no source trace. The dataset-id link
        // is the load-bearing tenant scope (the workspace is reachable through the dataset), so a null there is a
        // partial-mapper bug we want to fail loudly on rather than escape via the REST response.
        if (id == null) {
            throw new IllegalArgumentException("id must not be null");
        }

        if (datasetId == null) {
            throw new IllegalArgumentException("datasetId must not be null");
        }

        if (createdDate == null) {
            throw new IllegalArgumentException("createdDate must not be null");
        }
    }

    public static ItemView from(AiEvalDatasetItem item) {
        return new ItemView(
            item.getId(),
            item.getDatasetId(),
            item.getDatasetVersionId(),
            item.getSourceTraceId(),
            item.getCreatedDate());
    }
}
