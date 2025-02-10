/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.service;

import com.bytechef.ee.automation.configuration.domain.ProjectGitConfiguration;
import java.util.Optional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface ProjectGitConfigurationService {

    Optional<ProjectGitConfiguration> fetchProjectGitConfiguration(long projectId);

    ProjectGitConfiguration getProjectGitConfiguration(long projectId);

    void save(ProjectGitConfiguration projectGitConfiguration);
}
