/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.observability.service;

import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityTrace;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityTraceSource;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityTraceStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Workspace-scoped operations on {@link AiObservabilityTrace}. Owns the {@code workspace_ai_observability_trace}
 * membership row plus all tenant-aware queries (per-tenant retention, cross-tenant-leak-safe by-session lookups,
 * filtered listings). Entity-table CRUD belongs to the platform-side {@code AiObservabilityTraceService}.
 *
 * @version ee
 */
public interface WorkspaceAiObservabilityTraceService {

    AiObservabilityTrace createInWorkspace(AiObservabilityTrace trace, long workspaceId);

    /**
     * Workspace-scoped retention deletion. Prefer this variant in per-workspace cleanup loops to prevent cross-tenant
     * deletion when different workspaces have different retention policies.
     */
    void deleteOlderThanByWorkspace(Instant date, Long workspaceId);

    Optional<AiObservabilityTrace> findByExternalTraceId(Long workspaceId, String externalTraceId);

    Long getWorkspaceId(long traceId);

    List<AiObservabilityTrace> getTracesByWorkspace(Long workspaceId, Instant start, Instant end);

    List<AiObservabilityTrace> getTracesByWorkspaceAndSource(
        Long workspaceId, AiObservabilityTraceSource source, Instant start, Instant end);

    /**
     * Workspace-scoped equivalent of {@code AiObservabilityTraceService#getTracesBySession}. Prefer this variant from
     * any caller reachable by a workspace tenant to prevent cross-tenant trace leakage if session IDs ever collide or
     * are guessable.
     */
    List<AiObservabilityTrace> getTracesBySessionAndWorkspace(Long sessionId, Long workspaceId);

    List<AiObservabilityTrace> getTracesByWorkspaceFiltered(
        Long workspaceId, Instant start, Instant end,
        String userId, AiObservabilityTraceStatus status, AiObservabilityTraceSource source,
        String model, Long tagId);
}
