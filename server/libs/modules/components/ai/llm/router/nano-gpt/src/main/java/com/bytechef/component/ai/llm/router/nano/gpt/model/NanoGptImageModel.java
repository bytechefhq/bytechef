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

package com.bytechef.component.ai.llm.router.nano.gpt.model;

import static com.bytechef.component.ai.llm.router.nano.gpt.constant.NanoGptConstants.BASE_URL;

import com.bytechef.component.ai.llm.util.ModelUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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
 * @author Marko Kriskovic
 */
public class NanoGptImageModel implements ImageModel {

    private final RestClient restClient;
    private final String model;
    private final String size;
    private final String responseFormat;
    private final String user;
    private final Integer n;
    private final Integer seed;
    private final Double guidanceScale;
    private final Double strength;
    private final Integer numInferenceSteps;

    public String getModel() {
        return model;
    }

    public String getSize() {
        return size;
    }

    public String getResponseFormat() {
        return responseFormat;
    }

    public String getUser() {
        return user;
    }

    public Integer getN() {
        return n;
    }

    public Integer getSeed() {
        return seed;
    }

    public Double getGuidanceScale() {
        return guidanceScale;
    }

    public Double getStrength() {
        return strength;
    }

    public Integer getNumInferenceSteps() {
        return numInferenceSteps;
    }

    private NanoGptImageModel(Builder builder) {
        this.restClient = ModelUtils.getRestClientBuilder()
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + builder.apiKey)
            .build();
        this.model = builder.model;
        this.size = builder.size;
        this.responseFormat = builder.responseFormat;
        this.user = builder.user;
        this.n = builder.n;
        this.seed = builder.seed;
        this.guidanceScale = builder.guidanceScale;
        this.strength = builder.strength;
        this.numInferenceSteps = builder.numInferenceSteps;
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

        body.put("prompt", promptText);
        body.put("model", model);

        if (n != null) {
            body.put("n", n);
        }

        if (size != null) {
            body.put("size", size);
        }

        if (responseFormat != null) {
            body.put("response_format", responseFormat);
        }

        if (seed != null) {
            body.put("seed", seed);
        }

        if (guidanceScale != null) {
            body.put("guidance_scale", guidanceScale);
        }

        if (strength != null) {
            body.put("strength", strength);
        }

        if (numInferenceSteps != null) {
            body.put("num_inference_steps", numInferenceSteps);
        }

        if (user != null) {
            body.put("user", user);
        }

        Map<String, Object> response = restClient.post()
            .uri(BASE_URL + "/images/generations")
            .contentType(MediaType.APPLICATION_JSON)
            .body(body)
            .retrieve()
            .body(new ParameterizedTypeReference<>() {});

        return buildResponse(response);
    }

    @SuppressWarnings("unchecked")
    private ImageResponse buildResponse(Map<String, Object> response) {
        List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("data");

        List<ImageGeneration> generations = data.stream()
            .map(item -> {
                String url = (String) item.get("url");
                String b64Json = (String) item.get("b64_json");

                return new ImageGeneration(new Image(url, b64Json));
            })
            .collect(Collectors.toList());

        return new ImageResponse(generations);
    }

    public static class Builder {

        private String apiKey;
        private String model;
        private String size;
        private String responseFormat;
        private String user;
        private Integer n;
        private Integer seed;
        private Double guidanceScale;
        private Double strength;
        private Integer numInferenceSteps;

        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public Builder size(String size) {
            this.size = size;
            return this;
        }

        public Builder responseFormat(String responseFormat) {
            this.responseFormat = responseFormat;
            return this;
        }

        public Builder user(String user) {
            this.user = user;
            return this;
        }

        public Builder n(Integer n) {
            this.n = n;
            return this;
        }

        public Builder seed(Integer seed) {
            this.seed = seed;
            return this;
        }

        public Builder guidanceScale(Double guidanceScale) {
            this.guidanceScale = guidanceScale;
            return this;
        }

        public Builder strength(Double strength) {
            this.strength = strength;
            return this;
        }

        public Builder numInferenceSteps(Integer numInferenceSteps) {
            this.numInferenceSteps = numInferenceSteps;
            return this;
        }

        public NanoGptImageModel build() {
            return new NanoGptImageModel(this);
        }
    }
}
