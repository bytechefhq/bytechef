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

package com.bytechef.ai.model.config;

import static com.bytechef.component.ai.llm.constant.LLMConstants.ENDPOINT;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MODEL;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.URL;
import static com.bytechef.component.definition.Authorization.TOKEN;

import com.bytechef.component.ai.llm.anthropic.action.AnthropicChatAction;
import com.bytechef.component.ai.llm.azure.openai.action.AzureOpenAiChatAction;
import com.bytechef.component.ai.llm.deepseek.action.DeepSeekChatAction;
import com.bytechef.component.ai.llm.gemini.action.GeminiChatAction;
import com.bytechef.component.ai.llm.groq.action.GroqChatAction;
import com.bytechef.component.ai.llm.mistral.action.MistralChatAction;
import com.bytechef.component.ai.llm.mistral.cluster.MistralAiEmbedding;
import com.bytechef.component.ai.llm.nvidia.action.NvidiaChatAction;
import com.bytechef.component.ai.llm.ollama.action.OllamaChatAction;
import com.bytechef.component.ai.llm.ollama.cluster.OllamaEmbedding;
import com.bytechef.component.ai.llm.openai.action.OpenAiChatAction;
import com.bytechef.component.ai.llm.openai.cluster.OpenAiEmbedding;
import com.bytechef.component.ai.llm.perplexity.action.PerplexityChatAction;
import com.bytechef.component.definition.Parameters;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.config.ApplicationProperties.Ai.Provider.OpenAi;
import com.bytechef.platform.ai.llm.Provider;
import com.bytechef.platform.annotation.ConditionalOnCEVersion;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.definition.ai.vectorstore.EmbeddingFunction;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnCEVersion
class AiModelConfiguration {

    private final ApplicationProperties applicationProperties;
    private final String openAiApiKey;

    @SuppressFBWarnings("EI")
    AiModelConfiguration(ApplicationProperties applicationProperties) {
        this.applicationProperties = applicationProperties;

        OpenAi openAi = applicationProperties.getAi()
            .getProvider()
            .getOpenAi();

        this.openAiApiKey = openAi.getApiKey();
    }

    @Bean
    @ConditionalOnMissingBean(EmbeddingModel.class)
    @Conditional(EmbeddingProviderConfiguredCondition.class)
    EmbeddingModel embeddingModel() {
        EmbeddingModel embeddingModel = resolveEmbeddingModel();

        if (embeddingModel == null) {
            throw new IllegalStateException(
                "Embedding AI provider '%s' is configured but does not support embeddings"
                    .formatted(resolveEmbeddingProvider()));
        }

        return embeddingModel;
    }

    @Bean
    @ConditionalOnMissingBean(ChatModel.class)
    @Conditional(CopilotProviderConfiguredCondition.class)
    ChatModel copilotChatModel() {
        ChatModel chatModel = resolveChatModel();

        if (chatModel == null) {
            throw new IllegalStateException(
                "Copilot AI provider '%s' is configured but does not support chat completions"
                    .formatted(resolveProvider()));
        }

        return chatModel;
    }

    @Nullable
    Provider resolveProvider() {
        String explicitProviderKey = applicationProperties.getAi()
            .getCopilot()
            .getProvider();

        if (explicitProviderKey != null && !explicitProviderKey.isBlank()) {
            return Provider.valueOfKey(explicitProviderKey);
        }

        for (Provider provider : Provider.CHAT_PROVIDERS) {
            if (isConfigured(provider)) {
                return provider;
            }
        }

        return null;
    }

    @Nullable
    ChatModel resolveChatModel() {
        Provider provider = resolveProvider();

        if (provider == null) {
            return null;
        }

        com.bytechef.component.ai.llm.ChatModel chatModelFactory = resolveFactory(provider);

        if (chatModelFactory == null) {
            return null;
        }

        String model = getModel(provider);

        if (model == null || model.isBlank()) {
            throw new IllegalStateException(
                ("AI provider '%s' is enabled but no chat model is configured. Set "
                    + "bytechef.ai.provider.chat.%s.options.model.").formatted(provider.getKey(), provider.getKey()));
        }

        Parameters inputParameters = ParametersFactory.create(Map.of(MODEL, model));
        Parameters connectionParameters = ParametersFactory.create(
            createConnectionParameters(getApiKey(provider), getEndpoint(provider)));

        return chatModelFactory.createChatModel(inputParameters, connectionParameters, false);
    }

    @Nullable
    Provider resolveEmbeddingProvider() {
        for (Provider provider : Provider.EMBEDDING_PROVIDERS) {
            if (isEmbeddingConfigured(provider)) {
                return provider;
            }
        }

        return null;
    }

    @Nullable
    EmbeddingModel resolveEmbeddingModel() {
        Provider provider = resolveEmbeddingProvider();

        if (provider == null) {
            return null;
        }

        EmbeddingFunction embeddingFactory = resolveEmbeddingFactory(provider);

        if (embeddingFactory == null) {
            return null;
        }

        String model = getEmbeddingModel(provider);

        if (model == null || model.isBlank()) {
            throw new IllegalStateException(
                ("AI provider '%s' is enabled but no embedding model is configured. Set "
                    + "bytechef.ai.provider.embedding.%s.options.model.").formatted(provider.getKey(),
                        provider.getKey()));
        }

        Parameters inputParameters = ParametersFactory.create(Map.of(MODEL, model));
        Parameters connectionParameters = ParametersFactory.create(
            createConnectionParameters(getApiKey(provider), getEndpoint(provider)));

        return embeddingFactory.apply(inputParameters, connectionParameters);
    }

    private boolean isEmbeddingConfigured(Provider provider) {
        return switch (provider) {
            case MISTRAL -> {
                String model = applicationProperties.getAi()
                    .getProvider()
                    .getEmbedding()
                    .getMistral()
                    .getOptions()
                    .getModel();
                String apiKey = getApiKey(provider);

                yield model != null && !model.isBlank() && apiKey != null && !apiKey.isBlank();
            }
            case OPEN_AI -> openAiApiKey != null && !openAiApiKey.isBlank();
            case OLLAMA -> {
                String model = applicationProperties.getAi()
                    .getProvider()
                    .getEmbedding()
                    .getOllama()
                    .getOptions()
                    .getModel();

                yield model != null && !model.isBlank();
            }
            default -> false;
        };
    }

    private boolean isConfigured(Provider provider) {
        if (provider.requiresApiKey()) {
            String apiKey = getApiKey(provider);

            if (apiKey == null || apiKey.isBlank()) {
                return false;
            }
        } else {
            // Keyless providers (e.g. Ollama) still require an explicit opt-in (api key or self-hosted URL) so an
            // unconfigured environment doesn't silently default to a local server.
            String apiKey = getApiKey(provider);
            String endpoint = getEndpoint(provider);

            return (apiKey != null && !apiKey.isBlank()) || (endpoint != null && !endpoint.isBlank());
        }

        // requiresApiKey() providers that ALSO require an endpoint (e.g. Azure OpenAI) need both present; the
        // api-key check above already failed fast, so only the endpoint remains to be verified here.
        if (provider.requiresEndpoint()) {
            String endpoint = getEndpoint(provider);

            return endpoint != null && !endpoint.isBlank();
        }

        return true;
    }

    private @Nullable String getApiKey(Provider provider) {
        ApplicationProperties.Ai.Provider providerProperties = applicationProperties.getAi()
            .getProvider();

        return switch (provider) {
            case ANTHROPIC -> providerProperties.getAnthropic()
                .getApiKey();
            case AZURE_OPEN_AI -> providerProperties.getAzureOpenAi()
                .getApiKey();
            case DEEPSEEK -> providerProperties.getDeepSeek()
                .getApiKey();
            case GROQ -> providerProperties.getGroq()
                .getApiKey();
            case MISTRAL -> providerProperties.getMistral()
                .getApiKey();
            case NVIDIA -> providerProperties.getNvidia()
                .getApiKey();
            case OLLAMA -> providerProperties.getOllama()
                .getApiKey();
            case OPEN_AI -> providerProperties.getOpenAi()
                .getApiKey();
            case PERPLEXITY -> providerProperties.getPerplexity()
                .getApiKey();
            case VERTEX_GEMINI -> providerProperties.getVertexGemini()
                .getApiKey();
            default -> null;
        };
    }

    private @Nullable String getEndpoint(Provider provider) {
        ApplicationProperties.Ai.Provider providerProperties = applicationProperties.getAi()
            .getProvider();

        return switch (provider) {
            case AZURE_OPEN_AI -> providerProperties.getAzureOpenAi()
                .getEndpoint();
            case OLLAMA -> providerProperties.getOllama()
                .getUrl();
            default -> null;
        };
    }

    private @Nullable String getModel(Provider provider) {
        ApplicationProperties.Ai.Provider.Chat chat = applicationProperties.getAi()
            .getProvider()
            .getChat();

        return switch (provider) {
            case ANTHROPIC -> chat.getAnthropic()
                .getOptions()
                .getModel();
            case AZURE_OPEN_AI -> chat.getAzureOpenAi()
                .getOptions()
                .getModel();
            case DEEPSEEK -> chat.getDeepSeek()
                .getOptions()
                .getModel();
            case GROQ -> chat.getGroq()
                .getOptions()
                .getModel();
            case MISTRAL -> chat.getMistral()
                .getOptions()
                .getModel();
            case NVIDIA -> chat.getNvidia()
                .getOptions()
                .getModel();
            case OLLAMA -> chat.getOllama()
                .getOptions()
                .getModel();
            case OPEN_AI -> chat.getOpenAi()
                .getOptions()
                .getModel();
            case PERPLEXITY -> chat.getPerplexity()
                .getOptions()
                .getModel();
            case VERTEX_GEMINI -> chat.getVertexGemini()
                .getOptions()
                .getModel();
            default -> null;
        };
    }

    private @Nullable String getEmbeddingModel(Provider provider) {
        ApplicationProperties.Ai.Provider.Embedding embedding = applicationProperties.getAi()
            .getProvider()
            .getEmbedding();

        return switch (provider) {
            case MISTRAL -> embedding.getMistral()
                .getOptions()
                .getModel();
            case OLLAMA -> embedding.getOllama()
                .getOptions()
                .getModel();
            case OPEN_AI -> embedding.getOpenAi()
                .getOptions()
                .getModel();
            default -> null;
        };
    }

    private static Map<String, Object> createConnectionParameters(@Nullable String apiKey, @Nullable String url) {
        Map<String, Object> connectionParameters = new HashMap<>();

        if (apiKey != null && !apiKey.isBlank()) {
            connectionParameters.put(TOKEN, apiKey);
        }

        if (url != null && !url.isBlank()) {
            connectionParameters.put(URL, url);
            connectionParameters.put(ENDPOINT, url);
        }

        return connectionParameters;
    }

    private static com.bytechef.component.ai.llm.@Nullable ChatModel resolveFactory(Provider provider) {
        return switch (provider) {
            case ANTHROPIC -> AnthropicChatAction.CHAT_MODEL;
            case AZURE_OPEN_AI -> AzureOpenAiChatAction.CHAT_MODEL;
            case DEEPSEEK -> DeepSeekChatAction.CHAT_MODEL;
            case GROQ -> GroqChatAction.CHAT_MODEL;
            case MISTRAL -> MistralChatAction.CHAT_MODEL;
            case NVIDIA -> NvidiaChatAction.CHAT_MODEL;
            case OLLAMA -> OllamaChatAction.CHAT_MODEL;
            case OPEN_AI -> OpenAiChatAction.CHAT_MODEL;
            case PERPLEXITY -> PerplexityChatAction.CHAT_MODEL;
            case VERTEX_GEMINI -> GeminiChatAction.CHAT_MODEL;
            default -> null;
        };
    }

    private static @Nullable EmbeddingFunction resolveEmbeddingFactory(Provider provider) {
        return switch (provider) {
            case MISTRAL -> MistralAiEmbedding.EMBEDDING_MODEL;
            case OLLAMA -> OllamaEmbedding.EMBEDDING_MODEL;
            case OPEN_AI -> OpenAiEmbedding.EMBEDDING_MODEL;
            default -> null;
        };
    }

    /**
     * Matches iff {@link #resolveProvider()} resolves to a non-{@code null} {@link Provider} for the bound
     * {@link ApplicationProperties}, i.e. some provider's credentials/endpoint are actually present. Keeps the
     * {@code copilotChatModel} {@link Bean @Bean} method from ever being invoked when nothing is configured, so it
     * never has to return {@code null}.
     */
    private static class CopilotProviderConfiguredCondition implements Condition {

        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            ApplicationProperties applicationProperties = Binder.get(context.getEnvironment())
                .bind("bytechef", Bindable.of(ApplicationProperties.class))
                .orElseGet(ApplicationProperties::new);

            return new AiModelConfiguration(applicationProperties).resolveProvider() != null;
        }
    }

    /**
     * Matches iff {@link #resolveEmbeddingProvider()} resolves to a non-{@code null} {@link Provider} for the bound
     * {@link ApplicationProperties}, i.e. some embedding provider's credentials are actually present. Keeps the
     * {@code embeddingModel} {@link Bean @Bean} method from ever being invoked when nothing is configured, so it never
     * has to return {@code null}.
     */
    private static class EmbeddingProviderConfiguredCondition implements Condition {

        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            ApplicationProperties applicationProperties = Binder.get(context.getEnvironment())
                .bind("bytechef", Bindable.of(ApplicationProperties.class))
                .orElseGet(ApplicationProperties::new);

            return new AiModelConfiguration(applicationProperties).resolveEmbeddingProvider() != null;
        }
    }
}
