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

package com.bytechef.component.ai.agent.constant;

import static com.bytechef.component.definition.ComponentDsl.integer;

import com.bytechef.component.ai.llm.constant.LLMConstants;
import com.bytechef.component.definition.ComponentDsl.ModifiableIntegerProperty;
import com.bytechef.component.definition.Property;
import java.util.List;

/**
 * @author Ivica Cardic
 */
public class AiAgentConstants {

    public static final String AI_AGENT = "aiAgent";
    public static final String CHAT = "chat";
    public static final String MAX_TOOL_CALLS = "maxToolCalls";

    /**
     * Caps how many tool calls one agent run may make in total, across every tool. Counted by Spring AI's own
     * {@code ToolCallLimits} (2.0.1+), so the unit is individual tool calls rather than agent loop iterations - one
     * iteration can issue several calls in parallel. Left unset the provider default applies
     * ({@code DefaultToolCallingManager.DEFAULT_MAX_TOTAL_TOOL_CALLS}, currently 150).
     *
     * <p>
     * On breach the model is told it ran out of tool calls and gets to answer with what it has
     * ({@code ToolCallLimitBehavior.RETURN_ERROR_RESPONSE}) rather than the run failing outright - an agent answering
     * over Slack or WhatsApp must not go silent because it hit a cap.
     */
    public static final ModifiableIntegerProperty MAX_TOOL_CALLS_PROPERTY = integer(MAX_TOOL_CALLS)
        .label("Max Tool Calls")
        .description(
            "Maximum number of tool calls the agent may make in one run, across all tools. Leave empty for the " +
                "default of 150.")
        .minValue(1)
        .maxValue(1000)
        .advancedOption(true);

    public static final List<Property> CHAT_PROPERTIES = List.of(
        LLMConstants.FORMAT_PROPERTY,
        LLMConstants.PROMPT_PROPERTY,
        LLMConstants.SYSTEM_PROMPT_PROPERTY,
        LLMConstants.ATTACHMENTS_PROPERTY,
        LLMConstants.MESSAGES_PROPERTY,
        LLMConstants.RESPONSE_PROPERTY,
        MAX_TOOL_CALLS_PROPERTY);

    // Streaming chat always streams text: a streamed response cannot be validated against a JSON schema or
    // self-corrected (the tokens are already flowing), so the structured-output response format is omitted.
    public static final List<Property> STREAM_CHAT_PROPERTIES = List.of(
        LLMConstants.FORMAT_PROPERTY,
        LLMConstants.PROMPT_PROPERTY,
        LLMConstants.SYSTEM_PROMPT_PROPERTY,
        LLMConstants.ATTACHMENTS_PROPERTY,
        LLMConstants.MESSAGES_PROPERTY,
        MAX_TOOL_CALLS_PROPERTY);
}
