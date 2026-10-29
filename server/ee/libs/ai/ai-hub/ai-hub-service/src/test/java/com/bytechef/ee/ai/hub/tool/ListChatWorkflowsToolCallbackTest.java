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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade.ChatWorkflow;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Unit tests for {@link ListChatWorkflowsToolCallback}: the workspace and environment come from the
 * {@link ToolContext}, the {@code projectId} filter narrows the listing, and each item carries exactly the four fields
 * the agent passes on to {@code createWorkflowChat}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class ListChatWorkflowsToolCallbackTest {

    private static final long WORKSPACE_ID = 7L;

    private final JsonMapper jsonMapper = new JsonMapper();

    private ProjectDeploymentFacade projectDeploymentFacade;
    private ListChatWorkflowsToolCallback callback;

    @BeforeEach
    void beforeEach() {
        projectDeploymentFacade = mock(ProjectDeploymentFacade.class);

        callback = new ListChatWorkflowsToolCallback(projectDeploymentFacade);

        when(projectDeploymentFacade.getWorkspaceChatWorkflows(anyLong(), any()))
            .thenReturn(
                List.of(
                    new ChatWorkflow(11L, 100L, "Support", 1001L, "exec-1", "wf-1", "Support bot"),
                    new ChatWorkflow(12L, 200L, "Sales", 2001L, "exec-2", "wf-2", "Lead qualifier")));
    }

    @Test
    void testToolDefinitionExposesNameAndSchema() {
        ToolDefinition definition = callback.getToolDefinition();

        assertThat(definition.name()).isEqualTo("listChatWorkflows");
        assertThat(definition.description()).isNotBlank();
        assertThat(definition.inputSchema()).contains("projectId");
    }

    @Test
    void testListChatWorkflowsUsesTheTurnEnvironment() {
        callback.call("{}", contextWithEnvironment(1L));

        verify(projectDeploymentFacade).getWorkspaceChatWorkflows(WORKSPACE_ID, 1L);
    }

    @Test
    void testListsEveryChatWorkflowWithTheFourFields() throws Exception {
        JsonNode node = jsonMapper.readTree(callback.call("{}", contextWithEnvironment(0L)));

        assertThat(node.isArray()).isTrue();
        assertThat(node.size()).isEqualTo(2);

        JsonNode first = node.get(0);

        assertThat(first.propertyNames()).containsExactlyInAnyOrder(
            "projectDeploymentId", "projectId", "workflowExecutionTriggerId", "workflowLabel");
        assertThat(first.get("projectDeploymentId")
            .asText()).isEqualTo("11");
        assertThat(first.get("projectId")
            .asText()).isEqualTo("100");
        assertThat(first.get("workflowExecutionTriggerId")
            .asText()).isEqualTo("exec-1");
        assertThat(first.get("workflowLabel")
            .asText()).isEqualTo("Support bot");
    }

    @Test
    void testFiltersByProjectId() throws Exception {
        JsonNode node = jsonMapper.readTree(callback.call("{\"projectId\":\"200\"}", contextWithEnvironment(0L)));

        assertThat(node.size()).isEqualTo(1);
        assertThat(node.get(0)
            .get("workflowExecutionTriggerId")
            .asText()).isEqualTo("exec-2");
    }

    @Test
    void testRejectsNonNumericProjectId() throws Exception {
        JsonNode node = jsonMapper.readTree(callback.call("{\"projectId\":\"abc\"}", contextWithEnvironment(0L)));

        assertThat(node.has("error")).isTrue();
        assertThat(node.get("error")
            .asText()).contains("projectId");
    }

    @Test
    void testRejectsMissingWorkspaceContext() throws Exception {
        JsonNode node = jsonMapper.readTree(callback.call("{}", null));

        assertThat(node.has("error")).isTrue();
        assertThat(node.get("error")
            .asText()).contains("Workspace context unavailable");

        verifyNoInteractions(projectDeploymentFacade);
    }

    private static ToolContext contextWithEnvironment(long environmentId) {
        AiHubToolInvocationContext invocationContext = new AiHubToolInvocationContext(
            WORKSPACE_ID, 3L, null, null, environmentId, "thread-1", null);

        return new ToolContext(invocationContext.toToolContext());
    }
}
