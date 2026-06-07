/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.agent;

import com.agui.core.state.State;
import com.bytechef.ee.automation.ai.gateway.domain.WorkspaceAiGatewayProvider;
import com.bytechef.ee.automation.ai.gateway.service.WorkspaceAiGatewayProviderService;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayProvider;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayProviderType;
import com.bytechef.ee.platform.ai.gateway.provider.AiGatewayChatModelFactory;
import com.bytechef.ee.platform.ai.gateway.service.AiGatewayProviderService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Resolves a per-request override {@link ChatClient} for Copilot conversations that carry a user-selected (provider,
 * model) pair, supplied by the chat-toolbar picker via AG-UI state keys {@code userSelectedLlmProvider} +
 * {@code userSelectedLlmModel}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
public class CopilotChatClientResolver implements OverrideChatClientResolver {

    private static final Logger log = LoggerFactory.getLogger(CopilotChatClientResolver.class);

    /**
     * AG-UI state key for the user-selected LLM provider.
     */
    static final String USER_SELECTED_LLM_PROVIDER_KEY = "userSelectedLlmProvider";

    /**
     * AG-UI state key for the user-selected LLM model.
     */
    static final String USER_SELECTED_LLM_MODEL_KEY = "userSelectedLlmModel";

    /**
     * AG-UI state key for the workspace id.
     */
    static final String WORKSPACE_ID_KEY = "workspaceId";

    private final WorkspaceAiGatewayProviderService workspaceAiGatewayProviderService;
    private final AiGatewayProviderService aiGatewayProviderService;
    private final AiGatewayChatModelFactory aiGatewayChatModelFactory;

    @SuppressFBWarnings("EI")
    public CopilotChatClientResolver(
        WorkspaceAiGatewayProviderService workspaceAiGatewayProviderService,
        AiGatewayProviderService aiGatewayProviderService, AiGatewayChatModelFactory aiGatewayChatModelFactory) {

        this.workspaceAiGatewayProviderService = workspaceAiGatewayProviderService;
        this.aiGatewayProviderService = aiGatewayProviderService;
        this.aiGatewayChatModelFactory = aiGatewayChatModelFactory;
    }

    @Override
    public @Nullable ChatClient resolve(State state) {
        if (state == null) {
            return null;
        }

        String llmProvider = asString(state.get(USER_SELECTED_LLM_PROVIDER_KEY));
        String llmModel = asString(state.get(USER_SELECTED_LLM_MODEL_KEY));

        if (llmProvider == null || llmModel == null) {
            if ((llmProvider == null) != (llmModel == null)) {
                log.warn(
                    "Copilot user-selected LLM half-set (provider={}, model={}); falling back to workspace default",
                    llmProvider, llmModel);
            }

            return null;
        }

        Long workspaceId = asLong(state.get(WORKSPACE_ID_KEY));

        if (workspaceId == null) {
            log.warn(
                "Copilot user-selected LLM override skipped: state has no parseable workspaceId. Falling back to "
                    + "workspace default for this turn.");

            return null;
        }

        AiGatewayProvider provider = resolveProvider(workspaceId, llmProvider);

        if (provider == null) {
            log.warn(
                "Copilot user-selected LLM override skipped: workspace {} has no enabled provider matching '{}'. "
                    + "Falling back to workspace default for this turn.",
                workspaceId, llmProvider);

            return null;
        }

        ChatModel chatModel = aiGatewayChatModelFactory.getChatModel(provider);

        return ChatClient.builder(chatModel)
            .defaultOptions(
                ChatOptions.builder()
                    .model(llmModel))
            .build();
    }

    private @Nullable AiGatewayProvider resolveProvider(long workspaceId, String llmProvider) {
        for (WorkspaceAiGatewayProvider workspaceProvider : workspaceAiGatewayProviderService
            .getWorkspaceProviders(workspaceId)) {

            AiGatewayProvider aiGatewayProvider = aiGatewayProviderService.getProvider(
                workspaceProvider.getProviderId());

            if (aiGatewayProvider == null || !aiGatewayProvider.isEnabled()) {
                continue;
            }

            AiGatewayProviderType type = aiGatewayProvider.getType();

            String name = type.name();

            if (name.equalsIgnoreCase(llmProvider)) {
                return aiGatewayProvider;
            }
        }

        return null;
    }

    private static @Nullable String asString(@Nullable Object value) {
        return value == null ? null : value.toString();
    }

    private static @Nullable Long asLong(@Nullable Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Number number) {
            return number.longValue();
        }

        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
