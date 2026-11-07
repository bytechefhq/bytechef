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
import java.util.concurrent.atomic.AtomicLong;
import org.jspecify.annotations.Nullable;
import reactor.core.publisher.Flux;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AiGatewayFacade {

    AiGatewayChatCompletionResponse chatCompletion(
        AiGatewayChatCompletionRequest request, @Nullable AiObservabilityTracingHeaders tracingHeaders);

    AiGatewayChatCompletionResponse chatCompletion(
        AiGatewayChatCompletionRequest request, @Nullable AiObservabilityTracingHeaders tracingHeaders,
        @Nullable AiPromptHeaders promptHeaders);

    AiGatewayChatCompletionResponse chatCompletion(
        AiGatewayChatCompletionRequest request, @Nullable AiObservabilityTracingHeaders tracingHeaders,
        @Nullable AiPromptHeaders promptHeaders, @Nullable Long connectedUserId);

    Flux<AiGatewayChatCompletionResponse> chatCompletionStream(
        AiGatewayChatCompletionRequest request, @Nullable AiObservabilityTracingHeaders tracingHeaders);

    Flux<AiGatewayChatCompletionResponse> chatCompletionStream(
        AiGatewayChatCompletionRequest request, @Nullable AiObservabilityTracingHeaders tracingHeaders,
        @Nullable AiPromptHeaders promptHeaders);

    Flux<AiGatewayChatCompletionResponse> chatCompletionStream(
        AiGatewayChatCompletionRequest request, @Nullable AiObservabilityTracingHeaders tracingHeaders,
        @Nullable AiPromptHeaders promptHeaders, @Nullable AtomicLong traceIdHolder);

    Flux<AiGatewayChatCompletionResponse> chatCompletionStream(
        AiGatewayChatCompletionRequest request, @Nullable AiObservabilityTracingHeaders tracingHeaders,
        @Nullable AiPromptHeaders promptHeaders, @Nullable Long connectedUserId,
        @Nullable AtomicLong traceIdHolder);

    AiGatewayEmbeddingResponse embedding(
        AiGatewayEmbeddingRequest request, @Nullable AiObservabilityTracingHeaders tracingHeaders);
}
