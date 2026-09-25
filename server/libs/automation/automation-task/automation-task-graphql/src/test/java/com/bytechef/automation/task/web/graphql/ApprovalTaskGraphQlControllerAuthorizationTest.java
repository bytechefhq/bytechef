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

package com.bytechef.automation.task.web.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.security.AutomationMethodSecurityExpressionHandler;
import com.bytechef.automation.configuration.security.AutomationPermissionEvaluator;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.automation.task.web.graphql.ApprovalTaskGraphQlController.ApprovalTaskInput;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.util.SimpleMethodInvocation;

/**
 * Evaluates the real {@code @PreAuthorize} expressions on the {@link ApprovalTaskGraphQlController} mutations through
 * the real {@link AutomationMethodSecurityExpressionHandler} and {@link AutomationPermissionEvaluator}, asserting each
 * guard in both directions and the exact checks that reach {@link PermissionService}. A task created here is raised for
 * no job and so belongs to no workspace, which leaves its creation to a tenant admin; a task is changed or deleted by
 * whoever may operate the deployment whose job raised it, and changed also by its assignee.
 *
 * @author Ivica Cardic
 */
class ApprovalTaskGraphQlControllerAuthorizationTest {

    private static final long APPROVAL_TASK_ID = 3L;

    @ParameterizedTest(name = "tenantAdmin={0}")
    @ValueSource(booleans = {
        false, true
    })
    void testCreateApprovalTaskRequiresATenantAdmin(boolean tenantAdmin) throws Exception {
        PermissionService permissionService = mock(PermissionService.class);

        when(permissionService.isTenantAdmin()).thenReturn(tenantAdmin);

        Method method = ApprovalTaskGraphQlController.class.getMethod("createApprovalTask", ApprovalTaskInput.class);

        assertThat(evaluateGuard(permissionService, method, approvalTaskInput(null)))
            .as("createApprovalTask must %s when isTenantAdmin() returns %s", tenantAdmin ? "allow" : "deny",
                tenantAdmin)
            .isEqualTo(tenantAdmin);

        verify(permissionService).isTenantAdmin();
        verifyNoMoreInteractions(permissionService);
    }

    @ParameterizedTest(name = "granted={0}")
    @ValueSource(booleans = {
        false, true
    })
    void testDeleteApprovalTaskRequiresDeploymentEditOnTheTasksJob(boolean granted) throws Exception {
        PermissionService permissionService = mock(PermissionService.class);

        when(permissionService.hasResourceScope(APPROVAL_TASK_ID, "ApprovalTask", "DEPLOYMENT_EDIT"))
            .thenReturn(granted);

        Method method = ApprovalTaskGraphQlController.class.getMethod("deleteApprovalTask", long.class);

        assertThat(evaluateGuard(permissionService, method, APPROVAL_TASK_ID))
            .as("deleteApprovalTask must %s when DEPLOYMENT_EDIT on the task is %s", granted ? "allow" : "deny",
                granted ? "granted" : "refused")
            .isEqualTo(granted);

        verify(permissionService).hasResourceScope(APPROVAL_TASK_ID, "ApprovalTask", "DEPLOYMENT_EDIT");
        verifyNoMoreInteractions(permissionService);
    }

    @ParameterizedTest(name = "scopeGranted={0} assignee={1}")
    @CsvSource({
        "false, false, false", "false, true, true", "true, false, true"
    })
    void testUpdateApprovalTaskRequiresDeploymentEditOnTheTasksJobOrItsAssignee(
        boolean scopeGranted, boolean assignee, boolean expected) throws Exception {

        PermissionService permissionService = mock(PermissionService.class);

        when(permissionService.hasResourceScope(APPROVAL_TASK_ID, "ApprovalTask", "DEPLOYMENT_EDIT"))
            .thenReturn(scopeGranted);
        when(permissionService.isResourceOwner("ApprovalTask", APPROVAL_TASK_ID)).thenReturn(assignee);

        Method method = ApprovalTaskGraphQlController.class.getMethod("updateApprovalTask", ApprovalTaskInput.class);

        assertThat(evaluateGuard(permissionService, method, approvalTaskInput(APPROVAL_TASK_ID)))
            .as("updateApprovalTask must %s when DEPLOYMENT_EDIT is %s and the caller %s the assignee",
                expected ? "allow" : "deny", scopeGranted ? "granted" : "refused", assignee ? "is" : "is not")
            .isEqualTo(expected);

        verify(permissionService).hasResourceScope(APPROVAL_TASK_ID, "ApprovalTask", "DEPLOYMENT_EDIT");

        if (scopeGranted) {
            verify(permissionService, never()).isResourceOwner("ApprovalTask", APPROVAL_TASK_ID);
        } else {
            verify(permissionService).isResourceOwner("ApprovalTask", APPROVAL_TASK_ID);
        }

        verifyNoMoreInteractions(permissionService);
    }

    private static ApprovalTaskInput approvalTaskInput(Long id) {
        return new ApprovalTaskInput(id, "Approve the invoice", null, null, null, null, null, null);
    }

    @SuppressFBWarnings(
        value = "SPEL_INJECTION",
        justification = "The expression is this repository's own @PreAuthorize value, not untrusted input.")
    private static boolean evaluateGuard(PermissionService permissionService, Method method, Object argument) {
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize)
            .as("%s must carry a @PreAuthorize guard", method.getName())
            .isNotNull();

        AutomationMethodSecurityExpressionHandler expressionHandler =
            new AutomationMethodSecurityExpressionHandler(permissionService);

        expressionHandler.setPermissionEvaluator(new AutomationPermissionEvaluator(permissionService));

        Authentication authentication = new UsernamePasswordAuthenticationToken("alice", "credentials", List.of());

        SimpleMethodInvocation methodInvocation = new SimpleMethodInvocation(new Object(), method, argument);

        EvaluationContext evaluationContext =
            expressionHandler.createEvaluationContext(() -> authentication, methodInvocation);

        Expression expression = expressionHandler.getExpressionParser()
            .parseExpression(preAuthorize.value());

        return Boolean.TRUE.equals(expression.getValue(evaluationContext, Boolean.class));
    }
}
