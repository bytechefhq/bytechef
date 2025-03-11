/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.configuration.facade;

import com.bytechef.component.ai.llm.Provider;
import com.bytechef.ee.platform.configuration.dto.AiProviderDTO;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.domain.Property;
import com.bytechef.platform.configuration.domain.Property.Scope;
import com.bytechef.platform.configuration.service.PropertyService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Arrays;
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

    @SuppressFBWarnings("EI")
    public AiProviderFacadeImpl(
        ComponentDefinitionService componentDefinitionService, PropertyService propertyService) {

        this.componentDefinitionService = componentDefinitionService;
        this.propertyService = propertyService;
    }

    @Override
    public void deleteAiProvider(int id) {
        Provider provider = getProvider(id);

        propertyService.delete(provider.getKey(), Scope.PLATFORM, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiProviderDTO> getAiProviders() {
        List<ComponentDefinition> componentDefinitions = componentDefinitionService.getComponentDefinitions();

        List<Property> properties = propertyService.getProperties(
            Arrays.stream(Provider.values())
                .map(Provider::getKey)
                .toList(),
            Scope.PLATFORM, null);

        return Arrays.stream(Provider.values())
            .map(provider -> {
                ComponentDefinition componentDefinition = componentDefinitions
                    .stream()
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

                Property property = properties
                    .stream()
                    .filter(curProperty -> curProperty.getKey()
                        .equals(provider.getKey()))
                    .findFirst()
                    .orElse(null);

                String apiKey = property != null ? (String) property.get("apiKey") : null;
                boolean enabled = property != null && property.isEnabled();

                return new AiProviderDTO(
                    provider.getId(), provider.getLabel(), componentDefinition.getIcon(), apiKey, enabled);
            })
            .filter(Objects::nonNull)
            .toList();
    }

    @Override
    public void updateAiProvider(int id, boolean enabled) {
        Provider provider = getProvider(id);

        propertyService.update(provider.getKey(), enabled, Scope.PLATFORM, null);
    }

    @Override
    public void updateAiProvider(int id, String apiKey) {
        Provider provider = getProvider(id);

        propertyService.save(provider.getKey(), Map.of("apiKey", apiKey), Scope.PLATFORM, null);
    }

    private static Provider getProvider(int id) {
        return Arrays.stream(Provider.values())
            .filter(curProvider -> curProvider.getId() == id)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Provider not found for id: " + id));
    }
}
