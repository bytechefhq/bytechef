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
public record AiGatewayEmbeddingResponse(
    String object,
    List<EmbeddingData> data,
    String model,
    Usage usage,
    AiGatewayChatCompletionResponse.GatewayMetadata gatewayMetadata) {

    public AiGatewayEmbeddingResponse(
        String object, List<EmbeddingData> data, String model, Usage usage) {
        this(object, data, model, usage, null);
    }

    @SuppressFBWarnings("EI")
    public record EmbeddingData(String object, int index, List<Float> embedding) {
    }

    public record Usage(int promptTokens, int totalTokens) {
    }
}
