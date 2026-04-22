/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.eval.facade;

import com.bytechef.ee.platform.ai.eval.domain.AiEvalScore;
import com.bytechef.ee.platform.ai.eval.dto.AiEvalScoreTrendPoint;
import java.time.Instant;
import java.util.List;

/**
 * Facade for AI eval score operations. Hosts the authorization guards so they apply to every caller of the facade
 * rather than only the GraphQL entry point, and keeps them off the shared {@code AiEvalScoreService} which the gateway
 * evaluation data plane relies on.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AiEvalScoreFacade {

    AiEvalScore createInWorkspace(AiEvalScore score, long workspaceId);

    void deleteInWorkspace(long id);

    List<AiEvalScore> getScoresByWorkspace(Long workspaceId);

    /**
     * Workspace-scoped scores for the admin analytics view. Carries the {@code ADMIN} guard rather than the
     * {@code VIEWER} guard of {@link #getScoresByWorkspace(Long)} because the analytics endpoint is admin-only.
     */
    List<AiEvalScore> getScoresByWorkspaceForAnalytics(Long workspaceId);

    List<AiEvalScore> getScoresByTrace(Long traceId);

    List<AiEvalScoreTrendPoint> getScoreTrend(Long workspaceId, String name, Instant start, Instant end);
}
