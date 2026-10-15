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

package com.bytechef.component.ai.llm.router.requesty.model;

import static com.bytechef.component.ai.llm.router.requesty.constant.RequestyConstants.BASE_URL;

import com.bytechef.component.ai.llm.router.model.RouterChatModel;
import com.bytechef.component.ai.llm.util.ModelUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.JdkClientHttpConnector;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

/**
 * Requesty builds its own chat completions request instead of the one from {@link RouterChatModel}: the Requesty router
 * treats an explicit {@code response_format} of type {@code text} as JSON mode for OpenAI models and rejects the
 * request, so {@code response_format} is only sent when JSON output is requested.
 *
 * @author Thibault Jaigu
 */
public class RequestyChatModel extends RouterChatModel {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final RestClient restClient;
    private final WebClient webClient;

    private RequestyChatModel(Builder builder) {
        super(BASE_URL, builder);

        String authorization = "Bearer " + builder.getApiKey();

        this.restClient = ModelUtils.getRestClientBuilder()
            .baseUrl(BASE_URL)
            .defaultHeader(HttpHeaders.AUTHORIZATION, authorization)
            .build();
        this.webClient = WebClient.builder()
            .clientConnector(new JdkClientHttpConnector())
            .baseUrl(BASE_URL)
            .defaultHeader(HttpHeaders.AUTHORIZATION, authorization)
            .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        Map<String, Object> response = restClient.post()
            .uri("/chat/completions")
            .contentType(MediaType.APPLICATION_JSON)
            .body(buildRequestBody(prompt.getInstructions()))
            .retrieve()
            .body(new ParameterizedTypeReference<>() {});

        if (response == null) {
            throw new IllegalStateException("Empty response from chat completions endpoint");
        }

        return new ChatResponse(List.of(new Generation(new AssistantMessage(getContent(response, "message")))));
    }

    @Override
    public Flux<ChatResponse> stream(Prompt prompt) {
        Map<String, Object> body = buildRequestBody(prompt.getInstructions());

        body.put("stream", true);

        return webClient.post()
            .uri("/chat/completions")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(body)
            .retrieve()
            .bodyToFlux(new ParameterizedTypeReference<ServerSentEvent<String>>() {})
            .mapNotNull(event -> parseStreamChunk(event.data()));
    }

    @Override
    protected void addProviderSpecificParams(Map<String, Object> body) {
        if (reasoning != null) {
            body.put("reasoning_effort", reasoning);
        }
    }

    Map<String, Object> buildRequestBody(List<Message> messages) {
        Map<String, Object> body = new HashMap<>();

        body.put("model", model);
        body.put("messages", buildMessages(messages));

        putIfNotNull(body, "frequency_penalty", frequencyPenalty);
        putIfNotNull(body, "logit_bias", logitBias);
        putIfNotNull(body, "logprobs", logprobs);
        putIfNotNull(body, "max_completion_tokens", maxCompletionTokens);
        putIfNotNull(body, "max_tokens", maxTokens);
        putIfNotNull(body, "presence_penalty", presencePenalty);
        putIfNotNull(body, "seed", seed);

        if (stop != null && !stop.isEmpty()) {
            body.put("stop", stop);
        }

        putIfNotNull(body, "temperature", temperature);
        putIfNotNull(body, "top_k", topK);
        putIfNotNull(body, "top_p", topP);
        putIfNotNull(body, "top_logprobs", topLogprobs);
        putIfNotNull(body, "user", user);
        putIfNotNull(body, "verbosity", verbosity);

        addProviderSpecificParams(body);

        if (jsonResponseFormat) {
            body.put("response_format", Map.of("type", "json_object"));
        }

        return body;
    }

    private static List<Map<String, Object>> buildMessages(List<Message> messages) {
        List<Map<String, Object>> result = new ArrayList<>();

        for (Message message : messages) {
            MessageType messageType = message.getMessageType();
            String text = message.getText() == null ? "" : message.getText();

            if (messageType == MessageType.USER) {
                List<Media> media = ((UserMessage) message).getMedia();

                result.add(Map.of("role", "user", "content", buildUserContent(text, media)));
            } else if (messageType == MessageType.SYSTEM) {
                result.add(Map.of("role", "system", "content", text));
            } else if (messageType == MessageType.ASSISTANT) {
                result.add(Map.of("role", "assistant", "content", text));
            }
        }

        return result;
    }

    private static Object buildUserContent(String text, List<Media> media) {
        if (media == null || media.isEmpty()) {
            return text;
        }

        List<Map<String, Object>> contentParts = new ArrayList<>();

        contentParts.add(Map.of("type", "text", "text", text));

        for (Media attachment : media) {
            String mimeType = attachment.getMimeType()
                .toString();

            if (mimeType.startsWith("image/")) {
                String base64 = Base64.getEncoder()
                    .encodeToString(attachment.getDataAsByteArray());

                contentParts.add(
                    Map.of("type", "image_url", "image_url", Map.of("url", "data:" + mimeType + ";base64," + base64)));
            }
        }

        return contentParts;
    }

    @SuppressWarnings("unchecked")
    private static String getContent(Map<String, Object> response, String messageKey) {
        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");

        if (choices == null || choices.isEmpty()) {
            return null;
        }

        Map<String, Object> message = (Map<String, Object>) choices.getFirst()
            .get(messageKey);

        return message == null ? null : (String) message.get("content");
    }

    private static ChatResponse parseStreamChunk(String data) {
        if (data == null || "[DONE]".equals(data)) {
            return null;
        }

        try {
            String content = getContent(OBJECT_MAPPER.readValue(data, new TypeReference<>() {}), "delta");

            return content == null ? null : new ChatResponse(List.of(new Generation(new AssistantMessage(content))));
        } catch (JsonProcessingException exception) {
            return null;
        }
    }

    private static void putIfNotNull(Map<String, Object> body, String key, Object value) {
        if (value != null) {
            body.put(key, value);
        }
    }

    public static class Builder extends RouterChatModel.Builder<Builder> {

        public RequestyChatModel build() {
            return new RequestyChatModel(this);
        }

        private String getApiKey() {
            return apiKey;
        }
    }
}
