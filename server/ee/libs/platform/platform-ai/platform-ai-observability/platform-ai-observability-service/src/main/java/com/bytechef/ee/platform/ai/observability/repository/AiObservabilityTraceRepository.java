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
import org.springframework.data.repository.ListCrudRepository;

/**
 * Workspace-agnostic queries on the {@code ai_observability_trace} table. Tenant-scoped queries (by-workspace listings,
 * per-workspace retention deletes, by-workspace external-id lookups) live on
 * {@code WorkspaceAiObservabilityTraceRepository} so the platform repo is reusable from non-tenant contexts.
 *
 * @version ee
 */
public interface AiObservabilityTraceRepository extends ListCrudRepository<AiObservabilityTrace, Long> {

    List<AiObservabilityTrace> findAllBySessionId(Long sessionId);

    List<AiObservabilityTrace> findAllByCreatedDateBefore(Instant date);

    void deleteAllByCreatedDateBefore(Instant date);
}
