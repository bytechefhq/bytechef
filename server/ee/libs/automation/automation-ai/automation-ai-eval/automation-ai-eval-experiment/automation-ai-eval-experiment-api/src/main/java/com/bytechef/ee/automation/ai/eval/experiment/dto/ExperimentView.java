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
 * Wire view of an experiment. {@code status} is typed as {@link AiEvalExperimentStatus} (not a free-form String) so
 * consumers get compile-time exhaustiveness when switching on it. Jackson serializes the enum to its name on the wire
 * (e.g. {@code "RUNNING"}) so the JSON contract is unchanged.
 *
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
