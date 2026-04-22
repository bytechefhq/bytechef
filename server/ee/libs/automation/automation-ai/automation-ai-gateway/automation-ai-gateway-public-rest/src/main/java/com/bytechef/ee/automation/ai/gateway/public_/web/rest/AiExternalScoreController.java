/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.public_.web.rest;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.automation.ai.gateway.facade.AiExternalScoreFacade;
import com.bytechef.ee.automation.ai.gateway.public_.workspace.AiGatewayWorkspaceHeaderResolver;
import com.bytechef.ee.automation.ai.gateway.ratelimit.AiGatewayRateLimitChecker;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayRateLimitResult;
import com.bytechef.ee.platform.ai.gateway.dto.AiExternalScoreBatchRequest;
import com.bytechef.ee.platform.ai.gateway.dto.AiExternalScoreBatchResult;
import com.bytechef.ee.platform.ai.gateway.dto.AiExternalScoreRequest;
import com.bytechef.ee.platform.ai.gateway.dto.AiExternalScoreResult;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.annotation.Nullable;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * External Scores API — attaches scores produced by third-party evaluators (Ragas, DeepEval, TruLens, custom pipelines)
 * to existing traces and spans. Workspace ownership is enforced by the facade; a cross-workspace target raises
 * {@link com.bytechef.ee.platform.ai.gateway.exception.AiScoreWorkspaceBoundaryException} which the gateway's exception
 * handler maps to HTTP 403.
 *
 * @author Ivica Cardic
 * @version ee
 */
@RestController
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
@RequestMapping("/api/ai-gateway/v1")
@SuppressFBWarnings("EI")
class AiExternalScoreController {

    private final AiExternalScoreFacade aiExternalScoreFacade;
    private final AiGatewayRateLimitChecker aiGatewayRateLimitChecker;
    private final AiGatewayWorkspaceHeaderResolver workspaceHeaderResolver;
    private final ApplicationProperties applicationProperties;

    AiExternalScoreController(
        AiExternalScoreFacade aiExternalScoreFacade,
        @Nullable AiGatewayRateLimitChecker aiGatewayRateLimitChecker,
        AiGatewayWorkspaceHeaderResolver workspaceHeaderResolver,
        ApplicationProperties applicationProperties) {

        this.aiExternalScoreFacade = aiExternalScoreFacade;
        this.aiGatewayRateLimitChecker = aiGatewayRateLimitChecker;
        this.workspaceHeaderResolver = workspaceHeaderResolver;
        this.applicationProperties = applicationProperties;
    }

    @PostMapping("/traces/{traceId}/scores")
    public ResponseEntity<Object> recordTraceScore(
        @RequestHeader(name = "X-ByteChef-Workspace-Id", required = false) String workspaceHeader,
        @PathVariable("traceId") Long traceId,
        @RequestBody AiExternalScoreRequest request) {

        Long workspaceId = workspaceHeaderResolver.resolveAndVerify(workspaceHeader);

        if (workspaceId == null) {
            return missingWorkspaceResponse();
        }

        ResponseEntity<Object> rateLimitResponse = checkRateLimit(workspaceId);

        if (rateLimitResponse != null) {
            return rateLimitResponse;
        }

        AiExternalScoreResult result = aiExternalScoreFacade.recordTraceScore(workspaceId, traceId, request);

        return ResponseEntity.ok(result);
    }

    @PostMapping("/spans/{spanId}/scores")
    public ResponseEntity<Object> recordSpanScore(
        @RequestHeader(name = "X-ByteChef-Workspace-Id", required = false) String workspaceHeader,
        @PathVariable("spanId") Long spanId,
        @RequestBody AiExternalScoreRequest request) {

        Long workspaceId = workspaceHeaderResolver.resolveAndVerify(workspaceHeader);

        if (workspaceId == null) {
            return missingWorkspaceResponse();
        }

        ResponseEntity<Object> rateLimitResponse = checkRateLimit(workspaceId);

        if (rateLimitResponse != null) {
            return rateLimitResponse;
        }

        AiExternalScoreResult result = aiExternalScoreFacade.recordSpanScore(workspaceId, spanId, request);

        return ResponseEntity.ok(result);
    }

    @PostMapping("/scores/batch")
    public ResponseEntity<Object> recordBatch(
        @RequestHeader(name = "X-ByteChef-Workspace-Id", required = false) String workspaceHeader,
        @RequestBody AiExternalScoreBatchRequest request) {

        Long workspaceId = workspaceHeaderResolver.resolveAndVerify(workspaceHeader);

        if (workspaceId == null) {
            return missingWorkspaceResponse();
        }

        // Cap before rate-limiting so a single oversize body doesn't consume a token. The facade opens one
        // transaction per row, so an unbounded batch could monopolise the connection pool — symmetric with
        // AiGatewayOtlpController.maxSpansPerRequest.
        int maxBatchSize = applicationProperties.getAi()
            .getGateway()
            .getExternalScores()
            .getMaxBatchSize();

        int batchSize = request.scores()
            .size();

        if (batchSize > maxBatchSize) {
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(Map.of("error", Map.of(
                    "type", "too_many_scores",
                    "message", "Request contains " + batchSize + " scores; max is " + maxBatchSize)));
        }

        ResponseEntity<Object> rateLimitResponse = checkRateLimit(workspaceId);

        if (rateLimitResponse != null) {
            return rateLimitResponse;
        }

        AiExternalScoreBatchResult result = aiExternalScoreFacade.recordBatch(workspaceId, request);

        return ResponseEntity.ok(result);
    }

    private static ResponseEntity<Object> missingWorkspaceResponse() {
        return ResponseEntity.badRequest()
            .body(Map.of(
                "error", Map.of(
                    "message", "Request must include header 'X-ByteChef-Workspace-Id' with a numeric workspace id.",
                    "type", "missing_workspace_id")));
    }

    /**
     * Enforces the per-workspace control-plane rate limit if the checker bean is present (it is wired only when
     * {@code bytechef.ai.gateway.rate-limiting.enabled=true}). Returns {@code null} when the request is allowed or when
     * rate-limiting is disabled; returns a 429 {@link ResponseEntity} with a {@code Retry-After} header when the
     * workspace is over its budget.
     */
    @Nullable
    private ResponseEntity<Object> checkRateLimit(Long workspaceId) {
        if (aiGatewayRateLimitChecker == null) {
            return null;
        }

        AiGatewayRateLimitResult result = aiGatewayRateLimitChecker.checkWorkspaceRequest(workspaceId, "scores");

        if (!result.allowed()) {
            long retryAfterSeconds = Math.max(0L, (result.resetAtEpochMs() - System.currentTimeMillis()) / 1000L);

            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", String.valueOf(retryAfterSeconds))
                .body(Map.of("error", Map.of(
                    "type", "rate_limit_exceeded",
                    "message", "Rate limit exceeded; retry after " + retryAfterSeconds + " seconds.")));
        }

        return null;
    }
}
