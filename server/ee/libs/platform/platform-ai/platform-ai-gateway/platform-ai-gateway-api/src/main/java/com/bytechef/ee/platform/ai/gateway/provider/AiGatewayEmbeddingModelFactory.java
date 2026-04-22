/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.provider;

import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayProvider;
import org.springframework.ai.embedding.EmbeddingModel;

/**
 * @version ee
 */
public interface AiGatewayEmbeddingModelFactory {

    String AI_GATEWAY_EMBEDDING_MODEL_CACHE = "ai-gateway-embedding-model";

    EmbeddingModel getEmbeddingModel(AiGatewayProvider provider);

    void evict(long providerId);

    void evictAll();
}
