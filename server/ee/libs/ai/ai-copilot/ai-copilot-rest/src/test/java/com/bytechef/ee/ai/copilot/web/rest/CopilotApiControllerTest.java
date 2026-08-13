/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.web.rest;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agui.core.state.State;
import com.agui.server.LocalAgent;
import com.agui.server.spring.AgUiParameters;
import com.agui.server.spring.AgUiService;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class CopilotApiControllerTest {

    private final AgUiService agUiService = mock(AgUiService.class);
    private final PermissionService permissionService = mock(PermissionService.class);
    private final ProjectWorkflowService projectWorkflowService = mock(ProjectWorkflowService.class);

    private final CopilotApiController controller = new CopilotApiController(
        agUiService, List.of(localAgent("converter_build"), localAgent("workflow_editor_ask")),
        Optional.of(permissionService), Optional.of(projectWorkflowService), Optional.empty());

    @Test
    void testChatDeniedWhenUserLacksWorkflowScope() {
        AgUiParameters parameters = parameters(Map.<String, Object>of("workflowId", "wf1", "mode", "ASK"));

        givenWorkflowProject("wf1", 42L);

        when(permissionService.hasWorkspaceScopeForProject(42L, "WORKFLOW_VIEW")).thenReturn(false);

        assertThatThrownBy(() -> controller.chat("workflow_editor", parameters))
            .isInstanceOf(AccessDeniedException.class);

        verify(agUiService, never()).runAgent(any(), any());
    }

    @Test
    void testChatBuildRequiresWorkflowEditScope() {
        AgUiParameters parameters = parameters(Map.<String, Object>of("workflowId", "wf1", "mode", "BUILD"));

        givenWorkflowProject("wf1", 42L);

        when(permissionService.hasWorkspaceScopeForProject(42L, "WORKFLOW_EDIT")).thenReturn(false);

        assertThatThrownBy(() -> controller.chat("workflow_editor", parameters))
            .isInstanceOf(AccessDeniedException.class);

        verify(permissionService).hasWorkspaceScopeForProject(42L, "WORKFLOW_EDIT");
    }

    @Test
    void testChatAllowedWhenUserHasScope() {
        AgUiParameters parameters = parameters(Map.<String, Object>of("workflowId", "wf1", "mode", "ASK"));

        givenWorkflowProject("wf1", 42L);

        when(permissionService.hasWorkspaceScopeForProject(42L, "WORKFLOW_VIEW")).thenReturn(true);
        when(agUiService.runAgent(any(), any())).thenReturn(new SseEmitter());

        controller.chat("workflow_editor", parameters);

        verify(agUiService).runAgent(any(), any());
    }

    @Test
    void testChatWithoutWorkflowIdSkipsAuthorization() {
        AgUiParameters parameters = parameters(Map.<String, Object>of("mode", "BUILD"));

        when(agUiService.runAgent(any(), any())).thenReturn(new SseEmitter());

        controller.chat("converter", parameters);

        verify(agUiService).runAgent(any(), any());
        verify(permissionService, never()).hasWorkspaceScopeForProject(anyLong(), any());
    }

    @Test
    void testChatFailsClosedWhenAuthorizationServicesAbsent() {
        CopilotApiController controllerWithoutServices = new CopilotApiController(
            agUiService, List.of(), Optional.empty(), Optional.empty(), Optional.empty());

        AgUiParameters parameters = parameters(Map.<String, Object>of("workflowId", "wf1", "mode", "ASK"));

        assertThatThrownBy(() -> controllerWithoutServices.chat("workflow_editor", parameters))
            .isInstanceOf(AccessDeniedException.class);

        verify(agUiService, never()).runAgent(any(), any());
    }

    @Test
    void testChatRoutesWorkflowCodeEditorAsk() {
        LocalAgent agent = localAgent("workflow_code_editor_ask");
        CopilotApiController controller = controllerWith(agent);

        when(agUiService.runAgent(eq(agent), any())).thenReturn(new SseEmitter());

        controller.chat("workflow_code_editor", agUiParameters("ASK"));

        verify(agUiService).runAgent(eq(agent), any());
    }

    @Test
    void testChatRoutesWorkflowCodeEditorBuild() {
        LocalAgent agent = localAgent("workflow_code_editor_build");
        CopilotApiController controller = controllerWith(agent);

        when(agUiService.runAgent(eq(agent), any())).thenReturn(new SseEmitter());

        controller.chat("workflow_code_editor", agUiParameters("BUILD"));

        verify(agUiService).runAgent(eq(agent), any());
    }

    @Test
    void testChatRoutesWorkflowEditorEmbeddedAsk() {
        LocalAgent agent = localAgent("workflow_editor_embedded_ask");
        CopilotApiController controller = controllerWith(agent);

        when(agUiService.runAgent(eq(agent), any())).thenReturn(new SseEmitter());

        controller.chat("workflow_editor_embedded", agUiParameters("ASK"));

        verify(agUiService).runAgent(eq(agent), any());
    }

    @Test
    void testChatRoutesWorkflowEditorEmbeddedBuild() {
        LocalAgent agent = localAgent("workflow_editor_embedded_build");
        CopilotApiController controller = controllerWith(agent);

        when(agUiService.runAgent(eq(agent), any())).thenReturn(new SseEmitter());

        controller.chat("workflow_editor_embedded", agUiParameters("BUILD"));

        verify(agUiService).runAgent(eq(agent), any());
    }

    @Test
    void testChatRoutesWorkflowExecutionEmbeddedAsk() {
        LocalAgent agent = localAgent("workflow_execution_embedded_ask");
        CopilotApiController controller = controllerWith(agent);

        when(agUiService.runAgent(eq(agent), any())).thenReturn(new SseEmitter());

        controller.chat("workflow_execution_embedded", agUiParameters("ASK"));

        verify(agUiService).runAgent(eq(agent), any());
    }

    @Test
    void testChatRoutesWorkflowExecutionEmbeddedBuild() {
        LocalAgent agent = localAgent("workflow_execution_embedded_build");
        CopilotApiController controller = controllerWith(agent);

        when(agUiService.runAgent(eq(agent), any())).thenReturn(new SseEmitter());

        controller.chat("workflow_execution_embedded", agUiParameters("BUILD"));

        verify(agUiService).runAgent(eq(agent), any());
    }

    @Test
    void testChatRoutesCodeWorkflowAsk() {
        LocalAgent agent = localAgent("code_workflow_ask");
        CopilotApiController controller = controllerWith(agent);

        when(agUiService.runAgent(eq(agent), any())).thenReturn(new SseEmitter());

        controller.chat("code_workflow", agUiParameters("ASK"));

        verify(agUiService).runAgent(eq(agent), any());
    }

    @Test
    void testChatRoutesCodeWorkflowBuild() {
        LocalAgent agent = localAgent("code_workflow_build");
        CopilotApiController controller = controllerWith(agent);

        when(agUiService.runAgent(eq(agent), any())).thenReturn(new SseEmitter());

        controller.chat("code_workflow", agUiParameters("BUILD"));

        verify(agUiService).runAgent(eq(agent), any());
    }

    @Test
    void testChatRoutesCodeWorkflowEmbeddedAsk() {
        LocalAgent agent = localAgent("code_workflow_embedded_ask");
        CopilotApiController controller = controllerWith(agent);

        when(agUiService.runAgent(eq(agent), any())).thenReturn(new SseEmitter());

        controller.chat("code_workflow_embedded", agUiParameters("ASK"));

        verify(agUiService).runAgent(eq(agent), any());
    }

    @Test
    void testChatRoutesCodeWorkflowEmbeddedBuild() {
        LocalAgent agent = localAgent("code_workflow_embedded_build");
        CopilotApiController controller = controllerWith(agent);

        when(agUiService.runAgent(eq(agent), any())).thenReturn(new SseEmitter());

        controller.chat("code_workflow_embedded", agUiParameters("BUILD"));

        verify(agUiService).runAgent(eq(agent), any());
    }

    @Test
    void testChatRoutesProjectToBuildByDefaultRule() {
        LocalAgent agent = localAgent("project_build");
        CopilotApiController controller = controllerWith(agent);

        when(agUiService.runAgent(eq(agent), any())).thenReturn(new SseEmitter());

        controller.chat("project", agUiParameters("BUILD"));

        verify(agUiService).runAgent(eq(agent), any());
    }

    @Test
    void testChatRoutesConverterToBuildRegardlessOfMode() {
        LocalAgent agent = localAgent("converter_build");
        CopilotApiController controller = controllerWith(agent);

        when(agUiService.runAgent(eq(agent), any())).thenReturn(new SseEmitter());

        controller.chat("converter", agUiParameters("ASK"));

        verify(agUiService).runAgent(eq(agent), any());
    }

    @Test
    void testChatRejectsUnregisteredAgentWithClientError() {
        CopilotApiController controllerWithoutAgents = new CopilotApiController(
            agUiService, List.of(), Optional.empty(), Optional.empty(), Optional.empty());

        assertThatThrownBy(() -> controllerWithoutAgents.chat("workflow_editor", agUiParameters("ASK")))
            .isInstanceOf(ResponseStatusException.class)
            .hasFieldOrPropertyWithValue("statusCode", HttpStatus.NOT_FOUND);

        verify(agUiService, never()).runAgent(any(), any());
    }

    private AgUiParameters agUiParameters(String mode) {
        return parameters(Map.<String, Object>of("mode", mode));
    }

    private LocalAgent localAgent(String agentId) {
        LocalAgent localAgent = mock(LocalAgent.class);

        when(localAgent.getAgentId()).thenReturn(agentId);

        return localAgent;
    }

    private CopilotApiController controllerWith(LocalAgent localAgent) {
        return new CopilotApiController(
            agUiService, List.of(localAgent), Optional.empty(), Optional.empty(), Optional.empty());
    }

    private AgUiParameters parameters(Map<String, Object> stateMap) {
        State state = mock(State.class);

        when(state.getState()).thenReturn(new HashMap<>(stateMap));

        AgUiParameters parameters = mock(AgUiParameters.class);

        when(parameters.getState()).thenReturn(state);

        return parameters;
    }

    private void givenWorkflowProject(String workflowId, long projectId) {
        ProjectWorkflow projectWorkflow = mock(ProjectWorkflow.class);

        when(projectWorkflow.getProjectId()).thenReturn(projectId);
        when(projectWorkflowService.getWorkflowProjectWorkflow(workflowId)).thenReturn(projectWorkflow);
    }
}
