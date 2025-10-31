/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.remote.client.facade;

import com.bytechef.ee.ai.copilot.dto.ContextDTO;
import com.bytechef.ee.ai.copilot.facade.AiCopilotFacade;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnProperty(prefix = "bytechef.ai.copilot", name = "enabled", havingValue = "true")
public class RemoteAiCopilotFacadeClient implements AiCopilotFacade {

    private final WebClient loadBalancedWebClient;
    private String baseUrl;

    public RemoteAiCopilotFacadeClient(
        @Value("${ai.copilot.base-url}") String baseUrl,
        @Autowired(
            required = false) @Qualifier("loadBalancedWebClientBuilder") WebClient.Builder loadBalancedWebClientBuilder) {
        if (loadBalancedWebClientBuilder == null) {
            this.baseUrl = baseUrl;

            loadBalancedWebClientBuilder = WebClient.builder();
        }

        this.loadBalancedWebClient = loadBalancedWebClientBuilder.build();
    }

    @Override
    public Flux<Map<String, ?>> chat(String message, String conversationId, ContextDTO context) {
        WebClient.RequestBodyUriSpec requestBodyUriSpec = loadBalancedWebClient
            .post();

        WebClient.RequestBodySpec requestBodySpec;

        if (baseUrl == null) {
            requestBodySpec = requestBodyUriSpec
                .uri(uriBuilder -> uriBuilder
                    .host("ai-copilot-app")
                    .path("/remote/ai-copilot/chat")
                    .build());
        } else {
            requestBodySpec = requestBodyUriSpec.uri(baseUrl + "/remote/ai-copilot/chat");
        }

        return requestBodySpec.bodyValue(new Request(message, conversationId, context))
            .exchangeToFlux(response -> response.bodyToFlux(new ParameterizedTypeReference<>() {}));
    }

    public record Request(String message, String conversationId, ContextDTO context) {
    }
}
