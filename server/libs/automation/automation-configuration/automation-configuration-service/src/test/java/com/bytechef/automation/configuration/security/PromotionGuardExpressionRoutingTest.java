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
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.dto.ProjectDeploymentDTO;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.platform.configuration.domain.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.util.SimpleMethodInvocation;

/**
 * Evaluates the real {@code @PreAuthorize} expression string taken off
 * {@code ProjectDeploymentFacade#createProjectDeployment} through a real Spring expression handler backed by the real
 * {@link AutomationPermissionEvaluator}.
 * <p>
 * Pinning the expression string and unit-testing the evaluator separately leaves a gap between them: whether the
 * expression actually routes to the overload that receives the deployment object. Spring's three-argument
 * {@code hasPermission} form casts its first argument to {@code Serializable}, which this record is not, and its
 * four-argument evaluator method dispatches to {@code hasResourceScope} rather than to the promotion branch. Either
 * mistake leaves promotion checking the wrong environment, or failing outright, while both other tests stay green.
 *
 * @author Ivica Cardic
 */
class PromotionGuardExpressionRoutingTest {

    private static final long PROJECT_ID = 42L;

    @Test
    void testTheRealGuardExpressionReachesThePromotionBranch() throws Exception {
        PermissionService permissionService = mock(PermissionService.class);

        when(permissionService.hasWorkspaceScopeForProject(PROJECT_ID, "WORKFLOW_EDIT", Environment.PRODUCTION))
            .thenReturn(false);

        ProjectDeploymentDTO projectDeploymentDTO = projectDeployment(PROJECT_ID, Environment.PRODUCTION);

        assertThat(evaluateGuard(permissionService, projectDeploymentDTO)).isFalse();

        // The target environment decided, and it was read off the deployment rather than from any ambient state.
        verify(permissionService).hasWorkspaceScopeForProject(PROJECT_ID, "WORKFLOW_EDIT", Environment.PRODUCTION);
    }

    @Test
    void testTheRealGuardExpressionAllowsWhenTheTargetEnvironmentGrantsTheScope() throws Exception {
        PermissionService permissionService = mock(PermissionService.class);

        when(permissionService.hasWorkspaceScopeForProject(PROJECT_ID, "WORKFLOW_EDIT", Environment.DEVELOPMENT))
            .thenReturn(true);

        assertThat(evaluateGuard(permissionService, projectDeployment(PROJECT_ID, Environment.DEVELOPMENT))).isTrue();
    }

    // The expression parsed here is read straight off our own @PreAuthorize annotation in this repository's compiled
    // bytecode -- reading it rather than restating it as a literal is the entire point of the test, since a literal
    // could drift from the guard it claims to verify. It is not attacker-influenced input.
    @SuppressFBWarnings(
        value = "SPEL_INJECTION",
        justification = "The expression is this repository's own @PreAuthorize value, not untrusted input.")
    private static boolean evaluateGuard(
        PermissionService permissionService, ProjectDeploymentDTO projectDeploymentDTO) throws Exception {

        Method method = ProjectDeploymentFacade.class.getMethod("createProjectDeployment", ProjectDeploymentDTO.class);

        PreAuthorize preAuthorize = findPreAuthorize(method);

        DefaultMethodSecurityExpressionHandler expressionHandler = new DefaultMethodSecurityExpressionHandler();

        expressionHandler.setPermissionEvaluator(
            new AutomationPermissionEvaluator(permissionService, mock(ObjectProvider.class)));

        Authentication authentication =
            new UsernamePasswordAuthenticationToken("alice", "credentials", List.of());

        SimpleMethodInvocation methodInvocation = new SimpleMethodInvocation(
            new Object(), method, projectDeploymentDTO);

        EvaluationContext evaluationContext =
            expressionHandler.createEvaluationContext(() -> authentication, methodInvocation);

        Expression expression = expressionHandler.getExpressionParser()
            .parseExpression(preAuthorize.value());

        return Boolean.TRUE.equals(expression.getValue(evaluationContext, Boolean.class));
    }

    private static PreAuthorize findPreAuthorize(Method method) throws Exception {
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        if (preAuthorize != null) {
            return preAuthorize;
        }

        // The interface method carries no annotation; the guard lives on the implementation.
        Class<?> implementationClass =
            Class.forName("com.bytechef.automation.configuration.facade.ProjectDeploymentFacadeImpl");

        Method implementationMethod =
            implementationClass.getMethod(method.getName(), method.getParameterTypes());

        preAuthorize = implementationMethod.getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize)
            .as("createProjectDeployment must carry a @PreAuthorize guard")
            .isNotNull();

        return preAuthorize;
    }

    private static ProjectDeploymentDTO projectDeployment(long projectId, Environment environment) {
        return new ProjectDeploymentDTO(
            null, null, null, true, environment, 1L, "deployment", null, null, null, null, projectId, 1, List.of(),
            List.of(), 0);
    }
}
