/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.observability.service;

import com.bytechef.ee.platform.ai.observability.domain.AiObservabilitySession;
import com.bytechef.ee.platform.ai.observability.repository.AiObservabilitySessionRepository;
import com.bytechef.ee.platform.ai.observability.service.AiObservabilitySessionService;
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
class WorkspaceAiObservabilitySessionServiceImpl implements WorkspaceAiObservabilitySessionService {

    private final AiObservabilitySessionRepository aiObservabilitySessionRepository;
    private final AiObservabilitySessionService aiObservabilitySessionService;

    WorkspaceAiObservabilitySessionServiceImpl(
        AiObservabilitySessionRepository aiObservabilitySessionRepository,
        AiObservabilitySessionService aiObservabilitySessionService) {

        this.aiObservabilitySessionRepository = aiObservabilitySessionRepository;
        this.aiObservabilitySessionService = aiObservabilitySessionService;
    }

    @Override
    public AiObservabilitySession createInWorkspace(AiObservabilitySession session, long workspaceId) {
        session.setWorkspaceId(workspaceId);

        return aiObservabilitySessionService.create(session);
    }

    @Override
    public AiObservabilitySession getOrCreateSession(Long workspaceId, Long projectId, String userId) {
        Validate.notNull(workspaceId, "workspaceId must not be null");

        AiObservabilitySession session = new AiObservabilitySession();

        session.setProjectId(projectId);
        session.setUserId(userId);

        return createInWorkspace(session, workspaceId);
    }

    @Override
    public AiObservabilitySession getOrCreateSessionByExternalId(
        Long workspaceId, String externalSessionId, Long projectId, String userId) {

        Validate.notNull(workspaceId, "workspaceId must not be null");
        Validate.notBlank(externalSessionId, "externalSessionId must not be blank");

        return aiObservabilitySessionRepository
            .findByWorkspaceIdAndExternalSessionId(workspaceId, externalSessionId)
            .orElseGet(() -> {
                AiObservabilitySession session = new AiObservabilitySession();

                session.setExternalSessionId(externalSessionId);
                session.setProjectId(projectId);
                session.setUserId(userId);

                return createInWorkspace(session, workspaceId);
            });
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
    @Transactional(readOnly = true)
    public Long getWorkspaceId(long sessionId) {
        // findById rather than the service's getSession: an unknown id must still yield null (the pre-collapse
        // "no membership row" answer) because callers use this as an authorization probe, not as a fetch.
        return aiObservabilitySessionRepository.findById(sessionId)
            .map(AiObservabilitySession::getWorkspaceId)
            .orElse(null);
    }
}
