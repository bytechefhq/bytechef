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

package com.bytechef.automation.ai.mcp.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.ai.mcp.domain.McpProject;
import com.bytechef.automation.ai.mcp.domain.McpProjectWorkflow;
import com.bytechef.automation.ai.mcp.event.McpServerAfterSaveEventListener;
import com.bytechef.automation.ai.mcp.event.McpServerBeforeDeleteEventListener;
import com.bytechef.automation.ai.mcp.repository.McpProjectRepository;
import com.bytechef.automation.ai.mcp.security.McpProjectWorkspaceGuard;
import com.bytechef.automation.ai.mcp.security.McpServerOwnershipResolver;
import com.bytechef.automation.ai.mcp.service.McpProjectService;
import com.bytechef.automation.ai.mcp.service.McpProjectServiceImpl;
import com.bytechef.automation.ai.mcp.service.McpProjectWorkflowService;
import com.bytechef.automation.ai.mcp.service.McpProjectWorkflowServiceImpl;
import com.bytechef.automation.ai.mcp.service.WorkspaceMcpServerService;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacadeImpl;
import com.bytechef.automation.configuration.security.AutomationMethodSecurityExpressionHandler;
import com.bytechef.automation.configuration.security.AutomationPermissionEvaluator;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.facade.McpServerFacade;
import com.bytechef.platform.mcp.facade.McpServerFacadeImpl;
import com.bytechef.platform.mcp.service.McpComponentService;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.mcp.service.McpToolService;
import com.bytechef.platform.tag.service.TagService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.relational.core.mapping.event.AfterSaveEvent;
import org.springframework.data.relational.core.mapping.event.BeforeDeleteEvent;
import org.springframework.data.relational.core.mapping.event.Identifier;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authorization.method.AuthorizationManagerBeforeMethodInterceptor;
import org.springframework.security.authorization.method.PreAuthorizeAuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.util.SimpleMethodInvocation;

/**
 * Evaluates the real {@code @PreAuthorize} expressions on {@link WorkspaceMcpServerFacadeImpl} through the real
 * {@link AutomationMethodSecurityExpressionHandler} backed by the real {@link AutomationPermissionEvaluator}, asserting
 * each guard in both directions and the exact check that reaches {@link PermissionService}, and runs the workspace MCP
 * server create and delete flows through the real guards, with a caller who is not a tenant admin and whose MCP server
 * scopes come from the workspace the server is assigned to.
 *
 * @author Ivica Cardic
 */
class WorkspaceMcpServerFacadeTest {

    private static final Environment ENVIRONMENT = Environment.STAGING;
    private static final long MCP_PROJECT_ID = 9L;
    private static final long MCP_PROJECT_WORKFLOW_ID = 11L;
    private static final long MCP_SERVER_ID = 5L;
    private static final long PROJECT_DEPLOYMENT_ID = 7L;
    private static final long PROJECT_DEPLOYMENT_WORKFLOW_ID = 12L;
    private static final long WORKSPACE_ID = 42L;

    private final Map<Long, Long> workspaceIdsByMcpServerId = new HashMap<>();
    private final McpProjectRepository mcpProjectRepository = mock(McpProjectRepository.class);
    private final McpServerService mcpServerService = mock(McpServerService.class);
    private final PermissionService permissionService = mock(PermissionService.class);
    private final ProjectDeploymentFacadeImpl projectDeploymentFacade = mock(ProjectDeploymentFacadeImpl.class);
    private final WorkspaceMcpServerService workspaceMcpServerService = mock(WorkspaceMcpServerService.class);

    private Set<String> grantedScopes = Set.of();

    @BeforeEach
    void beforeEach() {
        when(workspaceMcpServerService.fetchWorkspaceIdByMcpServerId(anyLong()))
            .thenAnswer(
                invocation -> Optional.ofNullable(workspaceIdsByMcpServerId.get(invocation.<Long>getArgument(0))));

        doAnswer(invocation -> workspaceIdsByMcpServerId.put(invocation.getArgument(0), invocation.getArgument(1)))
            .when(workspaceMcpServerService)
            .assignMcpServerToWorkspace(anyLong(), anyLong());
        doAnswer(invocation -> workspaceIdsByMcpServerId.remove(invocation.<Long>getArgument(0)))
            .when(workspaceMcpServerService)
            .removeMcpServerFromWorkspace(anyLong());

        McpServerOwnershipResolver mcpServerOwnershipResolver =
            new McpServerOwnershipResolver(workspaceMcpServerService);

        when(permissionService.hasResourceScope(any(Serializable.class), eq("McpServer"), anyString()))
            .thenAnswer(invocation -> {
                long mcpServerId = invocation.getArgument(0);

                return mcpServerOwnershipResolver.resolveOwner(mcpServerId)
                    .workspaceId()
                    .isPresent() && grantedScopes.contains(invocation.<String>getArgument(2));
            });
        when(permissionService.hasWorkspaceScope(anyLong(), anyString(), any(Environment.class)))
            .thenAnswer(invocation -> grantedScopes.contains(invocation.<String>getArgument(1)));

        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();

        securityContext.setAuthentication(
            new UsernamePasswordAuthenticationToken(
                "workspace-admin", null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));

        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testGetWorkspaceMcpServersDeniesWhenTheMcpViewScopeIsRefusedOnTheWorkspace() throws Exception {
        assertResourceGuard(getWorkspaceMcpServersMethod(), new Object[] {
            WORKSPACE_ID
        }, WORKSPACE_ID, "Workspace", "MCP_VIEW", false);
    }

    @Test
    void testGetWorkspaceMcpServersAllowsWhenTheMcpViewScopeIsGrantedOnTheWorkspace() throws Exception {
        assertResourceGuard(getWorkspaceMcpServersMethod(), new Object[] {
            WORKSPACE_ID
        }, WORKSPACE_ID, "Workspace", "MCP_VIEW", true);
    }

    @Test
    void testDeleteWorkspaceMcpServerDeniesWhenTheMcpDeleteScopeIsRefused() throws Exception {
        assertResourceGuard(deleteWorkspaceMcpServerMethod(), new Object[] {
            MCP_SERVER_ID
        }, MCP_SERVER_ID, "McpServer", "MCP_DELETE", false);
    }

    @Test
    void testDeleteWorkspaceMcpServerAllowsWhenTheMcpDeleteScopeIsGranted() throws Exception {
        assertResourceGuard(deleteWorkspaceMcpServerMethod(), new Object[] {
            MCP_SERVER_ID
        }, MCP_SERVER_ID, "McpServer", "MCP_DELETE", true);
    }

    @Test
    void testCreateWorkspaceMcpServerDeniesWhenTheMcpCreateScopeIsRefusedInTheServersEnvironment() throws Exception {
        assertCreateGuard(false);
    }

    @Test
    void testCreateWorkspaceMcpServerAllowsWhenTheMcpCreateScopeIsGrantedInTheServersEnvironment() throws Exception {
        assertCreateGuard(true);
    }

    @Test
    void testWorkspaceAdminDeletesAnMcpServerOfTheWorkspace() {
        grantedScopes = Set.of("MCP_DELETE", "MCP_EDIT", "MCP_VIEW");

        workspaceIdsByMcpServerId.put(MCP_SERVER_ID, WORKSPACE_ID);

        givenDeleteFiresTheBeforeDeleteListener();

        createWorkspaceMcpServerFacade().deleteWorkspaceMcpServer(MCP_SERVER_ID);

        verify(mcpServerService).delete(MCP_SERVER_ID);

        assertThat(workspaceIdsByMcpServerId).doesNotContainKey(MCP_SERVER_ID);
    }

    @Test
    void testMemberWithoutMcpDeleteCannotDeleteAnMcpServerOfTheWorkspace() {
        grantedScopes = Set.of("MCP_EDIT", "MCP_VIEW");

        workspaceIdsByMcpServerId.put(MCP_SERVER_ID, WORKSPACE_ID);

        givenDeleteFiresTheBeforeDeleteListener();

        WorkspaceMcpServerFacade workspaceMcpServerFacade = createWorkspaceMcpServerFacade();

        assertThatThrownBy(() -> workspaceMcpServerFacade.deleteWorkspaceMcpServer(MCP_SERVER_ID))
            .isInstanceOf(AccessDeniedException.class);

        verify(mcpServerService, never()).delete(anyLong());

        assertThat(workspaceIdsByMcpServerId).containsEntry(MCP_SERVER_ID, WORKSPACE_ID);
    }

    @Test
    void testWorkspaceAdminCreatesAnMcpServerInTheWorkspace() {
        grantedScopes = Set.of("MCP_CREATE", "MCP_EDIT", "MCP_VIEW");

        givenCreateFiresTheAfterSaveListener();

        McpServer mcpServer = createWorkspaceMcpServerFacade().createWorkspaceMcpServer(
            "server", PlatformType.AUTOMATION, Environment.DEVELOPMENT, true, WORKSPACE_ID);

        assertThat(mcpServer.getId()).isEqualTo(MCP_SERVER_ID);
        assertThat(workspaceIdsByMcpServerId).containsEntry(MCP_SERVER_ID, WORKSPACE_ID);
    }

    @Test
    void testMemberWithoutMcpCreateCannotCreateAnMcpServerInTheWorkspace() {
        grantedScopes = Set.of("MCP_EDIT", "MCP_VIEW");

        givenCreateFiresTheAfterSaveListener();

        WorkspaceMcpServerFacade workspaceMcpServerFacade = createWorkspaceMcpServerFacade();

        assertThatThrownBy(() -> workspaceMcpServerFacade.createWorkspaceMcpServer(
            "server", PlatformType.AUTOMATION, Environment.DEVELOPMENT, true, WORKSPACE_ID))
                .isInstanceOf(AccessDeniedException.class);

        verify(mcpServerService, never()).create(anyString(), any(), any(), anyBoolean());
    }

    @Test
    void testMemberWithMcpDeleteButNoMcpEditDeletesAnMcpServerWithMcpProjects() {
        grantedScopes = Set.of("MCP_DELETE");

        workspaceIdsByMcpServerId.put(MCP_SERVER_ID, WORKSPACE_ID);

        when(mcpProjectRepository.findAllByMcpServerId(MCP_SERVER_ID))
            .thenReturn(List.of(new McpProject(MCP_PROJECT_ID, PROJECT_DEPLOYMENT_ID, MCP_SERVER_ID)));

        McpProjectWorkflowServiceImpl mcpProjectWorkflowService = mock(McpProjectWorkflowServiceImpl.class);
        ProjectDeploymentService projectDeploymentService = mock(ProjectDeploymentService.class);
        ProjectDeploymentWorkflowService projectDeploymentWorkflowService =
            mock(ProjectDeploymentWorkflowService.class);

        McpProjectWorkflow mcpProjectWorkflow = new McpProjectWorkflow();

        mcpProjectWorkflow.setId(MCP_PROJECT_WORKFLOW_ID);
        mcpProjectWorkflow.setMcpProjectId(MCP_PROJECT_ID);
        mcpProjectWorkflow.setProjectDeploymentWorkflowId(PROJECT_DEPLOYMENT_WORKFLOW_ID);

        ProjectDeploymentWorkflow projectDeploymentWorkflow = new ProjectDeploymentWorkflow();

        projectDeploymentWorkflow.setId(PROJECT_DEPLOYMENT_WORKFLOW_ID);

        when(mcpProjectWorkflowService.getMcpProjectMcpProjectWorkflows(MCP_PROJECT_ID))
            .thenReturn(List.of(mcpProjectWorkflow));
        when(projectDeploymentWorkflowService.getProjectDeploymentWorkflow(PROJECT_DEPLOYMENT_WORKFLOW_ID))
            .thenReturn(projectDeploymentWorkflow);

        givenDeleteFiresTheBeforeDeleteListener(
            new McpServerBeforeDeleteEventListener(
                createMcpProjectService(), secure(mcpProjectWorkflowService), projectDeploymentWorkflowService,
                projectDeploymentService, secure(projectDeploymentFacade), workspaceMcpServerService));

        createWorkspaceMcpServerFacade().deleteWorkspaceMcpServer(MCP_SERVER_ID);

        verify(projectDeploymentFacade).enableProjectDeployment(PROJECT_DEPLOYMENT_ID, false);
        verify(mcpProjectWorkflowService).delete(MCP_PROJECT_WORKFLOW_ID);
        verify(mcpProjectRepository).deleteById(MCP_PROJECT_ID);
        verify(projectDeploymentService).delete(PROJECT_DEPLOYMENT_ID);
        verify(mcpServerService).delete(MCP_SERVER_ID);

        assertThat(workspaceIdsByMcpServerId).doesNotContainKey(MCP_SERVER_ID);
    }

    private static void assertResourceGuard(
        Method method, Object[] arguments, long expectedId, String expectedType, String expectedScope,
        boolean granted) {

        PermissionService permissionService = mock(PermissionService.class);

        when(permissionService.hasResourceScope(expectedId, expectedType, expectedScope)).thenReturn(granted);

        assertThat(evaluateGuard(permissionService, method, arguments))
            .as("%s must %s when hasResourceScope(%s, '%s', '%s') returns %s", method.getName(),
                granted ? "allow" : "deny", expectedId, expectedType, expectedScope, granted)
            .isEqualTo(granted);

        verify(permissionService).hasResourceScope(expectedId, expectedType, expectedScope);
        verifyNoMoreInteractions(permissionService);
    }

    private static void assertCreateGuard(boolean granted) throws Exception {
        PermissionService permissionService = mock(PermissionService.class);

        when(permissionService.hasWorkspaceScope(WORKSPACE_ID, "MCP_CREATE", ENVIRONMENT)).thenReturn(granted);

        Method method = WorkspaceMcpServerFacadeImpl.class.getMethod(
            "createWorkspaceMcpServer", String.class, PlatformType.class, Environment.class, Boolean.class,
            Long.class);

        assertThat(evaluateGuard(permissionService, method, new Object[] {
            "name", PlatformType.AUTOMATION, ENVIRONMENT, Boolean.TRUE, WORKSPACE_ID
        }))
            .isEqualTo(granted);

        verify(permissionService).hasWorkspaceScope(WORKSPACE_ID, "MCP_CREATE", ENVIRONMENT);
        verifyNoMoreInteractions(permissionService);
    }

    // The expression parsed here is read straight off our own @PreAuthorize annotation in this repository's compiled
    // bytecode, not attacker-influenced input.
    @SuppressFBWarnings(
        value = "SPEL_INJECTION",
        justification = "The expression is this repository's own @PreAuthorize value, not untrusted input.")
    private static boolean evaluateGuard(PermissionService permissionService, Method method, Object[] arguments) {
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize)
            .as("%s must carry a @PreAuthorize guard", method.getName())
            .isNotNull();

        AutomationMethodSecurityExpressionHandler expressionHandler =
            new AutomationMethodSecurityExpressionHandler(permissionService);

        expressionHandler.setPermissionEvaluator(new AutomationPermissionEvaluator(permissionService));

        Authentication authentication = new UsernamePasswordAuthenticationToken("alice", "credentials", List.of());

        SimpleMethodInvocation methodInvocation = new SimpleMethodInvocation(new Object(), method, arguments);

        EvaluationContext evaluationContext =
            expressionHandler.createEvaluationContext(() -> authentication, methodInvocation);

        Expression expression = expressionHandler.getExpressionParser()
            .parseExpression(preAuthorize.value());

        return Boolean.TRUE.equals(expression.getValue(evaluationContext, Boolean.class));
    }

    private static Method deleteWorkspaceMcpServerMethod() throws NoSuchMethodException {
        return WorkspaceMcpServerFacadeImpl.class.getMethod("deleteWorkspaceMcpServer", Long.class);
    }

    private static Method getWorkspaceMcpServersMethod() throws NoSuchMethodException {
        return WorkspaceMcpServerFacadeImpl.class.getMethod("getWorkspaceMcpServers", Long.class);
    }

    private WorkspaceMcpServerFacade createWorkspaceMcpServerFacade() {
        McpComponentService mcpComponentService = mock(McpComponentService.class);

        when(mcpComponentService.getMcpServerMcpComponents(anyLong())).thenReturn(List.of());

        McpServerFacade mcpServerFacade = secure(
            new McpServerFacadeImpl(
                mcpComponentService, mcpServerService, mock(McpToolService.class), mock(TagService.class)));

        return secure(new WorkspaceMcpServerFacadeImpl(mcpServerFacade, mcpServerService, workspaceMcpServerService));
    }

    private McpProjectService createMcpProjectService() {
        return secure(new McpProjectServiceImpl(mcpProjectRepository, mock(McpProjectWorkspaceGuard.class)));
    }

    @SuppressWarnings("unchecked")
    private void givenCreateFiresTheAfterSaveListener() {
        McpServerAfterSaveEventListener mcpServerAfterSaveEventListener = new McpServerAfterSaveEventListener(
            mcpProjectRepository, mock(ProjectDeploymentFacade.class));

        when(mcpServerService.create(anyString(), any(), any(), anyBoolean())).thenAnswer(invocation -> {
            McpServer mcpServer = new McpServer(
                invocation.getArgument(0), invocation.getArgument(1), invocation.getArgument(2),
                invocation.getArgument(3));

            mcpServer.setId(MCP_SERVER_ID);

            AfterSaveEvent<McpServer> afterSaveEvent = mock(AfterSaveEvent.class);

            when(afterSaveEvent.getEntity()).thenReturn(mcpServer);
            when(afterSaveEvent.getType()).thenReturn(McpServer.class);

            mcpServerAfterSaveEventListener.onApplicationEvent(afterSaveEvent);

            return mcpServer;
        });
    }

    private void givenDeleteFiresTheBeforeDeleteListener() {
        givenDeleteFiresTheBeforeDeleteListener(
            new McpServerBeforeDeleteEventListener(
                createMcpProjectService(), mock(McpProjectWorkflowService.class),
                mock(ProjectDeploymentWorkflowService.class), mock(ProjectDeploymentService.class),
                mock(ProjectDeploymentFacade.class), workspaceMcpServerService));
    }

    @SuppressWarnings("unchecked")
    private void givenDeleteFiresTheBeforeDeleteListener(
        McpServerBeforeDeleteEventListener mcpServerBeforeDeleteEventListener) {

        doAnswer(invocation -> {
            long mcpServerId = invocation.getArgument(0);

            BeforeDeleteEvent<McpServer> beforeDeleteEvent = mock(BeforeDeleteEvent.class);

            when(beforeDeleteEvent.getId()).thenReturn(Identifier.of(mcpServerId));
            when(beforeDeleteEvent.getType()).thenReturn(McpServer.class);

            mcpServerBeforeDeleteEventListener.onApplicationEvent(beforeDeleteEvent);

            if (workspaceIdsByMcpServerId.containsKey(mcpServerId)) {
                throw new DataIntegrityViolationException("workspace_mcp_server still references the server");
            }

            return null;
        }).when(mcpServerService)
            .delete(anyLong());
    }

    @SuppressWarnings("unchecked")
    private <T> T secure(T target) {
        AutomationMethodSecurityExpressionHandler expressionHandler =
            new AutomationMethodSecurityExpressionHandler(permissionService);

        expressionHandler.setPermissionEvaluator(new AutomationPermissionEvaluator(permissionService));

        PreAuthorizeAuthorizationManager preAuthorizeAuthorizationManager = new PreAuthorizeAuthorizationManager();

        preAuthorizeAuthorizationManager.setExpressionHandler(expressionHandler);

        ProxyFactory proxyFactory = new ProxyFactory(target);

        proxyFactory.addAdvice(AuthorizationManagerBeforeMethodInterceptor.preAuthorize(
            preAuthorizeAuthorizationManager));

        return (T) proxyFactory.getProxy();
    }
}
