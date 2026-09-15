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

import com.bytechef.automation.configuration.security.AutomationMethodSecurityExpressionHandler;
import com.bytechef.automation.configuration.security.AutomationPermissionEvaluator;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.platform.mcp.service.McpServerServiceImpl;
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
 * Evaluates the real {@code @PreAuthorize} expressions on {@link McpServerServiceImpl} through the real
 * {@link AutomationMethodSecurityExpressionHandler} backed by the real {@link AutomationPermissionEvaluator}, asserting
 * each guard in both directions and the exact check that reaches {@link PermissionService}.
 *
 * @author Ivica Cardic
 */
class McpServerServiceAuthorizationTest {

    private static final long MCP_SERVER_ID = 11L;
    private static final String MCP_SERVER_TYPE = "McpServer";

    @Test
    void testGetMcpServerDeniesWhenTheMcpViewScopeIsRefused() throws Exception {
        assertResourceGuard(getMcpServerMethod(), new Object[] {
            MCP_SERVER_ID
        }, "MCP_VIEW", false);
    }

    @Test
    void testGetMcpServerAllowsWhenTheMcpViewScopeIsGranted() throws Exception {
        assertResourceGuard(getMcpServerMethod(), new Object[] {
            MCP_SERVER_ID
        }, "MCP_VIEW", true);
    }

    @Test
    void testUpdateDeniesWhenTheMcpEditScopeIsRefused() throws Exception {
        assertResourceGuard(updateMethod(), new Object[] {
            MCP_SERVER_ID, "name", Boolean.TRUE
        }, "MCP_EDIT", false);
    }

    @Test
    void testUpdateAllowsWhenTheMcpEditScopeIsGranted() throws Exception {
        assertResourceGuard(updateMethod(), new Object[] {
            MCP_SERVER_ID, "name", Boolean.TRUE
        }, "MCP_EDIT", true);
    }

    private static void assertResourceGuard(
        Method method, Object[] arguments, String expectedScope, boolean granted) {

        PermissionService permissionService = mock(PermissionService.class);

        when(permissionService.hasResourceScope(MCP_SERVER_ID, MCP_SERVER_TYPE, expectedScope)).thenReturn(granted);

        assertThat(evaluateGuard(permissionService, method, arguments))
            .as("%s must %s when hasResourceScope(%s, '%s', '%s') returns %s", method.getName(),
                granted ? "allow" : "deny", MCP_SERVER_ID, MCP_SERVER_TYPE, expectedScope, granted)
            .isEqualTo(granted);

        verify(permissionService).hasResourceScope(MCP_SERVER_ID, MCP_SERVER_TYPE, expectedScope);
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

    private static Method getMcpServerMethod() throws NoSuchMethodException {
        return McpServerServiceImpl.class.getMethod("getMcpServer", long.class);
    }

    private static Method updateMethod() throws NoSuchMethodException {
        return McpServerServiceImpl.class.getMethod("update", long.class, String.class, Boolean.class);
    }
}
