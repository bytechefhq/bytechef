/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.web.graphql.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.ee.ai.copilot.workflow.WorkflowDescriptionCopilotGenerator;
import com.bytechef.ee.ai.copilot.workflow.WorkflowDescriptionCopilotRequest;
import com.bytechef.ee.ai.copilot.workflow.WorkflowDescriptionCopilotResult;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

/**
 * Pins the {@code WORKFLOW_VIEW} guard that moved off {@code WorkflowDescriptionCopilotGraphQlController}'s method body
 * onto this facade. Deleting the guard makes {@code testGenerateDeniedWhenUserLacksWorkflowViewScope} fail: it asserts
 * both that the call is refused and that the generator is never reached.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class WorkflowDescriptionCopilotFacadeImplTest {

    private final PermissionService permissionService = mock(PermissionService.class);
    private final ProjectWorkflowService projectWorkflowService = mock(ProjectWorkflowService.class);
    private final WorkflowDescriptionCopilotGenerator generator = mock(WorkflowDescriptionCopilotGenerator.class);

    private final WorkflowDescriptionCopilotFacade workflowDescriptionCopilotFacade =
        new WorkflowDescriptionCopilotFacadeImpl(
            permissionService, projectWorkflowService, Optional.of(generator));

    @Test
    void testGenerateDeniedWhenUserLacksWorkflowViewScope() {
        givenWorkflowProject(42L);

        when(permissionService.hasWorkspaceScopeForProject(42L, "WORKFLOW_VIEW")).thenReturn(false);

        assertThatThrownBy(() -> workflowDescriptionCopilotFacade.generateWorkflowDescription(request()))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessage("Access denied to workflow wf1");

        verify(generator, never()).generate(any());
    }

    @Test
    void testGenerateAllowedWhenUserHasWorkflowViewScope() {
        givenWorkflowProject(42L);

        when(permissionService.hasWorkspaceScopeForProject(42L, "WORKFLOW_VIEW")).thenReturn(true);
        when(generator.generate(any())).thenReturn(new WorkflowDescriptionCopilotResult("Syncs records."));

        WorkflowDescriptionCopilotResult result =
            workflowDescriptionCopilotFacade.generateWorkflowDescription(request());

        assertThat(result.value()).isEqualTo("Syncs records.");
    }

    @Test
    void testGenerateRejectedWhenGeneratorIsNotWired() {
        WorkflowDescriptionCopilotFacade facade = new WorkflowDescriptionCopilotFacadeImpl(
            permissionService, projectWorkflowService, Optional.empty());

        assertThatThrownBy(() -> facade.generateWorkflowDescription(request()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Workflow Description Copilot is not enabled");
    }

    private void givenWorkflowProject(long projectId) {
        ProjectWorkflow projectWorkflow = mock(ProjectWorkflow.class);

        when(projectWorkflow.getProjectId()).thenReturn(projectId);
        when(projectWorkflowService.getWorkflowProjectWorkflow("wf1")).thenReturn(projectWorkflow);
    }

    private static WorkflowDescriptionCopilotRequest request() {
        return new WorkflowDescriptionCopilotRequest("wf1", null, 0);
    }
}
