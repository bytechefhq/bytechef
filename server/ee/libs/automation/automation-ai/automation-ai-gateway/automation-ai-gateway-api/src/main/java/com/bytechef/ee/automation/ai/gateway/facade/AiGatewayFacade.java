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
import reactor.core.publisher.Flux;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AiGatewayFacade {

    AiGatewayChatCompletionResponse chatCompletion(AiGatewayChatCompletionRequest request);

    Flux<AiGatewayChatCompletionResponse> chatCompletionStream(AiGatewayChatCompletionRequest request);

    AiGatewayEmbeddingResponse embedding(AiGatewayEmbeddingRequest request);
}
