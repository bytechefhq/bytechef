/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.agent;

import com.agui.core.state.State;
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

    /**
     * AG-UI state key for the active environment id (client-supplied). Used to resolve the platform AI provider catalog
     * API key for the chosen provider.
     */
    static final String ENVIRONMENT_ID_KEY = "environmentId";

    private final CatalogChatClientResolver catalogChatClientResolver;

    @SuppressFBWarnings("EI")
    public CopilotChatClientResolver(CatalogChatClientResolver catalogChatClientResolver) {
        this.catalogChatClientResolver = catalogChatClientResolver;
    }

    @Override
    public @Nullable ChatClient resolve(State state) {
        if (state == null) {
            return null;
        }

        String llmProvider = StringUtils.asString(state.get(USER_SELECTED_LLM_PROVIDER_KEY));
        String llmModel = StringUtils.asString(state.get(USER_SELECTED_LLM_MODEL_KEY));

        if (llmProvider == null || llmModel == null) {
            if ((llmProvider == null) != (llmModel == null)) {
                log.warn(
                    "Copilot user-selected LLM half-set (provider={}, model={}); falling back to workspace default",
                    llmProvider, llmModel);
            }

            return null;
        }

        Long environmentId = NumberUtils.asLong(state.get(ENVIRONMENT_ID_KEY));

        if (environmentId != null) {
            return catalogChatClientResolver.resolve(environmentId.intValue(), llmProvider, llmModel);
        }

        return null;
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
