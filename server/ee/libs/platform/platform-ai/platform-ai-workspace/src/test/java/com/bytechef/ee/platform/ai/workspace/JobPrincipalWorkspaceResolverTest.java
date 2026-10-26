/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.workspace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.platform.constant.PlatformType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class JobPrincipalWorkspaceResolverTest {

    private final ProjectDeploymentService projectDeploymentService = mock(ProjectDeploymentService.class);
    private final ProjectService projectService = mock(ProjectService.class);

    @Test
    void testResolvesTheWorkspaceOfAnAutomationJobPrincipal() {
        ProjectDeployment projectDeployment = mock(ProjectDeployment.class);
        Project project = mock(Project.class);

        when(projectDeployment.getProjectId()).thenReturn(7L);
        when(project.getWorkspaceId()).thenReturn(42L);
        when(projectDeploymentService.getProjectDeployment(3L)).thenReturn(projectDeployment);
        when(projectService.getProject(7L)).thenReturn(project);

        assertThat(newResolver().resolve(PlatformType.AUTOMATION, 3L)).isEqualTo(42L);
    }

    @Test
    void testANonAutomationPlatformTypeResolvesToTheTenantDefault() {
        assertThat(newResolver().resolve(PlatformType.EMBEDDED, 3L)).isNull();

        verifyNoInteractions(projectDeploymentService);
    }

    @Test
    void testANullJobPrincipalResolvesToTheTenantDefault() {
        assertThat(newResolver().resolve(PlatformType.AUTOMATION, null)).isNull();
    }

    @Test
    void testAResolutionFailureDegradesToTheTenantDefaultRatherThanThrowing() {
        when(projectDeploymentService.getProjectDeployment(3L))
            .thenThrow(new IllegalStateException("deployment was deleted"));

        assertThat(newResolver().resolve(PlatformType.AUTOMATION, 3L)).isNull();
    }

    @Test
    void testTheLookupIsCachedPerJobPrincipal() {
        ProjectDeployment projectDeployment = mock(ProjectDeployment.class);
        Project project = mock(Project.class);

        when(projectDeployment.getProjectId()).thenReturn(7L);
        when(project.getWorkspaceId()).thenReturn(42L);
        when(projectDeploymentService.getProjectDeployment(3L)).thenReturn(projectDeployment);
        when(projectService.getProject(7L)).thenReturn(project);

        JobPrincipalWorkspaceResolver resolver = newResolver();

        resolver.resolve(PlatformType.AUTOMATION, 3L);
        resolver.resolve(PlatformType.AUTOMATION, 3L);

        verify(projectDeploymentService, times(1)).getProjectDeployment(3L);
    }

    @SuppressWarnings("unchecked")
    private JobPrincipalWorkspaceResolver newResolver() {
        ObjectProvider<ProjectDeploymentService> projectDeploymentServiceProvider = mock(ObjectProvider.class);
        ObjectProvider<ProjectService> projectServiceProvider = mock(ObjectProvider.class);

        when(projectDeploymentServiceProvider.getIfAvailable()).thenReturn(projectDeploymentService);
        when(projectServiceProvider.getIfAvailable()).thenReturn(projectService);

        return new JobPrincipalWorkspaceResolver(projectDeploymentServiceProvider, projectServiceProvider);
    }
}
