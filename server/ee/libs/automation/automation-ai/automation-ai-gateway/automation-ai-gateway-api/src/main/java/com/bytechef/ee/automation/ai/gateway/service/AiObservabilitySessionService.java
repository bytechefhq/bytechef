/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilitySession;
import java.util.List;

/**
 * @version ee
 */
public interface AiObservabilitySessionService {

    void create(AiObservabilitySession session);

    AiObservabilitySession getSession(long id);

    List<AiObservabilitySession> getSessionsByWorkspace(Long workspaceId);

    List<AiObservabilitySession> getSessionsByWorkspaceAndUser(Long workspaceId, String userId);

    AiObservabilitySession getOrCreateSession(Long workspaceId, Long projectId, String userId);

    AiObservabilitySession getOrCreateSessionByExternalId(
        Long workspaceId, String externalSessionId, Long projectId, String userId);
}
