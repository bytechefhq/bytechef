/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.guardrails.advisor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.ee.platform.ai.guardrails.AiGuardrails;
import com.bytechef.ee.platform.ai.guardrails.domain.AiGuardrailsSettingsScope;
import com.bytechef.ee.platform.ai.guardrails.domain.AiGuardrailsSettingsTarget;
import com.bytechef.ee.platform.ai.workspace.JobPrincipalWorkspaceResolver;
import com.bytechef.platform.ai.guardrails.GuardrailSurface;
import com.bytechef.platform.constant.PlatformType;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SuppressWarnings("unchecked")
class AiGuardrailsAdvisorProviderScopeTest {

    private static final long JOB_PRINCIPAL_ID = 42L;
    private static final long PROJECT_ID = 5L;
    private static final long WORKSPACE_ID = 7L;

    private final AiGuardrails aiGuardrails = mock(AiGuardrails.class);
    private final ProjectDeploymentService projectDeploymentService = mock(ProjectDeploymentService.class);
    private final ProjectService projectService = mock(ProjectService.class);
    private final ObjectProvider<MeterRegistry> meterRegistryProvider = mock(ObjectProvider.class);
    private final ObjectProvider<ProjectDeploymentService> projectDeploymentServiceProvider =
        mock(ObjectProvider.class);
    private final ObjectProvider<ProjectService> projectServiceProvider = mock(ObjectProvider.class);

    private AiGuardrailsAdvisorProviderImpl provider;

    @BeforeEach
    void setUp() {
        when(meterRegistryProvider.getIfAvailable()).thenReturn(new SimpleMeterRegistry());
        when(projectDeploymentServiceProvider.getIfAvailable()).thenReturn(projectDeploymentService);
        when(projectServiceProvider.getIfAvailable()).thenReturn(projectService);
        when(aiGuardrails.isActive(any(AiGuardrailsSettingsTarget.class))).thenReturn(true);

        provider = new AiGuardrailsAdvisorProviderImpl(
            aiGuardrails, meterRegistryProvider,
            new JobPrincipalWorkspaceResolver(projectDeploymentServiceProvider, projectServiceProvider));
    }

    @Test
    void testAnEmbeddedRunResolvesTheEmbeddedTarget() {
        provider.getAdvisor(PlatformType.EMBEDDED, JOB_PRINCIPAL_ID, GuardrailSurface.AI_AGENT);

        assertThat(capturedTarget().scope()).isEqualTo(AiGuardrailsSettingsScope.EMBEDDED);
    }

    @Test
    void testAnAutomationRunWithAWorkspaceResolvesThatWorkspace() {
        when(projectDeploymentService.getProjectDeployment(JOB_PRINCIPAL_ID))
            .thenReturn(projectDeployment(PROJECT_ID));
        when(projectService.getProject(PROJECT_ID)).thenReturn(project(WORKSPACE_ID));

        provider.getAdvisor(PlatformType.AUTOMATION, JOB_PRINCIPAL_ID, GuardrailSurface.AI_AGENT);

        assertThat(capturedTarget()).isEqualTo(AiGuardrailsSettingsTarget.workspace(WORKSPACE_ID));
    }

    @Test
    void testAnAutomationRunWithoutAWorkspaceResolvesTheTenantDefault() {
        when(projectDeploymentService.getProjectDeployment(JOB_PRINCIPAL_ID))
            .thenThrow(new RuntimeException("project deployment not found"));

        provider.getAdvisor(PlatformType.AUTOMATION, JOB_PRINCIPAL_ID, GuardrailSurface.AI_AGENT);

        assertThat(capturedTarget().scope()).isEqualTo(AiGuardrailsSettingsScope.PLATFORM);
    }

    @Test
    void testGetMetricsResolvesTheSameTargetAsGetAdvisor() {
        provider.getMetrics(PlatformType.EMBEDDED, JOB_PRINCIPAL_ID, GuardrailSurface.AI_AGENT);

        assertThat(capturedTarget().scope()).isEqualTo(AiGuardrailsSettingsScope.EMBEDDED);
    }

    private AiGuardrailsSettingsTarget capturedTarget() {
        ArgumentCaptor<AiGuardrailsSettingsTarget> targetCaptor = ArgumentCaptor.forClass(
            AiGuardrailsSettingsTarget.class);

        verify(aiGuardrails).isActive(targetCaptor.capture());

        return targetCaptor.getValue();
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
