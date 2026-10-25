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

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.test.extension.ObjectMapperSetupExtension;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class OllamaModelClientTest {

    private static final String TAGS = """
        {"models":[
          {"name":"deepseek-v4-pro:cloud","remote_host":"https://ollama.com:443","capabilities":["completion","tools"]},
          {"name":"nomic-embed-text:latest","capabilities":["embedding"]},
          {"name":"qwen3:8b","capabilities":["completion","tools","thinking"]},
          {"name":"llama3.2:latest"},
          {"name":"mxbai-embed-large:latest"}
        ]}
        """;

    private final AtomicInteger requestCount = new AtomicInteger();
    private final OllamaModelClient ollamaModelClient = new OllamaModelClient();

    private HttpServer ollamaServer;

    @BeforeEach
    void beforeEach() throws IOException {
        ollamaServer = HttpServer.create(new InetSocketAddress("localhost", 0), 0);

        ollamaServer.createContext("/api/tags", exchange -> {
            requestCount.incrementAndGet();

            byte[] body = TAGS.getBytes(StandardCharsets.UTF_8);

            exchange.getResponseHeaders()
                .add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);

            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(body);
            }
        });

        ollamaServer.createContext("/secured/api/tags", exchange -> {
            String authorization = exchange.getRequestHeaders()
                .getFirst("Authorization");

            byte[] body = TAGS.getBytes(StandardCharsets.UTF_8);

            if (!"Bearer secret-token".equals(authorization)) {
                exchange.sendResponseHeaders(401, -1);
                exchange.close();

                return;
            }

            exchange.sendResponseHeaders(200, body.length);

            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(body);
            }
        });

        ollamaServer.start();
    }

    @AfterEach
    void afterEach() {
        ollamaServer.stop(0);
    }

    @Test
    void testGetChatModelsListsCompletionModelsLocalFirst() {
        assertThat(ollamaModelClient.getChatModels(getUrl(), null))
            .containsExactly("qwen3:8b", "llama3.2:latest", "deepseek-v4-pro:cloud");
    }

    @Test
    void testGetEmbeddingModelsListsEmbeddingModels() {
        assertThat(ollamaModelClient.getEmbeddingModels(getUrl() + "/", null))
            .containsExactly("nomic-embed-text:latest", "mxbai-embed-large:latest");
    }

    @Test
    void testGetChatModelsReturnsEmptyListWhenServerIsUnreachable() {
        int port = ollamaServer.getAddress()
            .getPort();

        ollamaServer.stop(0);

        assertThat(ollamaModelClient.getChatModels("http://localhost:" + port, null)).isEmpty();
    }

    @Test
    void testGetChatModelsSendsApiKeyAsBearerToken() {
        assertThat(ollamaModelClient.getChatModels(getUrl() + "/secured", "secret-token"))
            .containsExactly("qwen3:8b", "llama3.2:latest", "deepseek-v4-pro:cloud");
    }

    @Test
    void testGetChatModelsReturnsEmptyListWithoutApiKeyWhenServerRequiresOne() {
        assertThat(ollamaModelClient.getChatModels(getUrl() + "/secured", null)).isEmpty();
    }

    @Test
    void testGetModelsCachesServerResponse() {
        ollamaModelClient.getChatModels(getUrl(), null);
        ollamaModelClient.getEmbeddingModels(getUrl(), null);

        assertThat(requestCount).hasValue(1);
    }

    private String getUrl() {
        return "http://localhost:" + ollamaServer.getAddress()
            .getPort();
    }
}
