/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.agent.catalog;

import com.bytechef.ee.platform.configuration.dto.AiDefaultModelDTO;
import com.bytechef.ee.platform.configuration.facade.AiProviderFacade;
import com.bytechef.platform.ai.llm.Provider;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
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
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class CatalogChatClientResolverImpl implements CatalogChatClientResolver {

    private final CatalogChatModelFactory catalogChatModelFactory;
    private final AiProviderFacade aiProviderFacade;

    @SuppressFBWarnings("EI")
    public CatalogChatClientResolverImpl(
        CatalogChatModelFactory catalogChatModelFactory, AiProviderFacade aiProviderFacade) {

        this.catalogChatModelFactory = catalogChatModelFactory;
        this.aiProviderFacade = aiProviderFacade;
    }

    @Override
    public @Nullable ChatClient resolve(String providerKey, String model, int environment) {
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

        String apiKey = aiProviderFacade.getApiKey(provider.getKey(), environment);

        if ((apiKey == null || apiKey.isBlank()) && provider.requiresApiKey()) {
            return null;
        }

        String url = aiProviderFacade.getUrl(provider.getKey(), environment);

        ChatModel chatModel = catalogChatModelFactory.createChatModel(provider, model, apiKey, url);

        if (chatModel == null) {
            return null;
        }

        return ChatClient.builder(chatModel)
            .defaultOptions(
                ChatOptions.builder()
                    .model(model))
            .build();
    }

    @Override
    public @Nullable ChatClient resolveDefault(int environment) {
        if (environment < 0 || environment >= Environment.values().length) {
            return null;
        }

        AiDefaultModelDTO defaultModel = aiProviderFacade.getAiDefaultChatModel(environment);

        if (defaultModel == null) {
            return null;
        }

        return resolve(defaultModel.provider(), defaultModel.model(), environment);
    }
}
