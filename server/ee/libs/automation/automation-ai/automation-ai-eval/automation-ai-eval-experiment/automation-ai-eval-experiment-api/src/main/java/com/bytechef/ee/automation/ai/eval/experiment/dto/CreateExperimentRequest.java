/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.eval.experiment.dto;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.apache.commons.lang3.Validate;

/**
 * Request body for creating an experiment.
 *
 * <p>
 * {@code model} semantics:
 * <ul>
 * <li><strong>Non-null, non-blank</strong> — pins the experiment to that model; every replay overrides the model in the
 * dataset item's input so the comparison is apples-to-apples.</li>
 * <li><strong>Null</strong> — the executor falls back to the model recorded inside each dataset item's input JSON. This
 * is the "replay-as-recorded" mode: useful when the dataset captures real production traffic and you want to re-run it
 * under whatever model each call originally used.</li>
 * </ul>
 * Blank-but-non-null is rejected to prevent accidental empty-string drift in callers (an empty string would otherwise
 * mask the "use the original model" semantics under a confusing bug report).
 *
 * @author Ivica Cardic
 * @version ee
 */
@SuppressFBWarnings("EI")
public record CreateExperimentRequest(
    Long datasetVersionId,
    Long promptVersionId,
    String model,
    Map<String, Object> metadata) {

    public CreateExperimentRequest {
        Validate.notNull(datasetVersionId, "datasetVersionId must not be null");

        if (model != null) {
            Validate.isTrue(
                !model.isBlank(),
                "model must be non-blank when provided; pass null to fall back to the dataset item's recorded model");
        }

        if (metadata != null) {
            metadata = Map.copyOf(metadata);
        }
    }
}
