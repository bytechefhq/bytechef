/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.agent;

import com.agui.core.state.State;
import com.bytechef.ai.copilot.agent.OverrideChatClientResolver;
import com.bytechef.ai.copilot.constant.CopilotConstants;
import com.bytechef.commons.util.NumberUtils;
import com.bytechef.commons.util.StringUtils;
import com.bytechef.ee.platform.ai.agent.catalog.CatalogChatClientResolver;
// Gateway resolver path disabled for now — see the commented block at the bottom of this class.
// import com.bytechef.ee.automation.ai.gateway.domain.WorkspaceAiGatewayProvider;
// import com.bytechef.ee.automation.ai.gateway.service.WorkspaceAiGatewayProviderService;
// import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayProvider;
// import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayProviderType;
// import com.bytechef.ee.platform.ai.gateway.provider.AiGatewayChatModelFactory;
// import com.bytechef.ee.platform.ai.gateway.service.AiGatewayProviderService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Value;
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
@ConditionalOnProperty(prefix = "bytechef.ai.copilot", name = "enabled", havingValue = "true")
public class CopilotChatClientResolver implements OverrideChatClientResolver {

    private static final Logger log = LoggerFactory.getLogger(CopilotChatClientResolver.class);

    private final CatalogChatClientResolver catalogChatClientResolver;
    private final String defaultProvider;

    @SuppressFBWarnings("EI")
    public CopilotChatClientResolver(
        CatalogChatClientResolver catalogChatClientResolver,
        @Value("${bytechef.ai.copilot.provider:}") String defaultProvider) {

        this.catalogChatClientResolver = catalogChatClientResolver;
        this.defaultProvider = defaultProvider;
    }

    @Override
    public @Nullable ChatClient resolve(State state) {
        if (state == null) {
            return null;
        }

        Long environmentId = NumberUtils.asLong(state.get(CopilotConstants.STATE_ENVIRONMENT_ID));

        if (environmentId == null) {
            return null;
        }

        String llmProvider = StringUtils.asString(state.get(CopilotConstants.STATE_USER_SELECTED_LLM_PROVIDER));
        String llmModel = StringUtils.asString(state.get(CopilotConstants.STATE_USER_SELECTED_LLM_MODEL));

        if (llmProvider != null && llmModel != null) {
            return catalogChatClientResolver.resolve(llmProvider, llmModel, environmentId.intValue());
        }

        if ((llmProvider == null) != (llmModel == null)) {
            log.warn(
                "Copilot user-selected LLM half-set (provider={}, model={}); using the environment default",
                llmProvider, llmModel);
        }

        if (defaultProvider != null && !defaultProvider.isBlank()) {
            ChatClient preferred =
                catalogChatClientResolver.resolvePreferred(defaultProvider, environmentId.intValue());

            if (preferred != null) {
                return preferred;
            }

            log.warn(
                "Copilot default provider '{}' (bytechef.ai.copilot.provider) is not usable in environment {} "
                    + "(disabled or no configured chat model); falling back to the first enabled chat provider",
                defaultProvider, environmentId);
        }

        return catalogChatClientResolver.resolveDefault(environmentId.intValue());
    }

    @Override
    public @Nullable ChatModel resolveDefaultChatModel(int environmentId) {
        return catalogChatClientResolver.resolveDefaultChatModel(defaultProvider, environmentId);
    }

    // ---------------------------------------------------------------------------------------------------------------
    // AI Gateway fallback path — disabled for now. Restore by re-adding the gateway imports/fields/constructor params
    // and inlining the block below after the catalog attempt in resolve(...).
    // ---------------------------------------------------------------------------------------------------------------
    //
    // private final WorkspaceAiGatewayProviderService workspaceAiGatewayProviderService;
    // private final AiGatewayProviderService aiGatewayProviderService;
    // private final AiGatewayChatModelFactory aiGatewayChatModelFactory;
    //
    // Long workspaceId = NumberUtils.asLong(state.get(WORKSPACE_ID_KEY));
    //
    // if (workspaceId == null) {
    // log.warn(
    // "Copilot user-selected LLM override skipped: state has no parseable workspaceId. Falling back to "
    // + "workspace default for this turn.");
    //
    // return null;
    // }
    //
    // AiGatewayProvider provider = resolveProvider(workspaceId, llmProvider);
    //
    // if (provider == null) {
    // log.warn(
    // "Copilot user-selected LLM override skipped: workspace {} has no enabled provider matching '{}'. "
    // + "Falling back to workspace default for this turn.",
    // workspaceId, llmProvider);
    //
    // return null;
    // }
    //
    // ChatModel chatModel = aiGatewayChatModelFactory.getChatModel(provider);
    //
    // return ChatClient.builder(chatModel)
    // .defaultOptions(
    // ChatOptions.builder()
    // .model(llmModel))
    // .build();
    //
    // private @Nullable AiGatewayProvider resolveProvider(long workspaceId, String llmProvider) {
    // for (WorkspaceAiGatewayProvider workspaceProvider : workspaceAiGatewayProviderService
    // .getWorkspaceProviders(workspaceId)) {
    //
    // AiGatewayProvider aiGatewayProvider = aiGatewayProviderService.getProvider(
    // workspaceProvider.getProviderId());
    //
    // if (aiGatewayProvider == null || !aiGatewayProvider.isEnabled()) {
    // continue;
    // }
    //
    // AiGatewayProviderType type = aiGatewayProvider.getType();
    //
    // String name = type.name();
    //
    // if (name.equalsIgnoreCase(llmProvider)) {
    // return aiGatewayProvider;
    // }
    // }
    //
    // return null;
    // }
}
