/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.facade;

import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.ee.automation.ai.prompt.service.WorkspaceAiPromptService;
import com.bytechef.ee.platform.ai.gateway.dto.AiGatewayChatCompletionRequest;
import com.bytechef.ee.platform.ai.gateway.dto.AiGatewayChatCompletionResponse;
import com.bytechef.ee.platform.ai.gateway.dto.AiGatewayChatMessage;
import com.bytechef.ee.platform.ai.gateway.dto.AiGatewayChatRole;
import com.bytechef.ee.platform.ai.gateway.dto.AiPromptHeaders;
import com.bytechef.ee.platform.ai.prompt.AiPromptVersion;
import com.bytechef.ee.platform.ai.prompt.AiPromptVersionService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
class AiGatewayPlaygroundFacadeImpl implements AiGatewayPlaygroundFacade {

    private static final Logger log = LoggerFactory.getLogger(AiGatewayPlaygroundFacadeImpl.class);

    private static final String DENIED_MESSAGE = "Not authorized for the requested workspace";
    private static final String MINIMUM_EDITOR_ROLE = "EDITOR";
    private static final String WORKSPACE_ID_TAG = "workspace_id";

    private final AiGatewayFacade aiGatewayFacade;
    private final AiPromptVersionService aiPromptVersionService;

    @Nullable
    private final PermissionService permissionService;

    private final WorkspaceAiPromptService workspaceAiPromptService;

    @SuppressFBWarnings("EI")
    AiGatewayPlaygroundFacadeImpl(
        AiGatewayFacade aiGatewayFacade, AiPromptVersionService aiPromptVersionService,
        @Nullable PermissionService permissionService, WorkspaceAiPromptService workspaceAiPromptService) {

        this.aiGatewayFacade = aiGatewayFacade;
        this.aiPromptVersionService = aiPromptVersionService;
        this.permissionService = permissionService;
        this.workspaceAiPromptService = workspaceAiPromptService;
    }

    @Override
    public AiGatewayChatCompletionResponse playgroundChatCompletion(
        long workspaceId, AiGatewayChatCompletionRequest request, @Nullable Long promptId,
        @Nullable String promptVariables) {

        requireWorkspaceEditor(workspaceId);

        AiGatewayChatCompletionRequest scopedRequest = withWorkspaceTag(request, workspaceId);

        if (promptId != null) {
            scopedRequest = prependManagedPrompt(scopedRequest, workspaceId, promptId, promptVariables);
        }

        return aiGatewayFacade.chatCompletion(scopedRequest, null);
    }

    @Override
    public Flux<AiGatewayChatCompletionResponse> playgroundChatCompletionStream(
        long workspaceId, Supplier<AiGatewayChatCompletionRequest> requestSupplier,
        @Nullable AtomicLong traceIdHolder) {

        requireWorkspaceEditor(workspaceId);

        return Flux.defer(() -> aiGatewayFacade.chatCompletionStream(
            withWorkspaceTag(requestSupplier.get(), workspaceId), null, null, traceIdHolder));
    }

    private void requireWorkspaceEditor(long workspaceId) {
        if (permissionService == null) {
            throw new AccessDeniedException(DENIED_MESSAGE);
        }

        if (!permissionService.hasWorkspaceRole(workspaceId, MINIMUM_EDITOR_ROLE)) {
            log.warn(
                "Playground authorization rejected: workspaceId={} requiredRole={}", workspaceId,
                MINIMUM_EDITOR_ROLE);

            throw new AccessDeniedException(DENIED_MESSAGE);
        }
    }

    private AiGatewayChatCompletionRequest prependManagedPrompt(
        AiGatewayChatCompletionRequest request, long workspaceId, long promptId, @Nullable String promptVariables) {

        Long promptWorkspaceId = workspaceAiPromptService.getWorkspaceId(promptId);

        if (!Objects.equals(promptWorkspaceId, workspaceId)) {
            log.warn(
                "Playground prompt rejected: promptId={} promptWorkspaceId={} workspaceId={}", promptId,
                promptWorkspaceId, workspaceId);

            throw new IllegalArgumentException("Prompt not found: " + promptId);
        }

        Optional<AiPromptVersion> activeVersionOptional = aiPromptVersionService.getActiveVersion(
            promptId, AiPromptHeaders.DEFAULT_ENVIRONMENT);

        if (activeVersionOptional.isEmpty()) {
            return request;
        }

        AiPromptVersion activeVersion = activeVersionOptional.get();

        String rendered = renderManagedPrompt(activeVersion.getContent(), promptVariables);

        List<AiGatewayChatMessage> messages = new ArrayList<>(request.messages());

        messages.addFirst(new AiGatewayChatMessage(AiGatewayChatRole.SYSTEM, rendered, null, null, null));

        return new AiGatewayChatCompletionRequest(
            request.model(), messages, request.temperature(), request.maxTokens(), request.topP(), request.stream(),
            request.routingPolicy(), request.cache(), request.toolChoice(), request.tools(), request.tags());
    }

    @SuppressWarnings("unchecked")
    private static String renderManagedPrompt(String template, @Nullable String variablesJson) {
        if (template == null || template.isEmpty() || variablesJson == null || variablesJson.isBlank()) {
            return template;
        }

        Map<String, Object> variables;

        try {
            variables = JsonUtils.read(variablesJson, Map.class);
        } catch (RuntimeException parseFailure) {
            throw new IllegalArgumentException(
                "Malformed prompt variables JSON, fix the variables payload or clear it to run without substitution",
                parseFailure);
        }

        String rendered = template;

        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            String placeholder = "\\{\\{\\s*" + Pattern.quote(entry.getKey()) + "\\s*}}";
            Object value = entry.getValue();

            String replacement = Matcher.quoteReplacement(value != null ? value.toString() : "");

            rendered = rendered.replaceAll(placeholder, replacement);
        }

        return rendered;
    }

    private static AiGatewayChatCompletionRequest withWorkspaceTag(
        AiGatewayChatCompletionRequest request, long workspaceId) {

        return new AiGatewayChatCompletionRequest(
            request.model(), request.messages(), request.temperature(), request.maxTokens(), request.topP(),
            request.stream(), request.routingPolicy(), request.cache(), request.toolChoice(), request.tools(),
            Map.of(WORKSPACE_ID_TAG, String.valueOf(workspaceId)));
    }
}
