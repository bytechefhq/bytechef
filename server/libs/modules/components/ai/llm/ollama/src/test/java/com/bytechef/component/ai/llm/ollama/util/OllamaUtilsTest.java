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

import com.bytechef.component.test.definition.MockParametersFactory;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
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

    private final AtomicReference<String> authorizationHeaderReference = new AtomicReference<>();
    private HttpServer httpServer;

    @BeforeEach
    void beforeEach() throws IOException {
        httpServer = HttpServer.create(new InetSocketAddress("localhost", 0), 0);

        httpServer.createContext("/api/tags", exchange -> {
            authorizationHeaderReference.set(exchange.getRequestHeaders()
                .getFirst("Authorization"));

            byte[] body = "{\"models\":[]}".getBytes(StandardCharsets.UTF_8);

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

    private String getServerUrl() {
        InetSocketAddress address = httpServer.getAddress();

        return "http://localhost:" + address.getPort();
    }
}
