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

import com.bytechef.automation.configuration.facade.ProjectDeploymentFacadeImpl;
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
 * Evaluates the {@code DEPLOYMENT_VIEW} guards on {@link ProjectDeploymentFacadeImpl}'s reads through the real
 * {@link AutomationMethodSecurityExpressionHandler}, in both directions.
 *
 * @author Ivica Cardic
 */
class ProjectDeploymentFacadeAuthorizationTest {

    private static final long ENVIRONMENT_ID = 2L;
    private static final long PROJECT_DEPLOYMENT_ID = 11L;
    private static final long WORKSPACE_ID = 42L;

    @Test
    void testGetProjectDeploymentDeniesWhenTheDeploymentViewScopeIsRefused() throws Exception {
        assertGetProjectDeploymentGuard(false);
    }

    @Test
    void testGetProjectDeploymentAllowsWhenTheDeploymentViewScopeIsGranted() throws Exception {
        assertGetProjectDeploymentGuard(true);
    }

    @Test
    void testGetWorkspaceProjectDeploymentsDeniesWhenTheDeploymentViewScopeIsRefused() throws Exception {
        assertGetWorkspaceProjectDeploymentsGuard(false);
    }

    @Test
    void testGetWorkspaceProjectDeploymentsAllowsWhenTheDeploymentViewScopeIsGranted() throws Exception {
        assertGetWorkspaceProjectDeploymentsGuard(true);
    }

    private void assertGetProjectDeploymentGuard(boolean granted) throws Exception {
        PermissionService permissionService = mock(PermissionService.class);

        when(permissionService.hasResourceScope(PROJECT_DEPLOYMENT_ID, "ProjectDeployment", "DEPLOYMENT_VIEW"))
            .thenReturn(granted);

        Method method = ProjectDeploymentFacadeImpl.class.getMethod("getProjectDeployment", long.class);

        assertThat(evaluateGuard(permissionService, method, new Object[] {
            PROJECT_DEPLOYMENT_ID
        }))
            .isEqualTo(granted);

        verify(permissionService).hasResourceScope(PROJECT_DEPLOYMENT_ID, "ProjectDeployment", "DEPLOYMENT_VIEW");
        verifyNoMoreInteractions(permissionService);
    }

    private void assertGetWorkspaceProjectDeploymentsGuard(boolean granted) throws Exception {
        PermissionService permissionService = mock(PermissionService.class);
        Environment environment = Environment.values()[(int) ENVIRONMENT_ID];

        when(permissionService.hasWorkspaceScope(WORKSPACE_ID, "DEPLOYMENT_VIEW", environment)).thenReturn(granted);

        Method method = ProjectDeploymentFacadeImpl.class.getMethod(
            "getWorkspaceProjectDeployments", long.class, Long.class, Long.class, Long.class, boolean.class);

        assertThat(evaluateGuard(permissionService, method, new Object[] {
            WORKSPACE_ID, ENVIRONMENT_ID, null, null, false
        }))
            .isEqualTo(granted);

        verify(permissionService).hasWorkspaceScope(WORKSPACE_ID, "DEPLOYMENT_VIEW", environment);
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
}
