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

package com.bytechef.component.ai.agent.action;

import org.jspecify.annotations.Nullable;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * Record used for tool argument augmentation via AugmentedToolCallbackProvider. When added to a tool's method
 * parameters, the AI model is prompted to provide its reasoning and confidence level for the tool call.
 *
 * @author Ivica Cardic
 */
public record AgentThinking(
    @ToolParam(
        description = "Your reasoning for calling this tool and choosing these parameters",
        required = true) String reasoning,

    @ToolParam(description = "Confidence level: low, medium, or high", required = false) @Nullable String confidence) {
}
