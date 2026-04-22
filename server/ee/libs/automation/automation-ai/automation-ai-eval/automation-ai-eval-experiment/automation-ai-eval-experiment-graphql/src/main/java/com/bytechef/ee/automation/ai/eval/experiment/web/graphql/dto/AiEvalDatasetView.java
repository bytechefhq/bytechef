/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.eval.experiment.web.graphql.dto;

import java.time.Instant;

/**
 * GraphQL projection of {@code AiEvalDataset}. Lives under the experiment-graphql module rather than a sibling
 * dataset-graphql module because the operator console drills from a dataset into its dependent experiments — pinning
 * dataset GraphQL alongside experiment GraphQL keeps the cross-resource lookup queries colocated.
 *
 * @author Ivica Cardic
 * @version ee
 */
public record AiEvalDatasetView(
    Long id,
    Long workspaceId,
    String name,
    String description,
    String tags,
    Instant createdDate,
    Instant archivedDate) {
}
