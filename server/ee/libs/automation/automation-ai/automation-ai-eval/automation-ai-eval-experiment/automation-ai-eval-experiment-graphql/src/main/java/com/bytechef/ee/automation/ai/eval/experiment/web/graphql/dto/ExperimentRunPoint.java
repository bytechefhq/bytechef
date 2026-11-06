/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.eval.experiment.web.graphql.dto;

import java.math.BigDecimal;
import java.util.List;
import org.apache.commons.lang3.Validate;

/**
 * @author Ivica Cardic
 * @version ee
 */
public record ExperimentRunPoint(
    Long experimentId,
    Long runId,
    Long traceId,
    String status,
    BigDecimal cost,
    Integer latencyMs,
    List<ScorePoint> scores) {

    public ExperimentRunPoint {
        Validate.notNull(experimentId, "experimentId must not be null");
        Validate.notNull(runId, "runId must not be null");
        Validate.notBlank(status, "status must not be blank");
        scores = scores == null ? List.of() : List.copyOf(scores);
    }
}
