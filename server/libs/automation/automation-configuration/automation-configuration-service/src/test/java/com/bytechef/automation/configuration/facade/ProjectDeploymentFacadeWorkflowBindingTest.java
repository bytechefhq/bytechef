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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.config.ApplicationProperties;
import java.util.List;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.access.AccessDeniedException;

/**
 * Pins the binding between the deployment {@code updateProjectDeploymentWorkflow}'s {@code @PreAuthorize} AUTHORIZES
 * and the row it WRITES.
 *
 * <p>
 * The gate reads {@code #projectDeploymentWorkflow.projectDeploymentId}; the write is keyed by {@code getId()}, a
 * different field of the same caller-supplied object, and {@code ProjectDeploymentWorkflowServiceImpl.update} resolves
 * the row by that id with no gate of its own. The two path segments of
 * {@code PUT /project-deployments/{id}/project-deployment-workflows/{projectDeploymentWorkflowId}} bind those two
 * fields independently, so a caller holding {@code DEPLOYMENT_EDIT} on any ONE deployment could name their own in the
 * first segment and any row in the tenant in the second.
 *
 * <p>
 * A direct call cannot fire the annotation, so what these tests exercise is the half the annotation cannot express:
 * given that the caller was authorized for deployment A, the facade must refuse to write a row belonging to deployment
 * B. The expression itself is pinned next door in {@link ProjectDeploymentFacadeAuthorizationTest}.
 *
 * @author Ivica Cardic
 */
class ProjectDeploymentFacadeWorkflowBindingTest {

    private static final long OWN_PROJECT_DEPLOYMENT_ID = 1L;
    private static final long OTHER_PROJECT_DEPLOYMENT_ID = 2L;
    private static final long PROJECT_DEPLOYMENT_WORKFLOW_ID = 30L;

    private final ProjectDeploymentWorkflowService projectDeploymentWorkflowService =
        Mockito.mock(ProjectDeploymentWorkflowService.class);

    /**
     * The confused deputy. The caller was authorized for deployment A and named a row that belongs to deployment B;
     * before the binding check the facade wrote it, overwriting B's connections, inputs, workflowId and enabled flag.
     * {@code enabled = false} is the sharpest form of it: {@code validateProjectDeploymentWorkflow} short-circuits
     * entirely on a disabled row, so disabling somebody else's production workflow reached the write with no validation
     * of any kind in between.
     */
    @Test
    void testRefusesToWriteARowBelongingToAnotherProjectDeployment() {
        Mockito.when(projectDeploymentWorkflowService.getProjectDeploymentWorkflow(PROJECT_DEPLOYMENT_WORKFLOW_ID))
            .thenReturn(persistedProjectDeploymentWorkflow(OTHER_PROJECT_DEPLOYMENT_ID));

        assertThatThrownBy(
            () -> createProjectDeploymentFacade().updateProjectDeploymentWorkflow(
                submittedProjectDeploymentWorkflow(OWN_PROJECT_DEPLOYMENT_ID)))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage("Project deployment workflow 30 does not belong to project deployment 1");

        Mockito.verify(projectDeploymentWorkflowService, Mockito.never())
            .update(Mockito.any(ProjectDeploymentWorkflow.class));
    }

    /**
     * The over-deny guard. A row that really does belong to the authorized deployment still writes, so the check
     * refuses a mismatch rather than the operation.
     */
    @Test
    void testWritesARowBelongingToTheAuthorizedProjectDeployment() {
        Mockito.when(projectDeploymentWorkflowService.getProjectDeploymentWorkflow(PROJECT_DEPLOYMENT_WORKFLOW_ID))
            .thenReturn(persistedProjectDeploymentWorkflow(OWN_PROJECT_DEPLOYMENT_ID));

        ProjectDeploymentWorkflow projectDeploymentWorkflow = submittedProjectDeploymentWorkflow(
            OWN_PROJECT_DEPLOYMENT_ID);

        createProjectDeploymentFacade().updateProjectDeploymentWorkflow(projectDeploymentWorkflow);

        Mockito.verify(projectDeploymentWorkflowService)
            .update(projectDeploymentWorkflow);
    }

    /**
     * An unknown row id is denied rather than surfacing the {@code NoSuchElementException}
     * {@code ProjectDeploymentWorkflowServiceImpl.getProjectDeploymentWorkflow} throws, and with the SAME message a
     * mismatched row gets -- so the difference between "not yours" and "does not exist" is not readable from the
     * response.
     */
    @Test
    void testDeniesAnUnknownRowWithTheSameMessageAsAMismatchedOne() {
        Mockito.when(projectDeploymentWorkflowService.getProjectDeploymentWorkflow(PROJECT_DEPLOYMENT_WORKFLOW_ID))
            .thenThrow(new NoSuchElementException("No value present"));

        assertThatThrownBy(
            () -> createProjectDeploymentFacade().updateProjectDeploymentWorkflow(
                submittedProjectDeploymentWorkflow(OWN_PROJECT_DEPLOYMENT_ID)))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage("Project deployment workflow 30 does not belong to project deployment 1");

        Mockito.verify(projectDeploymentWorkflowService, Mockito.never())
            .update(Mockito.any(ProjectDeploymentWorkflow.class));
    }

    /**
     * A submitted object carrying no row id at all is denied before anything is looked up. The write would otherwise
     * reach {@code Validate.notNull(projectDeploymentWorkflow.getId(), "id")} inside the service and fail as a 500.
     */
    @Test
    void testDeniesASubmissionWithNoRowId() {
        ProjectDeploymentWorkflow projectDeploymentWorkflow = submittedProjectDeploymentWorkflow(
            OWN_PROJECT_DEPLOYMENT_ID);

        projectDeploymentWorkflow.setId(null);

        assertThatThrownBy(
            () -> createProjectDeploymentFacade().updateProjectDeploymentWorkflow(projectDeploymentWorkflow))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Project deployment workflow null does not belong to project deployment 1");

        Mockito.verifyNoInteractions(projectDeploymentWorkflowService);
    }

    /**
     * The binding is checked BEFORE the workflow's own validation, which is what lets every case above run with a null
     * {@code WorkflowService}: a mismatched row never reaches {@code validateProjectDeploymentWorkflow}, so it cannot
     * be the thing that rejected it, and a green here is the binding check rather than an incidental NPE.
     */
    @Test
    void testChecksTheBindingBeforeValidatingTheWorkflow() {
        Mockito.when(projectDeploymentWorkflowService.getProjectDeploymentWorkflow(PROJECT_DEPLOYMENT_WORKFLOW_ID))
            .thenReturn(persistedProjectDeploymentWorkflow(OTHER_PROJECT_DEPLOYMENT_ID));

        ProjectDeploymentWorkflow projectDeploymentWorkflow = submittedProjectDeploymentWorkflow(
            OWN_PROJECT_DEPLOYMENT_ID);

        projectDeploymentWorkflow.setEnabled(true);

        assertThatThrownBy(
            () -> createProjectDeploymentFacade().updateProjectDeploymentWorkflow(projectDeploymentWorkflow))
                .isInstanceOf(AccessDeniedException.class);

        assertThat(projectDeploymentWorkflow.isEnabled()).isTrue();
    }

    /**
     * Every collaborator but the one under test is null on purpose: a mismatched row must be refused before anything
     * else is consulted, so a null here would surface as an NPE rather than a green if the binding check were moved or
     * removed. {@code ApplicationProperties} is the one exception -- the constructor itself dereferences it.
     */
    private ProjectDeploymentFacadeImpl createProjectDeploymentFacade() {
        return new ProjectDeploymentFacadeImpl(
            null, null, null, null, null, null, null, null, List.of(), null, projectDeploymentWorkflowService,
            null, null, null, null, null, null, null, Mockito.mock(ApplicationProperties.class), null, null);
    }

    /**
     * What the controller hands the facade: the row id from one path segment and the deployment id from the other, with
     * no relationship between them beyond what the caller chose.
     */
    private static ProjectDeploymentWorkflow submittedProjectDeploymentWorkflow(long projectDeploymentId) {
        ProjectDeploymentWorkflow projectDeploymentWorkflow = new ProjectDeploymentWorkflow();

        projectDeploymentWorkflow.setId(PROJECT_DEPLOYMENT_WORKFLOW_ID);
        projectDeploymentWorkflow.setProjectDeploymentId(projectDeploymentId);
        projectDeploymentWorkflow.setEnabled(false);

        return projectDeploymentWorkflow;
    }

    /**
     * What the database actually holds for that row id -- the deployment it really belongs to.
     */
    private static ProjectDeploymentWorkflow persistedProjectDeploymentWorkflow(long projectDeploymentId) {
        ProjectDeploymentWorkflow projectDeploymentWorkflow = new ProjectDeploymentWorkflow();

        projectDeploymentWorkflow.setId(PROJECT_DEPLOYMENT_WORKFLOW_ID);
        projectDeploymentWorkflow.setProjectDeploymentId(projectDeploymentId);

        return projectDeploymentWorkflow;
    }
}
