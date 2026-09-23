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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.dto.ProjectDeploymentDTO;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacadeImpl;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.platform.configuration.domain.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.Serializable;
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
 * Evaluates the real {@code @PreAuthorize} expressions on the previously unguarded mutating methods of
 * {@code ProjectDeploymentFacadeImpl} through a real Spring expression handler backed by the real
 * {@link AutomationPermissionEvaluator}.
 * <p>
 * Each guard is asserted in both directions. A deny-only assertion cannot tell "refuses an outsider" from "refuses
 * everybody", which is the failure mode these guards are most exposed to: {@code hasResourceScope} denies
 * unconditionally when no {@link ResourceOwnershipResolver} is registered for the named resource type, so a guard
 * naming a type with no resolver locks out every non-tenant-admin while looking like protection. Both directions also
 * verify the exact {@code (id, resourceType, scope)} triple that reaches the {@link PermissionService}, which is what
 * catches a mistyped scope or a guard keyed on the wrong argument.
 *
 * @author Ivica Cardic
 */
class ProjectDeploymentGuardExpressionTest {

    private static final long PROJECT_DEPLOYMENT_ID = 11L;
    private static final long PROJECT_DEPLOYMENT_WORKFLOW_ID = 77L;
    private static final long PROJECT_ID = 42L;
    private static final String PROJECT_DEPLOYMENT_TYPE = "ProjectDeployment";
    private static final String PROJECT_DEPLOYMENT_WORKFLOW_TYPE = "ProjectDeploymentWorkflow";
    private static final String PROJECT_TYPE = "Project";
    private static final String WORKFLOW_ID = "workflow-1";

    @Test
    void testDeleteProjectDeploymentDeniesWhenTheDeploymentScopeIsRefused() throws Exception {
        assertGuard(
            deleteProjectDeploymentMethod(), new Object[] {
                PROJECT_DEPLOYMENT_ID
            }, PROJECT_DEPLOYMENT_ID, PROJECT_DEPLOYMENT_TYPE, "DEPLOYMENT_DELETE", false);
    }

    @Test
    void testDeleteProjectDeploymentAllowsWhenTheDeploymentScopeIsGranted() throws Exception {
        assertGuard(
            deleteProjectDeploymentMethod(), new Object[] {
                PROJECT_DEPLOYMENT_ID
            }, PROJECT_DEPLOYMENT_ID, PROJECT_DEPLOYMENT_TYPE, "DEPLOYMENT_DELETE", true);
    }

    @Test
    void testCreateProjectDeploymentWorkflowJobDeniesWhenTheDeploymentScopeIsRefused() throws Exception {
        assertGuard(
            createProjectDeploymentWorkflowJobMethod(), new Object[] {
                PROJECT_DEPLOYMENT_ID, "workflow-1"
            }, PROJECT_DEPLOYMENT_ID, PROJECT_DEPLOYMENT_TYPE, "DEPLOYMENT_EDIT", false);
    }

    @Test
    void testCreateProjectDeploymentWorkflowJobAllowsWhenTheDeploymentScopeIsGranted() throws Exception {
        assertGuard(
            createProjectDeploymentWorkflowJobMethod(), new Object[] {
                PROJECT_DEPLOYMENT_ID, "workflow-1"
            }, PROJECT_DEPLOYMENT_ID, PROJECT_DEPLOYMENT_TYPE, "DEPLOYMENT_EDIT", true);
    }

    @Test
    void testUpdateProjectDeploymentTagsDeniesWhenTheDeploymentScopeIsRefused() throws Exception {
        assertGuard(
            updateProjectDeploymentTagsMethod(), new Object[] {
                PROJECT_DEPLOYMENT_ID, List.of()
            }, PROJECT_DEPLOYMENT_ID, PROJECT_DEPLOYMENT_TYPE, "DEPLOYMENT_EDIT", false);
    }

    @Test
    void testUpdateProjectDeploymentTagsAllowsWhenTheDeploymentScopeIsGranted() throws Exception {
        assertGuard(
            updateProjectDeploymentTagsMethod(), new Object[] {
                PROJECT_DEPLOYMENT_ID, List.of()
            }, PROJECT_DEPLOYMENT_ID, PROJECT_DEPLOYMENT_TYPE, "DEPLOYMENT_EDIT", true);
    }

    @Test
    void testUpdateProjectDeploymentWorkflowDeniesWhenTheDeploymentWorkflowScopeIsRefused() throws Exception {
        assertGuard(
            updateProjectDeploymentWorkflowMethod(), new Object[] {
                projectDeploymentWorkflow()
            }, PROJECT_DEPLOYMENT_WORKFLOW_ID, PROJECT_DEPLOYMENT_WORKFLOW_TYPE, "DEPLOYMENT_EDIT", false);
    }

    @Test
    void testUpdateProjectDeploymentWorkflowAllowsWhenTheDeploymentWorkflowScopeIsGranted() throws Exception {
        assertGuard(
            updateProjectDeploymentWorkflowMethod(), new Object[] {
                projectDeploymentWorkflow()
            }, PROJECT_DEPLOYMENT_WORKFLOW_ID, PROJECT_DEPLOYMENT_WORKFLOW_TYPE, "DEPLOYMENT_EDIT", true);
    }

    /**
     * The row written is selected by its own id, so that is what the guard must ask about. Were it keyed on the
     * argument's {@code projectDeploymentId} instead, the two would be independently caller-supplied — the REST path
     * carries the deployment id and the row id as separate variables — and a member who is editor in Development and
     * viewer in Production could satisfy the check against a Development deployment while repointing a Production row's
     * connections and inputs. The argument here is exactly that mismatched pair.
     */
    @Test
    void testUpdateProjectDeploymentWorkflowNeverAsksAboutTheCallerSuppliedDeploymentId() throws Exception {
        PermissionService permissionService = mock(PermissionService.class);

        when(
            permissionService.hasResourceScope(
                PROJECT_DEPLOYMENT_ID, PROJECT_DEPLOYMENT_TYPE, "DEPLOYMENT_EDIT"))
                    .thenReturn(true);

        assertThat(
            evaluateGuard(
                permissionService, updateProjectDeploymentWorkflowMethod(), new Object[] {
                    projectDeploymentWorkflow()
                }))
                    .as("a scope held on the caller-supplied deployment id must not authorize a write to a row " +
                        "belonging to another deployment")
                    .isFalse();

        verify(permissionService)
            .hasResourceScope(PROJECT_DEPLOYMENT_WORKFLOW_ID, PROJECT_DEPLOYMENT_WORKFLOW_TYPE, "DEPLOYMENT_EDIT");
        verifyNoMoreInteractions(permissionService);
    }

    @Test
    void testEnableProjectDeploymentDeniesWhenTheDeploymentScopeIsRefused() throws Exception {
        assertGuard(
            enableProjectDeploymentMethod(), new Object[] {
                PROJECT_DEPLOYMENT_ID, true
            }, PROJECT_DEPLOYMENT_ID, PROJECT_DEPLOYMENT_TYPE, "DEPLOYMENT_EDIT", false);
    }

    @Test
    void testEnableProjectDeploymentAllowsWhenTheDeploymentScopeIsGranted() throws Exception {
        assertGuard(
            enableProjectDeploymentMethod(), new Object[] {
                PROJECT_DEPLOYMENT_ID, true
            }, PROJECT_DEPLOYMENT_ID, PROJECT_DEPLOYMENT_TYPE, "DEPLOYMENT_EDIT", true);
    }

    @Test
    void testEnableProjectDeploymentWorkflowDeniesWhenTheDeploymentScopeIsRefused() throws Exception {
        assertGuard(
            enableProjectDeploymentWorkflowMethod(), new Object[] {
                PROJECT_DEPLOYMENT_ID, WORKFLOW_ID, true
            }, PROJECT_DEPLOYMENT_ID, PROJECT_DEPLOYMENT_TYPE, "DEPLOYMENT_EDIT", false);
    }

    @Test
    void testEnableProjectDeploymentWorkflowAllowsWhenTheDeploymentScopeIsGranted() throws Exception {
        assertGuard(
            enableProjectDeploymentWorkflowMethod(), new Object[] {
                PROJECT_DEPLOYMENT_ID, WORKFLOW_ID, true
            }, PROJECT_DEPLOYMENT_ID, PROJECT_DEPLOYMENT_TYPE, "DEPLOYMENT_EDIT", true);
    }

    /**
     * The four-argument overload takes a projectId, so its check is environment-blind by necessity and names the
     * {@code 'Project'} token. Pinned in both directions because an environment-blind check is still a check, and
     * because the overload is reached from the three-argument one by self-invocation and so cannot borrow its guard.
     */
    @Test
    void testEnableProjectDeploymentWorkflowByEnvironmentDeniesWhenTheProjectScopeIsRefused() throws Exception {
        assertGuard(
            enableProjectDeploymentWorkflowByEnvironmentMethod(), new Object[] {
                PROJECT_ID, WORKFLOW_ID, true, Environment.PRODUCTION
            }, PROJECT_ID, PROJECT_TYPE, "DEPLOYMENT_EDIT", false);
    }

    @Test
    void testEnableProjectDeploymentWorkflowByEnvironmentAllowsWhenTheProjectScopeIsGranted() throws Exception {
        assertGuard(
            enableProjectDeploymentWorkflowByEnvironmentMethod(), new Object[] {
                PROJECT_ID, WORKFLOW_ID, true, Environment.PRODUCTION
            }, PROJECT_ID, PROJECT_TYPE, "DEPLOYMENT_EDIT", true);
    }

    @Test
    void testUpdateProjectDeploymentDeniesWhenTheDeploymentScopeIsRefused() throws Exception {
        assertGuard(
            updateProjectDeploymentMethod(), new Object[] {
                projectDeploymentDTO()
            }, PROJECT_DEPLOYMENT_ID, PROJECT_DEPLOYMENT_TYPE, "DEPLOYMENT_CREATE", false);
    }

    @Test
    void testUpdateProjectDeploymentAllowsWhenTheDeploymentScopeIsGranted() throws Exception {
        assertGuard(
            updateProjectDeploymentMethod(), new Object[] {
                projectDeploymentDTO()
            }, PROJECT_DEPLOYMENT_ID, PROJECT_DEPLOYMENT_TYPE, "DEPLOYMENT_CREATE", true);
    }

    /**
     * The update guard is keyed on the deployment id, not on the DTO, so the environment it is judged in comes from the
     * stored row rather than from the request body. Were it the DTO form the promotion branch would run instead, and a
     * caller could name the environment they hold a role in while editing a deployment in another.
     */
    @Test
    void testUpdateProjectDeploymentDoesNotRouteThroughThePromotionBranch() throws Exception {
        PermissionService permissionService = mock(PermissionService.class);

        evaluateGuard(
            permissionService, updateProjectDeploymentMethod(), new Object[] {
                projectDeploymentDTO()
            });

        // Asserted as "the id form and nothing else", not as "not this one promotion call": a return to the DTO form
        // under any other scope, environment or projectId would leave a scope-specific never() green.
        verify(permissionService).hasResourceScope(PROJECT_DEPLOYMENT_ID, PROJECT_DEPLOYMENT_TYPE, "DEPLOYMENT_CREATE");
        verify(permissionService, never()).hasWorkspaceScopeForProject(anyLong(), anyString());
        verify(permissionService, never()).hasWorkspaceScopeForProject(anyLong(), anyString(), any());
        verifyNoMoreInteractions(permissionService);
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

        DefaultMethodSecurityExpressionHandler expressionHandler = new DefaultMethodSecurityExpressionHandler();

        expressionHandler.setPermissionEvaluator(new AutomationPermissionEvaluator(permissionService));

        Authentication authentication = new UsernamePasswordAuthenticationToken("alice", "credentials", List.of());

        SimpleMethodInvocation methodInvocation = new SimpleMethodInvocation(new Object(), method, arguments);

        EvaluationContext evaluationContext =
            expressionHandler.createEvaluationContext(() -> authentication, methodInvocation);

        Expression expression = expressionHandler.getExpressionParser()
            .parseExpression(preAuthorize.value());

        return Boolean.TRUE.equals(expression.getValue(evaluationContext, Boolean.class));
    }

    private static Method deleteProjectDeploymentMethod() throws NoSuchMethodException {
        return ProjectDeploymentFacadeImpl.class.getMethod("deleteProjectDeployment", long.class);
    }

    private static Method createProjectDeploymentWorkflowJobMethod() throws NoSuchMethodException {
        return ProjectDeploymentFacadeImpl.class.getMethod(
            "createProjectDeploymentWorkflowJob", Long.class, String.class);
    }

    private static Method updateProjectDeploymentTagsMethod() throws NoSuchMethodException {
        return ProjectDeploymentFacadeImpl.class.getMethod("updateProjectDeploymentTags", long.class, List.class);
    }

    private static Method updateProjectDeploymentMethod() throws NoSuchMethodException {
        return ProjectDeploymentFacadeImpl.class.getMethod("updateProjectDeployment", ProjectDeploymentDTO.class);
    }

    private static Method updateProjectDeploymentWorkflowMethod() throws NoSuchMethodException {
        return ProjectDeploymentFacadeImpl.class.getMethod(
            "updateProjectDeploymentWorkflow", ProjectDeploymentWorkflow.class);
    }

    private static Method enableProjectDeploymentMethod() throws NoSuchMethodException {
        return ProjectDeploymentFacadeImpl.class.getMethod("enableProjectDeployment", long.class, boolean.class);
    }

    private static Method enableProjectDeploymentWorkflowMethod() throws NoSuchMethodException {
        return ProjectDeploymentFacadeImpl.class.getMethod(
            "enableProjectDeploymentWorkflow", long.class, String.class, boolean.class);
    }

    private static Method enableProjectDeploymentWorkflowByEnvironmentMethod() throws NoSuchMethodException {
        return ProjectDeploymentFacadeImpl.class.getMethod(
            "enableProjectDeploymentWorkflow", long.class, String.class, boolean.class, Environment.class);
    }

    // The two ids differ deliberately: the REST path supplies them independently, so a guard keyed on the wrong one
    // cannot be caught by a fixture that gives them the same value.
    private static ProjectDeploymentWorkflow projectDeploymentWorkflow() {
        ProjectDeploymentWorkflow projectDeploymentWorkflow = new ProjectDeploymentWorkflow();

        projectDeploymentWorkflow.setId(PROJECT_DEPLOYMENT_WORKFLOW_ID);
        projectDeploymentWorkflow.setProjectDeploymentId(PROJECT_DEPLOYMENT_ID);

        return projectDeploymentWorkflow;
    }

    private static ProjectDeploymentDTO projectDeploymentDTO() {
        return new ProjectDeploymentDTO(
            null, null, null, true, Environment.PRODUCTION, PROJECT_DEPLOYMENT_ID, "deployment", null, null, null, null,
            PROJECT_ID, 1, List.of(), List.of(), 0);
    }
}
