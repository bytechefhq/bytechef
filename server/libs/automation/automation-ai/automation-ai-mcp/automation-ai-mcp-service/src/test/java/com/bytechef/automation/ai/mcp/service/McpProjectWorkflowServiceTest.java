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

package com.bytechef.automation.ai.mcp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.ai.mcp.domain.McpProject;
import com.bytechef.automation.ai.mcp.domain.McpProjectWorkflow;
import com.bytechef.automation.ai.mcp.repository.McpProjectRepository;
import com.bytechef.automation.ai.mcp.repository.McpProjectWorkflowRepository;
import com.bytechef.automation.ai.mcp.security.McpProjectWorkspaceGuard;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.security.AutomationMethodSecurityExpressionHandler;
import com.bytechef.automation.configuration.security.AutomationPermissionEvaluator;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authorization.method.AuthorizationManagerBeforeMethodInterceptor;
import org.springframework.security.authorization.method.PreAuthorizeAuthorizationManager;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author Ivica Cardic
 */
class McpProjectWorkflowServiceTest {

    private static final long FOREIGN_PROJECT_DEPLOYMENT_WORKFLOW_ID = 71L;
    private static final long MCP_PROJECT_ID = 3L;
    private static final String MCP_PROJECT_TYPE = "McpProject";
    private static final long MCP_PROJECT_WORKFLOW_ID = 21L;
    private static final String MCP_PROJECT_WORKFLOW_TYPE = "McpProjectWorkflow";
    private static final long MCP_SERVER_ID = 5L;
    private static final long PROJECT_DEPLOYMENT_ID = 20L;
    private static final long PROJECT_DEPLOYMENT_WORKFLOW_ID = 70L;

    private final McpProjectRepository mcpProjectRepository = mock(McpProjectRepository.class);
    private final McpProjectWorkflowRepository mcpProjectWorkflowRepository = mock(McpProjectWorkflowRepository.class);
    private final McpProjectWorkspaceGuard mcpProjectWorkspaceGuard = mock(McpProjectWorkspaceGuard.class);
    private final ProjectDeploymentWorkflowService projectDeploymentWorkflowService =
        mock(ProjectDeploymentWorkflowService.class);

    private final McpProjectWorkflowServiceImpl mcpProjectWorkflowService = new McpProjectWorkflowServiceImpl(
        mcpProjectRepository, mcpProjectWorkflowRepository, mcpProjectWorkspaceGuard,
        projectDeploymentWorkflowService);

    @BeforeEach
    void beforeEach() {
        when(mcpProjectRepository.findById(MCP_PROJECT_ID))
            .thenReturn(Optional.of(new McpProject(MCP_PROJECT_ID, PROJECT_DEPLOYMENT_ID, MCP_SERVER_ID)));
        when(projectDeploymentWorkflowService.getProjectDeploymentWorkflow(PROJECT_DEPLOYMENT_WORKFLOW_ID))
            .thenReturn(createProjectDeploymentWorkflow(PROJECT_DEPLOYMENT_ID));
        when(projectDeploymentWorkflowService.getProjectDeploymentWorkflow(FOREIGN_PROJECT_DEPLOYMENT_WORKFLOW_ID))
            .thenReturn(createProjectDeploymentWorkflow(PROJECT_DEPLOYMENT_ID + 1));
        when(mcpProjectWorkflowRepository.save(any(McpProjectWorkflow.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void testCreateBindsAWorkflowOfTheMcpProjectsDeployment() {
        McpProjectWorkflow mcpProjectWorkflow = mcpProjectWorkflowService.create(
            MCP_PROJECT_ID, PROJECT_DEPLOYMENT_WORKFLOW_ID);

        assertThat(mcpProjectWorkflow.getProjectDeploymentWorkflowId()).isEqualTo(PROJECT_DEPLOYMENT_WORKFLOW_ID);

        verify(mcpProjectWorkspaceGuard).requireDeploymentInServerWorkspace(MCP_SERVER_ID, PROJECT_DEPLOYMENT_ID);
    }

    @Test
    void testCreateRefusesAWorkflowOfAnotherDeployment() {
        assertThatThrownBy(
            () -> mcpProjectWorkflowService.create(MCP_PROJECT_ID, FOREIGN_PROJECT_DEPLOYMENT_WORKFLOW_ID))
                .isInstanceOf(AccessDeniedException.class);

        verify(mcpProjectWorkflowRepository, never()).save(any());
    }

    @Test
    void testCreateRefusesAnMcpProjectWhoseDeploymentIsOutsideTheServersWorkspace() {
        doThrow(new AccessDeniedException("other workspace"))
            .when(mcpProjectWorkspaceGuard)
            .requireDeploymentInServerWorkspace(MCP_SERVER_ID, PROJECT_DEPLOYMENT_ID);

        assertThatThrownBy(() -> mcpProjectWorkflowService.create(MCP_PROJECT_ID, PROJECT_DEPLOYMENT_WORKFLOW_ID))
            .isInstanceOf(AccessDeniedException.class);

        verify(mcpProjectWorkflowRepository, never()).save(any());
    }

    @Test
    void testUpdateRefusesRebindingToAWorkflowOfAnotherDeployment() {
        McpProjectWorkflow mcpProjectWorkflow = new McpProjectWorkflow(MCP_PROJECT_ID, PROJECT_DEPLOYMENT_WORKFLOW_ID);

        mcpProjectWorkflow.setId(9L);

        when(mcpProjectWorkflowRepository.findById(9L)).thenReturn(Optional.of(mcpProjectWorkflow));

        assertThatThrownBy(() -> mcpProjectWorkflowService.update(9L, null, FOREIGN_PROJECT_DEPLOYMENT_WORKFLOW_ID))
            .isInstanceOf(AccessDeniedException.class);

        verify(mcpProjectWorkflowRepository, never()).save(any());
    }

    private static McpProjectWorkflow mcpProjectWorkflow() {
        McpProjectWorkflow mcpProjectWorkflow =
            new McpProjectWorkflow(MCP_PROJECT_ID, PROJECT_DEPLOYMENT_WORKFLOW_ID);

        mcpProjectWorkflow.setId(MCP_PROJECT_WORKFLOW_ID);

        return mcpProjectWorkflow;
    }

    private static ProjectDeploymentWorkflow createProjectDeploymentWorkflow(long projectDeploymentId) {
        ProjectDeploymentWorkflow projectDeploymentWorkflow = new ProjectDeploymentWorkflow();

        projectDeploymentWorkflow.setProjectDeploymentId(projectDeploymentId);

        return projectDeploymentWorkflow;
    }

    @Nested
    class MethodSecurityEnforcement {

        private static final String BODY_REACHED = "body reached";

        @BeforeEach
        void beforeEach() {
            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();

            securityContext.setAuthentication(
                new UsernamePasswordAuthenticationToken(
                    "member", null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));

            SecurityContextHolder.setContext(securityContext);
        }

        @AfterEach
        void afterEach() {
            SecurityContextHolder.clearContext();
        }

        @Test
        void testCreateDeniesWhenTheMcpEditScopeIsRefusedOnTheWorkflowsProject() {
            assertResourceGuard(
                service -> service.create(mcpProjectWorkflow()), MCP_PROJECT_ID, MCP_PROJECT_TYPE, "MCP_EDIT", false);
        }

        @Test
        void testCreateAllowsWhenTheMcpEditScopeIsGrantedOnTheWorkflowsProject() {
            assertResourceGuard(
                service -> service.create(mcpProjectWorkflow()), MCP_PROJECT_ID, MCP_PROJECT_TYPE, "MCP_EDIT", true);
        }

        @Test
        void testCreateByIdsDeniesWhenTheMcpEditScopeIsRefusedOnTheProject() {
            assertResourceGuard(
                service -> service.create(MCP_PROJECT_ID, PROJECT_DEPLOYMENT_WORKFLOW_ID), MCP_PROJECT_ID,
                MCP_PROJECT_TYPE, "MCP_EDIT", false);
        }

        @Test
        void testCreateByIdsAllowsWhenTheMcpEditScopeIsGrantedOnTheProject() {
            assertResourceGuard(
                service -> service.create(MCP_PROJECT_ID, PROJECT_DEPLOYMENT_WORKFLOW_ID), MCP_PROJECT_ID,
                MCP_PROJECT_TYPE, "MCP_EDIT", true);
        }

        @Test
        void testDeleteDeniesWhenTheMcpEditScopeIsRefused() {
            assertResourceGuard(
                service -> service.delete(MCP_PROJECT_WORKFLOW_ID), MCP_PROJECT_WORKFLOW_ID,
                MCP_PROJECT_WORKFLOW_TYPE, "MCP_EDIT", false);
        }

        @Test
        void testDeleteAllowsWhenTheMcpEditScopeIsGranted() {
            assertResourceGuard(
                service -> service.delete(MCP_PROJECT_WORKFLOW_ID), MCP_PROJECT_WORKFLOW_ID,
                MCP_PROJECT_WORKFLOW_TYPE, "MCP_EDIT", true);
        }

        @Test
        void testFetchMcpProjectWorkflowDeniesWhenTheMcpViewScopeIsRefused() {
            assertResourceGuard(
                service -> service.fetchMcpProjectWorkflow(MCP_PROJECT_WORKFLOW_ID), MCP_PROJECT_WORKFLOW_ID,
                MCP_PROJECT_WORKFLOW_TYPE, "MCP_VIEW", false);
        }

        @Test
        void testFetchMcpProjectWorkflowAllowsWhenTheMcpViewScopeIsGranted() {
            assertResourceGuard(
                service -> service.fetchMcpProjectWorkflow(MCP_PROJECT_WORKFLOW_ID), MCP_PROJECT_WORKFLOW_ID,
                MCP_PROJECT_WORKFLOW_TYPE, "MCP_VIEW", true);
        }

        @Test
        void testUpdateParametersDeniesWhenTheMcpEditScopeIsRefused() {
            assertResourceGuard(
                service -> service.updateParameters(MCP_PROJECT_WORKFLOW_ID, Map.of()), MCP_PROJECT_WORKFLOW_ID,
                MCP_PROJECT_WORKFLOW_TYPE, "MCP_EDIT", false);
        }

        @Test
        void testUpdateParametersAllowsWhenTheMcpEditScopeIsGranted() {
            assertResourceGuard(
                service -> service.updateParameters(MCP_PROJECT_WORKFLOW_ID, Map.of()), MCP_PROJECT_WORKFLOW_ID,
                MCP_PROJECT_WORKFLOW_TYPE, "MCP_EDIT", true);
        }

        @Test
        void testUpdateDeniesWhenTheMcpEditScopeIsRefusedOnTheWorkflow() {
            assertCompoundGuard(service -> service.update(mcpProjectWorkflow()), false, true);
        }

        @Test
        void testUpdateDeniesWhenTheMcpEditScopeIsRefusedOnTheProject() {
            assertCompoundGuard(service -> service.update(mcpProjectWorkflow()), true, false);
        }

        @Test
        void testUpdateAllowsWhenTheMcpEditScopeIsGrantedOnBothTheWorkflowAndTheProject() {
            assertCompoundGuard(service -> service.update(mcpProjectWorkflow()), true, true);
        }

        @Test
        void testUpdateByIdsDeniesWhenTheMcpEditScopeIsRefusedOnTheWorkflow() {
            assertCompoundGuard(
                service -> service.update(MCP_PROJECT_WORKFLOW_ID, MCP_PROJECT_ID, PROJECT_DEPLOYMENT_WORKFLOW_ID),
                false, true);
        }

        @Test
        void testUpdateByIdsDeniesWhenTheMcpEditScopeIsRefusedOnTheTargetProject() {
            assertCompoundGuard(
                service -> service.update(MCP_PROJECT_WORKFLOW_ID, MCP_PROJECT_ID, PROJECT_DEPLOYMENT_WORKFLOW_ID),
                true, false);
        }

        @Test
        void testUpdateByIdsAllowsWhenTheMcpEditScopeIsGrantedOnBothTheWorkflowAndTheTargetProject() {
            assertCompoundGuard(
                service -> service.update(MCP_PROJECT_WORKFLOW_ID, MCP_PROJECT_ID, PROJECT_DEPLOYMENT_WORKFLOW_ID),
                true, true);
        }

        @Test
        void testUpdateByIdsWithoutAProjectDeniesWhenTheMcpEditScopeIsRefusedOnTheWorkflow() {
            assertResourceGuard(
                service -> service.update(MCP_PROJECT_WORKFLOW_ID, null, PROJECT_DEPLOYMENT_WORKFLOW_ID),
                MCP_PROJECT_WORKFLOW_ID, MCP_PROJECT_WORKFLOW_TYPE, "MCP_EDIT", false);
        }

        @Test
        void testUpdateByIdsWithoutAProjectAllowsWhenTheMcpEditScopeIsGrantedOnTheWorkflow() {
            assertResourceGuard(
                service -> service.update(MCP_PROJECT_WORKFLOW_ID, null, PROJECT_DEPLOYMENT_WORKFLOW_ID),
                MCP_PROJECT_WORKFLOW_ID, MCP_PROJECT_WORKFLOW_TYPE, "MCP_EDIT", true);
        }

        private void assertResourceGuard(
            Consumer<McpProjectWorkflowService> invocation, long expectedId, String expectedType,
            String expectedScope, boolean granted) {

            PermissionService permissionService = mock(PermissionService.class);

            when(permissionService.hasResourceScope(expectedId, expectedType, expectedScope)).thenReturn(granted);

            assertInvocationOutcome(invocation, permissionService, granted);

            verify(permissionService).hasResourceScope(expectedId, expectedType, expectedScope);
            verifyNoMoreInteractions(permissionService);
        }

        private void assertCompoundGuard(
            Consumer<McpProjectWorkflowService> invocation, boolean workflowGranted, boolean projectGranted) {

            PermissionService permissionService = mock(PermissionService.class);

            when(permissionService.hasResourceScope(MCP_PROJECT_WORKFLOW_ID, MCP_PROJECT_WORKFLOW_TYPE, "MCP_EDIT"))
                .thenReturn(workflowGranted);
            when(permissionService.hasResourceScope(MCP_PROJECT_ID, MCP_PROJECT_TYPE, "MCP_EDIT"))
                .thenReturn(projectGranted);

            assertInvocationOutcome(invocation, permissionService, workflowGranted && projectGranted);

            verify(permissionService).hasResourceScope(MCP_PROJECT_WORKFLOW_ID, MCP_PROJECT_WORKFLOW_TYPE, "MCP_EDIT");
            verify(permissionService, times(workflowGranted ? 1 : 0))
                .hasResourceScope(MCP_PROJECT_ID, MCP_PROJECT_TYPE, "MCP_EDIT");
            verifyNoMoreInteractions(permissionService);
        }

        private void assertInvocationOutcome(
            Consumer<McpProjectWorkflowService> invocation, PermissionService permissionService, boolean allowed) {

            McpProjectWorkflowService securedMcpProjectWorkflowService = secure(
                new McpProjectWorkflowServiceImpl(
                    bodyReachedMock(McpProjectRepository.class), bodyReachedMock(McpProjectWorkflowRepository.class),
                    bodyReachedMock(McpProjectWorkspaceGuard.class),
                    bodyReachedMock(ProjectDeploymentWorkflowService.class)),
                permissionService);

            if (allowed) {
                assertThatThrownBy(() -> invocation.accept(securedMcpProjectWorkflowService))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage(BODY_REACHED);
            } else {
                assertThatThrownBy(() -> invocation.accept(securedMcpProjectWorkflowService))
                    .isInstanceOf(AccessDeniedException.class);
            }
        }

        private <T> T bodyReachedMock(Class<T> type) {
            return mock(type, invocation -> {
                throw new IllegalStateException(BODY_REACHED);
            });
        }

        @SuppressWarnings("unchecked")
        private <T> T secure(T target, PermissionService permissionService) {
            AutomationMethodSecurityExpressionHandler expressionHandler =
                new AutomationMethodSecurityExpressionHandler(permissionService);

            expressionHandler.setPermissionEvaluator(new AutomationPermissionEvaluator(permissionService));

            PreAuthorizeAuthorizationManager preAuthorizeAuthorizationManager =
                new PreAuthorizeAuthorizationManager();

            preAuthorizeAuthorizationManager.setExpressionHandler(expressionHandler);

            ProxyFactory proxyFactory = new ProxyFactory(target);

            proxyFactory.addAdvice(
                AuthorizationManagerBeforeMethodInterceptor.preAuthorize(preAuthorizeAuthorizationManager));

            return (T) proxyFactory.getProxy();
        }
    }
}
