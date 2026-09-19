/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.service;

import com.bytechef.ee.automation.configuration.domain.ProjectGitConfiguration;
import com.bytechef.ee.automation.configuration.repository.ProjectGitConfigurationRepository;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import java.util.List;
import java.util.Optional;
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
public class ProjectGitConfigurationServiceImpl implements ProjectGitConfigurationService {

    private final ProjectGitConfigurationRepository projectGitConfigurationRepository;

    public ProjectGitConfigurationServiceImpl(ProjectGitConfigurationRepository projectGitConfigurationRepository) {
        this.projectGitConfigurationRepository = projectGitConfigurationRepository;
    }

    @Override
    public void delete(long projectId) {
        projectGitConfigurationRepository.findByProjectId(projectId)
            .ifPresent(projectGitConfiguration -> projectGitConfigurationRepository.deleteById(
                projectGitConfiguration.getId()));
    }

    @Override
    public Optional<ProjectGitConfiguration> fetchProjectGitConfiguration(long projectId) {
        return projectGitConfigurationRepository.findByProjectId(projectId);
    }

    @Override
    public ProjectGitConfiguration getProjectGitConfiguration(long projectId) {
        return fetchProjectGitConfiguration(projectId)
            .orElseThrow(() -> new RuntimeException("ProjectGitConfiguration not found"));
    }

    @Override
    public List<ProjectGitConfiguration> getWorkspaceProjectGitConfigurations(long workspaceId) {
        return projectGitConfigurationRepository.findAllByWorkspaceId(workspaceId);
    }

    // Chooses the branch a project syncs with and turns sync on or off, which decides whether publishing pushes to Git.
    // Was unguarded. The reads above stay open: a branch name and an enabled flag are not sensitive, and the project
    // list and the project menu load them for every member.
    @Override
    @PreAuthorize("hasPermission(#projectGitConfiguration.projectId, 'Project', 'WORKSPACE_MANAGE')")
    public void save(ProjectGitConfiguration projectGitConfiguration) {
        projectGitConfigurationRepository.findByProjectId(projectGitConfiguration.getProjectId())
            .ifPresentOrElse(curProjectGitConfiguration -> {
                curProjectGitConfiguration.setBranch(projectGitConfiguration.getBranch());
                curProjectGitConfiguration.setEnabled(projectGitConfiguration.isEnabled());

                projectGitConfigurationRepository.save(curProjectGitConfiguration);
            }, () -> projectGitConfigurationRepository.save(projectGitConfiguration));
    }
}
