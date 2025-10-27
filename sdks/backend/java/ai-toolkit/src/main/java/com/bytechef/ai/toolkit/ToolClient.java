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

package com.bytechef.ai.toolkit;

import com.bytechef.ai.toolkit.model.ToolModel;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class ToolClient {

    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder()
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
        .addModules(new Jdk8Module())
        .addModules(new JavaTimeModule())
        .build();

    private final String apiKey;
    private final String baseUrl;
    private final Environment environment;

    public ToolClient(String apiKey, String baseUrl, Environment environment) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.environment = environment;
    }

    public Map<String, List<ToolModel>> getTools(String externalUserId) {
        try (HttpClient httpClient = HttpClient.newHttpClient()) {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("%s/api/embedded/v1/%s/tools".formatted(baseUrl, externalUserId)))
                .header("Authorization", "Bearer " + apiKey)
                .header("X-Environment", environment.name())
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            return OBJECT_MAPPER.readValue(response.body(), new TypeReference<>() {});
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to fetch tools", e);
        }
    }

    public Object executeTool(String externalUserId, String toolName, Map<String, Object> parameters) {
        try (HttpClient httpClient = HttpClient.newHttpClient()) {
            String requestBody = OBJECT_MAPPER.writeValueAsString(
                Map.of(
                    "name", toolName,
                    "parameters", parameters));

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/embedded/v1/%s/tools".formatted(externalUserId)))
                .header("Authorization", "Bearer " + apiKey)
                .header("X-Environment", environment.name())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            return OBJECT_MAPPER.readValue(response.body(), Object.class);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to execute tool: " + toolName, e);
        }
    }
}
