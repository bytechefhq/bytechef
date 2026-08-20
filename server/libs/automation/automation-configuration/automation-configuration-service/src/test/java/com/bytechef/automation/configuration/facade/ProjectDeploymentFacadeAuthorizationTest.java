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

package com.bytechef.automation.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.dto.ProjectDeploymentDTO;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Pins the {@code @PreAuthorize} expressions that close project-deployment IDOR (T22). Per-deployment-id operations
 * resolve the owning workspace via the {@code ProjectDeployment:ResourceRole} token (deployment &rarr; project &rarr;
 * workspace); create keys on the project via {@code ProjectScope}; the workspace listing keys on the workspace id.
 * <p>
 * The embedded-only overloads (used by {@code ConnectedUserProjectFacadeImpl} under an embedded principal with no
 * platform workspace) and the internal base overloads MUST stay ungated -- gating them would fail closed for embedded.
 * Negative assertions below lock that in.
 *
 * @author Ivica Cardic
 */
class ProjectDeploymentFacadeAuthorizationTest {

    @Test
    void testCreateProjectDeploymentRequiresWorkflowEditInTheTargetEnvironment() {
        // The whole DTO rather than its projectId, and the two-argument hasPermission form rather than the
        // three-argument one: creating a deployment is a promotion, so AutomationPermissionEvaluator reads the target
        // environment off the object and checks the role held THERE. The two-argument form is what lets the object
        // through as itself -- the three-argument form casts its first argument to Serializable, which this record is
        // not. Changing either half silently reverts the check to the source environment.
        assertExpression(
            "hasPermission(#projectDeploymentDTO, 'WORKFLOW_EDIT')",
            "createProjectDeployment", ProjectDeploymentDTO.class);
    }

    @Test
    void testCreateProjectDeploymentWorkflowJobRequiresEditor() {
        assertExpression(
            "hasPermission(#id, 'ProjectDeployment', 'DEPLOYMENT_EDIT')",
            "createProjectDeploymentWorkflowJob", Long.class, String.class);
    }

    @Test
    void testDeleteProjectDeploymentRequiresEditor() {
        assertExpression(
            "hasPermission(#id, 'ProjectDeployment', 'DEPLOYMENT_EDIT')",
            "deleteProjectDeployment", long.class);
    }

    @Test
    void testEnableProjectDeploymentRequiresEditor() {
        assertExpression(
            "hasPermission(#projectDeploymentId, 'ProjectDeployment', 'DEPLOYMENT_EDIT')",
            "enableProjectDeployment", long.class, boolean.class);
    }

    @Test
    void testEnableProjectDeploymentWorkflowRequiresEditor() {
        assertExpression(
            "hasPermission(#projectDeploymentId, 'ProjectDeployment', 'DEPLOYMENT_EDIT')",
            "enableProjectDeploymentWorkflow", long.class, String.class, boolean.class);
    }

    @Test
    void testGetProjectDeploymentRequiresViewer() {
        assertExpression(
            "hasPermission(#id, 'ProjectDeployment', 'DEPLOYMENT_VIEW')",
            "getProjectDeployment", long.class);
    }

    @Test
    void testGetProjectDeploymentTagsRequiresWorkspaceViewer() {
        assertExpression(
            "hasPermission(#workspaceId, 'Workspace', 'DEPLOYMENT_VIEW')",
            "getProjectDeploymentTags", long.class);
    }

    /**
     * The by-id read behind the {@code projectDeploymentWorkflow} GraphQL query, which had no gate anywhere until this
     * one: a root query keyed by the same string that is the path segment of the workflow's public static webhook URL,
     * returning the deployment's inputs and connection bindings and, through {@code projectWorkflow.workflow}, the
     * whole workflow definition — for any deployment in the tenant.
     *
     * <p>
     * Both conjuncts matter and neither is redundant. A custom role carries an arbitrary set of scope names, so
     * {@code DEPLOYMENT_VIEW} without {@code WORKFLOW_VIEW} is constructible and would read the definition; the reverse
     * would read the inputs and connections. Dropping either half is the whole finding again in miniature, which is why
     * the expression is pinned character for character rather than merely asserted to exist.
     *
     * <p>
     * Both are keyed on {@code 'ProjectDeployment'} rather than the sibling's {@code 'ProjectWorkflow'} because that is
     * the stronger spelling, not the cheaper one: a deployment has a {@code ResourceEnvironmentResolver} and a project
     * workflow does not, so this is answered by the role held in the deployment's own environment instead of the union
     * over the environments the caller can reach. The visibility precondition is the same either way.
     */
    @Test
    void testGetProjectDeploymentWorkflowRequiresDeploymentViewAndWorkflowView() {
        assertExpression(
            "hasPermission(#workflowExecutionId.jobPrincipalId, 'ProjectDeployment', 'DEPLOYMENT_VIEW') and " +
                "hasPermission(#workflowExecutionId.jobPrincipalId, 'ProjectDeployment', 'WORKFLOW_VIEW')",
            "getProjectDeploymentWorkflow", WorkflowExecutionId.class);
    }

    /**
     * {@code WORKFLOW_VIEW} rather than the {@code DEPLOYMENT_VIEW} its neighbour below uses: what this listing
     * discloses is project names and workflow labels, and it is the scope the by-id read each of its rows leads to asks
     * for. Both are VIEWER-rank, so no workspace member loses a row. The listing used to be assembled in
     * {@code ProjectDeploymentWorkflowGraphQlController} out of services, past this facade and past any gate.
     */
    @Test
    void testGetWorkspaceChatWorkflowsRequiresWorkspaceWorkflowViewer() {
        assertExpression(
            "hasPermission(#workspaceId, 'Workspace', 'WORKFLOW_VIEW')",
            "getWorkspaceChatWorkflows", long.class, long.class);
    }

    @Test
    void testGetWorkspaceProjectDeploymentsRequiresWorkspaceViewer() {
        assertExpression(
            "hasPermission(#id, 'Workspace', 'DEPLOYMENT_VIEW')",
            "getWorkspaceProjectDeployments", long.class, Long.class, Long.class, Long.class, boolean.class);
    }

    /**
     * The entity-returning overload the GraphQL listing delegates to. Same scope as the DTO overload above, because it
     * discloses the same rows; it exists only because the GraphQL {@code ProjectDeployment} type is assembled from the
     * domain object rather than from the DTO.
     */
    @Test
    void testGetWorkspaceProjectDeploymentRowsRequiresWorkspaceViewer() {
        assertExpression(
            "hasPermission(#workspaceId, 'Workspace', 'DEPLOYMENT_VIEW')",
            "getWorkspaceProjectDeployments", long.class, long.class, Long.class, Long.class);
    }

    @Test
    void testUpdateProjectDeploymentRequiresEditor() {
        assertExpression(
            "hasPermission(#projectDeploymentDTO.id, 'ProjectDeployment', 'DEPLOYMENT_EDIT')",
            "updateProjectDeployment", ProjectDeploymentDTO.class);
    }

    @Test
    void testUpdateProjectDeploymentTagsRequiresEditor() {
        assertExpression(
            "hasPermission(#id, 'ProjectDeployment', 'DEPLOYMENT_EDIT')",
            "updateProjectDeploymentTags", long.class, List.class);
    }

    @Test
    void testUpdateProjectDeploymentWorkflowRequiresEditor() {
        assertExpression(
            "hasPermission(#projectDeploymentWorkflow.projectDeploymentId, 'ProjectDeployment', 'DEPLOYMENT_EDIT')",
            "updateProjectDeploymentWorkflow", ProjectDeploymentWorkflow.class);
    }

    @Test
    void testEmbeddedCreateOverloadIsNotGated() {
        assertNotGated("createProjectDeployment", ProjectDeployment.class, String.class, List.class);
    }

    @Test
    void testEmbeddedUpdateOverloadIsNotGated() {
        assertNotGated(
            "updateProjectDeployment", long.class, int.class, String.class, List.class, Long.class);
    }

    @Test
    void testEmbeddedEnableWorkflowOverloadIsNotGated() {
        assertNotGated(
            "enableProjectDeploymentWorkflow", long.class, String.class, boolean.class, Environment.class);
    }

    private static void assertExpression(String expression, String methodName, Class<?>... parameterTypes) {
        Method method = findMethod(methodName, parameterTypes);

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize)
            .as("@PreAuthorize on %s", methodName)
            .isNotNull();
        assertThat(preAuthorize.value()).isEqualTo(expression);
    }

    private static void assertNotGated(String methodName, Class<?>... parameterTypes) {
        Method method = findMethod(methodName, parameterTypes);

        assertThat(method.isAnnotationPresent(PreAuthorize.class))
            .as("embedded/internal overload %s must NOT carry @PreAuthorize", methodName)
            .isFalse();
    }

    private static Method findMethod(String methodName, Class<?>... parameterTypes) {
        try {
            return ProjectDeploymentFacadeImpl.class.getDeclaredMethod(methodName, parameterTypes);
        } catch (NoSuchMethodException exception) {
            throw new AssertionError("method " + methodName + " not found", exception);
        }
    }
}
