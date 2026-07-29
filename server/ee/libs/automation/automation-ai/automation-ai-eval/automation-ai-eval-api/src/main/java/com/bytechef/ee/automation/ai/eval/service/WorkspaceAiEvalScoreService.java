/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.eval.service;

import com.bytechef.ee.platform.ai.eval.domain.AiEvalScore;
import com.bytechef.ee.platform.ai.eval.dto.AiEvalScoreTrendPoint;
import java.time.Instant;
import java.util.List;

/**
 * Workspace-scoped operations on eval scores.
 *
 * @author Ivica Cardic
 * @version ee
 */
public interface WorkspaceAiEvalScoreService {

    /**
     * Resolves the workspace-scoped {@code AiEvalScoreConfig} (if one exists), binds the score to {@code workspaceId},
     * and creates it via the platform service.
     */
    AiEvalScore createInWorkspace(AiEvalScore score, long workspaceId);

    void deleteInWorkspace(long scoreId);

    Long getWorkspaceId(long scoreId);

    List<AiEvalScore> getScoresByWorkspace(Long workspaceId);

    List<AiEvalScore> getScoresByWorkspaceAndName(Long workspaceId, String name);

    List<AiEvalScoreTrendPoint> getScoreTrend(Long workspaceId, String name, Instant start, Instant end);
}
