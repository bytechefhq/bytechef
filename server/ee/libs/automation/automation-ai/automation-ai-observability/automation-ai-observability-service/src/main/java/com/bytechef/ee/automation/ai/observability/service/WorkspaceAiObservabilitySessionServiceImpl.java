/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.observability.service;

import com.bytechef.ee.automation.ai.observability.domain.WorkspaceAiObservabilitySession;
import com.bytechef.ee.automation.ai.observability.repository.WorkspaceAiObservabilitySessionRepository;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilitySession;
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

    private final AiObservabilitySessionService aiObservabilitySessionService;
    private final WorkspaceAiObservabilitySessionRepository workspaceAiObservabilitySessionRepository;

    WorkspaceAiObservabilitySessionServiceImpl(
        AiObservabilitySessionService aiObservabilitySessionService,
        WorkspaceAiObservabilitySessionRepository workspaceAiObservabilitySessionRepository) {

        this.aiObservabilitySessionService = aiObservabilitySessionService;
        this.workspaceAiObservabilitySessionRepository = workspaceAiObservabilitySessionRepository;
    }

    @Override
    public AiObservabilitySession createInWorkspace(AiObservabilitySession session, long workspaceId) {
        AiObservabilitySession saved = aiObservabilitySessionService.create(session);

        workspaceAiObservabilitySessionRepository.save(new WorkspaceAiObservabilitySession(saved.getId(), workspaceId));

        return saved;
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

        return workspaceAiObservabilitySessionRepository
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

        return workspaceAiObservabilitySessionRepository.findAllSessionsByWorkspaceId(workspaceId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiObservabilitySession> getSessionsByWorkspaceAndUser(Long workspaceId, String userId) {
        Validate.notNull(workspaceId, "workspaceId must not be null");
        Validate.notNull(userId, "userId must not be null");

        return workspaceAiObservabilitySessionRepository.findAllSessionsByWorkspaceIdAndUserId(workspaceId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Long getWorkspaceId(long sessionId) {
        return workspaceAiObservabilitySessionRepository.findByAiObservabilitySessionId(sessionId)
            .map(WorkspaceAiObservabilitySession::getWorkspaceId)
            .orElse(null);
    }
}
