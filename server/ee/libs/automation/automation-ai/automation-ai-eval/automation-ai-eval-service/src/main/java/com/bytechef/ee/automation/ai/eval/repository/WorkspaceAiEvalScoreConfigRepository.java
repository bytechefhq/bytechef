/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.eval.repository;

import com.bytechef.ee.automation.ai.eval.domain.WorkspaceAiEvalScoreConfig;
import com.bytechef.ee.platform.ai.eval.domain.AiEvalScoreConfig;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;

/**
 * Workspace ↔ AI eval-score-config membership repository.
 *
 * @author Ivica Cardic
 * @version ee
 */
public interface WorkspaceAiEvalScoreConfigRepository
    extends ListCrudRepository<WorkspaceAiEvalScoreConfig, Long> {

    Optional<WorkspaceAiEvalScoreConfig> findByAiEvalScoreConfigId(long aiEvalScoreConfigId);

    @Query("""
        SELECT ai_eval_score_config.*
        FROM ai_eval_score_config
        JOIN workspace_ai_eval_score_config
            ON workspace_ai_eval_score_config.ai_eval_score_config_id = ai_eval_score_config.id
        WHERE workspace_ai_eval_score_config.workspace_id = :workspaceId
        """)
    List<AiEvalScoreConfig> findAllConfigsByWorkspaceId(@Param("workspaceId") Long workspaceId);

    @Query("""
        SELECT ai_eval_score_config.*
        FROM ai_eval_score_config
        JOIN workspace_ai_eval_score_config
            ON workspace_ai_eval_score_config.ai_eval_score_config_id = ai_eval_score_config.id
        WHERE workspace_ai_eval_score_config.workspace_id = :workspaceId
            AND ai_eval_score_config.name = :name
        """)
    Optional<AiEvalScoreConfig> findConfigByWorkspaceIdAndName(
        @Param("workspaceId") Long workspaceId, @Param("name") String name);
}
