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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.ai.llm.Provider;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class AiModelConfigurationTest {

    @Test
    void resolvesConfiguredProviderFactory() {
        ApplicationProperties applicationProperties = new ApplicationProperties();

        applicationProperties.getAi()
            .getProvider()
            .getOpenAi()
            .setApiKey("test-api-key");

        AiModelConfiguration aiModelConfiguration = new AiModelConfiguration(applicationProperties);

        assertThat(aiModelConfiguration.resolveProvider()).isEqualTo(Provider.OPEN_AI);
    }

    @Test
    void returnsNullWhenNoProviderConfigured() {
        ApplicationProperties applicationProperties = new ApplicationProperties();

        AiModelConfiguration aiModelConfiguration = new AiModelConfiguration(applicationProperties);

        assertThat(aiModelConfiguration.resolveProvider()).isNull();
    }

    @Test
    void prefersExplicitCopilotProviderOverride() {
        ApplicationProperties applicationProperties = new ApplicationProperties();

        applicationProperties.getAi()
            .getProvider()
            .getOpenAi()
            .setApiKey("test-api-key");
        applicationProperties.getAi()
            .getCopilot()
            .setProvider(Provider.ANTHROPIC.getKey());

        AiModelConfiguration aiModelConfiguration = new AiModelConfiguration(applicationProperties);

        assertThat(aiModelConfiguration.resolveProvider()).isEqualTo(Provider.ANTHROPIC);
    }

    @Test
    void acceptsShortProviderNameForCopilotOverride() {
        ApplicationProperties applicationProperties = new ApplicationProperties();

        applicationProperties.getAi()
            .getProvider()
            .getOpenAi()
            .setApiKey("test-api-key");
        applicationProperties.getAi()
            .getCopilot()
            .setProvider("anthropic");

        AiModelConfiguration aiModelConfiguration = new AiModelConfiguration(applicationProperties);

        assertThat(aiModelConfiguration.resolveProvider()).isEqualTo(Provider.ANTHROPIC);
    }

    @Test
    void fallsBackToAutoDetectionForUnknownCopilotOverride() {
        ApplicationProperties applicationProperties = new ApplicationProperties();

        applicationProperties.getAi()
            .getProvider()
            .getOpenAi()
            .setApiKey("test-api-key");
        applicationProperties.getAi()
            .getCopilot()
            .setProvider("does-not-exist");

        AiModelConfiguration aiModelConfiguration = new AiModelConfiguration(applicationProperties);

        assertThat(aiModelConfiguration.resolveProvider()).isEqualTo(Provider.OPEN_AI);
    }

    @Test
    void doesNotAutoSelectKeylessOllamaWithoutExplicitConfiguration() {
        ApplicationProperties applicationProperties = new ApplicationProperties();

        AiModelConfiguration aiModelConfiguration = new AiModelConfiguration(applicationProperties);

        assertThat(aiModelConfiguration.resolveProvider()).isNotEqualTo(Provider.OLLAMA);
    }

    @Test
    void selectsOllamaWhenUrlExplicitlyConfigured() {
        ApplicationProperties applicationProperties = new ApplicationProperties();

        applicationProperties.getAi()
            .getProvider()
            .getOllama()
            .setUrl("http://localhost:11434");

        AiModelConfiguration aiModelConfiguration = new AiModelConfiguration(applicationProperties);

        assertThat(aiModelConfiguration.resolveProvider()).isEqualTo(Provider.OLLAMA);
    }

    @Test
    void doesNotSelectAzureOpenAiWithoutEndpoint() {
        ApplicationProperties applicationProperties = new ApplicationProperties();

        applicationProperties.getAi()
            .getProvider()
            .getAzureOpenAi()
            .setApiKey("test-api-key");

        AiModelConfiguration aiModelConfiguration = new AiModelConfiguration(applicationProperties);

        assertThat(aiModelConfiguration.resolveProvider()).isNotEqualTo(Provider.AZURE_OPEN_AI);
    }

    @Test
    void doesNotSelectAzureOpenAiWithOnlyEndpointAndNoApiKey() {
        ApplicationProperties applicationProperties = new ApplicationProperties();

        applicationProperties.getAi()
            .getProvider()
            .getAzureOpenAi()
            .setEndpoint("https://test.openai.azure.com");

        AiModelConfiguration aiModelConfiguration = new AiModelConfiguration(applicationProperties);

        assertThat(aiModelConfiguration.resolveProvider()).isNotEqualTo(Provider.AZURE_OPEN_AI);
    }

    @Test
    void selectsAzureOpenAiWhenBothApiKeyAndEndpointConfigured() {
        ApplicationProperties applicationProperties = new ApplicationProperties();

        applicationProperties.getAi()
            .getProvider()
            .getAzureOpenAi()
            .setApiKey("test-api-key");
        applicationProperties.getAi()
            .getProvider()
            .getAzureOpenAi()
            .setEndpoint("https://test.openai.azure.com");

        AiModelConfiguration aiModelConfiguration = new AiModelConfiguration(applicationProperties);

        assertThat(aiModelConfiguration.resolveProvider()).isEqualTo(Provider.AZURE_OPEN_AI);
    }

    @Test
    void resolveChatModelReturnsNullWhenNoProviderConfigured() {
        ApplicationProperties applicationProperties = new ApplicationProperties();

        AiModelConfiguration aiModelConfiguration = new AiModelConfiguration(applicationProperties);

        assertThat(aiModelConfiguration.resolveChatModel()).isNull();
    }

    @Test
    void resolveChatModelThrowsWhenEnabledProviderHasNoModelConfigured() {
        ApplicationProperties applicationProperties = new ApplicationProperties();

        applicationProperties.getAi()
            .getProvider()
            .getOpenAi()
            .setApiKey("test-api-key");

        AiModelConfiguration aiModelConfiguration = new AiModelConfiguration(applicationProperties);

        assertThatThrownBy(aiModelConfiguration::resolveChatModel)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining(Provider.OPEN_AI.getKey());
    }

    @Test
    void resolveChatModelBuildsChatModelUsingProviderOwnChatModelOption() {
        ApplicationProperties applicationProperties = new ApplicationProperties();

        applicationProperties.getAi()
            .getProvider()
            .getAnthropic()
            .setApiKey("test-api-key");
        applicationProperties.getAi()
            .getProvider()
            .getChat()
            .getAnthropic()
            .getOptions()
            .setModel("claude-sonnet-4-6");

        AiModelConfiguration aiModelConfiguration = new AiModelConfiguration(applicationProperties);

        ChatModel chatModel = aiModelConfiguration.resolveChatModel();

        assertThat(chatModel).isNotNull();
    }

    @Test
    void resolveChatModelEmbeddingModelProviderReturnsNullWhenNoneConfigured() {
        ApplicationProperties applicationProperties = new ApplicationProperties();

        AiModelConfiguration aiModelConfiguration = new AiModelConfiguration(applicationProperties);

        assertThat(aiModelConfiguration.resolveEmbeddingProvider()).isNull();
    }

    @Test
    void resolveChatModelEmbeddingModelProviderSelectsOpenAiWhenApiKeyConfigured() {
        ApplicationProperties applicationProperties = new ApplicationProperties();

        applicationProperties.getAi()
            .getProvider()
            .getOpenAi()
            .setApiKey("test-api-key");

        AiModelConfiguration aiModelConfiguration = new AiModelConfiguration(applicationProperties);

        assertThat(aiModelConfiguration.resolveEmbeddingProvider()).isEqualTo(Provider.OPEN_AI);
    }

    @Test
    void resolveChatModelEmbeddingProviderSelectsOllamaWhenEmbeddingModelModelConfigured() {
        ApplicationProperties applicationProperties = new ApplicationProperties();

        applicationProperties.getAi()
            .getProvider()
            .getEmbedding()
            .getOllama()
            .getOptions()
            .setModel("qwen3-embedding:8b");

        AiModelConfiguration aiModelConfiguration = new AiModelConfiguration(applicationProperties);

        assertThat(aiModelConfiguration.resolveEmbeddingProvider()).isEqualTo(Provider.OLLAMA);
    }

    @Test
    void resolveChatModelEmbeddingModelProviderDoesNotSelectOllamaFromGenericUrlAlone() {
        ApplicationProperties applicationProperties = new ApplicationProperties();

        applicationProperties.getAi()
            .getProvider()
            .getOllama()
            .setUrl("http://localhost:11434");

        AiModelConfiguration aiModelConfiguration = new AiModelConfiguration(applicationProperties);

        assertThat(aiModelConfiguration.resolveEmbeddingProvider()).isNull();
    }

    @Test
    void resolveChatModelEmbeddingModelProviderPrefersOpenAiOverOllamaWhenBothConfigured() {
        ApplicationProperties applicationProperties = new ApplicationProperties();

        applicationProperties.getAi()
            .getProvider()
            .getOpenAi()
            .setApiKey("test-api-key");
        applicationProperties.getAi()
            .getProvider()
            .getEmbedding()
            .getOllama()
            .getOptions()
            .setModel("qwen3-embedding:8b");

        AiModelConfiguration aiModelConfiguration = new AiModelConfiguration(applicationProperties);

        assertThat(aiModelConfiguration.resolveEmbeddingProvider()).isEqualTo(Provider.OPEN_AI);
    }

    @Test
    void resolveChatModelEmbeddingModelReturnsNullWhenNoProviderConfigured() {
        ApplicationProperties applicationProperties = new ApplicationProperties();

        AiModelConfiguration aiModelConfiguration = new AiModelConfiguration(applicationProperties);

        assertThat(aiModelConfiguration.resolveEmbeddingModel()).isNull();
    }

    @Test
    void resolveChatModelEmbeddingModelThrowsWhenEnabledProviderHasNoModelConfigured() {
        ApplicationProperties applicationProperties = new ApplicationProperties();

        applicationProperties.getAi()
            .getProvider()
            .getOpenAi()
            .setApiKey("test-api-key");

        AiModelConfiguration aiModelConfiguration = new AiModelConfiguration(applicationProperties);

        assertThatThrownBy(aiModelConfiguration::resolveEmbeddingModel)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining(Provider.OPEN_AI.getKey());
    }

    @Test
    void resolveChatModelEmbeddingBuildsEmbeddingModelModelForOpenAi() {
        ApplicationProperties applicationProperties = new ApplicationProperties();

        applicationProperties.getAi()
            .getProvider()
            .getOpenAi()
            .setApiKey("test-api-key");
        applicationProperties.getAi()
            .getProvider()
            .getEmbedding()
            .getOpenAi()
            .getOptions()
            .setModel("text-embedding-ada-002");

        AiModelConfiguration aiModelConfiguration = new AiModelConfiguration(applicationProperties);

        EmbeddingModel embeddingModel = aiModelConfiguration.resolveEmbeddingModel();

        assertThat(embeddingModel).isNotNull();
    }

    @Test
    void resolveChatModelEmbeddingBuildsEmbeddingModelModelForOllama() {
        ApplicationProperties applicationProperties = new ApplicationProperties();

        applicationProperties.getAi()
            .getProvider()
            .getEmbedding()
            .getOllama()
            .getOptions()
            .setModel("qwen3-embedding:8b");

        AiModelConfiguration aiModelConfiguration = new AiModelConfiguration(applicationProperties);

        EmbeddingModel embeddingModel = aiModelConfiguration.resolveEmbeddingModel();

        assertThat(embeddingModel).isNotNull();
    }

    @Test
    void resolveChatModelEmbeddingProviderSelectsMistralWhenEmbeddingModelModelAndApiKeyConfigured() {
        ApplicationProperties applicationProperties = new ApplicationProperties();

        applicationProperties.getAi()
            .getProvider()
            .getMistral()
            .setApiKey("test-api-key");
        applicationProperties.getAi()
            .getProvider()
            .getEmbedding()
            .getMistral()
            .getOptions()
            .setModel("mistral-embed");

        AiModelConfiguration aiModelConfiguration = new AiModelConfiguration(applicationProperties);

        assertThat(aiModelConfiguration.resolveEmbeddingProvider()).isEqualTo(Provider.MISTRAL);
    }

    @Test
    void resolveChatModelEmbeddingModelProviderDoesNotSelectMistralFromApiKeyAlone() {
        ApplicationProperties applicationProperties = new ApplicationProperties();

        applicationProperties.getAi()
            .getProvider()
            .getMistral()
            .setApiKey("test-api-key");

        AiModelConfiguration aiModelConfiguration = new AiModelConfiguration(applicationProperties);

        assertThat(aiModelConfiguration.resolveEmbeddingProvider()).isNull();
    }

    @Test
    void resolveChatModelEmbeddingBuildsEmbeddingModelModelForMistral() {
        ApplicationProperties applicationProperties = new ApplicationProperties();

        applicationProperties.getAi()
            .getProvider()
            .getMistral()
            .setApiKey("test-api-key");
        applicationProperties.getAi()
            .getProvider()
            .getEmbedding()
            .getMistral()
            .getOptions()
            .setModel("mistral-embed");

        AiModelConfiguration aiModelConfiguration = new AiModelConfiguration(applicationProperties);

        EmbeddingModel embeddingModel = aiModelConfiguration.resolveEmbeddingModel();

        assertThat(embeddingModel).isNotNull();
    }
}
