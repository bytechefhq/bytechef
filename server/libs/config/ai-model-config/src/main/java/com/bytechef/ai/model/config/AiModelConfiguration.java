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

import com.bytechef.config.ApplicationProperties;
import com.bytechef.config.ApplicationProperties.Ai.Provider.OpenAi;
import com.bytechef.platform.annotation.ConditionalOnCEVersion;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import java.time.Duration;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaEmbeddingOptions;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnCEVersion
class AiModelConfiguration {

    private final String openAiApiKey;

    AiModelConfiguration(ApplicationProperties applicationProperties) {
        OpenAi openAi = applicationProperties.getAi()
            .getProvider()
            .getOpenAi();

        this.openAiApiKey = openAi.getApiKey();
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

    @Bean
    @ConditionalOnMissingBean(EmbeddingModel.class)
    @ConditionalOnProperty(prefix = "bytechef.ai.provider.embedding.ollama.options", name = "model")
    OllamaEmbeddingModel ollamaEmbeddingModel(ApplicationProperties applicationProperties) {
        ApplicationProperties.Ai.Provider.Embedding.Ollama.Options options = applicationProperties.getAi()
            .getProvider()
            .getEmbedding()
            .getOllama()
            .getOptions();

        return OllamaEmbeddingModel.builder()
            .ollamaApi(
                OllamaApi.builder()
                    .build())
            .options(
                OllamaEmbeddingOptions.builder()
                    .model(options.getModel())
                    .build())
            .build();
    }
}
