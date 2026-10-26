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
}
