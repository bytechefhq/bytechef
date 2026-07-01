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

import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.platform.configuration.dto.AiDefaultModelDTO;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.service.ComponentDefinitionService;
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
class AiProviderFacadeDefaultModelTest {

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

        // anthropic api-key defaults to null via deep stubs → disabled → filtered out, leaving openai.
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
