package com.bytechef.platform.ai.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.function.Consumer;

@Configuration
@ConditionalOnProperty(prefix = "bytechef.ai.copilot", name = "enabled", havingValue = "true")
public class AiCopilotConfiguration {

    @Value("${spring.ai.openai.api-key}")
    private String openAiApiKey;

    @Bean
    OpenAiApi openAiApi() {
        HttpComponentsClientHttpRequestFactory httpComponentsClientHttpRequestFactory = new HttpComponentsClientHttpRequestFactory();
        httpComponentsClientHttpRequestFactory.setConnectionRequestTimeout(Duration.ofSeconds(60));
        httpComponentsClientHttpRequestFactory.setConnectTimeout(Duration.ofSeconds(60));
        httpComponentsClientHttpRequestFactory.setReadTimeout(Duration.ofSeconds(60));

        RestClient.Builder builder = RestClient.builder()
            .requestFactory(httpComponentsClientHttpRequestFactory)
            .defaultStatusHandler(HttpStatusCode::isError, (request, response) -> {
                throw new RestClientException("Error response: " + response.getStatusCode() + "; " + response.getStatusText());
            })
            .defaultHeaders(new Consumer<HttpHeaders>() {
                @Override
                public void accept(HttpHeaders httpHeaders) {
                    httpHeaders.set("Accept-Encoding", "gzip, deflate");
                }
            });

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
                    .model(OpenAiApi.ChatModel.CHATGPT_4_O_LATEST)
                    .temperature(0.7)
                    .build())
            .build();

        return ChatClient.builder(chatModel);
    }
}
