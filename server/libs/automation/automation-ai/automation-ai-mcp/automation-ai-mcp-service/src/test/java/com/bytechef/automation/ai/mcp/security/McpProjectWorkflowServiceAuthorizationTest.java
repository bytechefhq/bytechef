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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.ai.mcp.domain.McpProjectWorkflow;
import com.bytechef.automation.ai.mcp.service.McpProjectWorkflowServiceImpl;
import com.bytechef.automation.configuration.security.AutomationMethodSecurityExpressionHandler;
import com.bytechef.automation.configuration.security.AutomationPermissionEvaluator;
import com.bytechef.automation.configuration.service.PermissionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.util.SimpleMethodInvocation;

/**
 * Evaluates the real {@code @PreAuthorize} expressions on {@link McpProjectWorkflowServiceImpl} through the real
 * {@link AutomationMethodSecurityExpressionHandler} backed by the real {@link AutomationPermissionEvaluator}, asserting
 * each guard in both directions and the exact checks that reach {@link PermissionService}. The two {@code update}
 * guards are conjunctions: each is refused when either operand is refused and allowed only when both are granted.
 *
 * @author Ivica Cardic
 */
class McpProjectWorkflowServiceAuthorizationTest {

    private static final long MCP_PROJECT_ID = 23L;
    private static final String MCP_PROJECT_TYPE = "McpProject";
    private static final long MCP_PROJECT_WORKFLOW_ID = 21L;
    private static final String MCP_PROJECT_WORKFLOW_TYPE = "McpProjectWorkflow";
    private static final long PROJECT_DEPLOYMENT_WORKFLOW_ID = 5L;

    @Test
    void testCreateDeniesWhenTheMcpEditScopeIsRefusedOnTheWorkflowsProject() throws Exception {
        assertResourceGuard(createMethod(), new Object[] {
            mcpProjectWorkflow()
        }, MCP_PROJECT_ID, MCP_PROJECT_TYPE, "MCP_EDIT", false);
    }

    @Test
    void testCreateAllowsWhenTheMcpEditScopeIsGrantedOnTheWorkflowsProject() throws Exception {
        assertResourceGuard(createMethod(), new Object[] {
            mcpProjectWorkflow()
        }, MCP_PROJECT_ID, MCP_PROJECT_TYPE, "MCP_EDIT", true);
    }

    @Test
    void testCreateByIdsDeniesWhenTheMcpEditScopeIsRefusedOnTheProject() throws Exception {
        assertResourceGuard(createByIdsMethod(), new Object[] {
            MCP_PROJECT_ID, PROJECT_DEPLOYMENT_WORKFLOW_ID
        }, MCP_PROJECT_ID, MCP_PROJECT_TYPE, "MCP_EDIT", false);
    }

    @Test
    void testCreateByIdsAllowsWhenTheMcpEditScopeIsGrantedOnTheProject() throws Exception {
        assertResourceGuard(createByIdsMethod(), new Object[] {
            MCP_PROJECT_ID, PROJECT_DEPLOYMENT_WORKFLOW_ID
        }, MCP_PROJECT_ID, MCP_PROJECT_TYPE, "MCP_EDIT", true);
    }

    @Test
    void testDeleteDeniesWhenTheMcpEditScopeIsRefused() throws Exception {
        assertResourceGuard(deleteMethod(), new Object[] {
            MCP_PROJECT_WORKFLOW_ID
        }, MCP_PROJECT_WORKFLOW_ID, MCP_PROJECT_WORKFLOW_TYPE, "MCP_EDIT", false);
    }

    @Test
    void testDeleteAllowsWhenTheMcpEditScopeIsGranted() throws Exception {
        assertResourceGuard(deleteMethod(), new Object[] {
            MCP_PROJECT_WORKFLOW_ID
        }, MCP_PROJECT_WORKFLOW_ID, MCP_PROJECT_WORKFLOW_TYPE, "MCP_EDIT", true);
    }

    @Test
    void testFetchMcpProjectWorkflowDeniesWhenTheMcpViewScopeIsRefused() throws Exception {
        assertResourceGuard(fetchMcpProjectWorkflowMethod(), new Object[] {
            MCP_PROJECT_WORKFLOW_ID
        }, MCP_PROJECT_WORKFLOW_ID, MCP_PROJECT_WORKFLOW_TYPE, "MCP_VIEW", false);
    }

    @Test
    void testFetchMcpProjectWorkflowAllowsWhenTheMcpViewScopeIsGranted() throws Exception {
        assertResourceGuard(fetchMcpProjectWorkflowMethod(), new Object[] {
            MCP_PROJECT_WORKFLOW_ID
        }, MCP_PROJECT_WORKFLOW_ID, MCP_PROJECT_WORKFLOW_TYPE, "MCP_VIEW", true);
    }

    @Test
    void testUpdateParametersDeniesWhenTheMcpEditScopeIsRefused() throws Exception {
        assertResourceGuard(updateParametersMethod(), new Object[] {
            MCP_PROJECT_WORKFLOW_ID, Map.of()
        }, MCP_PROJECT_WORKFLOW_ID, MCP_PROJECT_WORKFLOW_TYPE, "MCP_EDIT", false);
    }

    @Test
    void testUpdateParametersAllowsWhenTheMcpEditScopeIsGranted() throws Exception {
        assertResourceGuard(updateParametersMethod(), new Object[] {
            MCP_PROJECT_WORKFLOW_ID, Map.of()
        }, MCP_PROJECT_WORKFLOW_ID, MCP_PROJECT_WORKFLOW_TYPE, "MCP_EDIT", true);
    }

    @Test
    void testUpdateDeniesWhenTheMcpEditScopeIsRefusedOnTheWorkflow() throws Exception {
        assertCompoundGuard(updateMethod(), new Object[] {
            mcpProjectWorkflow()
        }, false, true);
    }

    @Test
    void testUpdateDeniesWhenTheMcpEditScopeIsRefusedOnTheProject() throws Exception {
        assertCompoundGuard(updateMethod(), new Object[] {
            mcpProjectWorkflow()
        }, true, false);
    }

    @Test
    void testUpdateAllowsWhenTheMcpEditScopeIsGrantedOnBothTheWorkflowAndTheProject() throws Exception {
        assertCompoundGuard(updateMethod(), new Object[] {
            mcpProjectWorkflow()
        }, true, true);
    }

    @Test
    void testUpdateByIdsDeniesWhenTheMcpEditScopeIsRefusedOnTheWorkflow() throws Exception {
        assertCompoundGuard(updateByIdsMethod(), new Object[] {
            MCP_PROJECT_WORKFLOW_ID, MCP_PROJECT_ID, PROJECT_DEPLOYMENT_WORKFLOW_ID
        }, false, true);
    }

    @Test
    void testUpdateByIdsDeniesWhenTheMcpEditScopeIsRefusedOnTheTargetProject() throws Exception {
        assertCompoundGuard(updateByIdsMethod(), new Object[] {
            MCP_PROJECT_WORKFLOW_ID, MCP_PROJECT_ID, PROJECT_DEPLOYMENT_WORKFLOW_ID
        }, true, false);
    }

    @Test
    void testUpdateByIdsAllowsWhenTheMcpEditScopeIsGrantedOnBothTheWorkflowAndTheTargetProject() throws Exception {
        assertCompoundGuard(updateByIdsMethod(), new Object[] {
            MCP_PROJECT_WORKFLOW_ID, MCP_PROJECT_ID, PROJECT_DEPLOYMENT_WORKFLOW_ID
        }, true, true);
    }

    @Test
    void testUpdateByIdsWithoutAProjectDeniesWhenTheMcpEditScopeIsRefusedOnTheWorkflow() throws Exception {
        assertResourceGuard(updateByIdsMethod(), new Object[] {
            MCP_PROJECT_WORKFLOW_ID, null, PROJECT_DEPLOYMENT_WORKFLOW_ID
        }, MCP_PROJECT_WORKFLOW_ID, MCP_PROJECT_WORKFLOW_TYPE, "MCP_EDIT", false);
    }

    @Test
    void testUpdateByIdsWithoutAProjectAllowsWhenTheMcpEditScopeIsGrantedOnTheWorkflow() throws Exception {
        assertResourceGuard(updateByIdsMethod(), new Object[] {
            MCP_PROJECT_WORKFLOW_ID, null, PROJECT_DEPLOYMENT_WORKFLOW_ID
        }, MCP_PROJECT_WORKFLOW_ID, MCP_PROJECT_WORKFLOW_TYPE, "MCP_EDIT", true);
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

    // SpEL's 'and' short-circuits, so the project check is reached only once the workflow check is granted.
    private static void assertCompoundGuard(
        Method method, Object[] arguments, boolean workflowGranted, boolean projectGranted) {

        PermissionService permissionService = mock(PermissionService.class);

        when(permissionService.hasResourceScope(MCP_PROJECT_WORKFLOW_ID, MCP_PROJECT_WORKFLOW_TYPE, "MCP_EDIT"))
            .thenReturn(workflowGranted);
        when(permissionService.hasResourceScope(MCP_PROJECT_ID, MCP_PROJECT_TYPE, "MCP_EDIT"))
            .thenReturn(projectGranted);

        boolean expected = workflowGranted && projectGranted;

        assertThat(evaluateGuard(permissionService, method, arguments))
            .as("%s must %s when the workflow check returns %s and the project check returns %s", method.getName(),
                expected ? "allow" : "deny", workflowGranted, projectGranted)
            .isEqualTo(expected);

        verify(permissionService).hasResourceScope(MCP_PROJECT_WORKFLOW_ID, MCP_PROJECT_WORKFLOW_TYPE, "MCP_EDIT");
        verify(permissionService, times(workflowGranted ? 1 : 0))
            .hasResourceScope(MCP_PROJECT_ID, MCP_PROJECT_TYPE, "MCP_EDIT");
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

    private static McpProjectWorkflow mcpProjectWorkflow() {
        McpProjectWorkflow mcpProjectWorkflow =
            new McpProjectWorkflow(MCP_PROJECT_ID, PROJECT_DEPLOYMENT_WORKFLOW_ID);

        mcpProjectWorkflow.setId(MCP_PROJECT_WORKFLOW_ID);

        return mcpProjectWorkflow;
    }

    private static Method createMethod() throws NoSuchMethodException {
        return McpProjectWorkflowServiceImpl.class.getMethod("create", McpProjectWorkflow.class);
    }

    private static Method createByIdsMethod() throws NoSuchMethodException {
        return McpProjectWorkflowServiceImpl.class.getMethod("create", Long.class, Long.class);
    }

    private static Method deleteMethod() throws NoSuchMethodException {
        return McpProjectWorkflowServiceImpl.class.getMethod("delete", long.class);
    }

    private static Method fetchMcpProjectWorkflowMethod() throws NoSuchMethodException {
        return McpProjectWorkflowServiceImpl.class.getMethod("fetchMcpProjectWorkflow", long.class);
    }

    private static Method updateMethod() throws NoSuchMethodException {
        return McpProjectWorkflowServiceImpl.class.getMethod("update", McpProjectWorkflow.class);
    }

    private static Method updateByIdsMethod() throws NoSuchMethodException {
        return McpProjectWorkflowServiceImpl.class.getMethod("update", long.class, Long.class, Long.class);
    }

    private static Method updateParametersMethod() throws NoSuchMethodException {
        return McpProjectWorkflowServiceImpl.class.getMethod("updateParameters", long.class, Map.class);
    }
}
