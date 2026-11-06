/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.eval.dataset.dto;

import com.bytechef.ee.platform.ai.eval.dataset.domain.AiEvalDataset;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;

/**
 * @author Ivica Cardic
 * @version ee
 */
@SuppressFBWarnings("EI")
public record DatasetView(
    Long id, Long workspaceId, String name, String description,
    Instant createdDate, Instant archivedDate) {

    public DatasetView {
        if (id == null) {
            throw new IllegalArgumentException("id must not be null");
        }

        if (workspaceId == null) {
            throw new IllegalArgumentException("workspaceId must not be null");
        }

        if (name == null) {
            throw new IllegalArgumentException("name must not be null");
        }

        if (createdDate == null) {
            throw new IllegalArgumentException("createdDate must not be null");
        }
    }

    public static DatasetView from(AiEvalDataset dataset, Long workspaceId) {
        return new DatasetView(
            dataset.getId(),
            workspaceId,
            dataset.getName(),
            dataset.getDescription(),
            dataset.getCreatedDate(),
            dataset.getArchivedDate());
    }
}
