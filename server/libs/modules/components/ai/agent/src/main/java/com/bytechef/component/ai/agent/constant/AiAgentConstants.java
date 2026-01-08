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

import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.ai.llm.constant.LLMConstants;
import com.bytechef.component.definition.Property;
import java.util.List;

/**
 * @author Ivica Cardic
 */
public class AiAgentConstants {

    public static final String AI_AGENT = "aiAgent";
    public static final String CHAT = "chat";
    public static final String CONVERSATION_ID = "conversationId";

    public static final List<Property> CHAT_PROPERTIES = List.of(
        LLMConstants.FORMAT_PROPERTY,
        LLMConstants.PROMPT_PROPERTY,
        LLMConstants.SYSTEM_PROMPT_PROPERTY,
        LLMConstants.ATTACHMENTS_PROPERTY,
        LLMConstants.MESSAGES_PROPERTY,
        LLMConstants.RESPONSE_PROPERTY,
        string(CONVERSATION_ID)
            .description("The conversation id used in conjunction with memory."));
}
