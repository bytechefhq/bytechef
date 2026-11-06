/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.eval.experiment.dto;

import com.bytechef.ee.platform.ai.eval.experiment.domain.AiEvalExperimentRun;
import com.bytechef.ee.platform.ai.eval.experiment.domain.AiEvalExperimentRunStatus;
import java.math.BigDecimal;
import java.time.Instant;
import org.apache.commons.lang3.Validate;

/**
 * @author Ivica Cardic
 * @version ee
 */
public record ExperimentRunView(
    Long id,
    Long experimentId,
    Long datasetItemId,
    Long traceId,
    AiEvalExperimentRunStatus status,
    Integer latencyMs,
    BigDecimal cost,
    String errorMessage,
    Instant createdDate) {

    public ExperimentRunView {
        Validate.notNull(id, "id must not be null");
        Validate.notNull(experimentId, "experimentId must not be null");
        Validate.notNull(status, "status must not be null");
    }

    public static ExperimentRunView from(AiEvalExperimentRun run) {
        return new ExperimentRunView(
            run.getId(),
            run.getExperimentId(),
            run.getDatasetItemId(),
            run.getTraceId(),
            run.getStatus(),
            run.getLatencyMs(),
            run.getCost(),
            run.getErrorMessage(),
            run.getCreatedDate());
    }
}
