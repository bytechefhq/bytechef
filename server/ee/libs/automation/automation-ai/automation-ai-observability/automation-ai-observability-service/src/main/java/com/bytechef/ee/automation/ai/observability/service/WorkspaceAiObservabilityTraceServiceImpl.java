/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.observability.service;

import com.bytechef.ee.automation.ai.observability.domain.WorkspaceAiObservabilityTrace;
import com.bytechef.ee.automation.ai.observability.repository.WorkspaceAiObservabilityTraceRepository;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityTrace;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityTraceSource;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityTraceStatus;
import com.bytechef.ee.platform.ai.observability.service.AiObservabilityTraceService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.Validate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 */
@Service
@Transactional
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
@SuppressFBWarnings("EI")
class WorkspaceAiObservabilityTraceServiceImpl implements WorkspaceAiObservabilityTraceService {

    private final AiObservabilityTraceService aiObservabilityTraceService;
    private final WorkspaceAiObservabilityTraceRepository workspaceAiObservabilityTraceRepository;

    WorkspaceAiObservabilityTraceServiceImpl(
        AiObservabilityTraceService aiObservabilityTraceService,
        WorkspaceAiObservabilityTraceRepository workspaceAiObservabilityTraceRepository) {

        this.aiObservabilityTraceService = aiObservabilityTraceService;
        this.workspaceAiObservabilityTraceRepository = workspaceAiObservabilityTraceRepository;
    }

    @Override
    public AiObservabilityTrace createInWorkspace(AiObservabilityTrace trace, long workspaceId) {
        AiObservabilityTrace saved = aiObservabilityTraceService.create(trace);

        workspaceAiObservabilityTraceRepository.save(new WorkspaceAiObservabilityTrace(saved.getId(), workspaceId));

        return saved;
    }

    @Override
    public void deleteOlderThanByWorkspace(Instant date, Long workspaceId) {
        Validate.notNull(date, "date must not be null");
        Validate.notNull(workspaceId, "workspaceId must not be null");

        // Spans must be deleted BEFORE the parent traces so the FK CASCADE on workspace_ai_observability_trace
        // doesn't wipe spans first; mirrors the original platform impl ordering.
        workspaceAiObservabilityTraceRepository
            .deleteAllSpansByWorkspaceIdAndTraceCreatedDateBefore(workspaceId, date);

        workspaceAiObservabilityTraceRepository.deleteAllByWorkspaceIdAndTraceCreatedDateBefore(workspaceId, date);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AiObservabilityTrace> findByExternalTraceId(Long workspaceId, String externalTraceId) {
        Validate.notNull(workspaceId, "workspaceId must not be null");
        Validate.notNull(externalTraceId, "externalTraceId must not be null");

        return workspaceAiObservabilityTraceRepository.findByWorkspaceIdAndExternalTraceId(workspaceId,
            externalTraceId);
    }

    @Override
    @Transactional(readOnly = true)
    public Long getWorkspaceId(long traceId) {
        return workspaceAiObservabilityTraceRepository.findByAiObservabilityTraceId(traceId)
            .map(WorkspaceAiObservabilityTrace::getWorkspaceId)
            .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiObservabilityTrace> getTracesByWorkspace(Long workspaceId, Instant start, Instant end) {
        Validate.notNull(workspaceId, "workspaceId must not be null");
        Validate.notNull(start, "start must not be null");
        Validate.notNull(end, "end must not be null");

        return workspaceAiObservabilityTraceRepository.findAllByWorkspaceIdAndCreatedDateBetween(
            workspaceId, start, end);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiObservabilityTrace> getTracesByWorkspaceAndSource(
        Long workspaceId, AiObservabilityTraceSource source, Instant start, Instant end) {

        Validate.notNull(workspaceId, "workspaceId must not be null");
        Validate.notNull(source, "source must not be null");

        return workspaceAiObservabilityTraceRepository.findAllByWorkspaceIdAndSourceAndCreatedDateBetween(
            workspaceId, source.ordinal(), start, end);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiObservabilityTrace> getTracesBySessionAndWorkspace(Long sessionId, Long workspaceId) {
        Validate.notNull(sessionId, "sessionId must not be null");
        Validate.notNull(workspaceId, "workspaceId must not be null");

        return workspaceAiObservabilityTraceRepository.findAllBySessionIdAndWorkspaceId(sessionId, workspaceId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiObservabilityTrace> getTracesByWorkspaceFiltered(
        Long workspaceId, Instant start, Instant end,
        String userId, AiObservabilityTraceStatus status, AiObservabilityTraceSource source,
        String model, Long tagId) {

        Validate.notNull(workspaceId, "workspaceId must not be null");

        return workspaceAiObservabilityTraceRepository.findAllByFilters(
            workspaceId, start, end, userId,
            status != null ? status.ordinal() : null,
            source != null ? source.ordinal() : null,
            model, tagId);
    }
}
