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

package com.bytechef.automation.configuration.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.facade.WorkspaceApiKeyFacadeImpl;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.security.domain.ApiKey;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.util.SimpleMethodInvocation;

/**
 * Evaluates the real {@code @PreAuthorize} expressions on {@link WorkspaceApiKeyFacadeImpl} through the real
 * {@link AutomationMethodSecurityExpressionHandler} backed by the real {@link AutomationPermissionEvaluator}, asserting
 * each guard in both directions and the exact check that reaches {@link PermissionService}.
 *
 * @author Ivica Cardic
 */
class WorkspaceApiKeyFacadeAuthorizationTest {

    private static final long API_KEY_ID = 7L;
    private static final long ENVIRONMENT_ID = 2L;
    private static final long WORKSPACE_ID = 42L;

    @Test
    void testCreateDeniesWhenTheApiKeyCreateScopeIsRefusedOnTheWorkspace() throws Exception {
        assertResourceGuard(createMethod(), new Object[] {
            WORKSPACE_ID, new ApiKey()
        }, WORKSPACE_ID, "Workspace", "API_KEY_CREATE", false);
    }

    @Test
    void testCreateAllowsWhenTheApiKeyCreateScopeIsGrantedOnTheWorkspace() throws Exception {
        assertResourceGuard(createMethod(), new Object[] {
            WORKSPACE_ID, new ApiKey()
        }, WORKSPACE_ID, "Workspace", "API_KEY_CREATE", true);
    }

    @Test
    void testDeleteDeniesWhenTheApiKeyDeleteScopeIsRefusedOnTheApiKey() throws Exception {
        assertResourceGuard(deleteMethod(), new Object[] {
            API_KEY_ID
        }, API_KEY_ID, "ApiKey", "API_KEY_DELETE", false);
    }

    @Test
    void testDeleteAllowsWhenTheApiKeyDeleteScopeIsGrantedOnTheApiKey() throws Exception {
        assertResourceGuard(deleteMethod(), new Object[] {
            API_KEY_ID
        }, API_KEY_ID, "ApiKey", "API_KEY_DELETE", true);
    }

    @Test
    void testGetApiKeysDeniesWhenTheApiKeyViewScopeIsRefusedInTheNamedEnvironment() throws Exception {
        assertGetApiKeysGuard(false);
    }

    @Test
    void testGetApiKeysAllowsWhenTheApiKeyViewScopeIsGrantedInTheNamedEnvironment() throws Exception {
        assertGetApiKeysGuard(true);
    }

    private void assertResourceGuard(
        Method method, Object[] arguments, long resourceId, String resourceType, String expectedScope,
        boolean granted) {

        PermissionService permissionService = mock(PermissionService.class);

        when(permissionService.hasResourceScope(resourceId, resourceType, expectedScope)).thenReturn(granted);

        assertThat(evaluateGuard(permissionService, method, arguments))
            .as("%s must %s when hasResourceScope(%s, '%s', '%s') returns %s", method.getName(),
                granted ? "allow" : "deny", resourceId, resourceType, expectedScope, granted)
            .isEqualTo(granted);

        verify(permissionService).hasResourceScope(resourceId, resourceType, expectedScope);
        verifyNoMoreInteractions(permissionService);
    }

    private void assertGetApiKeysGuard(boolean granted) throws Exception {
        PermissionService permissionService = mock(PermissionService.class);
        Environment environment = Environment.values()[(int) ENVIRONMENT_ID];

        when(permissionService.hasWorkspaceScope(WORKSPACE_ID, "API_KEY_VIEW", environment)).thenReturn(granted);

        Method method = WorkspaceApiKeyFacadeImpl.class.getMethod("getApiKeys", long.class, long.class);

        assertThat(evaluateGuard(permissionService, method, new Object[] {
            WORKSPACE_ID, ENVIRONMENT_ID
        }))
            .isEqualTo(granted);

        verify(permissionService).hasWorkspaceScope(WORKSPACE_ID, "API_KEY_VIEW", environment);
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

    private static Method createMethod() throws NoSuchMethodException {
        return WorkspaceApiKeyFacadeImpl.class.getMethod("create", long.class, ApiKey.class);
    }

    private static Method deleteMethod() throws NoSuchMethodException {
        return WorkspaceApiKeyFacadeImpl.class.getMethod("delete", long.class);
    }
}
