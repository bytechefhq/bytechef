/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.public_.web.rest;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.automation.ai.gateway.public_.workspace.AiGatewayWorkspaceHeaderResolver;
import com.bytechef.ee.automation.ai.gateway.ratelimit.AiGatewayRateLimitChecker;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayRateLimitResult;
import com.bytechef.ee.platform.ai.gateway.otlp.dto.OtelSpanBatch;
import com.bytechef.ee.platform.ai.gateway.otlp.mapper.OtlpProtobufMapper;
import com.bytechef.ee.platform.ai.observability.facade.AiObservabilityOtlpIngestFacade;
import com.bytechef.ee.platform.ai.observability.facade.OtlpIngestResult;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.google.protobuf.InvalidProtocolBufferException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import jakarta.annotation.Nullable;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * OTLP traces ingest endpoint. Accepts {@code application/x-protobuf} (OTLP/HTTP spec) and maps spans into the existing
 * observability store. Mirrors the Langfuse OTel endpoint so customers can point
 * {@code OTEL_EXPORTER_OTLP_TRACES_ENDPOINT} at us without code changes.
 *
 * @author Ivica Cardic
 * @version ee
 */
@RestController
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
@RequestMapping("/api/ai-gateway/v1/otlp")
@SuppressFBWarnings("EI")
class AiGatewayOtlpController {

    // Generous upper bound for a single OTLP gen_ai span on the wire — typical spans are 1-4 KB; tail spans with
    // heavy attribute payloads (large prompt text, multi-modal content references) reach into the 32-64 KB range.
    // Bodies exceeding maxSpansPerRequest * BYTES_PER_SPAN_HEURISTIC are rejected before parseFrom materializes
    // the protobuf object graph, defending against an adversarial 100 MB body crammed with nested KvlistValue
    // attributes that would spike heap inside the parser before the span-count cap could fire. Operators tuning
    // server.tomcat.max-http-form-post-size still own the absolute wire-size cap; this is layered defence.
    private static final int BYTES_PER_SPAN_HEURISTIC = 64 * 1024;

    private final AiObservabilityOtlpIngestFacade aiObservabilityOtlpIngestFacade;
    private final AiGatewayRateLimitChecker aiGatewayRateLimitChecker;
    private final AiGatewayWorkspaceHeaderResolver workspaceHeaderResolver;
    private final ApplicationProperties applicationProperties;
    private final OtlpProtobufMapper otlpProtobufMapper;

    AiGatewayOtlpController(
        AiObservabilityOtlpIngestFacade aiObservabilityOtlpIngestFacade,
        @Nullable AiGatewayRateLimitChecker aiGatewayRateLimitChecker,
        AiGatewayWorkspaceHeaderResolver workspaceHeaderResolver, ApplicationProperties applicationProperties,
        OtlpProtobufMapper otlpProtobufMapper) {

        this.aiObservabilityOtlpIngestFacade = aiObservabilityOtlpIngestFacade;
        this.aiGatewayRateLimitChecker = aiGatewayRateLimitChecker;
        this.workspaceHeaderResolver = workspaceHeaderResolver;
        this.applicationProperties = applicationProperties;
        this.otlpProtobufMapper = otlpProtobufMapper;
    }

    @PostMapping(value = "/traces", consumes = {
        "application/x-protobuf", "application/octet-stream"
    }, produces = "application/json")
    public ResponseEntity<?> ingestTraces(
        @RequestHeader(name = "X-ByteChef-Workspace-Id", required = false) String workspaceHeader,
        @RequestBody byte[] body) {

        Long workspaceId = workspaceHeaderResolver.resolveAndVerify(workspaceHeader);

        if (workspaceId == null) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", Map.of(
                    "type", "missing_workspace_id",
                    "message", "Request must include header 'X-ByteChef-Workspace-Id' with a numeric workspace id.")));
        }

        if (aiGatewayRateLimitChecker != null) {
            AiGatewayRateLimitResult result = aiGatewayRateLimitChecker.checkOtlpIngest(workspaceId);

            if (!result.allowed()) {
                long retryAfterSeconds = Math.max(0L, (result.resetAtEpochMs() - System.currentTimeMillis()) / 1000L);

                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header("Retry-After", String.valueOf(retryAfterSeconds))
                    .body(Map.of("error", Map.of(
                        "type", "rate_limit_exceeded",
                        "message", "Rate limit exceeded; retry after " + retryAfterSeconds + " seconds.")));
            }
        }

        int maxSpans = applicationProperties.getAi()
            .getGateway()
            .getOtlp()
            .getMaxSpansPerRequest();

        // Wire-size pre-check BEFORE parseFrom — defends against an adversarial body that would spike heap
        // inside the parser (deeply nested KvlistValue attributes, oversized BYTES values) before the span-count
        // cap could fire. The bound is generous (64 KB per span) so legitimate workloads never trip it; bodies
        // crammed with non-gen_ai junk still consume parser memory and earn the early rejection.
        long maxBodyBytes = (long) maxSpans * BYTES_PER_SPAN_HEURISTIC;

        if (body.length > maxBodyBytes) {
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(Map.of("error", Map.of(
                    "type", "body_too_large",
                    "message", "Request body of " + body.length + " bytes exceeds the per-request budget of "
                        + maxBodyBytes + " bytes (" + maxSpans + " spans * " + BYTES_PER_SPAN_HEURISTIC
                        + " bytes/span heuristic); split the batch or raise both the bytechef.ai.gateway.otlp"
                        + ".maxSpansPerRequest knob and server.tomcat.max-http-form-post-size in tandem")));
        }

        ExportTraceServiceRequest request;

        try {
            request = ExportTraceServiceRequest.parseFrom(body);
        } catch (InvalidProtocolBufferException invalidProtocolBufferException) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", Map.of(
                    "type", "invalid_protobuf",
                    "message", "Request body is not a valid OTLP ExportTraceServiceRequest protobuf.")));
        }

        // Count protobuf spans BEFORE invoking toBatch — toBatch materializes a DTO per span (attribute maps,
        // resource attributes, etc.). For a 100k-span body submitted to defeat the cap, materializing the DTOs
        // first costs the heap before the 413 fires; counting on the wire shape is O(n) on integers and skips
        // the allocation. Includes both gen_ai and non-gen_ai spans because a body crammed with non-gen_ai
        // spans still consumes parser memory.
        int totalSpans = countSpans(request);

        if (totalSpans > maxSpans) {
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(Map.of("error", Map.of(
                    "type", "too_many_spans",
                    "message", "Request contains " + totalSpans + " spans; max is " + maxSpans)));
        }

        OtelSpanBatch batch = otlpProtobufMapper.toBatch(request);

        OtlpIngestResult result = aiObservabilityOtlpIngestFacade.ingest(workspaceId, batch);

        return ResponseEntity.status(HttpStatus.ACCEPTED)
            .body(result);
    }

    private static int countSpans(ExportTraceServiceRequest request) {
        int total = 0;

        for (var resourceSpans : request.getResourceSpansList()) {
            for (var scopeSpans : resourceSpans.getScopeSpansList()) {
                total += scopeSpans.getSpansCount();
            }
        }

        return total;
    }
}
