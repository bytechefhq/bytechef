/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.eval.facade;

import com.bytechef.ee.platform.ai.eval.domain.AiEvalScoreConfig;
import java.util.List;

/**
 * @version ee
 * @author Ivica Cardic
 */
public interface AiEvalScoreConfigFacade {

    AiEvalScoreConfig getScoreConfig(long id);

    List<AiEvalScoreConfig> getScoreConfigsByWorkspace(Long workspaceId);

    AiEvalScoreConfig createInWorkspace(AiEvalScoreConfig scoreConfig, Long workspaceId);

    void deleteInWorkspace(long id);

    AiEvalScoreConfig update(AiEvalScoreConfig scoreConfig);
}
