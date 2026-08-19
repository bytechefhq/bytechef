/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ee.ai.hub.chat.AiHubChat;
import com.bytechef.ee.ai.hub.chat.AiHubChatService;
import com.bytechef.ee.ai.hub.chat.AiHubChatToolBinding;
import com.bytechef.ee.ai.hub.chat.AiHubChatToolFacade;
import com.bytechef.ee.ai.hub.metric.AiHubToolAttachMetrics;
import java.util.List;
import java.util.Map;
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
class ListChatToolsToolCallbackTest {

    private final JsonMapper jsonMapper = new JsonMapper();

    @Test
    void testListsAttachedTools() throws Exception {
        AiHubChatService chatService = mock(AiHubChatService.class);
        AiHubChatToolFacade chatToolFacade = mock(AiHubChatToolFacade.class);

        AiHubChat chat = mock(AiHubChat.class);

        when(chat.getId()).thenReturn(7L);
        when(chatService.findByThreadId("thread-1")).thenReturn(Optional.of(chat));

        AiHubChatToolBinding binding = new AiHubChatToolBinding(
            11L, 99L, 7L, "slack", 1, "sendMessage", 42L, 0, Map.of("channel", "#engineering"));

        when(chatToolFacade.listChatTools(7L)).thenReturn(List.of(binding));

        ListChatToolsToolCallback callback =
            new ListChatToolsToolCallback(chatService, chatToolFacade, mock(AiHubToolAttachMetrics.class), jsonMapper);

        ToolContext toolContext = new ToolContext(
            new AiHubToolInvocationContext(1L, 10L, (short) 0, "x", 0L, "thread-1").toToolContext());

        String result = callback.call("{}", toolContext);

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.get("tools")
            .size()).isEqualTo(1);
        assertThat(node.get("tools")
            .get(0)
            .get("componentName")
            .asText()).isEqualTo("slack");
        assertThat(node.get("tools")
            .get(0)
            .get("actionName")
            .asText()).isEqualTo("sendMessage");
        assertThat(node.get("tools")
            .get(0)
            .get("connectionId")
            .asLong()).isEqualTo(42L);
    }

    @Test
    void testReturnsEmptyToolsArrayWhenNoneAttached() throws Exception {
        AiHubChatService chatService = mock(AiHubChatService.class);
        AiHubChatToolFacade chatToolFacade = mock(AiHubChatToolFacade.class);

        AiHubChat chat = mock(AiHubChat.class);

        when(chat.getId()).thenReturn(7L);
        when(chatService.findByThreadId("thread-1")).thenReturn(Optional.of(chat));

        when(chatToolFacade.listChatTools(7L)).thenReturn(List.of());

        ListChatToolsToolCallback callback =
            new ListChatToolsToolCallback(chatService, chatToolFacade, mock(AiHubToolAttachMetrics.class), jsonMapper);

        ToolContext toolContext = new ToolContext(
            new AiHubToolInvocationContext(1L, 10L, (short) 0, "x", 0L, "thread-1").toToolContext());

        String result = callback.call("{}", toolContext);

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.get("tools")
            .isEmpty()).isTrue();
    }

    @Test
    void testReturnsToolErrorWhenContextMissing() throws Exception {
        ListChatToolsToolCallback callback = new ListChatToolsToolCallback(
            mock(AiHubChatService.class), mock(AiHubChatToolFacade.class), mock(AiHubToolAttachMetrics.class),
            jsonMapper);

        String result = callback.call("{}");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("error")).isTrue();
        assertThat(node.get("error")
            .asText()).contains("AiHubChat context unavailable");
    }

    @Test
    void testReturnsToolErrorWhenChatNotFound() throws Exception {
        AiHubChatService chatService = mock(AiHubChatService.class);

        when(chatService.findByThreadId("missing")).thenReturn(Optional.empty());

        ListChatToolsToolCallback callback = new ListChatToolsToolCallback(
            chatService, mock(AiHubChatToolFacade.class), mock(AiHubToolAttachMetrics.class), jsonMapper);

        ToolContext toolContext = new ToolContext(
            new AiHubToolInvocationContext(1L, 10L, (short) 0, "x", 0L, "missing").toToolContext());

        String result = callback.call("{}", toolContext);

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("error")).isTrue();
        assertThat(node.get("error")
            .asText()).contains("AiHubChat not found");
    }
}
