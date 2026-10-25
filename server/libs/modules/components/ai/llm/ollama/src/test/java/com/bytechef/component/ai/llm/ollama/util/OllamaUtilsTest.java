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

package com.bytechef.component.ai.llm.ollama.util;

import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.URL;
import static com.bytechef.component.definition.Authorization.TOKEN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.bytechef.component.definition.Option;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.ollama.api.OllamaApi;

/**
 * @author Ivica Cardic
 */
class OllamaUtilsTest {

    private static final String TAGS_RESPONSE = """
        {"models":[
            {"name":"qwen3:8b","model":"qwen3:8b","size":5225388164,"digest":"500a1f067a9f",
                "details":{"format":"gguf","family":"qwen3","parameter_size":"8.2B","quantization_level":"Q4_K_M"}},
            {"name":"nomic-embed-text:latest","model":"nomic-embed-text:latest","size":274302450,
                "digest":"0a109f422b47","details":{"format":"gguf","family":"nomic-bert","parameter_size":"137M"}},
            {"name":"llama3.2:1b","model":"llama3.2:1b","size":1321098329,"digest":"baf6a787fdff",
                "details":{"format":"gguf","family":"llama","parameter_size":"1.2B"}}
        ]}
        """;

    private final AtomicReference<String> authorizationHeaderReference = new AtomicReference<>();
    private HttpServer httpServer;

    @BeforeEach
    void beforeEach() throws IOException {
        httpServer = HttpServer.create(new InetSocketAddress("localhost", 0), 0);

        httpServer.createContext("/api/tags", exchange -> {
            authorizationHeaderReference.set(exchange.getRequestHeaders()
                .getFirst("Authorization"));

            byte[] body = TAGS_RESPONSE.getBytes(StandardCharsets.UTF_8);

            exchange.getResponseHeaders()
                .add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);

            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(body);
            }
        });

        httpServer.start();
    }

    @AfterEach
    void afterEach() {
        httpServer.stop(0);
    }

    @Test
    void testCreateOllamaApiSendsBearerToken() {
        OllamaApi ollamaApi = OllamaUtils.createOllamaApi(
            MockParametersFactory.create(Map.of(URL, getServerUrl(), TOKEN, "secret-token")));

        ollamaApi.listModels();

        assertEquals("Bearer secret-token", authorizationHeaderReference.get());
    }

    @Test
    void testCreateOllamaApiWithoutTokenSendsNoAuthorization() {
        OllamaApi ollamaApi = OllamaUtils.createOllamaApi(MockParametersFactory.create(Map.of(URL, getServerUrl())));

        ollamaApi.listModels();

        assertNull(authorizationHeaderReference.get());
    }

    @Test
    void testCreateOllamaApiWithEmptyTokenSendsNoAuthorization() {
        Map<String, Object> connectionParameters = new HashMap<>();

        connectionParameters.put(URL, getServerUrl());
        connectionParameters.put(TOKEN, "");

        OllamaApi ollamaApi = OllamaUtils.createOllamaApi(MockParametersFactory.create(connectionParameters));

        ollamaApi.listModels();

        assertNull(authorizationHeaderReference.get());
    }

    @Test
    void testGetModelOptionsListsChatModelsFromServer() throws Exception {
        assertEquals(List.of("llama3.2:1b", "qwen3:8b"), getModelOptionValues(false));
    }

    @Test
    void testGetModelOptionsListsEmbeddingModelsFromServer() throws Exception {
        assertEquals(List.of("nomic-embed-text:latest"), getModelOptionValues(true));
    }

    @Test
    void testGetModelOptionsSendsBearerToken() throws Exception {
        OllamaUtils.getModelOptions(false)
            .apply(
                MockParametersFactory.create(Map.of()),
                MockParametersFactory.create(Map.of(URL, getServerUrl(), TOKEN, "secret-token")), Map.of(), null,
                null);

        assertEquals("Bearer secret-token", authorizationHeaderReference.get());
    }

    private List<String> getModelOptionValues(boolean embeddingModels) throws Exception {
        List<? extends Option<String>> options = OllamaUtils.getModelOptions(embeddingModels)
            .apply(
                MockParametersFactory.create(Map.of()), MockParametersFactory.create(Map.of(URL, getServerUrl())),
                Map.of(), null, null);

        return options.stream()
            .map(Option::getValue)
            .toList();
    }

    private String getServerUrl() {
        InetSocketAddress address = httpServer.getAddress();

        return "http://localhost:" + address.getPort();
    }
}
