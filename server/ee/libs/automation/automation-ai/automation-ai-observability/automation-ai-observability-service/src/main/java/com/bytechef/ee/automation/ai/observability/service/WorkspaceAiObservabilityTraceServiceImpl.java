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
import com.bytechef.ee.platform.ai.observability.repository.AiObservabilityTraceRepository;
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

    private final AiObservabilityTraceRepository aiObservabilityTraceRepository;
    private final AiObservabilityTraceService aiObservabilityTraceService;

    WorkspaceAiObservabilityTraceServiceImpl(
        AiObservabilityTraceRepository aiObservabilityTraceRepository,
        AiObservabilityTraceService aiObservabilityTraceService) {

        this.aiObservabilityTraceRepository = aiObservabilityTraceRepository;
        this.aiObservabilityTraceService = aiObservabilityTraceService;
    }

    @Override
    public AiObservabilityTrace createInWorkspace(AiObservabilityTrace trace, long workspaceId) {
        trace.setWorkspaceId(workspaceId);

        return aiObservabilityTraceService.create(trace);
    }

    @Override
    public void deleteOlderThanByWorkspace(Instant date, Long workspaceId) {
        Validate.notNull(date, "date must not be null");
        Validate.notNull(workspaceId, "workspaceId must not be null");

        // Spans must be deleted BEFORE the parent traces so the span FK doesn't reject the trace delete; mirrors
        // the original platform impl ordering.
        aiObservabilityTraceRepository.deleteAllSpansByWorkspaceIdAndTraceCreatedDateBefore(workspaceId, date);

        aiObservabilityTraceRepository.deleteAllByWorkspaceIdAndTraceCreatedDateBefore(workspaceId, date);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AiObservabilityTrace> findByExternalTraceId(Long workspaceId, String externalTraceId) {
        Validate.notNull(workspaceId, "workspaceId must not be null");
        Validate.notNull(externalTraceId, "externalTraceId must not be null");

        return aiObservabilityTraceRepository.findByWorkspaceIdAndExternalTraceId(workspaceId, externalTraceId);
    }

    @Override
    @Transactional(readOnly = true)
    public Long getWorkspaceId(long traceId) {
        // findById rather than the service's getTrace: an unknown id must still yield null (the pre-collapse
        // "no membership row" answer) because callers use this as an authorization probe, not as a fetch.
        return aiObservabilityTraceRepository.findById(traceId)
            .map(AiObservabilityTrace::getWorkspaceId)
            .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiObservabilityTrace> getTracesByWorkspace(Long workspaceId, Instant start, Instant end) {
        Validate.notNull(workspaceId, "workspaceId must not be null");
        Validate.notNull(start, "start must not be null");
        Validate.notNull(end, "end must not be null");

        return aiObservabilityTraceRepository.findAllByWorkspaceIdAndCreatedDateBetween(workspaceId, start, end);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiObservabilityTrace> getTracesByWorkspaceAndSource(
        Long workspaceId, AiObservabilityTraceSource source, Instant start, Instant end) {

        Validate.notNull(workspaceId, "workspaceId must not be null");
        Validate.notNull(source, "source must not be null");

        return aiObservabilityTraceRepository.findAllByWorkspaceIdAndSourceAndCreatedDateBetween(
            workspaceId, source.ordinal(), start, end);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiObservabilityTrace> getTracesBySessionAndWorkspace(Long sessionId, Long workspaceId) {
        Validate.notNull(sessionId, "sessionId must not be null");
        Validate.notNull(workspaceId, "workspaceId must not be null");

        return aiObservabilityTraceRepository.findAllBySessionIdAndWorkspaceId(sessionId, workspaceId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiObservabilityTrace> getTracesByWorkspaceFiltered(
        Long workspaceId, Instant start, Instant end,
        String userId, AiObservabilityTraceStatus status, AiObservabilityTraceSource source,
        String model, Long tagId) {

        Validate.notNull(workspaceId, "workspaceId must not be null");

        return aiObservabilityTraceRepository.findAllByFilters(
            workspaceId, start, end, userId,
            status != null ? status.ordinal() : null,
            source != null ? source.ordinal() : null,
            model, tagId);
    }
}
