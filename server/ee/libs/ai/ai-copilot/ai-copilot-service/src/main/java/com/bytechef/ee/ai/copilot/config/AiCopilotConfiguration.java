/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.config;

import com.bytechef.config.ApplicationProperties;
import com.github.mizosoft.methanol.Methanol;
import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * @version ee
 *
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
        HttpClient httpClient = Methanol.newBuilder()
            .autoAcceptEncoding(true)
            .connectTimeout(Duration.ofSeconds(60))
            .defaultHeaders(httpHeaders -> httpHeaders.setHeader("Accept-Encoding", "gzip, deflate"))
            .headersTimeout(Duration.ofSeconds(60))
            .readTimeout(Duration.ofSeconds(60))
            .requestTimeout(Duration.ofSeconds(60))
            .build();

        JdkClientHttpRequestFactory jdkClientHttpRequestFactory = new JdkClientHttpRequestFactory(httpClient);

        RestClient.Builder builder = RestClient.builder()
            .requestFactory(jdkClientHttpRequestFactory);

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
