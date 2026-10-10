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
import static org.mockito.Mockito.doReturn;
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
import com.bytechef.platform.ai.provider.dto.AiDefaultModelWithApiKeyDTO;
import com.bytechef.platform.ai.provider.dto.AiProviderCatalogItemDTO;
import com.bytechef.platform.ai.provider.dto.AiProviderDTO;
import com.bytechef.platform.ai.provider.ollama.OllamaModelClient;
import com.bytechef.platform.component.domain.ActionDefinition;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.domain.Option;
import com.bytechef.platform.component.domain.StringProperty;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.domain.Property;
import com.bytechef.platform.configuration.domain.Property.Scope;
import com.bytechef.platform.configuration.repository.PropertyRepository;
import com.bytechef.platform.configuration.service.PropertyService;
import com.bytechef.platform.configuration.service.PropertyServiceImpl;
import com.bytechef.platform.connection.aiprovider.AiProviderConnectionSource;
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

    @MockitoBean
    private AiProviderConnectionSource aiProviderConnectionSource;

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
                when(componentDefinitionService.getComponentDefinition(provider.getName(), 0))
                    .thenReturn(componentDefinition);

                return componentDefinition;
            })
            .toList();

        when(componentDefinitionService.getComponentDefinitions()).thenReturn(componentDefinitions);
    }

    @AfterEach
    void afterEach() {
        ollamaServer.stop(0);

        propertyRepository.deleteAll();

        applicationProperties.setAi(new ApplicationProperties.Ai());
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

        AiProviderCatalogItemDTO ollama = getCatalogItem(Provider.OLLAMA);

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
    void testGetChatProviderCatalogUsesOptionLabel() {
        ComponentDefinition anthropicDefinition = mockChatComponentDefinition(
            "anthropic", "anthropic-icon", List.of(mockOption("claude-fine-tune", "My Claude Fine-Tune")));

        when(componentDefinitionService.getComponentDefinitions()).thenReturn(List.of(anthropicDefinition));

        AiProviderCatalogItemDTO anthropic = getCatalogItem(Provider.ANTHROPIC);

        assertThat(anthropic.models()
            .getFirst()
            .label()).isEqualTo("My Claude Fine-Tune");
    }

    @Test
    void testGetChatProviderCatalogMarksProviderNotEnabledWhenFullDefinitionFailsToLoad() {
        ComponentDefinition anthropicDefinition = mock(ComponentDefinition.class);

        when(anthropicDefinition.getName()).thenReturn("anthropic");
        when(anthropicDefinition.getIcon()).thenReturn("anthropic-icon");
        when(anthropicDefinition.getVersion()).thenReturn(1);

        when(componentDefinitionService.getComponentDefinitions()).thenReturn(List.of(anthropicDefinition));
        when(componentDefinitionService.getComponentDefinition("anthropic", 1))
            .thenThrow(new IllegalStateException("definition failed to load"));

        aiProviderFacade.updateAiProvider(Provider.ANTHROPIC.getId(), true, ENVIRONMENT);

        assertThat(getSavedProperty(Provider.ANTHROPIC).isEnabled()).isTrue();

        AiProviderCatalogItemDTO anthropic = getCatalogItem(Provider.ANTHROPIC);

        assertThat(anthropic.enabled()).isFalse();
        assertThat(anthropic.models()).isEmpty();
    }

    @Test
    void testGetChatProviderCatalogFallsBackToModelIdWhenNoLabelAnywhere() {
        ComponentDefinition anthropicDefinition = mockChatComponentDefinition(
            "anthropic", "anthropic-icon", List.of(mockOption("claude-fine-tune", null)));

        when(componentDefinitionService.getComponentDefinitions()).thenReturn(List.of(anthropicDefinition));

        AiProviderCatalogItemDTO anthropic = getCatalogItem(Provider.ANTHROPIC);

        assertThat(anthropic.models()
            .getFirst()
            .label()).isEqualTo("claude-fine-tune");
    }

    @Test
    void testGetChatProviderCatalogContainsOpenAiAndAnthropic() {
        List<String> keys = getCatalogKeys();

        assertThat(keys).contains("ai.provider.openAi", "ai.provider.anthropic");
    }

    @Test
    void testGetChatProviderCatalogExcludesUnsupportedProviders() {
        List<String> keys = getCatalogKeys();

        assertThat(keys).contains("ai.provider.anthropic", "ai.provider.openAi", "ai.provider.groq")
            .doesNotContain("ai.provider.stability", "ai.provider.huggingFace");
    }

    @Test
    void testGetChatProviderCatalogWithModelOptionsYieldsModelsAndSupportsModelByIdFalse() {
        ComponentDefinition anthropicDefinition = mockChatComponentDefinition(
            "anthropic", "anthropic-icon",
            List.of(
                mockOption("claude-sonnet-4-6", "Claude Sonnet 4.6"),
                mockOption("claude-haiku-4-5", "Claude Haiku 4.5")));

        when(componentDefinitionService.getComponentDefinitions()).thenReturn(List.of(anthropicDefinition));

        AiProviderCatalogItemDTO anthropic = getCatalogItem(Provider.ANTHROPIC);

        assertThat(anthropic.models()).hasSize(2);
        assertThat(anthropic.models()
            .get(0)
            .name()).isEqualTo("claude-sonnet-4-6");
        assertThat(anthropic.models()
            .get(0)
            .label()).isEqualTo("Claude Sonnet 4.6");
        assertThat(anthropic.models()
            .get(1)
            .name()).isEqualTo("claude-haiku-4-5");
        assertThat(anthropic.supportsModelById()).isFalse();
    }

    @Test
    void testGetChatProviderCatalogWithNoModelOptionsYieldsEmptyModelsAndSupportsModelByIdTrue() {
        ComponentDefinition groqDefinition = mockChatComponentDefinition("groq", "groq-icon", List.of());

        when(componentDefinitionService.getComponentDefinitions()).thenReturn(List.of(groqDefinition));

        AiProviderCatalogItemDTO groq = getCatalogItem(Provider.GROQ);

        assertThat(groq.models()).isEmpty();
        assertThat(groq.supportsModelById()).isTrue();
    }

    @Test
    void testGetChatProviderCatalogReadsModelOptionsFromFullDefinitionNotListStubs() {
        ComponentDefinition anthropicStub = mock(ComponentDefinition.class);

        when(anthropicStub.getName()).thenReturn("anthropic");
        when(anthropicStub.getVersion()).thenReturn(1);
        when(anthropicStub.getActions()).thenReturn(List.of());

        when(componentDefinitionService.getComponentDefinitions()).thenReturn(List.of(anthropicStub));

        ActionDefinition chatAction = mockChatAction(List.of(mockOption("claude-sonnet-4-6", "Claude Sonnet 4.6")));

        ComponentDefinition anthropicFullDefinition = mock(ComponentDefinition.class);

        when(anthropicFullDefinition.getActions()).thenReturn(List.of(chatAction));
        when(componentDefinitionService.getComponentDefinition("anthropic", 1)).thenReturn(anthropicFullDefinition);

        AiProviderCatalogItemDTO anthropic = getCatalogItem(Provider.ANTHROPIC);

        assertThat(anthropic.models()).hasSize(1);
        assertThat(anthropic.models()
            .getFirst()
            .name()).isEqualTo("claude-sonnet-4-6");
        assertThat(anthropic.supportsModelById()).isFalse();
    }

    @Test
    void testGetChatProviderCatalogDtoContainsNoApiKey() {
        List<AiProviderCatalogItemDTO> catalog = aiProviderFacade.getAiChatProviderCatalog(ENVIRONMENT);

        assertThat(catalog).isNotEmpty();
        assertThat(catalog.get(0)).isInstanceOf(AiProviderCatalogItemDTO.class);
    }

    @Test
    void testGetAiChatProviderCatalogIncludesVertexGeminiMatchedByGeminiComponent() {
        ComponentDefinition gemini = mock(ComponentDefinition.class);

        when(gemini.getName()).thenReturn("gemini");
        when(gemini.getIcon()).thenReturn("<svg>gemini</svg>");
        when(gemini.getActions()).thenReturn(List.of());

        when(componentDefinitionService.getComponentDefinitions()).thenReturn(List.of(gemini));

        List<AiProviderCatalogItemDTO> catalog = aiProviderFacade.getAiChatProviderCatalog(ENVIRONMENT);

        assertThat(catalog)
            .extracting(AiProviderCatalogItemDTO::key)
            .contains("ai.provider.vertexGemini");
    }

    @Test
    void testGetAiProviderCatalogMarksChatProviderActiveWhenConfiguredViaApplicationProperties() {
        applicationProperties.getAi()
            .getProvider()
            .getOpenAi()
            .setApiKey("sk-config");

        AiProviderCatalogItemDTO openAi = getCatalogItem(Provider.OPEN_AI);

        assertThat(openAi.enabled()).isTrue();

        AiProviderCatalogItemDTO anthropic = getCatalogItem(Provider.ANTHROPIC);

        assertThat(anthropic.enabled()).isFalse();
    }

    @Test
    void testCatalogEnabledViaDelegatedSource() {
        when(aiProviderConnectionSource.getSupportedProviders()).thenReturn(List.of(Provider.OPEN_AI));
        when(aiProviderConnectionSource.isEnabled(Provider.OPEN_AI, ENVIRONMENT)).thenReturn(true);

        AiProviderCatalogItemDTO openAi = getCatalogItem(Provider.OPEN_AI);

        assertThat(openAi.enabled()).isTrue();
        assertThat(propertyService.fetchProperty(Provider.OPEN_AI.getKey(), Scope.PLATFORM, null, (long) ENVIRONMENT))
            .isEmpty();
    }

    @Test
    void testResolvesPreferredProviderByShortName() {
        configureAnthropic("claude-sonnet-4-6");
        configureOpenAi("gpt-5.1");

        AiDefaultModelDTO result = aiProviderFacade.getAiDefaultChatModel("anthropic", ENVIRONMENT);

        assertThat(result).isEqualTo(new AiDefaultModelDTO("ai.provider.anthropic", "claude-sonnet-4-6"));
    }

    @Test
    void testReturnsAnthropicWhenAnthropicApiKeyConfigured() {
        configureAnthropic("claude-sonnet-4-6");

        AiDefaultModelDTO result = aiProviderFacade.getAiDefaultChatModel(ENVIRONMENT);

        assertThat(result).isEqualTo(new AiDefaultModelDTO("ai.provider.anthropic", "claude-sonnet-4-6"));
    }

    @Test
    void testFallsBackToOpenAiWhenOnlyOpenAiApiKeyConfigured() {
        configureOpenAi("gpt-5.1");

        AiDefaultModelDTO result = aiProviderFacade.getAiDefaultChatModel(ENVIRONMENT);

        assertThat(result).isEqualTo(new AiDefaultModelDTO("ai.provider.openAi", "gpt-5.1"));
    }

    @Test
    void testReturnsNullWhenNoApiKeyConfigured() {
        AiDefaultModelDTO result = aiProviderFacade.getAiDefaultChatModel(ENVIRONMENT);

        assertThat(result).isNull();
    }

    @Test
    void testReturnsNullWhenResolvedProviderHasNoConfiguredModel() {
        configureAnthropic("  ");

        AiDefaultModelDTO result = aiProviderFacade.getAiDefaultChatModel(ENVIRONMENT);

        assertThat(result).isNull();
    }

    @Test
    void testReturnsOpenAiEmbeddingModelWhenEnabled() {
        configureOpenAiEmbedding("text-embedding-3-small");

        AiDefaultModelDTO result = aiProviderFacade.getAiDefaultEmbeddingModel(ENVIRONMENT);

        assertThat(result).isEqualTo(new AiDefaultModelDTO("ai.provider.openAi", "text-embedding-3-small"));
    }

    @Test
    void testReturnsNullEmbeddingModelWhenNoProviderEnabled() {
        AiDefaultModelDTO result = aiProviderFacade.getAiDefaultEmbeddingModel(ENVIRONMENT);

        assertThat(result).isNull();
    }

    @Test
    void testGetApiKeyReturnsStoredKeyWhenEnabled() {
        aiProviderFacade.updateAiProvider(Provider.OPEN_AI.getId(), "sk-stored", null, ENVIRONMENT);

        String apiKey = aiProviderFacade.getApiKey("ai.provider.openAi", ENVIRONMENT);

        assertThat(apiKey).isEqualTo("sk-stored");
    }

    @Test
    void testGetApiKeyFallsBackToConfigKeyWhenNoStoredProperty() {
        applicationProperties.getAi()
            .getProvider()
            .getOpenAi()
            .setApiKey("sk-openai");

        String apiKey = aiProviderFacade.getApiKey("ai.provider.openAi", ENVIRONMENT);

        assertThat(apiKey).isEqualTo("sk-openai");
    }

    @Test
    void testReturnsChatModelApiKeyWhenActivated() {
        configureAnthropic("claude-sonnet-4-6");

        AiDefaultModelWithApiKeyDTO result = aiProviderFacade.getAiDefaultChatModelApiKey(ENVIRONMENT);

        assertThat(result).isNotNull();
        assertThat(result.provider()).isEqualTo(Provider.ANTHROPIC);
        assertThat(result.model()).isEqualTo("claude-sonnet-4-6");
        assertThat(result.apiKey()).isEqualTo("sk-anthropic");
    }

    @Test
    void testReturnsNullChatModelApiKeyWhenNoProviderActivated() {
        assertThat(aiProviderFacade.getAiDefaultChatModelApiKey(ENVIRONMENT)).isNull();
    }

    @Test
    void testReturnsEmbeddingModelApiKeyWhenActivated() {
        configureOpenAiEmbedding("text-embedding-3-small");

        AiDefaultModelWithApiKeyDTO result = aiProviderFacade.getAiDefaultEmbeddingModelApiKey(ENVIRONMENT);

        assertThat(result).isNotNull();
        assertThat(result.provider()).isEqualTo(Provider.OPEN_AI);
        assertThat(result.model()).isEqualTo("text-embedding-3-small");
        assertThat(result.apiKey()).isEqualTo("sk-openai");
    }

    @Test
    void testGetAiProvidersFallsBackToConfigApiKeyWhenNoStoredProperty() {
        applicationProperties.getAi()
            .getProvider()
            .getOpenAi()
            .setApiKey("sk-config");

        AiProviderDTO openAi = getAiProvider(Provider.OPEN_AI);

        assertThat(openAi.apiKey()).isEqualTo("sk-config");
        assertThat(openAi.enabled()).isTrue();
    }

    @Test
    void testGetAiProvidersPrefersStoredPropertyApiKeyOverConfig() {
        applicationProperties.getAi()
            .getProvider()
            .getOpenAi()
            .setApiKey("sk-config");

        aiProviderFacade.updateAiProvider(Provider.OPEN_AI.getId(), "sk-stored", null, ENVIRONMENT);

        AiProviderDTO openAi = getAiProvider(Provider.OPEN_AI);

        assertThat(openAi.apiKey()).isEqualTo("sk-stored");
        assertThat(openAi.enabled()).isTrue();
    }

    @Test
    void testGetAiProvidersLeavesProviderDisabledWithoutStoredOrConfigKey() {
        AiProviderDTO openAi = getAiProvider(Provider.OPEN_AI);

        assertThat(openAi.apiKey()).isNull();
        assertThat(openAi.enabled()).isFalse();
    }

    @Test
    void testGetUrlPrefersStoredUrlOverConfig() {
        applicationProperties.getAi()
            .getProvider()
            .getOllama()
            .setUrl("http://config-host:11434");

        aiProviderFacade.updateAiProvider(Provider.OLLAMA.getId(), null, "http://stored-host:11434", ENVIRONMENT);

        assertThat(aiProviderFacade.getUrl(Provider.OLLAMA.getKey(), ENVIRONMENT))
            .isEqualTo("http://stored-host:11434");
    }

    @Test
    void testGetUrlFallsBackToConfigUrl() {
        applicationProperties.getAi()
            .getProvider()
            .getOllama()
            .setUrl("http://config-host:11434");

        assertThat(aiProviderFacade.getUrl(Provider.OLLAMA.getKey(), ENVIRONMENT))
            .isEqualTo("http://config-host:11434");
    }

    @Test
    void testGetUrlReturnsNullWhenNoStoredOrConfigUrl() {
        assertThat(aiProviderFacade.getUrl(Provider.OLLAMA.getKey(), ENVIRONMENT)).isNull();
    }

    private void configureAnthropic(String model) {
        ApplicationProperties.Ai.Provider provider = applicationProperties.getAi()
            .getProvider();

        provider.getAnthropic()
            .setApiKey("sk-anthropic");
        provider.getChat()
            .getAnthropic()
            .getOptions()
            .setModel(model);
    }

    private void configureOpenAi(String model) {
        ApplicationProperties.Ai.Provider provider = applicationProperties.getAi()
            .getProvider();

        provider.getOpenAi()
            .setApiKey("sk-openai");
        provider.getChat()
            .getOpenAi()
            .getOptions()
            .setModel(model);
    }

    private void configureOpenAiEmbedding(String model) {
        ApplicationProperties.Ai.Provider provider = applicationProperties.getAi()
            .getProvider();

        provider.getOpenAi()
            .setApiKey("sk-openai");
        provider.getEmbedding()
            .getOpenAi()
            .getOptions()
            .setModel(model);
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

    private AiProviderCatalogItemDTO getCatalogItem(Provider provider) {
        return aiProviderFacade.getAiChatProviderCatalog(ENVIRONMENT)
            .stream()
            .filter(catalogItem -> Provider.valueOfKey(catalogItem.key()) == provider)
            .findFirst()
            .orElseThrow();
    }

    private List<String> getCatalogKeys() {
        return aiProviderFacade.getAiChatProviderCatalog(ENVIRONMENT)
            .stream()
            .map(AiProviderCatalogItemDTO::key)
            .toList();
    }

    private Property getSavedProperty(Provider provider) {
        return propertyService.fetchProperty(provider.getKey(), Scope.PLATFORM, null, (long) ENVIRONMENT)
            .orElseThrow();
    }

    private ActionDefinition mockChatAction(List<Option> options) {
        StringProperty modelProperty = mock(StringProperty.class);

        when(modelProperty.getName()).thenReturn("model");
        when(modelProperty.getOptions()).thenReturn(options);

        ActionDefinition chatAction = mock(ActionDefinition.class);

        when(chatAction.getName()).thenReturn("ask");
        doReturn(List.of(modelProperty)).when(chatAction)
            .getProperties();

        return chatAction;
    }

    private ComponentDefinition mockChatComponentDefinition(String name, String icon, List<Option> options) {
        ActionDefinition chatAction = mockChatAction(options);

        ComponentDefinition componentDefinition = mock(ComponentDefinition.class);

        when(componentDefinition.getName()).thenReturn(name);
        when(componentDefinition.getIcon()).thenReturn(icon);
        when(componentDefinition.getVersion()).thenReturn(1);
        when(componentDefinition.getActions()).thenReturn(List.of(chatAction));
        when(componentDefinitionService.getComponentDefinition(name, 1)).thenReturn(componentDefinition);

        return componentDefinition;
    }

    private Option mockOption(String value, String label) {
        Option option = mock(Option.class);

        when(option.getValue()).thenReturn(value);
        when(option.getLabel()).thenReturn(label);

        return option;
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
