/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.platform.configuration.facade;

import com.bytechef.component.ai.llm.constant.LLMConstants;
import com.bytechef.component.ai.llm.constant.Provider;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.domain.Property;
import com.bytechef.platform.configuration.dto.AiProviderDTO;
import com.bytechef.platform.configuration.service.PropertyService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * @author Ivica Cardic
 */
@Service
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

        propertyService.delete(provider.getKey());
    }

    @Override
    public List<AiProviderDTO> getAiProviders() {
        List<ComponentDefinition> componentDefinitions = componentDefinitionService.getComponentDefinitions();

        List<Property> properties = propertyService.getProperties(
            LLMConstants.PROVIDERS
                .stream()
                .map(Provider::getKey)
                .toList());

        return LLMConstants.PROVIDERS
            .stream()
            .map(provider -> {
                ComponentDefinition componentDefinition = componentDefinitions
                    .stream()
                    .filter(curComponentDefinition -> {
                        String name = provider.getName();

                        return name.contains(curComponentDefinition.getName());
                    })
                    .findFirst()
                    .orElseThrow(
                        () -> new IllegalStateException(
                            "Component definition not found for provider: " + provider.getKey()));

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
            .toList();
    }

    @Override
    public void updateAiProvider(int id, boolean enabled) {
        Provider provider = getProvider(id);

        propertyService.update(provider.getKey(), enabled);
    }

    @Override
    public void updateAiProvider(int id, String apiKey) {
        Provider provider = getProvider(id);

        propertyService.save(provider.getKey(), Map.of("apiKey", apiKey));
    }

    private static Provider getProvider(int id) {
        return LLMConstants.PROVIDERS.stream()
            .filter(curProvider -> curProvider.getId() == id)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Provider not found for id: " + id));
    }
}
