/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.facade;

import com.bytechef.ee.platform.ai.gateway.dto.AiGatewayChatCompletionRequest;
import com.bytechef.ee.platform.ai.gateway.dto.AiGatewayChatCompletionResponse;
import jakarta.annotation.Nullable;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import reactor.core.publisher.Flux;

/**
 * Facade for the AI LLM Gateway playground. Hosts the {@code ADMIN} authorization guard so the playground entry point
 * is protected without placing the guard on the shared {@code AiGatewayFacade#chatCompletion}, which also serves the
 * public data-plane traffic.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AiGatewayPlaygroundFacade {

    AiGatewayChatCompletionResponse playgroundChatCompletion(AiGatewayChatCompletionRequest request);

    /**
     * Streaming counterpart of {@link #playgroundChatCompletion}, carrying the workspace-membership guard that the SSE
     * entry point used to apply in its own method body.
     *
     * <p>
     * The request arrives as a {@link Supplier} rather than a built request because building it can throw (an unknown
     * chat role, say) and the caller frames such a throw as an {@code event: error} SSE frame rather than an abruptly
     * closed stream. Construction therefore has to stay inside the returned Flux, while the guard has to run
     * <em>before</em> it — a denial is a 403 on the request, not a frame on an accepted stream. Splitting the two
     * across the facade boundary is what keeps both true.
     *
     * @param workspaceId     the workspace the run is billed and traced against
     * @param requestSupplier builds the gateway request, invoked on subscription
     * @param traceIdHolder   receives the created trace's internal id; see
     *                        {@code AiGatewayFacade#chatCompletionStream(AiGatewayChatCompletionRequest,
     *                       AiObservabilityTracingHeaders, AiPromptHeaders, AtomicLong)}
     * @return the streamed chat-completion chunks
     */
    Flux<AiGatewayChatCompletionResponse> playgroundChatCompletionStream(
        long workspaceId, Supplier<AiGatewayChatCompletionRequest> requestSupplier,
        @Nullable AtomicLong traceIdHolder);
}
