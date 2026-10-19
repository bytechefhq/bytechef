/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.memory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.session.CreateSessionRequest;
import org.springframework.ai.session.InMemorySessionRepository;
import org.springframework.ai.session.SessionEvent;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ResentUserMessageAdvisorTest {

    private static final String SESSION_ID = "thread-1";
    private static final String STORED_MESSAGE_ID = "1b9d6bcd-bbfd-4b2d-9b5d-ab8dfbbd4bed";
    private static final String NEW_MESSAGE_ID = "6ec0bd7f-11c0-43da-975e-2a8ad9ebae0b";

    private AiHubSessionMemory sessionMemory;

    @BeforeEach
    void setUp() {
        sessionMemory = new AiHubSessionMemory(InMemorySessionRepository.builder()
            .build(), null);

        sessionMemory.sessionService()
            .create(CreateSessionRequest.builder()
                .id(SESSION_ID)
                .userId(AiHubSessionMemory.SESSION_USER_ID)
                .build());

        append(STORED_MESSAGE_ID, new UserMessage("Now rename it"), false);
    }

    @Test
    void testDropsTheUserMessageSentAgainUnderTheIdItIsStoredUnder() {
        List<Message> sentMessages = send(STORED_MESSAGE_ID, new UserMessage("Now rename it"));

        assertThat(sentMessages).extracting(Message::getText)
            .containsExactly("You are the AI Hub");
    }

    @Test
    void testKeepsARepeatOfTheSameTextUnderANewId() {
        List<Message> sentMessages = send(NEW_MESSAGE_ID, new UserMessage("Now rename it"));

        assertThat(sentMessages).extracting(Message::getText)
            .containsExactly("You are the AI Hub", "Now rename it");
    }

    @Test
    void testKeepsADifferentTextSentUnderTheStoredId() {
        List<Message> sentMessages = send(STORED_MESSAGE_ID, new UserMessage("Now delete it"));

        assertThat(sentMessages).extracting(Message::getText)
            .containsExactly("You are the AI Hub", "Now delete it");
    }

    @Test
    void testKeepsAMessageWhoseStoredCopyWasTruncatedAway() {
        append(NEW_MESSAGE_ID, new UserMessage("Rename it back"), true);

        List<Message> sentMessages = send(NEW_MESSAGE_ID, new UserMessage("Rename it back"));

        assertThat(sentMessages).extracting(Message::getText)
            .containsExactly("You are the AI Hub", "Rename it back");
    }

    @Test
    void testLeavesAPromptEndingWithAnAssistantMessageAlone() {
        List<Message> sentMessages = send(STORED_MESSAGE_ID, new AssistantMessage("Renamed."));

        assertThat(sentMessages).hasSize(2);
    }

    private void append(String eventId, Message message, boolean archived) {
        sessionMemory.sessionService()
            .appendEvent(SessionEvent.builder()
                .id(eventId)
                .sessionId(SESSION_ID)
                .message(message)
                .archived(archived)
                .build());
    }

    private List<Message> send(String userMessageId, Message lastMessage) {
        CallAdvisorChain chain = mock(CallAdvisorChain.class);

        when(chain.nextCall(any())).thenReturn(mock(ChatClientResponse.class));

        sessionMemory.createResentUserMessageAdvisor()
            .adviseCall(
                ChatClientRequest.builder()
                    .prompt(new Prompt(List.of(new SystemMessage("You are the AI Hub"), lastMessage)))
                    .context(Map.of(
                        ChatMemory.CONVERSATION_ID, SESSION_ID, AiHubSessionMemory.USER_MESSAGE_ID_PARAM,
                        userMessageId))
                    .build(),
                chain);

        ArgumentCaptor<ChatClientRequest> requestCaptor = ArgumentCaptor.forClass(ChatClientRequest.class);

        verify(chain).nextCall(requestCaptor.capture());

        return requestCaptor.getValue()
            .prompt()
            .getInstructions();
    }
}
