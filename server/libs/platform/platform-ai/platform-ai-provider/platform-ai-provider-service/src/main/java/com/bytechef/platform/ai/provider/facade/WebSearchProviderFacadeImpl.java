/*
 * Copyright 2025 ByteChef
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

package com.bytechef.platform.ai.provider.facade;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.ai.provider.dto.WebSearchProviderDTO;
import com.bytechef.platform.ai.websearch.WebSearchProvider;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.domain.Property;
import com.bytechef.platform.configuration.domain.Property.Scope;
import com.bytechef.platform.configuration.service.PropertyService;
import com.bytechef.platform.security.constant.AuthorityConstants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
public class WebSearchProviderFacadeImpl implements WebSearchProviderFacade {

    private static final String API_KEY = "apiKey";
    private static final String API_KEY_FROM_PROPERTIES = "apiKeyFromProperties";
    private static final int MASK_VISIBLE_CHARACTERS = 4;

    private final ApplicationProperties applicationProperties;
    private final ComponentDefinitionService componentDefinitionService;
    private final PropertyService propertyService;

    @SuppressFBWarnings("EI")
    public WebSearchProviderFacadeImpl(
        ApplicationProperties applicationProperties, ComponentDefinitionService componentDefinitionService,
        PropertyService propertyService) {

        this.applicationProperties = applicationProperties;
        this.componentDefinitionService = componentDefinitionService;
        this.propertyService = propertyService;
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public void deleteWebSearchProvider(String name, int environment) {
        WebSearchProvider webSearchProvider = WebSearchProvider.valueOfName(name);

        propertyService.delete(webSearchProvider.getKey(), Scope.PLATFORM, null, (long) environment);
    }

    @Override
    @Transactional(readOnly = true)
    public @Nullable String fetchEnabledApiKey(WebSearchProvider webSearchProvider, int environment) {
        Optional<Property> propertyOptional = propertyService.fetchProperty(
            webSearchProvider.getKey(), Scope.PLATFORM, null, (long) environment);

        if (propertyOptional.isEmpty()) {
            return isEnabledByProperties(webSearchProvider) ? getPropertiesApiKey(webSearchProvider) : null;
        }

        Property property = propertyOptional.get();

        if (!property.isEnabled()) {
            return null;
        }

        String savedApiKey = getSavedApiKey(property);

        return savedApiKey == null ? getPropertiesApiKey(webSearchProvider) : savedApiKey;
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    @Transactional(readOnly = true)
    public List<WebSearchProviderDTO> getWebSearchProviders(int environment) {
        List<WebSearchProvider> webSearchProviders = Arrays.asList(WebSearchProvider.values());

        List<Property> properties = propertyService.getProperties(
            webSearchProviders.stream()
                .map(WebSearchProvider::getKey)
                .toList(),
            Scope.PLATFORM, null, (long) environment);

        return webSearchProviders.stream()
            .map(webSearchProvider -> toWebSearchProviderDTO(
                webSearchProvider, findProperty(properties, webSearchProvider)))
            .toList();
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public void enableWebSearchProvider(String name, boolean enabled, int environment) {
        WebSearchProvider webSearchProvider = WebSearchProvider.valueOfName(name);

        long environmentId = environment;

        if (propertyService.fetchProperty(webSearchProvider.getKey(), Scope.PLATFORM, null, environmentId)
            .isEmpty()) {

            propertyService.save(
                webSearchProvider.getKey(), Map.of(API_KEY_FROM_PROPERTIES, true), Scope.PLATFORM, null,
                environmentId);
        }

        propertyService.update(webSearchProvider.getKey(), enabled, Scope.PLATFORM, null, environmentId);
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public void updateWebSearchProvider(String name, String apiKey, int environment) {
        WebSearchProvider webSearchProvider = WebSearchProvider.valueOfName(name);

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("The API key of " + webSearchProvider.getLabel() + " is required");
        }

        propertyService.save(
            webSearchProvider.getKey(), Map.of(API_KEY, apiKey.strip()), Scope.PLATFORM, null, (long) environment);
    }

    private static @Nullable Property findProperty(List<Property> properties, WebSearchProvider webSearchProvider) {
        return properties.stream()
            .filter(property -> Objects.equals(property.getKey(), webSearchProvider.getKey()))
            .findFirst()
            .orElse(null);
    }

    private static @Nullable String getSavedApiKey(Property property) {
        if (property.get(API_KEY) instanceof String apiKey && !apiKey.isBlank()) {
            return apiKey;
        }

        return null;
    }

    private static @Nullable String mask(@Nullable String apiKey) {
        if (apiKey == null) {
            return null;
        }

        if (apiKey.length() <= MASK_VISIBLE_CHARACTERS) {
            return "*".repeat(apiKey.length());
        }

        return "*".repeat(8) + apiKey.substring(apiKey.length() - MASK_VISIBLE_CHARACTERS);
    }

    private @Nullable String getIcon(WebSearchProvider webSearchProvider) {
        return componentDefinitionService.fetchComponentDefinition(webSearchProvider.getName(), null)
            .map(ComponentDefinition::getIcon)
            .orElse(null);
    }

    private @Nullable String getPropertiesApiKey(WebSearchProvider webSearchProvider) {
        ApplicationProperties.Ai ai = applicationProperties.getAi();

        String apiKey = switch (webSearchProvider) {
            case BRAVE -> ai.getBrave()
                .getApiKey();
            case FIRECRAWL -> ai.getFirecrawl()
                .getApiKey();
        };

        return apiKey == null || apiKey.isBlank() ? null : apiKey;
    }

    private boolean isEnabledByProperties(WebSearchProvider webSearchProvider) {
        ApplicationProperties.Ai ai = applicationProperties.getAi();

        return switch (webSearchProvider) {
            case BRAVE -> ai.getBrave()
                .isEnabled();
            case FIRECRAWL -> ai.getFirecrawl()
                .isEnabled();
        };
    }

    private WebSearchProviderDTO toWebSearchProviderDTO(
        WebSearchProvider webSearchProvider, @Nullable Property property) {

        String savedApiKey = property == null ? null : getSavedApiKey(property);
        String propertiesApiKey = getPropertiesApiKey(webSearchProvider);

        String apiKey = savedApiKey == null ? propertiesApiKey : savedApiKey;

        boolean enabled = property == null
            ? isEnabledByProperties(webSearchProvider) && propertiesApiKey != null
            : property.isEnabled() && apiKey != null;

        return new WebSearchProviderDTO(
            webSearchProvider.getName(), webSearchProvider.getLabel(), webSearchProvider.getDescription(),
            getIcon(webSearchProvider), enabled, mask(apiKey), savedApiKey == null && propertiesApiKey != null);
    }
}
