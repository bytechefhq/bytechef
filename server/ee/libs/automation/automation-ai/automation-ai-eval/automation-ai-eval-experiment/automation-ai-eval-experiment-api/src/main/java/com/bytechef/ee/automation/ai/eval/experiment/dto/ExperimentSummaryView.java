/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.eval.experiment.dto;

import com.bytechef.ee.platform.ai.eval.experiment.domain.AiEvalExperimentStatus;
import java.time.Instant;
import org.apache.commons.lang3.Validate;

/**
 * @author Ivica Cardic
 * @version ee
 */
public record ExperimentSummaryView(
    Long id,
    Long workspaceId,
    Long datasetVersionId,
    Long promptVersionId,
    String model,
    AiEvalExperimentStatus status,
    long totalRuns,
    long completedRuns,
    long failedRuns,
    Instant createdDate,
    Instant startedDate,
    Instant completedDate) {

    public ExperimentSummaryView {
        Validate.notNull(id, "id must not be null");
        Validate.notNull(workspaceId, "workspaceId must not be null");
        Validate.notNull(status, "status must not be null");
    }
}
