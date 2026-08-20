/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.automation.ai.gateway.facade.AiGatewayPlaygroundFacade;
import com.bytechef.ee.platform.ai.gateway.dto.AiGatewayChatCompletionRequest;
import com.bytechef.ee.platform.ai.gateway.dto.AiGatewayChatCompletionResponse;
import com.bytechef.ee.platform.ai.gateway.dto.AiGatewayChatMessage;
import com.bytechef.ee.platform.ai.gateway.dto.AiGatewayChatRole;
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
 * Wraps {@link AiGatewayPlaygroundFacade#playgroundChatCompletionStream} with session-based authentication so
 * playground streaming requests go through the full gateway pipeline (routing, cost tracking, tracing with
 * {@code source = PLAYGROUND}).
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

    private final AiGatewayPlaygroundFacade aiGatewayPlaygroundFacade;

    @SuppressFBWarnings("EI")
    AiGatewayPlaygroundRestController(AiGatewayPlaygroundFacade aiGatewayPlaygroundFacade) {
        this.aiGatewayPlaygroundFacade = aiGatewayPlaygroundFacade;
    }

    /**
     * Authorization lives on
     * {@link AiGatewayPlaygroundFacade#playgroundChatCompletionStream(long, java.util.function.Supplier, AtomicLong)},
     * which requires at least the EDITOR role in the workspace the run is billed and traced against &mdash; the API
     * facade is this codebase's authorization layer, and this controller carries no gate of its own. That check used to
     * live in this method's body, where it was invisible to any audit scanning for {@code @PreAuthorize}.
     */
    @PostMapping(value = "/chat/completions/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    Flux<ServerSentEvent<Object>> chatCompletionsStream(
        @RequestBody PlaygroundChatCompletionStreamInput input) {

        if (input.workspaceId() == null) {
            throw new IllegalArgumentException("workspaceId is required for playground streaming");
        }

        long startTime = System.currentTimeMillis();

        AtomicInteger inputTokens = new AtomicInteger(0);
        AtomicInteger outputTokens = new AtomicInteger(0);
        AtomicReference<String> finishReason = new AtomicReference<>();
        AtomicLong traceId = new AtomicLong(0);

        // The facade defers request construction and the gateway invocation inside the reactive pipeline so any
        // synchronous throw (invalid role, budget/rate-limit rejection, etc.) surfaces to onErrorResume as a framed
        // `event: error` SSE frame instead of an abruptly-closed stream. The workspace guard it carries deliberately
        // runs outside that deferral, so a denial is a 403 on the request rather than a frame on an accepted stream.
        Flux<ServerSentEvent<Object>> chunks = aiGatewayPlaygroundFacade
            .playgroundChatCompletionStream(input.workspaceId(), () -> buildRequest(input), traceId)
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

    private static AiGatewayChatCompletionRequest buildRequest(PlaygroundChatCompletionStreamInput input) {
        List<AiGatewayChatMessage> messages = input.messages()
            .stream()
            .map(message -> new AiGatewayChatMessage(
                AiGatewayChatRole.valueOf(message.role()), message.content(), null, null, null))
            .toList();

        return new AiGatewayChatCompletionRequest(
            input.model(), messages, input.temperature(), input.maxTokens(), input.topP(),
            true, null, null, null, null, Map.of("workspace_id", input.workspaceId()
                .toString()));
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
