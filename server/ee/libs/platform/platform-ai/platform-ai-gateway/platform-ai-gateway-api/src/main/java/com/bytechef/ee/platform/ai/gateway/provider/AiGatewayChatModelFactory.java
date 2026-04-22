/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.provider;

import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayProvider;
import org.springframework.ai.chat.model.ChatModel;

/**
 * @version ee
 */
public interface AiGatewayChatModelFactory {

    String AI_GATEWAY_CHAT_MODEL_CACHE = "ai-gateway-chat-model";

    ChatModel getChatModel(AiGatewayProvider provider);

    void evict(long providerId);

    void evictAll();
}
