/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.observability.repository;

import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityTrace;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;

/**
 * Queries on the {@code ai_observability_trace} table, both workspace-agnostic (reusable from non-tenant contexts) and
 * workspace-scoped. A trace whose {@code workspace_id} is null belongs to no workspace and is invisible to every
 * workspace-scoped query below, which is the intended behavior.
 *
 * @version ee
 */
public interface AiObservabilityTraceRepository extends ListCrudRepository<AiObservabilityTrace, Long> {

    List<AiObservabilityTrace> findAllBySessionId(Long sessionId);

    List<AiObservabilityTrace> findAllByCreatedDateBefore(Instant date);

    void deleteAllByCreatedDateBefore(Instant date);

    Optional<AiObservabilityTrace> findByWorkspaceIdAndExternalTraceId(Long workspaceId, String externalTraceId);

    List<AiObservabilityTrace> findAllByWorkspaceIdAndCreatedDateBetween(Long workspaceId, Instant start, Instant end);

    List<AiObservabilityTrace> findAllByWorkspaceIdAndSourceAndCreatedDateBetween(
        Long workspaceId, int source, Instant start, Instant end);

    List<AiObservabilityTrace> findAllBySessionIdAndWorkspaceId(Long sessionId, Long workspaceId);

    /**
     * Cleanup helper: delete all span rows whose parent trace is older than {@code date} and belongs to
     * {@code workspaceId}. Mirrors {@link #deleteAllByWorkspaceIdAndTraceCreatedDateBefore} but operates on the span
     * table — must be called BEFORE deleting the parent traces, whose span FK would otherwise reject the delete, and it
     * returns the span count so the caller's per-workspace counter is taken before the rows disappear.
     */
    @Modifying
    @Query("""
        DELETE FROM ai_observability_span
        WHERE trace_id IN (
            SELECT t.id FROM ai_observability_trace t
            WHERE t.workspace_id = :workspaceId AND t.created_date < :date)
        """)
    int deleteAllSpansByWorkspaceIdAndTraceCreatedDateBefore(
        @Param("workspaceId") Long workspaceId, @Param("date") Instant date);

    /**
     * Cleanup helper: delete all trace rows older than {@code date} that belong to {@code workspaceId}. Stays an
     * explicit set-based statement rather than a derived delete — a derived delete would load every matching aggregate
     * (traces plus their tag collections) into memory, and this is the highest-volume table here.
     */
    @Modifying
    @Query("""
        DELETE FROM ai_observability_trace
        WHERE created_date < :date
          AND workspace_id = :workspaceId
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
        LEFT JOIN ai_observability_span s ON s.trace_id = t.id
        LEFT JOIN ai_observability_trace_tag tt ON tt.ai_observability_trace = t.id
        WHERE t.workspace_id = :workspaceId
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
