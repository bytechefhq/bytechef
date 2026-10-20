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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade.ChatWorkflow;
import com.bytechef.ee.ai.hub.chat.AiHubChat;
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
 * Unit tests for {@link CreateWorkflowChatToolCallback}. They pin the contract the LLM and
 * {@link OpenWorkflowChatTabToolCallback} depend on:
 *
 * <ul>
 * <li>Validation errors and refusals come back as a JSON {@code error} field, never as an exception.</li>
 * <li>Workspace, user and environment come from the {@link ToolContext}, never from the input arguments.</li>
 * <li>A workflow execution id that is not a chat workflow of the workspace is refused before any chat is created.</li>
 * <li>The output shape is {@code {threadId, chatId, title}}.</li>
 * </ul>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class CreateWorkflowChatToolCallbackTest {

    private static final long WORKSPACE_ID = 1L;
    private static final long USER_ID = 3L;

    private final JsonMapper jsonMapper = new JsonMapper();

    private AiHubChatService chatService;
    private ProjectDeploymentFacade projectDeploymentFacade;
    private CreateWorkflowChatToolCallback callback;
    private ToolContext context;

    @BeforeEach
    void beforeEach() {
        chatService = mock(AiHubChatService.class);
        projectDeploymentFacade = mock(ProjectDeploymentFacade.class);

        callback = new CreateWorkflowChatToolCallback(chatService, projectDeploymentFacade);
        context = contextFor(WORKSPACE_ID, USER_ID, null);

        when(projectDeploymentFacade.findWorkspaceChatWorkflow(WORKSPACE_ID, "abc"))
            .thenReturn(Optional.of(listedChatWorkflow("abc", 1L)));
    }

    @Test
    void testToolDefinitionExposesNameAndSchema() {
        ToolDefinition definition = callback.getToolDefinition();

        assertThat(definition.name()).isEqualTo("createWorkflowChat");
        assertThat(definition.description()).isNotBlank();
        assertThat(definition.inputSchema()).contains("workflowExecutionTriggerId");
        assertThat(definition.inputSchema()).contains("projectDeploymentId");
    }

    @Test
    void testRejectsMissingWorkflowExecutionTriggerId() throws Exception {
        JsonNode node = jsonMapper.readTree(callback.call("{\"projectDeploymentId\":\"1\"}", context));

        assertThat(node.has("error")).isTrue();
        assertThat(node.get("error")
            .asText()).contains("workflowExecutionTriggerId");
    }

    @Test
    void testRejectsMissingProjectDeploymentId() throws Exception {
        JsonNode node = jsonMapper.readTree(callback.call("{\"workflowExecutionTriggerId\":\"abc\"}", context));

        assertThat(node.has("error")).isTrue();
        assertThat(node.get("error")
            .asText()).contains("projectDeploymentId");
    }

    @Test
    void testRejectsNonNumericProjectDeploymentId() throws Exception {
        JsonNode node = jsonMapper.readTree(
            callback.call(
                "{\"workflowExecutionTriggerId\":\"abc\",\"projectDeploymentId\":\"not-a-number\"}", context));

        assertThat(node.has("error")).isTrue();
        assertThat(node.get("error")
            .asText()).contains("numeric id");

        verifyNoInteractions(chatService, projectDeploymentFacade);
    }

    @Test
    void testRejectsMissingWorkspaceContext() throws Exception {
        JsonNode node = jsonMapper.readTree(
            callback.call("{\"workflowExecutionTriggerId\":\"abc\",\"projectDeploymentId\":\"1\"}", null));

        assertThat(node.has("error")).isTrue();
        assertThat(node.get("error")
            .asText()).contains("Workspace context unavailable");

        verifyNoInteractions(chatService, projectDeploymentFacade);
    }

    @Test
    void testCreateWorkflowChatRefusesAnUnknownWorkflow() throws Exception {
        when(projectDeploymentFacade.findWorkspaceChatWorkflow(WORKSPACE_ID, "exec-x")).thenReturn(Optional.empty());

        String result = callback.call("{\"workflowExecutionTriggerId\":\"exec-x\",\"projectDeploymentId\":\"42\"}",
            context);

        assertThat(result).contains("not a chat workflow");
        assertThat(jsonMapper.readTree(result)
            .has("error")).isTrue();

        verifyNoInteractions(chatService);
    }

    @Test
    void testCreatesChatAndReturnsThreadId() throws Exception {
        when(chatService.createWorkflowChat(anyLong(), anyLong(), anyInt(), anyString(), anyLong(), isNull()))
            .thenReturn(newChat(101L, "00000000-0000-0000-0000-00000000abc3", "Support bot"));

        JsonNode node = jsonMapper.readTree(
            callback.call("{\"workflowExecutionTriggerId\":\"abc\",\"projectDeploymentId\":\"1\"}", context));

        assertThat(node.get("threadId")
            .asText()).isEqualTo("00000000-0000-0000-0000-00000000abc3");
        assertThat(node.get("chatId")
            .asLong()).isEqualTo(101L);
        assertThat(node.get("title")
            .asText()).isEqualTo("Support bot");

        verify(chatService).createWorkflowChat(eq(WORKSPACE_ID), eq(USER_ID), eq(0), eq("abc"), eq(1L), isNull());
    }

    @Test
    void testStoresTheListingsDeploymentIdInsteadOfAMismatchedClientId() {
        when(chatService.createWorkflowChat(anyLong(), anyLong(), anyInt(), anyString(), anyLong(), isNull()))
            .thenReturn(newChat(101L, "thread", null));

        callback.call("{\"workflowExecutionTriggerId\":\"abc\",\"projectDeploymentId\":\"999\"}", context);

        verify(chatService).createWorkflowChat(eq(WORKSPACE_ID), eq(USER_ID), eq(0), eq("abc"), eq(1L), isNull());
    }

    @Test
    void testCreatesChatInTheTurnEnvironment() {
        when(chatService.createWorkflowChat(anyLong(), anyLong(), anyInt(), anyString(), anyLong(), isNull()))
            .thenReturn(newChat(101L, "thread", null));

        callback.call(
            "{\"workflowExecutionTriggerId\":\"abc\",\"projectDeploymentId\":\"1\"}",
            contextFor(WORKSPACE_ID, USER_ID, 2L));

        verify(chatService).createWorkflowChat(eq(WORKSPACE_ID), eq(USER_ID), eq(2), eq("abc"), eq(1L), isNull());
    }

    @Test
    void testPropagatesTitleArgument() {
        when(chatService.createWorkflowChat(anyLong(), anyLong(), anyInt(), anyString(), anyLong(), eq("My Title")))
            .thenReturn(newChat(1L, "thread", "My Title"));

        callback.call(
            "{\"workflowExecutionTriggerId\":\"abc\",\"projectDeploymentId\":\"1\",\"title\":\"My Title\"}", context);

        verify(chatService).createWorkflowChat(
            eq(WORKSPACE_ID), eq(USER_ID), anyInt(), eq("abc"), eq(1L), eq("My Title"));
    }

    private static ChatWorkflow listedChatWorkflow(String workflowExecutionId, long projectDeploymentId) {
        return new ChatWorkflow(
            projectDeploymentId, 100L, "Support", 1001L, workflowExecutionId, "wf-1", "Support bot");
    }

    private static AiHubChat newChat(long id, String threadId, String title) {
        AiHubChat chat = new AiHubChat(USER_ID);

        chat.setId(id);
        chat.setThreadId(threadId);
        chat.setKind(AiHubChatKind.WORKFLOW_CHAT);
        chat.setTitle(title);

        return chat;
    }

    private static ToolContext contextFor(long workspaceId, long userId, Long environmentId) {
        AiHubToolInvocationContext invocationContext = new AiHubToolInvocationContext(
            workspaceId, userId, null, null, environmentId, "thread-1", null);

        return new ToolContext(invocationContext.toToolContext());
    }
}
