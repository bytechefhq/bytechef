/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.event;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectVersion;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.ee.automation.configuration.domain.ProjectGitConfiguration;
import com.bytechef.ee.automation.configuration.facade.ProjectGitFacade;
import com.bytechef.ee.automation.configuration.service.ProjectGitConfigurationService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.data.relational.core.mapping.event.AbstractRelationalEventListener;
import org.springframework.data.relational.core.mapping.event.BeforeSaveEvent;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class ProjectBeforeSaveEventListener extends AbstractRelationalEventListener<Project> {

    private final ProjectGitConfigurationService projectGitConfigurationService;
    private final ProjectGitFacade projectGitFacade;
    private final ProjectService projectService;

    @SuppressFBWarnings("EI")
    public ProjectBeforeSaveEventListener(
        ProjectGitConfigurationService projectGitConfigurationService, ProjectGitFacade projectGitFacade,
        ProjectService projectService) {

        this.projectGitConfigurationService = projectGitConfigurationService;
        this.projectGitFacade = projectGitFacade;
        this.projectService = projectService;
    }

    @Override
    protected void onBeforeSave(BeforeSaveEvent<Project> event) {
        Project project = event.getEntity();

        if (project.getId() == null) {
            return;
        }

        boolean enabled = projectGitConfigurationService.fetchProjectGitConfiguration(project.getId())
            .map(ProjectGitConfiguration::isEnabled)
            .orElse(false);

        if (enabled) {
            Project curProject = projectService.getProject(project.getId());

            if (curProject.getLastVersion() != project.getLastVersion()) {
                ProjectVersion projectVersion = project.getLastPublishedProjectVersion();

                projectGitFacade.pushProjectToGit(
                    project.getId(),
                    "V%s - %s".formatted(projectVersion.getVersion(), projectVersion.getDescription()));
            }
        }
    }
}
