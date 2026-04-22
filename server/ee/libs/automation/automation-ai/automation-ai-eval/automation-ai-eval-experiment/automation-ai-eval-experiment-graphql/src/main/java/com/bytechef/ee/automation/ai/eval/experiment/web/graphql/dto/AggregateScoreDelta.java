/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.eval.experiment.web.graphql.dto;

import java.util.List;
import org.apache.commons.lang3.Validate;

/**
 * Per-score-name aggregate: for each experiment, the numeric average and sample count. The UI computes pairwise deltas
 * (e.g., experiment A average minus experiment B average) from this data — the server returns the raw averages so the
 * response remains symmetric regardless of experiment ordering.
 *
 * <p>
 * Only NUMERIC scores are aggregated. BOOLEAN and CATEGORICAL scores are intentionally omitted — a well-defined
 * aggregation semantics (% true, mode, etc.) is deferred.
 *
 * @author Ivica Cardic
 * @version ee
 */
public record AggregateScoreDelta(String scoreName, List<ExperimentScoreAverage> deltas) {

    public AggregateScoreDelta {
        Validate.notBlank(scoreName, "scoreName must not be blank");
        deltas = deltas == null ? List.of() : List.copyOf(deltas);
    }
}
