/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.ai.hub.chat.AiHubChat;
import com.bytechef.ee.ai.hub.chat.AiHubChatService;
import com.bytechef.ee.ai.hub.chat.AiHubChatToolFacade;
import com.bytechef.ee.ai.hub.metric.AiHubToolAttachMetrics;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class AttachChatToolToolCallbackTest {

    private final JsonMapper jsonMapper = new JsonMapper();

    @Test
    void testAttachInvokesFacadeAndEchoesIds() throws Exception {
        // Pin the two-phase facade orchestration: attach component (returns componentId), then add tool
        // (returns toolId). The LLM-facing output carries both ids so a follow-up turn can address either.
        AiHubChatService chatService = mock(AiHubChatService.class);
        AiHubChatToolFacade chatToolFacade = mock(AiHubChatToolFacade.class);

        AiHubChat chat = mock(AiHubChat.class);
        when(chat.getId()).thenReturn(7L);
        when(chatService.findByThreadId("thread-1")).thenReturn(Optional.of(chat));

        when(chatToolFacade.attachComponent(7L, "slack", 1, 42L, 0)).thenReturn(99L);
        when(
            chatToolFacade.addTool(eq(99L), eq("sendMessage"), eq(java.util.Map.of("channel", "#engineering"))))
                .thenReturn(11L);

        AttachChatToolToolCallback callback = new AttachChatToolToolCallback(chatService, chatToolFacade,
            mock(ConnectionService.class), mock(AiHubToolAttachMetrics.class));

        ToolContext toolContext = new ToolContext(
            new AiHubToolInvocationContext(1L, 10L, (short) 0, "x", 0L, "thread-1").toToolContext());

        String result = callback.call(
            "{\"componentName\":\"slack\",\"actionName\":\"sendMessage\",\"parameters\":{\"channel\":\"#engineering\"},"
                +
                "\"connectionId\":\"42\"}",
            toolContext);

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.get("chatToolId")
            .asLong()).isEqualTo(11L);
        assertThat(node.get("chatComponentId")
            .asLong()).isEqualTo(99L);
        assertThat(node.get("attachedToolName")
            .asText()).isEqualTo("slack_sendMessage");

        verify(chatToolFacade).attachComponent(7L, "slack", 1, 42L, 0);
        verify(chatToolFacade).addTool(99L, "sendMessage", java.util.Map.of("channel", "#engineering"));
    }

    @Test
    void testReattachWithDifferentConnectionRebindsInPlace() throws Exception {
        // Autonomous flow: agent first attaches with connectionId=null during discovery, then back-fills the
        // real id after the user picks one in the chat picker. Without the hardened lookup, this would create
        // a SECOND ai_hub_chat_component row because connectionId is part of attachComponent's idempotency tuple.
        AiHubChatService chatService = mock(AiHubChatService.class);
        AiHubChatToolFacade chatToolFacade = mock(AiHubChatToolFacade.class);

        AiHubChat chat = mock(AiHubChat.class);

        when(chat.getId()).thenReturn(7L);
        when(chatService.findByThreadId("thread-1")).thenReturn(Optional.of(chat));

        // The existing component binding (with null connection) is found by the new ignore-connection lookup.
        when(chatToolFacade.findChatComponentIdIgnoringConnection(7L, "slack", 1, 0)).thenReturn(Optional.of(99L));

        when(chatToolFacade.addTool(eq(99L), eq("sendMessage"), eq(java.util.Map.of())))
            .thenReturn(11L);

        AttachChatToolToolCallback callback =
            new AttachChatToolToolCallback(chatService, chatToolFacade, mock(ConnectionService.class),
                mock(AiHubToolAttachMetrics.class));

        ToolContext toolContext = new ToolContext(
            new AiHubToolInvocationContext(1L, 10L, (short) 0, "x", 0L, "thread-1").toToolContext());

        String result = callback.call(
            "{\"componentName\":\"slack\",\"actionName\":\"sendMessage\",\"parameters\":{}," +
                "\"connectionId\":\"42\"}",
            toolContext);

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.get("chatComponentId")
            .asLong()).isEqualTo(99L);
        assertThat(node.get("chatToolId")
            .asLong()).isEqualTo(11L);

        // Critical assertions: setComponentConnection runs (rebind in place); attachComponent never runs (no new row).
        verify(chatToolFacade).setComponentConnection(99L, 42L);
        verify(chatToolFacade, org.mockito.Mockito.never())
            .attachComponent(org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void testReturnsToolErrorOnMissingThreadId() throws Exception {
        AttachChatToolToolCallback callback = new AttachChatToolToolCallback(
            mock(AiHubChatService.class), mock(AiHubChatToolFacade.class), mock(ConnectionService.class),
            mock(AiHubToolAttachMetrics.class));

        String result = callback.call(
            "{\"componentName\":\"slack\",\"actionName\":\"sendMessage\",\"parameters\":{}}");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("error")).isTrue();
        assertThat(node.get("error")
            .asText()).contains("AiHubChat context unavailable");
    }

    @Test
    void testRejectsCrossEnvironmentConnection() throws Exception {
        // Defense-in-depth: the LLM passed a connectionId that resolves to a different environment than the
        // chat. The autonomous flow would normally not produce this because listConnectionsForComponent filters
        // by env, but a hallucinated id from prior chat memory could slip past. We reject before the facade so
        // the cross-env binding never persists.
        AiHubChatService chatService = mock(AiHubChatService.class);
        AiHubChatToolFacade chatToolFacade = mock(AiHubChatToolFacade.class);
        ConnectionService connectionService = mock(ConnectionService.class);

        AiHubChat chat = mock(AiHubChat.class);

        when(chat.getId()).thenReturn(7L);
        when(chatService.findByThreadId("thread-1")).thenReturn(Optional.of(chat));

        Connection connection = mock(Connection.class);

        when(connection.getEnvironmentId()).thenReturn(2);
        when(connectionService.getConnection(42L)).thenReturn(connection);

        AttachChatToolToolCallback callback = new AttachChatToolToolCallback(
            chatService, chatToolFacade, connectionService, mock(AiHubToolAttachMetrics.class));

        // Chat's environment from the invocation context is 0; the connection's environment is 2 → mismatch.
        ToolContext toolContext = new ToolContext(
            new AiHubToolInvocationContext(1L, 10L, (short) 0, "x", 0L, "thread-1").toToolContext());

        String result = callback.call(
            "{\"componentName\":\"slack\",\"actionName\":\"sendMessage\",\"parameters\":{},"
                + "\"connectionId\":\"42\"}",
            toolContext);

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("error")).isTrue();
        assertThat(node.get("error")
            .asText()).contains("environment 2");
        assertThat(node.get("error")
            .asText()).contains("environment 0");

        // Critical: neither facade call should fire on the rejected path.
        verify(chatToolFacade, org.mockito.Mockito.never())
            .attachComponent(org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyInt());
        verify(chatToolFacade, org.mockito.Mockito.never())
            .setComponentConnection(org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.any());
        verify(chatToolFacade, org.mockito.Mockito.never())
            .addTool(org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void testReturnsToolErrorOnMalformedConnectionId() throws Exception {
        // Bad connection-id is almost always a hallucination — surface as a tool-error rather than silently
        // attaching with null connection (which would make the next invocation fail with the
        // "connectionRequired" envelope, confusing the LLM about cause).
        AiHubChatService chatService = mock(AiHubChatService.class);
        AiHubChat chat = mock(AiHubChat.class);

        when(chat.getId()).thenReturn(7L);
        when(chatService.findByThreadId("thread-1")).thenReturn(Optional.of(chat));

        AttachChatToolToolCallback callback = new AttachChatToolToolCallback(chatService,
            mock(AiHubChatToolFacade.class), mock(ConnectionService.class), mock(AiHubToolAttachMetrics.class));

        ToolContext toolContext = new ToolContext(
            new AiHubToolInvocationContext(1L, 10L, (short) 0, "x", 0L, "thread-1").toToolContext());

        String result = callback.call(
            "{\"componentName\":\"slack\",\"actionName\":\"sendMessage\",\"parameters\":{}," +
                "\"connectionId\":\"not-a-number\"}",
            toolContext);

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("error")).isTrue();
        assertThat(node.get("error")
            .asText()).contains("connectionId");
    }
}
