/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.configuration.facade;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.security.AutomationMethodSecurityExpressionHandler;
import com.bytechef.automation.configuration.security.AutomationPermissionEvaluator;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.platform.configuration.dto.GitConfigurationDTO;
import com.bytechef.platform.configuration.domain.Property.Scope;
import com.bytechef.platform.configuration.service.PropertyService;
import java.util.List;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
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
 * Calls {@link GitConfigurationFacadeImpl} through Spring Security's real {@code @PreAuthorize} method interceptor,
 * backed by the real {@link AutomationMethodSecurityExpressionHandler} and {@link AutomationPermissionEvaluator},
 * asserting each guard in both directions and the exact check that reaches {@link PermissionService}. An allowed call
 * proves it entered the method body by reaching the {@link PropertyService} stub that throws.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class GitConfigurationFacadeTest {

    private static final String BODY_REACHED = "body reached";
    private static final long WORKSPACE_ID = 42L;

    private final PermissionService permissionService = mock(PermissionService.class);
    private final PropertyService propertyService = mock(PropertyService.class);

    private GitConfigurationFacade gitConfigurationFacade;

    @BeforeEach
    void beforeEach() {
        when(propertyService.fetchProperty(anyString(), eq(Scope.WORKSPACE), anyLong()))
            .thenThrow(new IllegalStateException(BODY_REACHED));

        gitConfigurationFacade = secure(new GitConfigurationFacadeImpl(propertyService));

        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();

        securityContext.setAuthentication(new UsernamePasswordAuthenticationToken("alice", "credentials", List.of()));

        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testFetchGitConfigurationDeniesWhenTheWorkspaceManageScopeIsRefused() {
        assertWorkspaceManageGuard(() -> gitConfigurationFacade.fetchGitConfiguration(WORKSPACE_ID), false);
    }

    @Test
    void testFetchGitConfigurationAllowsWhenTheWorkspaceManageScopeIsGranted() {
        assertWorkspaceManageGuard(() -> gitConfigurationFacade.fetchGitConfiguration(WORKSPACE_ID), true);
    }

    @Test
    void testSaveDeniesWhenTheWorkspaceManageScopeIsRefused() {
        assertWorkspaceManageGuard(
            () -> gitConfigurationFacade.save(
                new GitConfigurationDTO("https://example.com/repository.git", "username", "password"), WORKSPACE_ID),
            false);
    }

    @Test
    void testSaveAllowsWhenTheWorkspaceManageScopeIsGranted() {
        assertWorkspaceManageGuard(
            () -> gitConfigurationFacade.save(
                new GitConfigurationDTO("https://example.com/repository.git", "username", "password"), WORKSPACE_ID),
            true);
    }

    @Test
    void testGetGitConfigurationCarriesNoScopeGuard() {
        assertThatThrownBy(() -> gitConfigurationFacade.getGitConfiguration(WORKSPACE_ID))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage(BODY_REACHED);

        verifyNoInteractions(permissionService);
    }

    private void assertWorkspaceManageGuard(ThrowingCallable callable, boolean granted) {
        when(permissionService.hasResourceScope(WORKSPACE_ID, "Workspace", "WORKSPACE_MANAGE")).thenReturn(granted);

        if (granted) {
            assertThatThrownBy(callable)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(BODY_REACHED);
        } else {
            assertThatThrownBy(callable).isInstanceOf(AccessDeniedException.class);

            verifyNoInteractions(propertyService);
        }

        verify(permissionService).hasResourceScope(WORKSPACE_ID, "Workspace", "WORKSPACE_MANAGE");
        verifyNoMoreInteractions(permissionService);
    }

    @SuppressWarnings("unchecked")
    private <T> T secure(T target) {
        AutomationMethodSecurityExpressionHandler expressionHandler =
            new AutomationMethodSecurityExpressionHandler(permissionService);

        expressionHandler.setPermissionEvaluator(new AutomationPermissionEvaluator(permissionService));

        PreAuthorizeAuthorizationManager preAuthorizeAuthorizationManager = new PreAuthorizeAuthorizationManager();

        preAuthorizeAuthorizationManager.setExpressionHandler(expressionHandler);

        ProxyFactory proxyFactory = new ProxyFactory(target);

        proxyFactory.addAdvisor(
            AuthorizationManagerBeforeMethodInterceptor.preAuthorize(preAuthorizeAuthorizationManager));

        return (T) proxyFactory.getProxy();
    }
}
