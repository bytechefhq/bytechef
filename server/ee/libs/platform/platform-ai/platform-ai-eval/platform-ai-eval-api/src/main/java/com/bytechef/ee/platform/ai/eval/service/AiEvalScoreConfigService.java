/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.eval.service;

import com.bytechef.ee.platform.ai.eval.domain.AiEvalScoreConfig;

/**
 * Workspace-agnostic eval-score-config service. Workspace-aware queries (by-workspace, by-workspace-and-name) live in
 * {@code com.bytechef.ee.automation.ai.eval.service.WorkspaceAiEvalScoreConfigService}.
 *
 * @author Ivica Cardic
 * @version ee
 */
public interface AiEvalScoreConfigService {

    AiEvalScoreConfig create(AiEvalScoreConfig scoreConfig);

    void delete(long id);

    AiEvalScoreConfig getScoreConfig(long id);

    AiEvalScoreConfig update(AiEvalScoreConfig scoreConfig);
}
