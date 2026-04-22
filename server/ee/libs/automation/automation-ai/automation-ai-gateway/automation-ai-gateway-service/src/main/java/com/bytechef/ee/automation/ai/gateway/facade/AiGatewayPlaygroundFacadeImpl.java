/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.facade;

import com.bytechef.ee.platform.ai.gateway.dto.AiGatewayChatCompletionRequest;
import com.bytechef.ee.platform.ai.gateway.dto.AiGatewayChatCompletionResponse;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.constant.AuthorityConstants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * Implementation of {@link AiGatewayPlaygroundFacade}. Delegates to the shared {@code AiGatewayFacade} (so playground
 * runs through the full routing/cost-tracking/tracing pipeline) and carries the {@code ADMIN} guard so it is enforced
 * for every caller of the facade.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
class AiGatewayPlaygroundFacadeImpl implements AiGatewayPlaygroundFacade {

    private final AiGatewayFacade aiGatewayFacade;

    @SuppressFBWarnings("EI")
    AiGatewayPlaygroundFacadeImpl(AiGatewayFacade aiGatewayFacade) {
        this.aiGatewayFacade = aiGatewayFacade;
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public AiGatewayChatCompletionResponse playgroundChatCompletion(AiGatewayChatCompletionRequest request) {
        return aiGatewayFacade.chatCompletion(request, null);
    }
}
