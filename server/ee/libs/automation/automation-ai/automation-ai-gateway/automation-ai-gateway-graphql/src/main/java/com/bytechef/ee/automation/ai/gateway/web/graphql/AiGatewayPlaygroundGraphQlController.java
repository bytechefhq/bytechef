/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.ee.automation.ai.gateway.dto.AiGatewayChatCompletionRequest;
import com.bytechef.ee.automation.ai.gateway.dto.AiGatewayChatCompletionResponse;
import com.bytechef.ee.automation.ai.gateway.dto.AiGatewayChatMessage;
import com.bytechef.ee.automation.ai.gateway.dto.AiGatewayChatRole;
import com.bytechef.ee.automation.ai.gateway.facade.AiGatewayFacade;
import com.bytechef.ee.automation.ai.gateway.service.AiPromptVersionService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.constant.AuthorityConstants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

/**
 * GraphQL controller for the AI Gateway Playground.
 *
 * <p>
 * Wraps {@link AiGatewayFacade#chatCompletion} with session-based authentication so playground requests go through the
 * full gateway pipeline (routing, cost tracking, tracing with {@code source = PLAYGROUND}).
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
@ConditionalOnCoordinator
class AiGatewayPlaygroundGraphQlController {

    private final AiGatewayFacade aiGatewayFacade;
    private final AiPromptVersionService aiPromptVersionService;

    @SuppressFBWarnings("EI")
    AiGatewayPlaygroundGraphQlController(
        AiGatewayFacade aiGatewayFacade, AiPromptVersionService aiPromptVersionService) {

        this.aiGatewayFacade = aiGatewayFacade;
        this.aiPromptVersionService = aiPromptVersionService;
    }

    // Streaming support: the gateway facade supports stream=true for server-sent responses, but exposing incremental
    // chunks through GraphQL requires a @SubscriptionMapping backed by Reactor Flux and a WebSocket-aware GraphQL
    // transport. That is out of scope for this pass — the playground currently always issues a non-streaming request.
    // When subscription support is added, introduce a playgroundChatCompletionStream subscription that emits chunks.
    @MutationMapping
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public PlaygroundChatCompletionResponse playgroundChatCompletion(
        @Argument PlaygroundChatCompletionInput input) {

        List<AiGatewayChatMessage> messages = new ArrayList<>(input.messages()
            .stream()
            .map(message -> new AiGatewayChatMessage(
                AiGatewayChatRole.valueOf(message.role()
                    .name()),
                message.content(), null, null, null))
            .toList());

        // Managed-prompt mode: when a promptId is supplied, look up the active production-environment version,
        // substitute {{vars}} from promptVariables (JSON), and prepend as a system message. Frees the UI from
        // re-shipping prompt content on every playground run.
        if (input.promptId() != null) {
            aiPromptVersionService.getActiveVersion(input.promptId(), "production")
                .ifPresent(version -> {
                    String rendered = renderManagedPrompt(version.getContent(), input.promptVariables());

                    messages.addFirst(new AiGatewayChatMessage(
                        AiGatewayChatRole.SYSTEM, rendered, null, null, null));
                });
        }

        AiGatewayChatCompletionRequest request = new AiGatewayChatCompletionRequest(
            input.model(), messages, input.temperature(), input.maxTokens(), input.topP(),
            false, null, null);

        long startTime = System.currentTimeMillis();

        AiGatewayChatCompletionResponse response = aiGatewayFacade.chatCompletion(request, null);

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

        if (response.usage() != null) {
            promptTokens = response.usage()
                .promptTokens();
            completionTokens = response.usage()
                .completionTokens();
            totalTokens = response.usage()
                .totalTokens();
        }

        return new PlaygroundChatCompletionResponse(
            content, response.model(), finishReason, promptTokens, completionTokens,
            totalTokens, null, (int) latencyMs, null);
    }

    /**
     * Substitutes {@code {{var}}} placeholders in the prompt content with values from the caller-provided JSON map.
     * Unknown placeholders are left as-is so the downstream LLM still sees them — surfaces misconfiguration instead of
     * silently dropping them.
     */
    @SuppressWarnings("unchecked")
    private static String renderManagedPrompt(String template, String variablesJson) {
        if (template == null || template.isEmpty() || variablesJson == null || variablesJson.isBlank()) {
            return template;
        }

        Map<String, Object> variables;

        try {
            variables = JsonUtils.read(variablesJson, Map.class);
        } catch (RuntimeException parseFailure) {
            throw new IllegalArgumentException(
                "Malformed prompt variables JSON — fix the variables payload or clear it to run without substitution",
                parseFailure);
        }

        String rendered = template;

        for (var entry : variables.entrySet()) {
            String placeholder = "\\{\\{\\s*" + Pattern.quote(entry.getKey()) + "\\s*}}";
            String replacement = Matcher.quoteReplacement(
                entry.getValue() != null ? entry.getValue()
                    .toString() : "");

            rendered = rendered.replaceAll(placeholder, replacement);
        }

        return rendered;
    }

    @SuppressFBWarnings("EI")
    record PlaygroundChatCompletionInput(
        String model, List<PlaygroundChatMessageInput> messages,
        Double temperature, Integer maxTokens, Double topP,
        Long promptId, String promptVariables) {
    }

    @SuppressFBWarnings("EI")
    record PlaygroundChatMessageInput(String content, PlaygroundChatRole role) {
    }

    enum PlaygroundChatRole {
        ASSISTANT, SYSTEM, USER
    }

    @SuppressFBWarnings("EI")
    record PlaygroundChatCompletionResponse(
        String content, String model, String finishReason,
        Integer promptTokens, Integer completionTokens, Integer totalTokens,
        BigDecimal cost, Integer latencyMs, Long traceId) {
    }
}
