/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProject;
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectDTO;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserWorkflowTemplateDTO;
import com.bytechef.ee.embedded.configuration.exception.CodeWorkflowNotCopyableException;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectWorkflowService;
import com.bytechef.platform.configuration.domain.Environment;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ConnectedUserProjectFacadeTest {

    private static final String EXTERNAL_USER_ID = "ext-user-1";
    private static final String WORKFLOW_UUID = "7d3c1a52-9b0e-4f7a-8c61-2e5f4b9d0a13";

    private final AutomationWorkflowProjectFacade automationWorkflowProjectFacade =
        mock(AutomationWorkflowProjectFacade.class);
    private final ConnectedUserProjectWorkflowManager connectedUserProjectWorkflowManager =
        mock(ConnectedUserProjectWorkflowManager.class);
    private final ConnectedUserProjectWorkflowService connectedUserProjectWorkflowService =
        mock(ConnectedUserProjectWorkflowService.class);
    private final ProjectWorkflowService projectWorkflowService = mock(ProjectWorkflowService.class);
    private final WorkflowService workflowService = mock(WorkflowService.class);

    private final ConnectedUserProjectFacadeImpl connectedUserProjectFacade = new ConnectedUserProjectFacadeImpl(
        null, automationWorkflowProjectFacade, null, null, null, connectedUserProjectWorkflowManager, null,
        connectedUserProjectWorkflowService, null, null, null, null, null, null, null, null, null, null, null, null,
        null,
        null, null, null, null, null, projectWorkflowService, null,
        null, workflowService, null, null);

    @Test
    void testCopyWorkflowTemplateCopiesAVisualTemplate() {
        givenCatalogProject(false);

        ConnectedUserProject connectedUserProject = new ConnectedUserProject();

        connectedUserProject.setId(1L);

        when(connectedUserProjectWorkflowManager.getOrCreateConnectedUserProject(EXTERNAL_USER_ID,
            Environment.PRODUCTION))
                .thenReturn(connectedUserProject);
        when(projectWorkflowService.getProjectWorkflows(1L, 1))
            .thenReturn(List.of(new ProjectWorkflow(1L, 1, "workflow-1", UUID.fromString(WORKFLOW_UUID))));
        when(workflowService.getWorkflow("workflow-1"))
            .thenReturn(new Workflow("{\"tasks\":[]}", Workflow.Format.JSON));
        when(connectedUserProjectWorkflowManager.createProjectWorkflow(
            eq(EXTERNAL_USER_ID), any(), eq(Environment.PRODUCTION), eq(WORKFLOW_UUID)))
                .thenReturn("copy-uuid");

        String copyWorkflowUuid = connectedUserProjectFacade.copyWorkflowTemplate(
            EXTERNAL_USER_ID, WORKFLOW_UUID, Environment.PRODUCTION);

        assertThat(copyWorkflowUuid).isEqualTo("copy-uuid");
    }

    @Test
    void testCopyWorkflowTemplateRejectsACodeWorkflowTemplate() {
        givenCatalogProject(true);

        assertThatThrownBy(
            () -> connectedUserProjectFacade.copyWorkflowTemplate(
                EXTERNAL_USER_ID, WORKFLOW_UUID, Environment.PRODUCTION))
                    .isInstanceOf(CodeWorkflowNotCopyableException.class);

        verify(connectedUserProjectWorkflowManager, never())
            .createProjectWorkflow(any(), any(), any(), any());
    }

    private void givenCatalogProject(boolean codeWorkflowProject) {
        ConnectedUserWorkflowTemplateDTO workflowTemplate = new ConnectedUserWorkflowTemplateDTO(
            WORKFLOW_UUID, "Label", "Description", null, List.of(), List.of(), List.of(), null);

        when(automationWorkflowProjectFacade.getPublishedProjects(EXTERNAL_USER_ID, Environment.PRODUCTION))
            .thenReturn(
                List.of(
                    new AutomationWorkflowProjectDTO(
                        1L, "Catalog", "", null, List.of(), true, 1, 1, List.of(workflowTemplate), null,
                        codeWorkflowProject, true, 0, null)));
    }
}
