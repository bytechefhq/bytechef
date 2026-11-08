/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.facade;

import com.bytechef.ee.platform.ai.gateway.dto.AiGatewayChatCompletionRequest;
import com.bytechef.ee.platform.ai.gateway.dto.AiGatewayChatCompletionResponse;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;
import reactor.core.publisher.Flux;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AiGatewayPlaygroundFacade {

    AiGatewayChatCompletionResponse playgroundChatCompletion(
        long workspaceId, AiGatewayChatCompletionRequest request, @Nullable Long promptId,
        @Nullable String promptVariables);

    Flux<AiGatewayChatCompletionResponse> playgroundChatCompletionStream(
        long workspaceId, Supplier<AiGatewayChatCompletionRequest> requestSupplier,
        @Nullable AtomicLong traceIdHolder);
}
