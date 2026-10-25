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

package com.bytechef.platform.ai.provider.ollama;

import com.bytechef.commons.util.JsonUtils;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class OllamaModelClient {

    public static final String DEFAULT_URL = "http://localhost:11434";

    private static final Duration CACHE_TTL = Duration.ofSeconds(30);
    private static final String CAPABILITY_COMPLETION = "completion";
    private static final String CAPABILITY_EMBEDDING = "embedding";
    private static final Duration TIMEOUT = Duration.ofSeconds(2);

    private static final Logger log = LoggerFactory.getLogger(OllamaModelClient.class);

    private final Map<String, CachedModels> cachedModelsMap = new ConcurrentHashMap<>();
    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(TIMEOUT)
        .build();

    public List<String> getChatModels(@Nullable String url, @Nullable String apiKey) {
        return filterModelNames(getModels(url, apiKey), OllamaModelClient::isChatModel);
    }

    public List<String> getEmbeddingModels(@Nullable String url, @Nullable String apiKey) {
        return filterModelNames(getModels(url, apiKey), OllamaModelClient::isEmbeddingModel);
    }

    private List<OllamaModel> getModels(@Nullable String url, @Nullable String apiKey) {
        String baseUrl = normalizeUrl(url);
        String cacheKey = apiKey == null || apiKey.isBlank() ? baseUrl : baseUrl + "#" + apiKey;

        CachedModels cachedModels = cachedModelsMap.get(cacheKey);

        if (cachedModels != null && !cachedModels.isExpired()) {
            return cachedModels.models();
        }

        List<OllamaModel> models = fetchModels(baseUrl, apiKey);

        cachedModelsMap.put(cacheKey, new CachedModels(models, System.nanoTime() + CACHE_TTL.toNanos()));

        return models;
    }

    private List<OllamaModel> fetchModels(String baseUrl, @Nullable String apiKey) {
        HttpRequest.Builder httpRequestBuilder = HttpRequest.newBuilder(URI.create(baseUrl + "/api/tags"))
            .timeout(TIMEOUT)
            .GET();

        if (apiKey != null && !apiKey.isBlank()) {
            httpRequestBuilder.header("Authorization", "Bearer " + apiKey);
        }

        HttpRequest httpRequest = httpRequestBuilder.build();

        try {
            HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (httpResponse.statusCode() != 200) {
                log.debug("Ollama server {} returned status {} for /api/tags", baseUrl, httpResponse.statusCode());

                return List.of();
            }

            return parseModels(httpResponse.body());
        } catch (IOException | IllegalArgumentException exception) {
            log.debug("Unable to list the models of the Ollama server {}: {}", baseUrl, exception.getMessage());

            return List.of();
        } catch (InterruptedException interruptedException) {
            Thread.currentThread()
                .interrupt();

            return List.of();
        }
    }

    private static List<String> filterModelNames(List<OllamaModel> models, Predicate<OllamaModel> predicate) {
        return models.stream()
            .filter(predicate)
            .sorted(Comparator.comparing(OllamaModel::remote))
            .map(OllamaModel::name)
            .toList();
    }

    private static boolean isChatModel(OllamaModel model) {
        List<String> capabilities = model.capabilities();

        if (capabilities.isEmpty()) {
            return !isEmbeddingModelName(model.name());
        }

        return capabilities.contains(CAPABILITY_COMPLETION);
    }

    private static boolean isEmbeddingModel(OllamaModel model) {
        List<String> capabilities = model.capabilities();

        if (capabilities.isEmpty()) {
            return isEmbeddingModelName(model.name());
        }

        return capabilities.contains(CAPABILITY_EMBEDDING);
    }

    private static boolean isEmbeddingModelName(String name) {
        return name.contains("embed");
    }

    private static String normalizeUrl(@Nullable String url) {
        if (url == null || url.isBlank()) {
            return DEFAULT_URL;
        }

        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    @SuppressWarnings("unchecked")
    private static List<OllamaModel> parseModels(String body) {
        Map<String, ?> tags = JsonUtils.read(body, Map.class);

        if (!(tags.get("models") instanceof Collection<?> modelEntries)) {
            return List.of();
        }

        List<OllamaModel> models = new ArrayList<>();

        for (Object modelEntry : modelEntries) {
            if (!(modelEntry instanceof Map<?, ?> modelMap) || !(modelMap.get("name") instanceof String name)) {
                continue;
            }

            List<String> capabilities = modelMap.get("capabilities") instanceof Collection<?> capabilityEntries
                ? capabilityEntries.stream()
                    .map(String::valueOf)
                    .toList()
                : List.of();

            models.add(new OllamaModel(name, capabilities, modelMap.get("remote_host") != null));
        }

        return models;
    }

    private record CachedModels(List<OllamaModel> models, long expiresAtNanos) {

        boolean isExpired() {
            return System.nanoTime() - expiresAtNanos > 0;
        }
    }

    private record OllamaModel(String name, List<String> capabilities, boolean remote) {
    }
}
