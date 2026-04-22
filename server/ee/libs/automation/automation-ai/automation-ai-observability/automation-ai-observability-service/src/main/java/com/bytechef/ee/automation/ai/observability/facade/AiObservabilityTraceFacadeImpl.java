/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.observability.facade;

import com.bytechef.ee.automation.ai.observability.service.WorkspaceAiObservabilityTraceService;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityTrace;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityTraceSource;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityTraceStatus;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * Implementation of {@link AiObservabilityTraceFacade}. Delegates to the shared
 * {@code WorkspaceAiObservabilityTraceService} and carries the authorization guard so it is enforced for every caller
 * of the facade rather than only the GraphQL entry point.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
class AiObservabilityTraceFacadeImpl implements AiObservabilityTraceFacade {

    private final WorkspaceAiObservabilityTraceService workspaceAiObservabilityTraceService;

    @SuppressFBWarnings("EI")
    AiObservabilityTraceFacadeImpl(WorkspaceAiObservabilityTraceService workspaceAiObservabilityTraceService) {
        this.workspaceAiObservabilityTraceService = workspaceAiObservabilityTraceService;
    }

    @Override
    @PreAuthorize("hasPermission(#workspaceId, 'Workspace', 'AI_GATEWAY_VIEW')")
    public List<AiObservabilityTrace> getTracesByWorkspaceFiltered(
        Long workspaceId, Instant start, Instant end, String userId, AiObservabilityTraceStatus status,
        AiObservabilityTraceSource source, String model, Long tagId) {

        return workspaceAiObservabilityTraceService.getTracesByWorkspaceFiltered(
            workspaceId, start, end, userId, status, source, model, tagId);
    }
}
