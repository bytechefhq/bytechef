/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.configuration.facade;

import com.bytechef.ee.platform.configuration.dto.GitConfigurationDTO;
import java.util.Optional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface GitConfigurationFacade {

    Optional<GitConfigurationDTO> fetchGitConfiguration(long workspaceId);

    GitConfigurationDTO getGitConfiguration(long workspaceId);

    void save(GitConfigurationDTO gitConfigurationDTO, long workspaceId);
}
