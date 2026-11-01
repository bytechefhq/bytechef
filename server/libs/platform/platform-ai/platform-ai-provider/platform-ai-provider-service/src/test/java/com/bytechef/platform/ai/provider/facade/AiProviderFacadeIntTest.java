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

package com.bytechef.platform.ai.provider.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.commons.data.jdbc.converter.EncryptedMapWrapperToStringConverter;
import com.bytechef.commons.data.jdbc.converter.EncryptedStringToMapWrapperConverter;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.encryption.Encryption;
import com.bytechef.encryption.EncryptionKey;
import com.bytechef.jackson.config.JacksonConfiguration;
import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.platform.ai.llm.Provider;
import com.bytechef.platform.ai.provider.dto.AiDefaultModelDTO;
import com.bytechef.platform.ai.provider.dto.AiProviderCatalogItemDTO;
import com.bytechef.platform.ai.provider.dto.AiProviderDTO;
import com.bytechef.platform.ai.provider.ollama.OllamaModelClient;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.domain.Property;
import com.bytechef.platform.configuration.domain.Property.Scope;
import com.bytechef.platform.configuration.repository.PropertyRepository;
import com.bytechef.platform.configuration.service.PropertyService;
import com.bytechef.platform.configuration.service.PropertyServiceImpl;
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import com.sun.net.httpserver.HttpServer;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tools.jackson.databind.ObjectMapper;

/**
 * @author Ivica Cardic
 */
@ActiveProfiles("testint")
@SpringBootTest(classes = AiProviderFacadeIntTest.AiProviderFacadeIntTestConfiguration.class)
class AiProviderFacadeIntTest {

    private static final int ENVIRONMENT = 0;

    private static final String TAGS = """
        {"models":[
          {"name":"deepseek-v4-pro:cloud","remote_host":"https://ollama.com:443","capabilities":["completion","tools"]},
          {"name":"nomic-embed-text:latest","capabilities":["embedding"]},
          {"name":"qwen3:8b","capabilities":["completion","tools","thinking"]}
        ]}
        """;

    @Autowired
    private AiProviderFacade aiProviderFacade;

    @Autowired
    private ApplicationProperties applicationProperties;

    @MockitoBean
    private ComponentDefinitionService componentDefinitionService;

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private PropertyService propertyService;

    private HttpServer ollamaServer;

    @BeforeEach
    void beforeEach() throws IOException {
        ollamaServer = HttpServer.create(new InetSocketAddress("localhost", 0), 0);

        ollamaServer.createContext("/api/tags", exchange -> {
            byte[] body = TAGS.getBytes(StandardCharsets.UTF_8);

            exchange.getResponseHeaders()
                .add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);

            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(body);
            }
        });

        ollamaServer.createContext("/secured/api/tags", exchange -> {
            byte[] body = TAGS.getBytes(StandardCharsets.UTF_8);

            if (!"Bearer secret-token".equals(exchange.getRequestHeaders()
                .getFirst("Authorization"))) {

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

        List<ComponentDefinition> componentDefinitions = Arrays.stream(Provider.values())
            .map(provider -> {
                ComponentDefinition componentDefinition = mock(ComponentDefinition.class);

                when(componentDefinition.getName()).thenReturn(provider.getName());
                when(componentDefinition.getActions()).thenReturn(List.of());

                return componentDefinition;
            })
            .toList();

        when(componentDefinitionService.getComponentDefinitions()).thenReturn(componentDefinitions);
    }

    @AfterEach
    void afterEach() {
        ollamaServer.stop(0);

        propertyRepository.deleteAll();

        applicationProperties.getAi()
            .getProvider()
            .getChat()
            .getOllama()
            .getOptions()
            .setModel(null);

        applicationProperties.getAi()
            .getProvider()
            .getOllama()
            .setUrl(null);
    }

    @Test
    void testOllamaDefaultChatModelIsFirstInstalledLocalModelWhenNoneIsConfigured() {
        enableOllama();

        AiDefaultModelDTO defaultModel = aiProviderFacade.getAiDefaultChatModel(ENVIRONMENT);

        assertThat(defaultModel).isEqualTo(new AiDefaultModelDTO(Provider.OLLAMA.getKey(), "qwen3:8b"));
    }

    @Test
    void testOllamaConfiguredChatModelTakesPrecedenceOverInstalledModels() {
        applicationProperties.getAi()
            .getProvider()
            .getChat()
            .getOllama()
            .getOptions()
            .setModel("llama3.2:latest");

        enableOllama();

        AiDefaultModelDTO defaultModel = aiProviderFacade.getAiDefaultChatModel(ENVIRONMENT);

        assertThat(defaultModel).isEqualTo(new AiDefaultModelDTO(Provider.OLLAMA.getKey(), "llama3.2:latest"));
    }

    @Test
    void testOllamaDefaultEmbeddingModelIsFirstInstalledEmbeddingModelWhenNoneIsConfigured() {
        enableOllama();

        AiDefaultModelDTO defaultModel = aiProviderFacade.getAiDefaultEmbeddingModel(ENVIRONMENT);

        assertThat(defaultModel)
            .isEqualTo(new AiDefaultModelDTO(Provider.OLLAMA.getKey(), "nomic-embed-text:latest"));
    }

    @Test
    void testOllamaCatalogListsInstalledChatModels() {
        enableOllama();

        AiProviderCatalogItemDTO ollama = aiProviderFacade.getAiChatProviderCatalog(ENVIRONMENT)
            .stream()
            .filter(catalogItem -> Provider.OLLAMA.getKey()
                .equals(catalogItem.key()))
            .findFirst()
            .orElseThrow();

        assertThat(ollama.enabled()).isTrue();
        assertThat(ollama.models()).containsExactly(
            new AiProviderCatalogItemDTO.Model("qwen3:8b", "qwen3:8b"),
            new AiProviderCatalogItemDTO.Model("deepseek-v4-pro:cloud", "deepseek-v4-pro:cloud"));
    }

    @Test
    void testOllamaDefaultChatModelUsesApiKeyToListInstalledModels() {
        String url = "http://localhost:" + ollamaServer.getAddress()
            .getPort() + "/secured";

        aiProviderFacade.updateAiProvider(Provider.OLLAMA.getId(), "secret-token", url, ENVIRONMENT);

        AiDefaultModelDTO defaultModel = aiProviderFacade.getAiDefaultChatModel(ENVIRONMENT);

        assertThat(defaultModel).isEqualTo(new AiDefaultModelDTO(Provider.OLLAMA.getKey(), "qwen3:8b"));
    }

    @Test
    void testUpdateAiProviderEnabledUpdatesProperty() {
        aiProviderFacade.updateAiProvider(Provider.OLLAMA.getId(), true, ENVIRONMENT);

        assertThat(getSavedProperty(Provider.OLLAMA).isEnabled()).isTrue();
        assertThat(getAiProvider(Provider.OLLAMA).enabled()).isTrue();

        aiProviderFacade.updateAiProvider(Provider.OLLAMA.getId(), false, ENVIRONMENT);

        assertThat(getSavedProperty(Provider.OLLAMA).isEnabled()).isFalse();
        assertThat(getAiProvider(Provider.OLLAMA).enabled()).isFalse();
    }

    @Test
    void testEmbeddingProvidersSupportEmbeddings() {
        List<AiProviderDTO> aiProviders = aiProviderFacade.getAiProviders(ENVIRONMENT);

        assertThat(aiProviders).hasSize(Provider.values().length);

        for (AiProviderDTO aiProvider : aiProviders) {
            Provider matchingProvider = Arrays.stream(Provider.values())
                .filter(provider -> provider.getId() == aiProvider.id())
                .findFirst()
                .orElseThrow();

            assertThat(aiProvider.supportsEmbeddings())
                .as("Provider %s supports embeddings iff in Provider.EMBEDDING_PROVIDERS", matchingProvider)
                .isEqualTo(Provider.EMBEDDING_PROVIDERS.contains(matchingProvider));
        }
    }

    @Test
    void testKeylessProviderConfiguredWithUrlInApplicationPropertiesIsEnabled() {
        applicationProperties.getAi()
            .getProvider()
            .getOllama()
            .setUrl("http://localhost:11434");

        AiProviderDTO ollama = getAiProvider(Provider.OLLAMA);

        assertThat(ollama.enabled()).isTrue();
        assertThat(ollama.url()).isEqualTo("http://localhost:11434");
    }

    @Test
    void testKeylessProviderWithoutUrlInApplicationPropertiesIsDisabled() {
        AiProviderDTO ollama = getAiProvider(Provider.OLLAMA);

        assertThat(ollama.enabled()).isFalse();
    }

    @Test
    void testKeyedProviderWithoutApiKeyInApplicationPropertiesIsDisabled() {
        AiProviderDTO openAi = getAiProvider(Provider.OPEN_AI);

        assertThat(openAi.enabled()).isFalse();
    }

    @Test
    void testUpdateAiProviderKeepsSavedApiKeyWhenNoneIsSent() {
        saveOllamaProperty(Map.of("apiKey", "secret-token", "url", "http://ollama.internal:11434"));

        aiProviderFacade.updateAiProvider(Provider.OLLAMA.getId(), null, "http://ollama.example:11434", ENVIRONMENT);

        assertThat(getSavedProperty(Provider.OLLAMA).getValue())
            .isEqualTo(Map.of("apiKey", "secret-token", "url", "http://ollama.example:11434"));
    }

    @Test
    void testUpdateAiProviderRemovesSavedUrlWhenBlankUrlIsSent() {
        saveOllamaProperty(Map.of("apiKey", "secret-token", "url", "http://ollama.internal:11434"));

        aiProviderFacade.updateAiProvider(Provider.OLLAMA.getId(), null, "", ENVIRONMENT);

        assertThat(getSavedProperty(Provider.OLLAMA).getValue()).isEqualTo(Map.of("apiKey", "secret-token"));
    }

    @Test
    void testUpdateAiProviderRemovesSavedApiKeyWhenBlankApiKeyIsSent() {
        saveOllamaProperty(Map.of("apiKey", "secret-token", "url", "http://ollama.internal:11434"));

        aiProviderFacade.updateAiProvider(Provider.OLLAMA.getId(), "", "http://ollama.internal:11434", ENVIRONMENT);

        assertThat(getSavedProperty(Provider.OLLAMA).getValue())
            .isEqualTo(Map.of("url", "http://ollama.internal:11434"));
    }

    @Test
    void testUpdateAiProviderSavesNewApiKeyAndUrl() {
        assertThat(propertyService.fetchProperty(Provider.OLLAMA.getKey(), Scope.PLATFORM, null, (long) ENVIRONMENT))
            .isEmpty();

        aiProviderFacade.updateAiProvider(
            Provider.OLLAMA.getId(), "secret-token", "http://ollama.example:11434", ENVIRONMENT);

        assertThat(getSavedProperty(Provider.OLLAMA).getValue())
            .isEqualTo(Map.of("apiKey", "secret-token", "url", "http://ollama.example:11434"));
    }

    @Test
    void testDeleteOfAnUnconfiguredProviderFails() {
        assertThatThrownBy(() -> aiProviderFacade.deleteAiProvider(Provider.OLLAMA.getId(), ENVIRONMENT))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testDeleteRemovesAConfiguredProvider() {
        enableOllama();

        aiProviderFacade.deleteAiProvider(Provider.OLLAMA.getId(), ENVIRONMENT);

        assertThat(propertyRepository.count()).isZero();
    }

    private void enableOllama() {
        String url = "http://localhost:" + ollamaServer.getAddress()
            .getPort();

        aiProviderFacade.updateAiProvider(Provider.OLLAMA.getId(), null, url, ENVIRONMENT);
    }

    private AiProviderDTO getAiProvider(Provider provider) {
        return aiProviderFacade.getAiProviders(ENVIRONMENT)
            .stream()
            .filter(aiProvider -> aiProvider.id() == provider.getId())
            .findFirst()
            .orElseThrow();
    }

    private Property getSavedProperty(Provider provider) {
        return propertyService.fetchProperty(provider.getKey(), Scope.PLATFORM, null, (long) ENVIRONMENT)
            .orElseThrow();
    }

    private void saveOllamaProperty(Map<String, ?> value) {
        propertyService.save(Provider.OLLAMA.getKey(), value, Scope.PLATFORM, null, (long) ENVIRONMENT);
    }

    @ComponentScan(basePackages = "com.bytechef.encryption")
    @Configuration
    @EnableAutoConfiguration
    @Import({
        AiProviderFacadeImpl.class, JacksonConfiguration.class, LiquibaseConfiguration.class, OllamaModelClient.class,
        PostgreSQLContainerConfiguration.class, PropertyServiceImpl.class
    })
    static class AiProviderFacadeIntTestConfiguration {

        @Bean
        ApplicationProperties applicationProperties() {
            return new ApplicationProperties();
        }

        @Bean
        EncryptionKey encryptionKey() {
            return () -> "tTB1/UBIbYLuCXVi4PPfzA==";
        }

        @Configuration
        @EnableJdbcAuditing(auditorAwareRef = "auditorProvider", dateTimeProviderRef = "auditingDateTimeProvider")
        static class AiProviderFacadeIntTestJdbcConfiguration extends AbstractIntTestJdbcConfiguration {

            private final Encryption encryption;
            private final ObjectMapper objectMapper;

            @SuppressFBWarnings("EI2")
            AiProviderFacadeIntTestJdbcConfiguration(Encryption encryption, ObjectMapper objectMapper) {
                this.encryption = encryption;
                this.objectMapper = objectMapper;
            }

            @Override
            protected List<?> userConverters() {
                return Arrays.asList(
                    new EncryptedMapWrapperToStringConverter(encryption, objectMapper),
                    new EncryptedStringToMapWrapperConverter(encryption, objectMapper));
            }
        }
    }
}
