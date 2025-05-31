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

package com.bytechef.platform.ai.config;

import com.bytechef.config.ApplicationProperties;
import java.time.Duration;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * @author Marko Kriskovic
 */
@Configuration
@ConditionalOnProperty(prefix = "bytechef.ai.copilot", name = "enabled", havingValue = "true")
public class AiCopilotConfiguration {

    private final String model;
    private final Double temperature;
    private final String openAiApiKey;

    public AiCopilotConfiguration(ApplicationProperties applicationProperties) {
        ApplicationProperties.Ai.OpenAi openAi = applicationProperties.getAi()
            .getCopilot()
            .getOpenAi();

        this.model = openAi.getChat()
            .getOptions()
            .getModel();
        this.temperature = openAi.getChat()
            .getOptions()
            .getTemperature();
        this.openAiApiKey = openAi.getApiKey();
    }

    @Bean
    OpenAiApi openAiApi() {
        HttpComponentsClientHttpRequestFactory httpComponentsClientHttpRequestFactory =
            new HttpComponentsClientHttpRequestFactory();

        httpComponentsClientHttpRequestFactory.setConnectionRequestTimeout(Duration.ofSeconds(60));
        httpComponentsClientHttpRequestFactory.setConnectTimeout(Duration.ofSeconds(60));
        httpComponentsClientHttpRequestFactory.setReadTimeout(Duration.ofSeconds(60));

        RestClient.Builder builder = RestClient.builder()
            .requestFactory(httpComponentsClientHttpRequestFactory)
            .defaultStatusHandler(HttpStatusCode::isError, (request, response) -> {
                throw new RestClientException(
                    "Error response: " + response.getStatusCode() + "; " + response.getStatusText());
            })
            .defaultHeaders(httpHeaders -> httpHeaders.set("Accept-Encoding", "gzip, deflate"));

        return OpenAiApi.builder()
            .apiKey(openAiApiKey)
            .restClientBuilder(builder)
            .build();
    }

    @Bean
    ChatClient.Builder chatClientBuilder(OpenAiApi openAiApi) {
        OpenAiChatModel chatModel = OpenAiChatModel.builder()
            .openAiApi(openAiApi)
            .defaultOptions(
                OpenAiChatOptions.builder()
                    .model(model)
                    .temperature(temperature)
                    .build())
            .build();

        return ChatClient.builder(chatModel);
    }
}
