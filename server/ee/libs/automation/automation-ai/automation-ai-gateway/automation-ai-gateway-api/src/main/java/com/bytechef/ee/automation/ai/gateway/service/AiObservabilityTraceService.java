/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityTrace;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityTraceSource;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityTraceStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * @version ee
 */
public interface AiObservabilityTraceService {

    void create(AiObservabilityTrace trace);

    void deleteOlderThan(Instant date);

    /**
     * Workspace-scoped deletion. Prefer this variant in per-workspace cleanup loops to prevent cross-tenant deletion
     * when different workspaces have different retention policies.
     */
    void deleteOlderThanByWorkspace(Instant date, Long workspaceId);

    Optional<AiObservabilityTrace> findByExternalTraceId(Long workspaceId, String externalTraceId);

    AiObservabilityTrace getTrace(long id);

    List<AiObservabilityTrace> getTracesByWorkspace(Long workspaceId, Instant start, Instant end);

    List<AiObservabilityTrace> getTracesByWorkspaceAndSource(
        Long workspaceId, AiObservabilityTraceSource source, Instant start, Instant end);

    List<AiObservabilityTrace> getTracesBySession(Long sessionId);

    /**
     * Workspace-scoped equivalent of {@link #getTracesBySession(Long)}. Prefer this variant from any caller reachable
     * by a workspace tenant to prevent cross-tenant trace leakage if session IDs ever collide or are guessable.
     */
    List<AiObservabilityTrace> getTracesBySessionAndWorkspace(Long sessionId, Long workspaceId);

    List<AiObservabilityTrace> getTracesByWorkspaceFiltered(
        Long workspaceId, Instant start, Instant end,
        String userId, AiObservabilityTraceStatus status, AiObservabilityTraceSource source,
        String model, Long tagId);

    void update(AiObservabilityTrace trace);

    AiObservabilityTrace setTraceTags(long traceId, List<Long> tagIds);
}
