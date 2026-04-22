/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.repository;

import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilitySpan;
import com.bytechef.ee.automation.ai.gateway.dto.AiPromptVersionMetrics;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;

/**
 * @version ee
 */
public interface AiObservabilitySpanRepository extends ListCrudRepository<AiObservabilitySpan, Long> {

    List<AiObservabilitySpan> findAllByTraceId(Long traceId);

    @Modifying
    @Query("DELETE FROM ai_observability_span " +
        "WHERE trace_id IN (SELECT id FROM ai_observability_trace WHERE created_date < :date)")
    int deleteAllByTraceCreatedDateBefore(@Param("date") Instant date);

    @Modifying
    @Query("""
        DELETE FROM ai_observability_span
        WHERE trace_id IN (
            SELECT id FROM ai_observability_trace
            WHERE workspace_id = :workspaceId AND created_date < :date)
        """)
    int deleteAllByWorkspaceIdAndTraceCreatedDateBefore(
        @Param("workspaceId") Long workspaceId, @Param("date") Instant date);

    /**
     * Aggregates observability metrics for one prompt version. Status ordinal {@code 2} == ERROR; relies on the INT
     * ordinal encoding used throughout this module. Returns an empty-metrics row (count=0, nulls) when no spans
     * reference the version, so resolver code never needs null-checks.
     */
    @Query("""
        SELECT
          COUNT(*)                                                                         AS invocation_count,
          AVG(latency_ms)                                                                  AS avg_latency_ms,
          AVG(cost)                                                                        AS avg_cost_usd,
          CASE WHEN COUNT(*) = 0 THEN NULL
               ELSE SUM(CASE WHEN status = 2 THEN 1.0 ELSE 0.0 END) / COUNT(*)
          END                                                                              AS error_rate
        FROM ai_observability_span
        WHERE prompt_version_id = :promptVersionId
        """)
    Optional<AiPromptVersionMetrics> aggregateMetricsByPromptVersion(
        @Param("promptVersionId") Long promptVersionId);
}
