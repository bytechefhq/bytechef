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
 * @author Ivica Cardic
 * @version ee
 */
public record ItemView(
    Long id, Long datasetId, Long datasetVersionId, Long sourceTraceId, Instant createdDate) {

    public ItemView {
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
