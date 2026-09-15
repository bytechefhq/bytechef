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
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.security.AutomationMethodSecurityExpressionHandler;
import com.bytechef.automation.configuration.security.AutomationPermissionEvaluator;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.util.SimpleMethodInvocation;

/**
 * Evaluates the real {@code @PreAuthorize} expressions on the project and deployment GraphQL reads through the real
 * {@link AutomationMethodSecurityExpressionHandler} and {@link AutomationPermissionEvaluator}.
 *
 * @author Ivica Cardic
 */
class ProjectReadGraphQlControllersAuthorizationTest {

    private static final long ENVIRONMENT_ID = 2L;
    private static final long PROJECT_DEPLOYMENT_ID = 7L;
    private static final long PROJECT_ID = 11L;
    private static final long WORKSPACE_ID = 42L;

    @BeforeAll
    static void warmUpGuardEvaluation() throws NoSuchMethodException {
        evaluateGuard(
            mock(PermissionService.class), ProjectGraphQlController.class.getMethod("projects"), new Object[0]);
    }

    @ParameterizedTest
    @ValueSource(booleans = {
        false, true
    })
    void testProjectRequiresWorkflowViewOnTheProject(boolean granted) throws NoSuchMethodException {
        PermissionService permissionService = mock(PermissionService.class);

        when(permissionService.hasResourceScope(PROJECT_ID, "Project", "WORKFLOW_VIEW")).thenReturn(granted);

        Method method = ProjectGraphQlController.class.getMethod("project", long.class);

        assertThat(evaluateGuard(permissionService, method, new Object[] {
            PROJECT_ID
        })).isEqualTo(granted);

        verify(permissionService).hasResourceScope(PROJECT_ID, "Project", "WORKFLOW_VIEW");
    }

    @ParameterizedTest
    @ValueSource(booleans = {
        false, true
    })
    void testProjectsRequiresATenantAdmin(boolean granted) throws NoSuchMethodException {
        PermissionService permissionService = mock(PermissionService.class);

        when(permissionService.isTenantAdmin()).thenReturn(granted);

        Method method = ProjectGraphQlController.class.getMethod("projects");

        assertThat(evaluateGuard(permissionService, method, new Object[0])).isEqualTo(granted);
    }

    @ParameterizedTest
    @ValueSource(booleans = {
        false, true
    })
    void testWorkspaceProjectDeploymentsRequiresDeploymentViewInTheNamedEnvironment(boolean granted)
        throws NoSuchMethodException {

        PermissionService permissionService = mock(PermissionService.class);
        Environment environment = Environment.values()[(int) ENVIRONMENT_ID];

        when(permissionService.hasWorkspaceScope(WORKSPACE_ID, "DEPLOYMENT_VIEW", environment)).thenReturn(granted);

        Method method = ProjectDeploymentGraphQlController.class.getMethod(
            "workspaceProjectDeployments", Long.class, Long.class, Long.class, Long.class);

        assertThat(evaluateGuard(permissionService, method, new Object[] {
            WORKSPACE_ID, ENVIRONMENT_ID, null, null
        })).isEqualTo(granted);

        verify(permissionService).hasWorkspaceScope(WORKSPACE_ID, "DEPLOYMENT_VIEW", environment);
    }

    @ParameterizedTest
    @ValueSource(booleans = {
        false, true
    })
    void testWorkspaceChatWorkflowsRequiresDeploymentViewInTheNamedEnvironment(boolean granted)
        throws NoSuchMethodException {

        PermissionService permissionService = mock(PermissionService.class);
        Environment environment = Environment.values()[(int) ENVIRONMENT_ID];

        when(permissionService.hasWorkspaceScope(WORKSPACE_ID, "DEPLOYMENT_VIEW", environment)).thenReturn(granted);

        Method method = ProjectDeploymentWorkflowGraphQlController.class.getMethod(
            "workspaceChatWorkflows", Long.class, Long.class);

        assertThat(evaluateGuard(permissionService, method, new Object[] {
            WORKSPACE_ID, ENVIRONMENT_ID
        })).isEqualTo(granted);

        verify(permissionService).hasWorkspaceScope(WORKSPACE_ID, "DEPLOYMENT_VIEW", environment);
    }

    @ParameterizedTest
    @ValueSource(booleans = {
        false, true
    })
    void testProjectDeploymentWorkflowRequiresDeploymentViewOnTheEncodedDeployment(boolean granted)
        throws NoSuchMethodException {

        PermissionService permissionService = mock(PermissionService.class);

        when(permissionService.hasResourceScope(PROJECT_DEPLOYMENT_ID, "ProjectDeployment", "DEPLOYMENT_VIEW"))
            .thenReturn(granted);

        WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.of(
            PlatformType.AUTOMATION, PROJECT_DEPLOYMENT_ID, "workflow-uuid", "trigger_1");

        Method method = ProjectDeploymentWorkflowGraphQlController.class.getMethod(
            "projectDeploymentWorkflow", String.class);

        assertThat(evaluateGuard(permissionService, method, new Object[] {
            workflowExecutionId.toString()
        })).isEqualTo(granted);

        verify(permissionService).hasResourceScope(PROJECT_DEPLOYMENT_ID, "ProjectDeployment", "DEPLOYMENT_VIEW");
    }

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
}
