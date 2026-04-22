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
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityTraceTag;
import com.bytechef.ee.automation.ai.gateway.repository.AiObservabilitySpanRepository;
import com.bytechef.ee.automation.ai.gateway.repository.AiObservabilityTraceRepository;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
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
class AiObservabilityTraceServiceImpl implements AiObservabilityTraceService {

    private final AiObservabilitySpanRepository aiObservabilitySpanRepository;
    private final AiObservabilityTraceRepository aiObservabilityTraceRepository;

    AiObservabilityTraceServiceImpl(
        AiObservabilitySpanRepository aiObservabilitySpanRepository,
        AiObservabilityTraceRepository aiObservabilityTraceRepository) {

        this.aiObservabilitySpanRepository = aiObservabilitySpanRepository;
        this.aiObservabilityTraceRepository = aiObservabilityTraceRepository;
    }

    @Override
    public void create(AiObservabilityTrace trace) {
        Validate.notNull(trace, "trace must not be null");
        Validate.isTrue(trace.getId() == null, "trace id must be null");

        aiObservabilityTraceRepository.save(trace);
    }

    @Override
    public void deleteOlderThan(Instant date) {
        Validate.notNull(date, "date must not be null");

        // Bulk-delete spans in a single statement keyed off the trace's created_date, then delete traces.
        // Avoids the prior N+1 (one findAllByTraceId + deleteAll per expired trace).
        aiObservabilitySpanRepository.deleteAllByTraceCreatedDateBefore(date);

        aiObservabilityTraceRepository.deleteAllByCreatedDateBefore(date);
    }

    @Override
    public void deleteOlderThanByWorkspace(Instant date, Long workspaceId) {
        Validate.notNull(date, "date must not be null");
        Validate.notNull(workspaceId, "workspaceId must not be null");

        aiObservabilitySpanRepository.deleteAllByWorkspaceIdAndTraceCreatedDateBefore(workspaceId, date);

        aiObservabilityTraceRepository.deleteAllByWorkspaceIdAndCreatedDateBefore(workspaceId, date);
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
    public AiObservabilityTrace getTrace(long id) {
        return aiObservabilityTraceRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("AiObservabilityTrace not found with id: " + id));
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
        Validate.notNull(start, "start must not be null");
        Validate.notNull(end, "end must not be null");

        return aiObservabilityTraceRepository.findAllByWorkspaceIdAndSourceAndCreatedDateBetween(
            workspaceId, source.ordinal(), start, end);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiObservabilityTrace> getTracesByWorkspaceFiltered(
        Long workspaceId, Instant start, Instant end,
        String userId, AiObservabilityTraceStatus status, AiObservabilityTraceSource source,
        String model, Long tagId) {

        Validate.notNull(workspaceId, "workspaceId must not be null");
        Validate.notNull(start, "start must not be null");
        Validate.notNull(end, "end must not be null");

        return aiObservabilityTraceRepository.findAllByFilters(
            workspaceId, start, end, userId,
            status != null ? status.ordinal() : null,
            source != null ? source.ordinal() : null,
            model, tagId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiObservabilityTrace> getTracesBySession(Long sessionId) {
        Validate.notNull(sessionId, "sessionId must not be null");

        return aiObservabilityTraceRepository.findAllBySessionId(sessionId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiObservabilityTrace> getTracesBySessionAndWorkspace(Long sessionId, Long workspaceId) {
        Validate.notNull(sessionId, "sessionId must not be null");
        Validate.notNull(workspaceId, "workspaceId must not be null");

        return aiObservabilityTraceRepository.findAllBySessionIdAndWorkspaceId(sessionId, workspaceId);
    }

    @Override
    public void update(AiObservabilityTrace trace) {
        Validate.notNull(trace, "trace must not be null");
        Validate.notNull(trace.getId(), "trace id must not be null");

        aiObservabilityTraceRepository.save(trace);
    }

    @Override
    public AiObservabilityTrace setTraceTags(long traceId, List<Long> tagIds) {
        Validate.notNull(tagIds, "tagIds must not be null");

        AiObservabilityTrace trace = aiObservabilityTraceRepository.findById(traceId)
            .orElseThrow(() -> new IllegalArgumentException("AiObservabilityTrace not found with id: " + traceId));

        Set<AiObservabilityTraceTag> newTags = tagIds.stream()
            .map(AiObservabilityTraceTag::new)
            .collect(Collectors.toCollection(HashSet::new));

        trace.setTags(newTags);

        return aiObservabilityTraceRepository.save(trace);
    }
}
