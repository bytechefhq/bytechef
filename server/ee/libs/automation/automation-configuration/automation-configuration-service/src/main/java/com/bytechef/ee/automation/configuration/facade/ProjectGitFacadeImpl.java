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
import com.bytechef.automation.configuration.domain.ProjectWorkflowType;
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
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
    private final List<ProjectContentContributor> projectContentContributors;
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
        WorkspaceService workspaceService, List<ProjectContentContributor> projectContentContributors) {

        this.gitConfigurationFacade = gitConfigurationFacade;
        this.projectContentContributors = projectContentContributors;
        this.projectFacade = projectFacade;
        this.projectGitConfigurationService = projectGitConfigurationService;
        this.projectGitService = projectGitService;
        this.projectService = projectService;
        this.projectWorkflowFacade = projectWorkflowFacade;
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
            gitConfiguration.password(), getContentDirectories());

        Project project = projectService.getProject(projectId);

        List<Workflow> oldWorkflows = getOrdinaryProjectWorkflows(projectId, project.getLastProjectVersion())
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

        Map<String, byte[]> contentFiles = gitWorkflows.contentFiles();

        for (ProjectContentContributor projectContentContributor : projectContentContributors) {
            Map<String, byte[]> contributorContentFiles = getContentFiles(
                contentFiles, projectContentContributor.getContentDirectory());

            if (!contributorContentFiles.isEmpty()) {
                projectContentContributor.pullProjectContent(projectId, contributorContentFiles);
            }
        }

        if (gitWorkflows.workflows()
            .isEmpty() && contentFiles.isEmpty()) {

            return;
        }

        for (ProjectContentContributor projectContentContributor : projectContentContributors) {
            if (!projectContentContributor.isProjectContentPublishable(projectId)) {
                return;
            }
        }

        GitInfo gitInfo = gitWorkflows.gitInfo();

        projectFacade.publishProject(
            projectId,
            """
                %s

                Project pulled from git repository:
                Repository: %s
                Branch: %s
                Commit hash: %s
                """.formatted(
                gitInfo.message(), gitConfiguration.url(), projectGitConfiguration.getBranch(),
                gitInfo.commitHash()),
            false);
    }

    @Override
    public List<String> getRemoteBranches(long projectId) {
        Workspace workspace = workspaceService.getProjectWorkspace(projectId);

        GitConfigurationDTO gitConfiguration = gitConfigurationFacade.getGitConfiguration(workspace.getId());

        return projectGitService.getRemoteBranches(
            gitConfiguration.url(), gitConfiguration.username(), gitConfiguration.password());
    }

    @Override
    public String pushProjectToGit(long projectId, String commitMessage) {
        Project project = projectService.getProject(projectId);

        List<ProjectWorkflow> projectWorkflows = getOrdinaryProjectWorkflows(
            projectId, project.getLastProjectVersion());

        Workspace workspace = workspaceService.getProjectWorkspace(projectId);

        GitConfigurationDTO gitConfigurationDTO = gitConfigurationFacade.getGitConfiguration(workspace.getId());
        ProjectGitConfiguration projectGitConfiguration = projectGitConfigurationService.getProjectGitConfiguration(
            projectId);

        Map<String, byte[]> contentFiles = new LinkedHashMap<>();

        for (ProjectContentContributor projectContentContributor : projectContentContributors) {
            contentFiles.putAll(
                projectContentContributor.exportProjectContent(projectId, project.getLastProjectVersion()));
        }

        return projectGitService.save(
            projectWorkflows.stream()
                .map(projectWorkflow -> workflowService.getWorkflow(projectWorkflow.getWorkflowId()))
                .toList(),
            contentFiles, getContentDirectories(), commitMessage, gitConfigurationDTO.url(),
            projectGitConfiguration.getBranch(), gitConfigurationDTO.username(), gitConfigurationDTO.password());
    }

    private List<String> getContentDirectories() {
        return projectContentContributors.stream()
            .map(ProjectContentContributor::getContentDirectory)
            .toList();
    }

    private static Map<String, byte[]> getContentFiles(Map<String, byte[]> contentFiles, String contentDirectory) {
        Map<String, byte[]> directoryContentFiles = new LinkedHashMap<>();

        for (Map.Entry<String, byte[]> entry : contentFiles.entrySet()) {
            if (entry.getKey()
                .startsWith(contentDirectory)) {

                directoryContentFiles.put(entry.getKey(), entry.getValue());
            }
        }

        return directoryContentFiles;
    }

    private List<ProjectWorkflow> getOrdinaryProjectWorkflows(long projectId, int projectVersion) {
        return projectWorkflowService.getProjectWorkflows(projectId, projectVersion)
            .stream()
            .filter(projectWorkflow -> projectWorkflow.getType() == ProjectWorkflowType.WORKFLOW)
            .toList();
    }
}
