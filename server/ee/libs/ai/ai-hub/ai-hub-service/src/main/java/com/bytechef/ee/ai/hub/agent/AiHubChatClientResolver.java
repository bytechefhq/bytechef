/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.agent;

import com.agui.core.state.State;
import com.bytechef.ee.ai.hub.config.ConditionalOnAiHub;
import com.bytechef.ee.ai.hub.util.AiHubStateKeys;
import com.bytechef.ee.platform.ai.agent.catalog.CatalogChatClientResolver;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

/**
 * Resolves a per-request override {@link ChatClient} for AI Hub conversations that carry the user's per-conversation
 * model selection ({@link AiHubStateKeys#USER_SELECTED_LLM_PROVIDER_KEY} +
 * {@link AiHubStateKeys#USER_SELECTED_LLM_MODEL_KEY}), looked up in the platform AI provider catalog through
 * {@link CatalogChatClientResolver}.
 *
 * <p>
 * Returns {@code null} (the agent falls back to the default {@code ChatModel}) when the selection is not fully set,
 * when {@link AiHubStateKeys#VERIFIED_WORKSPACE_ID} or {@link AiHubStateKeys#VERIFIED_ENVIRONMENT_ID} is absent, or
 * when the catalog can't resolve the pair. A half-set selection logs a single warning instead of failing the turn.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnAiHub
@ConditionalOnEEVersion
public class AiHubChatClientResolver implements AiHubSpringAIAgent.OverrideChatClientResolver {

    private static final Logger log = LoggerFactory.getLogger(AiHubChatClientResolver.class);

    private final CatalogChatClientResolver catalogChatClientResolver;

    @SuppressFBWarnings("EI")
    public AiHubChatClientResolver(CatalogChatClientResolver catalogChatClientResolver) {
        this.catalogChatClientResolver = catalogChatClientResolver;
    }

    @Override
    public @Nullable ChatClient resolve(@Nullable State state) {
        if (state == null || asLong(state.get(AiHubStateKeys.VERIFIED_WORKSPACE_ID)) == null) {
            return null;
        }

        String llmProvider = asString(state.get(AiHubStateKeys.USER_SELECTED_LLM_PROVIDER_KEY));
        String llmModel = asString(state.get(AiHubStateKeys.USER_SELECTED_LLM_MODEL_KEY));

        if ((llmProvider == null) != (llmModel == null)) {
            log.warn(
                "User-selected LLM half-set (provider={}, model={}); falling back to the workspace default",
                llmProvider, llmModel);
        }

        if (llmProvider == null || llmModel == null) {
            return null;
        }

        Long environment = asLong(state.get(AiHubStateKeys.VERIFIED_ENVIRONMENT_ID));

        if (environment == null) {
            return null;
        }

        return catalogChatClientResolver.resolve(llmProvider, llmModel, environment.intValue());
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
