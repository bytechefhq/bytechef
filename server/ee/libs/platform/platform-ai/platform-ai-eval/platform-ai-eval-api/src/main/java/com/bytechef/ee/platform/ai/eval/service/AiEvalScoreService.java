/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.eval.service;

import com.bytechef.ee.platform.ai.eval.domain.AiEvalScore;
import com.bytechef.ee.platform.ai.eval.domain.AiEvalScoreConfig;
import java.util.List;
import java.util.Optional;

/**
 * Workspace-agnostic eval-score service. Workspace-aware queries live in
 * {@code com.bytechef.ee.automation.ai.eval.service.WorkspaceAiEvalScoreService}.
 *
 * <p>
 * {@link #create} validates the score against a corresponding {@link AiEvalScoreConfig} when the caller passes one.
 * Without that config, the service skips validation and saves the score as-is. The lookup of the right config (which
 * has historically been keyed by workspace + score name) is the workspace service's job — it resolves the config before
 * calling this method.
 *
 * @author Ivica Cardic
 * @version ee
 */
public interface AiEvalScoreService {

    AiEvalScore create(AiEvalScore score, Optional<AiEvalScoreConfig> matchingConfig);

    void delete(long id);

    AiEvalScore getScore(long id);

    List<AiEvalScore> getScores(List<Long> ids);

    List<AiEvalScore> getScoresByTrace(Long traceId);
}
