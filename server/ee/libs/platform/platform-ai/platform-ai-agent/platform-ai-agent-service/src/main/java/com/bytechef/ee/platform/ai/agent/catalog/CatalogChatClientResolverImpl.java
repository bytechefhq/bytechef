/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.agent.catalog;

import com.bytechef.component.ai.llm.Provider;
import com.bytechef.platform.configuration.domain.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Arrays;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.stereotype.Component;

/**
 * Default {@link CatalogChatClientResolver}: given a catalog provider key (e.g. {@code "ai.provider.openAi"}) + model
 * name, reads the environment-scoped platform API key and builds a Spring-AI {@link ChatModel} via
 * {@link CatalogChatModelFactory}. Returns {@code null} (caller falls back) when the key is unknown, the provider is
 * disabled, no API key is stored, or the factory can't build it.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class CatalogChatClientResolverImpl implements CatalogChatClientResolver {

    private final CatalogChatModelFactory catalogChatModelFactory;
    private final ProviderApiKeyResolver providerApiKeyResolver;

    @SuppressFBWarnings("EI")
    public CatalogChatClientResolverImpl(
        CatalogChatModelFactory catalogChatModelFactory, ProviderApiKeyResolver providerApiKeyResolver) {

        this.catalogChatModelFactory = catalogChatModelFactory;
        this.providerApiKeyResolver = providerApiKeyResolver;
    }

    @Override
    public @Nullable ChatClient resolve(int environment, String providerKey, String model) {
        if (environment < 0 || environment >= Environment.values().length) {
            return null;
        }

        Provider provider = Arrays.stream(Provider.values())
            .filter(curProvider -> Objects.equals(curProvider.getKey(), providerKey))
            .findFirst()
            .orElse(null);

        if (provider == null) {
            return null;
        }

        String apiKey = providerApiKeyResolver.resolve(provider, environment);

        if (apiKey == null || apiKey.isBlank()) {
            return null;
        }

        ChatModel chatModel = catalogChatModelFactory.createChatModel(provider, model, apiKey);

        if (chatModel == null) {
            return null;
        }

        return ChatClient.builder(chatModel)
            .defaultOptions(
                ChatOptions.builder()
                    .model(model))
            .build();
    }
}
