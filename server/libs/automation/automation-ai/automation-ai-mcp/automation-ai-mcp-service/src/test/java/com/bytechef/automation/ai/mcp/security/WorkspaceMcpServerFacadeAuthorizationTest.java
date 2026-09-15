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

package com.bytechef.automation.ai.mcp.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.ai.mcp.facade.WorkspaceMcpServerFacadeImpl;
import com.bytechef.automation.configuration.security.AutomationMethodSecurityExpressionHandler;
import com.bytechef.automation.configuration.security.AutomationPermissionEvaluator;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
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
 * Evaluates the real {@code @PreAuthorize} expressions on {@link WorkspaceMcpServerFacadeImpl} through the real
 * {@link AutomationMethodSecurityExpressionHandler} backed by the real {@link AutomationPermissionEvaluator}, asserting
 * each guard in both directions and the exact check that reaches {@link PermissionService}.
 *
 * @author Ivica Cardic
 */
class WorkspaceMcpServerFacadeAuthorizationTest {

    private static final Environment ENVIRONMENT = Environment.STAGING;
    private static final long MCP_SERVER_ID = 11L;
    private static final long WORKSPACE_ID = 42L;

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
}
