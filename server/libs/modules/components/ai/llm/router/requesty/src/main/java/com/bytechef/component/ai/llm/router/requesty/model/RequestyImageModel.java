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

import com.bytechef.component.ai.llm.util.ModelUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.ai.image.Image;
import org.springframework.ai.image.ImageGeneration;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

/**
 * @author Thibault Jaigu
 */
public class RequestyImageModel implements ImageModel {

    private final RestClient restClient;
    private final String model;
    private final String quality;
    private final String size;
    private final String user;

    private RequestyImageModel(Builder builder) {
        this.restClient = ModelUtils.getRestClientBuilder()
            .baseUrl(BASE_URL)
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + builder.apiKey)
            .build();
        this.model = builder.model;
        this.quality = builder.quality;
        this.size = builder.size;
        this.user = builder.user;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public ImageResponse call(ImagePrompt prompt) {
        String promptText = prompt.getInstructions()
            .getFirst()
            .getText();

        Map<String, Object> body = new HashMap<>();

        body.put("model", model);
        body.put("prompt", promptText);

        if (quality != null) {
            body.put("quality", quality);
        }

        if (size != null) {
            body.put("size", size);
        }

        if (user != null) {
            body.put("user", user);
        }

        Map<String, Object> response = restClient.post()
            .uri("/images/generations")
            .contentType(MediaType.APPLICATION_JSON)
            .body(body)
            .retrieve()
            .body(new ParameterizedTypeReference<>() {});

        if (response == null) {
            throw new IllegalStateException("Image generation response body is missing");
        }

        return buildResponse(response);
    }

    @SuppressWarnings("unchecked")
    private static ImageResponse buildResponse(Map<String, Object> response) {
        List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("data");

        if (data == null) {
            throw new IllegalStateException("Image generation response contains no data");
        }

        List<ImageGeneration> generations = data.stream()
            .map(item -> new ImageGeneration(new Image((String) item.get("url"), (String) item.get("b64_json"))))
            .toList();

        return new ImageResponse(generations);
    }

    public String getModel() {
        return model;
    }

    public String getQuality() {
        return quality;
    }

    public String getSize() {
        return size;
    }

    public String getUser() {
        return user;
    }

    public static class Builder {

        private String apiKey;
        private String model;
        private String quality;
        private String size;
        private String user;

        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public Builder quality(String quality) {
            this.quality = quality;
            return this;
        }

        public Builder size(String size) {
            this.size = size;
            return this;
        }

        public Builder user(String user) {
            this.user = user;
            return this;
        }

        public RequestyImageModel build() {
            return new RequestyImageModel(this);
        }
    }
}
