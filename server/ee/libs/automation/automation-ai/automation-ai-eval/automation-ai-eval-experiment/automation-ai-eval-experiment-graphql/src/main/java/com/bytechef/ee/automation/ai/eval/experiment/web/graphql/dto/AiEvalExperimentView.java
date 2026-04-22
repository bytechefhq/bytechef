/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.eval.experiment.web.graphql.dto;

import java.time.Instant;

/**
 * GraphQL projection of {@code AiEvalExperiment} extended with denormalized run-count fields the operator console needs
 * at list time. The run counts are computed by the resolver via
 * {@code AiEvalExperimentRunService.countByExperimentAndStatus} so the wire shape stays a single round-trip per
 * workspace — fan-in over N experiments would otherwise cost N+1 queries per status bucket.
 *
 * @author Ivica Cardic
 * @version ee
 */
public record AiEvalExperimentView(
    Long id,
    Long datasetVersionId,
    Long promptVersionId,
    String model,
    String metadata,
    String status,
    Boolean stopRequested,
    long totalRuns,
    long completedRuns,
    long failedRuns,
    Instant createdDate,
    Instant startedDate,
    Instant completedDate) {
}
