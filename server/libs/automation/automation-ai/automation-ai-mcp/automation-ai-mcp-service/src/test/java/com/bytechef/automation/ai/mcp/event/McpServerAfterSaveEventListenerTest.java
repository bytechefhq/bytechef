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

package com.bytechef.automation.ai.mcp.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.ai.mcp.domain.McpProject;
import com.bytechef.automation.ai.mcp.repository.McpProjectRepository;
import com.bytechef.automation.ai.mcp.security.McpServerOwnershipResolver;
import com.bytechef.automation.ai.mcp.service.WorkspaceMcpServerService;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacadeImpl;
import com.bytechef.automation.configuration.security.AutomationMethodSecurityExpressionHandler;
import com.bytechef.automation.configuration.security.AutomationPermissionEvaluator;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.repository.McpServerRepository;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.mcp.service.McpServerServiceImpl;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.data.relational.core.mapping.event.AfterSaveEvent;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authorization.method.AuthorizationManagerBeforeMethodInterceptor;
import org.springframework.security.authorization.method.PreAuthorizeAuthorizationManager;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Unit test for {@link McpServerAfterSaveEventListener}.
 *
 * @author Ivica Cardic
 */
public class McpServerAfterSaveEventListenerTest {

    private final McpProjectRepository mcpProjectRepository = mock(McpProjectRepository.class);
    private final ProjectDeploymentFacade projectDeploymentFacade = mock(ProjectDeploymentFacade.class);

    @Test
    public void testOnAfterSaveEnabledServerEnablesProjectDeployments() {
        // Given
        McpServerAfterSaveEventListener listener = new McpServerAfterSaveEventListener(
            mcpProjectRepository, projectDeploymentFacade);

        McpServer mcpServer = new McpServer();
        mcpServer.setId(1L);
        mcpServer.setEnabled(true);

        McpProject mcpProject1 = new McpProject(100L, 1L);
        McpProject mcpProject2 = new McpProject(200L, 1L);
        List<McpProject> mcpProjects = Arrays.asList(mcpProject1, mcpProject2);

        when(mcpProjectRepository.findAllByMcpServerId(1L)).thenReturn(mcpProjects);

        @SuppressWarnings("unchecked")
        AfterSaveEvent<McpServer> event = mock(AfterSaveEvent.class);
        when(event.getEntity()).thenReturn(mcpServer);

        // When
        listener.onAfterSave(event);

        // Then
        verify(mcpProjectRepository).findAllByMcpServerId(1L);
        verify(projectDeploymentFacade).enableProjectDeployment(eq(100L), eq(true));
        verify(projectDeploymentFacade).enableProjectDeployment(eq(200L), eq(true));
    }

    @Test
    public void testOnAfterSaveDisabledServerDisablesProjectDeployments() {
        // Given
        McpServerAfterSaveEventListener listener = new McpServerAfterSaveEventListener(
            mcpProjectRepository, projectDeploymentFacade);

        McpServer mcpServer = new McpServer();
        mcpServer.setId(1L);
        mcpServer.setEnabled(false);

        McpProject mcpProject1 = new McpProject(100L, 1L);
        McpProject mcpProject2 = new McpProject(200L, 1L);
        List<McpProject> mcpProjects = Arrays.asList(mcpProject1, mcpProject2);

        when(mcpProjectRepository.findAllByMcpServerId(1L)).thenReturn(mcpProjects);

        @SuppressWarnings("unchecked")
        AfterSaveEvent<McpServer> event = mock(AfterSaveEvent.class);
        when(event.getEntity()).thenReturn(mcpServer);

        // When
        listener.onAfterSave(event);

        // Then
        verify(mcpProjectRepository).findAllByMcpServerId(1L);
        verify(projectDeploymentFacade).enableProjectDeployment(eq(100L), eq(false));
        verify(projectDeploymentFacade).enableProjectDeployment(eq(200L), eq(false));
    }

    @Test
    public void testOnAfterSaveServerWithNoProjectsNoFacadeCalls() {
        // Given
        McpServerAfterSaveEventListener listener = new McpServerAfterSaveEventListener(
            mcpProjectRepository, projectDeploymentFacade);

        McpServer mcpServer = new McpServer();
        mcpServer.setId(1L);
        mcpServer.setEnabled(true);

        when(mcpProjectRepository.findAllByMcpServerId(1L)).thenReturn(Collections.emptyList());

        @SuppressWarnings("unchecked")
        AfterSaveEvent<McpServer> event = mock(AfterSaveEvent.class);
        when(event.getEntity()).thenReturn(mcpServer);

        // When
        listener.onAfterSave(event);

        // Then
        verify(mcpProjectRepository).findAllByMcpServerId(1L);
        verify(projectDeploymentFacade, times(0)).enableProjectDeployment(eq(100L), eq(true));
        verify(projectDeploymentFacade, times(0)).enableProjectDeployment(eq(200L), eq(true));
    }

    @Test
    public void testOnAfterSaveMultipleProjectsWithDifferentDeployments() {
        // Given
        McpServerAfterSaveEventListener listener = new McpServerAfterSaveEventListener(
            mcpProjectRepository, projectDeploymentFacade);

        McpServer mcpServer = new McpServer();
        mcpServer.setId(2L);
        mcpServer.setEnabled(true);

        McpProject mcpProject1 = new McpProject(300L, 2L);
        McpProject mcpProject2 = new McpProject(400L, 2L);
        McpProject mcpProject3 = new McpProject(500L, 2L);
        List<McpProject> mcpProjects = Arrays.asList(mcpProject1, mcpProject2, mcpProject3);

        when(mcpProjectRepository.findAllByMcpServerId(2L)).thenReturn(mcpProjects);

        @SuppressWarnings("unchecked")
        AfterSaveEvent<McpServer> event = mock(AfterSaveEvent.class);
        when(event.getEntity()).thenReturn(mcpServer);

        // When
        listener.onAfterSave(event);

        // Then
        verify(mcpProjectRepository).findAllByMcpServerId(2L);
        verify(projectDeploymentFacade).enableProjectDeployment(eq(300L), eq(true));
        verify(projectDeploymentFacade).enableProjectDeployment(eq(400L), eq(true));
        verify(projectDeploymentFacade).enableProjectDeployment(eq(500L), eq(true));
    }

    /**
     * Toggles an MCP server through the real {@code @PreAuthorize} guards, with a caller who is not a tenant admin and
     * whose MCP server scopes come from the workspace the server is assigned to.
     */
    @Nested
    class SecuredMcpServerToggle {

        private static final long MCP_PROJECT_ID = 9L;
        private static final long MCP_SERVER_ID = 5L;
        private static final long PROJECT_DEPLOYMENT_ID = 7L;
        private static final long WORKSPACE_ID = 42L;

        private final Map<Long, Long> workspaceIdsByMcpServerId = new HashMap<>();
        private final PermissionService permissionService = mock(PermissionService.class);
        private final ProjectDeploymentFacadeImpl projectDeploymentFacadeImpl =
            mock(ProjectDeploymentFacadeImpl.class);
        private final WorkspaceMcpServerService workspaceMcpServerService = mock(WorkspaceMcpServerService.class);

        private Set<String> grantedScopes = Set.of();

        @BeforeEach
        void beforeEach() {
            when(workspaceMcpServerService.fetchWorkspaceIdByMcpServerId(anyLong()))
                .thenAnswer(
                    invocation -> Optional.ofNullable(
                        workspaceIdsByMcpServerId.get(invocation.<Long>getArgument(0))));

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
        void testMemberWithMcpEditButNoDeploymentEditTogglesAnMcpServerWithMcpProjects() {
            grantedScopes = Set.of("MCP_EDIT");

            workspaceIdsByMcpServerId.put(MCP_SERVER_ID, WORKSPACE_ID);

            when(mcpProjectRepository.findAllByMcpServerId(MCP_SERVER_ID))
                .thenReturn(List.of(new McpProject(MCP_PROJECT_ID, PROJECT_DEPLOYMENT_ID, MCP_SERVER_ID)));

            McpServer mcpServer = createSecuredMcpServerService().update(MCP_SERVER_ID, null, false);

            assertThat(mcpServer.isEnabled()).isFalse();

            verify(projectDeploymentFacadeImpl).enableProjectDeployment(PROJECT_DEPLOYMENT_ID, false);
        }

        @Test
        void testMemberWithoutMcpEditCannotToggleAnMcpServer() {
            grantedScopes = Set.of("DEPLOYMENT_EDIT", "MCP_DELETE", "MCP_VIEW");

            workspaceIdsByMcpServerId.put(MCP_SERVER_ID, WORKSPACE_ID);

            McpServerService securedMcpServerService = createSecuredMcpServerService();

            assertThatThrownBy(() -> securedMcpServerService.update(MCP_SERVER_ID, null, false))
                .isInstanceOf(AccessDeniedException.class);

            verify(projectDeploymentFacadeImpl, never()).enableProjectDeployment(anyLong(), anyBoolean());
        }

        private McpServerService createSecuredMcpServerService() {
            McpServerRepository mcpServerRepository = mock(McpServerRepository.class);

            McpServer mcpServer = new McpServer("server", PlatformType.AUTOMATION, Environment.DEVELOPMENT, true);

            mcpServer.setId(MCP_SERVER_ID);

            McpServerAfterSaveEventListener mcpServerAfterSaveEventListener = new McpServerAfterSaveEventListener(
                mcpProjectRepository, secure(projectDeploymentFacadeImpl));

            when(mcpServerRepository.findById(MCP_SERVER_ID)).thenReturn(Optional.of(mcpServer));
            when(mcpServerRepository.save(any(McpServer.class))).thenAnswer(invocation -> {
                McpServer savedMcpServer = invocation.getArgument(0);

                AfterSaveEvent<McpServer> afterSaveEvent = createAfterSaveEvent(savedMcpServer);

                mcpServerAfterSaveEventListener.onAfterSave(afterSaveEvent);

                return savedMcpServer;
            });

            return secure(new McpServerServiceImpl(mcpServerRepository));
        }

        @SuppressWarnings("unchecked")
        private AfterSaveEvent<McpServer> createAfterSaveEvent(McpServer mcpServer) {
            AfterSaveEvent<McpServer> afterSaveEvent = mock(AfterSaveEvent.class);

            when(afterSaveEvent.getEntity()).thenReturn(mcpServer);

            return afterSaveEvent;
        }

        @SuppressWarnings("unchecked")
        private <T> T secure(T target) {
            AutomationMethodSecurityExpressionHandler expressionHandler =
                new AutomationMethodSecurityExpressionHandler(permissionService);

            expressionHandler.setPermissionEvaluator(new AutomationPermissionEvaluator(permissionService));

            PreAuthorizeAuthorizationManager preAuthorizeAuthorizationManager =
                new PreAuthorizeAuthorizationManager();

            preAuthorizeAuthorizationManager.setExpressionHandler(expressionHandler);

            ProxyFactory proxyFactory = new ProxyFactory(target);

            proxyFactory.addAdvice(AuthorizationManagerBeforeMethodInterceptor.preAuthorize(
                preAuthorizeAuthorizationManager));

            return (T) proxyFactory.getProxy();
        }
    }
}
