/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilitySession;
import com.bytechef.ee.automation.ai.gateway.repository.AiObservabilitySessionRepository;
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
class AiObservabilitySessionServiceImpl implements AiObservabilitySessionService {

    private final AiObservabilitySessionRepository aiObservabilitySessionRepository;

    AiObservabilitySessionServiceImpl(AiObservabilitySessionRepository aiObservabilitySessionRepository) {
        this.aiObservabilitySessionRepository = aiObservabilitySessionRepository;
    }

    @Override
    public void create(AiObservabilitySession session) {
        Validate.notNull(session, "session must not be null");
        Validate.isTrue(session.getId() == null, "session id must be null");

        aiObservabilitySessionRepository.save(session);
    }

    @Override
    @Transactional(readOnly = true)
    public AiObservabilitySession getSession(long id) {
        return aiObservabilitySessionRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("AiObservabilitySession not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiObservabilitySession> getSessionsByWorkspace(Long workspaceId) {
        Validate.notNull(workspaceId, "workspaceId must not be null");

        return aiObservabilitySessionRepository.findAllByWorkspaceId(workspaceId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiObservabilitySession> getSessionsByWorkspaceAndUser(Long workspaceId, String userId) {
        Validate.notNull(workspaceId, "workspaceId must not be null");
        Validate.notNull(userId, "userId must not be null");

        return aiObservabilitySessionRepository.findAllByWorkspaceIdAndUserId(workspaceId, userId);
    }

    @Override
    public AiObservabilitySession getOrCreateSession(Long workspaceId, Long projectId, String userId) {
        Validate.notNull(workspaceId, "workspaceId must not be null");

        AiObservabilitySession session = new AiObservabilitySession(workspaceId);

        session.setProjectId(projectId);
        session.setUserId(userId);

        return aiObservabilitySessionRepository.save(session);
    }

    @Override
    public AiObservabilitySession getOrCreateSessionByExternalId(
        Long workspaceId, String externalSessionId, Long projectId, String userId) {

        Validate.notNull(workspaceId, "workspaceId must not be null");
        Validate.notBlank(externalSessionId, "externalSessionId must not be blank");

        return aiObservabilitySessionRepository
            .findByWorkspaceIdAndExternalSessionId(workspaceId, externalSessionId)
            .orElseGet(() -> {
                AiObservabilitySession session = new AiObservabilitySession(workspaceId);

                session.setExternalSessionId(externalSessionId);
                session.setProjectId(projectId);
                session.setUserId(userId);

                return aiObservabilitySessionRepository.save(session);
            });
    }
}
