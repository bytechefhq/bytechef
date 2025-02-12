/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.facade;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.repository.git.GitWorkflowRepository.GitWorkflows;
import com.bytechef.atlas.configuration.repository.git.operations.GitWorkflowOperations.GitInfo;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.facade.ProjectFacade;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.automation.configuration.service.WorkspaceService;
import com.bytechef.ee.automation.configuration.domain.ProjectGitConfiguration;
import com.bytechef.ee.automation.configuration.service.ProjectGitConfigurationService;
import com.bytechef.ee.automation.configuration.service.ProjectGitService;
import com.bytechef.ee.platform.configuration.dto.GitConfigurationDTO;
import com.bytechef.ee.platform.configuration.facade.GitConfigurationFacade;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Objects;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@Transactional
@ConditionalOnEEVersion
public class ProjectGitFacadeImpl implements ProjectGitFacade {

    private final GitConfigurationFacade gitConfigurationFacade;
    private final ProjectFacade projectFacade;
    private final ProjectGitConfigurationService projectGitConfigurationService;
    private final ProjectGitService projectGitService;
    private final ProjectService projectService;
    private final ProjectWorkflowService projectWorkflowService;
    private final WorkflowService workflowService;
    private final WorkspaceService workspaceService;

    @SuppressFBWarnings("EI")
    public ProjectGitFacadeImpl(
        GitConfigurationFacade gitConfigurationFacade, ProjectFacade projectFacade,
        ProjectGitConfigurationService projectGitConfigurationService, ProjectGitService projectGitService,
        ProjectService projectService, ProjectWorkflowService projectWorkflowService,
        WorkflowService workflowService, WorkspaceService workspaceService) {

        this.gitConfigurationFacade = gitConfigurationFacade;
        this.projectFacade = projectFacade;
        this.projectGitConfigurationService = projectGitConfigurationService;
        this.projectGitService = projectGitService;
        this.projectService = projectService;
        this.projectWorkflowService = projectWorkflowService;
        this.workflowService = workflowService;
        this.workspaceService = workspaceService;
    }

    @Override
    @SuppressFBWarnings("VA_FORMAT_STRING_USES_NEWLINE")
    public void pullProjectFromGit(long projectId) {
        Workspace workspace = workspaceService.getProjectWorkspace(projectId);

        GitConfigurationDTO gitConfiguration = gitConfigurationFacade.getGitConfiguration(workspace.getId());
        ProjectGitConfiguration projectGitConfiguration = projectGitConfigurationService.getProjectGitConfiguration(
            projectId);

        GitWorkflows gitWorkflows = projectGitService.getWorkflows(
            gitConfiguration.url(), projectGitConfiguration.getBranch(), gitConfiguration.username(),
            gitConfiguration.password());

        Project project = projectService.getProject(projectId);

        List<Workflow> oldWorkflows = projectWorkflowService.getProjectWorkflows(projectId, project.getLastVersion())
            .stream()
            .map(projectWorkflow -> workflowService.getWorkflow(projectWorkflow.getWorkflowId()))
            .toList();

        for (Workflow workflow : gitWorkflows.workflows()) {
            Workflow oldWorkflow = oldWorkflows.stream()
                .filter(curWorkflow -> Objects.equals(curWorkflow.getLabel(), workflow.getLabel()))
                .findFirst()
                .orElse(null);

            if (oldWorkflow == null) {
                projectFacade.addWorkflow(projectId, workflow.getDefinition());
            } else {
                oldWorkflow.setDefinition(workflow.getDefinition());

                projectFacade.updateWorkflow(
                    Objects.requireNonNull(oldWorkflow.getId()), workflow.getDefinition(), oldWorkflow.getVersion());
            }

            GitInfo gitInfo = gitWorkflows.gitInfo();
            GitConfigurationDTO gitConfigurationDTO = gitConfigurationFacade.getGitConfiguration(workspace.getId());

            projectFacade.publishProject(
                projectId,
                """
                    %s

                    Project pulled from git repository:
                    Repository: %s
                    Branch: %s
                    Commit hash: %s
                    """.formatted(
                    gitInfo.message(), gitConfigurationDTO.url(), projectGitConfiguration.getBranch(),
                    gitInfo.commitHash()),
                false);
        }
    }

    @Override
    public String pushProjectToGit(long projectId, @NonNull String commitMessage) {
        Project project = projectService.getProject(projectId);

        List<ProjectWorkflow> projectWorkflows = projectWorkflowService.getProjectWorkflows(
            projectId, project.getLastVersion());

        Workspace workspace = workspaceService.getProjectWorkspace(projectId);

        GitConfigurationDTO gitConfigurationDTO = gitConfigurationFacade.getGitConfiguration(workspace.getId());
        ProjectGitConfiguration projectGitConfiguration = projectGitConfigurationService.getProjectGitConfiguration(
            projectId);

        return projectGitService.save(
            projectWorkflows.stream()
                .map(projectWorkflow -> workflowService.getWorkflow(projectWorkflow.getWorkflowId()))
                .toList(),
            commitMessage, gitConfigurationDTO.url(), projectGitConfiguration.getBranch(),
            gitConfigurationDTO.username(), gitConfigurationDTO.password());
    }
}
