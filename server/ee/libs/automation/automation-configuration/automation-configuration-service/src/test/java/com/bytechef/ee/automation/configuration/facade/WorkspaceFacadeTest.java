/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.facade;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.automation.configuration.security.AutomationMethodSecurityExpressionHandler;
import com.bytechef.automation.configuration.security.AutomationPermissionEvaluator;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.automation.configuration.service.WorkspaceService;
import com.bytechef.ee.automation.configuration.service.WorkspaceUserService;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authorization.method.AuthorizationManagerBeforeMethodInterceptor;
import org.springframework.security.authorization.method.PreAuthorizeAuthorizationManager;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Calls {@link WorkspaceFacadeImpl#getUserWorkspaces} through Spring Security's real {@code @PreAuthorize} method
 * interceptor, backed by the real {@link AutomationMethodSecurityExpressionHandler} and
 * {@link AutomationPermissionEvaluator}. Another user's workspace memberships are readable only by a tenant admin or by
 * that user; an allowed call proves it entered the method body by reaching the {@link WorkspaceService} stub that
 * throws.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class WorkspaceFacadeTest {

    private static final String BODY_REACHED = "body reached";
    private static final long USER_ID = 999L;

    private final PermissionService permissionService = mock(PermissionService.class);
    private final WorkspaceService workspaceService = mock(WorkspaceService.class);
    private final WorkspaceUserService workspaceUserService = mock(WorkspaceUserService.class);

    private WorkspaceFacade workspaceFacade;

    @BeforeEach
    void beforeEach() {
        when(workspaceService.getWorkspaces()).thenThrow(new IllegalStateException(BODY_REACHED));

        AutomationMethodSecurityExpressionHandler expressionHandler =
            new AutomationMethodSecurityExpressionHandler(permissionService);

        expressionHandler.setPermissionEvaluator(new AutomationPermissionEvaluator(permissionService));

        PreAuthorizeAuthorizationManager preAuthorizeAuthorizationManager = new PreAuthorizeAuthorizationManager();

        preAuthorizeAuthorizationManager.setExpressionHandler(expressionHandler);

        ProxyFactory proxyFactory = new ProxyFactory(
            new WorkspaceFacadeImpl(permissionService, workspaceService, workspaceUserService));

        proxyFactory.addAdvisor(
            AuthorizationManagerBeforeMethodInterceptor.preAuthorize(preAuthorizeAuthorizationManager));

        workspaceFacade = (WorkspaceFacade) proxyFactory.getProxy();

        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();

        securityContext.setAuthentication(new UsernamePasswordAuthenticationToken("alice", "credentials", List.of()));

        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testGetUserWorkspacesIsDeniedToANonAdminReadingAnotherUser() {
        assertThatThrownBy(() -> workspaceFacade.getUserWorkspaces(USER_ID))
            .isInstanceOf(AccessDeniedException.class);

        verify(permissionService).isTenantAdmin();
        verify(permissionService).isCurrentUser(USER_ID);
        verifyNoMoreInteractions(permissionService);
        verifyNoInteractions(workspaceService, workspaceUserService);
    }

    @Test
    void testGetUserWorkspacesIsAllowedToATenantAdmin() {
        when(permissionService.isTenantAdmin()).thenReturn(true);

        assertThatThrownBy(() -> workspaceFacade.getUserWorkspaces(USER_ID))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage(BODY_REACHED);

        verify(permissionService).isTenantAdmin();
        verifyNoMoreInteractions(permissionService);
    }

    @Test
    void testGetUserWorkspacesIsAllowedToTheUserThemselves() {
        when(permissionService.isCurrentUser(USER_ID)).thenReturn(true);

        assertThatThrownBy(() -> workspaceFacade.getUserWorkspaces(USER_ID))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage(BODY_REACHED);

        verify(permissionService).isTenantAdmin();
        verify(permissionService).isCurrentUser(USER_ID);
        verifyNoMoreInteractions(permissionService);
    }
}
