/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.eval.experiment.web.graphql.dto;

import java.util.List;

/**
 * Top-level response for the {@code experimentComparison} GraphQL query. Returns per-experiment summaries, per dataset
 * item run diffs, and per-score-name numeric averages so the UI can render a side-by-side comparison matrix.
 *
 * @author Ivica Cardic
 * @version ee
 */
public record ExperimentComparisonView(
    List<ExperimentSummary> experiments,
    List<ExperimentComparisonRow> rows,
    List<AggregateScoreDelta> aggregateScoreDeltas) {

    public ExperimentComparisonView {
        experiments = experiments == null ? List.of() : List.copyOf(experiments);
        rows = rows == null ? List.of() : List.copyOf(rows);
        aggregateScoreDeltas = aggregateScoreDeltas == null ? List.of() : List.copyOf(aggregateScoreDeltas);
    }
}
