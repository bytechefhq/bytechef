/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.tool.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record WorkflowExecutionDetailInfo(
    @JsonProperty("id") @JsonPropertyDescription("The workflow execution id") long id,
    @JsonProperty("workflowId") @JsonPropertyDescription("The workflow id") @Nullable String workflowId,
    @JsonProperty("workflowLabel") @JsonPropertyDescription("The workflow label") @Nullable String workflowLabel,
    @JsonProperty("integrationName") @JsonPropertyDescription("The integration name") @Nullable String integrationName,
    @JsonProperty("status") @JsonPropertyDescription("The job status") @Nullable String status,
    @JsonProperty("startDate") @JsonPropertyDescription("When the run started") @Nullable Instant startDate,
    @JsonProperty("endDate") @JsonPropertyDescription("When the run ended") @Nullable Instant endDate,
    @JsonProperty("error") @JsonPropertyDescription("The job-level error message, if any") @Nullable String error,
    @JsonProperty("trigger") @JsonPropertyDescription("The trigger execution") @Nullable TriggerExecutionInfo trigger,
    @JsonProperty("taskExecutions") @JsonPropertyDescription("The ordered task executions") List<TaskExecutionInfo> taskExecutions) {
}
