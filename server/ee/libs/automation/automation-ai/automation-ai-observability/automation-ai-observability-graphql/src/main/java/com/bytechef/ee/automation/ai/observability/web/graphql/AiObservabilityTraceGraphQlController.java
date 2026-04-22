/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.observability.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.automation.ai.observability.facade.AiObservabilityTraceFacade;
import com.bytechef.ee.automation.ai.observability.service.WorkspaceAiObservabilityTraceService;
import com.bytechef.ee.automation.ai.observability.web.graphql.authorization.WorkspaceAuthorization;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilitySpan;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityTrace;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityTraceSource;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityTraceStatus;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityTraceTag;
import com.bytechef.ee.platform.ai.observability.service.AiObservabilitySpanService;
import com.bytechef.ee.platform.ai.observability.service.AiObservabilityTraceService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

/**
 * GraphQL controller for querying AI Observability traces.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
@ConditionalOnCoordinator
class AiObservabilityTraceGraphQlController {

    private final AiObservabilitySpanService aiObservabilitySpanService;
    private final AiObservabilityTraceFacade aiObservabilityTraceFacade;
    private final WorkspaceAiObservabilityTraceService workspaceAiObservabilityTraceService;
    private final AiObservabilityTraceService aiObservabilityTraceService;
    private final WorkspaceAuthorization workspaceAuthorization;

    @SuppressFBWarnings("EI")
    AiObservabilityTraceGraphQlController(
        AiObservabilitySpanService aiObservabilitySpanService,
        AiObservabilityTraceFacade aiObservabilityTraceFacade,
        WorkspaceAiObservabilityTraceService workspaceAiObservabilityTraceService,
        AiObservabilityTraceService aiObservabilityTraceService,
        WorkspaceAuthorization workspaceAuthorization) {

        this.aiObservabilitySpanService = aiObservabilitySpanService;
        this.aiObservabilityTraceFacade = aiObservabilityTraceFacade;
        this.workspaceAiObservabilityTraceService = workspaceAiObservabilityTraceService;
        this.aiObservabilityTraceService = aiObservabilityTraceService;
        this.workspaceAuthorization = workspaceAuthorization;
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public AiObservabilityTrace aiObservabilityTrace(@Argument long id) {
        AiObservabilityTrace trace = aiObservabilityTraceService.getTrace(id);

        workspaceAuthorization.requireWorkspaceRole(workspaceAiObservabilityTraceService.getWorkspaceId(trace.getId()),
            "VIEWER");

        return trace;
    }

    @QueryMapping
    public List<AiObservabilityTrace> aiObservabilityTraces(
        @Argument Long workspaceId, @Argument long startDate, @Argument long endDate,
        @Argument String userId, @Argument AiObservabilityTraceStatus status,
        @Argument AiObservabilityTraceSource source, @Argument String model, @Argument Long tagId) {

        Instant start = Instant.ofEpochMilli(startDate);
        Instant end = Instant.ofEpochMilli(endDate);

        // Authorization (workspace VIEWER role) is enforced on AiObservabilityTraceFacade so it protects every caller
        // of
        // the facade, not just this GraphQL entry point.
        return aiObservabilityTraceFacade.getTracesByWorkspaceFiltered(
            workspaceId, start, end, userId, status, source, model, tagId);
    }

    @SchemaMapping(typeName = "AiObservabilityTrace", field = "spans")
    public List<AiObservabilitySpan> spans(AiObservabilityTrace trace) {
        return aiObservabilitySpanService.getSpansByTrace(trace.getId());
    }

    /**
     * Exposes platform Tag IDs attached to this trace. Client resolves names via a separate tag query so we avoid
     * cross-module join coupling here.
     */
    @SchemaMapping(typeName = "AiObservabilityTrace", field = "tagIds")
    public List<Long> tagIds(AiObservabilityTrace trace) {
        if (trace.getTags() == null) {
            return List.of();
        }

        return trace.getTags()
            .stream()
            .map(AiObservabilityTraceTag::getTagId)
            .toList();
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public AiObservabilityTrace setAiObservabilityTraceTags(
        @Argument long traceId, @Argument List<Long> tagIds) {

        AiObservabilityTrace trace = aiObservabilityTraceService.getTrace(traceId);

        workspaceAuthorization.requireWorkspaceRole(workspaceAiObservabilityTraceService.getWorkspaceId(trace.getId()),
            "EDITOR");

        return aiObservabilityTraceService.setTraceTags(traceId, tagIds);
    }

}
