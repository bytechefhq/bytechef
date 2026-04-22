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

package com.bytechef.component.ai.llm.open.router.util;

import com.bytechef.component.ai.llm.util.ModelUtils;
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
import org.springframework.web.client.RestClient;

/**
 * @author Marko Kriskovic
 */
public class OpenRouterChatModel implements org.springframework.ai.chat.model.ChatModel {

    private static final String BASE_URL = "https://openrouter.ai/api/v1";

    private final RestClient restClient;
    private final String model;
    private final Double frequencyPenalty;
    private final Map<String, Double> logitBias;
    private final Boolean logprobs;
    private final Integer maxCompletionTokens;
    private final Integer maxTokens;
    private final Double presencePenalty;
    private final String reasoning;
    private final boolean jsonResponseFormat;
    private final Integer seed;
    private final List<String> stop;
    private final Double temperature;
    private final Double topK;
    private final Integer topLogprobs;
    private final Double topP;
    private final String user;
    private final String verbosity;

    private OpenRouterChatModel(Builder builder) {
        this.restClient = ModelUtils.getRestClientBuilder()
            .baseUrl(BASE_URL)
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + builder.apiKey)
            .build();
        this.model = builder.model;
        this.frequencyPenalty = builder.frequencyPenalty;
        this.logitBias = builder.logitBias;
        this.logprobs = builder.logprobs;
        this.maxCompletionTokens = builder.maxCompletionTokens;
        this.maxTokens = builder.maxTokens;
        this.presencePenalty = builder.presencePenalty;
        this.reasoning = builder.reasoning;
        this.jsonResponseFormat = builder.jsonResponseFormat;
        this.seed = builder.seed;
        this.stop = builder.stop;
        this.temperature = builder.temperature;
        this.topK = builder.topK;
        this.topLogprobs = builder.topLogprobs;
        this.topP = builder.topP;
        this.user = builder.user;
        this.verbosity = builder.verbosity;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        List<Map<String, Object>> messages = buildMessages(prompt.getInstructions());
        Map<String, Object> body = buildRequestBody(messages);

        Map<String, Object> response = restClient.post()
            .uri("/chat/completions")
            .contentType(MediaType.APPLICATION_JSON)
            .body(body)
            .retrieve()
            .body(new ParameterizedTypeReference<>() {});

        return buildChatResponse(response);
    }

    private List<Map<String, Object>> buildMessages(List<Message> messages) {
        List<Map<String, Object>> result = new ArrayList<>();

        for (Message message : messages) {
            MessageType messageType = message.getMessageType();

            if (messageType == MessageType.USER) {
                UserMessage userMessage = (UserMessage) message;
                List<Media> media = userMessage.getMedia();

                if (media == null || media.isEmpty()) {
                    result.add(Map.of("role", "user", "content", message.getText()));
                } else {
                    result.add(buildUserMessageWithMedia(message.getText(), media));
                }
            } else if (messageType == MessageType.SYSTEM) {
                result.add(Map.of("role", "system", "content", message.getText()));
            } else if (messageType == MessageType.ASSISTANT) {
                result.add(Map.of("role", "assistant", "content", message.getText()));
            }
        }

        return result;
    }

    private Map<String, Object> buildUserMessageWithMedia(String text, List<Media> media) {
        List<Map<String, Object>> contentParts = new ArrayList<>();
        StringBuilder textBuilder = new StringBuilder(text);

        for (Media attachment : media) {
            String mimeType = attachment.getMimeType()
                .toString();

            if (mimeType.startsWith("image/")) {
                byte[] bytes = attachment.getDataAsByteArray();
                String base64 = Base64.getEncoder()
                    .encodeToString(bytes);

                contentParts.add(Map.of(
                    "type", "image_url",
                    "image_url", Map.of("url", "data:" + mimeType + ";base64," + base64)));
            }
        }

        contentParts.addFirst(Map.of("type", "text", "text", textBuilder.toString()));

        return Map.of("role", "user", "content", contentParts);
    }

    private Map<String, Object> buildRequestBody(List<Map<String, Object>> messages) {
        Map<String, Object> body = new HashMap<>();

        body.put("model", model);
        body.put("messages", messages);

        if (frequencyPenalty != null) {
            body.put("frequency_penalty", frequencyPenalty);
        }

        if (logitBias != null) {
            body.put("logit_bias", logitBias);
        }

        if (logprobs != null) {
            body.put("logprobs", logprobs);
        }

        if (maxCompletionTokens != null) {
            body.put("max_completion_tokens", maxCompletionTokens);
        }

        if (maxTokens != null) {
            body.put("max_tokens", maxTokens);
        }

        if (presencePenalty != null) {
            body.put("presence_penalty", presencePenalty);
        }

        if (reasoning != null) {
            body.put("reasoning", reasoning);
        }

        if (seed != null) {
            body.put("seed", seed);
        }

        if (stop != null && !stop.isEmpty()) {
            body.put("stop", stop);
        }

        if (temperature != null) {
            body.put("temperature", temperature);
        }

        if (topK != null) {
            body.put("top_k", topK);
        }

        if (topP != null) {
            body.put("top_p", topP);
        }

        if (topLogprobs != null) {
            body.put("top_logprobs", topLogprobs);
        }

        if (user != null) {
            body.put("user", user);
        }

        if (verbosity != null) {
            body.put("verbosity", verbosity);
        }

        body.put("response_format", Map.of("type", jsonResponseFormat ? "json_object" : "text"));

        return body;
    }

    @SuppressWarnings("unchecked")
    private ChatResponse buildChatResponse(Map<String, Object> response) {
        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        Map<String, Object> message = (Map<String, Object>) choices.getFirst()
            .get("message");
        String content = (String) message.get("content");

        return new ChatResponse(List.of(new Generation(new AssistantMessage(content))));
    }

    public static class Builder {

        private String apiKey;
        private String model;
        private Double frequencyPenalty;
        private Map<String, Double> logitBias;
        private Boolean logprobs;
        private Integer maxCompletionTokens;
        private Integer maxTokens;
        private Double presencePenalty;
        private String reasoning;
        private boolean jsonResponseFormat;
        private Integer seed;
        private List<String> stop;
        private Double temperature;
        private Double topK;
        private Integer topLogprobs;
        private Double topP;
        private String user;
        private String verbosity;

        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        public Builder frequencyPenalty(Double frequencyPenalty) {
            this.frequencyPenalty = frequencyPenalty;
            return this;
        }

        public Builder jsonResponseFormat(boolean jsonResponseFormat) {
            this.jsonResponseFormat = jsonResponseFormat;
            return this;
        }

        public Builder logitBias(Map<String, Double> logitBias) {
            this.logitBias = logitBias == null ? null : new HashMap<>(logitBias);
            return this;
        }

        public Builder logprobs(Boolean logprobs) {
            this.logprobs = logprobs;
            return this;
        }

        public Builder maxCompletionTokens(Integer maxCompletionTokens) {
            this.maxCompletionTokens = maxCompletionTokens;
            return this;
        }

        public Builder maxTokens(Integer maxTokens) {
            this.maxTokens = maxTokens;
            return this;
        }

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public Builder presencePenalty(Double presencePenalty) {
            this.presencePenalty = presencePenalty;
            return this;
        }

        public Builder reasoning(String reasoning) {
            this.reasoning = reasoning;
            return this;
        }

        public Builder seed(Integer seed) {
            this.seed = seed;
            return this;
        }

        public Builder stop(List<String> stop) {
            this.stop = stop == null ? null : new ArrayList<>(stop);
            return this;
        }

        public Builder temperature(Double temperature) {
            this.temperature = temperature;
            return this;
        }

        public Builder topK(Double topK) {
            this.topK = topK;
            return this;
        }

        public Builder topLogprobs(Integer topLogprobs) {
            this.topLogprobs = topLogprobs;
            return this;
        }

        public Builder topP(Double topP) {
            this.topP = topP;
            return this;
        }

        public Builder user(String user) {
            this.user = user;
            return this;
        }

        public Builder verbosity(String verbosity) {
            this.verbosity = verbosity;
            return this;
        }

        public OpenRouterChatModel build() {
            return new OpenRouterChatModel(this);
        }
    }
}
