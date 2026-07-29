/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.observability.service;

import com.bytechef.ee.platform.ai.observability.domain.AiObservabilitySession;
import java.util.List;

/**
 * Workspace-scoped operations on {@link AiObservabilitySession}. Owns the session's {@code workspace_id} binding plus
 * the tenant-aware queries; entity-table CRUD belongs to the platform-side {@code AiObservabilitySessionService}.
 *
 * @version ee
 */
public interface WorkspaceAiObservabilitySessionService {

    AiObservabilitySession createInWorkspace(AiObservabilitySession session, long workspaceId);

    AiObservabilitySession getOrCreateSession(Long workspaceId, Long projectId, String userId);

    AiObservabilitySession getOrCreateSessionByExternalId(
        Long workspaceId, String externalSessionId, Long projectId, String userId);

    List<AiObservabilitySession> getSessionsByWorkspace(Long workspaceId);

    List<AiObservabilitySession> getSessionsByWorkspaceAndUser(Long workspaceId, String userId);

    Long getWorkspaceId(long sessionId);
}
