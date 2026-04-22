/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.observability.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.automation.ai.observability.facade.AiObservabilitySessionFacade;
import com.bytechef.ee.automation.ai.observability.service.WorkspaceAiObservabilitySessionService;
import com.bytechef.ee.automation.ai.observability.service.WorkspaceAiObservabilityTraceService;
import com.bytechef.ee.automation.ai.observability.web.graphql.authorization.WorkspaceAuthorization;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilitySession;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityTrace;
import com.bytechef.ee.platform.ai.observability.service.AiObservabilitySessionService;
import com.bytechef.ee.platform.ai.observability.service.AiObservabilityTraceService;
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

    private final AiObservabilitySessionFacade aiObservabilitySessionFacade;
    private final AiObservabilitySessionService aiObservabilitySessionService;
    private final WorkspaceAiObservabilityTraceService workspaceAiObservabilityTraceService;
    private final WorkspaceAiObservabilitySessionService workspaceAiObservabilitySessionService;
    private final AiObservabilityTraceService aiObservabilityTraceService;
    private final WorkspaceAuthorization workspaceAuthorization;

    @SuppressFBWarnings("EI")
    AiObservabilitySessionGraphQlController(
        AiObservabilitySessionFacade aiObservabilitySessionFacade,
        AiObservabilitySessionService aiObservabilitySessionService,
        WorkspaceAiObservabilityTraceService workspaceAiObservabilityTraceService,
        WorkspaceAiObservabilitySessionService workspaceAiObservabilitySessionService,
        AiObservabilityTraceService aiObservabilityTraceService,
        WorkspaceAuthorization workspaceAuthorization) {

        this.aiObservabilitySessionFacade = aiObservabilitySessionFacade;
        this.aiObservabilitySessionService = aiObservabilitySessionService;
        this.workspaceAiObservabilityTraceService = workspaceAiObservabilityTraceService;
        this.workspaceAiObservabilitySessionService = workspaceAiObservabilitySessionService;
        this.aiObservabilityTraceService = aiObservabilityTraceService;
        this.workspaceAuthorization = workspaceAuthorization;
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public AiObservabilitySession aiObservabilitySession(@Argument long id) {
        AiObservabilitySession session = aiObservabilitySessionService.getSession(id);

        workspaceAuthorization.requireWorkspaceRole(
            workspaceAiObservabilitySessionService.getWorkspaceId(session.getId()),
            "VIEWER");

        return session;
    }

    @QueryMapping
    public List<AiObservabilitySession> aiObservabilitySessions(@Argument Long workspaceId) {
        // Authorization (workspace VIEWER role) is enforced on AiObservabilitySessionFacade so it protects every caller
        // of the facade, not just this GraphQL entry point.
        return aiObservabilitySessionFacade.getSessionsByWorkspace(workspaceId);
    }

    @SchemaMapping(typeName = "AiObservabilitySession", field = "traceCount")
    public int traceCount(AiObservabilitySession session) {
        return workspaceAiObservabilityTraceService
            .getTracesBySessionAndWorkspace(session.getId(),
                workspaceAiObservabilitySessionService.getWorkspaceId(session.getId()))
            .size();
    }

    @SchemaMapping(typeName = "AiObservabilitySession", field = "traces")
    public List<AiObservabilityTrace> traces(AiObservabilitySession session) {
        return workspaceAiObservabilityTraceService.getTracesBySessionAndWorkspace(session.getId(),
            workspaceAiObservabilitySessionService.getWorkspaceId(session.getId()));
    }

}
