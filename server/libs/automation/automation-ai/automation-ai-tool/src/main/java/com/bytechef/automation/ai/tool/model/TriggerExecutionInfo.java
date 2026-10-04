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
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
public record TriggerExecutionInfo(
    @JsonProperty("type") @JsonPropertyDescription("The trigger type") @Nullable String type,
    @JsonProperty("status") @JsonPropertyDescription("The trigger execution status") @Nullable String status,
    @JsonProperty("output") @JsonPropertyDescription("The trigger output value") @Nullable Object output,
    @JsonProperty("error") @JsonPropertyDescription("The error message if the trigger failed") @Nullable String error) {
}
