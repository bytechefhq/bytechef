/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.facade;

import com.bytechef.ee.platform.ai.gateway.dto.AiGatewayChatCompletionRequest;
import com.bytechef.ee.platform.ai.gateway.dto.AiGatewayChatCompletionResponse;
import com.bytechef.ee.platform.ai.gateway.dto.AiGatewayEmbeddingRequest;
import com.bytechef.ee.platform.ai.gateway.dto.AiGatewayEmbeddingResponse;
import com.bytechef.ee.platform.ai.gateway.dto.AiPromptHeaders;
import com.bytechef.ee.platform.ai.observability.facade.AiObservabilityTracingHeaders;
import jakarta.annotation.Nullable;
import java.util.concurrent.atomic.AtomicLong;
import reactor.core.publisher.Flux;

/**
 * Public surface for the AI Gateway façade. Lives in the api module so the public-rest layer (and other web-tier
 * callers) can depend on this interface without pulling in the service module's Spring AI clients, response cache, or
 * routing-strategy beans.
 *
 * @author Ivica Cardic
 * @version ee
 */
public interface AiGatewayFacade {

    AiGatewayChatCompletionResponse chatCompletion(
        AiGatewayChatCompletionRequest request, @Nullable AiObservabilityTracingHeaders tracingHeaders);

    AiGatewayChatCompletionResponse chatCompletion(
        AiGatewayChatCompletionRequest request, @Nullable AiObservabilityTracingHeaders tracingHeaders,
        @Nullable AiPromptHeaders promptHeaders);

    Flux<AiGatewayChatCompletionResponse> chatCompletionStream(
        AiGatewayChatCompletionRequest request, @Nullable AiObservabilityTracingHeaders tracingHeaders);

    Flux<AiGatewayChatCompletionResponse> chatCompletionStream(
        AiGatewayChatCompletionRequest request, @Nullable AiObservabilityTracingHeaders tracingHeaders,
        @Nullable AiPromptHeaders promptHeaders);

    /**
     * Streaming overload that reports the created trace's internal id back to the caller via {@code traceIdHolder}. The
     * holder is populated inside {@code doOnComplete} (after the trace row is persisted and before the downstream
     * Flux's terminal signal propagates), so a caller that appends a final "totals" chunk via
     * {@code concatWith(Flux.defer(...))} can read {@code traceIdHolder.get()} in the defer and include it in the final
     * chunk. Pass {@code null} if the caller does not need the trace id.
     */
    Flux<AiGatewayChatCompletionResponse> chatCompletionStream(
        AiGatewayChatCompletionRequest request, @Nullable AiObservabilityTracingHeaders tracingHeaders,
        @Nullable AiPromptHeaders promptHeaders, @Nullable AtomicLong traceIdHolder);

    AiGatewayEmbeddingResponse embedding(
        AiGatewayEmbeddingRequest request, @Nullable AiObservabilityTracingHeaders tracingHeaders);
}
