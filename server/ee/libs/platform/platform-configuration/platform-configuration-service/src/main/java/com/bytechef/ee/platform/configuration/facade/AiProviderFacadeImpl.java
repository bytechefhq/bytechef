/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.configuration.facade;

import com.bytechef.component.ai.llm.Provider;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.platform.configuration.dto.AiDefaultModelDTO;
import com.bytechef.ee.platform.configuration.dto.AiProviderCatalogItemDTO;
import com.bytechef.ee.platform.configuration.dto.AiProviderDTO;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.component.domain.ActionDefinition;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.domain.StringProperty;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.domain.Property;
import com.bytechef.platform.configuration.domain.Property.Scope;
import com.bytechef.platform.configuration.service.PropertyService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@Transactional
@ConditionalOnEEVersion
public class AiProviderFacadeImpl implements AiProviderFacade {

    private static final Set<Provider> CHAT_PROVIDERS = EnumSet.of(
        Provider.ANTHROPIC,
        Provider.GROQ,
        Provider.MISTRAL,
        Provider.NVIDIA,
        Provider.OPEN_AI,
        Provider.VERTEX_GEMINI,
        Provider.PERPLEXITY,
        Provider.DEEPSEEK);

    private final ComponentDefinitionService componentDefinitionService;
    private final PropertyService propertyService;
    private final ApplicationProperties applicationProperties;

    @SuppressFBWarnings("EI")
    public AiProviderFacadeImpl(
        ComponentDefinitionService componentDefinitionService, PropertyService propertyService,
        ApplicationProperties applicationProperties) {

        this.componentDefinitionService = componentDefinitionService;
        this.propertyService = propertyService;
        this.applicationProperties = applicationProperties;
    }

    @Override
    public void deleteAiProvider(int id, int environment) {
        Provider provider = getProvider(id);

        propertyService.delete(provider.getKey(), Scope.PLATFORM, null, (long) environment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiProviderCatalogItemDTO> getAiProviderCatalog(int environment) {
        List<ComponentDefinition> componentDefinitions = componentDefinitionService.getComponentDefinitions();

        List<Property> properties = propertyService.getProperties(
            CHAT_PROVIDERS.stream()
                .map(Provider::getKey)
                .toList(),
            Scope.PLATFORM, null, (long) environment);

        return CHAT_PROVIDERS.stream()
            .map(provider -> {
                String providerName = provider.getName()
                    .toLowerCase();

                ComponentDefinition componentDefinition = componentDefinitions.stream()
                    .filter(curComponentDefinition -> providerName.contains(
                        curComponentDefinition.getName()
                            .toLowerCase()))
                    .max(Comparator.comparingInt(curComponentDefinition -> curComponentDefinition.getName()
                        .length()))
                    .orElse(null);

                if (componentDefinition == null) {
                    return null;
                }

                Property property = properties.stream()
                    .filter(curProperty -> curProperty.getKey()
                        .equals(provider.getKey()))
                    .findFirst()
                    .orElse(null);

                boolean enabled = (property != null && property.isEnabled()) || hasConfigApiKey(provider);

                List<AiProviderCatalogItemDTO.Model> models = readChatModels(componentDefinition);

                return new AiProviderCatalogItemDTO(
                    provider.getKey(), provider.getLabel(), componentDefinition.getIcon(), enabled,
                    models.isEmpty(), models);
            })
            .filter(Objects::nonNull)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AiDefaultModelDTO getAiDefaultModel() {
        Provider provider;

        if (hasConfigApiKey(Provider.ANTHROPIC)) {
            provider = Provider.ANTHROPIC;
        } else if (hasConfigApiKey(Provider.OPEN_AI)) {
            provider = Provider.OPEN_AI;
        } else {
            return null;
        }

        String model = resolveDefaultModel(provider);

        if (model == null || model.isBlank()) {
            return null;
        }

        return new AiDefaultModelDTO(provider.getKey(), model);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiProviderDTO> getAiProviders(int environment) {
        List<ComponentDefinition> componentDefinitions = componentDefinitionService.getComponentDefinitions();

        List<Property> properties = propertyService.getProperties(
            Arrays.stream(Provider.values())
                .map(Provider::getKey)
                .toList(),
            Scope.PLATFORM, null, (long) environment);

        return Arrays.stream(Provider.values())
            .map(provider -> {
                ComponentDefinition componentDefinition = componentDefinitions.stream()
                    .filter(curComponentDefinition -> {
                        String name = provider.getName();

                        return name.contains(curComponentDefinition.getName());
                    })
                    .findFirst()
                    .orElse(null);
//                    .orElseThrow(
//                        () -> new IllegalStateException(
//                            "Component definition not found for provider: " + provider.getKey()));

                if (componentDefinition == null) {
                    return null;
                }

                Property property = properties.stream()
                    .filter(curProperty -> curProperty.getKey()
                        .equals(provider.getKey()))
                    .findFirst()
                    .orElse(null);

                String apiKey = property != null ? (String) property.get("apiKey") : null;
                boolean enabled = property != null && property.isEnabled();

                return new AiProviderDTO(
                    provider.getId(), provider.getLabel(), componentDefinition.getIcon(), apiKey, enabled,
                    provider.isEmbeddingSupported());
            })
            .filter(Objects::nonNull)
            .toList();
    }

    @Override
    public void updateAiProvider(int id, boolean enabled, int environment) {
        Provider provider = getProvider(id);

        propertyService.update(provider.getKey(), enabled, Scope.PLATFORM, null, (long) environment);
    }

    @Override
    public void updateAiProvider(int id, String apiKey, int environment) {
        Provider provider = getProvider(id);

        propertyService.save(provider.getKey(), Map.of("apiKey", apiKey), Scope.PLATFORM, null, (long) environment);
    }

    private static List<AiProviderCatalogItemDTO.Model> extractModelOptions(ActionDefinition actionDefinition) {
        return actionDefinition.getProperties()
            .stream()
            .filter(property -> "model".equals(property.getName()) && property instanceof StringProperty)
            .map(property -> (StringProperty) property)
            .findFirst()
            .map(stringProperty -> stringProperty.getOptions()
                .stream()
                .map(option -> new AiProviderCatalogItemDTO.Model(
                    String.valueOf(option.getValue()), option.getLabel()))
                .toList())
            .orElse(List.of());
    }

    private static Provider getProvider(int id) {
        return Arrays.stream(Provider.values())
            .filter(curProvider -> curProvider.getId() == id)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Provider not found for id: " + id));
    }

    private static List<AiProviderCatalogItemDTO.Model> readChatModels(ComponentDefinition componentDefinition) {
        return componentDefinition.getActions()
            .stream()
            .filter(actionDefinition -> "ask".equals(actionDefinition.getName()))
            .findFirst()
            .map(AiProviderFacadeImpl::extractModelOptions)
            .orElse(List.of());
    }

    private String resolveDefaultModel(Provider provider) {
        ApplicationProperties.Ai.Provider.Chat chat = applicationProperties.getAi()
            .getProvider()
            .getChat();

        return switch (provider) {
            case OPEN_AI -> chat.getOpenAi()
                .getOptions()
                .getModel();
            case ANTHROPIC -> chat.getAnthropic()
                .getOptions()
                .getModel();
            default -> null;
        };
    }

    private boolean hasConfigApiKey(Provider provider) {
        ApplicationProperties.Ai.Provider configProvider = applicationProperties.getAi()
            .getProvider();

        String apiKey = switch (provider) {
            case OPEN_AI -> configProvider.getOpenAi()
                .getApiKey();
            case ANTHROPIC -> configProvider.getAnthropic()
                .getApiKey();
            case MISTRAL -> configProvider.getMistral()
                .getApiKey();
            case VERTEX_GEMINI -> configProvider.getVertexGemini()
                .getApiKey();
            case GROQ -> configProvider.getGroq()
                .getApiKey();
            case PERPLEXITY -> configProvider.getPerplexity()
                .getApiKey();
            case NVIDIA -> configProvider.getNvidia()
                .getApiKey();
            case DEEPSEEK -> configProvider.getDeepSeek()
                .getApiKey();
            default -> null;
        };

        return apiKey != null && !apiKey.isBlank();
    }
}
