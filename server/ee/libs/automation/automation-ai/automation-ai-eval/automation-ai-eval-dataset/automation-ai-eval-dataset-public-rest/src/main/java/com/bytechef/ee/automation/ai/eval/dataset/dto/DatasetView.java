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
 * Read-only view of an {@link AiEvalDataset} returned by the dataset REST endpoints.
 *
 * <p>
 * The dataset entity persists a {@code tags} JSON column server-side, but it is intentionally NOT exposed on this view
 * today: a placeholder field that always serializes as {@code null} would set client expectations for "tags available"
 * before the deserialization is wired, and any client coding to that shape would silently break the day tags get
 * populated. The field will be added back as part of the tag-filtering API rollout, with a real decoder.
 *
 * @author Ivica Cardic
 * @version ee
 */
@SuppressFBWarnings("EI")
public record DatasetView(
    Long id, Long workspaceId, String name, String description,
    Instant createdDate, Instant archivedDate) {

    public DatasetView {
        // Identity fields are non-null by schema contract; rejecting nulls here protects against partial mappers
        // (e.g., Spring Data projection failures, Jackson partial deserialization) that would otherwise let a
        // half-populated view escape into the REST response and surface as a 500-class bug far from the cause.
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
