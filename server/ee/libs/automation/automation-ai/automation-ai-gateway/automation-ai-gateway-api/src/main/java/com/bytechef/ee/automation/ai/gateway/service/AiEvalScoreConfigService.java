/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import com.bytechef.ee.automation.ai.gateway.domain.AiEvalScoreConfig;
import java.util.List;
import java.util.Optional;

/**
 * @version ee
 */
public interface AiEvalScoreConfigService {

    AiEvalScoreConfig create(AiEvalScoreConfig scoreConfig);

    void delete(long id);

    Optional<AiEvalScoreConfig> fetchScoreConfigByWorkspaceIdAndName(Long workspaceId, String name);

    AiEvalScoreConfig getScoreConfig(long id);

    List<AiEvalScoreConfig> getScoreConfigsByWorkspace(Long workspaceId);

    AiEvalScoreConfig update(AiEvalScoreConfig scoreConfig);
}
