/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.eval.experiment.web.graphql.dto;

import java.time.Instant;

/**
 * GraphQL projection of {@code AiEvalDatasetVersion}. The denormalized {@code itemCount} field is computed by the
 * resolver via {@code AiEvalDatasetItemService.countByVersion} so the operator console can render the version table
 * without a per- row drill into items.
 *
 * @author Ivica Cardic
 * @version ee
 */
public record AiEvalDatasetVersionView(
    Long id,
    Long datasetId,
    String label,
    boolean frozen,
    long itemCount,
    Instant createdDate) {
}
