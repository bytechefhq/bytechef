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
 * One row per dataset item, grouping the per-experiment runs that replayed that item.
 *
 * @author Ivica Cardic
 * @version ee
 */
public record ExperimentComparisonRow(Long datasetItemId, List<ExperimentRunPoint> runsByExperiment) {

    public ExperimentComparisonRow {
        Validate.notNull(datasetItemId, "datasetItemId must not be null");
        runsByExperiment = runsByExperiment == null ? List.of() : List.copyOf(runsByExperiment);
    }
}
