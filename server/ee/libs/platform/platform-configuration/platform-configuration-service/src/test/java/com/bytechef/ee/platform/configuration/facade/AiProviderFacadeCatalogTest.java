/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.platform.configuration.dto.AiProviderCatalogItemDTO;
import com.bytechef.platform.component.domain.ActionDefinition;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.domain.Option;
import com.bytechef.platform.component.domain.StringProperty;
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
class AiProviderFacadeCatalogTest {

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
    void testGetChatProviderCatalogContainsOpenAiAndAnthropic() {
        List<ComponentDefinition> minimalDefinitions = buildMinimalComponentDefinitions();

        when(componentDefinitionService.getComponentDefinitions()).thenReturn(minimalDefinitions);
        when(propertyService.getProperties(
            ArgumentMatchers.anyList(),
            ArgumentMatchers.eq(Scope.PLATFORM),
            ArgumentMatchers.isNull(),
            ArgumentMatchers.eq((long) ENVIRONMENT)))
                .thenReturn(List.of());

        List<AiProviderCatalogItemDTO> catalog = facade.getAiProviderCatalog(ENVIRONMENT);

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

        List<AiProviderCatalogItemDTO> catalog = facade.getAiProviderCatalog(ENVIRONMENT);

        List<String> keys = catalog.stream()
            .map(AiProviderCatalogItemDTO::key)
            .toList();

        // STABILITY is image-only; HUGGING_FACE is no longer supported by Spring AI; AZURE_OPEN_AI needs an
        // endpoint the catalog doesn't store.
        assertThat(keys).doesNotContain(
            "ai.provider.stability", "ai.provider.huggingFace", "ai.provider.azureOpenAi");
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
        when(anthropicDefinition.getActions()).thenReturn(List.of(chatAction));

        when(componentDefinitionService.getComponentDefinitions()).thenReturn(List.of(anthropicDefinition));
        when(propertyService.getProperties(
            ArgumentMatchers.anyList(),
            ArgumentMatchers.eq(Scope.PLATFORM),
            ArgumentMatchers.isNull(),
            ArgumentMatchers.eq((long) ENVIRONMENT)))
                .thenReturn(List.of());

        List<AiProviderCatalogItemDTO> catalog = facade.getAiProviderCatalog(ENVIRONMENT);

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
        when(groqDefinition.getActions()).thenReturn(List.of(chatAction));

        when(componentDefinitionService.getComponentDefinitions()).thenReturn(List.of(groqDefinition));
        when(propertyService.getProperties(
            ArgumentMatchers.anyList(),
            ArgumentMatchers.eq(Scope.PLATFORM),
            ArgumentMatchers.isNull(),
            ArgumentMatchers.eq((long) ENVIRONMENT)))
                .thenReturn(List.of());

        List<AiProviderCatalogItemDTO> catalog = facade.getAiProviderCatalog(ENVIRONMENT);

        AiProviderCatalogItemDTO groq = catalog.stream()
            .filter(item -> item.key()
                .equals("ai.provider.groq"))
            .findFirst()
            .orElseThrow();

        assertThat(groq.models()).isEmpty();
        assertThat(groq.supportsModelById()).isTrue();
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

        List<AiProviderCatalogItemDTO> catalog = facade.getAiProviderCatalog(ENVIRONMENT);

        // AiProviderCatalogItemDTO has no apiKey field by design; this compiles only if the record truly lacks it.
        assertThat(catalog).isNotEmpty();
        assertThat(catalog.get(0))
            .isInstanceOf(AiProviderCatalogItemDTO.class);
    }

    @Test
    void testGetAiProviderCatalogIncludesVertexGeminiMatchedByGeminiComponent() {
        ComponentDefinition gemini = mockComponentDefinition("gemini", "<svg>gemini</svg>");

        when(componentDefinitionService.getComponentDefinitions()).thenReturn(List.of(gemini));
        when(propertyService.getProperties(
            ArgumentMatchers.anyList(),
            ArgumentMatchers.eq(Scope.PLATFORM),
            ArgumentMatchers.isNull(),
            ArgumentMatchers.eq((long) ENVIRONMENT)))
                .thenReturn(List.of());

        List<AiProviderCatalogItemDTO> catalog = facade.getAiProviderCatalog(ENVIRONMENT);

        assertThat(catalog)
            .extracting(AiProviderCatalogItemDTO::key)
            .contains("ai.provider.vertexGemini");
    }

    @Test
    void testGetAiProviderCatalogMarksProviderActiveWhenConfiguredViaApplicationProperties() {
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

        List<AiProviderCatalogItemDTO> catalog = facade.getAiProviderCatalog(ENVIRONMENT);

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

    /**
     * Builds a minimal list of ComponentDefinition mocks covering the chat-capable providers that ByteChef ships by
     * default. Each mock has no chat actions, so models will be empty and supportsModelById will be true — the test
     * methods that care about model extraction supply their own full mock.
     */
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
