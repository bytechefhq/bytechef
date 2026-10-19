/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ResentUserMessageAdvisorTest {

    @Test
    void testDropsTheResentCopyOfTheStoredUserMessage() {
        List<Message> sentMessages = send(
            new SystemMessage("You are the AI Hub"), new UserMessage("Bind a Slack connection"),
            new AssistantMessage("Bound."), new UserMessage("Now rename it"), new UserMessage("Now rename it"));

        assertThat(sentMessages).extracting(Message::getText)
            .containsExactly("You are the AI Hub", "Bind a Slack connection", "Bound.", "Now rename it");
    }

    @Test
    void testKeepsANewUserMessageAfterAnAssistantReply() {
        List<Message> sentMessages = send(
            new UserMessage("Now rename it"), new AssistantMessage("Renamed."), new UserMessage("Now rename it"));

        assertThat(sentMessages).hasSize(3);
    }

    @Test
    void testKeepsAUserMessageAfterADifferentUserMessage() {
        List<Message> sentMessages = send(new UserMessage("Bind a Slack connection"), new UserMessage("Now rename it"));

        assertThat(sentMessages).hasSize(2);
    }

    private static List<Message> send(Message... messages) {
        CallAdvisorChain chain = mock(CallAdvisorChain.class);

        when(chain.nextCall(any())).thenReturn(mock(ChatClientResponse.class));

        new ResentUserMessageAdvisor().adviseCall(
            ChatClientRequest.builder()
                .prompt(new Prompt(List.of(messages)))
                .build(),
            chain);

        ArgumentCaptor<ChatClientRequest> requestCaptor = ArgumentCaptor.forClass(ChatClientRequest.class);

        verify(chain).nextCall(requestCaptor.capture());

        return requestCaptor.getValue()
            .prompt()
            .getInstructions();
    }
}
