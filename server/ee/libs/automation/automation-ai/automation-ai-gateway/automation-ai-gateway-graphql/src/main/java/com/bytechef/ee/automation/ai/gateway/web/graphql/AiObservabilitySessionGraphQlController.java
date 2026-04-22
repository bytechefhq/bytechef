/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilitySession;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityTrace;
import com.bytechef.ee.automation.ai.gateway.service.AiObservabilitySessionService;
import com.bytechef.ee.automation.ai.gateway.service.AiObservabilityTraceService;
import com.bytechef.ee.automation.ai.gateway.web.graphql.authorization.WorkspaceAuthorization;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

/**
 * GraphQL controller for querying AI Observability sessions.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
@ConditionalOnCoordinator
class AiObservabilitySessionGraphQlController {

    private final AiObservabilitySessionService aiObservabilitySessionService;
    private final AiObservabilityTraceService aiObservabilityTraceService;
    private final WorkspaceAuthorization workspaceAuthorization;

    @SuppressFBWarnings("EI")
    AiObservabilitySessionGraphQlController(
        AiObservabilitySessionService aiObservabilitySessionService,
        AiObservabilityTraceService aiObservabilityTraceService,
        WorkspaceAuthorization workspaceAuthorization) {

        this.aiObservabilitySessionService = aiObservabilitySessionService;
        this.aiObservabilityTraceService = aiObservabilityTraceService;
        this.workspaceAuthorization = workspaceAuthorization;
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public AiObservabilitySession aiObservabilitySession(@Argument long id) {
        AiObservabilitySession session = aiObservabilitySessionService.getSession(id);

        workspaceAuthorization.requireWorkspaceRole(session.getWorkspaceId(), "VIEWER");

        return session;
    }

    @QueryMapping
    @PreAuthorize("@permissionService.hasWorkspaceRole(#workspaceId, 'VIEWER')")
    public List<AiObservabilitySession> aiObservabilitySessions(@Argument Long workspaceId) {
        return aiObservabilitySessionService.getSessionsByWorkspace(workspaceId);
    }

    @SchemaMapping(typeName = "AiObservabilitySession", field = "traceCount")
    public int traceCount(AiObservabilitySession session) {
        return aiObservabilityTraceService.getTracesBySessionAndWorkspace(session.getId(), session.getWorkspaceId())
            .size();
    }

    @SchemaMapping(typeName = "AiObservabilitySession", field = "traces")
    public List<AiObservabilityTrace> traces(AiObservabilitySession session) {
        return aiObservabilityTraceService.getTracesBySessionAndWorkspace(session.getId(), session.getWorkspaceId());
    }

}
