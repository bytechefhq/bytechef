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

package com.bytechef.ai.copilot.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.config.ApplicationProperties.Ai.Copilot.Embedding.Provider;
import com.bytechef.platform.ai.provider.facade.AiProviderFacade;
import com.bytechef.platform.configuration.ai.EmbeddingProviderStatusProvider;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreProperties;

/**
 * @author Ivica Cardic
 */
class CopilotPgVectorConfigurationTest {

    private final List<String> authorizations = new CopyOnWriteArrayList<>();
    private final List<String> requestPaths = new CopyOnWriteArrayList<>();

    private HttpServer ollamaServer;

    @BeforeEach
    void beforeEach() throws IOException {
        ollamaServer = HttpServer.create(new InetSocketAddress("localhost", 0), 0);

        ollamaServer.createContext("/", exchange -> {
            requestPaths.add(exchange.getRequestURI()
                .getPath());

            String authorization = exchange.getRequestHeaders()
                .getFirst("Authorization");

            authorizations.add(authorization == null ? "" : authorization);

            byte[] body = "{\"model\":\"nomic-embed-text\",\"embeddings\":[[0.1,0.2,0.3]]}"
                .getBytes(StandardCharsets.UTF_8);

            exchange.getResponseHeaders()
                .add("Content-Type", "application/json");
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
    void testOllamaDocsEmbeddingModelUsesConfiguredUrl() {
        ApplicationProperties applicationProperties = mockOllamaApplicationProperties();

        EmbeddingModel embeddingModel = new CopilotPgVectorConfiguration().copilotEmbeddingModel(applicationProperties);

        float[] embedding = embeddingModel.embed("ByteChef");

        assertThat(embedding).containsExactly(0.1f, 0.2f, 0.3f);
        assertThat(requestPaths).containsExactly("/api/embed");
        assertThat(authorizations).containsExactly("");
    }

    @Test
    void testOllamaDocsEmbeddingModelSendsConfiguredApiKeyAsBearerToken() {
        ApplicationProperties applicationProperties = mockOllamaApplicationProperties();

        when(applicationProperties.getAi()
            .getProvider()
            .getOllama()
            .getApiKey()).thenReturn("secret-token");

        EmbeddingModel embeddingModel = new CopilotPgVectorConfiguration().copilotEmbeddingModel(applicationProperties);

        embeddingModel.embed("ByteChef");

        assertThat(authorizations).containsExactly("Bearer secret-token");
    }

    @Test
    void testOllamaQueryEmbeddingModelUsesTheServerModelWithoutAUserKey() {
        ApplicationProperties applicationProperties = mockOllamaApplicationProperties();
        AiProviderFacade aiProviderFacade = mock(AiProviderFacade.class);

        EmbeddingModel embeddingModel = new CopilotPgVectorConfiguration().copilotQueryEmbeddingModel(
            applicationProperties, aiProviderFacade, new PgVectorStoreProperties());

        embeddingModel.embed("ByteChef");

        assertThat(requestPaths).containsExactly("/api/embed");
        verifyNoInteractions(aiProviderFacade);
    }

    @Test
    void testOpenAiQueryEmbeddingModelTakesTheKeyFromTheAiProviders() {
        ApplicationProperties applicationProperties = mock(ApplicationProperties.class, RETURNS_DEEP_STUBS);

        when(applicationProperties.getAi()
            .getCopilot()
            .getEmbedding()
            .getProvider()).thenReturn(Provider.OPENAI);

        EmbeddingModel embeddingModel = new CopilotPgVectorConfiguration().copilotQueryEmbeddingModel(
            applicationProperties, mock(AiProviderFacade.class), new PgVectorStoreProperties());

        assertThat(embeddingModel).isInstanceOf(CopilotQueryEmbeddingModel.class);
    }

    @Test
    void testDocsSearchIsActiveWithOllamaOrAUserOpenAiKey() {
        AiProviderFacade aiProviderFacade = mock(AiProviderFacade.class);

        when(aiProviderFacade.getApiKey("ai.provider.openAi", 0)).thenReturn("user-key");

        EmbeddingProviderStatusProvider ollamaStatusProvider = new CopilotPgVectorConfiguration()
            .copilotEmbeddingProviderStatusProvider(mockOllamaApplicationProperties(), aiProviderFacade);

        ApplicationProperties openAiApplicationProperties = mock(ApplicationProperties.class, RETURNS_DEEP_STUBS);

        when(openAiApplicationProperties.getAi()
            .getCopilot()
            .getEmbedding()
            .getProvider()).thenReturn(Provider.OPENAI);

        EmbeddingProviderStatusProvider openAiStatusProvider = new CopilotPgVectorConfiguration()
            .copilotEmbeddingProviderStatusProvider(openAiApplicationProperties, aiProviderFacade);

        assertThat(ollamaStatusProvider.isEmbeddingActive(1)).isTrue();
        assertThat(openAiStatusProvider.isEmbeddingActive(0)).isTrue();
        assertThat(openAiStatusProvider.isEmbeddingActive(1)).isFalse();
    }

    private ApplicationProperties mockOllamaApplicationProperties() {
        ApplicationProperties applicationProperties = mock(ApplicationProperties.class, RETURNS_DEEP_STUBS);

        when(applicationProperties.getAi()
            .getCopilot()
            .getEmbedding()
            .getProvider()).thenReturn(Provider.OLLAMA);
        when(applicationProperties.getAi()
            .getProvider()
            .getEmbedding()
            .getOllama()
            .getOptions()
            .getModel()).thenReturn("nomic-embed-text");
        when(applicationProperties.getAi()
            .getProvider()
            .getOllama()
            .getUrl()).thenReturn("http://localhost:"
                + ollamaServer.getAddress()
                    .getPort());

        return applicationProperties;
    }
}
