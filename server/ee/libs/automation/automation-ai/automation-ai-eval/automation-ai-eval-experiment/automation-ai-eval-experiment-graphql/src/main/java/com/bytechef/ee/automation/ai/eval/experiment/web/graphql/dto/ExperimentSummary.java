/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.eval.experiment.web.graphql.dto;

import java.math.BigDecimal;
import org.apache.commons.lang3.Validate;

/**
 * Per-experiment aggregate summary in the comparison view. {@code totalCost} sums all run costs; {@code
 * averageLatencyMs} averages only runs with a non-null latency.
 *
 * @author Ivica Cardic
 * @version ee
 */
public record ExperimentSummary(
    Long id,
    String model,
    long totalRuns,
    long completedRuns,
    long failedRuns,
    BigDecimal totalCost,
    Integer averageLatencyMs) {

    public ExperimentSummary {
        Validate.notNull(id, "id must not be null");
        Validate.isTrue(totalRuns >= 0, "totalRuns must be non-negative");
        Validate.isTrue(completedRuns >= 0, "completedRuns must be non-negative");
        Validate.isTrue(failedRuns >= 0, "failedRuns must be non-negative");
    }
}
