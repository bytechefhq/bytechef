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

import java.util.List;
import java.util.Objects;
import org.springframework.ai.chat.client.ChatClientMessageAggregator;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

/**
 * Stores only the final assistant reply of a resumed agent turn in the chat memory, skipping blank replies. It writes
 * either straight to the {@link ChatMemory} or through the chat memory advisor when the memory is not exposed.
 *
 * @author Ivica Cardic
 */
final class ResumedTurnChatMemoryAdvisor implements BaseAdvisor {

    private final MemoryWriter memoryWriter;
    private final int order;

    ResumedTurnChatMemoryAdvisor(ChatMemory chatMemory, int order) {
        Objects.requireNonNull(chatMemory, "chatMemory");

        this.memoryWriter = (chatClientResponse, generations, advisorChain) -> {
            Object conversationId = chatClientResponse.context()
                .get(ChatMemory.CONVERSATION_ID);

            if (conversationId == null) {
                return;
            }

            List<Message> assistantMessages = generations.stream()
                .map(generation -> (Message) generation.getOutput())
                .toList();

            chatMemory.add(conversationId.toString(), assistantMessages);
        };
        this.order = order;
    }

    ResumedTurnChatMemoryAdvisor(BaseAdvisor memoryAdvisor) {
        Objects.requireNonNull(memoryAdvisor, "memoryAdvisor");

        this.memoryWriter = (chatClientResponse, generations, advisorChain) -> {
            ChatResponse chatResponse = Objects.requireNonNull(chatClientResponse.chatResponse());

            memoryAdvisor.after(
                chatClientResponse.mutate()
                    .chatResponse(new ChatResponse(generations, chatResponse.getMetadata()))
                    .build(),
                advisorChain);
        };
        this.order = memoryAdvisor.getOrder();
    }

    @Override
    public ChatClientRequest before(ChatClientRequest chatClientRequest, AdvisorChain advisorChain) {
        return chatClientRequest;
    }

    @Override
    public ChatClientResponse after(ChatClientResponse chatClientResponse, AdvisorChain advisorChain) {
        ChatResponse chatResponse = chatClientResponse.chatResponse();

        if (chatResponse == null) {
            return chatClientResponse;
        }

        List<Generation> generations = chatResponse.getResults()
            .stream()
            .filter(generation -> !isBlankAssistantMessage(generation.getOutput()))
            .toList();

        if (!generations.isEmpty()) {
            memoryWriter.write(chatClientResponse, generations, advisorChain);
        }

        return chatClientResponse;
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(
        ChatClientRequest chatClientRequest, StreamAdvisorChain streamAdvisorChain) {

        Scheduler scheduler = getScheduler();

        return Mono.just(chatClientRequest)
            .publishOn(scheduler)
            .flatMapMany(streamAdvisorChain::nextStream)
            .transform(flux -> new ChatClientMessageAggregator().aggregateChatClientResponse(
                flux, chatClientResponse -> after(chatClientResponse, streamAdvisorChain)));
    }

    @Override
    public int getOrder() {
        return order;
    }

    static boolean isBlankAssistantMessage(AssistantMessage assistantMessage) {
        String text = assistantMessage.getText();

        return !assistantMessage.hasToolCalls() && (text == null || text.isBlank()) && assistantMessage.getMedia()
            .isEmpty();
    }

    @FunctionalInterface
    private interface MemoryWriter {

        void write(ChatClientResponse chatClientResponse, List<Generation> generations, AdvisorChain advisorChain);
    }
}
