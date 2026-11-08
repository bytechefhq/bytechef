/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.ai.hub.chat.AiHubChat;
import com.bytechef.ee.ai.hub.chat.AiHubChatService;
import com.bytechef.ee.ai.hub.chat.AiHubChatToolConnectionValidator;
import com.bytechef.ee.ai.hub.chat.AiHubChatToolFacade;
import com.bytechef.ee.ai.hub.metric.AiHubToolAttachMetrics;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * @version ee
 * @author Ivica Cardic
 */
class AttachChatToolToolCallbackTest {

    private static final long CHAT_ID = 7L;
    private static final long OWNER_USER_ID = 10L;
    private static final long WORKSPACE_ID = 1L;

    private final JsonMapper jsonMapper = new JsonMapper();
    private final AiHubChatService chatService = mock(AiHubChatService.class);
    private final AiHubChatToolFacade chatToolFacade = mock(AiHubChatToolFacade.class);
    private final AiHubChatToolConnectionValidator connectionValidator = mock(AiHubChatToolConnectionValidator.class);
    private final AttachChatToolToolCallback callback = new AttachChatToolToolCallback(
        chatService, chatToolFacade, connectionValidator, mock(AiHubToolAttachMetrics.class));

    @BeforeEach
    void beforeEach() {
        AiHubChat chat = mock(AiHubChat.class);

        when(chat.getId()).thenReturn(CHAT_ID);
        when(chat.getUserId()).thenReturn(OWNER_USER_ID);
        when(chat.getEnvironmentId()).thenReturn(0L);
        when(chatService.findNotDeletedByThreadId("thread-1")).thenReturn(Optional.of(chat));
        when(chatService.getWorkspaceId(CHAT_ID)).thenReturn(WORKSPACE_ID);
    }

    @Test
    void testAttachInvokesFacadeAndEchoesIds() {
        when(chatToolFacade.findChatComponentIdIgnoringConnection(CHAT_ID, "slack", 1, 0))
            .thenReturn(Optional.empty());
        when(chatToolFacade.attachComponent(CHAT_ID, "slack", 1, 42L, 0)).thenReturn(99L);
        when(chatToolFacade.addTool(99L, "sendMessage", Map.of("channel", "#engineering"))).thenReturn(11L);

        JsonNode node = call(
            OWNER_USER_ID,
            "{\"componentName\":\"slack\",\"actionName\":\"sendMessage\",\"parameters\":{\"channel\":\"#engineering\"},"
                +
                "\"connectionId\":\"42\"}");

        assertThat(node.get("chatToolId")
            .asLong()).isEqualTo(11L);
        assertThat(node.get("chatComponentId")
            .asLong()).isEqualTo(99L);
        assertThat(node.get("attachedToolName")
            .asString()).isEqualTo("slack_sendMessage");

        verify(connectionValidator).validate(WORKSPACE_ID, "slack", 42L, 0);
    }

    @Test
    void testReattachWithDifferentConnectionRebindsInPlace() {
        when(chatToolFacade.findChatComponentIdIgnoringConnection(CHAT_ID, "slack", 1, 0))
            .thenReturn(Optional.of(99L));
        when(chatToolFacade.addTool(99L, "sendMessage", Map.of())).thenReturn(11L);

        JsonNode node = call(
            OWNER_USER_ID,
            "{\"componentName\":\"slack\",\"actionName\":\"sendMessage\",\"parameters\":{},\"connectionId\":\"42\"}");

        assertThat(node.get("chatComponentId")
            .asLong()).isEqualTo(99L);

        verify(chatToolFacade).setComponentConnection(99L, 42L);
        verify(chatToolFacade, never()).attachComponent(anyLong(), anyString(), anyInt(), any(), anyInt());
    }

    @Test
    void testRejectsSenderWhoIsNotTheChatOwner() {
        JsonNode node = call(
            20L, "{\"componentName\":\"slack\",\"actionName\":\"sendMessage\",\"parameters\":{}}");

        assertThat(node.get("error")
            .asString()).contains("Only the owner");

        verify(chatToolFacade, never()).attachComponent(anyLong(), anyString(), anyInt(), any(), anyInt());
        verify(chatToolFacade, never()).addTool(anyLong(), anyString(), any());
    }

    @Test
    void testRejectsConnectionNotAvailableInTheWorkspaceAndEnvironment() {
        doThrow(new IllegalArgumentException("Connection 42 is not a slack connection available"))
            .when(connectionValidator)
            .validate(WORKSPACE_ID, "slack", 42L, 0);

        JsonNode node = call(
            OWNER_USER_ID,
            "{\"componentName\":\"slack\",\"actionName\":\"sendMessage\",\"parameters\":{},\"connectionId\":\"42\"}");

        assertThat(node.get("error")
            .asString()).contains("Connection 42");

        verify(chatToolFacade, never()).attachComponent(anyLong(), anyString(), anyInt(), any(), anyInt());
        verify(chatToolFacade, never()).setComponentConnection(anyLong(), any());
        verify(chatToolFacade, never()).addTool(anyLong(), anyString(), any());
    }

    @Test
    void testReturnsToolErrorOnMissingThreadId() {
        String result = callback.call("{\"componentName\":\"slack\",\"actionName\":\"sendMessage\",\"parameters\":{}}");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.get("error")
            .asString()).contains("AiHubChat context unavailable");
    }

    @Test
    void testReturnsToolErrorOnMalformedConnectionId() {
        JsonNode node = call(
            OWNER_USER_ID,
            "{\"componentName\":\"slack\",\"actionName\":\"sendMessage\",\"parameters\":{}," +
                "\"connectionId\":\"not-a-number\"}");

        assertThat(node.get("error")
            .asString()).contains("connectionId");
    }

    private JsonNode call(long userId, String toolInput) {
        ToolContext toolContext = new ToolContext(
            new AiHubToolInvocationContext(WORKSPACE_ID, userId, (short) 0, "x", 0L, "thread-1").toToolContext());

        return jsonMapper.readTree(callback.call(toolInput, toolContext));
    }
}
