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

package com.bytechef.automation.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.security.AutomationMethodSecurityExpressionHandler;
import com.bytechef.automation.configuration.security.AutomationPermissionEvaluator;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.Serializable;
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
 * Evaluates the real {@code @PreAuthorize} expressions on {@link ProjectDeploymentServiceImpl} through a real Spring
 * expression handler backed by the real {@link AutomationPermissionEvaluator}.
 *
 * @author Ivica Cardic
 */
class ProjectDeploymentServiceTest {

    private static final long PROJECT_DEPLOYMENT_ID = 11L;
    private static final long PROJECT_ID = 42L;
    private static final String PROJECT_DEPLOYMENT_TYPE = "ProjectDeployment";

    @Test
    void testServiceUpdateIsKeyedOnTheStoredDeploymentNotTheSuppliedProject() throws Exception {
        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setId(PROJECT_DEPLOYMENT_ID);
        projectDeployment.setProjectId(PROJECT_ID);

        assertGuard(
            ProjectDeploymentServiceImpl.class.getMethod("update", ProjectDeployment.class), new Object[] {
                projectDeployment
            }, PROJECT_DEPLOYMENT_ID, PROJECT_DEPLOYMENT_TYPE, "DEPLOYMENT_CREATE", false);
    }

    private void assertGuard(
        Method method, Object[] arguments, Serializable expectedId, String expectedResourceType, String expectedScope,
        boolean granted) {

        PermissionService permissionService = mock(PermissionService.class);

        when(permissionService.hasResourceScope(expectedId, expectedResourceType, expectedScope)).thenReturn(granted);

        assertThat(evaluateGuard(permissionService, method, arguments))
            .as("%s must %s when hasResourceScope(%s, '%s', '%s') returns %s", method.getName(),
                granted ? "allow" : "deny", expectedId, expectedResourceType, expectedScope, granted)
            .isEqualTo(granted);

        verify(permissionService).hasResourceScope(expectedId, expectedResourceType, expectedScope);
        verifyNoMoreInteractions(permissionService);
    }

    // The expression parsed here is read straight off our own @PreAuthorize annotation in this repository's compiled
    // bytecode -- reading it rather than restating it as a literal is the entire point of the test, since a literal
    // could drift from the guard it claims to verify. It is not attacker-influenced input.
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
