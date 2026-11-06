/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.eval.experiment.dto;

import com.bytechef.ee.platform.ai.eval.experiment.domain.AiEvalExperiment;
import com.bytechef.ee.platform.ai.eval.experiment.domain.AiEvalExperimentStatus;
import java.time.Instant;

/**
 * @author Ivica Cardic
 * @version ee
 */
public record ExperimentView(
    Long id,
    Long workspaceId,
    Long datasetVersionId,
    Long promptVersionId,
    String model,
    AiEvalExperimentStatus status,
    Instant createdDate,
    Instant startedDate,
    Instant completedDate) {

    public static ExperimentView from(AiEvalExperiment experiment, Long workspaceId) {
        return new ExperimentView(
            experiment.getId(),
            workspaceId,
            experiment.getDatasetVersionId(),
            experiment.getPromptVersionId(),
            experiment.getModel(),
            experiment.getStatus(),
            experiment.getCreatedDate(),
            experiment.getStartedDate(),
            experiment.getCompletedDate());
    }
}
