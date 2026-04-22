/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.observability.repository;

import com.bytechef.ee.automation.ai.observability.domain.WorkspaceAiObservabilityTrace;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityTrace;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;

/**
 * Workspace association for {@code ai_observability_trace}. Owns both the membership row CRUD and all tenant-scoped
 * trace queries (the JOINs through {@code workspace_ai_observability_trace} formerly on the platform repo).
 *
 * @version ee
 */
public interface WorkspaceAiObservabilityTraceRepository
    extends ListCrudRepository<WorkspaceAiObservabilityTrace, Long> {

    Optional<WorkspaceAiObservabilityTrace> findByAiObservabilityTraceId(long aiObservabilityTraceId);

    @Query("""
        SELECT ai_observability_trace.*
        FROM ai_observability_trace
        JOIN workspace_ai_observability_trace
            ON workspace_ai_observability_trace.ai_observability_trace_id = ai_observability_trace.id
        WHERE workspace_ai_observability_trace.workspace_id = :workspaceId
          AND ai_observability_trace.external_trace_id = :externalTraceId
        """)
    Optional<AiObservabilityTrace> findByWorkspaceIdAndExternalTraceId(
        @Param("workspaceId") Long workspaceId, @Param("externalTraceId") String externalTraceId);

    @Query("""
        SELECT ai_observability_trace.*
        FROM ai_observability_trace
        JOIN workspace_ai_observability_trace
            ON workspace_ai_observability_trace.ai_observability_trace_id = ai_observability_trace.id
        WHERE workspace_ai_observability_trace.workspace_id = :workspaceId
          AND ai_observability_trace.created_date BETWEEN :start AND :end
        """)
    List<AiObservabilityTrace> findAllByWorkspaceIdAndCreatedDateBetween(
        @Param("workspaceId") Long workspaceId, @Param("start") Instant start, @Param("end") Instant end);

    @Query("""
        SELECT ai_observability_trace.*
        FROM ai_observability_trace
        JOIN workspace_ai_observability_trace
            ON workspace_ai_observability_trace.ai_observability_trace_id = ai_observability_trace.id
        WHERE workspace_ai_observability_trace.workspace_id = :workspaceId
          AND ai_observability_trace.source = :source
          AND ai_observability_trace.created_date BETWEEN :start AND :end
        """)
    List<AiObservabilityTrace> findAllByWorkspaceIdAndSourceAndCreatedDateBetween(
        @Param("workspaceId") Long workspaceId, @Param("source") int source,
        @Param("start") Instant start, @Param("end") Instant end);

    @Query("""
        SELECT ai_observability_trace.*
        FROM ai_observability_trace
        JOIN workspace_ai_observability_trace
            ON workspace_ai_observability_trace.ai_observability_trace_id = ai_observability_trace.id
        WHERE ai_observability_trace.session_id = :sessionId
          AND workspace_ai_observability_trace.workspace_id = :workspaceId
        """)
    List<AiObservabilityTrace> findAllBySessionIdAndWorkspaceId(
        @Param("sessionId") Long sessionId, @Param("workspaceId") Long workspaceId);

    /**
     * Cleanup helper: delete all span rows whose parent trace is older than {@code date} and belongs to
     * {@code workspaceId} via the relation table. Mirrors {@link #deleteAllByWorkspaceIdAndTraceCreatedDateBefore} but
     * operates on the span table — must be called BEFORE deleting the parent traces or the FK CASCADE wipes spans first
     * and leaves the per-workspace counter at 0.
     */
    @Modifying
    @Query("""
        DELETE FROM ai_observability_span
        WHERE trace_id IN (
            SELECT t.id FROM ai_observability_trace t
            JOIN workspace_ai_observability_trace wt ON wt.ai_observability_trace_id = t.id
            WHERE wt.workspace_id = :workspaceId AND t.created_date < :date)
        """)
    int deleteAllSpansByWorkspaceIdAndTraceCreatedDateBefore(
        @Param("workspaceId") Long workspaceId, @Param("date") Instant date);

    /**
     * Cleanup helper: delete all trace rows older than {@code date} that belong to {@code workspaceId} via the relation
     * table. Uses an EXISTS subquery so the FK CASCADE on workspace_ai_observability_trace handles the relation row in
     * turn.
     */
    @Modifying
    @Query("""
        DELETE FROM ai_observability_trace
        WHERE created_date < :date
          AND id IN (
            SELECT ai_observability_trace_id
            FROM workspace_ai_observability_trace
            WHERE workspace_id = :workspaceId
          )
        """)
    void deleteAllByWorkspaceIdAndTraceCreatedDateBefore(
        @Param("workspaceId") Long workspaceId, @Param("date") Instant date);

    /**
     * Dynamic filtered trace search. Every predicate is null-safe: passing {@code null} for a filter skips it. Joins
     * {@code ai_observability_span} for span-level {@code model} matching and {@code ai_observability_trace_tag} for
     * tag membership. Returned trace rows are distinct.
     */
    @Query("""
        SELECT DISTINCT t.* FROM ai_observability_trace t
        JOIN workspace_ai_observability_trace wt ON wt.ai_observability_trace_id = t.id
        LEFT JOIN ai_observability_span s ON s.trace_id = t.id
        LEFT JOIN ai_observability_trace_tag tt ON tt.ai_observability_trace = t.id
        WHERE wt.workspace_id = :workspaceId
          AND t.created_date BETWEEN :start AND :end
          AND (:userId IS NULL OR t.user_id = :userId)
          AND (:status IS NULL OR t.status = :status)
          AND (:source IS NULL OR t.source = :source)
          AND (:model IS NULL OR s.model = :model)
          AND (:tagId IS NULL OR tt.tag_id = :tagId)
        ORDER BY t.created_date DESC
        """)
    List<AiObservabilityTrace> findAllByFilters(
        @Param("workspaceId") Long workspaceId,
        @Param("start") Instant start,
        @Param("end") Instant end,
        @Param("userId") String userId,
        @Param("status") Integer status,
        @Param("source") Integer source,
        @Param("model") String model,
        @Param("tagId") Long tagId);
}
