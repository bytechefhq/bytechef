/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.facade;

import com.bytechef.ee.platform.ai.gateway.dto.AiGatewayChatCompletionRequest;
import com.bytechef.ee.platform.ai.gateway.dto.AiGatewayChatCompletionResponse;

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
}
