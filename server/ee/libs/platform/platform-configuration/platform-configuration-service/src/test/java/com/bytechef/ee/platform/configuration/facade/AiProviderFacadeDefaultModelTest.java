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
import com.bytechef.ee.platform.configuration.dto.AiDefaultModelWithApiKeyDTO;
import com.bytechef.platform.ai.llm.Provider;
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
