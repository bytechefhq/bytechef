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

package com.bytechef.component.ai.agent.tool;

import com.bytechef.platform.ai.tool.ToolSuspension;
import java.util.ArrayList;
import java.util.List;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;

/**
 * @author Ivica Cardic
 */
public record AgentToolSuspension(ConversationState conversation, String pendingToolCallId) {

    public static final String CONTINUE_PARAMETER_KEY = "__bytechef_agent_tool_suspension__";

    public List<Message> resumeConversation(String toolResult) {
        List<Message> messages = new ArrayList<>();
        int patchedCount = 0;

        for (Message message : conversation.toMessages()) {
            if (!(message instanceof ToolResponseMessage toolResponseMessage)) {
                messages.add(message);

                continue;
            }

            List<ToolResponseMessage.ToolResponse> responses = new ArrayList<>();

            for (ToolResponseMessage.ToolResponse toolResponse : toolResponseMessage.getResponses()) {
                if (pendingToolCallId.equals(toolResponse.id()) &&
                    ToolSuspension.isSuspendedToolResult(toolResponse.responseData())) {

                    responses
                        .add(new ToolResponseMessage.ToolResponse(toolResponse.id(), toolResponse.name(), toolResult));

                    patchedCount++;
                } else {
                    responses.add(toolResponse);
                }
            }

            messages.add(
                ToolResponseMessage.builder()
                    .responses(responses)
                    .metadata(toolResponseMessage.getMetadata())
                    .build());
        }

        if (patchedCount == 0) {
            throw new IllegalStateException(
                "No suspended tool response with id '" + pendingToolCallId + "' found in the stored conversation. " +
                    "Continuing would send the model the suspended tool result instead of the human's answer.");
        }

        if (patchedCount > 1) {
            throw new IllegalStateException(
                "Found " + patchedCount + " suspended tool responses with id '" + pendingToolCallId + "' in the " +
                    "stored conversation, so the human's answer cannot be matched to a single tool call.");
        }

        return messages;
    }
}
