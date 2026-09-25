/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.agent;

import com.agui.core.state.State;
import com.bytechef.ai.copilot.agent.ChatModelUnavailableException;
import com.bytechef.ai.copilot.agent.OverrideChatClientResolver;
import com.bytechef.ai.copilot.agent.SelectedChatModelUnavailableException;
import com.bytechef.ai.copilot.constant.CopilotConstants;
import com.bytechef.commons.util.NumberUtils;
import com.bytechef.commons.util.StringUtils;
import com.bytechef.ee.platform.ai.agent.catalog.CatalogChatClientResolver;
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
 * Resolves the per-request {@link ChatClient} for Copilot conversations from the provider catalog: the user-selected
 * (provider, model) pair supplied by the chat-toolbar picker via AG-UI state keys {@code userSelectedLlmProvider} +
 * {@code userSelectedLlmModel}, otherwise the environment default. Never falls back to the startup model; an
 * unresolvable model fails the run with a {@link ChatModelUnavailableException}.
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

    private static final String NO_ENVIRONMENT_MESSAGE =
        "The Copilot run carries no environment, so no AI provider can be resolved for it.";

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
    public ChatClient resolve(@Nullable State state) {
        if (state == null) {
            throw new ChatModelUnavailableException(NO_ENVIRONMENT_MESSAGE);
        }

        Long environmentId = NumberUtils.asLong(state.get(CopilotConstants.STATE_ENVIRONMENT_ID));

        if (environmentId == null) {
            throw new ChatModelUnavailableException(NO_ENVIRONMENT_MESSAGE);
        }

        String llmProvider = StringUtils.asString(state.get(CopilotConstants.STATE_USER_SELECTED_LLM_PROVIDER));
        String llmModel = StringUtils.asString(state.get(CopilotConstants.STATE_USER_SELECTED_LLM_MODEL));

        if (llmProvider != null && llmModel != null) {
            return resolveSelected(llmProvider, llmModel, environmentId);
        }

        if (llmProvider != null || llmModel != null) {
            log.warn(
                "Copilot user-selected LLM half-set (provider={}, model={}) in environment {}", llmProvider, llmModel,
                environmentId);

            throw new ChatModelUnavailableException(
                "The model selection is incomplete (provider=%s, model=%s). Pick a model again.".formatted(
                    llmProvider, llmModel));
        }

        return resolveEnvironmentDefault(environmentId);
    }

    @Override
    public ChatModel resolveDefaultChatModel(int environmentId) {
        ChatModel chatModel;

        try {
            chatModel = catalogChatClientResolver.resolveDefaultChatModel(defaultProvider, environmentId);
        } catch (RuntimeException exception) {
            throw ChatModelUnavailableException.noProviderEnabled(environmentId, exception);
        }

        if (chatModel == null) {
            throw ChatModelUnavailableException.noProviderEnabled(environmentId);
        }

        return chatModel;
    }

    private ChatClient resolveSelected(String llmProvider, String llmModel, long environmentId) {
        ChatClient selected;

        try {
            selected = catalogChatClientResolver.resolve(llmProvider, llmModel, Math.toIntExact(environmentId));
        } catch (RuntimeException exception) {
            throw new SelectedChatModelUnavailableException(llmProvider, llmModel, environmentId, exception);
        }

        if (selected == null) {
            log.warn(
                "Copilot user-selected LLM (provider={}, model={}) is not usable in environment {}", llmProvider,
                llmModel, environmentId);

            throw new SelectedChatModelUnavailableException(llmProvider, llmModel, environmentId);
        }

        return selected;
    }

    private ChatClient resolveEnvironmentDefault(long environmentId) {
        int environment = Math.toIntExact(environmentId);

        try {
            if (defaultProvider != null && !defaultProvider.isBlank()) {
                ChatClient preferred = catalogChatClientResolver.resolvePreferred(defaultProvider, environment);

                if (preferred != null) {
                    return preferred;
                }

                log.warn(
                    "Copilot default provider '{}' (bytechef.ai.copilot.provider) is not usable in environment {} "
                        + "(disabled or no configured chat model); using the first enabled chat provider",
                    defaultProvider, environmentId);
            }

            ChatClient environmentDefault = catalogChatClientResolver.resolveDefault(environment);

            if (environmentDefault != null) {
                return environmentDefault;
            }
        } catch (RuntimeException exception) {
            throw ChatModelUnavailableException.noProviderEnabled(environmentId, exception);
        }

        throw ChatModelUnavailableException.noProviderEnabled(environmentId);
    }
}
