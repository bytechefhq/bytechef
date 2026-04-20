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

package com.bytechef.ai.config;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.config.ApplicationProperties.Ai.Anthropic;
import com.bytechef.config.ApplicationProperties.Ai.OpenAi;
import com.github.mizosoft.methanol.Methanol;
import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * @author Ivica Cardic
 */
@Configuration
class AiConfiguration {

    private final String anthropicApiKey;
    private final String openAiApiKey;

    public AiConfiguration(ApplicationProperties applicationProperties) {
        ApplicationProperties.Ai ai = applicationProperties.getAi();

        Anthropic anthropic = ai.getAnthropic();

        this.anthropicApiKey = anthropic.getApiKey();

        OpenAi openAi = ai.getOpenAi();

        this.openAiApiKey = openAi.getApiKey();
    }

    @Bean
    @ConditionalOnProperty(prefix = "bytechef.ai.anthropic", name = "api-key")
    AnthropicClient anthropicClient() {
        return AnthropicOkHttpClient.builder()
            .apiKey(anthropicApiKey)
            .build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "bytechef.ai.openai", name = "api-key")
    OpenAiApi openAiApi() {
        return OpenAiApi.builder()
            .apiKey(openAiApiKey)
            .restClientBuilder(getRestClientBuilder())
            .build();
    }

    private static RestClient.Builder getRestClientBuilder() {
        HttpClient httpClient = Methanol.newBuilder()
            .autoAcceptEncoding(true)
            .connectTimeout(Duration.ofSeconds(60))
            .defaultHeaders(httpHeaders -> {
                httpHeaders.setHeader("Accept-Encoding", "gzip, deflate");
            })
            .headersTimeout(Duration.ofSeconds(60))
            .readTimeout(Duration.ofSeconds(60))
            .requestTimeout(Duration.ofSeconds(60))
            .build();

        JdkClientHttpRequestFactory jdkClientHttpRequestFactory = new JdkClientHttpRequestFactory(httpClient);

        return RestClient.builder()
            .requestFactory(jdkClientHttpRequestFactory);
    }
}
