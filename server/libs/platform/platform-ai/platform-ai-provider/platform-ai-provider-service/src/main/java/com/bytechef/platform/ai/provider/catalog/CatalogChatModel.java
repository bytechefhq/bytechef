/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.platform.ai.provider.catalog;

import com.bytechef.platform.ai.llm.NoChatProviderActivatedException;
import com.bytechef.platform.ai.llm.Provider;
import com.bytechef.platform.ai.provider.dto.AiDefaultModelWithApiKeyDTO;
import com.bytechef.platform.ai.provider.facade.AiProviderFacade;
import com.bytechef.platform.configuration.context.EnvironmentContext;
import com.bytechef.platform.configuration.domain.Environment;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Duration;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
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
 * @author Ivica Cardic
 */
@Component
@Primary
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
    public ChatOptions getOptions() {
        return resolveDelegate().getOptions();
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
            throw new NoChatProviderActivatedException(
                "No chat provider is activated for environment " + environment + ". Activate the Anthropic or OpenAI " +
                    "provider in the AI Providers UI (or set bytechef.ai.provider.anthropic.api-key / " +
                    "bytechef.ai.provider.openai.api-key) so the copilot can generate.");
        }

        Provider provider = defaultModel.provider();
        String model = defaultModel.model();
        String apiKey = defaultModel.apiKey();
        String url = defaultModel.url();

        String cacheKey = ordinal + ":" + provider.getKey() + ":" + model + ":" + apiKey + ":" + url;

        return delegateCache.get(cacheKey,
            key -> catalogChatModelFactory.createChatModel(provider, model, apiKey, url));
    }
}
