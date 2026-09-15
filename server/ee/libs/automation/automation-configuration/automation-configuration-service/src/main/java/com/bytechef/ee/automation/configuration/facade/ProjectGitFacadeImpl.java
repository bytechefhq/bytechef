/*
 * Copyright 2025 ByteChef
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
import com.bytechef.automation.configuration.facade.ProjectWorkflowFacade;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.ee.automation.configuration.domain.ProjectGitConfiguration;
import com.bytechef.ee.automation.configuration.service.ProjectGitConfigurationService;
import com.bytechef.ee.automation.configuration.service.ProjectGitService;
import com.bytechef.ee.automation.configuration.service.WorkspaceService;
import com.bytechef.ee.platform.configuration.dto.GitConfigurationDTO;
import com.bytechef.ee.platform.configuration.facade.GitConfigurationFacade;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Objects;
import org.springframework.security.access.prepost.PreAuthorize;
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
    private final ProjectWorkflowFacade projectWorkflowFacade;
    private final ProjectWorkflowService projectWorkflowService;
    private final WorkflowService workflowService;
    private final WorkspaceService workspaceService;

    @SuppressFBWarnings("EI")
    public ProjectGitFacadeImpl(
        GitConfigurationFacade gitConfigurationFacade, ProjectFacade projectFacade,
        ProjectGitConfigurationService projectGitConfigurationService, ProjectGitService projectGitService,
        ProjectService projectService, ProjectWorkflowFacade projectWorkflowFacade,
        ProjectWorkflowService projectWorkflowService, WorkflowService workflowService,
        WorkspaceService workspaceService) {

        this.gitConfigurationFacade = gitConfigurationFacade;
        this.projectFacade = projectFacade;
        this.projectGitConfigurationService = projectGitConfigurationService;
        this.projectGitService = projectGitService;
        this.projectService = projectService;
        this.projectWorkflowFacade = projectWorkflowFacade;
        this.projectWorkflowService = projectWorkflowService;
        this.workflowService = workflowService;
        this.workspaceService = workspaceService;
    }

    // Overwrites the project's workflows from the remote branch, so it needs the pull scope in its own right. The
    // workflow writes and the publishProject call further down carry their own guards, but they run only after the pull
    // has started, and not at all when the branch is empty, so they cannot stand in for one here. The publish requires
    // WORKFLOW_EDIT and PROJECT_PUBLISH on the project.
    @Override
    @PreAuthorize("hasPermission(#projectId, 'Project', 'PROJECT_PULL')")
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

        List<Workflow> oldWorkflows = projectWorkflowService
            .getProjectWorkflows(projectId, project.getLastProjectVersion())
            .stream()
            .map(projectWorkflow -> workflowService.getWorkflow(projectWorkflow.getWorkflowId()))
            .toList();

        for (Workflow workflow : gitWorkflows.workflows()) {
            Workflow oldWorkflow = oldWorkflows.stream()
                .filter(curWorkflow -> Objects.equals(curWorkflow.getLabel(), workflow.getLabel()))
                .findFirst()
                .orElse(null);

            if (oldWorkflow == null) {
                projectWorkflowFacade.addWorkflow(projectId, workflow.getDefinition());
            } else {
                oldWorkflow.setDefinition(workflow.getDefinition());

                projectWorkflowFacade.updateWorkflow(
                    Objects.requireNonNull(oldWorkflow.getId()), workflow.getDefinition(), oldWorkflow.getVersion());
            }
        }

        if (gitWorkflows.workflows()
            .isEmpty()) {

            return;
        }

        GitInfo gitInfo = gitWorkflows.gitInfo();

        // One version for the whole pull, after every workflow is written.
        projectFacade.publishProject(
            projectId,
            """
                %s

                Project pulled from git repository:
                Repository: %s
                Branch: %s
                Commit hash: %s
                """.formatted(
                gitInfo.message(), gitConfiguration.url(), projectGitConfiguration.getBranch(), gitInfo.commitHash()),
            false);
    }

    // Contacts the remote with the workspace's stored credentials so the Git Configuration dialog can offer a branch.
    // Gated with that dialog's submit, ProjectGitConfigurationServiceImpl.save, so the two cannot disagree.
    @Override
    @PreAuthorize("hasPermission(#projectId, 'Project', 'WORKSPACE_MANAGE')")
    public List<String> getRemoteBranches(long projectId) {
        Workspace workspace = workspaceService.getProjectWorkspace(projectId);

        GitConfigurationDTO gitConfiguration = gitConfigurationFacade.getGitConfiguration(workspace.getId());

        return projectGitService.getRemoteBranches(
            gitConfiguration.url(), gitConfiguration.username(), gitConfiguration.password());
    }

    // PROJECT_PUSH, not the PROJECT_PUBLISH that ProjectServiceImpl.publishProject requires of the publish this is
    // normally reached from. Publishing a project whose Git configuration is enabled needs both, since
    // ProjectGitSyncEventListenerImpl reaches this only for such a project. Both sit at EDITOR rank, so only a custom
    // role can hold one without the other. Push stays with editors because it writes to a branch of a repository an
    // administrator configured and changes nothing inside ByteChef; pull, which overwrites the project, is ADMIN.
    @Override
    @PreAuthorize("hasPermission(#projectId, 'Project', 'PROJECT_PUSH')")
    public String pushProjectToGit(long projectId, String commitMessage) {
        Project project = projectService.getProject(projectId);

        List<ProjectWorkflow> projectWorkflows = projectWorkflowService.getProjectWorkflows(
            projectId, project.getLastProjectVersion());

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
