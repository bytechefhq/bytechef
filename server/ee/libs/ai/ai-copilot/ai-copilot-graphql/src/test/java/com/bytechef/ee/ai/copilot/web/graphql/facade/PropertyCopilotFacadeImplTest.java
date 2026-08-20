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
import com.bytechef.ee.ai.copilot.property.PropertyCopilotGenerator;
import com.bytechef.ee.ai.copilot.property.PropertyCopilotMode;
import com.bytechef.ee.ai.copilot.property.PropertyCopilotRequest;
import com.bytechef.ee.ai.copilot.property.PropertyCopilotResult;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

/**
 * Pins the {@code WORKFLOW_VIEW} guard that moved off {@code PropertyCopilotGraphQlController}'s method body onto this
 * facade. Deleting the guard makes {@code testGenerateDeniedWhenUserLacksWorkflowViewScope} fail: it asserts both that
 * the call is refused and that the generator is never reached, so a guard removed in favour of "the generator will
 * check" cannot pass here either.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class PropertyCopilotFacadeImplTest {

    private final PermissionService permissionService = mock(PermissionService.class);
    private final ProjectWorkflowService projectWorkflowService = mock(ProjectWorkflowService.class);
    private final PropertyCopilotGenerator propertyCopilotGenerator = mock(PropertyCopilotGenerator.class);

    private final PropertyCopilotFacade propertyCopilotFacade = new PropertyCopilotFacadeImpl(
        permissionService, projectWorkflowService, Optional.of(propertyCopilotGenerator));

    @Test
    void testGenerateDeniedWhenUserLacksWorkflowViewScope() {
        givenWorkflowProject(42L);

        when(permissionService.hasWorkspaceScopeForProject(42L, "WORKFLOW_VIEW")).thenReturn(false);

        assertThatThrownBy(() -> propertyCopilotFacade.generatePropertyValue(request()))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessage("Access denied to workflow wf1");

        verify(propertyCopilotGenerator, never()).generate(any());
    }

    @Test
    void testGenerateAllowedWhenUserHasWorkflowViewScope() {
        givenWorkflowProject(42L);

        when(permissionService.hasWorkspaceScopeForProject(42L, "WORKFLOW_VIEW")).thenReturn(true);
        when(propertyCopilotGenerator.generate(any())).thenReturn(new PropertyCopilotResult("Hello", true, null));

        PropertyCopilotResult result = propertyCopilotFacade.generatePropertyValue(request());

        assertThat(result.value()).isEqualTo("Hello");
        assertThat(result.valid()).isTrue();
    }

    @Test
    void testGenerateRejectedWhenGeneratorIsNotWired() {
        PropertyCopilotFacade facade =
            new PropertyCopilotFacadeImpl(permissionService, projectWorkflowService, Optional.empty());

        assertThatThrownBy(() -> facade.generatePropertyValue(request()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Property Copilot is not enabled");
    }

    private void givenWorkflowProject(long projectId) {
        ProjectWorkflow projectWorkflow = mock(ProjectWorkflow.class);

        when(projectWorkflow.getProjectId()).thenReturn(projectId);
        when(projectWorkflowService.getWorkflowProjectWorkflow("wf1")).thenReturn(projectWorkflow);
    }

    private static PropertyCopilotRequest request() {
        return new PropertyCopilotRequest("greet", PropertyCopilotMode.TEXT, "wf1", "n1", "p", "STRING", true, 0);
    }
}
