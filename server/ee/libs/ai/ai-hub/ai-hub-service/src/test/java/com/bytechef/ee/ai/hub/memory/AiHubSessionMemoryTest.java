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
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class AiHubSessionMemoryTest {

    private static final String USER_MESSAGE_ID = "1b9d6bcd-bbfd-4b2d-9b5d-ab8dfbbd4bed";

    @Test
    void testStoresTheUserMessageUnderItsAgUiMessageId() {
        assertThat(AiHubSessionMemory.requestEventId(request(USER_MESSAGE_ID), new UserMessage("Now rename it")))
            .isEqualTo(USER_MESSAGE_ID);
    }

    @Test
    void testStoresAUserMessageWhoseIdIsNotAUuidUnderARandomId() {
        String eventId = AiHubSessionMemory.requestEventId(request("message-1"), new UserMessage("Now rename it"));

        assertThat(eventId).isNotEqualTo("message-1");
        assertThat(UUID.fromString(eventId)).isNotNull();
    }

    @Test
    void testStoresAToolResponseUnderARandomId() {
        Message toolResponse = ToolResponseMessage.builder()
            .responses(List.of(new ToolResponseMessage.ToolResponse("call-1", "listConnections", "[]")))
            .build();

        assertThat(AiHubSessionMemory.requestEventId(request(USER_MESSAGE_ID), toolResponse))
            .isNotEqualTo(USER_MESSAGE_ID);
    }

    private static ChatClientRequest request(String userMessageId) {
        return ChatClientRequest.builder()
            .prompt(new Prompt(List.of()))
            .context(Map.of(AiHubSessionMemory.USER_MESSAGE_ID_PARAM, userMessageId))
            .build();
    }
}
