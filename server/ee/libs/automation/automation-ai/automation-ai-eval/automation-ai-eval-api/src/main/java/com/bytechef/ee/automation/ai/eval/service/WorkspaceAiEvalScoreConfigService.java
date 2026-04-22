/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.eval.service;

import com.bytechef.ee.platform.ai.eval.domain.AiEvalScoreConfig;
import java.util.List;
import java.util.Optional;

/**
 * Workspace-scoped operations on eval score configs.
 *
 * @author Ivica Cardic
 * @version ee
 */
public interface WorkspaceAiEvalScoreConfigService {

    AiEvalScoreConfig createInWorkspace(AiEvalScoreConfig scoreConfig, long workspaceId);

    void deleteInWorkspace(long scoreConfigId);

    Optional<AiEvalScoreConfig> fetchScoreConfigByWorkspaceIdAndName(Long workspaceId, String name);

    Long getWorkspaceId(long scoreConfigId);

    List<AiEvalScoreConfig> getScoreConfigsByWorkspace(Long workspaceId);
}
