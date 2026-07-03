/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.workspacesettings.repository;

import com.bytechef.ee.ai.hub.workspacesettings.AiHubWorkspaceSettings;
import java.util.Optional;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Repository
public interface AiHubWorkspaceSettingsRepository extends CrudRepository<AiHubWorkspaceSettings, Long> {

    Optional<AiHubWorkspaceSettings> findByWorkspaceId(long workspaceId);
}
