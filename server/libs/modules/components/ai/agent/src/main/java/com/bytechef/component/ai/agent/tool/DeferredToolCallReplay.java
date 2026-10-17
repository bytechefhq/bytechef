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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

/**
 * The tool calls a resumed turn runs before the model is asked again. When the model called several tools in one round
 * and one of them suspended the agent, {@link SuspendableToolCallingManager} did not execute the others; on resume
 * their results are replaced with {@link #DEFERRED_TOOL_RESULT} and the calls are issued again, as the model's next
 * round, so they run (and a gated one raises its own approval) without depending on the model to repeat them.
 *
 * @author Ivica Cardic
 */
public record DeferredToolCallReplay(List<Message> conversation, List<AssistantMessage.ToolCall> toolCalls) {

    public static final String DEFERRED_TOOL_RESULT =
        "Deferred: not executed in this round because another tool call in it paused the conversation for a human " +
            "response. It was issued again once that response arrived; its result follows.";

    static final String REPLAYED_TOOL_CALL_ID_SUFFIX = "_deferred";

    public DeferredToolCallReplay {
        conversation = List.copyOf(conversation);
        toolCalls = List.copyOf(toolCalls);
    }

    /**
     * Finds the calls of the conversation's last tool round that were not executed because another call of the round
     * suspended the agent.
     */
    public static DeferredToolCallReplay of(List<Message> conversation) {
        int toolResponseIndex = conversation.size() - 1;

        if (toolResponseIndex < 1 ||
            !(conversation.get(toolResponseIndex) instanceof ToolResponseMessage toolResponseMessage) ||
            !(conversation.get(toolResponseIndex - 1) instanceof AssistantMessage assistantMessage) ||
            !assistantMessage.hasToolCalls()) {

            return new DeferredToolCallReplay(conversation, List.of());
        }

        Map<String, AssistantMessage.ToolCall> toolCallsById = new HashMap<>();

        for (AssistantMessage.ToolCall toolCall : assistantMessage.getToolCalls()) {
            toolCallsById.put(toolCall.id(), toolCall);
        }

        List<ToolResponseMessage.ToolResponse> toolResponses = new ArrayList<>();
        List<AssistantMessage.ToolCall> deferredToolCalls = new ArrayList<>();

        for (ToolResponseMessage.ToolResponse toolResponse : toolResponseMessage.getResponses()) {
            AssistantMessage.ToolCall toolCall = toolCallsById.get(toolResponse.id());

            if (toolCall == null ||
                !SuspendableToolCallingManager.NOT_EXECUTED_TOOL_RESULT.equals(toolResponse.responseData())) {

                toolResponses.add(toolResponse);

                continue;
            }

            toolResponses.add(
                new ToolResponseMessage.ToolResponse(toolResponse.id(), toolResponse.name(), DEFERRED_TOOL_RESULT));

            deferredToolCalls.add(
                new AssistantMessage.ToolCall(
                    toolCall.id() + REPLAYED_TOOL_CALL_ID_SUFFIX, toolCall.type(), toolCall.name(),
                    toolCall.arguments()));
        }

        if (deferredToolCalls.isEmpty()) {
            return new DeferredToolCallReplay(conversation, List.of());
        }

        List<Message> replayConversation = new ArrayList<>(conversation.subList(0, toolResponseIndex));

        replayConversation.add(
            ToolResponseMessage.builder()
                .responses(toolResponses)
                .metadata(toolResponseMessage.getMetadata())
                .build());

        return new DeferredToolCallReplay(replayConversation, deferredToolCalls);
    }

    /**
     * Returns a chat model whose first response issues the deferred tool calls, after which it delegates to
     * {@code chatModel}; returns {@code chatModel} itself when nothing was deferred.
     */
    public ChatModel replayingChatModel(ChatModel chatModel) {
        if (toolCalls.isEmpty()) {
            return chatModel;
        }

        return new ReplayingChatModel(chatModel, toolCalls);
    }

    private static final class ReplayingChatModel implements ChatModel {

        private final ChatModel delegate;
        private final AtomicReference<@Nullable List<AssistantMessage.ToolCall>> pendingToolCalls;

        private ReplayingChatModel(ChatModel delegate, List<AssistantMessage.ToolCall> toolCalls) {
            this.delegate = delegate;
            this.pendingToolCalls = new AtomicReference<>(toolCalls);
        }

        @Override
        public ChatResponse call(Prompt prompt) {
            List<AssistantMessage.ToolCall> toolCalls = pendingToolCalls.getAndSet(null);

            if (toolCalls == null) {
                return delegate.call(prompt);
            }

            return toolCallResponse(toolCalls);
        }

        @Override
        public Flux<ChatResponse> stream(Prompt prompt) {
            List<AssistantMessage.ToolCall> toolCalls = pendingToolCalls.getAndSet(null);

            if (toolCalls == null) {
                return delegate.stream(prompt);
            }

            return Flux.just(toolCallResponse(toolCalls));
        }

        @Override
        public ChatOptions getDefaultOptions() {
            return delegate.getDefaultOptions();
        }

        @Override
        public ChatOptions getOptions() {
            return delegate.getOptions();
        }

        private static ChatResponse toolCallResponse(List<AssistantMessage.ToolCall> toolCalls) {
            AssistantMessage assistantMessage = AssistantMessage.builder()
                .content("")
                .toolCalls(toolCalls)
                .build();

            return ChatResponse.builder()
                .generations(List.of(new Generation(assistantMessage)))
                .build();
        }
    }
}
