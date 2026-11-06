/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.eval.experiment.web.graphql.dto;

import java.time.Instant;

/**
 * @author Ivica Cardic
 * @version ee
 */
public record AiEvalDatasetItemView(
    Long id,
    Long datasetVersionId,
    String input,
    String expectedOutput,
    String metadata,
    Long sourceTraceId,
    Instant createdDate) {
}
