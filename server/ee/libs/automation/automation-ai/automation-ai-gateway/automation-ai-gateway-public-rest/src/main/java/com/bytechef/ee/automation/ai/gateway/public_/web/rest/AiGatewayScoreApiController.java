/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.public_.web.rest;

import com.bytechef.ee.automation.ai.eval.service.WorkspaceAiEvalScoreService;
import com.bytechef.ee.automation.ai.gateway.public_.workspace.AiGatewayWorkspaceHeaderResolver;
import com.bytechef.ee.automation.ai.observability.service.WorkspaceAiObservabilityTraceService;
import com.bytechef.ee.platform.ai.eval.domain.AiEvalScore;
import com.bytechef.ee.platform.ai.eval.domain.AiEvalScoreDataType;
import com.bytechef.ee.platform.ai.eval.domain.AiEvalScoreSource;
import com.bytechef.ee.platform.ai.gateway.exception.AiScoreTargetType;
import com.bytechef.ee.platform.ai.gateway.exception.AiScoreWorkspaceBoundaryException;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityTrace;
import com.bytechef.ee.platform.ai.observability.service.AiObservabilityTraceService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.math.BigDecimal;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Ivica Cardic
 * @version ee
 */
@RestController
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
@RequestMapping("/api/ai-gateway/v1")
@SuppressFBWarnings("EI")
class AiGatewayScoreApiController {

    private final WorkspaceAiEvalScoreService workspaceAiEvalScoreService;
    private final AiObservabilityTraceService aiObservabilityTraceService;
    private final WorkspaceAiObservabilityTraceService workspaceAiObservabilityTraceService;
    private final AiGatewayWorkspaceHeaderResolver workspaceHeaderResolver;

    AiGatewayScoreApiController(
        WorkspaceAiEvalScoreService workspaceAiEvalScoreService,
        AiObservabilityTraceService aiObservabilityTraceService,
        WorkspaceAiObservabilityTraceService workspaceAiObservabilityTraceService,
        AiGatewayWorkspaceHeaderResolver workspaceHeaderResolver) {

        this.workspaceAiEvalScoreService = workspaceAiEvalScoreService;
        this.aiObservabilityTraceService = aiObservabilityTraceService;
        this.workspaceAiObservabilityTraceService = workspaceAiObservabilityTraceService;
        this.workspaceHeaderResolver = workspaceHeaderResolver;
    }

    @PostMapping("/scores")
    public ResponseEntity<Object> createScore(
        @RequestHeader(name = "X-ByteChef-Workspace-Id", required = false) String workspaceHeader,
        @RequestBody ScoreRequestModel scoreRequestModel) {

        Long workspaceId = workspaceHeaderResolver.resolveAndVerify(workspaceHeader);

        if (workspaceId == null) {
            return missingWorkspaceResponse();
        }

        AiObservabilityTrace trace = aiObservabilityTraceService.getTrace(scoreRequestModel.traceId());

        // The trace's workspace must match the caller's resolved workspace; otherwise this becomes a
        // cross-tenant write (any authenticated API key writing scores against any tenant's traces by
        // guessing the trace id). Mirrors AiExternalScoreFacade.assertSameWorkspace.
        if (!workspaceId.equals(workspaceAiObservabilityTraceService.getWorkspaceId(trace.getId()))) {
            throw AiScoreWorkspaceBoundaryException.forTarget(
                workspaceId, AiScoreTargetType.TRACE, scoreRequestModel.traceId());
        }

        AiEvalScoreDataType dataType = AiEvalScoreDataType.valueOf(
            scoreRequestModel.dataType()
                .toUpperCase());

        AiEvalScore score = buildScore(scoreRequestModel, dataType);

        if (scoreRequestModel.spanId() != null) {
            score.setSpanId(scoreRequestModel.spanId());
        }

        if (scoreRequestModel.comment() != null) {
            score.setComment(scoreRequestModel.comment());
        }

        AiEvalScore savedScore = workspaceAiEvalScoreService.createInWorkspace(score, workspaceId);

        return ResponseEntity.ok(new ScoreResponseModel(savedScore.getId(), savedScore.getName()));
    }

    private static ResponseEntity<Object> missingWorkspaceResponse() {
        return ResponseEntity.badRequest()
            .body(Map.of(
                "error", Map.of(
                    "message", "Request must include header 'X-ByteChef-Workspace-Id' with a numeric workspace id.",
                    "type", "missing_workspace_id")));
    }

    /**
     * Dispatches to the typed {@link AiEvalScore} factories so NUMERIC/BOOLEAN/CATEGORICAL cannot be persisted with a
     * payload that contradicts the declared dataType.
     */
    private static AiEvalScore buildScore(ScoreRequestModel request, AiEvalScoreDataType dataType) {
        return switch (dataType) {
            case NUMERIC -> {
                if (request.value() == null) {
                    throw new IllegalArgumentException(
                        "NUMERIC score '" + request.name() + "' requires a value");
                }

                yield AiEvalScore.numeric(
                    request.traceId(), request.name(), AiEvalScoreSource.API, request.value());
            }
            case BOOLEAN -> {
                if (request.value() == null) {
                    throw new IllegalArgumentException(
                        "BOOLEAN score '" + request.name() + "' requires a value (0 or 1)");
                }

                yield AiEvalScore.bool(
                    request.traceId(), request.name(), AiEvalScoreSource.API,
                    request.value()
                        .signum() != 0);
            }
            case CATEGORICAL -> {
                if (request.stringValue() == null || request.stringValue()
                    .isBlank()) {

                    throw new IllegalArgumentException(
                        "CATEGORICAL score '" + request.name() + "' requires a non-blank stringValue");
                }

                yield AiEvalScore.categorical(
                    request.traceId(), request.name(), AiEvalScoreSource.API, request.stringValue());
            }
        };
    }

    record ScoreRequestModel(
        String comment,
        String dataType,
        String name,
        Long spanId,
        String stringValue,
        Long traceId,
        BigDecimal value) {
    }

    record ScoreResponseModel(Long id, String name) {
    }
}
