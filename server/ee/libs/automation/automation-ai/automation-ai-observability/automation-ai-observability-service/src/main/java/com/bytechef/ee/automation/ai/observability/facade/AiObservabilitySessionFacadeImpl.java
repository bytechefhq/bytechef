/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.observability.facade;

import com.bytechef.ee.automation.ai.observability.service.WorkspaceAiObservabilitySessionService;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilitySession;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * Implementation of {@link AiObservabilitySessionFacade}. Delegates to the shared
 * {@code WorkspaceAiObservabilitySessionService} and carries the authorization guard so it is enforced for every caller
 * of the facade rather than only the GraphQL entry point.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
class AiObservabilitySessionFacadeImpl implements AiObservabilitySessionFacade {

    private final WorkspaceAiObservabilitySessionService workspaceAiObservabilitySessionService;

    @SuppressFBWarnings("EI")
    AiObservabilitySessionFacadeImpl(WorkspaceAiObservabilitySessionService workspaceAiObservabilitySessionService) {
        this.workspaceAiObservabilitySessionService = workspaceAiObservabilitySessionService;
    }

    @Override
    @PreAuthorize("hasPermission(#workspaceId, 'Workspace', 'AI_GATEWAY_VIEW')")
    public List<AiObservabilitySession> getSessionsByWorkspace(Long workspaceId) {
        return workspaceAiObservabilitySessionService.getSessionsByWorkspace(workspaceId);
    }
}
