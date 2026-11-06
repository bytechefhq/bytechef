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
