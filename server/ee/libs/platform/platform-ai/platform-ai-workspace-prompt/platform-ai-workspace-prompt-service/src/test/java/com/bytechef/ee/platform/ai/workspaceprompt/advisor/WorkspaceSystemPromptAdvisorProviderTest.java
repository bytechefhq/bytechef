/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.workspaceprompt.advisor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.ee.platform.ai.workspaceprompt.WorkspaceSystemPrompts;
import com.bytechef.platform.constant.PlatformType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

/**
 * @version ee
 */
class WorkspaceSystemPromptAdvisorProviderTest {

    private final WorkspaceSystemPrompts workspaceSystemPrompts = mock(WorkspaceSystemPrompts.class);
    private final ProjectDeploymentService projectDeploymentService = mock(ProjectDeploymentService.class);
    private final ProjectService projectService = mock(ProjectService.class);

    @SuppressWarnings("unchecked")
    private WorkspaceSystemPromptAdvisorProviderImpl provider() {
        ObjectProvider<ProjectDeploymentService> deploymentProvider = mock(ObjectProvider.class);
        ObjectProvider<ProjectService> projectProvider = mock(ObjectProvider.class);

        when(deploymentProvider.getIfAvailable()).thenReturn(projectDeploymentService);
        when(projectProvider.getIfAvailable()).thenReturn(projectService);

        return new WorkspaceSystemPromptAdvisorProviderImpl(
            workspaceSystemPrompts, deploymentProvider, projectProvider);
    }

    @Test
    void testReturnsAdvisorWhenWorkspaceHasPrompt() {
        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setProjectId(11L);

        Project project = new Project();

        project.setWorkspaceId(7L);

        when(projectDeploymentService.getProjectDeployment(42L)).thenReturn(projectDeployment);
        when(projectService.getProject(11L)).thenReturn(project);
        when(workspaceSystemPrompts.fetchPrompt(7L)).thenReturn("Be concise.");

        assertThat(provider().getAdvisor(PlatformType.AUTOMATION, 42L, "ai_agent")).isPresent();
    }

    @Test
    void testReturnsEmptyWhenWorkspaceHasNoPrompt() {
        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setProjectId(11L);

        Project project = new Project();

        project.setWorkspaceId(7L);

        when(projectDeploymentService.getProjectDeployment(42L)).thenReturn(projectDeployment);
        when(projectService.getProject(11L)).thenReturn(project);
        when(workspaceSystemPrompts.fetchPrompt(7L)).thenReturn(null);

        assertThat(provider().getAdvisor(PlatformType.AUTOMATION, 42L, "ai_agent")).isEmpty();
    }

    @Test
    void testReturnsEmptyForNonAutomationOrUnknownPrincipal() {
        assertThat(provider().getAdvisor(PlatformType.EMBEDDED, 42L, "ai_agent")).isEmpty();
        assertThat(provider().getAdvisor(PlatformType.AUTOMATION, null, "ai_agent")).isEmpty();
        assertThat(provider().getAdvisor(null, 42L, "ai_agent")).isEmpty();
    }

    @Test
    void testFailsOpenWhenResolutionThrows() {
        when(projectDeploymentService.getProjectDeployment(42L)).thenThrow(new IllegalStateException("gone"));

        assertThat(provider().getAdvisor(PlatformType.AUTOMATION, 42L, "ai_agent")).isEmpty();
    }
}
