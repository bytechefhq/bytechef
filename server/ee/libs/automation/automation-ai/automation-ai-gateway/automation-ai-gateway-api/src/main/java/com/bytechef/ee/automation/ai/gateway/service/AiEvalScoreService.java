/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import com.bytechef.ee.automation.ai.gateway.domain.AiEvalScore;
import com.bytechef.ee.automation.ai.gateway.dto.AiEvalScoreTrendPoint;
import java.time.Instant;
import java.util.List;

/**
 * @version ee
 */
public interface AiEvalScoreService {

    AiEvalScore create(AiEvalScore score);

    void delete(long id);

    AiEvalScore getScore(long id);

    List<AiEvalScore> getScoresByTrace(Long traceId);

    List<AiEvalScore> getScoresByWorkspace(Long workspaceId);

    List<AiEvalScore> getScoresByWorkspaceAndName(Long workspaceId, String name);

    List<AiEvalScoreTrendPoint> getScoreTrend(Long workspaceId, String name, Instant start, Instant end);
}
