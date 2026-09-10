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
import static org.mockito.Mockito.when;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.ai.llm.Provider;
import com.bytechef.platform.ai.provider.dto.AiProviderDTO;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.domain.Property;
import com.bytechef.platform.configuration.domain.Property.Scope;
import com.bytechef.platform.configuration.service.PropertyService;
import java.util.List;
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
class AiProviderFacadeProvidersTest {

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

        facade = new AiProviderFacadeImpl(componentDefinitionService, propertyService, applicationProperties);
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
