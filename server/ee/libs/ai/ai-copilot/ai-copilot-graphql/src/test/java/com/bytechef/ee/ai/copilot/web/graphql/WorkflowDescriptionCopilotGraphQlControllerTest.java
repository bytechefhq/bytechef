/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.web.graphql;

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
import com.bytechef.ee.ai.copilot.web.graphql.WorkflowDescriptionCopilotGraphQlController.GenerateWorkflowDescriptionInput;
import com.bytechef.ee.ai.copilot.web.graphql.WorkflowDescriptionCopilotGraphQlController.GenerateWorkflowDescriptionPayload;
import com.bytechef.ee.ai.copilot.workflow.WorkflowDescriptionCopilotGenerator;
import com.bytechef.ee.ai.copilot.workflow.WorkflowDescriptionCopilotResult;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class WorkflowDescriptionCopilotGraphQlControllerTest {

    private final PermissionService permissionService = mock(PermissionService.class);
    private final ProjectWorkflowService projectWorkflowService = mock(ProjectWorkflowService.class);
    private final WorkflowDescriptionCopilotGenerator generator = mock(WorkflowDescriptionCopilotGenerator.class);

    private final WorkflowDescriptionCopilotGraphQlController controller =
        new WorkflowDescriptionCopilotGraphQlController(
            permissionService, projectWorkflowService, Optional.of(generator));

    @Test
    void testGenerateDeniedWhenUserLacksWorkflowViewScope() {
        givenWorkflowProject(42L);

        when(permissionService.hasWorkspaceScopeForProject(42L, "WORKFLOW_VIEW")).thenReturn(false);

        assertThatThrownBy(() -> controller.generateWorkflowDescription(input()))
            .isInstanceOf(AccessDeniedException.class);

        verify(generator, never()).generate(any());
    }

    @Test
    void testGenerateAllowedWhenUserHasWorkflowViewScope() {
        givenWorkflowProject(42L);

        when(permissionService.hasWorkspaceScopeForProject(42L, "WORKFLOW_VIEW")).thenReturn(true);
        when(generator.generate(any())).thenReturn(new WorkflowDescriptionCopilotResult("Syncs records."));

        GenerateWorkflowDescriptionPayload payload = controller.generateWorkflowDescription(input());

        assertThat(payload.value()).isEqualTo("Syncs records.");
    }

    private void givenWorkflowProject(long projectId) {
        ProjectWorkflow projectWorkflow = mock(ProjectWorkflow.class);

        when(projectWorkflow.getProjectId()).thenReturn(projectId);
        when(projectWorkflowService.getWorkflowProjectWorkflow("wf1")).thenReturn(projectWorkflow);
    }

    private static GenerateWorkflowDescriptionInput input() {
        return new GenerateWorkflowDescriptionInput("wf1", null, 0);
    }
}
