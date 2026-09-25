/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.security.AutomationMethodSecurityExpressionHandler;
import com.bytechef.automation.configuration.security.AutomationPermissionEvaluator;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.automation.configuration.repository.WorkspaceRepository;
import com.bytechef.ee.automation.configuration.repository.WorkspaceUserRepository;
import com.bytechef.platform.user.service.UserService;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authorization.method.AuthorizationManagerBeforeMethodInterceptor;
import org.springframework.security.authorization.method.PreAuthorizeAuthorizationManager;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class WorkspaceServiceTest {

    private static final long WORKSPACE_ID = 5L;

    @Mock
    private PermissionService permissionService;

    @Mock
    private UserService userService;

    @Mock
    private WorkspaceRepository workspaceRepository;

    @Mock
    private WorkspaceUserRepository workspaceUserRepository;

    @InjectMocks
    private WorkspaceServiceImpl workspaceService;

    @Test
    void testDeleteOfAnEmptyWorkspaceDeletesTheWorkspace() {
        when(workspaceUserRepository.findAllByWorkspaceId(WORKSPACE_ID)).thenReturn(List.of());

        workspaceService.delete(WORKSPACE_ID);

        verify(workspaceRepository).deleteById(WORKSPACE_ID);
    }

    @Nested
    class SecuredOperations {

        private static final String BODY_REACHED = "body reached";

        private final PermissionService expressionPermissionService = mock(PermissionService.class);

        private WorkspaceService securedWorkspaceService;

        @BeforeEach
        void beforeEach() {
            AutomationMethodSecurityExpressionHandler expressionHandler =
                new AutomationMethodSecurityExpressionHandler(expressionPermissionService);

            expressionHandler.setPermissionEvaluator(new AutomationPermissionEvaluator(expressionPermissionService));

            PreAuthorizeAuthorizationManager preAuthorizeAuthorizationManager = new PreAuthorizeAuthorizationManager();

            preAuthorizeAuthorizationManager.setExpressionHandler(expressionHandler);

            ProxyFactory proxyFactory = new ProxyFactory(workspaceService);

            proxyFactory.addAdvisor(
                AuthorizationManagerBeforeMethodInterceptor.preAuthorize(preAuthorizeAuthorizationManager));

            securedWorkspaceService = (WorkspaceService) proxyFactory.getProxy();

            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();

            securityContext.setAuthentication(
                new UsernamePasswordAuthenticationToken("alice", "credentials", List.of()));

            SecurityContextHolder.setContext(securityContext);
        }

        @AfterEach
        void afterEach() {
            SecurityContextHolder.clearContext();
        }

        @Test
        void testCreateIsDeniedToANonTenantAdmin() {
            assertThatThrownBy(() -> securedWorkspaceService.create(new Workspace()))
                .isInstanceOf(AccessDeniedException.class);

            verify(expressionPermissionService).isTenantAdmin();
            verifyNoMoreInteractions(expressionPermissionService);
            verifyNoInteractions(workspaceRepository);
        }

        @Test
        void testCreateIsAllowedToATenantAdmin() {
            when(expressionPermissionService.isTenantAdmin()).thenReturn(true);
            when(workspaceRepository.save(any(Workspace.class))).thenThrow(new IllegalStateException(BODY_REACHED));

            assertThatThrownBy(() -> securedWorkspaceService.create(new Workspace()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(BODY_REACHED);

            verify(expressionPermissionService).isTenantAdmin();
            verifyNoMoreInteractions(expressionPermissionService);
        }

        @Test
        void testDeleteIsDeniedToANonTenantAdmin() {
            assertThatThrownBy(() -> securedWorkspaceService.delete(WORKSPACE_ID))
                .isInstanceOf(AccessDeniedException.class);

            verify(expressionPermissionService).isTenantAdmin();
            verifyNoMoreInteractions(expressionPermissionService);
            verifyNoInteractions(workspaceUserRepository);
        }

        @Test
        void testDeleteIsAllowedToATenantAdmin() {
            when(expressionPermissionService.isTenantAdmin()).thenReturn(true);
            when(workspaceUserRepository.findAllByWorkspaceId(WORKSPACE_ID))
                .thenThrow(new IllegalStateException(BODY_REACHED));

            assertThatThrownBy(() -> securedWorkspaceService.delete(WORKSPACE_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(BODY_REACHED);

            verify(expressionPermissionService).isTenantAdmin();
            verifyNoMoreInteractions(expressionPermissionService);
        }

        @Test
        void testGetWorkspaceIsDeniedWithoutTheWorkspaceViewScope() {
            assertThatThrownBy(() -> securedWorkspaceService.getWorkspace(WORKSPACE_ID))
                .isInstanceOf(AccessDeniedException.class);

            verify(expressionPermissionService).hasResourceScope(WORKSPACE_ID, "Workspace", "WORKSPACE_VIEW");
            verifyNoMoreInteractions(expressionPermissionService);
            verifyNoInteractions(workspaceRepository);
        }

        @Test
        void testGetWorkspaceIsAllowedWithTheWorkspaceViewScope() {
            when(expressionPermissionService.hasResourceScope(WORKSPACE_ID, "Workspace", "WORKSPACE_VIEW"))
                .thenReturn(true);
            when(workspaceRepository.findById(WORKSPACE_ID)).thenThrow(new IllegalStateException(BODY_REACHED));

            assertThatThrownBy(() -> securedWorkspaceService.getWorkspace(WORKSPACE_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(BODY_REACHED);

            verify(expressionPermissionService).hasResourceScope(WORKSPACE_ID, "Workspace", "WORKSPACE_VIEW");
            verifyNoMoreInteractions(expressionPermissionService);
        }

        @Test
        void testUpdateIsDeniedWithoutTheWorkspaceManageScope() {
            assertThatThrownBy(() -> securedWorkspaceService.update(workspace()))
                .isInstanceOf(AccessDeniedException.class);

            verify(expressionPermissionService).hasResourceScope(WORKSPACE_ID, "Workspace", "WORKSPACE_MANAGE");
            verifyNoMoreInteractions(expressionPermissionService);
            verifyNoInteractions(workspaceRepository);
        }

        @Test
        void testUpdateIsAllowedWithTheWorkspaceManageScope() {
            when(expressionPermissionService.hasResourceScope(WORKSPACE_ID, "Workspace", "WORKSPACE_MANAGE"))
                .thenReturn(true);
            when(workspaceRepository.findById(WORKSPACE_ID)).thenThrow(new IllegalStateException(BODY_REACHED));

            assertThatThrownBy(() -> securedWorkspaceService.update(workspace()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(BODY_REACHED);

            verify(expressionPermissionService).hasResourceScope(WORKSPACE_ID, "Workspace", "WORKSPACE_MANAGE");
            verifyNoMoreInteractions(expressionPermissionService);
        }

        private static Workspace workspace() {
            Workspace workspace = new Workspace();

            workspace.setId(WORKSPACE_ID);

            return workspace;
        }
    }
}
