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

package com.bytechef.component.ai.llm.router.model;

import com.bytechef.component.ai.llm.util.ModelUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
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
 * @author Marko Kriskovic
 */
public abstract class RouterChatModel implements org.springframework.ai.chat.model.ChatModel {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final RestClient restClient;
    private final WebClient webClient;
    protected final String model;
    protected final Double frequencyPenalty;
    protected final Map<String, Double> logitBias;
    protected final Boolean logprobs;
    protected final Integer maxCompletionTokens;
    protected final Integer maxTokens;
    protected final Double presencePenalty;
    protected final String reasoning;
    protected final boolean jsonResponseFormat;
    protected final Integer seed;
    protected final List<String> stop;
    protected final Double temperature;
    protected final Double topK;
    protected final Integer topLogprobs;
    protected final Double topP;
    protected final String user;
    protected final String verbosity;

    protected RouterChatModel(String baseUrl, Builder<?> builder) {
        this.restClient = ModelUtils.getRestClientBuilder()
            .baseUrl(baseUrl)
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + builder.apiKey)
            .build();
        this.webClient = WebClient.builder()
            .clientConnector(new JdkClientHttpConnector())
            .baseUrl(baseUrl)
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

    @Override
    public Flux<ChatResponse> stream(Prompt prompt) {
        List<Map<String, Object>> messages = buildMessages(prompt.getInstructions());
        Map<String, Object> body = buildRequestBody(messages);

        body.put("stream", true);

        return webClient.post()
            .uri("/chat/completions")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(body)
            .retrieve()
            .bodyToFlux(new ParameterizedTypeReference<ServerSentEvent<String>>() {})
            .mapNotNull(event -> {
                String data = event.data();

                if (data == null || "[DONE]".equals(data)) {
                    return null;
                }

                return parseStreamChunk(data);
            });
    }

    protected abstract void addProviderSpecificParams(Map<String, Object> body);

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

        addProviderSpecificParams(body);

        body.put("response_format", Map.of("type", jsonResponseFormat ? "json_object" : "text"));

        return body;
    }

    @SuppressWarnings("unchecked")
    private static ChatResponse parseStreamChunk(String data) {
        try {
            Map<String, Object> chunk = OBJECT_MAPPER.readValue(
                data, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
            List<Map<String, Object>> choices = (List<Map<String, Object>>) chunk.get("choices");

            if (choices == null || choices.isEmpty()) {
                return null;
            }

            Map<String, Object> delta = (Map<String, Object>) choices.get(0)
                .get("delta");

            if (delta == null) {
                return null;
            }

            String content = (String) delta.get("content");

            if (content == null) {
                return null;
            }

            return new ChatResponse(List.of(new Generation(new AssistantMessage(content))));
        } catch (JsonProcessingException ignored) {
            return null;
        }
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

    @SuppressWarnings("unchecked")
    private ChatResponse buildChatResponse(Map<String, Object> response) {
        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        Map<String, Object> message = (Map<String, Object>) choices.getFirst()
            .get("message");
        String content = (String) message.get("content");

        return new ChatResponse(List.of(new Generation(new AssistantMessage(content))));
    }

    public String getVerbosity() {
        return verbosity;
    }

    public String getUser() {
        return user;
    }

    public Double getTopP() {
        return topP;
    }

    public Integer getTopLogprobs() {
        return topLogprobs;
    }

    public Double getTopK() {
        return topK;
    }

    public Double getTemperature() {
        return temperature;
    }

    public List<String> getStop() {
        return Collections.unmodifiableList(stop);
    }

    public Integer getSeed() {
        return seed;
    }

    public boolean isJsonResponseFormat() {
        return jsonResponseFormat;
    }

    public String getReasoning() {
        return reasoning;
    }

    public Double getPresencePenalty() {
        return presencePenalty;
    }

    public Integer getMaxTokens() {
        return maxTokens;
    }

    public Integer getMaxCompletionTokens() {
        return maxCompletionTokens;
    }

    public Boolean getLogprobs() {
        return logprobs;
    }

    public Map<String, Double> getLogitBias() {
        return Collections.unmodifiableMap(logitBias);
    }

    public Double getFrequencyPenalty() {
        return frequencyPenalty;
    }

    public String getModel() {
        return model;
    }

    public abstract static class Builder<B extends Builder<B>> {

        protected String apiKey;
        protected String model;
        protected Double frequencyPenalty;
        protected Map<String, Double> logitBias;
        protected Boolean logprobs;
        protected Integer maxCompletionTokens;
        protected Integer maxTokens;
        protected Double presencePenalty;
        protected String reasoning;
        protected boolean jsonResponseFormat;
        protected Integer seed;
        protected List<String> stop;
        protected Double temperature;
        protected Double topK;
        protected Integer topLogprobs;
        protected Double topP;
        protected String user;
        protected String verbosity;

        @SuppressWarnings("unchecked")
        protected final B self() {
            return (B) this;
        }

        public B apiKey(String apiKey) {
            this.apiKey = apiKey;

            return self();
        }

        public B frequencyPenalty(Double frequencyPenalty) {
            this.frequencyPenalty = frequencyPenalty;

            return self();
        }

        public B jsonResponseFormat(boolean jsonResponseFormat) {
            this.jsonResponseFormat = jsonResponseFormat;

            return self();
        }

        public B logitBias(Map<String, Double> logitBias) {
            this.logitBias = logitBias == null ? null : new HashMap<>(logitBias);

            return self();
        }

        public B logprobs(Boolean logprobs) {
            this.logprobs = logprobs;

            return self();
        }

        public B maxCompletionTokens(Integer maxCompletionTokens) {
            this.maxCompletionTokens = maxCompletionTokens;

            return self();
        }

        public B maxTokens(Integer maxTokens) {
            this.maxTokens = maxTokens;

            return self();
        }

        public B model(String model) {
            this.model = model;

            return self();
        }

        public B presencePenalty(Double presencePenalty) {
            this.presencePenalty = presencePenalty;

            return self();
        }

        public B reasoning(String reasoning) {
            this.reasoning = reasoning;

            return self();
        }

        public B seed(Integer seed) {
            this.seed = seed;

            return self();
        }

        public B stop(List<String> stop) {
            this.stop = stop == null ? null : new ArrayList<>(stop);

            return self();
        }

        public B temperature(Double temperature) {
            this.temperature = temperature;

            return self();
        }

        public B topK(Double topK) {
            this.topK = topK;

            return self();
        }

        public B topLogprobs(Integer topLogprobs) {
            this.topLogprobs = topLogprobs;

            return self();
        }

        public B topP(Double topP) {
            this.topP = topP;

            return self();
        }

        public B user(String user) {
            this.user = user;

            return self();
        }

        public B verbosity(String verbosity) {
            this.verbosity = verbosity;

            return self();
        }
    }
}
