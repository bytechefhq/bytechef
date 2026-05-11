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

import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClientAsync;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.config.ApplicationProperties.Ai.Provider.Anthropic;
import com.bytechef.config.ApplicationProperties.Ai.Provider.OpenAi;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.client.okhttp.OpenAIOkHttpClientAsync;
import io.micrometer.observation.ObservationRegistry;
import java.time.Duration;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.chat.observation.ChatModelObservationConvention;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.model.tool.DefaultToolExecutionEligibilityPredicate;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionEligibilityPredicate;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * @author Ivica Cardic
 */
@Configuration
class AiModelConfiguration {

    private final ApplicationProperties.Ai ai;
    private final String anthropicApiKey;
    private final String openAiApiKey;

    AiModelConfiguration(ApplicationProperties applicationProperties) {
        this.ai = applicationProperties.getAi();

        ApplicationProperties.Ai.Provider provider = ai.getProvider();

        Anthropic anthropic = provider.getAnthropic();

        this.anthropicApiKey = anthropic.getApiKey();

        OpenAi openAi = provider.getOpenAi();

        this.openAiApiKey = openAi.getApiKey();
    }

    @Bean
    @Primary
    @ConditionalOnProperty(prefix = "bytechef.ai.copilot", name = "provider", havingValue = "anthropic")
    AnthropicChatModel anthropicChatModel(
        ObjectProvider<ToolExecutionEligibilityPredicate> anthropicToolExecutionEligibilityPredicate,
        ObjectProvider<ObservationRegistry> observationRegistryProvider,
        ObjectProvider<ChatModelObservationConvention> observationConvention, ToolCallingManager toolCallingManager) {

        ApplicationProperties.Ai.Provider.Chat.Anthropic.Options anthropicChatOptions = ai.getProvider()
            .getChat()
            .getAnthropic()
            .getOptions();

        var chatModel = AnthropicChatModel.builder()
            .anthropicClient(
                AnthropicOkHttpClient.builder()
                    .apiKey(anthropicApiKey)
                    .build())
            .anthropicClientAsync(
                AnthropicOkHttpClientAsync.builder()
                    .apiKey(anthropicApiKey)
                    .build())
            .options(
                AnthropicChatOptions.builder()
                    .model(anthropicChatOptions.getModel())
                    .temperature(anthropicChatOptions.getTemperature())
                    .maxTokens(64000)
                    .build())
            .observationRegistry(observationRegistryProvider.getIfUnique(() -> ObservationRegistry.NOOP))
            .toolCallingManager(toolCallingManager)
            .toolExecutionEligibilityPredicate(anthropicToolExecutionEligibilityPredicate
                .getIfUnique(DefaultToolExecutionEligibilityPredicate::new))
            .build();

        observationConvention.ifAvailable(chatModel::setObservationConvention);

        return chatModel;
    }

    @Bean
    @ConditionalOnProperty(prefix = "bytechef.ai.copilot", name = "provider", havingValue = "openai")
    OpenAiChatModel openAiChatModel(
        ObjectProvider<ObservationRegistry> observationRegistry,
        ObjectProvider<ChatModelObservationConvention> observationConvention,
        ObjectProvider<ToolExecutionEligibilityPredicate> openAiToolExecutionEligibilityPredicate,
        ToolCallingManager toolCallingManager) {

        ApplicationProperties.Ai.Provider.Chat.OpenAi.Options openAiChatOptions = ai.getProvider()
            .getChat()
            .getOpenAi()
            .getOptions();

        var chatModel = OpenAiChatModel.builder()
            .openAiClient(
                OpenAIOkHttpClient.builder()
                    .apiKey(openAiApiKey)
                    .timeout(Duration.ofSeconds(60))
                    .build())
            .openAiClientAsync(
                OpenAIOkHttpClientAsync.builder()
                    .apiKey(openAiApiKey)
                    .timeout(Duration.ofSeconds(60))
                    .build())
            .options(
                OpenAiChatOptions.builder()
                    .model(openAiChatOptions.getModel())
                    .temperature(openAiChatOptions.getTemperature())
                    .reasoningEffort(
                        openAiChatOptions.getReasoningEffect()
                            .name()
                            .toLowerCase())
                    .verbosity(
                        openAiChatOptions.getVerbosity()
                            .name()
                            .toLowerCase())
                    .build())
            .toolCallingManager(toolCallingManager)
            .observationRegistry(observationRegistry.getIfUnique(() -> ObservationRegistry.NOOP))
            .toolExecutionEligibilityPredicate(
                openAiToolExecutionEligibilityPredicate.getIfUnique(DefaultToolExecutionEligibilityPredicate::new))
            .build();

        observationConvention.ifAvailable(chatModel::setObservationConvention);

        return chatModel;
    }

    @Bean
    @ConditionalOnProperty(prefix = "bytechef.ai.provider.openai", name = "api-key")
    OpenAiEmbeddingModel openAiEmbeddingModel(ApplicationProperties applicationProperties) {
        ApplicationProperties.Ai.Provider.Embedding.OpenAi.Options options = applicationProperties.getAi()
            .getProvider()
            .getEmbedding()
            .getOpenAi()
            .getOptions();

        return new OpenAiEmbeddingModel(
            OpenAIOkHttpClient.builder()
                .apiKey(openAiApiKey)
                .timeout(Duration.ofSeconds(60))
                .build(),
            MetadataMode.ALL,
            OpenAiEmbeddingOptions.builder()
                .model(options.getModel())
                .build());
    }
}
