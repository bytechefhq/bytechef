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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.ai.hub.chat.AiHubChat;
import com.bytechef.ee.ai.hub.chat.AiHubChatKind;
import com.bytechef.ee.ai.hub.chat.AiHubChatService;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Unit tests for {@link CreateWorkflowChatToolCallback}. The tool is the LLM's create-or-restore handle on workflow
 * chats — the tests pin the contract callers (the LLM, the AI Hub sidebar that re-uses the threadId) depend on:
 *
 * <ul>
 * <li>The tool surfaces validation errors as a JSON {@code error} field rather than throwing — the LLM agent expects
 * tool results to always be JSON.</li>
 * <li>The tool resolves workspace + user from the {@link ToolContext} (not from the input arguments) so an LLM cannot
 * fabricate a workspace id and create chats elsewhere.</li>
 * <li>The output shape is {@code {threadId, chatId, title}} — pinned because {@link OpenWorkflowChatTabToolCallback}
 * reads {@code threadId} verbatim and a rename here would silently break the navigation tool.</li>
 * </ul>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class CreateWorkflowChatToolCallbackTest {

    private final JsonMapper jsonMapper = new JsonMapper();

    @Test
    void testToolDefinitionExposesNameAndSchema() {
        CreateWorkflowChatToolCallback callback =
            new CreateWorkflowChatToolCallback(mock(AiHubChatService.class));

        ToolDefinition definition = callback.getToolDefinition();

        assertThat(definition.name()).isEqualTo("createWorkflowChat");
        assertThat(definition.description()).isNotBlank();
        // Schema must mention both required fields so the LLM gets a useful structured-output hint. We don't
        // assert the full JSON schema (brittle to formatting) — just the field names.
        assertThat(definition.inputSchema()).contains("workflowExecutionTriggerId");
        assertThat(definition.inputSchema()).contains("projectDeploymentId");
    }

    @Test
    void testRejectsMissingWorkflowExecutionTriggerId() throws Exception {
        CreateWorkflowChatToolCallback callback =
            new CreateWorkflowChatToolCallback(mock(AiHubChatService.class));

        String result = callback.call("{\"projectDeploymentId\":\"1\"}", contextFor(7L, 3L));

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("error")).isTrue();
        assertThat(node.get("error")
            .asText()).contains("workflowExecutionTriggerId");
    }

    @Test
    void testRejectsMissingProjectDeploymentId() throws Exception {
        CreateWorkflowChatToolCallback callback =
            new CreateWorkflowChatToolCallback(mock(AiHubChatService.class));

        String result = callback.call("{\"workflowExecutionTriggerId\":\"abc\"}", contextFor(7L, 3L));

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("error")).isTrue();
        assertThat(node.get("error")
            .asText()).contains("projectDeploymentId");
    }

    @Test
    void testRejectsNonNumericProjectDeploymentId() throws Exception {
        CreateWorkflowChatToolCallback callback =
            new CreateWorkflowChatToolCallback(mock(AiHubChatService.class));

        String result = callback.call(
            "{\"workflowExecutionTriggerId\":\"abc\",\"projectDeploymentId\":\"not-a-number\"}", contextFor(7L, 3L));

        JsonNode node = jsonMapper.readTree(result);

        // The shape contract is checked here: project_deployment is the FK and must be a numeric id; an LLM that
        // hallucinates "deploy-123" needs to see a clear error so the next turn re-runs listChatWorkflows rather
        // than retry-looping with the same invalid input.
        assertThat(node.has("error")).isTrue();
        assertThat(node.get("error")
            .asText()).contains("numeric id");
    }

    @Test
    void testRejectsMissingWorkspaceContext() throws Exception {
        CreateWorkflowChatToolCallback callback =
            new CreateWorkflowChatToolCallback(mock(AiHubChatService.class));

        // No tool context — the LLM must NOT be able to forge workspaceId via input arguments. The tool refuses
        // to act when the workspace cannot be resolved from the trusted ToolContext channel.
        String result = callback.call(
            "{\"workflowExecutionTriggerId\":\"abc\",\"projectDeploymentId\":\"1\"}", null);

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("error")).isTrue();
        assertThat(node.get("error")
            .asText()).contains("Workspace context unavailable");
    }

    @Test
    void testCreatesChatAndReturnsThreadId() throws Exception {
        AiHubChatService chatService = mock(AiHubChatService.class);

        AiHubChat persisted =
            newChat(101L, "00000000-0000-0000-0000-00000000abc3", "Support bot");

        when(
            chatService.createWorkflowChat(
                anyLong(), anyLong(), anyInt(), anyString(), anyLong(), isNull())).thenReturn(persisted);

        CreateWorkflowChatToolCallback callback = new CreateWorkflowChatToolCallback(chatService);

        String result = callback.call(
            "{\"workflowExecutionTriggerId\":\"abc\",\"projectDeploymentId\":\"1\"}", contextFor(7L, 3L));

        JsonNode node = jsonMapper.readTree(result);

        // The output shape is the cross-tool contract: openWorkflowChatTab and the client both read these
        // exact field names. Pin them so a rename surfaces as a test failure rather than as a silently broken
        // navigation flow in the product.
        assertThat(node.get("threadId")
            .asText()).isEqualTo("00000000-0000-0000-0000-00000000abc3");
        assertThat(node.get("chatId")
            .asLong()).isEqualTo(101L);
        assertThat(node.get("title")
            .asText()).isEqualTo("Support bot");

        verify(chatService).createWorkflowChat(
            eq(7L), eq(3L), anyInt(), eq("abc"), eq(1L), isNull());
    }

    @Test
    void testPropagatesTitleArgument() throws Exception {
        AiHubChatService chatService = mock(AiHubChatService.class);

        when(
            chatService.createWorkflowChat(
                anyLong(), anyLong(), anyInt(), anyString(), anyLong(), eq("My Title")))
                    .thenReturn(newChat(1L, "thread", "My Title"));

        CreateWorkflowChatToolCallback callback = new CreateWorkflowChatToolCallback(chatService);

        callback.call(
            "{\"workflowExecutionTriggerId\":\"abc\",\"projectDeploymentId\":\"1\",\"title\":\"My Title\"}",
            contextFor(7L, 3L));

        // Title goes through verbatim — the chat service is responsible for null/blank handling, so we
        // just verify the tool doesn't strip or transform the value.
        verify(chatService).createWorkflowChat(
            eq(7L), eq(3L), anyInt(), eq("abc"), eq(1L), eq("My Title"));
    }

    private static AiHubChat newChat(long id, String threadId, String title) {
        AiHubChat chat = new AiHubChat(3L);

        chat.setId(id);
        chat.setThreadId(threadId);
        chat.setKind(AiHubChatKind.WORKFLOW_CHAT);
        chat.setTitle(title);

        return chat;
    }

    private static ToolContext contextFor(long workspaceId, long userId) {
        return new ToolContext(Map.of(
            AiHubToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY, workspaceId,
            AiHubToolInvocationContext.TOOL_CONTEXT_USER_ID_KEY, userId));
    }
}
