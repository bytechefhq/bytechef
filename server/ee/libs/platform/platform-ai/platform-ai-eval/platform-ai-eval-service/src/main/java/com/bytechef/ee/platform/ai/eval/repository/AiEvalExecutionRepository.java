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
     * Workspace-scoped deletion: deletes executions older than {@code date} whose owning eval rule belongs to
     * {@code workspaceId}. Stays a set-based statement rather than a derived delete because it is a retention sweep
     * over a table that grows with every evaluated trace.
     */
    @Modifying
    @Query("""
        DELETE FROM ai_eval_execution
        WHERE created_date < :date
          AND eval_rule_id IN (
            SELECT id FROM ai_eval_rule WHERE workspace_id = :workspaceId
          )
        """)
    void deleteAllByWorkspaceIdAndCreatedDateBefore(
        @Param("workspaceId") Long workspaceId, @Param("date") Instant date);
}
