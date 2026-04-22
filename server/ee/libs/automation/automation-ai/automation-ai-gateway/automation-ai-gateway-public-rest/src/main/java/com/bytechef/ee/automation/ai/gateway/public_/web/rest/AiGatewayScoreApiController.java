/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.public_.web.rest;

import com.bytechef.ee.automation.ai.gateway.domain.AiEvalScore;
import com.bytechef.ee.automation.ai.gateway.domain.AiEvalScoreDataType;
import com.bytechef.ee.automation.ai.gateway.domain.AiEvalScoreSource;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityTrace;
import com.bytechef.ee.automation.ai.gateway.service.AiEvalScoreService;
import com.bytechef.ee.automation.ai.gateway.service.AiObservabilityTraceService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.math.BigDecimal;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @version ee
 */
@RestController
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
@RequestMapping("/api/ai-gateway/v1")
@SuppressFBWarnings("EI")
class AiGatewayScoreApiController {

    private final AiEvalScoreService aiEvalScoreService;
    private final AiObservabilityTraceService aiObservabilityTraceService;

    AiGatewayScoreApiController(
        AiEvalScoreService aiEvalScoreService,
        AiObservabilityTraceService aiObservabilityTraceService) {

        this.aiEvalScoreService = aiEvalScoreService;
        this.aiObservabilityTraceService = aiObservabilityTraceService;
    }

    @PostMapping("/scores")
    public ResponseEntity<ScoreResponseModel> createScore(@RequestBody ScoreRequestModel scoreRequestModel) {
        AiObservabilityTrace trace = aiObservabilityTraceService.getTrace(scoreRequestModel.traceId());

        AiEvalScoreDataType dataType = AiEvalScoreDataType.valueOf(
            scoreRequestModel.dataType()
                .toUpperCase());

        AiEvalScore score = buildScore(trace.getWorkspaceId(), scoreRequestModel, dataType);

        if (scoreRequestModel.spanId() != null) {
            score.setSpanId(scoreRequestModel.spanId());
        }

        if (scoreRequestModel.comment() != null) {
            score.setComment(scoreRequestModel.comment());
        }

        AiEvalScore savedScore = aiEvalScoreService.create(score);

        return ResponseEntity.ok(new ScoreResponseModel(savedScore.getId(), savedScore.getName()));
    }

    /**
     * Dispatches to the typed {@link AiEvalScore} factories so NUMERIC/BOOLEAN/CATEGORICAL cannot be persisted with a
     * payload that contradicts the declared dataType.
     */
    private static AiEvalScore buildScore(Long workspaceId, ScoreRequestModel request, AiEvalScoreDataType dataType) {
        return switch (dataType) {
            case NUMERIC -> {
                if (request.value() == null) {
                    throw new IllegalArgumentException(
                        "NUMERIC score '" + request.name() + "' requires a value");
                }

                yield AiEvalScore.numeric(
                    workspaceId, request.traceId(), request.name(), AiEvalScoreSource.API, request.value());
            }
            case BOOLEAN -> {
                if (request.value() == null) {
                    throw new IllegalArgumentException(
                        "BOOLEAN score '" + request.name() + "' requires a value (0 or 1)");
                }

                yield AiEvalScore.bool(
                    workspaceId, request.traceId(), request.name(), AiEvalScoreSource.API,
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
                    workspaceId, request.traceId(), request.name(), AiEvalScoreSource.API, request.stringValue());
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
