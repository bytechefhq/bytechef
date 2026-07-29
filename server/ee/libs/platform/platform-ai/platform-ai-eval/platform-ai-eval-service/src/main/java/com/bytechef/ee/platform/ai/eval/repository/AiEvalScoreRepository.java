/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.eval.repository;

import com.bytechef.ee.platform.ai.eval.domain.AiEvalScore;
import com.bytechef.ee.platform.ai.eval.dto.AiEvalScoreTrendPoint;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;

/**
 * CRUD on {@code ai_eval_score}, including the workspace-scoped reads and the day-bucketed trend aggregation that
 * {@code WorkspaceAiEvalScoreService} in automation-ai-eval exposes.
 *
 * @author Ivica Cardic
 * @version ee
 */
public interface AiEvalScoreRepository extends ListCrudRepository<AiEvalScore, Long> {

    List<AiEvalScore> findAllByTraceId(Long traceId);

    /**
     * Scores owned by one workspace. A score whose {@code workspace_id} is null belongs to no workspace and is
     * invisible here, which is the intended behavior for a workspace-scoped listing. The same holds for the by-name
     * finder and the trend aggregation below.
     */
    List<AiEvalScore> findAllByWorkspaceId(Long workspaceId);

    List<AiEvalScore> findAllByWorkspaceIdAndName(Long workspaceId, String name);

    /**
     * Day-bucketed time-series for one score name. Only NUMERIC rows contribute to {@code average}; {@code count}
     * reflects rows of any type that landed in the day bucket. {@code day} is the UTC start-of-day epoch millis.
     */
    @Query("""
        SELECT
          EXTRACT(EPOCH FROM DATE_TRUNC('day', created_date)) * 1000 AS day,
          AVG(value)                                                 AS average,
          COUNT(*)                                                   AS count
        FROM ai_eval_score
        WHERE workspace_id = :workspaceId
          AND name         = :name
          AND created_date BETWEEN :start AND :end
        GROUP BY DATE_TRUNC('day', created_date)
        ORDER BY day
        """)
    List<AiEvalScoreTrendPoint> findTrendByWorkspaceIdAndName(
        @Param("workspaceId") Long workspaceId,
        @Param("name") String name,
        @Param("start") Instant start,
        @Param("end") Instant end);
}
