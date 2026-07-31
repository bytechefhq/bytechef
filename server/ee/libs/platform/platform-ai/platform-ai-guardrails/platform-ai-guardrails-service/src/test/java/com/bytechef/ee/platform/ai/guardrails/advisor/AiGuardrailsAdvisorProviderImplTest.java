/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.guardrails.advisor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.ee.platform.ai.guardrails.AiGuardrails;
import com.bytechef.platform.constant.PlatformType;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.beans.factory.ObjectProvider;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SuppressWarnings("unchecked")
class AiGuardrailsAdvisorProviderImplTest {

    private static final long JOB_PRINCIPAL_ID = 100L;
    private static final long PROJECT_ID = 5L;
    private static final long WORKSPACE_ID = 7L;
    private static final String SURFACE = "ai_agent";

    private final AiGuardrails aiGuardrails = mock(AiGuardrails.class);
    private final ProjectDeploymentService projectDeploymentService = mock(ProjectDeploymentService.class);
    private final ProjectService projectService = mock(ProjectService.class);
    private final ObjectProvider<MeterRegistry> meterRegistryProvider = mock(ObjectProvider.class);
    private final ObjectProvider<ProjectDeploymentService> projectDeploymentServiceProvider =
        mock(ObjectProvider.class);
    private final ObjectProvider<ProjectService> projectServiceProvider = mock(ObjectProvider.class);

    private AiGuardrailsAdvisorProviderImpl aiGuardrailsAdvisorProvider;

    @BeforeEach
    void setUp() {
        when(meterRegistryProvider.getIfAvailable()).thenReturn(new SimpleMeterRegistry());
        when(projectDeploymentServiceProvider.getIfAvailable()).thenReturn(projectDeploymentService);
        when(projectServiceProvider.getIfAvailable()).thenReturn(projectService);

        aiGuardrailsAdvisorProvider = new AiGuardrailsAdvisorProviderImpl(
            aiGuardrails, meterRegistryProvider, projectDeploymentServiceProvider, projectServiceProvider);
    }

    @Test
    void testAutomationJobPrincipalIdResolvesWorkspace() {
        when(projectDeploymentService.getProjectDeployment(JOB_PRINCIPAL_ID))
            .thenReturn(projectDeployment(PROJECT_ID));
        when(projectService.getProject(PROJECT_ID)).thenReturn(project(WORKSPACE_ID));
        when(aiGuardrails.isActive(WORKSPACE_ID)).thenReturn(true);

        Optional<Advisor> advisor = aiGuardrailsAdvisorProvider.getAdvisor(
            PlatformType.AUTOMATION, JOB_PRINCIPAL_ID, SURFACE);

        assertThat(advisor).isPresent();

        verify(aiGuardrails).isActive(WORKSPACE_ID);
        verify(aiGuardrails, never()).isActive(isNull());
    }

    @Test
    void testEmbeddedPlatformResolvesNullWorkspace() {
        when(aiGuardrails.isActive(isNull())).thenReturn(true);

        Optional<Advisor> advisor = aiGuardrailsAdvisorProvider.getAdvisor(
            PlatformType.EMBEDDED, JOB_PRINCIPAL_ID, SURFACE);

        assertThat(advisor).isPresent();

        verify(aiGuardrails).isActive(isNull());
        verify(projectDeploymentService, never()).getProjectDeployment(anyLong());
    }

    @Test
    void testNullJobPrincipalIdResolvesNullWorkspace() {
        when(aiGuardrails.isActive(isNull())).thenReturn(true);

        Optional<Advisor> advisor = aiGuardrailsAdvisorProvider.getAdvisor(PlatformType.AUTOMATION, null, SURFACE);

        assertThat(advisor).isPresent();

        verify(aiGuardrails).isActive(isNull());
        verify(projectDeploymentService, never()).getProjectDeployment(anyLong());
    }

    @Test
    void testResolutionExceptionFallsBackToNullWorkspace() {
        when(projectDeploymentService.getProjectDeployment(JOB_PRINCIPAL_ID))
            .thenThrow(new RuntimeException("project deployment not found"));
        when(aiGuardrails.isActive(isNull())).thenReturn(true);

        Optional<Advisor> advisor = aiGuardrailsAdvisorProvider.getAdvisor(
            PlatformType.AUTOMATION, JOB_PRINCIPAL_ID, SURFACE);

        assertThat(advisor).isPresent();

        verify(aiGuardrails).isActive(isNull());
    }

    @Test
    void testMissingProjectServicesResolvesNullWorkspace() {
        when(projectDeploymentServiceProvider.getIfAvailable()).thenReturn(null);
        when(projectServiceProvider.getIfAvailable()).thenReturn(null);
        when(aiGuardrails.isActive(isNull())).thenReturn(true);

        Optional<Advisor> advisor = aiGuardrailsAdvisorProvider.getAdvisor(
            PlatformType.AUTOMATION, JOB_PRINCIPAL_ID, SURFACE);

        assertThat(advisor).isPresent();

        verify(aiGuardrails).isActive(isNull());
        verify(projectDeploymentService, never()).getProjectDeployment(anyLong());
    }

    @Test
    void testAllGuardrailsDisabledReturnsEmpty() {
        when(projectDeploymentService.getProjectDeployment(JOB_PRINCIPAL_ID))
            .thenReturn(projectDeployment(PROJECT_ID));
        when(projectService.getProject(PROJECT_ID)).thenReturn(project(WORKSPACE_ID));
        when(aiGuardrails.isActive(WORKSPACE_ID)).thenReturn(false);

        Optional<Advisor> advisor = aiGuardrailsAdvisorProvider.getAdvisor(
            PlatformType.AUTOMATION, JOB_PRINCIPAL_ID, SURFACE);

        assertThat(advisor).isEmpty();
    }

    @Test
    void testWorkspaceResolutionIsCachedAcrossCalls() {
        when(projectDeploymentService.getProjectDeployment(JOB_PRINCIPAL_ID))
            .thenReturn(projectDeployment(PROJECT_ID));
        when(projectService.getProject(PROJECT_ID)).thenReturn(project(WORKSPACE_ID));
        when(aiGuardrails.isActive(WORKSPACE_ID)).thenReturn(true);

        aiGuardrailsAdvisorProvider.getAdvisor(PlatformType.AUTOMATION, JOB_PRINCIPAL_ID, SURFACE);
        aiGuardrailsAdvisorProvider.getAdvisor(PlatformType.AUTOMATION, JOB_PRINCIPAL_ID, SURFACE);

        verify(projectDeploymentService, times(1)).getProjectDeployment(JOB_PRINCIPAL_ID);
        verify(projectService, times(1)).getProject(PROJECT_ID);
    }

    private static ProjectDeployment projectDeployment(long projectId) {
        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setProjectId(projectId);

        return projectDeployment;
    }

    private static Project project(long workspaceId) {
        return Project.builder()
            .workspaceId(workspaceId)
            .build();
    }
}
