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

package com.bytechef.automation.configuration.facade;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.security.AutomationMethodSecurityExpressionHandler;
import com.bytechef.automation.configuration.security.AutomationPermissionEvaluator;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.automation.configuration.service.WorkspaceApiKeyService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.security.domain.ApiKey;
import com.bytechef.platform.security.facade.ApiKeyFacade;
import com.bytechef.platform.security.service.ApiKeyService;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
 * Calls {@link WorkspaceApiKeyFacadeImpl} through the real Spring method-security interceptor, backed by the real
 * {@link AutomationMethodSecurityExpressionHandler} and {@link AutomationPermissionEvaluator}, asserting each guard in
 * both directions and the exact check that reaches {@link PermissionService}.
 *
 * @author Ivica Cardic
 */
class WorkspaceApiKeyFacadeTest {

    private static final long API_KEY_ID = 7L;
    private static final String BODY_REACHED = "body reached";
    private static final long ENVIRONMENT_ID = 2L;
    private static final long WORKSPACE_ID = 42L;

    @BeforeEach
    void beforeEach() {
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();

        securityContext.setAuthentication(
            new UsernamePasswordAuthenticationToken("member", null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));

        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testCreateDeniesWhenTheApiKeyCreateScopeIsRefusedOnTheWorkspace() {
        assertResourceGuard(
            facade -> facade.create(WORKSPACE_ID, new ApiKey()), WORKSPACE_ID, "Workspace", "API_KEY_CREATE", false);
    }

    @Test
    void testCreateAllowsWhenTheApiKeyCreateScopeIsGrantedOnTheWorkspace() {
        assertResourceGuard(
            facade -> facade.create(WORKSPACE_ID, new ApiKey()), WORKSPACE_ID, "Workspace", "API_KEY_CREATE", true);
    }

    @Test
    void testDeleteDeniesWhenTheApiKeyDeleteScopeIsRefusedOnTheApiKey() {
        assertResourceGuard(facade -> facade.delete(API_KEY_ID), API_KEY_ID, "ApiKey", "API_KEY_DELETE", false);
    }

    @Test
    void testDeleteAllowsWhenTheApiKeyDeleteScopeIsGrantedOnTheApiKey() {
        assertResourceGuard(facade -> facade.delete(API_KEY_ID), API_KEY_ID, "ApiKey", "API_KEY_DELETE", true);
    }

    @Test
    void testGetApiKeysDeniesWhenTheApiKeyViewScopeIsRefusedInTheNamedEnvironment() {
        assertGetApiKeysGuard(false);
    }

    @Test
    void testGetApiKeysAllowsWhenTheApiKeyViewScopeIsGrantedInTheNamedEnvironment() {
        assertGetApiKeysGuard(true);
    }

    private void assertResourceGuard(
        Consumer<WorkspaceApiKeyFacade> invocation, long resourceId, String resourceType, String expectedScope,
        boolean granted) {

        PermissionService permissionService = mock(PermissionService.class);

        when(permissionService.hasResourceScope(resourceId, resourceType, expectedScope)).thenReturn(granted);

        assertInvocationOutcome(invocation, permissionService, granted);

        verify(permissionService).hasResourceScope(resourceId, resourceType, expectedScope);
        verifyNoMoreInteractions(permissionService);
    }

    private void assertGetApiKeysGuard(boolean granted) {
        PermissionService permissionService = mock(PermissionService.class);
        Environment environment = Environment.values()[(int) ENVIRONMENT_ID];

        when(permissionService.hasWorkspaceScope(WORKSPACE_ID, "API_KEY_VIEW", environment)).thenReturn(granted);

        assertInvocationOutcome(facade -> facade.getApiKeys(WORKSPACE_ID, ENVIRONMENT_ID), permissionService, granted);

        verify(permissionService).hasWorkspaceScope(WORKSPACE_ID, "API_KEY_VIEW", environment);
        verifyNoMoreInteractions(permissionService);
    }

    private void assertInvocationOutcome(
        Consumer<WorkspaceApiKeyFacade> invocation, PermissionService permissionService, boolean allowed) {

        WorkspaceApiKeyFacade workspaceApiKeyFacade = secure(
            new WorkspaceApiKeyFacadeImpl(
                bodyReachedMock(ApiKeyFacade.class), bodyReachedMock(ApiKeyService.class),
                bodyReachedMock(WorkspaceApiKeyService.class)),
            permissionService);

        if (allowed) {
            assertThatThrownBy(() -> invocation.accept(workspaceApiKeyFacade))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(BODY_REACHED);
        } else {
            assertThatThrownBy(() -> invocation.accept(workspaceApiKeyFacade))
                .isInstanceOf(AccessDeniedException.class);
        }
    }

    private static <T> T bodyReachedMock(Class<T> type) {
        return mock(type, invocation -> {
            throw new IllegalStateException(BODY_REACHED);
        });
    }

    @SuppressWarnings("unchecked")
    private static <T> T secure(T target, PermissionService permissionService) {
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
