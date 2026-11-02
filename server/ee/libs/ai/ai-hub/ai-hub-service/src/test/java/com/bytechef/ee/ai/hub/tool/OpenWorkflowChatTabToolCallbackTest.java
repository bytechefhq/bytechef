/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.ee.ai.hub.chat.AiHubChat;
import com.bytechef.ee.ai.hub.chat.AiHubChatAccessPolicy;
import com.bytechef.ee.ai.hub.chat.AiHubChatKind;
import com.bytechef.ee.ai.hub.chat.AiHubChatService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Unit tests for {@link OpenWorkflowChatTabToolCallback}. The client switches the active chat by the {@code chatId} the
 * tool answers with, so the tests pin the {@code {opened, chatId, threadId, title}} shape and every refusal.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class OpenWorkflowChatTabToolCallbackTest {

    private static final long CHAT_ID = 101L;
    private static final String THREAD_ID = "00000000-0000-0000-0000-00000000abc3";
    private static final long USER_ID = 7L;
    private static final long WORKSPACE_ID = 3L;

    private final JsonMapper jsonMapper = new JsonMapper();

    private AiHubChatAccessPolicy accessPolicy;
    private OpenWorkflowChatTabToolCallback callback;
    private AiHubChatService chatService;
    private ToolContext context;

    @BeforeEach
    void beforeEach() {
        accessPolicy = mock(AiHubChatAccessPolicy.class);
        chatService = mock(AiHubChatService.class);

        callback = new OpenWorkflowChatTabToolCallback(chatService, accessPolicy);
        context = contextFor(WORKSPACE_ID, USER_ID);

        when(accessPolicy.canParticipate(any(AiHubChat.class), anyLong())).thenAnswer(
            invocation -> {
                AiHubChat chat = invocation.getArgument(0);

                return chat.getUserId() == (long) invocation.getArgument(1);
            });
    }

    @Test
    void testToolDefinitionExposesName() {
        ToolDefinition definition = callback.getToolDefinition();

        assertThat(definition.name()).isEqualTo("openWorkflowChatTab");
        assertThat(definition.inputSchema()).contains("threadId");
    }

    @Test
    void testRejectsMissingThreadId() throws Exception {
        JsonNode node = jsonMapper.readTree(callback.call("{}", context));

        assertThat(node.has("error")).isTrue();
        assertThat(node.get("error")
            .asText()).contains("threadId");

        verifyNoInteractions(chatService);
    }

    @Test
    void testRejectsBlankThreadId() throws Exception {
        JsonNode node = jsonMapper.readTree(callback.call("{\"threadId\":\"   \"}", context));

        assertThat(node.has("error")).isTrue();

        verifyNoInteractions(chatService);
    }

    @Test
    void testRejectsMissingWorkspaceContext() throws Exception {
        JsonNode node = jsonMapper.readTree(callback.call("{\"threadId\":\"" + THREAD_ID + "\"}", null));

        assertThat(node.get("error")
            .asText()).contains("Workspace context unavailable");

        verifyNoInteractions(chatService);
    }

    @Test
    void testReturnsChatIdThreadIdAndTitle() throws Exception {
        stubChat(newChat(USER_ID, WORKSPACE_ID, AiHubChatKind.WORKFLOW_CHAT, "Stored title"));

        String result = callback.call("{\"threadId\":\"" + THREAD_ID + "\",\"title\":\"Support bot\"}", context);

        assertThat(result).isEqualTo(
            "{\"opened\":true,\"chatId\":101,\"threadId\":\"00000000-0000-0000-0000-00000000abc3\"," +
                "\"title\":\"Support bot\"}");
    }

    @Test
    void testFallsBackToTheStoredTitle() throws Exception {
        stubChat(newChat(USER_ID, WORKSPACE_ID, AiHubChatKind.WORKFLOW_CHAT, "Stored title"));

        JsonNode node = jsonMapper.readTree(callback.call("{\"threadId\":\"" + THREAD_ID + "\"}", context));

        assertThat(node.get("opened")
            .asBoolean()).isTrue();
        assertThat(node.get("chatId")
            .asLong()).isEqualTo(CHAT_ID);
        assertThat(node.get("title")
            .asText()).isEqualTo("Stored title");
    }

    @Test
    void testRefusesAnUnknownThreadId() throws Exception {
        when(chatService.findNotDeletedByThreadId(THREAD_ID)).thenReturn(Optional.empty());

        assertRefused(callback.call("{\"threadId\":\"" + THREAD_ID + "\"}", context));
    }

    @Test
    void testRefusesAChatOfAnotherWorkspace() throws Exception {
        stubChat(newChat(USER_ID, WORKSPACE_ID + 1, AiHubChatKind.WORKFLOW_CHAT, "Other workspace"));

        assertRefused(callback.call("{\"threadId\":\"" + THREAD_ID + "\"}", context));
    }

    @Test
    void testRefusesAChatOfAnotherUser() throws Exception {
        stubChat(newChat(USER_ID + 1, WORKSPACE_ID, AiHubChatKind.WORKFLOW_CHAT, "Other user"));

        assertRefused(callback.call("{\"threadId\":\"" + THREAD_ID + "\"}", context));
    }

    @Test
    void testRefusesAStandardChat() throws Exception {
        stubChat(newChat(USER_ID, WORKSPACE_ID, AiHubChatKind.STANDARD, "Standard chat"));

        assertRefused(callback.call("{\"threadId\":\"" + THREAD_ID + "\"}", context));
    }

    private void assertRefused(String result) throws Exception {
        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("opened")).isFalse();
        assertThat(node.has("chatId")).isFalse();
        assertThat(node.get("error")
            .asText()).contains("No workflow chat with threadId " + THREAD_ID);
    }

    private void stubChat(AiHubChat chat) {
        when(chatService.findNotDeletedByThreadId(THREAD_ID)).thenReturn(Optional.of(chat));
    }

    private static AiHubChat newChat(long userId, long workspaceId, AiHubChatKind kind, String title) {
        AiHubChat chat = new AiHubChat(userId);

        chat.setId(CHAT_ID);
        chat.setKind(kind);
        chat.setThreadId(THREAD_ID);
        chat.setTitle(title);
        chat.setWorkspaceId(workspaceId);

        return chat;
    }

    private static ToolContext contextFor(long workspaceId, long userId) {
        AiHubToolInvocationContext invocationContext = new AiHubToolInvocationContext(
            workspaceId, userId, null, null, null, "hub-thread", null);

        return new ToolContext(invocationContext.toToolContext());
    }
}
