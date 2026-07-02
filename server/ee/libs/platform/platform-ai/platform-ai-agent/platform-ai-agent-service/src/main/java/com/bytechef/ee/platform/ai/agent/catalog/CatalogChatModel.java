/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.agent.catalog;

import com.bytechef.ee.platform.configuration.dto.AiDefaultModelWithApiKeyDTO;
import com.bytechef.ee.platform.configuration.facade.AiProviderFacade;
import com.bytechef.platform.ai.llm.Provider;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.context.EnvironmentContext;
import com.bytechef.platform.configuration.domain.Environment;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Duration;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * Always-present {@link ChatModel} that resolves the underlying provider model at call time from the UI-activated chat
 * provider for the current {@link EnvironmentContext} environment. Lets the application boot without a
 * statically-configured chat provider; the copilot (and any other consumer injecting a bare {@link ChatModel}) starts
 * working as soon as a provider is activated in the AI Providers UI.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@Primary
@ConditionalOnEEVersion
public class CatalogChatModel implements ChatModel {

    private final AiProviderFacade aiProviderFacade;
    private final CatalogChatModelFactory catalogChatModelFactory;
    private final Cache<String, ChatModel> delegateCache = Caffeine.newBuilder()
        .maximumSize(100)
        .expireAfterWrite(Duration.ofHours(1))
        .build();

    @SuppressFBWarnings("EI")
    public CatalogChatModel(AiProviderFacade aiProviderFacade, CatalogChatModelFactory catalogChatModelFactory) {
        this.aiProviderFacade = aiProviderFacade;
        this.catalogChatModelFactory = catalogChatModelFactory;
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        return resolveDelegate().call(prompt);
    }

    @Override
    public Flux<ChatResponse> stream(Prompt prompt) {
        return resolveDelegate().stream(prompt);
    }

    private ChatModel resolveDelegate() {
        Environment environment = EnvironmentContext.getCurrentEnvironment();

        int ordinal = environment.ordinal();

        AiDefaultModelWithApiKeyDTO defaultModel = aiProviderFacade.getAiDefaultChatModelApiKey(ordinal);

        if (defaultModel == null) {
            throw new IllegalStateException(
                "No chat provider is activated for environment " + environment + ". Activate the Anthropic or OpenAI " +
                    "provider in the AI Providers UI (or set bytechef.ai.provider.anthropic.api-key / " +
                    "bytechef.ai.provider.openai.api-key) so the copilot can generate.");
        }

        Provider provider = defaultModel.provider();
        String model = defaultModel.model();
        String apiKey = defaultModel.apiKey();

        String cacheKey = ordinal + ":" + provider.getKey() + ":" + model + ":" + apiKey;

        return delegateCache.get(cacheKey, key -> catalogChatModelFactory.createChatModel(provider, model, apiKey));
    }
}
