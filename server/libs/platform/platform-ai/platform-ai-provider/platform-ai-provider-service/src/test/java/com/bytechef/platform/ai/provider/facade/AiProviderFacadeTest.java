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
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.config.ApplicationProperties;
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
import com.bytechef.platform.configuration.service.PropertyService;
import com.bytechef.platform.connection.aiprovider.AiProviderConnectionSource;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class AiProviderFacadeTest {

    @Nested
    class EmbeddingSupportTest {

        private static final int ENVIRONMENT = 2;

        @Mock
        private ComponentDefinitionService componentDefinitionService;

        @Mock
        private PropertyService propertyService;

        private ApplicationProperties applicationProperties;

        private AiProviderFacadeImpl facade;

        @BeforeEach
        void setUp() {
            applicationProperties = mock(ApplicationProperties.class, RETURNS_DEEP_STUBS);

            AiProviderConnectionSource aiProviderConnectionSource = mock(AiProviderConnectionSource.class);

            lenient().when(aiProviderConnectionSource.getSupportedProviders())
                .thenReturn(List.of());

            facade = new AiProviderFacadeImpl(
                aiProviderConnectionSource, componentDefinitionService, mock(OllamaModelClient.class),
                propertyService, applicationProperties);
        }

        @Test
        void testEmbeddingProvidersSupportEmbeddings() {
            List<ComponentDefinition> componentDefinitions = buildComponentDefinitionsForAllProviders();

            when(componentDefinitionService.getComponentDefinitions()).thenReturn(componentDefinitions);
            when(propertyService.getProperties(
                ArgumentMatchers.anyList(),
                ArgumentMatchers.eq(Scope.PLATFORM),
                ArgumentMatchers.isNull(),
                ArgumentMatchers.eq((long) ENVIRONMENT)))
                    .thenReturn(List.of());

            List<AiProviderDTO> providers = facade.getAiProviders(ENVIRONMENT);

            for (AiProviderDTO provider : providers) {
                Provider matchingProvider = Arrays.stream(Provider.values())
                    .filter(curProvider -> curProvider.getId() == provider.id())
                    .findFirst()
                    .orElseThrow();

                assertThat(provider.supportsEmbeddings())
                    .as("Provider %s supports embeddings iff in Provider.EMBEDDING_PROVIDERS", matchingProvider)
                    .isEqualTo(Provider.EMBEDDING_PROVIDERS.contains(matchingProvider));
            }
        }

        @Test
        void testKeylessProviderConfiguredWithUrlInApplicationPropertiesIsEnabled() {
            when(applicationProperties.getAi()
                .getProvider()
                .getOllama()
                .getUrl()).thenReturn("http://localhost:11434");

            AiProviderDTO ollama = getAiProvider(Provider.OLLAMA);

            assertThat(ollama.enabled()).isTrue();
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
        void testUpdateAiProviderEnabledUpdatesProperty() {
            facade.updateAiProvider(Provider.OLLAMA.getId(), true, ENVIRONMENT);

            verify(propertyService).update(Provider.OLLAMA.getKey(), true, Scope.PLATFORM, null, (long) ENVIRONMENT);
        }

        @Test
        void testUpdateAiProviderKeepsSavedApiKeyWhenNoneIsSent() {
            Property property = new Property();

            property.setValue(Map.of("apiKey", "secret-token", "url", "http://ollama.internal:11434"));

            when(propertyService.fetchProperty(
                Provider.OLLAMA.getKey(), Scope.PLATFORM, null, (long) ENVIRONMENT)).thenReturn(Optional.of(property));

            facade.updateAiProvider(Provider.OLLAMA.getId(), null, "http://ollama.example:11434", ENVIRONMENT);

            verify(propertyService).save(
                Provider.OLLAMA.getKey(), Map.of("apiKey", "secret-token", "url", "http://ollama.example:11434"),
                Scope.PLATFORM, null, (long) ENVIRONMENT);
        }

        @Test
        void testUpdateAiProviderRemovesSavedUrlWhenBlankUrlIsSent() {
            Property property = new Property();

            property.setValue(Map.of("apiKey", "secret-token", "url", "http://ollama.internal:11434"));

            when(propertyService.fetchProperty(
                Provider.OLLAMA.getKey(), Scope.PLATFORM, null, (long) ENVIRONMENT)).thenReturn(Optional.of(property));

            facade.updateAiProvider(Provider.OLLAMA.getId(), null, "", ENVIRONMENT);

            verify(propertyService).save(
                Provider.OLLAMA.getKey(), Map.of("apiKey", "secret-token"), Scope.PLATFORM, null, (long) ENVIRONMENT);
        }

        @Test
        void testUpdateAiProviderRemovesSavedApiKeyWhenBlankApiKeyIsSent() {
            Property property = new Property();

            property.setValue(Map.of("apiKey", "secret-token", "url", "http://ollama.internal:11434"));

            when(propertyService.fetchProperty(
                Provider.OLLAMA.getKey(), Scope.PLATFORM, null, (long) ENVIRONMENT)).thenReturn(Optional.of(property));

            facade.updateAiProvider(Provider.OLLAMA.getId(), "", "http://ollama.internal:11434", ENVIRONMENT);

            verify(propertyService).save(
                Provider.OLLAMA.getKey(), Map.of("url", "http://ollama.internal:11434"), Scope.PLATFORM, null,
                (long) ENVIRONMENT);
        }

        @Test
        void testUpdateAiProviderSavesNewApiKeyAndUrl() {
            when(propertyService.fetchProperty(
                Provider.OLLAMA.getKey(), Scope.PLATFORM, null, (long) ENVIRONMENT)).thenReturn(Optional.empty());

            facade.updateAiProvider(Provider.OLLAMA.getId(), "secret-token", "http://ollama.example:11434",
                ENVIRONMENT);

            verify(propertyService).save(
                Provider.OLLAMA.getKey(), Map.of("apiKey", "secret-token", "url", "http://ollama.example:11434"),
                Scope.PLATFORM, null, (long) ENVIRONMENT);
        }

        private AiProviderDTO getAiProvider(Provider provider) {
            List<ComponentDefinition> componentDefinitions = buildComponentDefinitionsForAllProviders();

            when(componentDefinitionService.getComponentDefinitions()).thenReturn(componentDefinitions);
            when(propertyService.getProperties(
                ArgumentMatchers.anyList(),
                ArgumentMatchers.eq(Scope.PLATFORM),
                ArgumentMatchers.isNull(),
                ArgumentMatchers.eq((long) ENVIRONMENT)))
                    .thenReturn(List.of());

            return facade.getAiProviders(ENVIRONMENT)
                .stream()
                .filter(aiProvider -> aiProvider.id() == provider.getId())
                .findFirst()
                .orElseThrow();
        }

        private List<ComponentDefinition> buildComponentDefinitionsForAllProviders() {
            return Arrays.stream(Provider.values())
                .map(provider -> {
                    ComponentDefinition definition = mock(ComponentDefinition.class);

                    when(definition.getName()).thenReturn(provider.getName());
                    lenient().when(definition.getIcon())
                        .thenReturn(null);
                    lenient().when(definition.getActions())
                        .thenReturn(List.of());

                    return definition;
                })
                .toList();
        }
    }

    @Nested
    class CatalogTest {

        private static final int ENVIRONMENT = 1;

        @Mock
        private ComponentDefinitionService componentDefinitionService;

        @Mock
        private PropertyService propertyService;

        private ApplicationProperties applicationProperties;

        private AiProviderFacadeImpl facade;

        @BeforeEach
        void setUp() {
            applicationProperties = mock(ApplicationProperties.class, RETURNS_DEEP_STUBS);

            AiProviderConnectionSource aiProviderConnectionSource = mock(AiProviderConnectionSource.class);

            lenient().when(aiProviderConnectionSource.getSupportedProviders())
                .thenReturn(List.of());

            facade = new AiProviderFacadeImpl(
                aiProviderConnectionSource, componentDefinitionService, mock(OllamaModelClient.class),
                propertyService, applicationProperties);
        }

        @Test
        void testGetChatProviderCatalogUsesOptionLabel() {
            Option option = mock(Option.class);
            when(option.getValue()).thenReturn("claude-fine-tune");
            when(option.getLabel()).thenReturn("My Claude Fine-Tune");

            StringProperty modelProperty = mock(StringProperty.class);
            when(modelProperty.getName()).thenReturn("model");
            when(modelProperty.getOptions()).thenReturn(List.of(option));

            ActionDefinition chatAction = mock(ActionDefinition.class);
            when(chatAction.getName()).thenReturn("ask");
            doReturn(List.of(modelProperty)).when(chatAction)
                .getProperties();

            ComponentDefinition anthropicDefinition = mock(ComponentDefinition.class);
            when(anthropicDefinition.getName()).thenReturn("anthropic");
            when(anthropicDefinition.getIcon()).thenReturn("anthropic-icon");
            when(anthropicDefinition.getVersion()).thenReturn(1);
            when(anthropicDefinition.getActions()).thenReturn(List.of(chatAction));

            when(componentDefinitionService.getComponentDefinitions()).thenReturn(List.of(anthropicDefinition));
            when(componentDefinitionService.getComponentDefinition("anthropic", 1)).thenReturn(anthropicDefinition);
            when(propertyService.getProperties(
                ArgumentMatchers.anyList(),
                ArgumentMatchers.eq(Scope.PLATFORM),
                ArgumentMatchers.isNull(),
                ArgumentMatchers.eq((long) ENVIRONMENT)))
                    .thenReturn(List.of());

            List<AiProviderCatalogItemDTO> catalog = facade.getAiChatProviderCatalog(ENVIRONMENT);

            AiProviderCatalogItemDTO anthropic = catalog.stream()
                .filter(item -> item.key()
                    .equals("ai.provider.anthropic"))
                .findFirst()
                .orElseThrow();

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

            Property property = mock(Property.class);
            when(property.getKey()).thenReturn("ai.provider.anthropic");
            when(property.isEnabled()).thenReturn(true);

            when(componentDefinitionService.getComponentDefinitions()).thenReturn(List.of(anthropicDefinition));
            when(componentDefinitionService.getComponentDefinition("anthropic", 1))
                .thenThrow(new IllegalStateException("definition failed to load"));
            when(propertyService.getProperties(
                ArgumentMatchers.anyList(),
                ArgumentMatchers.eq(Scope.PLATFORM),
                ArgumentMatchers.isNull(),
                ArgumentMatchers.eq((long) ENVIRONMENT)))
                    .thenReturn(List.of(property));

            List<AiProviderCatalogItemDTO> catalog = facade.getAiChatProviderCatalog(ENVIRONMENT);

            AiProviderCatalogItemDTO anthropic = catalog.stream()
                .filter(item -> item.key()
                    .equals("ai.provider.anthropic"))
                .findFirst()
                .orElseThrow();

            assertThat(anthropic.enabled()).isFalse();
            assertThat(anthropic.models()).isEmpty();
        }

        @Test
        void testGetChatProviderCatalogFallsBackToModelIdWhenNoLabelAnywhere() {
            Option option = mock(Option.class);
            when(option.getValue()).thenReturn("claude-fine-tune");
            when(option.getLabel()).thenReturn(null);

            StringProperty modelProperty = mock(StringProperty.class);
            when(modelProperty.getName()).thenReturn("model");
            when(modelProperty.getOptions()).thenReturn(List.of(option));

            ActionDefinition chatAction = mock(ActionDefinition.class);
            when(chatAction.getName()).thenReturn("ask");
            doReturn(List.of(modelProperty)).when(chatAction)
                .getProperties();

            ComponentDefinition anthropicDefinition = mock(ComponentDefinition.class);
            when(anthropicDefinition.getName()).thenReturn("anthropic");
            when(anthropicDefinition.getIcon()).thenReturn("anthropic-icon");
            when(anthropicDefinition.getVersion()).thenReturn(1);
            when(anthropicDefinition.getActions()).thenReturn(List.of(chatAction));

            when(componentDefinitionService.getComponentDefinitions()).thenReturn(List.of(anthropicDefinition));
            when(componentDefinitionService.getComponentDefinition("anthropic", 1)).thenReturn(anthropicDefinition);
            when(propertyService.getProperties(
                ArgumentMatchers.anyList(),
                ArgumentMatchers.eq(Scope.PLATFORM),
                ArgumentMatchers.isNull(),
                ArgumentMatchers.eq((long) ENVIRONMENT)))
                    .thenReturn(List.of());

            List<AiProviderCatalogItemDTO> catalog = facade.getAiChatProviderCatalog(ENVIRONMENT);

            AiProviderCatalogItemDTO anthropic = catalog.stream()
                .filter(item -> item.key()
                    .equals("ai.provider.anthropic"))
                .findFirst()
                .orElseThrow();

            assertThat(anthropic.models()
                .getFirst()
                .label()).isEqualTo("claude-fine-tune");
        }

        @Test
        void testGetChatProviderCatalogContainsOpenAiAndAnthropic() {
            List<ComponentDefinition> minimalDefinitions = buildMinimalComponentDefinitions();

            when(componentDefinitionService.getComponentDefinitions()).thenReturn(minimalDefinitions);
            when(propertyService.getProperties(
                ArgumentMatchers.anyList(),
                ArgumentMatchers.eq(Scope.PLATFORM),
                ArgumentMatchers.isNull(),
                ArgumentMatchers.eq((long) ENVIRONMENT)))
                    .thenReturn(List.of());

            List<AiProviderCatalogItemDTO> catalog = facade.getAiChatProviderCatalog(ENVIRONMENT);

            List<String> keys = catalog.stream()
                .map(AiProviderCatalogItemDTO::key)
                .toList();

            assertThat(keys).contains("ai.provider.openAi", "ai.provider.anthropic");
        }

        @Test
        void testGetChatProviderCatalogExcludesUnsupportedProviders() {
            List<ComponentDefinition> minimalDefinitions = buildMinimalComponentDefinitions();

            when(componentDefinitionService.getComponentDefinitions()).thenReturn(minimalDefinitions);
            when(propertyService.getProperties(
                ArgumentMatchers.anyList(),
                ArgumentMatchers.eq(Scope.PLATFORM),
                ArgumentMatchers.isNull(),
                ArgumentMatchers.eq((long) ENVIRONMENT)))
                    .thenReturn(List.of());

            List<AiProviderCatalogItemDTO> catalog = facade.getAiChatProviderCatalog(ENVIRONMENT);

            List<String> keys = catalog.stream()
                .map(AiProviderCatalogItemDTO::key)
                .toList();

            assertThat(keys).contains("ai.provider.anthropic", "ai.provider.openAi", "ai.provider.groq")
                .doesNotContain("ai.provider.stability", "ai.provider.huggingFace");
        }

        @Test
        void testGetChatProviderCatalogWithModelOptionsYieldsModelsAndSupportsModelByIdFalse() {
            Option option1 = mock(Option.class);
            when(option1.getValue()).thenReturn("claude-sonnet-4-6");
            when(option1.getLabel()).thenReturn("Claude Sonnet 4.6");

            Option option2 = mock(Option.class);
            when(option2.getValue()).thenReturn("claude-haiku-4-5");
            when(option2.getLabel()).thenReturn("Claude Haiku 4.5");

            StringProperty modelProperty = mock(StringProperty.class);
            when(modelProperty.getName()).thenReturn("model");
            when(modelProperty.getOptions()).thenReturn(List.of(option1, option2));

            ActionDefinition chatAction = mock(ActionDefinition.class);
            when(chatAction.getName()).thenReturn("ask");
            doReturn(List.of(modelProperty)).when(chatAction)
                .getProperties();

            ComponentDefinition anthropicDefinition = mock(ComponentDefinition.class);
            when(anthropicDefinition.getName()).thenReturn("anthropic");
            when(anthropicDefinition.getIcon()).thenReturn("anthropic-icon");
            when(anthropicDefinition.getVersion()).thenReturn(1);
            when(anthropicDefinition.getActions()).thenReturn(List.of(chatAction));

            when(componentDefinitionService.getComponentDefinitions()).thenReturn(List.of(anthropicDefinition));
            when(componentDefinitionService.getComponentDefinition("anthropic", 1)).thenReturn(anthropicDefinition);
            when(propertyService.getProperties(
                ArgumentMatchers.anyList(),
                ArgumentMatchers.eq(Scope.PLATFORM),
                ArgumentMatchers.isNull(),
                ArgumentMatchers.eq((long) ENVIRONMENT)))
                    .thenReturn(List.of());

            List<AiProviderCatalogItemDTO> catalog = facade.getAiChatProviderCatalog(ENVIRONMENT);

            AiProviderCatalogItemDTO anthropic = catalog.stream()
                .filter(item -> item.key()
                    .equals("ai.provider.anthropic"))
                .findFirst()
                .orElseThrow();

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
            StringProperty modelPropertyNoOptions = mock(StringProperty.class);
            when(modelPropertyNoOptions.getName()).thenReturn("model");
            when(modelPropertyNoOptions.getOptions()).thenReturn(List.of());

            ActionDefinition chatAction = mock(ActionDefinition.class);
            when(chatAction.getName()).thenReturn("ask");
            doReturn(List.of(modelPropertyNoOptions)).when(chatAction)
                .getProperties();

            ComponentDefinition groqDefinition = mock(ComponentDefinition.class);
            when(groqDefinition.getName()).thenReturn("groq");
            when(groqDefinition.getIcon()).thenReturn("groq-icon");
            when(groqDefinition.getVersion()).thenReturn(1);
            when(groqDefinition.getActions()).thenReturn(List.of(chatAction));

            when(componentDefinitionService.getComponentDefinitions()).thenReturn(List.of(groqDefinition));
            when(componentDefinitionService.getComponentDefinition("groq", 1)).thenReturn(groqDefinition);
            when(propertyService.getProperties(
                ArgumentMatchers.anyList(),
                ArgumentMatchers.eq(Scope.PLATFORM),
                ArgumentMatchers.isNull(),
                ArgumentMatchers.eq((long) ENVIRONMENT)))
                    .thenReturn(List.of());

            List<AiProviderCatalogItemDTO> catalog = facade.getAiChatProviderCatalog(ENVIRONMENT);

            AiProviderCatalogItemDTO groq = catalog.stream()
                .filter(item -> item.key()
                    .equals("ai.provider.groq"))
                .findFirst()
                .orElseThrow();

            assertThat(groq.models()).isEmpty();
            assertThat(groq.supportsModelById()).isTrue();
        }

        @Test
        void testGetChatProviderCatalogReadsModelOptionsFromFullDefinitionNotListStubs() {
            ComponentDefinition anthropicStub = buildEmptyActionComponentDefinition("anthropic");

            lenient().when(anthropicStub.getVersion())
                .thenReturn(1);

            when(componentDefinitionService.getComponentDefinitions()).thenReturn(List.of(anthropicStub));
            when(propertyService.getProperties(
                ArgumentMatchers.anyList(),
                ArgumentMatchers.eq(Scope.PLATFORM),
                ArgumentMatchers.isNull(),
                ArgumentMatchers.eq((long) ENVIRONMENT)))
                    .thenReturn(List.of());

            Option option = mock(Option.class);
            when(option.getValue()).thenReturn("claude-sonnet-4-6");
            when(option.getLabel()).thenReturn("Claude Sonnet 4.6");

            StringProperty modelProperty = mock(StringProperty.class);
            when(modelProperty.getName()).thenReturn("model");
            when(modelProperty.getOptions()).thenReturn(List.of(option));

            ActionDefinition chatAction = mock(ActionDefinition.class);
            when(chatAction.getName()).thenReturn("ask");
            doReturn(List.of(modelProperty)).when(chatAction)
                .getProperties();

            ComponentDefinition anthropicFullDefinition = mock(ComponentDefinition.class);
            when(anthropicFullDefinition.getActions()).thenReturn(List.of(chatAction));

            lenient().when(componentDefinitionService.getComponentDefinition("anthropic", 1))
                .thenReturn(anthropicFullDefinition);

            List<AiProviderCatalogItemDTO> catalog = facade.getAiChatProviderCatalog(ENVIRONMENT);

            AiProviderCatalogItemDTO anthropic = catalog.stream()
                .filter(item -> item.key()
                    .equals("ai.provider.anthropic"))
                .findFirst()
                .orElseThrow();

            assertThat(anthropic.models()).hasSize(1);
            assertThat(anthropic.models()
                .getFirst()
                .name()).isEqualTo("claude-sonnet-4-6");
            assertThat(anthropic.supportsModelById()).isFalse();
        }

        @Test
        void testGetChatProviderCatalogDtoContainsNoApiKey() {
            List<ComponentDefinition> minimalDefinitions = buildMinimalComponentDefinitions();

            when(componentDefinitionService.getComponentDefinitions()).thenReturn(minimalDefinitions);
            when(propertyService.getProperties(
                ArgumentMatchers.anyList(),
                ArgumentMatchers.eq(Scope.PLATFORM),
                ArgumentMatchers.isNull(),
                ArgumentMatchers.eq((long) ENVIRONMENT)))
                    .thenReturn(List.of());

            List<AiProviderCatalogItemDTO> catalog = facade.getAiChatProviderCatalog(ENVIRONMENT);

            assertThat(catalog).isNotEmpty();
            assertThat(catalog.get(0))
                .isInstanceOf(AiProviderCatalogItemDTO.class);
        }

        @Test
        void testGetAiChatProviderCatalogIncludesVertexGeminiMatchedByGeminiComponent() {
            ComponentDefinition gemini = mockComponentDefinition("gemini", "<svg>gemini</svg>");

            when(componentDefinitionService.getComponentDefinitions()).thenReturn(List.of(gemini));
            when(propertyService.getProperties(
                ArgumentMatchers.anyList(),
                ArgumentMatchers.eq(Scope.PLATFORM),
                ArgumentMatchers.isNull(),
                ArgumentMatchers.eq((long) ENVIRONMENT)))
                    .thenReturn(List.of());

            List<AiProviderCatalogItemDTO> catalog = facade.getAiChatProviderCatalog(ENVIRONMENT);

            assertThat(catalog)
                .extracting(AiProviderCatalogItemDTO::key)
                .contains("ai.provider.vertexGemini");
        }

        @Test
        void testGetAiProviderCatalogMarksChatProviderActiveWhenConfiguredViaApplicationProperties() {
            List<ComponentDefinition> minimalDefinitions = buildMinimalComponentDefinitions();

            when(componentDefinitionService.getComponentDefinitions()).thenReturn(minimalDefinitions);
            when(propertyService.getProperties(
                ArgumentMatchers.anyList(),
                ArgumentMatchers.eq(Scope.PLATFORM),
                ArgumentMatchers.isNull(),
                ArgumentMatchers.eq((long) ENVIRONMENT)))
                    .thenReturn(List.of());

            when(applicationProperties.getAi()
                .getProvider()
                .getOpenAi()
                .getApiKey()).thenReturn("sk-config");

            List<AiProviderCatalogItemDTO> catalog = facade.getAiChatProviderCatalog(ENVIRONMENT);

            AiProviderCatalogItemDTO openAi = catalog.stream()
                .filter(item -> item.key()
                    .equals("ai.provider.openAi"))
                .findFirst()
                .orElseThrow();

            assertThat(openAi.enabled()).isTrue();

            AiProviderCatalogItemDTO anthropic = catalog.stream()
                .filter(item -> item.key()
                    .equals("ai.provider.anthropic"))
                .findFirst()
                .orElseThrow();

            assertThat(anthropic.enabled()).isFalse();
        }

        @Test
        void testCatalogEnabledViaDelegatedSource() {
            AiProviderConnectionSource delegatingSource = mock(AiProviderConnectionSource.class);

            when(delegatingSource.getSupportedProviders()).thenReturn(List.of(Provider.OPEN_AI));
            when(delegatingSource.isEnabled(Provider.OPEN_AI, ENVIRONMENT)).thenReturn(true);

            AiProviderFacadeImpl delegatingFacade = new AiProviderFacadeImpl(
                delegatingSource, componentDefinitionService, mock(OllamaModelClient.class),
                propertyService, applicationProperties);

            ComponentDefinition openAiDefinition = buildEmptyActionComponentDefinition("openAi");

            when(componentDefinitionService.getComponentDefinitions()).thenReturn(List.of(openAiDefinition));
            when(propertyService.getProperties(
                ArgumentMatchers.anyList(),
                ArgumentMatchers.eq(Scope.PLATFORM),
                ArgumentMatchers.isNull(),
                ArgumentMatchers.eq((long) ENVIRONMENT)))
                    .thenReturn(List.of());

            List<AiProviderCatalogItemDTO> catalog = delegatingFacade.getAiChatProviderCatalog(ENVIRONMENT);

            AiProviderCatalogItemDTO openAi = catalog.stream()
                .filter(item -> item.key()
                    .equals(Provider.OPEN_AI.getKey()))
                .findFirst()
                .orElseThrow();

            assertThat(openAi.enabled()).isTrue();
        }

        private List<ComponentDefinition> buildMinimalComponentDefinitions() {
            return List.of(
                buildEmptyActionComponentDefinition("anthropic"),
                buildEmptyActionComponentDefinition("openAi"),
                buildEmptyActionComponentDefinition("groq"),
                buildEmptyActionComponentDefinition("stability"));
        }

        private ComponentDefinition buildEmptyActionComponentDefinition(String componentName) {
            ComponentDefinition definition = mock(ComponentDefinition.class);

            when(definition.getName()).thenReturn(componentName);
            lenient().when(definition.getIcon())
                .thenReturn(null);
            lenient().when(definition.getActions())
                .thenReturn(List.of());

            lenient().when(componentDefinitionService.getComponentDefinition(componentName, 0))
                .thenReturn(definition);

            return definition;
        }

        private ComponentDefinition mockComponentDefinition(String name, String icon) {
            ComponentDefinition definition = mock(ComponentDefinition.class);

            when(definition.getName()).thenReturn(name);
            lenient().when(definition.getIcon())
                .thenReturn(icon);
            lenient().when(definition.getActions())
                .thenReturn(List.of());

            return definition;
        }
    }

    @Nested
    class DefaultModelTest {

        private static final int ENVIRONMENT = 1;

        @Mock
        private ComponentDefinitionService componentDefinitionService;

        @Mock
        private PropertyService propertyService;

        private ApplicationProperties applicationProperties;

        private AiProviderFacadeImpl facade;

        @BeforeEach
        void setUp() {
            applicationProperties = mock(ApplicationProperties.class, RETURNS_DEEP_STUBS);

            AiProviderConnectionSource aiProviderConnectionSource = mock(AiProviderConnectionSource.class);

            lenient().when(aiProviderConnectionSource.getSupportedProviders())
                .thenReturn(List.of());

            facade = new AiProviderFacadeImpl(
                aiProviderConnectionSource, componentDefinitionService, mock(OllamaModelClient.class),
                propertyService, applicationProperties);
        }

        @Test
        void testResolvesPreferredProviderByShortName() {
            stubProviderComponentsWithNoStoredProperties();

            when(applicationProperties.getAi()
                .getProvider()
                .getAnthropic()
                .getApiKey()).thenReturn("sk-anthropic");
            when(applicationProperties.getAi()
                .getProvider()
                .getChat()
                .getAnthropic()
                .getOptions()
                .getModel()).thenReturn("claude-sonnet-4-6");

            AiDefaultModelDTO result = facade.getAiDefaultChatModel("anthropic", ENVIRONMENT);

            assertThat(result).isNotNull();
            assertThat(result.provider()).isEqualTo("ai.provider.anthropic");
            assertThat(result.model()).isEqualTo("claude-sonnet-4-6");
        }

        @Test
        void testReturnsAnthropicWhenAnthropicApiKeyConfigured() {
            stubProviderComponentsWithNoStoredProperties();

            when(applicationProperties.getAi()
                .getProvider()
                .getAnthropic()
                .getApiKey()).thenReturn("sk-anthropic");
            when(applicationProperties.getAi()
                .getProvider()
                .getChat()
                .getAnthropic()
                .getOptions()
                .getModel()).thenReturn("claude-sonnet-4-6");

            AiDefaultModelDTO result = facade.getAiDefaultChatModel(ENVIRONMENT);

            assertThat(result).isNotNull();
            assertThat(result.provider()).isEqualTo("ai.provider.anthropic");
            assertThat(result.model()).isEqualTo("claude-sonnet-4-6");
        }

        @Test
        void testFallsBackToOpenAiWhenOnlyOpenAiApiKeyConfigured() {
            stubProviderComponentsWithNoStoredProperties();

            when(applicationProperties.getAi()
                .getProvider()
                .getOpenAi()
                .getApiKey()).thenReturn("sk-openai");
            when(applicationProperties.getAi()
                .getProvider()
                .getChat()
                .getOpenAi()
                .getOptions()
                .getModel()).thenReturn("gpt-5.1");

            AiDefaultModelDTO result = facade.getAiDefaultChatModel(ENVIRONMENT);

            assertThat(result).isNotNull();
            assertThat(result.provider()).isEqualTo("ai.provider.openAi");
            assertThat(result.model()).isEqualTo("gpt-5.1");
        }

        @Test
        void testReturnsNullWhenNoApiKeyConfigured() {
            stubProviderComponentsWithNoStoredProperties();

            AiDefaultModelDTO result = facade.getAiDefaultChatModel(ENVIRONMENT);

            assertThat(result).isNull();
        }

        @Test
        void testReturnsNullWhenResolvedProviderHasNoConfiguredModel() {
            stubProviderComponentsWithNoStoredProperties();

            when(applicationProperties.getAi()
                .getProvider()
                .getAnthropic()
                .getApiKey()).thenReturn("sk-anthropic");
            when(applicationProperties.getAi()
                .getProvider()
                .getChat()
                .getAnthropic()
                .getOptions()
                .getModel()).thenReturn("  ");

            AiDefaultModelDTO result = facade.getAiDefaultChatModel(ENVIRONMENT);

            assertThat(result).isNull();
        }

        @Test
        void testReturnsOpenAiEmbeddingModelWhenEnabled() {
            stubProviderComponentsWithNoStoredProperties();

            when(applicationProperties.getAi()
                .getProvider()
                .getOpenAi()
                .getApiKey()).thenReturn("sk-openai");
            when(applicationProperties.getAi()
                .getProvider()
                .getEmbedding()
                .getOpenAi()
                .getOptions()
                .getModel()).thenReturn("text-embedding-3-small");

            AiDefaultModelDTO result = facade.getAiDefaultEmbeddingModel(ENVIRONMENT);

            assertThat(result).isNotNull();
            assertThat(result.provider()).isEqualTo("ai.provider.openAi");
            assertThat(result.model()).isEqualTo("text-embedding-3-small");
        }

        @Test
        void testReturnsNullEmbeddingModelWhenNoProviderEnabled() {
            stubProviderComponentsWithNoStoredProperties();

            AiDefaultModelDTO result = facade.getAiDefaultEmbeddingModel(ENVIRONMENT);

            assertThat(result).isNull();
        }

        @Test
        void testGetApiKeyReturnsStoredKeyWhenEnabled() {
            Property property = mock(Property.class);

            when(property.isEnabled()).thenReturn(true);
            when(property.get("apiKey")).thenReturn("sk-stored");
            when(propertyService.fetchProperty("ai.provider.openAi", Scope.PLATFORM, null, (long) ENVIRONMENT))
                .thenReturn(Optional.of(property));

            String apiKey = facade.getApiKey("ai.provider.openAi", ENVIRONMENT);

            assertThat(apiKey).isEqualTo("sk-stored");
        }

        @Test
        void testGetApiKeyFallsBackToConfigKeyWhenNoStoredProperty() {
            when(propertyService.fetchProperty("ai.provider.openAi", Scope.PLATFORM, null, (long) ENVIRONMENT))
                .thenReturn(Optional.empty());
            when(applicationProperties.getAi()
                .getProvider()
                .getOpenAi()
                .getApiKey()).thenReturn("sk-openai");

            String apiKey = facade.getApiKey("ai.provider.openAi", ENVIRONMENT);

            assertThat(apiKey).isEqualTo("sk-openai");
        }

        @Test
        void testReturnsChatModelApiKeyWhenActivated() {
            stubProviderComponentsWithNoStoredProperties();

            when(applicationProperties.getAi()
                .getProvider()
                .getAnthropic()
                .getApiKey()).thenReturn("sk-anthropic");
            when(applicationProperties.getAi()
                .getProvider()
                .getChat()
                .getAnthropic()
                .getOptions()
                .getModel()).thenReturn("claude-sonnet-4-6");
            when(propertyService.fetchProperty("ai.provider.anthropic", Scope.PLATFORM, null, (long) ENVIRONMENT))
                .thenReturn(Optional.empty());

            AiDefaultModelWithApiKeyDTO result = facade.getAiDefaultChatModelApiKey(ENVIRONMENT);

            assertThat(result).isNotNull();
            assertThat(result.provider()).isEqualTo(Provider.ANTHROPIC);
            assertThat(result.model()).isEqualTo("claude-sonnet-4-6");
            assertThat(result.apiKey()).isEqualTo("sk-anthropic");
        }

        @Test
        void testReturnsNullChatModelApiKeyWhenNoProviderActivated() {
            stubProviderComponentsWithNoStoredProperties();

            assertThat(facade.getAiDefaultChatModelApiKey(ENVIRONMENT)).isNull();
        }

        @Test
        void testReturnsEmbeddingModelApiKeyWhenActivated() {
            stubProviderComponentsWithNoStoredProperties();

            when(applicationProperties.getAi()
                .getProvider()
                .getOpenAi()
                .getApiKey()).thenReturn("sk-openai");
            when(applicationProperties.getAi()
                .getProvider()
                .getEmbedding()
                .getOpenAi()
                .getOptions()
                .getModel()).thenReturn("text-embedding-3-small");
            when(propertyService.fetchProperty("ai.provider.openAi", Scope.PLATFORM, null, (long) ENVIRONMENT))
                .thenReturn(Optional.empty());

            AiDefaultModelWithApiKeyDTO result = facade.getAiDefaultEmbeddingModelApiKey(ENVIRONMENT);

            assertThat(result).isNotNull();
            assertThat(result.provider()).isEqualTo(Provider.OPEN_AI);
            assertThat(result.model()).isEqualTo("text-embedding-3-small");
            assertThat(result.apiKey()).isEqualTo("sk-openai");
        }

        private void stubProviderComponentsWithNoStoredProperties() {
            ComponentDefinition anthropicDefinition = mockComponentDefinition("anthropic");
            ComponentDefinition openAiDefinition = mockComponentDefinition("openAi");

            when(componentDefinitionService.getComponentDefinitions())
                .thenReturn(List.of(anthropicDefinition, openAiDefinition));
            when(propertyService.getProperties(
                ArgumentMatchers.anyList(),
                ArgumentMatchers.eq(Scope.PLATFORM),
                ArgumentMatchers.isNull(),
                ArgumentMatchers.eq((long) ENVIRONMENT)))
                    .thenReturn(List.of());
        }

        private ComponentDefinition mockComponentDefinition(String componentName) {
            ComponentDefinition definition = mock(ComponentDefinition.class);

            when(definition.getName()).thenReturn(componentName);
            lenient().when(definition.getIcon())
                .thenReturn(null);

            return definition;
        }
    }

    @Nested
    class ProvidersTest {

        private static final int ENVIRONMENT = 1;

        @Mock
        private ComponentDefinitionService componentDefinitionService;

        @Mock
        private PropertyService propertyService;

        private ApplicationProperties applicationProperties;

        private AiProviderFacadeImpl facade;

        @BeforeEach
        void setUp() {
            applicationProperties = mock(ApplicationProperties.class, RETURNS_DEEP_STUBS);

            AiProviderConnectionSource aiProviderConnectionSource = mock(AiProviderConnectionSource.class);

            lenient().when(aiProviderConnectionSource.getSupportedProviders())
                .thenReturn(List.of());

            facade = new AiProviderFacadeImpl(
                aiProviderConnectionSource, componentDefinitionService, mock(OllamaModelClient.class),
                propertyService, applicationProperties);
        }

        @Test
        void testGetAiProvidersFallsBackToConfigApiKeyWhenNoStoredProperty() {
            ComponentDefinition openAiDefinition = mockComponentDefinition("openAi");

            when(componentDefinitionService.getComponentDefinitions())
                .thenReturn(List.of(openAiDefinition));
            when(propertyService.getProperties(
                ArgumentMatchers.anyList(),
                ArgumentMatchers.eq(Scope.PLATFORM),
                ArgumentMatchers.isNull(),
                ArgumentMatchers.eq((long) ENVIRONMENT)))
                    .thenReturn(List.of());

            when(applicationProperties.getAi()
                .getProvider()
                .getOpenAi()
                .getApiKey()).thenReturn("sk-config");

            AiProviderDTO openAi = findProvider(Provider.OPEN_AI);

            assertThat(openAi.apiKey()).isEqualTo("sk-config");
            assertThat(openAi.enabled()).isTrue();
        }

        @Test
        void testGetAiProvidersPrefersStoredPropertyApiKeyOverConfig() {
            Property property = mock(Property.class);

            when(property.getKey()).thenReturn(Provider.OPEN_AI.getKey());
            when(property.get("apiKey")).thenReturn("sk-stored");
            lenient().when(property.isEnabled())
                .thenReturn(true);

            ComponentDefinition openAiDefinition = mockComponentDefinition("openAi");

            when(componentDefinitionService.getComponentDefinitions())
                .thenReturn(List.of(openAiDefinition));
            when(propertyService.getProperties(
                ArgumentMatchers.anyList(),
                ArgumentMatchers.eq(Scope.PLATFORM),
                ArgumentMatchers.isNull(),
                ArgumentMatchers.eq((long) ENVIRONMENT)))
                    .thenReturn(List.of(property));

            AiProviderDTO openAi = findProvider(Provider.OPEN_AI);

            assertThat(openAi.apiKey()).isEqualTo("sk-stored");
            assertThat(openAi.enabled()).isTrue();
        }

        @Test
        void testGetAiProvidersLeavesProviderDisabledWithoutStoredOrConfigKey() {
            ComponentDefinition openAiDefinition = mockComponentDefinition("openAi");

            when(componentDefinitionService.getComponentDefinitions())
                .thenReturn(List.of(openAiDefinition));
            when(propertyService.getProperties(
                ArgumentMatchers.anyList(),
                ArgumentMatchers.eq(Scope.PLATFORM),
                ArgumentMatchers.isNull(),
                ArgumentMatchers.eq((long) ENVIRONMENT)))
                    .thenReturn(List.of());

            AiProviderDTO openAi = findProvider(Provider.OPEN_AI);

            assertThat(openAi.apiKey()).isNull();
            assertThat(openAi.enabled()).isFalse();
        }

        @Test
        void testGetUrlPrefersStoredUrlOverConfig() {
            Property property = mock(Property.class);

            when(property.isEnabled()).thenReturn(true);
            when(property.get("url")).thenReturn("http://stored-host:11434");

            when(propertyService.fetchProperty(
                ArgumentMatchers.eq(Provider.OLLAMA.getKey()),
                ArgumentMatchers.eq(Scope.PLATFORM),
                ArgumentMatchers.isNull(),
                ArgumentMatchers.eq((long) ENVIRONMENT)))
                    .thenReturn(Optional.of(property));

            assertThat(facade.getUrl(Provider.OLLAMA.getKey(), ENVIRONMENT)).isEqualTo("http://stored-host:11434");
        }

        @Test
        void testGetUrlFallsBackToConfigUrl() {
            when(propertyService.fetchProperty(
                ArgumentMatchers.eq(Provider.OLLAMA.getKey()),
                ArgumentMatchers.eq(Scope.PLATFORM),
                ArgumentMatchers.isNull(),
                ArgumentMatchers.eq((long) ENVIRONMENT)))
                    .thenReturn(Optional.empty());

            when(applicationProperties.getAi()
                .getProvider()
                .getOllama()
                .getUrl()).thenReturn("http://config-host:11434");

            assertThat(facade.getUrl(Provider.OLLAMA.getKey(), ENVIRONMENT)).isEqualTo("http://config-host:11434");
        }

        @Test
        void testGetUrlReturnsNullWhenNoStoredOrConfigUrl() {
            when(propertyService.fetchProperty(
                ArgumentMatchers.eq(Provider.OLLAMA.getKey()),
                ArgumentMatchers.eq(Scope.PLATFORM),
                ArgumentMatchers.isNull(),
                ArgumentMatchers.eq((long) ENVIRONMENT)))
                    .thenReturn(Optional.empty());

            assertThat(facade.getUrl(Provider.OLLAMA.getKey(), ENVIRONMENT)).isNull();
        }

        private AiProviderDTO findProvider(Provider provider) {
            return facade.getAiProviders(ENVIRONMENT)
                .stream()
                .filter(aiProviderDTO -> aiProviderDTO.id() == provider.getId())
                .findFirst()
                .orElseThrow();
        }

        private ComponentDefinition mockComponentDefinition(String componentName) {
            ComponentDefinition definition = mock(ComponentDefinition.class);

            when(definition.getName()).thenReturn(componentName);
            lenient().when(definition.getIcon())
                .thenReturn(null);

            return definition;
        }
    }
}
