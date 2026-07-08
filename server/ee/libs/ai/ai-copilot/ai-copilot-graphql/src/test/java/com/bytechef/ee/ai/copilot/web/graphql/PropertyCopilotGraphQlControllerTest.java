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
import com.bytechef.ee.ai.copilot.property.PropertyCopilotGenerator;
import com.bytechef.ee.ai.copilot.property.PropertyCopilotMode;
import com.bytechef.ee.ai.copilot.property.PropertyCopilotResult;
import com.bytechef.ee.ai.copilot.web.graphql.PropertyCopilotGraphQlController.GeneratePropertyValueInput;
import com.bytechef.ee.ai.copilot.web.graphql.PropertyCopilotGraphQlController.GeneratePropertyValuePayload;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class PropertyCopilotGraphQlControllerTest {

    private final PermissionService permissionService = mock(PermissionService.class);
    private final ProjectWorkflowService projectWorkflowService = mock(ProjectWorkflowService.class);
    private final PropertyCopilotGenerator propertyCopilotGenerator = mock(PropertyCopilotGenerator.class);

    private final PropertyCopilotGraphQlController controller = new PropertyCopilotGraphQlController(
        permissionService, projectWorkflowService, Optional.of(propertyCopilotGenerator));

    @Test
    void testGenerateDeniedWhenUserLacksWorkflowViewScope() {
        givenWorkflowProject(42L);

        when(permissionService.hasWorkspaceScopeForProject(42L, "WORKFLOW_VIEW")).thenReturn(false);

        assertThatThrownBy(() -> controller.generatePropertyValue(input()))
            .isInstanceOf(AccessDeniedException.class);

        verify(propertyCopilotGenerator, never()).generate(any());
    }

    @Test
    void testGenerateAllowedWhenUserHasWorkflowViewScope() {
        givenWorkflowProject(42L);

        when(permissionService.hasWorkspaceScopeForProject(42L, "WORKFLOW_VIEW")).thenReturn(true);
        when(propertyCopilotGenerator.generate(any())).thenReturn(new PropertyCopilotResult("Hello", true, null));

        GeneratePropertyValuePayload payload = controller.generatePropertyValue(input());

        assertThat(payload.value()).isEqualTo("Hello");
        assertThat(payload.valid()).isTrue();
    }

    private void givenWorkflowProject(long projectId) {
        ProjectWorkflow projectWorkflow = mock(ProjectWorkflow.class);

        when(projectWorkflow.getProjectId()).thenReturn(projectId);
        when(projectWorkflowService.getWorkflowProjectWorkflow("wf1")).thenReturn(projectWorkflow);
    }

    private static GeneratePropertyValueInput input() {
        return new GeneratePropertyValueInput("greet", PropertyCopilotMode.TEXT, "wf1", "n1", "p", "STRING", true, 0);
    }
}
