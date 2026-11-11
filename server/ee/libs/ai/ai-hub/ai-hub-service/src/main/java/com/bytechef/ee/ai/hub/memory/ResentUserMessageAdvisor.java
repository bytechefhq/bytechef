/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.memory;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class ResentUserMessageAdvisor implements CallAdvisor, StreamAdvisor {

    private final AiHubSessionMemory sessionMemory;
    private final int order;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    ResentUserMessageAdvisor(AiHubSessionMemory sessionMemory, int order) {
        this.sessionMemory = sessionMemory;
        this.order = order;
    }

    @Override
    public String getName() {
        return "ai-hub-resent-user-message";
    }

    @Override
    public int getOrder() {
        return order;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
        return callAdvisorChain.nextCall(withoutResentUserMessage(chatClientRequest));
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(
        ChatClientRequest chatClientRequest, StreamAdvisorChain streamAdvisorChain) {

        return streamAdvisorChain.nextStream(withoutResentUserMessage(chatClientRequest));
    }

    private ChatClientRequest withoutResentUserMessage(ChatClientRequest chatClientRequest) {
        Prompt prompt = chatClientRequest.prompt();

        List<Message> messages = prompt.getInstructions();

        if (messages.isEmpty() || !sessionMemory.isResentUserMessage(chatClientRequest, messages.getLast())) {
            return chatClientRequest;
        }

        return chatClientRequest.mutate()
            .prompt(new Prompt(messages.subList(0, messages.size() - 1), prompt.getOptions()))
            .build();
    }
}
