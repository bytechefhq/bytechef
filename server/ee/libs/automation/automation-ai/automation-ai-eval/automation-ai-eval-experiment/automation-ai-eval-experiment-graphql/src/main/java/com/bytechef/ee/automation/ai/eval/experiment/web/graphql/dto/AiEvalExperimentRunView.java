/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.eval.experiment.web.graphql.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * GraphQL projection of {@code AiEvalExperimentRun}. Distinct from
 * {@link com.bytechef.ee.automation.ai.eval.experiment.dto.ExperimentRunView} (which is the REST wire DTO) so the
 * GraphQL field set can evolve independently — the GraphQL view exposes the {@code output} body for the operator detail
 * panel, while the REST view keeps the body off the wire to avoid bloating the public API response.
 *
 * @author Ivica Cardic
 * @version ee
 */
public record AiEvalExperimentRunView(
    Long id,
    Long experimentId,
    Long datasetItemId,
    Long traceId,
    String status,
    Integer latencyMs,
    BigDecimal cost,
    String errorMessage,
    Instant createdDate) {
}
