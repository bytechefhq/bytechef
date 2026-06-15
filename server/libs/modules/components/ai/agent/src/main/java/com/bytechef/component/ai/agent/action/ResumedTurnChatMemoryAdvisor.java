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
import org.springframework.ai.chat.client.ChatClientMessageAggregator;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

/**
 * @author Ivica Cardic
 */
class ResumedTurnChatMemoryAdvisor implements BaseAdvisor {

    private final ChatMemory chatMemory;
    private final int order;

    ResumedTurnChatMemoryAdvisor(ChatMemory chatMemory, int order) {
        this.chatMemory = chatMemory;
        this.order = order;
    }

    @Override
    public ChatClientRequest before(ChatClientRequest chatClientRequest, AdvisorChain advisorChain) {
        return chatClientRequest;
    }

    @Override
    public ChatClientResponse after(ChatClientResponse chatClientResponse, AdvisorChain advisorChain) {
        ChatResponse chatResponse = chatClientResponse.chatResponse();
        Object conversationId = chatClientResponse.context()
            .get(ChatMemory.CONVERSATION_ID);

        if (chatResponse == null || conversationId == null) {
            return chatClientResponse;
        }

        List<Message> assistantMessages = chatResponse.getResults()
            .stream()
            .map(generation -> (Message) generation.getOutput())
            .toList();

        chatMemory.add(conversationId.toString(), assistantMessages);

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
}
