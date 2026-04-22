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
 * Numeric average of a named score across all runs in a single experiment.
 *
 * @author Ivica Cardic
 * @version ee
 */
public record ExperimentScoreAverage(Long experimentId, BigDecimal average, long count) {

    public ExperimentScoreAverage {
        Validate.notNull(experimentId, "experimentId must not be null");
        Validate.isTrue(count >= 0, "count must be non-negative");
    }
}
