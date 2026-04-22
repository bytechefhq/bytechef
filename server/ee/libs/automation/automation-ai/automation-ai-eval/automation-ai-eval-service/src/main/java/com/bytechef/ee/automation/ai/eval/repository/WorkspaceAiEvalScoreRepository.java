/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.eval.repository;

import com.bytechef.ee.automation.ai.eval.domain.WorkspaceAiEvalScore;
import com.bytechef.ee.platform.ai.eval.domain.AiEvalScore;
import com.bytechef.ee.platform.ai.eval.dto.AiEvalScoreTrendPoint;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;

/**
 * Workspace ↔ AI eval-score membership repository.
 *
 * @author Ivica Cardic
 * @version ee
 */
public interface WorkspaceAiEvalScoreRepository extends ListCrudRepository<WorkspaceAiEvalScore, Long> {

    Optional<WorkspaceAiEvalScore> findByAiEvalScoreId(long aiEvalScoreId);

    @Query("""
        SELECT ai_eval_score.*
        FROM ai_eval_score
        JOIN workspace_ai_eval_score
            ON workspace_ai_eval_score.ai_eval_score_id = ai_eval_score.id
        WHERE workspace_ai_eval_score.workspace_id = :workspaceId
        """)
    List<AiEvalScore> findAllScoresByWorkspaceId(@Param("workspaceId") Long workspaceId);

    @Query("""
        SELECT ai_eval_score.*
        FROM ai_eval_score
        JOIN workspace_ai_eval_score
            ON workspace_ai_eval_score.ai_eval_score_id = ai_eval_score.id
        WHERE workspace_ai_eval_score.workspace_id = :workspaceId
            AND ai_eval_score.name = :name
        """)
    List<AiEvalScore> findAllScoresByWorkspaceIdAndName(
        @Param("workspaceId") Long workspaceId, @Param("name") String name);

    /**
     * Day-bucketed time-series for one score name. Only NUMERIC rows contribute to {@code average}; {@code count}
     * reflects rows of any type that landed in the day bucket. {@code day} is the UTC start-of-day epoch millis.
     */
    @Query("""
        SELECT
          EXTRACT(EPOCH FROM DATE_TRUNC('day', ai_eval_score.created_date)) * 1000 AS day,
          AVG(ai_eval_score.value)                                                  AS average,
          COUNT(*)                                                                  AS count
        FROM ai_eval_score
        JOIN workspace_ai_eval_score
            ON workspace_ai_eval_score.ai_eval_score_id = ai_eval_score.id
        WHERE workspace_ai_eval_score.workspace_id = :workspaceId
          AND ai_eval_score.name                   = :name
          AND ai_eval_score.created_date BETWEEN :start AND :end
        GROUP BY DATE_TRUNC('day', ai_eval_score.created_date)
        ORDER BY day
        """)
    List<AiEvalScoreTrendPoint> findTrendByWorkspaceAndName(
        @Param("workspaceId") Long workspaceId,
        @Param("name") String name,
        @Param("start") Instant start,
        @Param("end") Instant end);
}
