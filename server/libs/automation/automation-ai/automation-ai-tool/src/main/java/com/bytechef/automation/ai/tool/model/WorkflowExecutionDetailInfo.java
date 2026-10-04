/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.automation.ai.tool.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record WorkflowExecutionDetailInfo(
    @JsonProperty("id") @JsonPropertyDescription("The workflow execution id") long id,
    @JsonProperty("workflowId") @JsonPropertyDescription("The workflow id") @Nullable String workflowId,
    @JsonProperty("workflowLabel") @JsonPropertyDescription("The workflow label") @Nullable String workflowLabel,
    @JsonProperty("projectName") @JsonPropertyDescription("The project name") @Nullable String projectName,
    @JsonProperty("status") @JsonPropertyDescription("The job status") @Nullable String status,
    @JsonProperty("startDate") @JsonPropertyDescription("When the run started") @Nullable Instant startDate,
    @JsonProperty("endDate") @JsonPropertyDescription("When the run ended") @Nullable Instant endDate,
    @JsonProperty("error") @JsonPropertyDescription("The job-level error message, if any") @Nullable String error,
    @JsonProperty("trigger") @JsonPropertyDescription("The trigger execution") @Nullable TriggerExecutionInfo trigger,
    @JsonProperty("taskExecutions") @JsonPropertyDescription("The ordered task executions") List<TaskExecutionInfo> taskExecutions) {
}
