/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.automation.ai.gateway.facade.AiGatewayPlaygroundFacade;
import com.bytechef.ee.platform.ai.gateway.dto.AiGatewayChatCompletionRequest;
import com.bytechef.ee.platform.ai.gateway.dto.AiGatewayChatCompletionResponse;
import com.bytechef.ee.platform.ai.gateway.dto.AiGatewayChatMessage;
import com.bytechef.ee.platform.ai.gateway.dto.AiGatewayChatRole;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
@ConditionalOnCoordinator
class AiGatewayPlaygroundGraphQlController {

    private final AiGatewayPlaygroundFacade aiGatewayPlaygroundFacade;

    @SuppressFBWarnings("EI")
    AiGatewayPlaygroundGraphQlController(AiGatewayPlaygroundFacade aiGatewayPlaygroundFacade) {
        this.aiGatewayPlaygroundFacade = aiGatewayPlaygroundFacade;
    }

    @MutationMapping
    public PlaygroundChatCompletionResponse playgroundChatCompletion(
        @Argument PlaygroundChatCompletionInput input) {

        List<AiGatewayChatMessage> messages = input.messages()
            .stream()
            .map(message -> new AiGatewayChatMessage(
                AiGatewayChatRole.valueOf(message.role()
                    .name()),
                message.content(), null, null, null))
            .toList();

        AiGatewayChatCompletionRequest request = new AiGatewayChatCompletionRequest(
            input.model(), messages, input.temperature(), input.maxTokens(), input.topP(), false, null, null);

        long startTime = System.currentTimeMillis();

        AiGatewayChatCompletionResponse response = aiGatewayPlaygroundFacade.playgroundChatCompletion(
            input.workspaceId(), request, input.promptId(), input.promptVariables());

        long latencyMs = System.currentTimeMillis() - startTime;

        String content = null;
        String finishReason = null;

        if (response.choices() != null && !response.choices()
            .isEmpty()) {

            AiGatewayChatCompletionResponse.Choice firstChoice = response.choices()
                .getFirst();

            if (firstChoice.message() != null) {
                content = firstChoice.message()
                    .content();
            }

            finishReason = firstChoice.finishReason();
        }

        Integer promptTokens = null;
        Integer completionTokens = null;
        Integer totalTokens = null;

        AiGatewayChatCompletionResponse.Usage usage = response.usage();

        if (usage != null) {
            promptTokens = usage.promptTokens();
            completionTokens = usage.completionTokens();
            totalTokens = usage.totalTokens();
        }

        return new PlaygroundChatCompletionResponse(
            content, response.model(), finishReason, promptTokens, completionTokens, totalTokens, null,
            (int) latencyMs, null);
    }

    @SuppressFBWarnings("EI")
    record PlaygroundChatCompletionInput(
        String model, List<PlaygroundChatMessageInput> messages, Double temperature, Integer maxTokens, Double topP,
        Long promptId, String promptVariables, long workspaceId) {
    }

    @SuppressFBWarnings("EI")
    record PlaygroundChatMessageInput(String content, PlaygroundChatRole role) {
    }

    enum PlaygroundChatRole {
        ASSISTANT, SYSTEM, USER
    }

    @SuppressFBWarnings("EI")
    record PlaygroundChatCompletionResponse(
        String content, String model, String finishReason, Integer promptTokens, Integer completionTokens,
        Integer totalTokens, BigDecimal cost, Integer latencyMs, Long traceId) {
    }
}
