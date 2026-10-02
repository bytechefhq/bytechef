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

package com.bytechef.platform.component.definition.ai.agent.guardrails;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;

/**
 * Marks the tool response that carries a human's answer, such as the answer to an AI agent's question or approval
 * request, so input guardrails check and sanitize it like a user message.
 *
 * @author Ivica Cardic
 */
public final class HumanToolResponses {

    public static final String HUMAN_RESPONSE_TOOL_CALL_ID = "bytechef_humanResponseToolCallId";

    private HumanToolResponses() {
    }

    /**
     * Returns the text of the response that carries a human's answer, or {@code null} when the message carries none.
     * The response is found by its tool call id, so it is found even when the responses were filtered or reordered.
     */
    public static @Nullable String getHumanResponseText(Message message) {
        ToolResponseMessage.ToolResponse humanToolResponse = findHumanToolResponse(message);

        return humanToolResponse == null ? null : humanToolResponse.responseData();
    }

    public static ToolResponseMessage markHumanResponse(ToolResponseMessage toolResponseMessage, String toolCallId) {
        Map<String, Object> metadata = new HashMap<>(toolResponseMessage.getMetadata());

        metadata.put(HUMAN_RESPONSE_TOOL_CALL_ID, toolCallId);

        return ToolResponseMessage.builder()
            .responses(toolResponseMessage.getResponses())
            .metadata(metadata)
            .build();
    }

    public static ToolResponseMessage withHumanResponseText(ToolResponseMessage toolResponseMessage, String text) {
        ToolResponseMessage.ToolResponse humanToolResponse = findHumanToolResponse(toolResponseMessage);

        if (humanToolResponse == null) {
            return toolResponseMessage;
        }

        List<ToolResponseMessage.ToolResponse> responses = toolResponseMessage.getResponses()
            .stream()
            .map(toolResponse -> toolResponse == humanToolResponse
                ? new ToolResponseMessage.ToolResponse(toolResponse.id(), toolResponse.name(), text) : toolResponse)
            .toList();

        return ToolResponseMessage.builder()
            .responses(responses)
            .metadata(toolResponseMessage.getMetadata())
            .build();
    }

    private static ToolResponseMessage.@Nullable ToolResponse findHumanToolResponse(Message message) {
        if (!(message instanceof ToolResponseMessage toolResponseMessage) ||
            !(toolResponseMessage.getMetadata()
                .get(HUMAN_RESPONSE_TOOL_CALL_ID) instanceof String toolCallId)) {

            return null;
        }

        return toolResponseMessage.getResponses()
            .stream()
            .filter(toolResponse -> toolCallId.equals(toolResponse.id()))
            .findFirst()
            .orElse(null);
    }
}
