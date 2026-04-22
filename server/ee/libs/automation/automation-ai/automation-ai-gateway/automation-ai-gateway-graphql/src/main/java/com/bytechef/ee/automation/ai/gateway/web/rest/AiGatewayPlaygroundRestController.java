/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.automation.ai.gateway.dto.AiGatewayChatCompletionRequest;
import com.bytechef.ee.automation.ai.gateway.dto.AiGatewayChatCompletionResponse;
import com.bytechef.ee.automation.ai.gateway.dto.AiGatewayChatMessage;
import com.bytechef.ee.automation.ai.gateway.dto.AiGatewayChatRole;
import com.bytechef.ee.automation.ai.gateway.facade.AiGatewayFacade;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * REST controller exposing a Server-Sent Events endpoint for the AI Gateway Playground.
 *
 * <p>
 * Wraps {@link AiGatewayFacade#chatCompletionStream} with session-based authentication so playground streaming requests
 * go through the full gateway pipeline (routing, cost tracking, tracing with {@code source = PLAYGROUND}).
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@RestController
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
@ConditionalOnCoordinator
@RequestMapping("/api/internal/ai-gateway/playground")
@PreAuthorize("isAuthenticated()")
class AiGatewayPlaygroundRestController {

    private final AiGatewayFacade aiGatewayFacade;
    private final PermissionService permissionService;

    @SuppressFBWarnings("EI")
    AiGatewayPlaygroundRestController(AiGatewayFacade aiGatewayFacade, PermissionService permissionService) {
        this.aiGatewayFacade = aiGatewayFacade;
        this.permissionService = permissionService;
    }

    @PostMapping(value = "/chat/completions/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    Flux<ServerSentEvent<Object>> chatCompletionsStream(
        @RequestBody PlaygroundChatCompletionStreamInput input) {

        if (input.workspaceId() == null) {
            throw new IllegalArgumentException("workspaceId is required for playground streaming");
        }

        // Tenant guard: ROLE_ADMIN alone is not enough — a tenant admin who is not a member of the target
        // workspace could otherwise bill that workspace's budget / pollute its traces by stamping its id here.
        if (!permissionService.hasWorkspaceRole(input.workspaceId(), "EDITOR")) {
            throw new AccessDeniedException(
                "Not authorized for workspace " + input.workspaceId() + " (requires EDITOR)");
        }

        long startTime = System.currentTimeMillis();

        AtomicInteger inputTokens = new AtomicInteger(0);
        AtomicInteger outputTokens = new AtomicInteger(0);
        AtomicReference<String> finishReason = new AtomicReference<>();
        AtomicLong traceId = new AtomicLong(0);

        // Defer request construction and facade invocation inside the reactive pipeline so any synchronous throw
        // (invalid role, budget/rate-limit rejection, etc.) surfaces to onErrorResume as a framed `event: error`
        // SSE frame instead of an abruptly-closed stream.
        Flux<ServerSentEvent<Object>> chunks = Flux.defer(() -> {
            List<AiGatewayChatMessage> messages = input.messages()
                .stream()
                .map(message -> new AiGatewayChatMessage(
                    AiGatewayChatRole.valueOf(message.role()), message.content(), null, null, null))
                .toList();

            AiGatewayChatCompletionRequest request = new AiGatewayChatCompletionRequest(
                input.model(), messages, input.temperature(), input.maxTokens(), input.topP(),
                true, null, null, null, null, Map.of("workspace_id", input.workspaceId()
                    .toString()));

            return aiGatewayFacade.chatCompletionStream(request, null, null, traceId)
                .map(response -> {
                    String content = null;

                    if (response.choices() != null && !response.choices()
                        .isEmpty()) {

                        AiGatewayChatCompletionResponse.Choice choice = response.choices()
                            .getFirst();

                        if (choice.message() != null) {
                            content = choice.message()
                                .content();
                        }

                        if (choice.finishReason() != null) {
                            finishReason.set(choice.finishReason());
                        }
                    }

                    if (response.usage() != null) {
                        if (response.usage()
                            .promptTokens() > 0) {

                            inputTokens.set(response.usage()
                                .promptTokens());
                        }

                        if (response.usage()
                            .completionTokens() > 0) {

                            outputTokens.set(response.usage()
                                .completionTokens());
                        }
                    }

                    PlaygroundStreamChunkDto chunk = new PlaygroundStreamChunkDto(
                        content, false, null, null, null, null, null);

                    return ServerSentEvent.<Object>builder()
                        .data(chunk)
                        .build();
                });
        });

        return chunks.concatWith(Flux.defer(() -> {
            int latencyMs = (int) (System.currentTimeMillis() - startTime);

            PlaygroundStreamChunkDto finalChunk = new PlaygroundStreamChunkDto(
                null, true, latencyMs, inputTokens.get(), outputTokens.get(), null,
                traceId.get() == 0 ? null : traceId.get());

            return Flux.<ServerSentEvent<Object>>just(ServerSentEvent.<Object>builder()
                .data(finalChunk)
                .build());
        }))
            // Emit a framed error event so client-side SSE readers can surface the failure instead of seeing the stream
            // simply terminate. Without this, a mid-stream error (budget 402, upstream timeout, etc.) reaches the
            // browser as an abrupt close and the user sees nothing.
            .onErrorResume(exception -> Flux.just(ServerSentEvent.<Object>builder()
                .event("error")
                .data(new PlaygroundStreamErrorDto(
                    exception.getClass()
                        .getSimpleName(),
                    exception.getMessage() != null ? exception.getMessage() : "Streaming failed"))
                .build()));
    }

    @SuppressFBWarnings("EI")
    record PlaygroundChatCompletionStreamInput(
        String model, List<PlaygroundChatMessageInput> messages,
        Double temperature, Integer maxTokens, Double topP, Long workspaceId) {
    }

    @SuppressFBWarnings("EI")
    record PlaygroundChatMessageInput(String content, String role) {
    }

    public record PlaygroundStreamChunkDto(
        String content,
        boolean finished,
        Integer latencyMs,
        Integer inputTokens,
        Integer outputTokens,
        BigDecimal cost,
        Long traceId) {
    }

    public record PlaygroundStreamErrorDto(String type, String message) {
    }
}
