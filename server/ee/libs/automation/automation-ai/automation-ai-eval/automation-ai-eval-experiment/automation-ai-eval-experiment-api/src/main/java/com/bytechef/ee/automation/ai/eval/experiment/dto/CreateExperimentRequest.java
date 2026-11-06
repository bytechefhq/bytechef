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
