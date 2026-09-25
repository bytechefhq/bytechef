/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.agent;

import com.bytechef.ai.copilot.agent.ChatModelUnavailableException;
import com.bytechef.ai.copilot.agent.SelectedChatModelUnavailableException;
import com.bytechef.ai.copilot.tool.catalog.SubAgentChatModelResolver;
import com.bytechef.ai.copilot.tool.catalog.SubAgentModelRequest;
import com.bytechef.ee.platform.ai.agent.catalog.CatalogChatClientResolver;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @version ee
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class CatalogSubAgentChatModelResolver implements SubAgentChatModelResolver {

    private static final Logger log = LoggerFactory.getLogger(CatalogSubAgentChatModelResolver.class);

    private final CatalogChatClientResolver catalogChatClientResolver;
    private final String defaultProvider;

    @SuppressFBWarnings("EI")
    public CatalogSubAgentChatModelResolver(
        CatalogChatClientResolver catalogChatClientResolver,
        @Value("${bytechef.ai.copilot.provider:}") String defaultProvider) {

        this.catalogChatClientResolver = catalogChatClientResolver;
        this.defaultProvider = defaultProvider;
    }

    @Override
    public ChatModel resolve(SubAgentModelRequest request) {
        String providerKey = request.llmProvider();
        String model = request.llmModel();
        Long environmentId = request.environmentId();

        if (environmentId == null) {
            throw new ChatModelUnavailableException(
                "The sub-agent call carries no environment, so no AI provider can be resolved for it.");
        }

        if (providerKey == null && model == null) {
            return resolveEnvironmentDefault(environmentId);
        }

        if (providerKey == null || model == null) {
            log.warn(
                "Subagent user-selected LLM half-set (provider={}, model={}) in environment {}", providerKey, model,
                environmentId);

            throw new ChatModelUnavailableException(
                "The model selection is incomplete (provider=%s, model=%s). Pick a model again.".formatted(
                    providerKey, model));
        }

        ChatModel chatModel;

        try {
            chatModel = catalogChatClientResolver.resolveChatModel(providerKey, model, Math.toIntExact(environmentId));
        } catch (RuntimeException exception) {
            throw new SelectedChatModelUnavailableException(providerKey, model, environmentId, exception);
        }

        if (chatModel == null) {
            log.warn(
                "Subagent user-selected LLM (provider={}, model={}) could not be resolved in environment {}",
                providerKey, model, environmentId);

            throw new SelectedChatModelUnavailableException(providerKey, model, environmentId);
        }

        return chatModel;
    }

    private ChatModel resolveEnvironmentDefault(long environmentId) {
        ChatModel chatModel;

        try {
            chatModel = catalogChatClientResolver.resolveDefaultChatModel(
                defaultProvider, Math.toIntExact(environmentId));
        } catch (RuntimeException exception) {
            throw ChatModelUnavailableException.noProviderEnabled(environmentId, exception);
        }

        if (chatModel == null) {
            throw ChatModelUnavailableException.noProviderEnabled(environmentId);
        }

        return chatModel;
    }
}
