/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.cache;

import com.bytechef.ee.platform.ai.gateway.dto.AiGatewayChatCompletionRequest;
import com.bytechef.ee.platform.ai.gateway.dto.AiGatewayChatCompletionResponse;

/**
 * @version ee
 */
public interface AiGatewayResponseCache {

    String AI_GATEWAY_RESPONSE_CACHE = "ai-gateway-response";

    AiGatewayChatCompletionResponse get(String cacheKey);

    AiGatewayChatCompletionResponse put(String cacheKey, AiGatewayChatCompletionResponse response);

    String computeCacheKey(AiGatewayChatCompletionRequest request);

    boolean shouldCache(AiGatewayChatCompletionRequest request);
}
