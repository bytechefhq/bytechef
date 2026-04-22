/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.repository;

import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityTrace;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;

/**
 * @version ee
 */
public interface AiObservabilityTraceRepository extends ListCrudRepository<AiObservabilityTrace, Long> {

    Optional<AiObservabilityTrace> findByWorkspaceIdAndExternalTraceId(Long workspaceId, String externalTraceId);

    List<AiObservabilityTrace> findAllByWorkspaceIdAndCreatedDateBetween(
        Long workspaceId, Instant start, Instant end);

    List<AiObservabilityTrace> findAllByWorkspaceIdAndSourceAndCreatedDateBetween(
        Long workspaceId, int source, Instant start, Instant end);

    List<AiObservabilityTrace> findAllBySessionId(Long sessionId);

    List<AiObservabilityTrace> findAllBySessionIdAndWorkspaceId(Long sessionId, Long workspaceId);

    List<AiObservabilityTrace> findAllByCreatedDateBefore(Instant date);

    void deleteAllByCreatedDateBefore(Instant date);

    void deleteAllByWorkspaceIdAndCreatedDateBefore(Long workspaceId, Instant date);

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
