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

package com.bytechef.component.ai.llm.advisor;

import java.util.ArrayList;
import java.util.List;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.ToolCallAdvisor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;

/**
 * Extends {@link ToolCallAdvisor} to restore complete in-memory tool-call context in each loop iteration when
 * {@link ToolCallAdvisor.Builder#disableInternalConversationHistory()} is active.
 *
 * <p>
 * When conversation history is delegated to a downstream
 * {@link org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor}, the default implementation passes only
 * {@code [SystemMessage, ToolResponseMessage_last]} as instructions for the next iteration and relies on the memory
 * advisor to reconstruct context from the database. However, when the memory store is wrapped with
 * {@code ToolCallIntermediateMessageFilteringChatMemory} (which prevents {@link AssistantMessage}-with-tool_calls and
 * {@link ToolResponseMessage} from being persisted to keep the database clean), the memory advisor can no longer
 * provide the required tool-call context.
 *
 * <p>
 * This class overrides {@link #doGetNextInstructionsForToolCall} to extract all
 * {@code (AssistantMessage-with-tool_calls, ToolResponseMessage)} pairs directly from the in-memory
 * {@link ToolExecutionResult#conversationHistory()} and include them in the instructions. The memory advisor then only
 * needs to supply the original user messages (which are still persisted), avoiding both database pollution and context
 * gaps during multi-step tool call loops.
 *
 * @author Marko Kriskovic
 */
public class ToolHistoryToolCallAdvisor extends ToolCallAdvisor {

    private final boolean conversationHistoryEnabled;

    protected ToolHistoryToolCallAdvisor(
        ToolCallingManager toolCallingManager, int advisorOrder, boolean conversationHistoryEnabled,
        boolean streamToolCallResponses) {

        super(toolCallingManager, advisorOrder, conversationHistoryEnabled, streamToolCallResponses);

        this.conversationHistoryEnabled = conversationHistoryEnabled;
    }

    @Override
    protected List<Message> doGetNextInstructionsForToolCall(
        ChatClientRequest chatClientRequest, ChatClientResponse chatClientResponse,
        ToolExecutionResult toolExecutionResult) {

        if (conversationHistoryEnabled) {
            return super.doGetNextInstructionsForToolCall(chatClientRequest, chatClientResponse, toolExecutionResult);
        }

        List<Message> toolCallPairs = extractToolCallPairs(toolExecutionResult.conversationHistory());

        if (toolCallPairs.isEmpty()) {
            return super.doGetNextInstructionsForToolCall(chatClientRequest, chatClientResponse, toolExecutionResult);
        }

        Message systemMessage = chatClientRequest.prompt()
            .getSystemMessage();

        List<Message> instructionMessages = new ArrayList<>(toolCallPairs.size() + 1);

        if (systemMessage != null) {
            instructionMessages.add(systemMessage);
        }

        instructionMessages.addAll(toolCallPairs);

        return instructionMessages;
    }

    @Override
    protected List<Message> doGetNextInstructionsForToolCallStream(
        ChatClientRequest chatClientRequest, ChatClientResponse chatClientResponse,
        ToolExecutionResult toolExecutionResult) {

        if (conversationHistoryEnabled) {
            return super.doGetNextInstructionsForToolCallStream(
                chatClientRequest, chatClientResponse, toolExecutionResult);
        }

        List<Message> toolCallPairs = extractToolCallPairs(toolExecutionResult.conversationHistory());

        if (toolCallPairs.isEmpty()) {
            return super.doGetNextInstructionsForToolCallStream(
                chatClientRequest, chatClientResponse, toolExecutionResult);
        }

        Message systemMessage = chatClientRequest.prompt()
            .getSystemMessage();

        List<Message> instructions = new ArrayList<>(toolCallPairs.size() + 1);

        if (systemMessage != null) {
            instructions.add(systemMessage);
        }

        instructions.addAll(toolCallPairs);

        return instructions;
    }

    /**
     * Extracts all {@code (AssistantMessage-with-tool_calls, ToolResponseMessage)} pairs from a conversation history,
     * ignoring user and system messages. Used to reconstruct tool-call context without relying on persisted history.
     */
    private static List<Message> extractToolCallPairs(List<Message> conversationHistoryMessages) {
        List<Message> toolCallPairMessages = new ArrayList<>();

        for (int i = 0; i < conversationHistoryMessages.size() - 1; i++) {
            if (conversationHistoryMessages.get(i) instanceof AssistantMessage assistantMessage &&
                assistantMessage.hasToolCalls() &&
                conversationHistoryMessages.get(i + 1) instanceof ToolResponseMessage toolResponseMessage) {

                toolCallPairMessages.add(assistantMessage);
                toolCallPairMessages.add(toolResponseMessage);

                i++;
            }
        }

        return toolCallPairMessages;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Fluent builder for {@link ToolHistoryToolCallAdvisor}.
     */
    public static final class Builder extends ToolCallAdvisor.Builder<Builder> {

        @Override
        protected Builder self() {
            return this;
        }

        @Override
        public ToolHistoryToolCallAdvisor build() {
            return new ToolHistoryToolCallAdvisor(
                getToolCallingManager(), getAdvisorOrder(), isConversationHistoryEnabled(),
                isStreamToolCallResponses());
        }
    }
}
