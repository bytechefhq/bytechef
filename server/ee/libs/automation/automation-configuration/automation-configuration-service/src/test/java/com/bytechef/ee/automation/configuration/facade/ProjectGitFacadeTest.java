/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.facade;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.repository.git.GitWorkflowRepository.GitWorkflows;
import com.bytechef.atlas.configuration.repository.git.operations.GitWorkflowOperations.GitInfo;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.facade.ProjectFacade;
import com.bytechef.automation.configuration.facade.ProjectWorkflowFacade;
import com.bytechef.automation.configuration.listener.ProjectContentContributor;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.ee.automation.configuration.domain.ProjectGitConfiguration;
import com.bytechef.ee.automation.configuration.service.ProjectGitConfigurationService;
import com.bytechef.ee.automation.configuration.service.ProjectGitService;
import com.bytechef.ee.automation.configuration.service.WorkspaceService;
import com.bytechef.ee.platform.configuration.dto.GitConfigurationDTO;
import com.bytechef.ee.platform.configuration.facade.GitConfigurationFacade;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ProjectGitFacadeTest {

    private static final long PROJECT_ID = 1L;

    private final GitConfigurationFacade gitConfigurationFacade = mock(GitConfigurationFacade.class);
    private final ProjectContentContributor projectContentContributor = mock(ProjectContentContributor.class);
    private final ProjectFacade projectFacade = mock(ProjectFacade.class);
    private final ProjectGitConfigurationService projectGitConfigurationService =
        mock(ProjectGitConfigurationService.class);
    private final ProjectGitService projectGitService = mock(ProjectGitService.class);
    private final ProjectService projectService = mock(ProjectService.class);
    private final WorkspaceService workspaceService = mock(WorkspaceService.class);

    private final ProjectGitFacadeImpl projectGitFacade = new ProjectGitFacadeImpl(
        gitConfigurationFacade, projectFacade, projectGitConfigurationService, projectGitService, projectService,
        mock(ProjectWorkflowFacade.class), mock(ProjectWorkflowService.class), mock(WorkflowService.class),
        workspaceService, List.of(projectContentContributor));

    @BeforeEach
    void beforeEach() {
        Workspace workspace = mock(Workspace.class);
        ProjectGitConfiguration projectGitConfiguration = mock(ProjectGitConfiguration.class);
        Project project = mock(Project.class);

        when(workspace.getId()).thenReturn(7L);
        when(workspaceService.getProjectWorkspace(PROJECT_ID)).thenReturn(workspace);
        when(gitConfigurationFacade.getGitConfiguration(7L))
            .thenReturn(new GitConfigurationDTO("https://git.example.com/repo.git", "user", "secret"));
        when(projectGitConfiguration.getBranch()).thenReturn("main");
        when(projectGitConfigurationService.getProjectGitConfiguration(PROJECT_ID))
            .thenReturn(projectGitConfiguration);
        when(project.getLastProjectVersion()).thenReturn(1);
        when(projectService.getProject(PROJECT_ID)).thenReturn(project);
        when(projectContentContributor.getContentDirectory()).thenReturn("agents/");
        when(projectGitService.getWorkflows(anyString(), anyString(), anyString(), anyString(), anyList()))
            .thenReturn(
                new GitWorkflows(
                    List.of(), new GitInfo("abc123", "Pull agents"),
                    Map.of("agents/support-bot.json", "{}".getBytes(StandardCharsets.UTF_8))));
    }

    @Test
    void testPullKeepsTheProjectADraftWhileItsContentIsNotPublishable() {
        when(projectContentContributor.isProjectContentPublishable(PROJECT_ID)).thenReturn(false);

        projectGitFacade.pullProjectFromGit(PROJECT_ID);

        verify(projectContentContributor).pullProjectContent(eq(PROJECT_ID), anyMap());
        verify(projectFacade, never()).publishProject(anyLong(), anyString(), anyBoolean());
    }

    @Test
    void testPullPublishesPublishableContent() {
        when(projectContentContributor.isProjectContentPublishable(PROJECT_ID)).thenReturn(true);

        projectGitFacade.pullProjectFromGit(PROJECT_ID);

        verify(projectFacade).publishProject(eq(PROJECT_ID), anyString(), eq(false));
    }
}
