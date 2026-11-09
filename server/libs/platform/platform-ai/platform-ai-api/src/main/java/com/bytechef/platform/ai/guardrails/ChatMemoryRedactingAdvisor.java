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

package com.bytechef.platform.ai.guardrails;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.UnaryOperator;
import org.springframework.ai.chat.client.ChatClientMessageAggregator;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

/**
 * @author Ivica Cardic
 */
public final class ChatMemoryRedactingAdvisor implements BaseAdvisor {

    private final BaseAdvisor memoryAdvisor;
    private final UnaryOperator<String> redactor;

    public ChatMemoryRedactingAdvisor(BaseAdvisor memoryAdvisor, UnaryOperator<String> redactor) {
        this.memoryAdvisor = Objects.requireNonNull(memoryAdvisor, "memoryAdvisor");
        this.redactor = Objects.requireNonNull(redactor, "redactor");
    }

    @Override
    public ChatClientRequest before(ChatClientRequest chatClientRequest, AdvisorChain advisorChain) {
        return memoryAdvisor.before(chatClientRequest, advisorChain);
    }

    @Override
    public ChatClientResponse after(ChatClientResponse chatClientResponse, AdvisorChain advisorChain) {
        memoryAdvisor.after(redact(chatClientResponse), advisorChain);

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
    public String getName() {
        return memoryAdvisor.getName();
    }

    @Override
    public int getOrder() {
        return memoryAdvisor.getOrder();
    }

    @Override
    public Scheduler getScheduler() {
        return memoryAdvisor.getScheduler();
    }

    private ChatClientResponse redact(ChatClientResponse chatClientResponse) {
        ChatResponse chatResponse = chatClientResponse.chatResponse();

        if (chatResponse == null) {
            return chatClientResponse;
        }

        List<Generation> generations = chatResponse.getResults();
        List<Generation> redactedGenerations = new ArrayList<>(generations.size());
        boolean redacted = false;

        for (Generation generation : generations) {
            AssistantMessage assistantMessage = generation.getOutput();
            String text = assistantMessage == null ? null : assistantMessage.getText();

            if (text == null || text.isEmpty()) {
                redactedGenerations.add(generation);

                continue;
            }

            String redactedText = redactor.apply(text);

            if (Objects.equals(redactedText, text)) {
                redactedGenerations.add(generation);

                continue;
            }

            redacted = true;

            AssistantMessage redactedAssistantMessage = AssistantMessage.builder()
                .content(redactedText)
                .properties(assistantMessage.getMetadata())
                .toolCalls(assistantMessage.getToolCalls())
                .media(assistantMessage.getMedia())
                .build();

            redactedGenerations.add(new Generation(redactedAssistantMessage, generation.getMetadata()));
        }

        if (!redacted) {
            return chatClientResponse;
        }

        ChatResponse redactedChatResponse = ChatResponse.builder()
            .generations(redactedGenerations)
            .metadata(chatResponse.getMetadata())
            .build();

        return chatClientResponse.mutate()
            .chatResponse(redactedChatResponse)
            .build();
    }
}
