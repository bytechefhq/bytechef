/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.repository;

import com.bytechef.ee.automation.ai.gateway.domain.AiEvalExecution;
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
     * Workspace-scoped deletion via the owning eval rule's workspace_id. Prevents cross-tenant deletion when called
     * inside a per-workspace loop.
     */
    @Modifying
    @Query("""
        DELETE FROM ai_eval_execution
        WHERE created_date < :date
          AND eval_rule_id IN (SELECT id FROM ai_eval_rule WHERE workspace_id = :workspaceId)
        """)
    void deleteAllByWorkspaceIdAndCreatedDateBefore(
        @Param("workspaceId") Long workspaceId, @Param("date") Instant date);
}
