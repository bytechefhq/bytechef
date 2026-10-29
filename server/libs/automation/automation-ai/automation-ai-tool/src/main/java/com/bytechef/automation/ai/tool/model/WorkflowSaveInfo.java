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
import java.util.List;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record WorkflowSaveInfo(
    @JsonProperty("id") @JsonPropertyDescription("The unique identifier of the workflow") String id,
    @JsonProperty("project_workflow_id") @JsonPropertyDescription("The unique identifier of the project workflow") Long projectWorkflowId,
    @JsonProperty("name") @JsonPropertyDescription("The name of the workflow") String name,
    @JsonProperty("version") @JsonPropertyDescription("The version of the saved workflow, to pass to the next updateWorkflow") int version,
    @JsonProperty("warnings") @JsonPropertyDescription("Validation warnings of the saved definition; they did not block the save") List<String> warnings) {
}
