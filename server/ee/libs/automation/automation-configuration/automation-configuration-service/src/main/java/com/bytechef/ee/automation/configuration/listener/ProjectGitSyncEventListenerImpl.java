/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.listener;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectVersion;
import com.bytechef.automation.configuration.listener.ProjectGitSyncEventListener;
import com.bytechef.ee.automation.configuration.domain.ProjectGitConfiguration;
import com.bytechef.ee.automation.configuration.facade.ProjectGitFacade;
import com.bytechef.ee.automation.configuration.service.ProjectGitConfigurationService;
import com.bytechef.ee.platform.configuration.dto.GitConfigurationDTO;
import com.bytechef.ee.platform.configuration.facade.GitConfigurationFacade;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class ProjectGitSyncEventListenerImpl implements ProjectGitSyncEventListener {

    private final GitConfigurationFacade gitConfigurationFacade;
    private final ProjectGitConfigurationService projectGitConfigurationService;
    private final ProjectGitFacade projectGitFacade;

    @SuppressFBWarnings("EI")
    public ProjectGitSyncEventListenerImpl(
        GitConfigurationFacade gitConfigurationFacade, ProjectGitConfigurationService projectGitConfigurationService,
        ProjectGitFacade projectGitFacade) {

        this.gitConfigurationFacade = gitConfigurationFacade;
        this.projectGitConfigurationService = projectGitConfigurationService;
        this.projectGitFacade = projectGitFacade;
    }

    @Override
    @SuppressFBWarnings("VA_FORMAT_STRING_USES_NEWLINE")
    public void onBeforePublishProject(Project project) {
        Optional<ProjectGitConfiguration> projectGitConfigurationOptional = projectGitConfigurationService
            .fetchProjectGitConfiguration(project.getId());

        boolean enabled = projectGitConfigurationOptional.map(ProjectGitConfiguration::isEnabled)
            .orElse(false);

        if (enabled) {
            GitConfigurationDTO gitConfigurationDTO = gitConfigurationFacade.getGitConfiguration(
                Objects.requireNonNull(project.getWorkspaceId()));
            ProjectGitConfiguration projectGitConfiguration = projectGitConfigurationOptional.get();
            ProjectVersion projectVersion = project.getLastPublishedProjectVersion();

            String description = projectVersion.getDescription();

            if (description == null) {
                description = "Update workflows";
            }

            String commitHash = projectGitFacade.pushProjectToGit(project.getId(), description);

            projectVersion.setDescription(
                """
                    %s

                    Project pushed to git repository:
                    Repository: %s
                    Branch: %s
                    Commit hash: %s
                    """.formatted(
                    description, gitConfigurationDTO.url(), projectGitConfiguration.getBranch(), commitHash));
        }
    }
}
