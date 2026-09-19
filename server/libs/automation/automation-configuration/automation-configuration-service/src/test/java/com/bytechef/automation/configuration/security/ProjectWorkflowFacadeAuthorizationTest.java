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

import com.bytechef.automation.configuration.facade.ProjectWorkflowFacadeImpl;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.platform.configuration.domain.Environment;
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
 * Evaluates the real {@code @PreAuthorize} expressions on {@link ProjectWorkflowFacadeImpl} through the real
 * {@link AutomationMethodSecurityExpressionHandler} backed by the real {@link AutomationPermissionEvaluator}, asserting
 * each guard in both directions and the exact check that reaches {@link PermissionService}.
 *
 * @author Ivica Cardic
 */
class ProjectWorkflowFacadeAuthorizationTest {

    private static final long PROJECT_ID = 42L;
    private static final String WORKFLOW_ID = "workflow-1";

    @Test
    void testUpdateWorkflowDeniesWhenTheWorkflowEditScopeIsRefused() throws Exception {
        assertWorkflowGuard(updateWorkflowMethod(), new Object[] {
            WORKFLOW_ID, "{}", 1
        }, "WORKFLOW_EDIT", false);
    }

    @Test
    void testUpdateWorkflowAllowsWhenTheWorkflowEditScopeIsGranted() throws Exception {
        assertWorkflowGuard(updateWorkflowMethod(), new Object[] {
            WORKFLOW_ID, "{}", 1
        }, "WORKFLOW_EDIT", true);
    }

    @Test
    void testUpdateWorkflowDeniesAMemberWhoHoldsTheWorkflowEditScopeOnlyInProduction() throws Exception {
        PermissionService permissionService = mock(PermissionService.class);

        when(permissionService.hasWorkflowScope(WORKFLOW_ID, "WORKFLOW_EDIT", Environment.PRODUCTION)).thenReturn(true);

        assertThat(evaluateGuard(permissionService, updateWorkflowMethod(), new Object[] {
            WORKFLOW_ID, "{}", 1
        })).isFalse();

        verify(permissionService).hasWorkflowScope(WORKFLOW_ID, "WORKFLOW_EDIT", Environment.DEVELOPMENT);
        verifyNoMoreInteractions(permissionService);
    }

    @Test
    void testExportSharedWorkflowDeniesWhenTheWorkflowEditScopeIsRefused() throws Exception {
        assertWorkflowGuard(exportSharedWorkflowMethod(), new Object[] {
            WORKFLOW_ID, "description"
        }, "WORKFLOW_EDIT", false);
    }

    @Test
    void testExportSharedWorkflowAllowsWhenTheWorkflowEditScopeIsGranted() throws Exception {
        assertWorkflowGuard(exportSharedWorkflowMethod(), new Object[] {
            WORKFLOW_ID, "description"
        }, "WORKFLOW_EDIT", true);
    }

    @Test
    void testDeleteSharedWorkflowDeniesWhenTheWorkflowDeleteScopeIsRefused() throws Exception {
        assertWorkflowGuard(deleteSharedWorkflowMethod(), new Object[] {
            WORKFLOW_ID
        }, "WORKFLOW_DELETE", false);
    }

    @Test
    void testDeleteSharedWorkflowAllowsWhenTheWorkflowDeleteScopeIsGranted() throws Exception {
        assertWorkflowGuard(deleteSharedWorkflowMethod(), new Object[] {
            WORKFLOW_ID
        }, "WORKFLOW_DELETE", true);
    }

    @Test
    void testReadsCarryNoWorkflowEditGuard() throws Exception {
        assertThat(
            ProjectWorkflowFacadeImpl.class.getMethod("getProjectWorkflow", String.class)
                .getAnnotation(PreAuthorize.class))
                    .isNull();
    }

    private static void assertWorkflowGuard(
        Method method, Object[] arguments, String expectedScope, boolean granted) {

        PermissionService permissionService = mock(PermissionService.class);

        when(permissionService.hasWorkflowScope(WORKFLOW_ID, expectedScope, Environment.DEVELOPMENT))
            .thenReturn(granted);

        assertThat(evaluateGuard(permissionService, method, arguments))
            .as("%s must %s when hasWorkflowScope('%s', '%s', DEVELOPMENT) returns %s", method.getName(),
                granted ? "allow" : "deny", WORKFLOW_ID, expectedScope, granted)
            .isEqualTo(granted);

        verify(permissionService).hasWorkflowScope(WORKFLOW_ID, expectedScope, Environment.DEVELOPMENT);
        verifyNoMoreInteractions(permissionService);
    }

    // The expression parsed here is read straight off our own @PreAuthorize annotation in this repository's compiled
    // bytecode, not attacker-influenced input.
    @SuppressFBWarnings(
        value = "SPEL_INJECTION",
        justification = "The expression is this repository's own @PreAuthorize value, not untrusted input.")
    static boolean evaluateGuard(PermissionService permissionService, Method method, Object[] arguments) {
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

    @Test
    void testAddWorkflowDeniesWhenTheWorkflowCreateScopeIsRefused() throws Exception {
        assertProjectGuard(addWorkflowMethod(), new Object[] {
            PROJECT_ID, "{}"
        }, false);
    }

    @Test
    void testAddWorkflowAllowsWhenTheWorkflowCreateScopeIsGranted() throws Exception {
        assertProjectGuard(addWorkflowMethod(), new Object[] {
            PROJECT_ID, "{}"
        }, true);
    }

    @Test
    void testImportWorkflowTemplateDeniesWhenTheWorkflowCreateScopeIsRefused() throws Exception {
        assertProjectGuard(importWorkflowTemplateMethod(), new Object[] {
            PROJECT_ID, "template", false
        }, false);
    }

    @Test
    void testImportWorkflowTemplateAllowsWhenTheWorkflowCreateScopeIsGranted() throws Exception {
        assertProjectGuard(importWorkflowTemplateMethod(), new Object[] {
            PROJECT_ID, "template", false
        }, true);
    }

    @Test
    void testDuplicateWorkflowAllowsWhenCreateOnTheTargetAndViewOnTheSourceAreGranted() throws Exception {
        assertThat(evaluateDuplicateGuard(true, true)).isTrue();
    }

    @Test
    void testDuplicateWorkflowDeniesWhenTheWorkflowCreateScopeIsRefused() throws Exception {
        assertThat(evaluateDuplicateGuard(false, true)).isFalse();
    }

    @Test
    void testDuplicateWorkflowDeniesWhenTheSourceWorkflowViewScopeIsRefused() throws Exception {
        assertThat(evaluateDuplicateGuard(true, false)).isFalse();
    }

    @Test
    void testDeleteWorkflowDeniesWhenTheWorkflowDeleteScopeIsRefused() throws Exception {
        assertWorkflowGuard(deleteWorkflowMethod(), new Object[] {
            WORKFLOW_ID
        }, "WORKFLOW_DELETE", false);
    }

    @Test
    void testDeleteWorkflowAllowsWhenTheWorkflowDeleteScopeIsGranted() throws Exception {
        assertWorkflowGuard(deleteWorkflowMethod(), new Object[] {
            WORKFLOW_ID
        }, "WORKFLOW_DELETE", true);
    }

    private static void assertProjectGuard(Method method, Object[] arguments, boolean granted) {
        PermissionService permissionService = mock(PermissionService.class);

        when(permissionService.hasResourceScopeInEnvironment(
            PROJECT_ID, "Project", "WORKFLOW_CREATE", Environment.DEVELOPMENT)).thenReturn(granted);

        assertThat(evaluateGuard(permissionService, method, arguments))
            .as("%s must %s when hasResourceScopeInEnvironment(%s, 'Project', 'WORKFLOW_CREATE', DEVELOPMENT) returns %s",
                method.getName(),
                granted ? "allow" : "deny", PROJECT_ID, granted)
            .isEqualTo(granted);

        verify(permissionService).hasResourceScopeInEnvironment(
            PROJECT_ID, "Project", "WORKFLOW_CREATE", Environment.DEVELOPMENT);
        verifyNoMoreInteractions(permissionService);
    }

    private static boolean evaluateDuplicateGuard(boolean createGranted, boolean viewGranted) throws Exception {
        PermissionService permissionService = mock(PermissionService.class);

        when(permissionService.hasResourceScopeInEnvironment(
            PROJECT_ID, "Project", "WORKFLOW_CREATE", Environment.DEVELOPMENT)).thenReturn(createGranted);
        when(permissionService.hasWorkflowScope(WORKFLOW_ID, "WORKFLOW_VIEW", Environment.DEVELOPMENT))
            .thenReturn(viewGranted);

        return evaluateGuard(
            permissionService,
            ProjectWorkflowFacadeImpl.class.getMethod("duplicateWorkflow", long.class, String.class),
            new Object[] {
                PROJECT_ID, WORKFLOW_ID
            });
    }

    private static Method addWorkflowMethod() throws NoSuchMethodException {
        return ProjectWorkflowFacadeImpl.class.getMethod("addWorkflow", long.class, String.class);
    }

    private static Method deleteWorkflowMethod() throws NoSuchMethodException {
        return ProjectWorkflowFacadeImpl.class.getMethod("deleteWorkflow", String.class);
    }

    private static Method importWorkflowTemplateMethod() throws NoSuchMethodException {
        return ProjectWorkflowFacadeImpl.class.getMethod(
            "importWorkflowTemplate", long.class, String.class, boolean.class);
    }

    private static Method deleteSharedWorkflowMethod() throws NoSuchMethodException {
        return ProjectWorkflowFacadeImpl.class.getMethod("deleteSharedWorkflow", String.class);
    }

    private static Method exportSharedWorkflowMethod() throws NoSuchMethodException {
        return ProjectWorkflowFacadeImpl.class.getMethod("exportSharedWorkflow", String.class, String.class);
    }

    private static Method updateWorkflowMethod() throws NoSuchMethodException {
        return ProjectWorkflowFacadeImpl.class.getMethod("updateWorkflow", String.class, String.class, int.class);
    }
}
