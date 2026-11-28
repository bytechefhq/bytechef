/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.ai.copilot.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agui.core.state.State;
import com.agui.server.LocalAgent;
import com.agui.server.spring.AgUiParameters;
import com.agui.server.spring.AgUiService;
import com.bytechef.ai.copilot.constant.CopilotConstants;
import com.bytechef.ai.copilot.security.CopilotEmbeddedAccessChecker;
import com.bytechef.ai.copilot.security.CopilotEmbeddedAccessChecker.AutomationProjectScope;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.platform.security.util.SecurityUtils;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.tenant.TenantContext;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * @author Ivica Cardic
 */
class CopilotChatFacadeTest {

    private final AgUiService agUiService = mock(AgUiService.class);
    private final PermissionService permissionService = mock(PermissionService.class);
    private final ProjectWorkflowService projectWorkflowService = mock(ProjectWorkflowService.class);

    private final CopilotChatFacadeImpl facade = new CopilotChatFacadeImpl(
        agUiService, List.of(localAgent("converter_build"), localAgent("workflow_editor_ask")),
        Optional.of(permissionService), Optional.of(projectWorkflowService), Optional.empty(), Optional.empty());

    @BeforeEach
    void beforeEach() {
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();

        securityContext.setAuthentication(
            new UsernamePasswordAuthenticationToken("someone@localhost.com", "", List.of()));

        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testChatReplacesAClientSuppliedConversationScope() {
        LocalAgent agent = localAgent("workflow_editor_ask");
        CopilotChatFacadeImpl facade = facadeWith(agent);

        AgUiParameters parameters = parameters(
            Map.<String, Object>of(
                CopilotConstants.STATE_CONVERSATION_SCOPE, "app:tenant:victim@localhost.com",
                CopilotConstants.STATE_SCOPED_PROJECT_ID, 9L, "mode", "ASK"));

        when(agUiService.runAgent(eq(agent), any())).thenReturn(new SseEmitter());

        facade.chat("workflow_editor", parameters);

        State state = parameters.getState();

        assertThat(state.getState())
            .containsEntry(
                CopilotConstants.STATE_CONVERSATION_SCOPE,
                String.join(":", "app", TenantContext.getCurrentTenantId(), "login", "someone%40localhost.com"))
            .doesNotContainKey(CopilotConstants.STATE_SCOPED_PROJECT_ID);
    }

    @Test
    void testChatCannotForgeAnEmbeddedConversationScope() {
        LocalAgent agent = localAgent("workflow_editor_embedded_ask");
        CopilotChatFacadeImpl facade = facadeWith(agent);

        AgUiParameters parameters = parameters(
            Map.<String, Object>of(
                CopilotConstants.STATE_CONVERSATION_SCOPE, "embedded:tenant:external-user:workflow-uuid", "mode",
                "ASK"));

        when(agUiService.runAgent(eq(agent), any())).thenReturn(new SseEmitter());

        facade.chat("workflow_editor_embedded", parameters);

        State state = parameters.getState();

        assertThat(state.getState())
            .containsEntry(
                CopilotConstants.STATE_CONVERSATION_SCOPE,
                String.join(":", "app", TenantContext.getCurrentTenantId(), "login", "someone%40localhost.com"));
    }

    @Test
    void testChatScopesTheSameThreadToEachSignedInUser() {
        LocalAgent agent = localAgent("workflow_editor_ask");
        CopilotChatFacadeImpl facade = facadeWith(agent);

        when(agUiService.runAgent(eq(agent), any())).thenReturn(new SseEmitter());

        AgUiParameters firstUserParameters = agUiParameters("ASK");
        AgUiParameters secondUserParameters = agUiParameters("ASK");

        SecurityUtils.runAs(
            "first@localhost.com", List.of(), () -> facade.chat("workflow_editor", firstUserParameters));
        SecurityUtils.runAs(
            "second@localhost.com", List.of(), () -> facade.chat("workflow_editor", secondUserParameters));

        State firstUserState = firstUserParameters.getState();
        State secondUserState = secondUserParameters.getState();

        assertThat(firstUserState.getState()
            .get(CopilotConstants.STATE_CONVERSATION_SCOPE))
                .isNotNull()
                .isNotEqualTo(secondUserState.getState()
                    .get(CopilotConstants.STATE_CONVERSATION_SCOPE));
    }

    @Test
    void testChatKeepsAUserIdAndAnEqualLoginInDifferentScopes() {
        LocalAgent agent = localAgent("workflow_editor_ask");

        UserService userService = mock(UserService.class);
        User user = mock(User.class);

        when(user.getId()).thenReturn(7L);
        when(userService.fetchUserByLogin("someone@localhost.com")).thenReturn(Optional.of(user));

        CopilotChatFacadeImpl facadeWithUsers = new CopilotChatFacadeImpl(
            agUiService, List.of(agent), Optional.empty(), Optional.empty(), Optional.of(userService),
            Optional.empty());

        when(agUiService.runAgent(eq(agent), any())).thenReturn(new SseEmitter());

        AgUiParameters userIdParameters = agUiParameters("ASK");
        AgUiParameters loginParameters = agUiParameters("ASK");

        SecurityUtils.runAs(
            "someone@localhost.com", List.of(), () -> facadeWithUsers.chat("workflow_editor", userIdParameters));
        SecurityUtils.runAs("7", List.of(), () -> facadeWith(agent).chat("workflow_editor", loginParameters));

        assertThat(conversationScope(userIdParameters)).isNotEqualTo(conversationScope(loginParameters));
    }

    @Test
    void testChatEncodesAColonInTheLoginScope() {
        LocalAgent agent = localAgent("workflow_editor_ask");
        CopilotChatFacadeImpl facade = facadeWith(agent);

        when(agUiService.runAgent(eq(agent), any())).thenReturn(new SseEmitter());

        AgUiParameters colonLoginParameters = agUiParameters("ASK");
        AgUiParameters plainLoginParameters = agUiParameters("ASK");

        SecurityUtils.runAs("7:x", List.of(), () -> facade.chat("workflow_editor", colonLoginParameters));
        SecurityUtils.runAs("7", List.of(), () -> facade.chat("workflow_editor", plainLoginParameters));

        String colonLoginScope = conversationScope(colonLoginParameters);

        assertThat(colonLoginScope.split(":")).hasSize(4);
        assertThat(colonLoginScope + ":" + "y").isNotEqualTo(conversationScope(plainLoginParameters) + ":" + "x:y");
    }

    @Test
    void testChatRejectsAConnectedUser() {
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
            "someone@localhost.com", null, List.of());

        securityContext.setAuthentication(authentication);

        SecurityContextHolder.setContext(securityContext);

        UserService userService = mock(UserService.class);
        User user = mock(User.class);

        when(user.getId()).thenReturn(7L);
        when(userService.fetchUserByLogin("someone@localhost.com")).thenReturn(Optional.of(user));

        LocalAgent agent = localAgent("workflow_editor_ask");

        CopilotEmbeddedAccessChecker copilotEmbeddedAccessChecker = mock(CopilotEmbeddedAccessChecker.class);

        when(copilotEmbeddedAccessChecker.isConnectedUser(authentication)).thenReturn(true);

        CopilotChatFacadeImpl facadeWithUsers = new CopilotChatFacadeImpl(
            agUiService, List.of(agent), Optional.empty(), Optional.empty(), Optional.of(userService),
            Optional.of(copilotEmbeddedAccessChecker));

        AgUiParameters parameters = agUiParameters("ASK");

        assertThatThrownBy(() -> facadeWithUsers.chat("workflow_editor", parameters))
            .isInstanceOf(AccessDeniedException.class);

        verify(agUiService, never()).runAgent(any(), any());
        verify(userService, never()).fetchUserByLogin(any());
    }

    @Test
    void testChatRejectsARunWithoutAnAuthenticatedUser() {
        SecurityContextHolder.clearContext();

        LocalAgent agent = localAgent("workflow_editor_ask");
        CopilotChatFacadeImpl facade = facadeWith(agent);

        AgUiParameters parameters = agUiParameters("ASK");

        assertThatThrownBy(() -> facade.chat("workflow_editor", parameters))
            .isInstanceOf(AccessDeniedException.class);

        verify(agUiService, never()).runAgent(any(), any());
    }

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
            .containsEntry(CopilotConstants.STATE_AUTHENTICATED_USER_ID, 7L)
            .containsEntry(
                CopilotConstants.STATE_CONVERSATION_SCOPE,
                String.join(":", "app", TenantContext.getCurrentTenantId(), "user", "7"));
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
    void testChatEmbeddedWorkflowEditorAuthorizesThroughTheEmbeddedAccessChecker() {
        LocalAgent agent = localAgent("workflow_editor_embedded_build");
        CopilotEmbeddedAccessChecker copilotEmbeddedAccessChecker = mock(CopilotEmbeddedAccessChecker.class);

        CopilotChatFacadeImpl facade = new CopilotChatFacadeImpl(
            agUiService, List.of(agent), Optional.of(permissionService), Optional.of(projectWorkflowService),
            Optional.empty(), Optional.of(copilotEmbeddedAccessChecker));

        when(agUiService.runAgent(eq(agent), any())).thenReturn(new SseEmitter());

        facade.chat(
            "workflow_editor_embedded", parameters(Map.<String, Object>of("workflowId", "wf1", "mode", "BUILD")));

        verify(copilotEmbeddedAccessChecker).checkIntegrationWorkflowAccess("wf1");
        verify(projectWorkflowService, never()).getWorkflowProjectWorkflow(any());
        verify(agUiService).runAgent(eq(agent), any());
    }

    @Test
    void testChatEmbeddedWorkflowEditorDeniedWhenTheEmbeddedAccessCheckerDenies() {
        LocalAgent agent = localAgent("workflow_editor_embedded_ask");
        CopilotEmbeddedAccessChecker copilotEmbeddedAccessChecker = mock(CopilotEmbeddedAccessChecker.class);

        CopilotChatFacadeImpl facade = new CopilotChatFacadeImpl(
            agUiService, List.of(agent), Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.of(copilotEmbeddedAccessChecker));

        doThrow(new AccessDeniedException("denied"))
            .when(copilotEmbeddedAccessChecker)
            .checkIntegrationWorkflowAccess("wf1");

        AgUiParameters parameters = parameters(Map.<String, Object>of("workflowId", "wf1", "mode", "ASK"));

        assertThatThrownBy(() -> facade.chat("workflow_editor_embedded", parameters))
            .isInstanceOf(AccessDeniedException.class);

        verify(agUiService, never()).runAgent(any(), any());
    }

    @Test
    void testChatEmbeddedWorkflowEditorDeniedForANonIntegrationWorkflow() {
        LocalAgent agent = localAgent("workflow_editor_embedded_ask");
        CopilotEmbeddedAccessChecker copilotEmbeddedAccessChecker = mock(CopilotEmbeddedAccessChecker.class);

        CopilotChatFacadeImpl facade = new CopilotChatFacadeImpl(
            agUiService, List.of(agent), Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.of(copilotEmbeddedAccessChecker));

        doThrow(new IllegalArgumentException("Workflow not found for id: wf1"))
            .when(copilotEmbeddedAccessChecker)
            .checkIntegrationWorkflowAccess("wf1");

        AgUiParameters parameters = parameters(Map.<String, Object>of("workflowId", "wf1", "mode", "ASK"));

        assertThatThrownBy(() -> facade.chat("workflow_editor_embedded", parameters))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessage("Access denied to workflow wf1");

        verify(agUiService, never()).runAgent(any(), any());
    }

    @Test
    void testChatEmbeddedWorkflowEditorFailsClosedWithoutTheEmbeddedAccessChecker() {
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
    void testChatEmbeddedSourcesAuthorizeThroughTheEmbeddedAccessChecker(String source) {
        LocalAgent agent = localAgent(source + "_ask");
        CopilotEmbeddedAccessChecker copilotEmbeddedAccessChecker = mock(CopilotEmbeddedAccessChecker.class);

        CopilotChatFacadeImpl facade = new CopilotChatFacadeImpl(
            agUiService, List.of(agent), Optional.of(permissionService), Optional.of(projectWorkflowService),
            Optional.empty(), Optional.of(copilotEmbeddedAccessChecker));

        when(agUiService.runAgent(eq(agent), any())).thenReturn(new SseEmitter());

        facade.chat(source, parameters(Map.<String, Object>of("workflowId", "wf1", "mode", "ASK")));

        verify(copilotEmbeddedAccessChecker).checkIntegrationWorkflowAccess("wf1");
        verify(projectWorkflowService, never()).getWorkflowProjectWorkflow(any());
        verify(agUiService).runAgent(eq(agent), any());
    }

    @Test
    void testChatEmbeddedWorkflowEditorWithWorkspaceIdAlsoChecksTheWorkspaceRoleForTheMode() {
        LocalAgent agent = localAgent("workflow_editor_embedded_build");
        CopilotEmbeddedAccessChecker copilotEmbeddedAccessChecker = mock(CopilotEmbeddedAccessChecker.class);

        CopilotChatFacadeImpl facade = new CopilotChatFacadeImpl(
            agUiService, List.of(agent), Optional.of(permissionService), Optional.of(projectWorkflowService),
            Optional.empty(), Optional.of(copilotEmbeddedAccessChecker));

        AgUiParameters parameters = parameters(
            Map.<String, Object>of("workflowId", "wf1", CopilotConstants.STATE_WORKSPACE_ID, "5", "mode", "BUILD"));

        when(permissionService.hasWorkspaceRole(5L, "EDITOR")).thenReturn(false);

        assertThatThrownBy(() -> facade.chat("workflow_editor_embedded", parameters))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessage("Access denied to workspace 5");

        verify(copilotEmbeddedAccessChecker).checkIntegrationWorkflowAccess("wf1");
        verify(permissionService).hasWorkspaceRole(5L, "EDITOR");
        verify(projectWorkflowService, never()).getWorkflowProjectWorkflow(any());
        verify(agUiService, never()).runAgent(any(), any());
    }

    @Test
    void testChatEmbeddedAutomationWorkflowEditorScopesTheRunToTheWorkflowProject() {
        LocalAgent agent = localAgent("workflow_editor_build");
        CopilotEmbeddedAccessChecker copilotEmbeddedAccessChecker = mock(CopilotEmbeddedAccessChecker.class);

        CopilotChatFacadeImpl facade = new CopilotChatFacadeImpl(
            agUiService, List.of(agent), Optional.of(permissionService), Optional.of(projectWorkflowService),
            Optional.empty(), Optional.of(copilotEmbeddedAccessChecker));

        AgUiParameters parameters = parameters(
            Map.<String, Object>of(
                "workflowId", "wf1", CopilotConstants.STATE_WORKSPACE_ID, "5",
                CopilotConstants.STATE_SCOPED_PROJECT_ID, 99L, "mode", "BUILD"));

        when(copilotEmbeddedAccessChecker.checkAutomationProjectWorkflowAccess("wf1"))
            .thenReturn(new AutomationProjectScope(7L, 1L));
        when(permissionService.hasWorkspaceRole(1L, "EDITOR")).thenReturn(true);
        when(agUiService.runAgent(eq(agent), any())).thenReturn(new SseEmitter());

        facade.chat("workflow_editor_embedded_automation", parameters);

        State state = parameters.getState();

        assertThat(state.getState())
            .containsEntry(CopilotConstants.STATE_SCOPED_PROJECT_ID, 7L)
            .containsEntry(CopilotConstants.STATE_WORKSPACE_ID, 1L);
        verify(permissionService).hasWorkspaceRole(1L, "EDITOR");
        verify(permissionService, never()).hasWorkspaceRole(5L, "EDITOR");
        verify(projectWorkflowService, never()).getWorkflowProjectWorkflow(any());
        verify(agUiService).runAgent(eq(agent), any());
    }

    @Test
    void testChatEmbeddedAutomationWorkflowEditorDeniedWithoutAWorkflow() {
        LocalAgent agent = localAgent("workflow_editor_ask");
        CopilotEmbeddedAccessChecker copilotEmbeddedAccessChecker = mock(CopilotEmbeddedAccessChecker.class);

        CopilotChatFacadeImpl facade = new CopilotChatFacadeImpl(
            agUiService, List.of(agent), Optional.of(permissionService), Optional.of(projectWorkflowService),
            Optional.empty(), Optional.of(copilotEmbeddedAccessChecker));

        AgUiParameters parameters = parameters(Map.<String, Object>of("mode", "ASK"));

        assertThatThrownBy(() -> facade.chat("workflow_editor_embedded_automation", parameters))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessage("The embedded automation Copilot run has no workflow");

        verify(agUiService, never()).runAgent(any(), any());
    }

    @Test
    void testChatEmbeddedAutomationWorkflowEditorDeniedForAWorkflowOutsideAnAutomationProject() {
        LocalAgent agent = localAgent("workflow_editor_ask");
        CopilotEmbeddedAccessChecker copilotEmbeddedAccessChecker = mock(CopilotEmbeddedAccessChecker.class);

        CopilotChatFacadeImpl facade = new CopilotChatFacadeImpl(
            agUiService, List.of(agent), Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.of(copilotEmbeddedAccessChecker));

        when(copilotEmbeddedAccessChecker.checkAutomationProjectWorkflowAccess("wf1"))
            .thenThrow(new IllegalArgumentException("Project with id 7 is not an automation project"));

        AgUiParameters parameters = parameters(Map.<String, Object>of("workflowId", "wf1", "mode", "ASK"));

        assertThatThrownBy(() -> facade.chat("workflow_editor_embedded_automation", parameters))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessage("Access denied to workflow wf1");

        verify(agUiService, never()).runAgent(any(), any());
    }

    @Test
    void testChatEmbeddedAutomationWorkflowEditorDeniedWhenTheEmbeddedAccessCheckerDenies() {
        LocalAgent agent = localAgent("workflow_editor_ask");
        CopilotEmbeddedAccessChecker copilotEmbeddedAccessChecker = mock(CopilotEmbeddedAccessChecker.class);

        CopilotChatFacadeImpl facade = new CopilotChatFacadeImpl(
            agUiService, List.of(agent), Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.of(copilotEmbeddedAccessChecker));

        when(copilotEmbeddedAccessChecker.checkAutomationProjectWorkflowAccess("wf1"))
            .thenThrow(new AccessDeniedException("denied"));

        AgUiParameters parameters = parameters(Map.<String, Object>of("workflowId", "wf1", "mode", "ASK"));

        assertThatThrownBy(() -> facade.chat("workflow_editor_embedded_automation", parameters))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessage("denied");

        verify(agUiService, never()).runAgent(any(), any());
    }

    @Test
    void testChatEmbeddedAutomationWorkflowEditorFailsClosedWithoutTheEmbeddedAccessChecker() {
        CopilotChatFacadeImpl facadeWithoutServices = new CopilotChatFacadeImpl(
            agUiService, List.of(localAgent("workflow_editor_ask")), Optional.empty(), Optional.empty(),
            Optional.empty(), Optional.empty());

        AgUiParameters parameters = parameters(Map.<String, Object>of("workflowId", "wf1", "mode", "ASK"));

        assertThatThrownBy(() -> facadeWithoutServices.chat("workflow_editor_embedded_automation", parameters))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessage("Workflow authorization is not available");

        verify(agUiService, never()).runAgent(any(), any());
    }

    private static String conversationScope(AgUiParameters parameters) {
        State state = parameters.getState();

        return (String) state.getState()
            .get(CopilotConstants.STATE_CONVERSATION_SCOPE);
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
