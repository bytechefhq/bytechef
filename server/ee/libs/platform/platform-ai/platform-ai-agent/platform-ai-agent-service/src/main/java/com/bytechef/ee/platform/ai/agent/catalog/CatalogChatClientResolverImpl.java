/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.agent.catalog;

import com.bytechef.component.ai.llm.Provider;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.domain.Property;
import com.bytechef.platform.configuration.domain.Property.Scope;
import com.bytechef.platform.configuration.service.PropertyService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
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

    private final PropertyService propertyService;
    private final CatalogChatModelFactory catalogChatModelFactory;
    private final ApplicationProperties applicationProperties;

    @SuppressFBWarnings("EI")
    public CatalogChatClientResolverImpl(
        PropertyService propertyService, CatalogChatModelFactory catalogChatModelFactory,
        ApplicationProperties applicationProperties) {

        this.propertyService = propertyService;
        this.catalogChatModelFactory = catalogChatModelFactory;
        this.applicationProperties = applicationProperties;
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

        String apiKey = resolveApiKey(provider, environment);

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

    private @Nullable String resolveApiKey(Provider provider, int environment) {
        Optional<Property> property = propertyService.fetchProperty(
            provider.getKey(), Scope.PLATFORM, null, (long) environment);

        if (property.isPresent() && property.get()
            .isEnabled()) {

            Object apiKey = property.get()
                .get("apiKey");

            if (apiKey != null && !apiKey.toString()
                .isBlank()) {

                return apiKey.toString();
            }
        }

        return configApiKey(provider);
    }

    private @Nullable String configApiKey(Provider provider) {
        ApplicationProperties.Ai.Provider configProvider = applicationProperties.getAi()
            .getProvider();

        return switch (provider) {
            case OPEN_AI -> configProvider.getOpenAi()
                .getApiKey();
            case ANTHROPIC -> configProvider.getAnthropic()
                .getApiKey();
            case MISTRAL -> configProvider.getMistral()
                .getApiKey();
            case VERTEX_GEMINI -> configProvider.getVertexGemini()
                .getApiKey();
            case GROQ -> configProvider.getGroq()
                .getApiKey();
            case PERPLEXITY -> configProvider.getPerplexity()
                .getApiKey();
            case NVIDIA -> configProvider.getNvidia()
                .getApiKey();
            case DEEPSEEK -> configProvider.getDeepSeek()
                .getApiKey();
            default -> null;
        };
    }
}
