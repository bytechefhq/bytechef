/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.agent;

import java.util.List;
import java.util.Objects;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

/**
 * Drops the user message a run sends again when the session already holds it. A reload keeps the user row and runs it
 * again, so the session memory advisor prepends the stored copy to a prompt that already ends with the message, and the
 * model would read it twice. Ordered after the session memory advisor, so it sees the prompt that advisor built; it
 * changes only a prompt whose last message is a user message right after a user message with the same text.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class ResentUserMessageAdvisor implements CallAdvisor, StreamAdvisor {

    private static final int ORDER = Integer.MAX_VALUE - 2;

    @Override
    public String getName() {
        return "ai-hub-resent-user-message";
    }

    @Override
    public int getOrder() {
        return ORDER;
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

    private static ChatClientRequest withoutResentUserMessage(ChatClientRequest chatClientRequest) {
        Prompt prompt = chatClientRequest.prompt();

        List<Message> messages = prompt.getInstructions();

        if (!endsWithResentUserMessage(messages)) {
            return chatClientRequest;
        }

        return chatClientRequest.mutate()
            .prompt(new Prompt(messages.subList(0, messages.size() - 1), prompt.getOptions()))
            .build();
    }

    private static boolean endsWithResentUserMessage(List<Message> messages) {
        if (messages.size() < 2) {
            return false;
        }

        Message lastMessage = messages.getLast();
        Message previousMessage = messages.get(messages.size() - 2);

        return lastMessage.getMessageType() == MessageType.USER
            && previousMessage.getMessageType() == MessageType.USER
            && Objects.equals(lastMessage.getText(), previousMessage.getText());
    }
}
