/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.eval.repository;

import com.bytechef.ee.automation.ai.eval.domain.WorkspaceAiEvalRule;
import com.bytechef.ee.platform.ai.eval.domain.AiEvalRule;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;

/**
 * Workspace ↔ AI eval-rule membership repository. Workspace-aware queries that JOIN to {@code ai_eval_rule} live here
 * (mirrors the platform-connection / WorkspaceConnection pattern: workspace-scoped reads belong to the automation layer
 * that owns the relation table).
 *
 * @author Ivica Cardic
 * @version ee
 */
public interface WorkspaceAiEvalRuleRepository extends ListCrudRepository<WorkspaceAiEvalRule, Long> {

    Optional<WorkspaceAiEvalRule> findByAiEvalRuleId(long aiEvalRuleId);

    @Query("""
        SELECT ai_eval_rule.*
        FROM ai_eval_rule
        JOIN workspace_ai_eval_rule
            ON workspace_ai_eval_rule.ai_eval_rule_id = ai_eval_rule.id
        WHERE workspace_ai_eval_rule.workspace_id = :workspaceId
        """)
    List<AiEvalRule> findAllRulesByWorkspaceId(@Param("workspaceId") Long workspaceId);

    @Query("""
        SELECT ai_eval_rule.*
        FROM ai_eval_rule
        JOIN workspace_ai_eval_rule
            ON workspace_ai_eval_rule.ai_eval_rule_id = ai_eval_rule.id
        WHERE workspace_ai_eval_rule.workspace_id = :workspaceId
            AND ai_eval_rule.enabled = :enabled
        """)
    List<AiEvalRule> findAllRulesByWorkspaceIdAndEnabled(
        @Param("workspaceId") Long workspaceId, @Param("enabled") boolean enabled);

    @Query("""
        SELECT ai_eval_rule.*
        FROM ai_eval_rule
        JOIN workspace_ai_eval_rule
            ON workspace_ai_eval_rule.ai_eval_rule_id = ai_eval_rule.id
        WHERE workspace_ai_eval_rule.workspace_id = :workspaceId
            AND ai_eval_rule.enabled = TRUE
            AND ai_eval_rule.target = :target
        """)
    List<AiEvalRule> findAllRulesByWorkspaceIdAndEnabledTrueAndTarget(
        @Param("workspaceId") Long workspaceId, @Param("target") int target);
}
