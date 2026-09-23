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
import com.bytechef.platform.mcp.domain.McpTool;
import com.bytechef.platform.mcp.service.McpToolServiceImpl;
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
 * Evaluates the real {@code @PreAuthorize} expression on {@link McpToolServiceImpl#delete(McpTool)} through the real
 * {@link AutomationMethodSecurityExpressionHandler} backed by the real {@link AutomationPermissionEvaluator}.
 *
 * @author Ivica Cardic
 */
class McpToolServiceAuthorizationTest {

    private static final long MCP_TOOL_ID = 21L;
    private static final String MCP_TOOL_TYPE = "McpTool";

    @Test
    void testDeleteDeniesWhenTheMcpEditAndMcpDeleteScopesAreBothRefused() throws Exception {
        PermissionService permissionService = mock(PermissionService.class);

        when(permissionService.hasResourceScope(MCP_TOOL_ID, MCP_TOOL_TYPE, "MCP_EDIT")).thenReturn(false);
        when(permissionService.hasResourceScope(MCP_TOOL_ID, MCP_TOOL_TYPE, "MCP_DELETE")).thenReturn(false);

        assertThat(evaluateDeleteGuard(permissionService)).isFalse();

        verify(permissionService).hasResourceScope(MCP_TOOL_ID, MCP_TOOL_TYPE, "MCP_EDIT");
        verify(permissionService).hasResourceScope(MCP_TOOL_ID, MCP_TOOL_TYPE, "MCP_DELETE");
        verifyNoMoreInteractions(permissionService);
    }

    @Test
    void testDeleteAllowsWhenTheMcpEditScopeIsGranted() throws Exception {
        PermissionService permissionService = mock(PermissionService.class);

        when(permissionService.hasResourceScope(MCP_TOOL_ID, MCP_TOOL_TYPE, "MCP_EDIT")).thenReturn(true);

        assertThat(evaluateDeleteGuard(permissionService)).isTrue();

        verify(permissionService).hasResourceScope(MCP_TOOL_ID, MCP_TOOL_TYPE, "MCP_EDIT");
        verifyNoMoreInteractions(permissionService);
    }

    @Test
    void testDeleteAllowsWhenOnlyTheMcpDeleteScopeIsGranted() throws Exception {
        PermissionService permissionService = mock(PermissionService.class);

        when(permissionService.hasResourceScope(MCP_TOOL_ID, MCP_TOOL_TYPE, "MCP_EDIT")).thenReturn(false);
        when(permissionService.hasResourceScope(MCP_TOOL_ID, MCP_TOOL_TYPE, "MCP_DELETE")).thenReturn(true);

        assertThat(evaluateDeleteGuard(permissionService)).isTrue();

        verify(permissionService).hasResourceScope(MCP_TOOL_ID, MCP_TOOL_TYPE, "MCP_EDIT");
        verify(permissionService).hasResourceScope(MCP_TOOL_ID, MCP_TOOL_TYPE, "MCP_DELETE");
        verifyNoMoreInteractions(permissionService);
    }

    // The expression parsed here is read straight off our own @PreAuthorize annotation in this repository's compiled
    // bytecode, not attacker-influenced input.
    @SuppressFBWarnings(
        value = "SPEL_INJECTION",
        justification = "The expression is this repository's own @PreAuthorize value, not untrusted input.")
    private static boolean evaluateDeleteGuard(PermissionService permissionService) throws NoSuchMethodException {
        Method method = McpToolServiceImpl.class.getMethod("delete", McpTool.class);

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize)
            .as("delete must carry a @PreAuthorize guard")
            .isNotNull();

        AutomationMethodSecurityExpressionHandler expressionHandler =
            new AutomationMethodSecurityExpressionHandler(permissionService);

        expressionHandler.setPermissionEvaluator(new AutomationPermissionEvaluator(permissionService));

        Authentication authentication = new UsernamePasswordAuthenticationToken("alice", "credentials", List.of());

        McpTool mcpTool = new McpTool();

        mcpTool.setId(MCP_TOOL_ID);

        SimpleMethodInvocation methodInvocation = new SimpleMethodInvocation(new Object(), method, mcpTool);

        EvaluationContext evaluationContext =
            expressionHandler.createEvaluationContext(() -> authentication, methodInvocation);

        Expression expression = expressionHandler.getExpressionParser()
            .parseExpression(preAuthorize.value());

        return Boolean.TRUE.equals(expression.getValue(evaluationContext, Boolean.class));
    }
}
