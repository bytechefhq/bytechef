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
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record TaskExecutionInfo(
    @JsonProperty("name") @JsonPropertyDescription("The task name (workflow task label or type)") @Nullable String name,
    @JsonProperty("type") @JsonPropertyDescription("The component/action type, e.g. httpClient/v1/get") @Nullable String type,
    @JsonProperty("status") @JsonPropertyDescription("The task execution status") @Nullable String status,
    @JsonProperty("input") @JsonPropertyDescription("The resolved task input") @Nullable Map<String, ?> input,
    @JsonProperty("output") @JsonPropertyDescription("The task output value") @Nullable Object output,
    @JsonProperty("error") @JsonPropertyDescription("The error message if the task failed") @Nullable String error) {
}
