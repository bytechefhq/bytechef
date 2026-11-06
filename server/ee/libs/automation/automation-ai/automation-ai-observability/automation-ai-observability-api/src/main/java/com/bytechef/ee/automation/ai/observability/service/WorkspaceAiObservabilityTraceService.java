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
 * @version ee
 */
public interface WorkspaceAiObservabilityTraceService {

    AiObservabilityTrace createInWorkspace(AiObservabilityTrace trace, long workspaceId);

    void deleteOlderThanByWorkspace(Instant date, Long workspaceId);

    Optional<AiObservabilityTrace> findByExternalTraceId(Long workspaceId, String externalTraceId);

    Long getWorkspaceId(long traceId);

    List<AiObservabilityTrace> getTracesByWorkspace(Long workspaceId, Instant start, Instant end);

    List<AiObservabilityTrace> getTracesByWorkspaceAndSource(
        Long workspaceId, AiObservabilityTraceSource source, Instant start, Instant end);

    List<AiObservabilityTrace> getTracesBySessionAndWorkspace(Long sessionId, Long workspaceId);

    List<AiObservabilityTrace> getTracesByWorkspaceFiltered(
        Long workspaceId, Instant start, Instant end,
        String userId, AiObservabilityTraceStatus status, AiObservabilityTraceSource source,
        String model, Long tagId);
}
