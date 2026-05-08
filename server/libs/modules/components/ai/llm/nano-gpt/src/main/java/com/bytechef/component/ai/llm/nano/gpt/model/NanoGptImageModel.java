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

package com.bytechef.component.ai.llm.nano.gpt.model;

import static com.bytechef.component.ai.llm.nano.gpt.constant.NanoGptConstants.BASE_URL;

import com.bytechef.component.ai.llm.util.ModelUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.ai.image.Image;
import org.springframework.ai.image.ImageGeneration;
import org.springframework.ai.image.ImageMessage;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

/**
 * @author Marko Kriskovic
 */
public class NanoGptImageModel implements ImageModel {
    private final RestClient restClient;
    private final String model;
    private final String aspectRatio;
    private final String size;
    private final String user;

    private NanoGptImageModel(Builder builder) {
        this.restClient = ModelUtils.getRestClientBuilder()
            .baseUrl(BASE_URL)
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + builder.apiKey)
            .build();
        this.model = builder.model;
        this.aspectRatio = builder.aspectRatio;
        this.size = builder.size;
        this.user = builder.user;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public ImageResponse call(ImagePrompt prompt) {
        Map<String, Object> body = buildRequestBody(prompt.getInstructions());

        Map<String, Object> response = restClient.post()
            .uri("/chat/completions")
            .contentType(MediaType.APPLICATION_JSON)
            .body(body)
            .retrieve()
            .body(new ParameterizedTypeReference<>() {});

        return buildResponse(response);
    }

    private Map<String, Object> buildRequestBody(List<ImageMessage> messages) {
        Map<String, Object> body = new HashMap<>();

        List<Map<String, Object>> messageList = messages.stream()
            .map(message -> Map.<String, Object>of("role", "user", "content", message.getText()))
            .collect(Collectors.toList());

        body.put("model", model);
        body.put("messages", messageList);
        body.put("modalities", List.of("image"));

        Map<String, Object> imageConfig = new HashMap<>();

        if (aspectRatio != null) {
            imageConfig.put("aspect_ratio", aspectRatio);
        }

        if (size != null) {
            imageConfig.put("image_size", size);
        }

        if (!imageConfig.isEmpty()) {
            body.put("image_config", imageConfig);
        }

        if (user != null) {
            body.put("user", user);
        }

        return body;
    }

    @SuppressWarnings("unchecked")
    private ImageResponse buildResponse(Map<String, Object> response) {
        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        Map<String, Object> message = (Map<String, Object>) choices.getFirst()
            .get("message");
        List<Map<String, Object>> images = (List<Map<String, Object>>) message.get("images");
        Map<String, Object> imageUrl = (Map<String, Object>) images.getFirst()
            .get("image_url");
        String url = (String) imageUrl.get("url");

        if (url.startsWith("data:")) {
            String b64Json = url.substring(url.indexOf(',') + 1);

            return new ImageResponse(List.of(new ImageGeneration(new Image(null, b64Json))));
        }

        return new ImageResponse(List.of(new ImageGeneration(new Image(url, null))));
    }

    public static class Builder {

        private String apiKey;
        private String model;
        private String aspectRatio;
        private String size;
        private String user;

        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        public Builder aspectRatio(String aspectRatio) {
            this.aspectRatio = aspectRatio;
            return this;
        }

        public Builder size(String size) {
            this.size = size;
            return this;
        }

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public Builder user(String user) {
            this.user = user;
            return this;
        }

        public NanoGptImageModel build() {
            return new NanoGptImageModel(this);
        }
    }
}
