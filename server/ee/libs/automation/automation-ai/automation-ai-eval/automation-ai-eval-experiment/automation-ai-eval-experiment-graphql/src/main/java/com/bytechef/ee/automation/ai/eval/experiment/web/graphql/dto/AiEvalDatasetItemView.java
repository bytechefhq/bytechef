/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.eval.experiment.web.graphql.dto;

import java.time.Instant;

/**
 * GraphQL projection of {@code AiEvalDatasetItem}. {@code input} is unbounded and may carry the full chat-completion
 * payload — the GraphQL response is per-version and operator-paginated, but a future optimization may want to either
 * (a) truncate the {@code input} field server-side at this projection or (b) split into a list-DTO without
 * {@code input} and a detail-DTO with it. The current shape mirrors {@code DatasetItemView} on the REST surface for
 * consistency.
 *
 * @author Ivica Cardic
 * @version ee
 */
public record AiEvalDatasetItemView(
    Long id,
    Long datasetVersionId,
    String input,
    String expectedOutput,
    String metadata,
    Long sourceTraceId,
    Instant createdDate) {
}
