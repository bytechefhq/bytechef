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

import com.bytechef.automation.configuration.facade.ProjectFacadeImpl;
import com.bytechef.automation.configuration.service.PermissionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.util.SimpleMethodInvocation;

/**
 * Evaluates the real {@code @PreAuthorize} expression on {@code ProjectFacadeImpl.duplicateProject} through a real
 * Spring expression handler backed by the real {@link AutomationPermissionEvaluator}.
 * <p>
 * Duplicating is a create: the body calls {@code projectService.create} and {@code projectWorkflowService.addWorkflow},
 * and {@code ProjectServiceImpl.create} carries no gate of its own, so this annotation is the only barrier. It named
 * {@code WORKFLOW_VIEW} alone — a read scope — which let a view-only member create a project and its workflows by
 * duplicating any project they could open, bypassing the {@code PROJECT_CREATE} that {@code createProject},
 * {@code importProject} and {@code importProjectTemplate} all demand.
 * <p>
 * The guard is a conjunction, so every combination is asserted rather than only the denial. Holding the read alone must
 * not create; holding the create alone must not reach a project the caller cannot read; holding both must pass. A test
 * that only checked "denies without {@code PROJECT_CREATE}" would stay green if a later edit dropped the read half, and
 * one that only checked the allow direction could not tell a working conjunction from a guard that admits everybody.
 * <p>
 * Only the outcome and the scopes actually consulted are asserted, never the order the two halves are evaluated in:
 * SpEL {@code and} short-circuits, so pinning which half runs first would redden a harmless reordering of an expression
 * that admits exactly the same principals either way.
 *
 * @author Ivica Cardic
 */
class ProjectDuplicationGuardExpressionTest {

    private static final String PROJECT_CREATE = "PROJECT_CREATE";
    private static final long PROJECT_ID = 42L;
    private static final String PROJECT_TYPE = "Project";
    private static final String WORKFLOW_VIEW = "WORKFLOW_VIEW";

    @Test
    void testDuplicateProjectDeniesWhenTheCallerHoldsOnlyTheViewScope() throws Exception {
        PermissionService permissionService = mock(PermissionService.class);

        when(permissionService.hasResourceScope(PROJECT_ID, PROJECT_TYPE, WORKFLOW_VIEW)).thenReturn(true);
        when(permissionService.hasResourceScope(PROJECT_ID, PROJECT_TYPE, PROJECT_CREATE)).thenReturn(false);

        assertThat(evaluateDuplicateProjectGuard(permissionService))
            .as("a member holding WORKFLOW_VIEW but not PROJECT_CREATE must not create a project by duplicating one")
            .isFalse();

        // Asked in both evaluation orders when the create scope is the one refused, so this verify does not pin the
        // order.
        verify(permissionService).hasResourceScope(PROJECT_ID, PROJECT_TYPE, PROJECT_CREATE);
    }

    @Test
    void testDuplicateProjectAllowsWhenTheCallerHoldsBothScopes() throws Exception {
        PermissionService permissionService = mock(PermissionService.class);

        when(permissionService.hasResourceScope(PROJECT_ID, PROJECT_TYPE, WORKFLOW_VIEW)).thenReturn(true);
        when(permissionService.hasResourceScope(PROJECT_ID, PROJECT_TYPE, PROJECT_CREATE)).thenReturn(true);

        assertThat(evaluateDuplicateProjectGuard(permissionService))
            .as("a member holding both WORKFLOW_VIEW and PROJECT_CREATE must be allowed to duplicate a project")
            .isTrue();

        // Both halves must be consulted. Granting only one and reading the other from a default would make the denial
        // assertions above green for the wrong reason.
        verify(permissionService).hasResourceScope(PROJECT_ID, PROJECT_TYPE, WORKFLOW_VIEW);
        verify(permissionService).hasResourceScope(PROJECT_ID, PROJECT_TYPE, PROJECT_CREATE);
    }

    @Test
    void testDuplicateProjectDeniesWhenTheCallerCannotReadTheSourceProject() throws Exception {
        PermissionService permissionService = mock(PermissionService.class);

        when(permissionService.hasResourceScope(PROJECT_ID, PROJECT_TYPE, WORKFLOW_VIEW)).thenReturn(false);
        when(permissionService.hasResourceScope(PROJECT_ID, PROJECT_TYPE, PROJECT_CREATE)).thenReturn(true);

        assertThat(evaluateDuplicateProjectGuard(permissionService))
            .as("PROJECT_CREATE must not admit a caller who cannot read the project being copied")
            .isFalse();

        verify(permissionService).hasResourceScope(PROJECT_ID, PROJECT_TYPE, WORKFLOW_VIEW);
    }

    @Test
    void testDuplicateProjectDeniesWhenTheCallerHoldsNeitherScope() throws Exception {
        PermissionService permissionService = mock(PermissionService.class);

        assertThat(evaluateDuplicateProjectGuard(permissionService))
            .as("a caller holding neither scope must be denied")
            .isFalse();
    }

    // The expression parsed here is read straight off our own @PreAuthorize annotation in this repository's compiled
    // bytecode -- reading it rather than restating it as a literal is the entire point of the test, since a literal
    // could drift from the guard it claims to verify. It is not attacker-influenced input.
    @SuppressFBWarnings(
        value = "SPEL_INJECTION",
        justification = "The expression is this repository's own @PreAuthorize value, not untrusted input.")
    private static boolean evaluateDuplicateProjectGuard(PermissionService permissionService) throws Exception {
        Method method = ProjectFacadeImpl.class.getMethod("duplicateProject", long.class);

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize)
            .as("%s must carry a @PreAuthorize guard", method.getName())
            .isNotNull();

        DefaultMethodSecurityExpressionHandler expressionHandler = new DefaultMethodSecurityExpressionHandler();

        expressionHandler.setPermissionEvaluator(new AutomationPermissionEvaluator(permissionService));

        Authentication authentication = new UsernamePasswordAuthenticationToken("alice", "credentials", List.of());

        SimpleMethodInvocation methodInvocation = new SimpleMethodInvocation(
            new Object(), method, new Object[] {
                PROJECT_ID
            });

        EvaluationContext evaluationContext =
            expressionHandler.createEvaluationContext(() -> authentication, methodInvocation);

        Expression expression = expressionHandler.getExpressionParser()
            .parseExpression(preAuthorize.value());

        return Boolean.TRUE.equals(expression.getValue(evaluationContext, Boolean.class));
    }
}
