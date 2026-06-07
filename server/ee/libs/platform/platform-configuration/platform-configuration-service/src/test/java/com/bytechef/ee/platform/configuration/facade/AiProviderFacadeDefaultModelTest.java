/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.platform.configuration.dto.AiDefaultModelDTO;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.service.PropertyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class AiProviderFacadeDefaultModelTest {

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

        AiDefaultModelDTO result = facade.getAiDefaultModel();

        assertThat(result).isNotNull();
        assertThat(result.provider()).isEqualTo("ai.provider.anthropic");
        assertThat(result.model()).isEqualTo("claude-sonnet-4-6");
    }

    @Test
    void testFallsBackToOpenAiWhenOnlyOpenAiApiKeyConfigured() {
        // anthropic api-key defaults to null via deep stubs → falls through to openai.
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

        AiDefaultModelDTO result = facade.getAiDefaultModel();

        assertThat(result).isNotNull();
        assertThat(result.provider()).isEqualTo("ai.provider.openAi");
        assertThat(result.model()).isEqualTo("gpt-5.1");
    }

    @Test
    void testReturnsNullWhenNoApiKeyConfigured() {
        AiDefaultModelDTO result = facade.getAiDefaultModel();

        assertThat(result).isNull();
    }

    @Test
    void testReturnsNullWhenResolvedProviderHasNoConfiguredModel() {
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

        AiDefaultModelDTO result = facade.getAiDefaultModel();

        assertThat(result).isNull();
    }
}
