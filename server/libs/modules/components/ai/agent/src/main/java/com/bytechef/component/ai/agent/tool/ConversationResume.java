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

import java.util.ArrayList;
import java.util.List;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;

/**
 * Rebuilds a suspended agent conversation for resume: replaces the suspended tool's sentinel {@code ToolResponse} with
 * the human's real answer so the reconstructed conversation is protocol-correct.
 *
 * @author Ivica Cardic
 */
public final class ConversationResume {

    private ConversationResume() {
    }

    public static List<Message> patchPendingToolResponse(
        List<Message> conversation, String pendingToolCallId, String resumeData) {

        List<Message> result = new ArrayList<>();
        int patchedCount = 0;

        for (Message message : conversation) {
            if (!(message instanceof ToolResponseMessage toolResponseMessage)) {
                result.add(message);

                continue;
            }

            List<ToolResponseMessage.ToolResponse> responses = new ArrayList<>();

            for (ToolResponseMessage.ToolResponse toolResponse : toolResponseMessage.getResponses()) {
                if (toolResponse.id()
                    .equals(pendingToolCallId)) {

                    responses.add(
                        new ToolResponseMessage.ToolResponse(
                            toolResponse.id(), toolResponse.name(), resumeData));

                    patchedCount++;
                } else {
                    responses.add(toolResponse);
                }
            }

            result.add(
                ToolResponseMessage.builder()
                    .responses(responses)
                    .build());
        }

        if (patchedCount != 1) {
            throw new IllegalStateException(
                "Expected exactly one tool response with id '" + pendingToolCallId +
                    "' to patch with the resume payload, but found " + patchedCount + ". " +
                    "Continuing would feed the LLM the suspend sentinel as a tool result, producing a corrupted answer.");
        }

        return result;
    }
}
