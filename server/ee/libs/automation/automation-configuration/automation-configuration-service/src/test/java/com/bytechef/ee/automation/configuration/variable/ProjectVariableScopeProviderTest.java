/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.variable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.ee.platform.variable.domain.VariableScope;
import com.bytechef.platform.constant.PlatformType;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 */
class ProjectVariableScopeProviderTest {

    private ProjectDeploymentService projectDeploymentService;
    private ProjectService projectService;
    private ProjectWorkflowService projectWorkflowService;
    private ProjectVariableScopeProvider provider;

    @BeforeEach
    void beforeEach() {
        projectDeploymentService = mock(ProjectDeploymentService.class);
        projectService = mock(ProjectService.class);
        projectWorkflowService = mock(ProjectWorkflowService.class);
        provider = new ProjectVariableScopeProvider(projectDeploymentService, projectService, projectWorkflowService);
    }

    @Test
    void testTypeIsAutomation() {
        assertThat(provider.getType()).isEqualTo(PlatformType.AUTOMATION);
    }

    @Test
    void testScopeForJobPrincipalWalksDeploymentToProjectWorkspace() {
        ProjectDeployment projectDeployment = mock(ProjectDeployment.class);
        Project project = mock(Project.class);

        when(projectDeploymentService.getProjectDeployment(42L)).thenReturn(projectDeployment);
        when(projectDeployment.getProjectId()).thenReturn(3L);
        when(projectService.getProject(3L)).thenReturn(project);
        when(project.getWorkspaceId()).thenReturn(7L);

        assertThat(provider.getVariableScope(42L)).contains(VariableScope.workspace(7L));
    }

    @Test
    void testScopeForJobPrincipalIsEmptyWhenProjectHasNoWorkspace() {
        ProjectDeployment projectDeployment = mock(ProjectDeployment.class);
        Project project = mock(Project.class);

        when(projectDeploymentService.getProjectDeployment(42L)).thenReturn(projectDeployment);
        when(projectDeployment.getProjectId()).thenReturn(3L);
        when(projectService.getProject(3L)).thenReturn(project);
        when(project.getWorkspaceId()).thenReturn(null);

        assertThat(provider.getVariableScope(42L)).isEmpty();
    }

    @Test
    void testScopeByWorkflowIdIsEmptyForNonProjectWorkflow() {
        when(projectWorkflowService.fetchWorkflowProjectWorkflow("wf-1")).thenReturn(Optional.empty());

        assertThat(provider.getVariableScopeByWorkflowId("wf-1")).isEmpty();
    }

    @Test
    void testScopeByWorkflowIdResolvesProjectWorkspace() {
        ProjectWorkflow projectWorkflow = mock(ProjectWorkflow.class);
        Project project = mock(Project.class);

        when(projectWorkflowService.fetchWorkflowProjectWorkflow("wf-1")).thenReturn(Optional.of(projectWorkflow));
        when(projectWorkflow.getProjectId()).thenReturn(3L);
        when(projectService.getProject(3L)).thenReturn(project);
        when(project.getWorkspaceId()).thenReturn(7L);

        assertThat(provider.getVariableScopeByWorkflowId("wf-1")).contains(VariableScope.workspace(7L));
    }
}
