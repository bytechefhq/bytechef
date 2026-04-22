/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilitySpan;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityTrace;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityTraceSource;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityTraceStatus;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityTraceTag;
import com.bytechef.ee.automation.ai.gateway.service.AiObservabilitySpanService;
import com.bytechef.ee.automation.ai.gateway.service.AiObservabilityTraceService;
import com.bytechef.ee.automation.ai.gateway.web.graphql.authorization.WorkspaceAuthorization;
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
    private final AiObservabilityTraceService aiObservabilityTraceService;
    private final WorkspaceAuthorization workspaceAuthorization;

    @SuppressFBWarnings("EI")
    AiObservabilityTraceGraphQlController(
        AiObservabilitySpanService aiObservabilitySpanService,
        AiObservabilityTraceService aiObservabilityTraceService,
        WorkspaceAuthorization workspaceAuthorization) {

        this.aiObservabilitySpanService = aiObservabilitySpanService;
        this.aiObservabilityTraceService = aiObservabilityTraceService;
        this.workspaceAuthorization = workspaceAuthorization;
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public AiObservabilityTrace aiObservabilityTrace(@Argument long id) {
        AiObservabilityTrace trace = aiObservabilityTraceService.getTrace(id);

        workspaceAuthorization.requireWorkspaceRole(trace.getWorkspaceId(), "VIEWER");

        return trace;
    }

    @QueryMapping
    @PreAuthorize("@permissionService.hasWorkspaceRole(#workspaceId, 'VIEWER')")
    public List<AiObservabilityTrace> aiObservabilityTraces(
        @Argument Long workspaceId, @Argument long startDate, @Argument long endDate,
        @Argument String userId, @Argument AiObservabilityTraceStatus status,
        @Argument AiObservabilityTraceSource source, @Argument String model, @Argument Long tagId) {

        Instant start = Instant.ofEpochMilli(startDate);
        Instant end = Instant.ofEpochMilli(endDate);

        return aiObservabilityTraceService.getTracesByWorkspaceFiltered(
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

        workspaceAuthorization.requireWorkspaceRole(trace.getWorkspaceId(), "EDITOR");

        return aiObservabilityTraceService.setTraceTags(traceId, tagIds);
    }

}
