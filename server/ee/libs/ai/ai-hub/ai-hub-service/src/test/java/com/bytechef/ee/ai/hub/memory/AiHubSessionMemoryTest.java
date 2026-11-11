/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.memory;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
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
class AiHubSessionMemoryTest {

    private static final String SESSION_ID = "thread-1";
    private static final String USER_MESSAGE_ID = "1b9d6bcd-bbfd-4b2d-9b5d-ab8dfbbd4bed";

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
    }

    @Test
    void testStoresANewUserMessageUnderItsAgUiMessageId() {
        assertThat(sessionMemory.requestEventId(request(USER_MESSAGE_ID), new UserMessage("Now rename it")))
            .isEqualTo(USER_MESSAGE_ID);
    }

    @Test
    void testReusesTheIdOfTheSameMessageSentAgain() {
        append(new UserMessage("Now rename it"), false);

        assertThat(sessionMemory.requestEventId(request(USER_MESSAGE_ID), new UserMessage("Now rename it")))
            .isEqualTo(USER_MESSAGE_ID);
    }

    @Test
    void testStoresAMessageSentUnderATruncatedIdAsANewRow() {
        append(new UserMessage("Now rename it"), true);

        assertThat(sessionMemory.requestEventId(request(USER_MESSAGE_ID), new UserMessage("Now rename it")))
            .isNotEqualTo(USER_MESSAGE_ID);
    }

    @Test
    void testStoresADifferentTextSentUnderAStoredIdAsANewRow() {
        append(new UserMessage("Now rename it"), false);

        assertThat(sessionMemory.requestEventId(request(USER_MESSAGE_ID), new UserMessage("Now delete it")))
            .isNotEqualTo(USER_MESSAGE_ID);
    }

    @Test
    void testStoresAUserMessageWhoseIdIsNotAUuidUnderARandomId() {
        String eventId = sessionMemory.requestEventId(request("message-1"), new UserMessage("Now rename it"));

        assertThat(eventId).isNotEqualTo("message-1");
        assertThat(UUID.fromString(eventId)).isNotNull();
    }

    @Test
    void testStoresAToolResponseUnderARandomId() {
        Message toolResponse = ToolResponseMessage.builder()
            .responses(List.of(new ToolResponseMessage.ToolResponse("call-1", "listConnections", "[]")))
            .build();

        assertThat(sessionMemory.requestEventId(request(USER_MESSAGE_ID), toolResponse))
            .isNotEqualTo(USER_MESSAGE_ID);
    }

    private void append(Message message, boolean archived) {
        sessionMemory.sessionService()
            .appendEvent(SessionEvent.builder()
                .id(USER_MESSAGE_ID)
                .sessionId(SESSION_ID)
                .message(message)
                .archived(archived)
                .build());
    }

    private static ChatClientRequest request(String userMessageId) {
        return ChatClientRequest.builder()
            .prompt(new Prompt(List.of()))
            .context(
                Map.of(ChatMemory.CONVERSATION_ID, SESSION_ID, AiHubSessionMemory.USER_MESSAGE_ID_PARAM, userMessageId))
            .build();
    }
}
