/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.guardrails.advisor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.ee.platform.ai.guardrails.AiGuardrailMetrics;
import com.bytechef.ee.platform.ai.guardrails.AiGuardrails;
import com.bytechef.ee.platform.ai.guardrails.domain.AiGuardrailsSettingsTarget;
import com.bytechef.ee.platform.ai.workspace.JobPrincipalWorkspaceResolver;
import com.bytechef.platform.ai.sensitivedata.SensitiveDataMetrics;
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
    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

    private AiGuardrailsAdvisorProviderImpl aiGuardrailsAdvisorProvider;

    @BeforeEach
    void setUp() {
        when(meterRegistryProvider.getIfAvailable()).thenReturn(meterRegistry);
        when(projectDeploymentServiceProvider.getIfAvailable()).thenReturn(projectDeploymentService);
        when(projectServiceProvider.getIfAvailable()).thenReturn(projectService);

        aiGuardrailsAdvisorProvider = new AiGuardrailsAdvisorProviderImpl(
            aiGuardrails, meterRegistryProvider,
            new JobPrincipalWorkspaceResolver(projectDeploymentServiceProvider, projectServiceProvider));
    }

    @Test
    void testAutomationJobPrincipalIdResolvesWorkspace() {
        when(projectDeploymentService.getProjectDeployment(JOB_PRINCIPAL_ID))
            .thenReturn(projectDeployment(PROJECT_ID));
        when(projectService.getProject(PROJECT_ID)).thenReturn(project(WORKSPACE_ID));
        when(aiGuardrails.isActive(AiGuardrailsSettingsTarget.workspace(WORKSPACE_ID))).thenReturn(true);

        Optional<Advisor> advisor = aiGuardrailsAdvisorProvider.getAdvisor(
            PlatformType.AUTOMATION, JOB_PRINCIPAL_ID, SURFACE);

        assertThat(advisor).isPresent();

        verify(aiGuardrails).isActive(AiGuardrailsSettingsTarget.workspace(WORKSPACE_ID));
        verify(aiGuardrails, never()).isActive(AiGuardrailsSettingsTarget.platform());
    }

    @Test
    void testEmbeddedPlatformResolvesTheEmbeddedTarget() {
        when(aiGuardrails.isActive(AiGuardrailsSettingsTarget.embedded())).thenReturn(true);

        Optional<Advisor> advisor = aiGuardrailsAdvisorProvider.getAdvisor(
            PlatformType.EMBEDDED, JOB_PRINCIPAL_ID, SURFACE);

        assertThat(advisor).isPresent();

        verify(aiGuardrails).isActive(AiGuardrailsSettingsTarget.embedded());
        verify(projectDeploymentService, never()).getProjectDeployment(anyLong());
    }

    @Test
    void testNullJobPrincipalIdResolvesTheTenantDefault() {
        when(aiGuardrails.isActive(AiGuardrailsSettingsTarget.platform())).thenReturn(true);

        Optional<Advisor> advisor = aiGuardrailsAdvisorProvider.getAdvisor(PlatformType.AUTOMATION, null, SURFACE);

        assertThat(advisor).isPresent();

        verify(aiGuardrails).isActive(AiGuardrailsSettingsTarget.platform());
        verify(projectDeploymentService, never()).getProjectDeployment(anyLong());
    }

    @Test
    void testResolutionExceptionFallsBackToTheTenantDefault() {
        when(projectDeploymentService.getProjectDeployment(JOB_PRINCIPAL_ID))
            .thenThrow(new RuntimeException("project deployment not found"));
        when(aiGuardrails.isActive(AiGuardrailsSettingsTarget.platform())).thenReturn(true);

        Optional<Advisor> advisor = aiGuardrailsAdvisorProvider.getAdvisor(
            PlatformType.AUTOMATION, JOB_PRINCIPAL_ID, SURFACE);

        assertThat(advisor).isPresent();

        verify(aiGuardrails).isActive(AiGuardrailsSettingsTarget.platform());
    }

    @Test
    void testMissingProjectServicesResolvesTheTenantDefault() {
        when(projectDeploymentServiceProvider.getIfAvailable()).thenReturn(null);
        when(projectServiceProvider.getIfAvailable()).thenReturn(null);
        when(aiGuardrails.isActive(AiGuardrailsSettingsTarget.platform())).thenReturn(true);

        Optional<Advisor> advisor = aiGuardrailsAdvisorProvider.getAdvisor(
            PlatformType.AUTOMATION, JOB_PRINCIPAL_ID, SURFACE);

        assertThat(advisor).isPresent();

        verify(aiGuardrails).isActive(AiGuardrailsSettingsTarget.platform());
        verify(projectDeploymentService, never()).getProjectDeployment(anyLong());
    }

    @Test
    void testAllGuardrailsDisabledReturnsEmpty() {
        when(projectDeploymentService.getProjectDeployment(JOB_PRINCIPAL_ID))
            .thenReturn(projectDeployment(PROJECT_ID));
        when(projectService.getProject(PROJECT_ID)).thenReturn(project(WORKSPACE_ID));
        when(aiGuardrails.isActive(AiGuardrailsSettingsTarget.workspace(WORKSPACE_ID))).thenReturn(false);

        Optional<Advisor> advisor = aiGuardrailsAdvisorProvider.getAdvisor(
            PlatformType.AUTOMATION, JOB_PRINCIPAL_ID, SURFACE);

        assertThat(advisor).isEmpty();
    }

    @Test
    void testWorkspaceResolutionIsCachedAcrossCalls() {
        when(projectDeploymentService.getProjectDeployment(JOB_PRINCIPAL_ID))
            .thenReturn(projectDeployment(PROJECT_ID));
        when(projectService.getProject(PROJECT_ID)).thenReturn(project(WORKSPACE_ID));
        when(aiGuardrails.isActive(AiGuardrailsSettingsTarget.workspace(WORKSPACE_ID))).thenReturn(true);

        aiGuardrailsAdvisorProvider.getAdvisor(PlatformType.AUTOMATION, JOB_PRINCIPAL_ID, SURFACE);
        aiGuardrailsAdvisorProvider.getAdvisor(PlatformType.AUTOMATION, JOB_PRINCIPAL_ID, SURFACE);

        verify(projectDeploymentService, times(1)).getProjectDeployment(JOB_PRINCIPAL_ID);
        verify(projectService, times(1)).getProject(PROJECT_ID);
    }

    @Test
    void testGetMetricsReturnsNullWhenAllGuardrailsDisabled() {
        when(projectDeploymentService.getProjectDeployment(JOB_PRINCIPAL_ID))
            .thenReturn(projectDeployment(PROJECT_ID));
        when(projectService.getProject(PROJECT_ID)).thenReturn(project(WORKSPACE_ID));
        when(aiGuardrails.isActive(AiGuardrailsSettingsTarget.workspace(WORKSPACE_ID))).thenReturn(false);

        SensitiveDataMetrics metrics = aiGuardrailsAdvisorProvider.getMetrics(
            PlatformType.AUTOMATION, JOB_PRINCIPAL_ID, SURFACE);

        assertThat(metrics).isNull();
    }

    @Test
    void testGetMetricsNonNullExactlyWhenAdvisorPresent() {
        when(projectDeploymentService.getProjectDeployment(JOB_PRINCIPAL_ID))
            .thenReturn(projectDeployment(PROJECT_ID));
        when(projectService.getProject(PROJECT_ID)).thenReturn(project(WORKSPACE_ID));
        when(aiGuardrails.isActive(AiGuardrailsSettingsTarget.workspace(WORKSPACE_ID))).thenReturn(true);

        Optional<Advisor> advisor = aiGuardrailsAdvisorProvider.getAdvisor(
            PlatformType.AUTOMATION, JOB_PRINCIPAL_ID, SURFACE);
        SensitiveDataMetrics metrics = aiGuardrailsAdvisorProvider.getMetrics(
            PlatformType.AUTOMATION, JOB_PRINCIPAL_ID, SURFACE);

        assertThat(advisor).isPresent();
        assertThat(metrics).isNotNull();
    }

    @Test
    void testGetMetricsRecordsUnderTheRequestedSurfaceTagNotAnotherCallers() {
        when(aiGuardrails.isActive(AiGuardrailsSettingsTarget.embedded())).thenReturn(true);

        SensitiveDataMetrics agentMetrics = aiGuardrailsAdvisorProvider.getMetrics(
            PlatformType.EMBEDDED, JOB_PRINCIPAL_ID, "ai_agent");
        SensitiveDataMetrics gatewayMetrics = aiGuardrailsAdvisorProvider.getMetrics(
            PlatformType.EMBEDDED, JOB_PRINCIPAL_ID, "gateway");

        assertThat(agentMetrics).isNotNull();
        assertThat(gatewayMetrics).isNotNull();

        agentMetrics.recordToolArgsRestored();

        assertThat(
            meterRegistry.get(AiGuardrailMetrics.COUNTER_NAME)
                .tag("event", "tool_args_restored")
                .tag("surface", "ai_agent")
                .counter()
                .count())
                    .isEqualTo(1.0);

        assertThat(meterRegistry.find(AiGuardrailMetrics.COUNTER_NAME)
            .tag("event", "tool_args_restored")
            .tag("surface", "gateway")
            .counter())
                .as("recording through the ai_agent-surfaced metrics must not also increment the gateway surface")
                .isNull();
    }

    @Test
    void testGetAdvisorForWorkspaceUsesTheGivenWorkspaceWithoutDerivingOne() {
        when(aiGuardrails.isActive(AiGuardrailsSettingsTarget.workspace(WORKSPACE_ID))).thenReturn(true);

        Optional<Advisor> advisor = aiGuardrailsAdvisorProvider.getAdvisorForWorkspace(WORKSPACE_ID, SURFACE);

        assertThat(advisor).isPresent();

        verify(projectDeploymentService, never()).getProjectDeployment(anyLong());
    }

    @Test
    void testGetAdvisorForWorkspaceIsEmptyWhenNothingIsActiveForThatWorkspace() {
        when(aiGuardrails.isActive(AiGuardrailsSettingsTarget.workspace(WORKSPACE_ID))).thenReturn(false);

        Optional<Advisor> advisor = aiGuardrailsAdvisorProvider.getAdvisorForWorkspace(WORKSPACE_ID, SURFACE);

        assertThat(advisor).isEmpty();

        verify(aiGuardrails).isActive(AiGuardrailsSettingsTarget.workspace(WORKSPACE_ID));
    }

    @Test
    void testGetAdvisorForWorkspaceFollowsThePolicyRatherThanAlwaysAnsweringTheSameWay() {
        long inactiveWorkspaceId = WORKSPACE_ID + 1;

        when(aiGuardrails.isActive(AiGuardrailsSettingsTarget.workspace(WORKSPACE_ID))).thenReturn(true);
        when(aiGuardrails.isActive(AiGuardrailsSettingsTarget.workspace(inactiveWorkspaceId))).thenReturn(false);

        assertThat(aiGuardrailsAdvisorProvider.getAdvisorForWorkspace(WORKSPACE_ID, SURFACE))
            .as("the same call must differ by workspace policy, or neither the present nor the empty case proves "
                + "the policy is being read")
            .isPresent();
        assertThat(aiGuardrailsAdvisorProvider.getAdvisorForWorkspace(inactiveWorkspaceId, SURFACE))
            .isEmpty();
    }

    @Test
    void testGetMetricsForWorkspaceAgreesWithGetAdvisorForWorkspace() {
        when(aiGuardrails.isActive(AiGuardrailsSettingsTarget.workspace(WORKSPACE_ID))).thenReturn(true);

        SensitiveDataMetrics activeMetrics = aiGuardrailsAdvisorProvider.getMetricsForWorkspace(
            WORKSPACE_ID, SURFACE);

        assertThat(activeMetrics).isNotNull();

        long inactiveWorkspaceId = WORKSPACE_ID + 1;

        when(aiGuardrails.isActive(AiGuardrailsSettingsTarget.workspace(inactiveWorkspaceId))).thenReturn(false);

        SensitiveDataMetrics inactiveMetrics = aiGuardrailsAdvisorProvider.getMetricsForWorkspace(
            inactiveWorkspaceId, SURFACE);

        assertThat(inactiveMetrics).isNull();
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
