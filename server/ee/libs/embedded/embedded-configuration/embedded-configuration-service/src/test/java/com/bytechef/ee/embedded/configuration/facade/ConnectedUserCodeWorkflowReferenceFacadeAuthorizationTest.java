/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProject;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProjectWorkflow;
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectDTO;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserWorkflowTemplateDTO;
import com.bytechef.ee.embedded.configuration.repository.ConnectedUserProjectWorkflowConnectionRepository;
import com.bytechef.ee.embedded.configuration.repository.ConnectedUserProjectWorkflowRepository;
import com.bytechef.platform.configuration.domain.Environment;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Provisioning-time authorization for {@link ConnectedUserCodeWorkflowReferenceFacadeImpl#getOrCreateReference}, the
 * reference-mode twin of {@link ConnectedUserProjectFacadeImpl#copyWorkflowTemplate}: a caller-supplied
 * {@code catalogWorkflowUuid} is provisioned only if it belongs to a template the PERMISSION-FILTERED catalog would
 * show the requesting connected user.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class ConnectedUserCodeWorkflowReferenceFacadeAuthorizationTest {

    private static final String EXTERNAL_USER_ID = "ext-user-1";
    private static final String HIDDEN_WORKFLOW_UUID = "hidden-uuid";
    private static final String UNKNOWN_WORKFLOW_UUID = "no-such-uuid";
    private static final String VISIBLE_WORKFLOW_UUID = "visible-uuid";

    private final AutomationWorkflowProjectFacade automationWorkflowProjectFacade =
        mock(AutomationWorkflowProjectFacade.class);
    private final ConnectedUserProjectWorkflowConnectionRepository connectedUserProjectWorkflowConnectionRepository =
        mock(ConnectedUserProjectWorkflowConnectionRepository.class);
    private final ConnectedUserProjectWorkflowRepository connectedUserProjectWorkflowRepository =
        mock(ConnectedUserProjectWorkflowRepository.class);
    private final ConnectedUserProjectWorkflowManager connectedUserProjectWorkflowManager =
        mock(ConnectedUserProjectWorkflowManager.class);
    private final ConnectedUserWorkflowConnectionResolver connectedUserWorkflowConnectionResolver =
        mock(ConnectedUserWorkflowConnectionResolver.class);
    private final ProjectDeploymentFacade projectDeploymentFacade = mock(ProjectDeploymentFacade.class);
    private final ProjectDeploymentService projectDeploymentService = mock(ProjectDeploymentService.class);
    private final ProjectDeploymentWorkflowService projectDeploymentWorkflowService =
        mock(ProjectDeploymentWorkflowService.class);
    private final ProjectWorkflowService projectWorkflowService = mock(ProjectWorkflowService.class);
    private final WorkflowService workflowService = mock(WorkflowService.class);

    private final ConnectedUserCodeWorkflowReferenceFacadeImpl facade =
        new ConnectedUserCodeWorkflowReferenceFacadeImpl(
            automationWorkflowProjectFacade, connectedUserProjectWorkflowConnectionRepository,
            connectedUserProjectWorkflowRepository, connectedUserProjectWorkflowManager,
            connectedUserWorkflowConnectionResolver, projectDeploymentFacade, projectDeploymentService,
            projectDeploymentWorkflowService, projectWorkflowService, workflowService);

    @Test
    void testGetOrCreateReferenceProvisionsATemplateTheConnectedUserIsPermittedToSee() {
        givenVisibleCatalogContains(VISIBLE_WORKFLOW_UUID);
        givenNoExistingReference();
        givenCatalogWorkflow(VISIBLE_WORKFLOW_UUID);

        ConnectedUserProjectWorkflow reference = facade.getOrCreateReference(
            EXTERNAL_USER_ID, VISIBLE_WORKFLOW_UUID, Environment.PRODUCTION);

        assertThat(reference.getCatalogWorkflowUuid()).isEqualTo(VISIBLE_WORKFLOW_UUID);
        assertThat(reference.isEnabled()).isTrue();
    }

    @Test
    void testGetOrCreateReferenceRejectsATemplateHiddenByThePermissionExpression() {
        givenVisibleCatalogContains(VISIBLE_WORKFLOW_UUID);
        givenNoExistingReference();

        assertThatThrownBy(
            () -> facade.getOrCreateReference(EXTERNAL_USER_ID, HIDDEN_WORKFLOW_UUID, Environment.PRODUCTION))
                .isInstanceOf(IllegalArgumentException.class);

        // Nothing was provisioned: no deployment, no reference row, no connection wiring.
        verifyNoInteractions(projectDeploymentFacade, projectDeploymentService, connectedUserWorkflowConnectionResolver,
            connectedUserProjectWorkflowConnectionRepository);

        verify(connectedUserProjectWorkflowRepository, never())
            .save(any());
    }

    @Test
    void testHiddenTemplateRejectionIsIndistinguishableFromAnUnknownUuid() {
        givenVisibleCatalogContains(VISIBLE_WORKFLOW_UUID);
        givenNoExistingReference();

        Throwable hiddenThrowable = catchThrowable(
            () -> facade.getOrCreateReference(EXTERNAL_USER_ID, HIDDEN_WORKFLOW_UUID, Environment.PRODUCTION));
        Throwable unknownThrowable = catchThrowable(
            () -> facade.getOrCreateReference(EXTERNAL_USER_ID, UNKNOWN_WORKFLOW_UUID, Environment.PRODUCTION));

        assertThat(hiddenThrowable).isExactlyInstanceOf(IllegalArgumentException.class);
        assertThat(unknownThrowable).isExactlyInstanceOf(IllegalArgumentException.class);
        assertThat(hiddenThrowable).hasMessage(rejectionMessage(HIDDEN_WORKFLOW_UUID));
        assertThat(unknownThrowable).hasMessage(rejectionMessage(UNKNOWN_WORKFLOW_UUID));
    }

    /**
     * The check gates PROVISIONING, not invocation: an already-provisioned reference is returned untouched, so a vendor
     * narrowing a permission expression cannot break automations that are already running.
     */
    @Test
    void testAlreadyProvisionedReferenceIsReturnedWithoutConsultingTheCatalog() {
        ConnectedUserProject connectedUserProject = new ConnectedUserProject();

        connectedUserProject.setId(10L);

        when(connectedUserProjectWorkflowManager.getOrCreateConnectedUserProject(
            EXTERNAL_USER_ID, Environment.PRODUCTION))
                .thenReturn(connectedUserProject);

        ConnectedUserProjectWorkflow existingReference = new ConnectedUserProjectWorkflow();

        existingReference.setId(1L);

        when(connectedUserProjectWorkflowRepository
            .findByConnectedUserProjectIdAndCatalogWorkflowUuid(10L, HIDDEN_WORKFLOW_UUID))
                .thenReturn(Optional.of(existingReference));

        ConnectedUserProjectWorkflow reference = facade.getOrCreateReference(
            EXTERNAL_USER_ID, HIDDEN_WORKFLOW_UUID, Environment.PRODUCTION);

        assertThat(reference).isSameAs(existingReference);

        verifyNoInteractions(automationWorkflowProjectFacade);
    }

    private void givenNoExistingReference() {
        ConnectedUserProject connectedUserProject = new ConnectedUserProject();

        connectedUserProject.setId(10L);

        when(connectedUserProjectWorkflowManager.getOrCreateConnectedUserProject(anyString(), any()))
            .thenReturn(connectedUserProject);
        when(connectedUserProjectWorkflowRepository
            .findByConnectedUserProjectIdAndCatalogWorkflowUuid(anyLong(), anyString()))
                .thenReturn(Optional.empty());
    }

    private void givenCatalogWorkflow(String workflowUuid) {
        Workflow workflow = new Workflow("{\"triggers\":[],\"tasks\":[]}", Workflow.Format.JSON);

        when(projectWorkflowService.getLastPublishedWorkflowId(workflowUuid)).thenReturn("workflow-1");
        when(projectWorkflowService.getWorkflowProjectWorkflow("workflow-1"))
            .thenReturn(new ProjectWorkflow(500L, 1, "workflow-1"));
        when(workflowService.getWorkflow("workflow-1")).thenReturn(workflow);
        when(connectedUserWorkflowConnectionResolver.resolve(workflow.getDefinition())).thenReturn(Map.of());
        when(projectDeploymentService.fetchProjectDeploymentByName(anyLong(), anyString()))
            .thenReturn(Optional.empty());
        when(projectDeploymentFacade.createProjectDeployment(
            any(ProjectDeployment.class), anyString(), anyList()))
                .thenReturn(900L);
        when(connectedUserProjectWorkflowRepository.save(any()))
            .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void givenVisibleCatalogContains(String... workflowUuids) {
        when(automationWorkflowProjectFacade.getPublishedProjects(EXTERNAL_USER_ID, Environment.PRODUCTION))
            .thenReturn(List.of(catalogProject(workflowUuids)));
    }

    private static AutomationWorkflowProjectDTO catalogProject(String... workflowUuids) {
        List<ConnectedUserWorkflowTemplateDTO> workflowTemplates = Stream.of(workflowUuids)
            .map(workflowUuid -> new ConnectedUserWorkflowTemplateDTO(
                workflowUuid, "Label", "Description", null, List.of(), List.of(), null))
            .toList();

        return new AutomationWorkflowProjectDTO(
            1L, "Catalog", "", null, List.of(), true, 1, 1, workflowTemplates, null, false);
    }

    private static String rejectionMessage(String workflowUuid) {
        return "Not a published catalog workflow template: " + workflowUuid;
    }
}
