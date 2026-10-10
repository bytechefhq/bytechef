/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.web.rest.facade;

import static org.assertj.core.api.Assertions.assertThat;
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
import com.bytechef.ai.copilot.constant.CopilotConstants;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.ee.embedded.configuration.facade.IntegrationWorkflowFacade;
import com.bytechef.platform.security.util.SecurityUtils;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.tenant.TenantContext;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * @version ee
 * @author Ivica Cardic
 */
class CopilotChatFacadeTest {

    private final AgUiService agUiService = mock(AgUiService.class);
    private final PermissionService permissionService = mock(PermissionService.class);
    private final ProjectWorkflowService projectWorkflowService = mock(ProjectWorkflowService.class);

    private final CopilotChatFacadeImpl facade = new CopilotChatFacadeImpl(
        agUiService, List.of(localAgent("converter_build"), localAgent("workflow_editor_ask")),
        Optional.of(permissionService), Optional.of(projectWorkflowService), Optional.empty(), Optional.empty());

    @Test
    void testChatDeniedWhenUserLacksWorkflowScope() {
        AgUiParameters parameters = parameters(Map.<String, Object>of("workflowId", "wf1", "mode", "ASK"));

        givenWorkflowProject("wf1", 42L);

        when(permissionService.hasWorkspaceScopeForProject(42L, "WORKFLOW_VIEW")).thenReturn(false);

        assertThatThrownBy(() -> facade.chat("workflow_editor", parameters))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessage("Access denied to workflow wf1");

        verify(agUiService, never()).runAgent(any(), any());
    }

    @Test
    void testChatBuildRequiresWorkflowEditScope() {
        CopilotChatFacadeImpl facade = new CopilotChatFacadeImpl(
            agUiService, List.of(localAgent("workflow_editor_build")), Optional.of(permissionService),
            Optional.of(projectWorkflowService), Optional.empty(), Optional.empty());

        AgUiParameters parameters = parameters(Map.<String, Object>of("workflowId", "wf1", "mode", "BUILD"));

        givenWorkflowProject("wf1", 42L);

        when(permissionService.hasWorkspaceScopeForProject(42L, "WORKFLOW_EDIT")).thenReturn(false);

        assertThatThrownBy(() -> facade.chat("workflow_editor", parameters))
            .isInstanceOf(AccessDeniedException.class);

        verify(permissionService).hasWorkspaceScopeForProject(42L, "WORKFLOW_EDIT");
    }

    @Test
    void testChatAllowedWhenUserHasScope() {
        AgUiParameters parameters = parameters(Map.<String, Object>of("workflowId", "wf1", "mode", "ASK"));

        givenWorkflowProject("wf1", 42L);

        when(permissionService.hasWorkspaceScopeForProject(42L, "WORKFLOW_VIEW")).thenReturn(true);
        when(agUiService.runAgent(any(), any())).thenReturn(new SseEmitter());

        facade.chat("workflow_editor", parameters);

        verify(agUiService).runAgent(any(), any());
    }

    @Test
    void testChatWithoutWorkflowIdSkipsAuthorization() {
        AgUiParameters parameters = parameters(Map.<String, Object>of("mode", "BUILD"));

        when(agUiService.runAgent(any(), any())).thenReturn(new SseEmitter());

        facade.chat("converter", parameters);

        verify(agUiService).runAgent(any(), any());
        verify(permissionService, never()).hasWorkspaceScopeForProject(anyLong(), any());
    }

    @Test
    void testChatFailsClosedWhenAuthorizationServicesAbsent() {
        CopilotChatFacadeImpl facadeWithoutServices = new CopilotChatFacadeImpl(
            agUiService, List.of(localAgent("workflow_editor_ask")), Optional.empty(), Optional.empty(),
            Optional.empty(), Optional.empty());

        AgUiParameters parameters = parameters(Map.<String, Object>of("workflowId", "wf1", "mode", "ASK"));

        assertThatThrownBy(() -> facadeWithoutServices.chat("workflow_editor", parameters))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessage("Workflow authorization is not available");

        verify(agUiService, never()).runAgent(any(), any());
    }

    @Test
    void testChatRoutesWorkflowCodeEditorAsk() {
        LocalAgent agent = localAgent("workflow_code_editor_ask");
        CopilotChatFacadeImpl facade = facadeWith(agent);

        when(agUiService.runAgent(eq(agent), any())).thenReturn(new SseEmitter());

        facade.chat("workflow_code_editor", agUiParameters("ASK"));

        verify(agUiService).runAgent(eq(agent), any());
    }

    @Test
    void testChatRoutesWorkflowCodeEditorBuild() {
        LocalAgent agent = localAgent("workflow_code_editor_build");
        CopilotChatFacadeImpl facade = facadeWith(agent);

        when(agUiService.runAgent(eq(agent), any())).thenReturn(new SseEmitter());

        facade.chat("workflow_code_editor", agUiParameters("BUILD"));

        verify(agUiService).runAgent(eq(agent), any());
    }

    @Test
    void testChatRoutesWorkflowEditorEmbeddedAsk() {
        LocalAgent agent = localAgent("workflow_editor_embedded_ask");
        CopilotChatFacadeImpl facade = facadeWith(agent);

        when(agUiService.runAgent(eq(agent), any())).thenReturn(new SseEmitter());

        facade.chat("workflow_editor_embedded", agUiParameters("ASK"));

        verify(agUiService).runAgent(eq(agent), any());
    }

    @Test
    void testChatRoutesWorkflowEditorEmbeddedBuild() {
        LocalAgent agent = localAgent("workflow_editor_embedded_build");
        CopilotChatFacadeImpl facade = facadeWith(agent);

        when(agUiService.runAgent(eq(agent), any())).thenReturn(new SseEmitter());

        facade.chat("workflow_editor_embedded", agUiParameters("BUILD"));

        verify(agUiService).runAgent(eq(agent), any());
    }

    @Test
    void testChatRoutesWorkflowExecutionEmbeddedAsk() {
        LocalAgent agent = localAgent("workflow_execution_embedded_ask");
        CopilotChatFacadeImpl facade = facadeWith(agent);

        when(agUiService.runAgent(eq(agent), any())).thenReturn(new SseEmitter());

        facade.chat("workflow_execution_embedded", agUiParameters("ASK"));

        verify(agUiService).runAgent(eq(agent), any());
    }

    @Test
    void testChatRoutesWorkflowExecutionEmbeddedBuild() {
        LocalAgent agent = localAgent("workflow_execution_embedded_build");
        CopilotChatFacadeImpl facade = facadeWith(agent);

        when(agUiService.runAgent(eq(agent), any())).thenReturn(new SseEmitter());

        facade.chat("workflow_execution_embedded", agUiParameters("BUILD"));

        verify(agUiService).runAgent(eq(agent), any());
    }

    @Test
    void testChatRoutesCodeWorkflowAsk() {
        LocalAgent agent = localAgent("code_workflow_ask");
        CopilotChatFacadeImpl facade = facadeWith(agent);

        when(agUiService.runAgent(eq(agent), any())).thenReturn(new SseEmitter());

        facade.chat("code_workflow", agUiParameters("ASK"));

        verify(agUiService).runAgent(eq(agent), any());
    }

    @Test
    void testChatRoutesCodeWorkflowBuild() {
        LocalAgent agent = localAgent("code_workflow_build");
        CopilotChatFacadeImpl facade = facadeWith(agent);

        when(agUiService.runAgent(eq(agent), any())).thenReturn(new SseEmitter());

        facade.chat("code_workflow", agUiParameters("BUILD"));

        verify(agUiService).runAgent(eq(agent), any());
    }

    @Test
    void testChatRoutesCodeWorkflowEmbeddedAsk() {
        LocalAgent agent = localAgent("code_workflow_embedded_ask");
        CopilotChatFacadeImpl facade = facadeWith(agent);

        when(agUiService.runAgent(eq(agent), any())).thenReturn(new SseEmitter());

        facade.chat("code_workflow_embedded", agUiParameters("ASK"));

        verify(agUiService).runAgent(eq(agent), any());
    }

    @Test
    void testChatRoutesCodeWorkflowEmbeddedBuild() {
        LocalAgent agent = localAgent("code_workflow_embedded_build");
        CopilotChatFacadeImpl facade = facadeWith(agent);

        when(agUiService.runAgent(eq(agent), any())).thenReturn(new SseEmitter());

        facade.chat("code_workflow_embedded", agUiParameters("BUILD"));

        verify(agUiService).runAgent(eq(agent), any());
    }

    @Test
    void testChatRoutesProjectToBuildByDefaultRule() {
        LocalAgent agent = localAgent("project_build");
        CopilotChatFacadeImpl facade = facadeWith(agent);

        when(agUiService.runAgent(eq(agent), any())).thenReturn(new SseEmitter());

        facade.chat("project", agUiParameters("BUILD"));

        verify(agUiService).runAgent(eq(agent), any());
    }

    @Test
    void testChatRoutesConverterToBuildRegardlessOfMode() {
        LocalAgent agent = localAgent("converter_build");
        CopilotChatFacadeImpl facade = facadeWith(agent);

        when(agUiService.runAgent(eq(agent), any())).thenReturn(new SseEmitter());

        facade.chat("converter", agUiParameters("ASK"));

        verify(agUiService).runAgent(eq(agent), any());
    }

    @Test
    void testChatRoutesMcpServerToBuildRegardlessOfMode() {
        LocalAgent agent = localAgent("mcp_server_build");
        CopilotChatFacadeImpl facade = facadeWith(agent);

        when(agUiService.runAgent(eq(agent), any())).thenReturn(new SseEmitter());

        facade.chat("mcp_server", agUiParameters("ASK"));

        verify(agUiService).runAgent(eq(agent), any());
    }

    @Test
    void testChatSeedsTenantIdAndAuthenticatedUserIdIntoState() {
        LocalAgent agent = localAgent("workflow_editor_ask");

        UserService userService = mock(UserService.class);
        User user = mock(User.class);

        when(user.getId()).thenReturn(7L);
        when(userService.fetchUserByLogin("someone@localhost.com")).thenReturn(Optional.of(user));

        CopilotChatFacadeImpl facade = new CopilotChatFacadeImpl(
            agUiService, List.of(agent), Optional.empty(), Optional.empty(), Optional.of(userService),
            Optional.empty());

        when(agUiService.runAgent(eq(agent), any())).thenReturn(new SseEmitter());

        SecurityUtils.runAs(
            "someone@localhost.com", List.of(), () -> facade.chat("workflow_editor", agUiParameters("ASK")));

        ArgumentCaptor<AgUiParameters> parametersArgumentCaptor = ArgumentCaptor.forClass(AgUiParameters.class);

        verify(agUiService).runAgent(eq(agent), parametersArgumentCaptor.capture());

        AgUiParameters capturedParameters = parametersArgumentCaptor.getValue();

        State capturedState = capturedParameters.getState();

        assertThat(capturedState.getState())
            .containsEntry(CopilotConstants.STATE_TENANT_ID, TenantContext.getCurrentTenantId())
            .containsEntry(CopilotConstants.STATE_AUTHENTICATED_USER_ID, 7L);
    }

    @Test
    void testChatDeniedWhenUserIsNotWorkspaceViewer() {
        AgUiParameters parameters = parameters(
            Map.<String, Object>of(CopilotConstants.STATE_WORKSPACE_ID, "5", "mode", "ASK"));

        when(permissionService.hasWorkspaceRole(5L, "VIEWER")).thenReturn(false);

        assertThatThrownBy(() -> facade.chat("workflow_editor", parameters))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessage("Access denied to workspace 5");

        verify(agUiService, never()).runAgent(any(), any());
    }

    @Test
    void testChatAlwaysBuildAgentRequiresWorkspaceEditorRoleInAskMode() {
        LocalAgent agent = localAgent("mcp_server_build");
        CopilotChatFacadeImpl facade = new CopilotChatFacadeImpl(
            agUiService, List.of(agent), Optional.of(permissionService), Optional.of(projectWorkflowService),
            Optional.empty(), Optional.empty());

        AgUiParameters parameters = parameters(
            Map.<String, Object>of(CopilotConstants.STATE_WORKSPACE_ID, "5", "mode", "ASK"));

        when(permissionService.hasWorkspaceRole(5L, "VIEWER")).thenReturn(true);
        when(permissionService.hasWorkspaceRole(5L, "EDITOR")).thenReturn(false);

        assertThatThrownBy(() -> facade.chat("mcp_server", parameters))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessage("Access denied to workspace 5");

        verify(agUiService, never()).runAgent(any(), any());
    }

    @Test
    void testChatBuildRequiresWorkspaceEditorRole() {
        LocalAgent agent = localAgent("data_table_build");
        CopilotChatFacadeImpl facade = new CopilotChatFacadeImpl(
            agUiService, List.of(agent), Optional.of(permissionService), Optional.of(projectWorkflowService),
            Optional.empty(), Optional.empty());

        AgUiParameters parameters = parameters(
            Map.<String, Object>of(CopilotConstants.STATE_WORKSPACE_ID, "5", "mode", "BUILD"));

        when(permissionService.hasWorkspaceRole(5L, "VIEWER")).thenReturn(true);
        when(permissionService.hasWorkspaceRole(5L, "EDITOR")).thenReturn(false);

        assertThatThrownBy(() -> facade.chat("data_table", parameters))
            .isInstanceOf(AccessDeniedException.class);

        verify(permissionService).hasWorkspaceRole(5L, "EDITOR");
        verify(agUiService, never()).runAgent(any(), any());
    }

    @Test
    void testChatAllowedWhenUserIsWorkspaceMember() {
        LocalAgent agent = localAgent("data_table_ask");
        CopilotChatFacadeImpl facade = new CopilotChatFacadeImpl(
            agUiService, List.of(agent), Optional.of(permissionService), Optional.of(projectWorkflowService),
            Optional.empty(), Optional.empty());

        AgUiParameters parameters = parameters(
            Map.<String, Object>of(CopilotConstants.STATE_WORKSPACE_ID, 5, "mode", "ASK"));

        when(permissionService.hasWorkspaceRole(5L, "VIEWER")).thenReturn(true);
        when(agUiService.runAgent(eq(agent), any())).thenReturn(new SseEmitter());

        facade.chat("data_table", parameters);

        verify(agUiService).runAgent(eq(agent), any());
    }

    @Test
    void testChatRejectsMalformedWorkspaceId() {
        AgUiParameters parameters = parameters(
            Map.<String, Object>of(CopilotConstants.STATE_WORKSPACE_ID, "abc", "mode", "ASK"));

        assertThatThrownBy(() -> facade.chat("workflow_editor", parameters))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessage("Invalid workspace id");

        verify(agUiService, never()).runAgent(any(), any());
    }

    @Test
    void testChatWorkspaceCheckFailsClosedWhenAuthorizationServicesAbsent() {
        CopilotChatFacadeImpl facadeWithoutServices = new CopilotChatFacadeImpl(
            agUiService, List.of(localAgent("workflow_editor_ask")), Optional.empty(), Optional.empty(),
            Optional.empty(), Optional.empty());

        AgUiParameters parameters = parameters(
            Map.<String, Object>of(CopilotConstants.STATE_WORKSPACE_ID, "5", "mode", "ASK"));

        assertThatThrownBy(() -> facadeWithoutServices.chat("workflow_editor", parameters))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessage("Workspace authorization is not available");

        verify(agUiService, never()).runAgent(any(), any());
    }

    @Test
    void testChatDropsClientSuppliedAuthenticatedUserId() {
        LocalAgent agent = localAgent("workflow_editor_ask");
        CopilotChatFacadeImpl facade = facadeWith(agent);

        AgUiParameters parameters = parameters(
            Map.<String, Object>of(CopilotConstants.STATE_AUTHENTICATED_USER_ID, 99L, "mode", "ASK"));

        when(agUiService.runAgent(eq(agent), any())).thenReturn(new SseEmitter());

        facade.chat("workflow_editor", parameters);

        State state = parameters.getState();

        assertThat(state.getState()).doesNotContainKey(CopilotConstants.STATE_AUTHENTICATED_USER_ID);
    }

    @Test
    void testChatRejectsUnknownModeWithClientError() {
        AgUiParameters parameters = parameters(Map.<String, Object>of("mode", "EDIT"));

        assertThatThrownBy(() -> facade.chat("workflow_editor", parameters))
            .isInstanceOf(ResponseStatusException.class)
            .hasFieldOrPropertyWithValue("statusCode", HttpStatus.BAD_REQUEST);

        verify(agUiService, never()).runAgent(any(), any());
    }

    @Test
    void testChatRejectsMissingModeWithClientError() {
        AgUiParameters parameters = parameters(Map.<String, Object>of());

        assertThatThrownBy(() -> facade.chat("workflow_editor", parameters))
            .isInstanceOf(ResponseStatusException.class)
            .hasFieldOrPropertyWithValue("statusCode", HttpStatus.BAD_REQUEST);

        verify(agUiService, never()).runAgent(any(), any());
    }

    @Test
    void testChatRejectsUnregisteredAgentWithClientError() {
        CopilotChatFacadeImpl facadeWithoutAgents = new CopilotChatFacadeImpl(
            agUiService, List.of(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());

        assertThatThrownBy(() -> facadeWithoutAgents.chat("workflow_editor", agUiParameters("ASK")))
            .isInstanceOf(ResponseStatusException.class)
            .hasFieldOrPropertyWithValue("statusCode", HttpStatus.NOT_FOUND);

        verify(agUiService, never()).runAgent(any(), any());
    }

    @Test
    void testChatEmbeddedWorkflowEditorAuthorizesThroughTheIntegrationWorkflowFacade() {
        LocalAgent agent = localAgent("workflow_editor_embedded_build");
        IntegrationWorkflowFacade integrationWorkflowFacade = mock(IntegrationWorkflowFacade.class);

        CopilotChatFacadeImpl facade = new CopilotChatFacadeImpl(
            agUiService, List.of(agent), Optional.of(permissionService), Optional.of(projectWorkflowService),
            Optional.empty(), Optional.of(integrationWorkflowFacade));

        when(agUiService.runAgent(eq(agent), any())).thenReturn(new SseEmitter());

        facade.chat(
            "workflow_editor_embedded", parameters(Map.<String, Object>of("workflowId", "wf1", "mode", "BUILD")));

        verify(integrationWorkflowFacade).getIntegrationWorkflow("wf1");
        verify(projectWorkflowService, never()).getWorkflowProjectWorkflow(any());
        verify(agUiService).runAgent(eq(agent), any());
    }

    @Test
    void testChatEmbeddedWorkflowEditorDeniedWhenIntegrationWorkflowFacadeDenies() {
        LocalAgent agent = localAgent("workflow_editor_embedded_ask");
        IntegrationWorkflowFacade integrationWorkflowFacade = mock(IntegrationWorkflowFacade.class);

        CopilotChatFacadeImpl facade = new CopilotChatFacadeImpl(
            agUiService, List.of(agent), Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.of(integrationWorkflowFacade));

        when(integrationWorkflowFacade.getIntegrationWorkflow("wf1")).thenThrow(new AccessDeniedException("denied"));

        AgUiParameters parameters = parameters(Map.<String, Object>of("workflowId", "wf1", "mode", "ASK"));

        assertThatThrownBy(() -> facade.chat("workflow_editor_embedded", parameters))
            .isInstanceOf(AccessDeniedException.class);

        verify(agUiService, never()).runAgent(any(), any());
    }

    @Test
    void testChatEmbeddedWorkflowEditorDeniedForANonIntegrationWorkflow() {
        LocalAgent agent = localAgent("workflow_editor_embedded_ask");
        IntegrationWorkflowFacade integrationWorkflowFacade = mock(IntegrationWorkflowFacade.class);

        CopilotChatFacadeImpl facade = new CopilotChatFacadeImpl(
            agUiService, List.of(agent), Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.of(integrationWorkflowFacade));

        when(integrationWorkflowFacade.getIntegrationWorkflow("wf1"))
            .thenThrow(new IllegalArgumentException("Workflow not found for id: wf1"));

        AgUiParameters parameters = parameters(Map.<String, Object>of("workflowId", "wf1", "mode", "ASK"));

        assertThatThrownBy(() -> facade.chat("workflow_editor_embedded", parameters))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessage("Access denied to workflow wf1");

        verify(agUiService, never()).runAgent(any(), any());
    }

    @Test
    void testChatEmbeddedWorkflowEditorFailsClosedWhenIntegrationWorkflowFacadeAbsent() {
        CopilotChatFacadeImpl facadeWithoutServices = new CopilotChatFacadeImpl(
            agUiService, List.of(localAgent("workflow_editor_embedded_ask")), Optional.empty(), Optional.empty(),
            Optional.empty(), Optional.empty());

        AgUiParameters parameters = parameters(Map.<String, Object>of("workflowId", "wf1", "mode", "ASK"));

        assertThatThrownBy(() -> facadeWithoutServices.chat("workflow_editor_embedded", parameters))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessage("Workflow authorization is not available");

        verify(agUiService, never()).runAgent(any(), any());
    }

    @Test
    void testChatDeniedWhenWorkflowIdIsUnknown() {
        IllegalArgumentException notFoundException = new IllegalArgumentException("ProjectWorkflow not found");

        when(projectWorkflowService.getWorkflowProjectWorkflow("missing")).thenThrow(notFoundException);

        AgUiParameters parameters = parameters(Map.<String, Object>of("workflowId", "missing", "mode", "ASK"));

        assertThatThrownBy(() -> facade.chat("workflow_editor", parameters))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessage("Access denied to workflow missing")
            .hasCause(notFoundException);

        verify(permissionService, never()).hasWorkspaceScopeForProject(anyLong(), any());
        verify(agUiService, never()).runAgent(any(), any());
    }

    @Test
    void testChatRejectsUnknownSourceWithNotFound() {
        assertThatThrownBy(() -> facade.chat("no_such_source", agUiParameters("ASK")))
            .isInstanceOf(ResponseStatusException.class)
            .hasFieldOrPropertyWithValue("statusCode", HttpStatus.NOT_FOUND);

        verify(agUiService, never()).runAgent(any(), any());
    }

    @Test
    void testChatMalformedWorkspaceIdKeepsTheParseFailureAsCause() {
        AgUiParameters parameters = parameters(
            Map.<String, Object>of(CopilotConstants.STATE_WORKSPACE_ID, "abc", "mode", "ASK"));

        assertThatThrownBy(() -> facade.chat("workflow_editor", parameters))
            .isInstanceOf(AccessDeniedException.class)
            .hasCauseInstanceOf(NumberFormatException.class);
    }

    @Test
    void testChatLeavesAuthenticatedUserIdUnsetWhenTheLoginHasNoUser() {
        LocalAgent agent = localAgent("workflow_editor_ask");
        UserService userService = mock(UserService.class);

        when(userService.fetchUserByLogin("ghost@localhost.com")).thenReturn(Optional.empty());

        CopilotChatFacadeImpl facade = new CopilotChatFacadeImpl(
            agUiService, List.of(agent), Optional.empty(), Optional.empty(), Optional.of(userService),
            Optional.empty());

        AgUiParameters parameters = agUiParameters("ASK");

        when(agUiService.runAgent(eq(agent), any())).thenReturn(new SseEmitter());

        SecurityUtils.runAs("ghost@localhost.com", List.of(), () -> facade.chat("workflow_editor", parameters));

        State state = parameters.getState();

        assertThat(state.getState()).doesNotContainKey(CopilotConstants.STATE_AUTHENTICATED_USER_ID);
        verify(agUiService).runAgent(eq(agent), any());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "workflow_editor_embedded", "code_workflow_embedded", "workflow_execution_embedded"
    })
    void testChatEmbeddedSourcesAuthorizeThroughTheIntegrationWorkflowFacade(String source) {
        LocalAgent agent = localAgent(source + "_ask");
        IntegrationWorkflowFacade integrationWorkflowFacade = mock(IntegrationWorkflowFacade.class);

        CopilotChatFacadeImpl facade = new CopilotChatFacadeImpl(
            agUiService, List.of(agent), Optional.of(permissionService), Optional.of(projectWorkflowService),
            Optional.empty(), Optional.of(integrationWorkflowFacade));

        when(agUiService.runAgent(eq(agent), any())).thenReturn(new SseEmitter());

        facade.chat(source, parameters(Map.<String, Object>of("workflowId", "wf1", "mode", "ASK")));

        verify(integrationWorkflowFacade).getIntegrationWorkflow("wf1");
        verify(projectWorkflowService, never()).getWorkflowProjectWorkflow(any());
        verify(agUiService).runAgent(eq(agent), any());
    }

    @Test
    void testChatEmbeddedWorkflowEditorWithWorkspaceIdAlsoChecksTheWorkspaceRoleForTheMode() {
        LocalAgent agent = localAgent("workflow_editor_embedded_build");
        IntegrationWorkflowFacade integrationWorkflowFacade = mock(IntegrationWorkflowFacade.class);

        CopilotChatFacadeImpl facade = new CopilotChatFacadeImpl(
            agUiService, List.of(agent), Optional.of(permissionService), Optional.of(projectWorkflowService),
            Optional.empty(), Optional.of(integrationWorkflowFacade));

        AgUiParameters parameters = parameters(
            Map.<String, Object>of("workflowId", "wf1", CopilotConstants.STATE_WORKSPACE_ID, "5", "mode", "BUILD"));

        when(permissionService.hasWorkspaceRole(5L, "EDITOR")).thenReturn(false);

        assertThatThrownBy(() -> facade.chat("workflow_editor_embedded", parameters))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessage("Access denied to workspace 5");

        verify(integrationWorkflowFacade).getIntegrationWorkflow("wf1");
        verify(permissionService).hasWorkspaceRole(5L, "EDITOR");
        verify(projectWorkflowService, never()).getWorkflowProjectWorkflow(any());
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

    private CopilotChatFacadeImpl facadeWith(LocalAgent localAgent) {
        return new CopilotChatFacadeImpl(
            agUiService, List.of(localAgent), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
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
