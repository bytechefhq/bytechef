/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.dto;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;

/**
 * @version ee
 */
@SuppressFBWarnings("EI")
public record AiGatewayChatCompletionResponse(
    String id,
    String object,
    long created,
    String model,
    List<Choice> choices,
    Usage usage,
    GatewayMetadata gatewayMetadata) {

    public AiGatewayChatCompletionResponse(
        String id, String object, long created, String model, List<Choice> choices, Usage usage) {
        this(id, object, created, model, choices, usage, null);
    }

    public record Choice(int index, AiGatewayChatMessage message, String finishReason) {
    }

    public record Usage(int promptTokens, int completionTokens, int totalTokens) {
    }

    /**
     * Transient gateway observability metadata. Populated by the facade; consumed by the public REST controllers to
     * emit {@code x-gateway-*} response headers. Never serialized in the JSON body.
     */
    public record GatewayMetadata(
        String provider,
        String model,
        Long latencyMs,
        Boolean cacheHit,
        String routingPolicy,
        String requestId,
        java.math.BigDecimal budgetWarningRemainingUsd) {

        public GatewayMetadata(
            String provider, String model, Long latencyMs, Boolean cacheHit, String routingPolicy, String requestId) {
            this(provider, model, latencyMs, cacheHit, routingPolicy, requestId, null);
        }
    }
}
