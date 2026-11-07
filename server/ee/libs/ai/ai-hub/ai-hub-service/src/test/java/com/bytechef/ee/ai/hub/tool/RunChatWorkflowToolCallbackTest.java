/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade.ChatWorkflow;
import com.bytechef.platform.configuration.facade.WorkflowFacade;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.security.access.AccessDeniedException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class RunChatWorkflowToolCallbackTest {

    private static final String TRIGGER_ID = "exec-1";
    private static final long USER_ID = 3L;
    private static final long WORKSPACE_ID = 1L;

    private final JsonMapper jsonMapper = new JsonMapper();

    private AiHubChatArtifactRecorder artifactRecorder;
    private RunChatWorkflowToolCallback callback;
    private ToolContext context;
    private ProjectDeploymentFacade projectDeploymentFacade;
    private WorkflowFacade workflowFacade;

    @BeforeEach
    void beforeEach() {
        artifactRecorder = mock(AiHubChatArtifactRecorder.class);
        projectDeploymentFacade = mock(ProjectDeploymentFacade.class);
        workflowFacade = mock(WorkflowFacade.class);

        callback = new RunChatWorkflowToolCallback(projectDeploymentFacade, workflowFacade, artifactRecorder);
        context = contextFor(WORKSPACE_ID, USER_ID, "thread-1");

        when(projectDeploymentFacade.findWorkspaceChatWorkflow(WORKSPACE_ID, TRIGGER_ID))
            .thenReturn(Optional.of(listedChatWorkflow()));
    }

    @Test
    void testToolDefinitionExposesNameAndSchema() {
        ToolDefinition definition = callback.getToolDefinition();

        assertThat(definition.name()).isEqualTo("runChatWorkflow");
        assertThat(definition.description()).isNotBlank();
        assertThat(definition.inputSchema()).contains("workflowExecutionTriggerId");
    }

    @Test
    void testRejectsMissingWorkflowExecutionTriggerId() {
        JsonNode node = jsonMapper.readTree(callback.call("{}", context));

        assertThat(node.get("error")
            .asString()).contains("workflowExecutionTriggerId");
        verifyNoInteractions(projectDeploymentFacade, workflowFacade, artifactRecorder);
    }

    @Test
    void testRejectsMalformedInput() {
        JsonNode node = jsonMapper.readTree(callback.call("not json", context));

        assertThat(node.has("error")).isTrue();
        verifyNoInteractions(projectDeploymentFacade, workflowFacade, artifactRecorder);
    }

    @Test
    void testRejectsMissingWorkspaceContext() {
        JsonNode node = jsonMapper.readTree(callback.call(input(TRIGGER_ID), null));

        assertThat(node.get("error")
            .asString()).contains("Workspace context unavailable");
        verifyNoInteractions(projectDeploymentFacade, workflowFacade, artifactRecorder);
    }

    @Test
    void testRefusesAWorkflowThatIsNotAChatWorkflowOfTheWorkspace() {
        when(projectDeploymentFacade.findWorkspaceChatWorkflow(WORKSPACE_ID, "other-workspace-exec"))
            .thenReturn(Optional.empty());

        JsonNode node = jsonMapper.readTree(callback.call(input("other-workspace-exec"), context));

        assertThat(node.get("error")
            .asString()).contains("is not an enabled chat workflow of this workspace");
        verifyNoInteractions(workflowFacade, artifactRecorder);
    }

    @Test
    void testRefusesWhenTheUserMayNotViewTheWorkspaceWorkflows() {
        when(projectDeploymentFacade.findWorkspaceChatWorkflow(WORKSPACE_ID, TRIGGER_ID))
            .thenThrow(new AccessDeniedException("denied"));

        JsonNode node = jsonMapper.readTree(callback.call(input(TRIGGER_ID), context));

        assertThat(node.get("error")
            .asString()).contains("permission");
        verifyNoInteractions(workflowFacade, artifactRecorder);
    }

    @Test
    void testReturnsTheStreamUrlForAStreamingWorkflow() {
        when(workflowFacade.hasSseStreamResponse("wf-1")).thenReturn(true);

        JsonNode node = jsonMapper.readTree(callback.call(input(TRIGGER_ID), context));

        assertThat(node.get("workflowExecutionId")
            .asString()).isEqualTo(TRIGGER_ID);
        assertThat(node.get("streaming")
            .asBoolean()).isTrue();
        assertThat(node.get("streamUrl")
            .asString()).isEqualTo("/webhooks/exec-1/sse");
        assertThat(node.has("responseUrl")).isFalse();
    }

    @Test
    void testReturnsTheResponseUrlForASingleResponseWorkflow() {
        when(workflowFacade.hasSseStreamResponse("wf-1")).thenReturn(false);

        JsonNode node = jsonMapper.readTree(callback.call(input(TRIGGER_ID), context));

        assertThat(node.get("streaming")
            .asBoolean()).isFalse();
        assertThat(node.get("responseUrl")
            .asString()).isEqualTo("/webhooks/exec-1");
        assertThat(node.has("streamUrl")).isFalse();
    }

    @Test
    void testIgnoresAnUnknownInputArgument() {
        JsonNode node = jsonMapper.readTree(
            callback.call("{\"workflowExecutionTriggerId\":\"exec-1\",\"input\":{\"name\":\"x\"}}", context));

        assertThat(node.get("responseUrl")
            .asString()).isEqualTo("/webhooks/exec-1");
    }

    @Test
    void testRecordsTheExecutionStartedArtifact() {
        callback.call(input(TRIGGER_ID), context);

        verify(artifactRecorder).record("thread-1", USER_ID, "WORKFLOW_EXECUTION_STARTED", TRIGGER_ID, "Support bot");
    }

    @Test
    void testSkipsTheArtifactWithoutAThread() {
        callback.call(input(TRIGGER_ID), contextFor(WORKSPACE_ID, USER_ID, null));

        verifyNoInteractions(artifactRecorder);
    }

    @Test
    void testARecorderFailureDoesNotFailTheRun() {
        doThrow(new IllegalStateException("boom")).when(artifactRecorder)
            .record(anyString(), anyLong(), anyString(), anyString(), anyString());

        JsonNode node = jsonMapper.readTree(callback.call(input(TRIGGER_ID), context));

        assertThat(node.has("error")).isFalse();
        assertThat(node.get("responseUrl")
            .asString()).isEqualTo("/webhooks/exec-1");
    }

    @Test
    void testRunsWithoutARecorder() {
        RunChatWorkflowToolCallback callbackWithoutRecorder = new RunChatWorkflowToolCallback(
            projectDeploymentFacade, workflowFacade, null);

        JsonNode node = jsonMapper.readTree(callbackWithoutRecorder.call(input(TRIGGER_ID), context));

        assertThat(node.get("responseUrl")
            .asString()).isEqualTo("/webhooks/exec-1");
    }

    @Test
    void testAFacadeFailureBecomesAToolError() {
        when(workflowFacade.hasSseStreamResponse("wf-1")).thenThrow(new IllegalStateException("db down"));

        JsonNode node = jsonMapper.readTree(callback.call(input(TRIGGER_ID), context));

        assertThat(node.has("error")).isTrue();
        verifyNoInteractions(artifactRecorder);
    }

    private static ToolContext contextFor(long workspaceId, long userId, String threadId) {
        AiHubToolInvocationContext invocationContext = new AiHubToolInvocationContext(
            workspaceId, userId, null, null, null, threadId, null);

        return new ToolContext(invocationContext.toToolContext());
    }

    private static String input(String workflowExecutionTriggerId) {
        return "{\"workflowExecutionTriggerId\":\"" + workflowExecutionTriggerId + "\"}";
    }

    private static ChatWorkflow listedChatWorkflow() {
        return new ChatWorkflow(42L, 100L, "Support", 1001L, TRIGGER_ID, "wf-1", "Support bot");
    }
}
