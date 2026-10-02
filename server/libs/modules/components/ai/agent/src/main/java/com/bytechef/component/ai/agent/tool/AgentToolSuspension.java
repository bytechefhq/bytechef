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
import com.bytechef.platform.component.definition.ai.agent.guardrails.HumanToolResponses;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;

/**
 * @author Ivica Cardic
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AgentToolSuspension(
    ConversationState conversation, String pendingToolCallId, String suspendedToolResult) {

    public static final String CONTINUE_PARAMETER_KEY = "__bytechef_agent_tool_suspension__";

    public AgentToolSuspension {
        Objects.requireNonNull(conversation, "The stored agent tool suspension has no conversation");
        Objects.requireNonNull(pendingToolCallId, "The stored agent tool suspension has no pending tool call id");
        Objects.requireNonNull(
            suspendedToolResult, "The stored agent tool suspension has no suspended tool result");

        int suspendedResponseCount = countSuspendedResponses(conversation, pendingToolCallId, suspendedToolResult);

        if (suspendedResponseCount == 0) {
            throw new IllegalArgumentException(
                "No suspended tool response with id '" + pendingToolCallId + "' found in the stored conversation. " +
                    "Continuing would send the model the suspended tool result instead of the human's answer.");
        }

        if (suspendedResponseCount > 1) {
            throw new IllegalArgumentException(
                "Found " + suspendedResponseCount + " suspended tool responses with id '" + pendingToolCallId +
                    "' in the stored conversation, so the human's answer cannot be matched to a single tool call.");
        }
    }

    public List<Message> resumeConversation(String toolResult) {
        List<Message> messages = new ArrayList<>();

        for (Message message : conversation.toMessages()) {
            if (!(message instanceof ToolResponseMessage toolResponseMessage)) {
                messages.add(message);

                continue;
            }

            List<ToolResponseMessage.ToolResponse> responses = new ArrayList<>();
            boolean carriesHumanResponse = false;

            for (ToolResponseMessage.ToolResponse toolResponse : toolResponseMessage.getResponses()) {
                if (pendingToolCallId.equals(toolResponse.id()) &&
                    ToolSuspension.isSuspendedToolResult(toolResponse.responseData(), suspendedToolResult)) {

                    carriesHumanResponse = true;

                    responses
                        .add(new ToolResponseMessage.ToolResponse(toolResponse.id(), toolResponse.name(), toolResult));
                } else {
                    responses.add(toolResponse);
                }
            }

            ToolResponseMessage patchedToolResponseMessage = ToolResponseMessage.builder()
                .responses(responses)
                .metadata(toolResponseMessage.getMetadata())
                .build();

            if (carriesHumanResponse) {
                patchedToolResponseMessage = HumanToolResponses.markHumanResponse(
                    patchedToolResponseMessage, pendingToolCallId);
            }

            messages.add(patchedToolResponseMessage);
        }

        return messages;
    }

    private static int countSuspendedResponses(
        ConversationState conversation, String pendingToolCallId, String suspendedToolResult) {

        int suspendedResponseCount = 0;

        for (ConversationState.Entry entry : conversation.messages()) {
            if (!(entry instanceof ConversationState.ToolEntry toolEntry)) {
                continue;
            }

            for (ConversationState.ToolResponseEntry toolResponseEntry : toolEntry.toolResponses()) {
                if (pendingToolCallId.equals(toolResponseEntry.id()) &&
                    ToolSuspension.isSuspendedToolResult(toolResponseEntry.responseData(), suspendedToolResult)) {

                    suspendedResponseCount++;
                }
            }
        }

        return suspendedResponseCount;
    }
}
