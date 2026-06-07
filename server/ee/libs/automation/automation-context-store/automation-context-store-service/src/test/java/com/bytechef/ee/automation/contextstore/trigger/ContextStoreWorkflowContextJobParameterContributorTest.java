/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.contextstore.trigger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ContextStoreWorkflowContextJobParameterContributor}. Covers the resolve chain (jobPrincipalId →
 * ProjectDeployment → Project.workspaceId), the embedded-mode skip, and every fall-through-to-empty branch the SPI
 * contract requires.
 *
 * @author Ivica Cardic
 * @version ee
 */
class ContextStoreWorkflowContextJobParameterContributorTest {

    private static final long PROJECT_DEPLOYMENT_ID = 100L;
    private static final long PROJECT_ID = 50L;
    private static final long WORKSPACE_ID = 7L;

    @Test
    void testHappyPathEmitsWorkspaceIdResolvedFromProjectDeployment() {
        ProjectDeploymentService projectDeploymentService = mock(ProjectDeploymentService.class);
        ProjectService projectService = mock(ProjectService.class);

        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setProjectId(PROJECT_ID);

        Project project = new Project();

        project.setWorkspaceId(WORKSPACE_ID);

        when(projectDeploymentService.getProjectDeployment(eq(PROJECT_DEPLOYMENT_ID))).thenReturn(projectDeployment);
        when(projectService.getProject(eq(PROJECT_ID))).thenReturn(project);

        var contributor = new ContextStoreWorkflowContextJobParameterContributor(
            projectDeploymentService, projectService);

        WorkflowExecutionId workflowExecutionId =
            WorkflowExecutionId.of(PlatformType.AUTOMATION, PROJECT_DEPLOYMENT_ID, "workflow-uuid", "trigger-1");

        Map<String, ?> result = contributor.contribute(Map.of(), workflowExecutionId);

        assertThat(result).hasSize(1);
        assertThat(result.get("contextStore.workspaceId")).isEqualTo(WORKSPACE_ID);
    }

    @Test
    void testEmbeddedPlatformTypeReturnsEmptyMap() {
        ProjectDeploymentService projectDeploymentService = mock(ProjectDeploymentService.class);
        ProjectService projectService = mock(ProjectService.class);

        var contributor = new ContextStoreWorkflowContextJobParameterContributor(
            projectDeploymentService, projectService);

        WorkflowExecutionId workflowExecutionId =
            WorkflowExecutionId.of(PlatformType.EMBEDDED, PROJECT_DEPLOYMENT_ID, "workflow-uuid", "trigger-1");

        Map<String, ?> result = contributor.contribute(Map.of(), workflowExecutionId);

        assertThat(result).isEmpty();
    }

    @Test
    void testNullWorkspaceIdOnProjectReturnsEmptyMap() {
        // Defensive branch: a Project without a workspaceId would be a data corruption case, but the contract says
        // "never throw" — collapse to empty so trigger dispatch survives.
        ProjectDeploymentService projectDeploymentService = mock(ProjectDeploymentService.class);
        ProjectService projectService = mock(ProjectService.class);

        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setProjectId(PROJECT_ID);

        Project project = new Project();
        // workspaceId deliberately unset.

        when(projectDeploymentService.getProjectDeployment(eq(PROJECT_DEPLOYMENT_ID))).thenReturn(projectDeployment);
        when(projectService.getProject(eq(PROJECT_ID))).thenReturn(project);

        var contributor = new ContextStoreWorkflowContextJobParameterContributor(
            projectDeploymentService, projectService);

        WorkflowExecutionId workflowExecutionId =
            WorkflowExecutionId.of(PlatformType.AUTOMATION, PROJECT_DEPLOYMENT_ID, "workflow-uuid", "trigger-1");

        Map<String, ?> result = contributor.contribute(Map.of(), workflowExecutionId);

        assertThat(result).isEmpty();
    }

    @Test
    void testServiceExceptionReturnsEmptyMap() {
        ProjectDeploymentService projectDeploymentService = mock(ProjectDeploymentService.class);
        ProjectService projectService = mock(ProjectService.class);

        when(projectDeploymentService.getProjectDeployment(eq(PROJECT_DEPLOYMENT_ID)))
            .thenThrow(new RuntimeException("DB transient"));

        var contributor = new ContextStoreWorkflowContextJobParameterContributor(
            projectDeploymentService, projectService);

        WorkflowExecutionId workflowExecutionId =
            WorkflowExecutionId.of(PlatformType.AUTOMATION, PROJECT_DEPLOYMENT_ID, "workflow-uuid", "trigger-1");

        Map<String, ?> result = contributor.contribute(Map.of(), workflowExecutionId);

        assertThat(result).isEmpty();
    }
}
