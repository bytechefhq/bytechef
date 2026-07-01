/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.ai.llm.Provider;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.platform.configuration.dto.AiProviderDTO;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.domain.Property;
import com.bytechef.platform.configuration.domain.Property.Scope;
import com.bytechef.platform.configuration.service.PropertyService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @version ee
 *
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
