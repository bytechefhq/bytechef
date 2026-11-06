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
 * @author Ivica Cardic
 * @version ee
 */
public record AggregateScoreDelta(String scoreName, List<ExperimentScoreAverage> deltas) {

    public AggregateScoreDelta {
        Validate.notBlank(scoreName, "scoreName must not be blank");
        deltas = deltas == null ? List.of() : List.copyOf(deltas);
    }
}
