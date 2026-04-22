/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.observability.service;

import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityTrace;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityTraceTag;
import com.bytechef.ee.platform.ai.observability.repository.AiObservabilitySpanRepository;
import com.bytechef.ee.platform.ai.observability.repository.AiObservabilityTraceRepository;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
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
    public AiObservabilityTrace create(AiObservabilityTrace trace) {
        Validate.notNull(trace, "trace must not be null");
        Validate.isTrue(trace.getId() == null, "trace id must be null");

        return aiObservabilityTraceRepository.save(trace);
    }

    @Override
    public void deleteOlderThan(Instant date) {
        Validate.notNull(date, "date must not be null");

        // Bulk-delete spans in a single statement keyed off the trace's created_date, then delete traces —
        // a per-trace findAllByTraceId + deleteAll loop would issue an N+1 against datasets with millions of
        // expired rows.
        aiObservabilitySpanRepository.deleteAllByTraceCreatedDateBefore(date);

        aiObservabilityTraceRepository.deleteAllByCreatedDateBefore(date);
    }

    @Override
    @Transactional(readOnly = true)
    public AiObservabilityTrace getTrace(long id) {
        return aiObservabilityTraceRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("AiObservabilityTrace not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiObservabilityTrace> getTracesBySession(Long sessionId) {
        Validate.notNull(sessionId, "sessionId must not be null");

        return aiObservabilityTraceRepository.findAllBySessionId(sessionId);
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
