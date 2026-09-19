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

package com.bytechef.automation.configuration.web.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.security.AutomationMethodSecurityExpressionHandler;
import com.bytechef.automation.configuration.security.AutomationPermissionEvaluator;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.platform.configuration.domain.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.util.SimpleMethodInvocation;

/**
 * Evaluates the real {@code @PreAuthorize} expression on {@link WorkspaceApiKeyGraphQlController#createWorkspaceApiKey}
 * through the real {@link AutomationMethodSecurityExpressionHandler} backed by the real
 * {@link AutomationPermissionEvaluator}, asserting the guard in both directions and the exact check that reaches
 * {@link PermissionService}.
 *
 * @author Ivica Cardic
 */
class WorkspaceApiKeyGraphQlControllerAuthorizationTest {

    private static final long ENVIRONMENT_ID = 2L;
    private static final long WORKSPACE_ID = 42L;

    // The first guard evaluation in a test JVM loads, and under coverage instruments, the Spring Security and SpEL
    // class graph. That one-time cost belongs under the longer @BeforeAll limit, not the per-test one.
    @BeforeAll
    static void warmUpGuardEvaluation() throws NoSuchMethodException {
        evaluateGuard(mock(PermissionService.class), createWorkspaceApiKeyMethod(), new Object[] {
            WORKSPACE_ID, "name", ENVIRONMENT_ID
        });
    }

    @Test
    void testCreateWorkspaceApiKeyDeniesWhenTheApiKeyCreateScopeIsRefusedInTheNamedEnvironment() throws Exception {
        assertEnvironmentGuard(false);
    }

    @Test
    void testCreateWorkspaceApiKeyAllowsWhenTheApiKeyCreateScopeIsGrantedInTheNamedEnvironment() throws Exception {
        assertEnvironmentGuard(true);
    }

    @Test
    void testCreateWorkspaceApiKeyDeniesWhenTheApiKeyCreateScopeIsRefusedWithoutAnEnvironment() throws Exception {
        assertEnvironmentUnawareGuard(false);
    }

    @Test
    void testCreateWorkspaceApiKeyAllowsWhenTheApiKeyCreateScopeIsGrantedWithoutAnEnvironment() throws Exception {
        assertEnvironmentUnawareGuard(true);
    }

    private void assertEnvironmentGuard(boolean granted) throws Exception {
        PermissionService permissionService = mock(PermissionService.class);
        Environment environment = Environment.values()[(int) ENVIRONMENT_ID];

        when(permissionService.hasWorkspaceScope(WORKSPACE_ID, "API_KEY_CREATE", environment)).thenReturn(granted);

        assertThat(evaluateGuard(permissionService, createWorkspaceApiKeyMethod(), new Object[] {
            WORKSPACE_ID, "name", ENVIRONMENT_ID
        }))
            .isEqualTo(granted);

        verify(permissionService).hasWorkspaceScope(WORKSPACE_ID, "API_KEY_CREATE", environment);
        verifyNoMoreInteractions(permissionService);
    }

    private void assertEnvironmentUnawareGuard(boolean granted) throws Exception {
        PermissionService permissionService = mock(PermissionService.class);

        when(permissionService.hasWorkspaceScope(WORKSPACE_ID, "API_KEY_CREATE")).thenReturn(granted);

        assertThat(evaluateGuard(permissionService, createWorkspaceApiKeyMethod(), new Object[] {
            WORKSPACE_ID, "name", null
        }))
            .isEqualTo(granted);

        verify(permissionService).hasWorkspaceScope(WORKSPACE_ID, "API_KEY_CREATE");
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

    private static Method createWorkspaceApiKeyMethod() throws NoSuchMethodException {
        return WorkspaceApiKeyGraphQlController.class.getMethod(
            "createWorkspaceApiKey", long.class, String.class, Long.class);
    }
}
