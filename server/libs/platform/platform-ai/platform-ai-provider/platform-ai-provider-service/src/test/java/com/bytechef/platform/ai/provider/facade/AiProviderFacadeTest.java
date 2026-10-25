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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.ai.llm.Provider;
import com.bytechef.platform.ai.provider.dto.AiProviderDTO;
import com.bytechef.platform.ai.provider.ollama.OllamaModelLister;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.domain.Property;
import com.bytechef.platform.configuration.domain.Property.Scope;
import com.bytechef.platform.configuration.service.PropertyService;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
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

        facade = new AiProviderFacadeImpl(
            componentDefinitionService, mock(OllamaModelLister.class), propertyService, applicationProperties);
    }

    @Test
    void testUpdateAiProviderEnabledUpdatesProperty() {
        facade.updateAiProvider(Provider.OLLAMA.getId(), true, ENVIRONMENT);

        verify(propertyService).update(Provider.OLLAMA.getKey(), true, Scope.PLATFORM, null, (long) ENVIRONMENT);
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

        facade.updateAiProvider(Provider.OLLAMA.getId(), "secret-token", "http://ollama.example:11434", ENVIRONMENT);

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
