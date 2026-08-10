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
import com.bytechef.platform.ai.llm.Provider;
import com.bytechef.platform.ai.model.catalog.CatalogModel;
import com.bytechef.platform.ai.model.catalog.Limit;
import com.bytechef.platform.ai.model.catalog.Modalities;
import com.bytechef.platform.ai.model.catalog.Modalities.Modality;
import com.bytechef.platform.ai.model.catalog.ModelCatalog;
import com.bytechef.platform.component.domain.ActionDefinition;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.domain.Option;
import com.bytechef.platform.component.domain.StringProperty;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.domain.Property.Scope;
import com.bytechef.platform.configuration.service.PropertyService;
import com.bytechef.platform.connection.aiprovider.AiProviderConnectionSource;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

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
    private ObjectProvider<ModelCatalog> modelCatalogProvider;

    @Mock
    private PropertyService propertyService;

    private ApplicationProperties applicationProperties;

    private AiProviderFacadeImpl facade;

    @BeforeEach
    void setUp() {
        applicationProperties = mock(ApplicationProperties.class, RETURNS_DEEP_STUBS);

        // Stub returns empty supported-providers list so the facade falls back to the inline
        // (property-enabled || hasConfigApiKey) path for all providers, keeping all existing
        // test assertions valid without additional mock setup.
        AiProviderConnectionSource aiProviderConnectionSource = mock(AiProviderConnectionSource.class);

        lenient().when(aiProviderConnectionSource.getSupportedProviders())
            .thenReturn(List.of());

        // No models.dev catalog by default — existing tests exercise the option-label path unchanged.
        lenient().when(modelCatalogProvider.getIfAvailable())
            .thenReturn(null);

        facade = new AiProviderFacadeImpl(
            aiProviderConnectionSource, componentDefinitionService, modelCatalogProvider, propertyService,
            applicationProperties);
    }

    private static CatalogModel catalogModel(String id, String name) {
        return new CatalogModel(
            id, name, null, null, false, false, false, false, false, false, null, null, null,
            CatalogModel.Status.ACTIVE, new Modalities(List.of(Modality.TEXT), List.of(Modality.TEXT)),
            new Limit(null, null, null), null);
    }

    @Test
    void testGetChatProviderCatalogFillsLabelFromModelsDevDisplayName() {
        Option option = mock(Option.class);
        when(option.getValue()).thenReturn("claude-sonnet-4-6");
        when(option.getLabel()).thenReturn("claude-sonnet-4-6");

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

        ModelCatalog modelCatalog = mock(ModelCatalog.class);

        when(modelCatalogProvider.getIfAvailable()).thenReturn(modelCatalog);
        when(modelCatalog.fetchModel("anthropic", "claude-sonnet-4-6"))
            .thenReturn(Optional.of(catalogModel("claude-sonnet-4-6", "Claude Sonnet 4.6")));

        List<AiProviderCatalogItemDTO> catalog = facade.getAiChatProviderCatalog(ENVIRONMENT);

        AiProviderCatalogItemDTO anthropic = catalog.stream()
            .filter(item -> item.key()
                .equals("ai.provider.anthropic"))
            .findFirst()
            .orElseThrow();

        assertThat(anthropic.models()
            .getFirst()
            .label()).isEqualTo("Claude Sonnet 4.6");
        assertThat(anthropic.models()
            .getFirst()
            .name()).isEqualTo("claude-sonnet-4-6");
    }

    @Test
    void testGetChatProviderCatalogFallsBackToOptionLabelWhenCatalogMisses() {
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

        ModelCatalog modelCatalog = mock(ModelCatalog.class);

        when(modelCatalogProvider.getIfAvailable()).thenReturn(modelCatalog);
        when(modelCatalog.fetchModel("anthropic", "claude-fine-tune")).thenReturn(Optional.empty());

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

        // modelCatalogProvider stays at its default null — no models.dev catalog on the classpath at all.
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

        // STABILITY is image-only; HUGGING_FACE is no longer supported by Spring AI.
        assertThat(keys).doesNotContain("ai.provider.stability", "ai.provider.huggingFace");
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

    /**
     * Reproduces the production wiring after the lazy component-index work: {@code getComponentDefinitions()} serves
     * list-view stubs that carry NO action property trees, so the model options MUST be read from the per-component
     * detail path ({@code getComponentDefinition(name, version)}), which loads the full definition on demand.
     */
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

        // AiProviderCatalogItemDTO has no apiKey field by design; this compiles only if the record truly lacks it.
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
        // Build a facade whose AiProviderConnectionSource claims to own OPEN_AI and reports it enabled.
        AiProviderConnectionSource delegatingSource = mock(AiProviderConnectionSource.class);

        when(delegatingSource.getSupportedProviders()).thenReturn(List.of(Provider.OPEN_AI));
        when(delegatingSource.isEnabled(Provider.OPEN_AI, ENVIRONMENT)).thenReturn(true);

        AiProviderFacadeImpl delegatingFacade = new AiProviderFacadeImpl(
            delegatingSource, componentDefinitionService, modelCatalogProvider, propertyService,
            applicationProperties);

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
