/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.ee.ai.hub.chat.AiHubChat;
import com.bytechef.ee.ai.hub.chat.AiHubChatKind;
import com.bytechef.ee.ai.hub.chat.AiHubChatService;
import com.bytechef.ee.ai.hub.chat.AiHubChatStatus;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.ai.chat.model.ToolContext;
import tools.jackson.databind.JsonNode;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class ListAiHubChatsToolCallbackTest {

    private static final long USER_ID = 3L;
    private static final long WORKSPACE_ID = 7L;

    private AiHubChatService chatService;
    private ListAiHubChatsToolCallback callback;

    @BeforeEach
    void beforeEach() {
        chatService = mock(AiHubChatService.class);

        callback = new ListAiHubChatsToolCallback(chatService);
    }

    @Test
    void testToolDefinitionExposesNameAndSchema() {
        assertThat(callback.getToolDefinition()
            .name()).isEqualTo("listAiHubChats");
        assertThat(callback.getToolDefinition()
            .inputSchema()).contains("status");
    }

    @Test
    void testListsActiveChatsOfTheCallerByDefault() {
        when(chatService.list(anyLong(), anyLong(), anyInt(), eq(AiHubChatStatus.ACTIVE)))
            .thenReturn(List.of(buildChat(11L, "Onboarding research", "Summarize the onboarding flow")));

        JsonNode node = readTree(callback.call("{}", contextWithEnvironment(1L)));

        verify(chatService).list(WORKSPACE_ID, USER_ID, 1, AiHubChatStatus.ACTIVE);

        assertThat(node.isArray()).isTrue();
        assertThat(node.size()).isEqualTo(1);

        JsonNode first = node.get(0);

        assertThat(first.get("id")
            .asLong()).isEqualTo(11L);
        assertThat(first.get("title")
            .asText()).isEqualTo("Onboarding research");
        assertThat(first.get("status")
            .asText()).isEqualTo("ACTIVE");
        assertThat(first.get("kind")
            .asText()).isEqualTo("STANDARD");
    }

    @Test
    void testPassesTheRequestedStatusCaseInsensitively() {
        when(chatService.list(anyLong(), anyLong(), anyInt(), eq(AiHubChatStatus.ARCHIVED)))
            .thenReturn(List.of());

        callback.call("{\"status\":\"archived\"}", contextWithEnvironment(0L));

        verify(chatService).list(WORKSPACE_ID, USER_ID, 0, AiHubChatStatus.ARCHIVED);
    }

    @Test
    void testFallsBackToActiveForAnUnknownStatus() {
        when(chatService.list(anyLong(), anyLong(), anyInt(), eq(AiHubChatStatus.ACTIVE)))
            .thenReturn(List.of());

        callback.call("{\"status\":\"PINNED\"}", contextWithEnvironment(0L));

        verify(chatService).list(WORKSPACE_ID, USER_ID, 0, AiHubChatStatus.ACTIVE);
    }

    @Test
    void testTruncatesLongTitlesAndPreviews() {
        when(chatService.list(anyLong(), anyLong(), anyInt(), eq(AiHubChatStatus.ACTIVE)))
            .thenReturn(List.of(buildChat(12L, "t".repeat(300), "p".repeat(500))));

        JsonNode first = readTree(callback.call("{}", contextWithEnvironment(0L)))
            .get(0);

        assertThat(first.get("title")
            .asText()).hasSize(121);
        assertThat(first.get("lastPreview")
            .asText()).hasSize(201);
    }

    @Test
    void testRejectsAMissingWorkspaceContext() {
        JsonNode node = readTree(callback.call("{}", null));

        assertThat(node.get("error")
            .asText()).contains("Workspace/user context unavailable");

        verifyNoInteractions(chatService);
    }

    @Test
    void testReportsInvalidInput() {
        JsonNode node = readTree(callback.call("not json", contextWithEnvironment(0L)));

        assertThat(node.has("error")).isTrue();

        verifyNoInteractions(chatService);
    }

    private static AiHubChat buildChat(long id, String title, String lastPreview) {
        AiHubChat chat = new AiHubChat();

        chat.setId(id);
        chat.setKind(AiHubChatKind.STANDARD);
        chat.setLastPreview(lastPreview);
        chat.setMessageCount(4);
        chat.setStatus(AiHubChatStatus.ACTIVE);
        chat.setTitle(title);

        return chat;
    }

    private static JsonNode readTree(String json) {
        return JsonUtils.read(json, JsonNode.class);
    }

    private static ToolContext contextWithEnvironment(long environmentId) {
        AiHubToolInvocationContext invocationContext = new AiHubToolInvocationContext(
            WORKSPACE_ID, USER_ID, null, null, environmentId, "thread-1", null);

        return new ToolContext(invocationContext.toToolContext());
    }
}
