/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilitySpan;
import com.bytechef.ee.automation.ai.gateway.repository.AiObservabilitySpanRepository;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
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
class AiObservabilitySpanServiceImpl implements AiObservabilitySpanService {

    private final AiObservabilitySpanRepository aiObservabilitySpanRepository;

    AiObservabilitySpanServiceImpl(AiObservabilitySpanRepository aiObservabilitySpanRepository) {
        this.aiObservabilitySpanRepository = aiObservabilitySpanRepository;
    }

    @Override
    public void create(AiObservabilitySpan span) {
        Validate.notNull(span, "span must not be null");
        Validate.isTrue(span.getId() == null, "span id must be null");

        aiObservabilitySpanRepository.save(span);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiObservabilitySpan> getSpansByTrace(Long traceId) {
        Validate.notNull(traceId, "traceId must not be null");

        return aiObservabilitySpanRepository.findAllByTraceId(traceId);
    }

    @Override
    public void update(AiObservabilitySpan span) {
        Validate.notNull(span, "span must not be null");
        Validate.notNull(span.getId(), "span id must not be null");

        aiObservabilitySpanRepository.save(span);
    }
}
