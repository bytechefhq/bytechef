/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.agent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientMessageAggregator;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class EmptyToolFollowUpAdvisor implements CallAdvisor, StreamAdvisor {

    static final String CONTINUE_PROMPT = "Please continue.";

    private static final Logger log = LoggerFactory.getLogger(EmptyToolFollowUpAdvisor.class);

    private static final int ORDER = Integer.MAX_VALUE - 2;

    private final ChatClientMessageAggregator chatClientMessageAggregator = new ChatClientMessageAggregator();

    @Override
    public String getName() {
        return "ai-hub-empty-tool-follow-up";
    }

    @Override
    public int getOrder() {
        return ORDER;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
        ChatClientResponse chatClientResponse = callAdvisorChain.nextCall(chatClientRequest);

        if (!followsToolResult(chatClientRequest) || !isEmpty(chatClientResponse)) {
            return chatClientResponse;
        }

        logContinuation(chatClientRequest);

        CallAdvisorChain retryCallAdvisorChain = callAdvisorChain.copy(this);

        return retryCallAdvisorChain.nextCall(withContinuePrompt(chatClientRequest));
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(
        ChatClientRequest chatClientRequest, StreamAdvisorChain streamAdvisorChain) {

        Flux<ChatClientResponse> responseFlux = streamAdvisorChain.nextStream(chatClientRequest);

        if (!followsToolResult(chatClientRequest)) {
            return responseFlux;
        }

        AtomicReference<ChatClientResponse> aggregatedResponseReference = new AtomicReference<>();

        return chatClientMessageAggregator.aggregateChatClientResponse(responseFlux, aggregatedResponseReference::set)
            .concatWith(Flux.defer(() -> {
                if (!isEmpty(aggregatedResponseReference.get())) {
                    return Flux.empty();
                }

                logContinuation(chatClientRequest);

                StreamAdvisorChain retryStreamAdvisorChain = streamAdvisorChain.copy(this);

                return retryStreamAdvisorChain.nextStream(withContinuePrompt(chatClientRequest));
            }));
    }

    static boolean followsToolResult(ChatClientRequest chatClientRequest) {
        Prompt prompt = chatClientRequest.prompt();

        List<Message> instructions = prompt.getInstructions();

        if (instructions.isEmpty()) {
            return false;
        }

        return instructions.getLast() instanceof ToolResponseMessage;
    }

    static boolean isEmpty(@Nullable ChatClientResponse chatClientResponse) {
        if (chatClientResponse == null) {
            return false;
        }

        ChatResponse chatResponse = chatClientResponse.chatResponse();

        if (chatResponse == null) {
            return false;
        }

        for (Generation generation : chatResponse.getResults()) {
            AssistantMessage assistantMessage = generation.getOutput();

            if (assistantMessage.hasToolCalls()) {
                return false;
            }

            String text = assistantMessage.getText();

            if (text != null && !text.isBlank()) {
                return false;
            }
        }

        return true;
    }

    private static ChatClientRequest withContinuePrompt(ChatClientRequest chatClientRequest) {
        Prompt prompt = chatClientRequest.prompt();

        List<Message> instructions = new ArrayList<>(prompt.getInstructions());

        instructions.add(new UserMessage(CONTINUE_PROMPT));

        return chatClientRequest.mutate()
            .prompt(new Prompt(instructions, prompt.getOptions()))
            .build();
    }

    private static void logContinuation(ChatClientRequest chatClientRequest) {
        Prompt prompt = chatClientRequest.prompt();

        List<Message> instructions = prompt.getInstructions();

        log.warn(
            "The model ended its turn with an empty response right after a tool result ({} messages); asking it once "
                + "to continue",
            instructions.size());
    }
}
