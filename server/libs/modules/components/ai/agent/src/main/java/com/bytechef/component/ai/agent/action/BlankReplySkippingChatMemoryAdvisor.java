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
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

/**
 * Wraps the agent's chat memory advisor so that an assistant reply without text, tool calls or media is never stored;
 * when a streamed turn suspends on a tool call, the aggregated reply is such a blank message. A resumed turn uses
 * {@link ResumedTurnChatMemoryAdvisor} instead.
 *
 * @author Ivica Cardic
 */
final class BlankReplySkippingChatMemoryAdvisor implements BaseAdvisor {

    private final BaseAdvisor memoryAdvisor;

    BlankReplySkippingChatMemoryAdvisor(BaseAdvisor memoryAdvisor) {
        this.memoryAdvisor = Objects.requireNonNull(memoryAdvisor, "memoryAdvisor");
    }

    @Override
    public ChatClientRequest before(ChatClientRequest chatClientRequest, AdvisorChain advisorChain) {
        return memoryAdvisor.before(chatClientRequest, advisorChain);
    }

    @Override
    public ChatClientResponse after(ChatClientResponse chatClientResponse, AdvisorChain advisorChain) {
        ChatResponse chatResponse = chatClientResponse.chatResponse();

        if (chatResponse == null) {
            return memoryAdvisor.after(chatClientResponse, advisorChain);
        }

        List<Generation> generations = chatResponse.getResults();

        List<Generation> nonBlankGenerations = generations.stream()
            .filter(generation -> !ResumedTurnChatMemoryAdvisor.isBlankAssistantMessage(generation.getOutput()))
            .toList();

        if (nonBlankGenerations.size() == generations.size()) {
            return memoryAdvisor.after(chatClientResponse, advisorChain);
        }

        if (!nonBlankGenerations.isEmpty()) {
            memoryAdvisor.after(
                chatClientResponse.mutate()
                    .chatResponse(new ChatResponse(nonBlankGenerations, chatResponse.getMetadata()))
                    .build(),
                advisorChain);
        }

        return chatClientResponse;
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(
        ChatClientRequest chatClientRequest, StreamAdvisorChain streamAdvisorChain) {

        Scheduler scheduler = getScheduler();

        return Mono.just(chatClientRequest)
            .publishOn(scheduler)
            .map(request -> before(request, streamAdvisorChain))
            .flatMapMany(streamAdvisorChain::nextStream)
            .transform(flux -> new ChatClientMessageAggregator().aggregateChatClientResponse(
                flux, chatClientResponse -> after(chatClientResponse, streamAdvisorChain)));
    }

    @Override
    public int getOrder() {
        return memoryAdvisor.getOrder();
    }

    @Override
    public Scheduler getScheduler() {
        return memoryAdvisor.getScheduler();
    }
}
