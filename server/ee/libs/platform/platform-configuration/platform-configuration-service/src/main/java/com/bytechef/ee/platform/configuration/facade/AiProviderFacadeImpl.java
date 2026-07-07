/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.configuration.facade;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.platform.configuration.dto.AiDefaultModelDTO;
import com.bytechef.ee.platform.configuration.dto.AiDefaultModelWithApiKeyDTO;
import com.bytechef.ee.platform.configuration.dto.AiProviderCatalogItemDTO;
import com.bytechef.ee.platform.configuration.dto.AiProviderDTO;
import com.bytechef.platform.ai.llm.Provider;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
    public List<AiProviderCatalogItemDTO> getAiChatProviderCatalog(int environment) {
        List<ComponentDefinition> componentDefinitions = componentDefinitionService.getComponentDefinitions();

        List<Property> properties = propertyService.getProperties(
            Provider.CHAT_PROVIDERS.stream()
                .map(Provider::getKey)
                .toList(),
            Scope.PLATFORM, null, (long) environment);

        return Provider.CHAT_PROVIDERS.stream()
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

                boolean enabled = property != null ? property.isEnabled() : hasConfigApiKey(provider);

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
    public AiDefaultModelDTO getAiDefaultChatModel(int environmentId) {
        return getAiProviders(environmentId)
            .stream()
            .filter(AiProviderDTO::enabled)
            .map(aiProviderDTO -> getProvider(aiProviderDTO.id()))
            .filter(Provider.CHAT_PROVIDERS::contains)
            .map(provider -> {
                String model = resolveDefaultChatModel(provider);

                if (model == null || model.isBlank()) {
                    return null;
                }

                return new AiDefaultModelDTO(provider.getKey(), model);
            })
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public AiDefaultModelWithApiKeyDTO getAiDefaultChatModelApiKey(int environmentId) {
        return resolveWithApiKey(getAiDefaultChatModel(environmentId), environmentId);
    }

    @Override
    @Transactional(readOnly = true)
    public AiDefaultModelDTO getAiDefaultEmbeddingModel(int environmentId) {
        return getAiProviders(environmentId)
            .stream()
            .filter(AiProviderDTO::enabled)
            .filter(AiProviderDTO::supportsEmbeddings)
            .map(aiProviderDTO -> {
                Provider provider = getProvider(aiProviderDTO.id());

                String model = resolveDefaultEmbeddingModel(provider);

                if (model == null || model.isBlank()) {
                    return null;
                }

                return new AiDefaultModelDTO(provider.getKey(), model);
            })
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public AiDefaultModelWithApiKeyDTO getAiDefaultEmbeddingModelApiKey(int environmentId) {
        return resolveWithApiKey(getAiDefaultEmbeddingModel(environmentId), environmentId);
    }

    @Override
    @Transactional(readOnly = true)
    public String getApiKey(String provider, int environment) {
        return resolveApiKey(Provider.valueOfKey(provider), environment);
    }

    @Override
    @Transactional(readOnly = true)
    public String getUrl(String provider, int environment) {
        return resolveUrl(Provider.valueOfKey(provider), environment);
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
                    .filter(curProperty -> Objects.equals(curProperty.getKey(), provider.getKey()))
                    .findFirst()
                    .orElse(null);

                String apiKey = property != null ? (String) property.get("apiKey") : null;

                if (apiKey == null || apiKey.isBlank()) {
                    apiKey = getConfigApiKey(provider);
                }

                String url = property != null ? (String) property.get("url") : null;

                if (url == null || url.isBlank()) {
                    url = getConfigUrl(provider);
                }

                boolean enabled = property != null ? property.isEnabled() : hasConfigApiKey(provider);

                return new AiProviderDTO(
                    provider.getId(), provider.getLabel(), componentDefinition.getIcon(), apiKey, url, enabled,
                    provider.isChatSupported(), provider.isImageSupported(), provider.isEmbeddingSupported(),
                    isCopilotDocsProvider(provider));
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
    public void updateAiProvider(int id, String apiKey, String url, int environment) {
        Provider provider = getProvider(id);

        Map<String, Object> values = new HashMap<>();

        if (apiKey != null) {
            values.put("apiKey", apiKey);
        }

        if (url != null && !url.isBlank()) {
            values.put("url", url);
        }

        propertyService.save(provider.getKey(), values, Scope.PLATFORM, null, (long) environment);
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

    private boolean isCopilotDocsProvider(Provider provider) {
        ApplicationProperties.Ai.Copilot.Docs.Embedding.Provider docsProvider = applicationProperties.getAi()
            .getCopilot()
            .getDocs()
            .getEmbedding()
            .getProvider();

        if (docsProvider == null || !provider.isEmbeddingSupported()) {
            return false;
        }

        return provider.name()
            .replace("_", "")
            .equalsIgnoreCase(docsProvider.name());
    }

    private static List<AiProviderCatalogItemDTO.Model> readChatModels(ComponentDefinition componentDefinition) {
        return componentDefinition.getActions()
            .stream()
            .filter(actionDefinition -> "ask".equals(actionDefinition.getName()))
            .findFirst()
            .map(AiProviderFacadeImpl::extractModelOptions)
            .orElse(List.of());
    }

    private String resolveDefaultChatModel(Provider provider) {
        ApplicationProperties.Ai.Provider.Chat chat = applicationProperties.getAi()
            .getProvider()
            .getChat();

        return switch (provider) {
            case ANTHROPIC -> chat.getAnthropic()
                .getOptions()
                .getModel();
            case AZURE_OPEN_AI -> chat.getAzureOpenAi()
                .getOptions()
                .getModel();
            case DEEPSEEK -> chat.getDeepSeek()
                .getOptions()
                .getModel();
            case GROQ -> chat.getGroq()
                .getOptions()
                .getModel();
            case MISTRAL -> chat.getMistral()
                .getOptions()
                .getModel();
            case NVIDIA -> chat.getNvidia()
                .getOptions()
                .getModel();
            case OLLAMA -> chat.getOllama()
                .getOptions()
                .getModel();
            case OPEN_AI -> chat.getOpenAi()
                .getOptions()
                .getModel();
            case PERPLEXITY -> chat.getPerplexity()
                .getOptions()
                .getModel();
            case VERTEX_GEMINI -> chat.getVertexGemini()
                .getOptions()
                .getModel();
            default -> null;
        };
    }

    private String resolveDefaultEmbeddingModel(Provider provider) {
        ApplicationProperties.Ai.Provider.Embedding embedding = applicationProperties.getAi()
            .getProvider()
            .getEmbedding();

        return switch (provider) {
            case MISTRAL -> embedding.getMistral()
                .getOptions()
                .getModel();
            case OLLAMA -> embedding.getOllama()
                .getOptions()
                .getModel();
            case OPEN_AI -> embedding.getOpenAi()
                .getOptions()
                .getModel();
            default -> null;
        };
    }

    private String resolveApiKey(Provider provider, int environment) {
        return propertyService.fetchProperty(provider.getKey(), Scope.PLATFORM, null, (long) environment)
            .filter(Property::isEnabled)
            .map(property -> property.get("apiKey"))
            .map(Object::toString)
            .filter(apiKey -> !apiKey.isBlank())
            .orElseGet(() -> getConfigApiKey(provider));
    }

    private String resolveUrl(Provider provider, int environment) {
        return propertyService.fetchProperty(provider.getKey(), Scope.PLATFORM, null, (long) environment)
            .filter(Property::isEnabled)
            .map(property -> property.get("url"))
            .map(Object::toString)
            .filter(url -> !url.isBlank())
            .orElseGet(() -> getConfigUrl(provider));
    }

    private AiDefaultModelWithApiKeyDTO resolveWithApiKey(AiDefaultModelDTO defaultModel, int environment) {
        if (defaultModel == null) {
            return null;
        }

        Provider provider = Provider.valueOfKey(defaultModel.provider());

        String apiKey = resolveApiKey(provider, environment);

        if ((apiKey == null || apiKey.isBlank()) && provider.requiresApiKey()) {
            return null;
        }

        String url = resolveUrl(provider, environment);

        return new AiDefaultModelWithApiKeyDTO(provider, defaultModel.model(), apiKey, url);
    }

    private String getConfigApiKey(Provider provider) {
        ApplicationProperties.Ai.Provider configProvider = applicationProperties.getAi()
            .getProvider();

        return switch (provider) {
            case ANTHROPIC -> configProvider.getAnthropic()
                .getApiKey();
            case DEEPSEEK -> configProvider.getDeepSeek()
                .getApiKey();
            case GROQ -> configProvider.getGroq()
                .getApiKey();
            case OLLAMA -> configProvider.getOllama()
                .getApiKey();
            case OPEN_AI -> configProvider.getOpenAi()
                .getApiKey();
            case MISTRAL -> configProvider.getMistral()
                .getApiKey();
            case NVIDIA -> configProvider.getNvidia()
                .getApiKey();
            case PERPLEXITY -> configProvider.getPerplexity()
                .getApiKey();
            case VERTEX_GEMINI -> configProvider.getVertexGemini()
                .getApiKey();
            default -> null;
        };
    }

    private String getConfigUrl(Provider provider) {
        ApplicationProperties.Ai.Provider configProvider = applicationProperties.getAi()
            .getProvider();

        return switch (provider) {
            case OLLAMA -> configProvider.getOllama()
                .getUrl();
            default -> null;
        };
    }

    private boolean hasConfigApiKey(Provider provider) {
        String apiKey = getConfigApiKey(provider);

        return apiKey != null && !apiKey.isBlank();
    }
}
