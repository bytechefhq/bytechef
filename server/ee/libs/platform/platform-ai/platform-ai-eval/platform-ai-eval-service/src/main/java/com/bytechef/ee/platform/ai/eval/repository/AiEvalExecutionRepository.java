/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.eval.repository;

import com.bytechef.ee.platform.ai.eval.domain.AiEvalExecution;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;

/**
 * @version ee
 */
public interface AiEvalExecutionRepository extends ListCrudRepository<AiEvalExecution, Long> {

    List<AiEvalExecution> findAllByEvalRuleId(Long evalRuleId);

    List<AiEvalExecution> findAllByTraceId(Long traceId);

    List<AiEvalExecution> findAllByStatus(int status);

    void deleteAllByCreatedDateBefore(Instant date);

    /**
     * Workspace-scoped deletion: deletes executions older than {@code date} whose owning eval rule is associated with
     * {@code workspaceId} via the {@code workspace_ai_eval_rule} relation. The relation table lives in
     * automation-ai-eval but the SQL is a string here — table names work at runtime regardless of which Gradle module
     * declares the relation entity.
     */
    @Modifying
    @Query("""
        DELETE FROM ai_eval_execution
        WHERE created_date < :date
          AND eval_rule_id IN (
            SELECT ai_eval_rule_id FROM workspace_ai_eval_rule WHERE workspace_id = :workspaceId
          )
        """)
    void deleteAllByWorkspaceIdAndCreatedDateBefore(
        @Param("workspaceId") Long workspaceId, @Param("date") Instant date);
}
