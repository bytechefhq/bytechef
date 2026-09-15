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
import com.bytechef.platform.mcp.domain.McpComponent;
import com.bytechef.platform.mcp.facade.McpServerFacadeImpl;
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
 * Evaluates the real {@code @PreAuthorize} expressions on {@link McpServerFacadeImpl} through the real
 * {@link AutomationMethodSecurityExpressionHandler} backed by the real {@link AutomationPermissionEvaluator}, asserting
 * each guard in both directions and the exact check that reaches {@link PermissionService}.
 *
 * @author Ivica Cardic
 */
class McpServerFacadeAuthorizationTest {

    private static final long MCP_COMPONENT_ID = 13L;
    private static final String MCP_COMPONENT_TYPE = "McpComponent";
    private static final long MCP_SERVER_ID = 11L;
    private static final String MCP_SERVER_TYPE = "McpServer";

    @Test
    void testCreateDeniesWhenTheMcpEditScopeIsRefusedOnTheComponentsServer() throws Exception {
        assertResourceGuard(createMethod(), new Object[] {
            mcpComponent(), List.of()
        }, MCP_SERVER_ID, MCP_SERVER_TYPE, "MCP_EDIT", false);
    }

    @Test
    void testCreateAllowsWhenTheMcpEditScopeIsGrantedOnTheComponentsServer() throws Exception {
        assertResourceGuard(createMethod(), new Object[] {
            mcpComponent(), List.of()
        }, MCP_SERVER_ID, MCP_SERVER_TYPE, "MCP_EDIT", true);
    }

    @Test
    void testDeleteMcpComponentDeniesWhenTheMcpEditScopeIsRefused() throws Exception {
        assertResourceGuard(deleteMcpComponentMethod(), new Object[] {
            MCP_COMPONENT_ID
        }, MCP_COMPONENT_ID, MCP_COMPONENT_TYPE, "MCP_EDIT", false);
    }

    @Test
    void testDeleteMcpComponentAllowsWhenTheMcpEditScopeIsGranted() throws Exception {
        assertResourceGuard(deleteMcpComponentMethod(), new Object[] {
            MCP_COMPONENT_ID
        }, MCP_COMPONENT_ID, MCP_COMPONENT_TYPE, "MCP_EDIT", true);
    }

    @Test
    void testDeleteMcpServerDeniesWhenTheMcpDeleteScopeIsRefused() throws Exception {
        assertResourceGuard(deleteMcpServerMethod(), new Object[] {
            MCP_SERVER_ID
        }, MCP_SERVER_ID, MCP_SERVER_TYPE, "MCP_DELETE", false);
    }

    @Test
    void testDeleteMcpServerAllowsWhenTheMcpDeleteScopeIsGranted() throws Exception {
        assertResourceGuard(deleteMcpServerMethod(), new Object[] {
            MCP_SERVER_ID
        }, MCP_SERVER_ID, MCP_SERVER_TYPE, "MCP_DELETE", true);
    }

    @Test
    void testUpdateDeniesWhenTheMcpEditScopeIsRefusedOnTheComponent() throws Exception {
        assertResourceGuard(updateMethod(), new Object[] {
            mcpComponent(), List.of()
        }, MCP_COMPONENT_ID, MCP_COMPONENT_TYPE, "MCP_EDIT", false);
    }

    @Test
    void testUpdateAllowsWhenTheMcpEditScopeIsGrantedOnTheComponent() throws Exception {
        assertResourceGuard(updateMethod(), new Object[] {
            mcpComponent(), List.of()
        }, MCP_COMPONENT_ID, MCP_COMPONENT_TYPE, "MCP_EDIT", true);
    }

    @Test
    void testUpdateMcpServerTagsDeniesWhenTheMcpEditScopeIsRefused() throws Exception {
        assertResourceGuard(updateMcpServerTagsMethod(), new Object[] {
            MCP_SERVER_ID, List.of()
        }, MCP_SERVER_ID, MCP_SERVER_TYPE, "MCP_EDIT", false);
    }

    @Test
    void testUpdateMcpServerTagsAllowsWhenTheMcpEditScopeIsGranted() throws Exception {
        assertResourceGuard(updateMcpServerTagsMethod(), new Object[] {
            MCP_SERVER_ID, List.of()
        }, MCP_SERVER_ID, MCP_SERVER_TYPE, "MCP_EDIT", true);
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

    private static McpComponent mcpComponent() {
        McpComponent mcpComponent = new McpComponent("component", 1, MCP_SERVER_ID, null);

        mcpComponent.setId(MCP_COMPONENT_ID);

        return mcpComponent;
    }

    private static Method createMethod() throws NoSuchMethodException {
        return McpServerFacadeImpl.class.getMethod("create", McpComponent.class, List.class);
    }

    private static Method deleteMcpComponentMethod() throws NoSuchMethodException {
        return McpServerFacadeImpl.class.getMethod("deleteMcpComponent", long.class);
    }

    private static Method deleteMcpServerMethod() throws NoSuchMethodException {
        return McpServerFacadeImpl.class.getMethod("deleteMcpServer", long.class);
    }

    private static Method updateMethod() throws NoSuchMethodException {
        return McpServerFacadeImpl.class.getMethod("update", McpComponent.class, List.class);
    }

    private static Method updateMcpServerTagsMethod() throws NoSuchMethodException {
        return McpServerFacadeImpl.class.getMethod("updateMcpServerTags", long.class, List.class);
    }
}
