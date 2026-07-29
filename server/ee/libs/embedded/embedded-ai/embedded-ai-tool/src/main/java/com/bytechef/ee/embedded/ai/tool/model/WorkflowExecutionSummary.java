/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.tool.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.time.Instant;
import org.jspecify.annotations.Nullable;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public record WorkflowExecutionSummary(
    @JsonProperty("id") @JsonPropertyDescription("The workflow execution id") long id,
    @JsonProperty("workflowLabel") @JsonPropertyDescription("The workflow label") @Nullable String workflowLabel,
    @JsonProperty("integrationName") @JsonPropertyDescription("The integration name") @Nullable String integrationName,
    @JsonProperty("status") @JsonPropertyDescription("The job status") @Nullable String status,
    @JsonProperty("startDate") @JsonPropertyDescription("When the run started") @Nullable Instant startDate,
    @JsonProperty("endDate") @JsonPropertyDescription("When the run ended") @Nullable Instant endDate) {
}
