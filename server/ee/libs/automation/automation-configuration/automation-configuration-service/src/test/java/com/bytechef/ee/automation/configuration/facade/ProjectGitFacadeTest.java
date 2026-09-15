/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.facade;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.repository.git.GitWorkflowRepository.GitWorkflows;
import com.bytechef.atlas.configuration.repository.git.operations.GitWorkflowOperations.GitInfo;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.facade.ProjectFacade;
import com.bytechef.automation.configuration.facade.ProjectWorkflowFacade;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.ee.automation.configuration.domain.ProjectGitConfiguration;
import com.bytechef.ee.automation.configuration.service.ProjectGitConfigurationService;
import com.bytechef.ee.automation.configuration.service.ProjectGitService;
import com.bytechef.ee.automation.configuration.service.WorkspaceService;
import com.bytechef.ee.platform.configuration.dto.GitConfigurationDTO;
import com.bytechef.ee.platform.configuration.facade.GitConfigurationFacade;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * A pull writes every workflow on the branch and then publishes the project once, so one pull adds one project version.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class ProjectGitFacadeTest {

    private static final long PROJECT_ID = 42L;
    private static final long WORKSPACE_ID = 7L;

    private final GitConfigurationFacade gitConfigurationFacade = mock(GitConfigurationFacade.class);
    private final ProjectFacade projectFacade = mock(ProjectFacade.class);
    private final ProjectGitConfigurationService projectGitConfigurationService =
        mock(ProjectGitConfigurationService.class);
    private final ProjectGitService projectGitService = mock(ProjectGitService.class);
    private final ProjectService projectService = mock(ProjectService.class);
    private final ProjectWorkflowFacade projectWorkflowFacade = mock(ProjectWorkflowFacade.class);
    private final ProjectWorkflowService projectWorkflowService = mock(ProjectWorkflowService.class);
    private final WorkflowService workflowService = mock(WorkflowService.class);
    private final WorkspaceService workspaceService = mock(WorkspaceService.class);

    private ProjectGitFacadeImpl projectGitFacade;

    @BeforeEach
    void setUp() {
        Workspace workspace = new Workspace();

        workspace.setId(WORKSPACE_ID);

        ProjectGitConfiguration projectGitConfiguration = mock(ProjectGitConfiguration.class);

        when(projectGitConfiguration.getBranch()).thenReturn("main");

        when(workspaceService.getProjectWorkspace(PROJECT_ID)).thenReturn(workspace);
        when(gitConfigurationFacade.getGitConfiguration(WORKSPACE_ID))
            .thenReturn(new GitConfigurationDTO("https://example.com/repository.git", "user", "secret"));
        when(projectGitConfigurationService.getProjectGitConfiguration(PROJECT_ID))
            .thenReturn(projectGitConfiguration);
        when(projectService.getProject(PROJECT_ID)).thenReturn(mock(Project.class));
        when(projectWorkflowService.getProjectWorkflows(anyLong(), anyInt())).thenReturn(List.of());

        projectGitFacade = new ProjectGitFacadeImpl(
            gitConfigurationFacade, projectFacade, projectGitConfigurationService, projectGitService, projectService,
            projectWorkflowFacade, projectWorkflowService, workflowService, workspaceService);
    }

    @Test
    void testPullPublishesTheProjectOnceForAllWorkflows() {
        givenBranchWorkflows(List.of(workflow("First", "{}"), workflow("Second", "{}"), workflow("Third", "{}")));

        projectGitFacade.pullProjectFromGit(PROJECT_ID);

        verify(projectWorkflowFacade, times(3)).addWorkflow(PROJECT_ID, "{}");
        verify(projectFacade, times(1)).publishProject(anyLong(), anyString(), anyBoolean());
    }

    @Test
    void testPullOfAnEmptyBranchDoesNotPublish() {
        givenBranchWorkflows(List.of());

        projectGitFacade.pullProjectFromGit(PROJECT_ID);

        verify(projectFacade, never()).publishProject(anyLong(), anyString(), anyBoolean());
    }

    private void givenBranchWorkflows(List<Workflow> workflows) {
        when(projectGitService.getWorkflows("https://example.com/repository.git", "main", "user", "secret"))
            .thenReturn(new GitWorkflows(workflows, new GitInfo("abc123", "Update workflows")));
    }

    private static Workflow workflow(String label, String definition) {
        Workflow workflow = mock(Workflow.class);

        when(workflow.getLabel()).thenReturn(label);
        when(workflow.getDefinition()).thenReturn(definition);

        return workflow;
    }
}
