/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.service;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.ee.automation.configuration.domain.ProjectCodeWorkflow;
import com.bytechef.ee.automation.configuration.repository.ProjectCodeWorkflowRepository;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import java.util.List;
import java.util.Optional;
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
public class ProjectCodeWorkflowServiceImpl implements ProjectCodeWorkflowService {

    private final ProjectCodeWorkflowRepository projectCodeWorkflowRepository;

    public ProjectCodeWorkflowServiceImpl(ProjectCodeWorkflowRepository projectCodeWorkflowRepository) {
        this.projectCodeWorkflowRepository = projectCodeWorkflowRepository;
    }

    @Override
    public ProjectCodeWorkflow create(CodeWorkflowContainer codeWorkflowContainer, Project project) {
        ProjectCodeWorkflow projectCodeWorkflow = new ProjectCodeWorkflow();

        projectCodeWorkflow.setCodeWorkflowContainer(codeWorkflowContainer);
        projectCodeWorkflow.setProject(project);
        projectCodeWorkflow.setProjectVersion(project.getLastProjectVersion());

        return projectCodeWorkflowRepository.save(projectCodeWorkflow);
    }

    @Override
    public void deleteProjectCodeWorkflows(long projectId) {
        projectCodeWorkflowRepository.deleteByProjectId(projectId);
    }

    @Override
    public Optional<ProjectCodeWorkflow> fetchProjectCodeWorkflow(long projectId) {
        return projectCodeWorkflowRepository.findFirstByProjectIdOrderByIdDesc(projectId);
    }

    @Override
    public List<Long> getCodeWorkflowProjectIds() {
        return projectCodeWorkflowRepository.findDistinctProjectIds();
    }

    /**
     * {@code noRollbackFor} is required here: {@code AutomationWorkflowProjectCodeWorkflowFacadeImpl
     * #fetchPreviousWorkflowUuidsByName} calls this method expecting to catch {@link IllegalArgumentException} as
     * normal control flow for "no previous deploy yet" -- but without {@code noRollbackFor}, Spring's default
     * {@code globalRollbackOnParticipationFailure} behavior marks the CALLER's (participating) transaction
     * rollback-only the instant this method's own {@code @Transactional} proxy sees the exception, regardless of
     * whether the caller catches it afterward. That poisons the whole {@code save()} transaction on every first deploy
     * of a project (no {@link ProjectCodeWorkflow} row yet), surfacing only against a real transactional datasource --
     * mocked unit tests never exercise the real proxy chain, so they never catch it.
     */
    @Override
    @Transactional(noRollbackFor = IllegalArgumentException.class)
    public ProjectCodeWorkflow getProjectCodeWorkflow(long projectId) {
        return projectCodeWorkflowRepository.findFirstByProjectIdOrderByIdDesc(projectId)
            .orElseThrow(
                () -> new IllegalArgumentException("No code workflow found for project id=" + projectId));
    }
}
