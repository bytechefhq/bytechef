/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.observability.service;

import com.bytechef.ee.platform.ai.observability.domain.AiObservabilitySession;
import com.bytechef.ee.platform.ai.observability.repository.AiObservabilitySessionRepository;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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
class AiObservabilitySessionServiceImpl implements AiObservabilitySessionService {

    private final AiObservabilitySessionRepository aiObservabilitySessionRepository;

    AiObservabilitySessionServiceImpl(AiObservabilitySessionRepository aiObservabilitySessionRepository) {
        this.aiObservabilitySessionRepository = aiObservabilitySessionRepository;
    }

    @Override
    public AiObservabilitySession create(AiObservabilitySession session) {
        Validate.notNull(session, "session must not be null");
        Validate.isTrue(session.getId() == null, "session id must be null");

        return aiObservabilitySessionRepository.save(session);
    }

    @Override
    @Transactional(readOnly = true)
    public AiObservabilitySession getSession(long id) {
        return aiObservabilitySessionRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("AiObservabilitySession not found with id: " + id));
    }
}
