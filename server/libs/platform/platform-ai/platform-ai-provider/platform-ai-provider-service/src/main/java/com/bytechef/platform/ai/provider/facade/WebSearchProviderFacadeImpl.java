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
import com.bytechef.platform.ai.provider.dto.WebSearchProviderConnection;
import com.bytechef.platform.ai.provider.dto.WebSearchProviderDTO;
import com.bytechef.platform.ai.provider.dto.WebSearchProviderTestResultDTO;
import com.bytechef.platform.ai.websearch.WebSearchProvider;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.domain.Property;
import com.bytechef.platform.configuration.domain.Property.Scope;
import com.bytechef.platform.configuration.service.PropertyService;
import com.bytechef.platform.security.constant.AuthorityConstants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
public class WebSearchProviderFacadeImpl implements WebSearchProviderFacade {

    private static final Logger log = LoggerFactory.getLogger(WebSearchProviderFacadeImpl.class);

    private static final String API_KEY = "apiKey";
    private static final String API_KEY_FROM_PROPERTIES = "apiKeyFromProperties";
    private static final String BASE_URL = "baseUrl";
    private static final int MASK_VISIBLE_CHARACTERS = 4;

    private final ApplicationProperties applicationProperties;
    private final ComponentDefinitionService componentDefinitionService;
    private final PropertyService propertyService;
    private final ObjectProvider<WebSearchProviderConnectionTester> webSearchProviderConnectionTesterProvider;

    @SuppressFBWarnings("EI")
    public WebSearchProviderFacadeImpl(
        ApplicationProperties applicationProperties, ComponentDefinitionService componentDefinitionService,
        PropertyService propertyService,
        ObjectProvider<WebSearchProviderConnectionTester> webSearchProviderConnectionTesterProvider) {

        this.applicationProperties = applicationProperties;
        this.componentDefinitionService = componentDefinitionService;
        this.propertyService = propertyService;
        this.webSearchProviderConnectionTesterProvider = webSearchProviderConnectionTesterProvider;
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public void deleteWebSearchProvider(String name, int environment) {
        WebSearchProvider webSearchProvider = WebSearchProvider.valueOfName(name);

        propertyService.delete(webSearchProvider.getKey(), Scope.PLATFORM, null, (long) environment);
    }

    @Override
    @Transactional(readOnly = true)
    public @Nullable WebSearchProviderConnection fetchEnabledConnection(
        WebSearchProvider webSearchProvider, int environment) {

        Optional<Property> propertyOptional = fetchProperty(webSearchProvider, environment);

        if (propertyOptional.isEmpty()) {
            String propertiesApiKey = getPropertiesApiKey(webSearchProvider);

            if (!isEnabledByProperties(webSearchProvider) || propertiesApiKey == null) {
                return null;
            }

            return new WebSearchProviderConnection(propertiesApiKey, getPropertiesBaseUrl(webSearchProvider));
        }

        Property property = propertyOptional.get();

        if (!property.isEnabled()) {
            return null;
        }

        String apiKey = getCurrentApiKey(webSearchProvider, property);

        if (apiKey == null) {
            return null;
        }

        return new WebSearchProviderConnection(apiKey, getBaseUrl(webSearchProvider, property));
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

        if (fetchProperty(webSearchProvider, environment).isEmpty()) {
            propertyService.save(
                webSearchProvider.getKey(), Map.of(API_KEY_FROM_PROPERTIES, true), Scope.PLATFORM, null,
                environmentId);
        }

        propertyService.update(webSearchProvider.getKey(), enabled, Scope.PLATFORM, null, environmentId);
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    @Transactional(readOnly = true)
    public WebSearchProviderTestResultDTO testWebSearchProvider(
        String name, @Nullable String apiKey, @Nullable String baseUrl, int environment) {

        WebSearchProvider webSearchProvider = WebSearchProvider.valueOfName(name);

        Property property = fetchProperty(webSearchProvider, environment).orElse(null);

        String resolvedApiKey = isBlank(apiKey) ? getCurrentApiKey(webSearchProvider, property) : apiKey.strip();

        if (resolvedApiKey == null) {
            return WebSearchProviderTestResultDTO.failure("Enter an API key to test.");
        }

        String resolvedBaseUrl;

        try {
            resolvedBaseUrl = isBlank(baseUrl) ? getPropertiesBaseUrl(webSearchProvider) : validateBaseUrl(baseUrl);
        } catch (IllegalArgumentException exception) {
            return WebSearchProviderTestResultDTO.failure(exception.getMessage());
        }

        WebSearchProviderConnectionTester webSearchProviderConnectionTester =
            webSearchProviderConnectionTesterProvider.getIfAvailable();

        if (webSearchProviderConnectionTester == null) {
            return WebSearchProviderTestResultDTO.failure("This server cannot test web search providers.");
        }

        try {
            webSearchProviderConnectionTester.test(
                webSearchProvider, new WebSearchProviderConnection(resolvedApiKey, resolvedBaseUrl));

            return WebSearchProviderTestResultDTO.succeeded();
        } catch (RuntimeException exception) {
            log.debug("Testing the {} web search provider failed", webSearchProvider.getLabel(), exception);

            return WebSearchProviderTestResultDTO.failure(mostSpecificMessage(exception));
        }
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public void updateWebSearchProvider(
        String name, @Nullable String apiKey, @Nullable String baseUrl, int environment) {

        WebSearchProvider webSearchProvider = WebSearchProvider.valueOfName(name);

        Property property = fetchProperty(webSearchProvider, environment).orElse(null);

        String savedApiKey = isBlank(apiKey)
            ? property == null ? null : getSavedValue(property, API_KEY)
            : apiKey.strip();

        if (savedApiKey == null && getPropertiesApiKey(webSearchProvider) == null) {
            throw new IllegalArgumentException("The API key of " + webSearchProvider.getLabel() + " is required");
        }

        Map<String, Object> value = new HashMap<>();

        if (savedApiKey == null) {
            value.put(API_KEY_FROM_PROPERTIES, true);
        } else {
            value.put(API_KEY, savedApiKey);
        }

        if (!isBlank(baseUrl)) {
            value.put(BASE_URL, validateBaseUrl(baseUrl));
        }

        propertyService.save(webSearchProvider.getKey(), value, Scope.PLATFORM, null, (long) environment);
    }

    private static @Nullable Property findProperty(List<Property> properties, WebSearchProvider webSearchProvider) {
        return properties.stream()
            .filter(property -> Objects.equals(property.getKey(), webSearchProvider.getKey()))
            .findFirst()
            .orElse(null);
    }

    private static @Nullable String getSavedValue(Property property, String key) {
        if (property.get(key) instanceof String value && !value.isBlank()) {
            return value;
        }

        return null;
    }

    private static boolean isBlank(@Nullable String value) {
        return value == null || value.isBlank();
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

    private static String mostSpecificMessage(Throwable throwable) {
        String message = null;

        for (Throwable current = throwable; current != null; current = current.getCause()) {
            if (!isBlank(current.getMessage())) {
                message = current.getMessage();
            }

            if (current.getCause() == current) {
                break;
            }
        }

        return message == null ? "The test search failed." : message;
    }

    private static String validateBaseUrl(String baseUrl) {
        String strippedBaseUrl = baseUrl.strip();

        try {
            URI uri = new URI(strippedBaseUrl);

            String scheme = uri.getScheme();

            if (uri.getHost() == null || !("https".equalsIgnoreCase(scheme) || "http".equalsIgnoreCase(scheme))) {
                throw new IllegalArgumentException("The base URL must be an http or https URL.");
            }
        } catch (URISyntaxException exception) {
            throw new IllegalArgumentException("The base URL must be an http or https URL.", exception);
        }

        return strippedBaseUrl;
    }

    private Optional<Property> fetchProperty(WebSearchProvider webSearchProvider, int environment) {
        return propertyService.fetchProperty(webSearchProvider.getKey(), Scope.PLATFORM, null, (long) environment);
    }

    private String getBaseUrl(WebSearchProvider webSearchProvider, @Nullable Property property) {
        String savedBaseUrl = property == null ? null : getSavedValue(property, BASE_URL);

        return savedBaseUrl == null ? getPropertiesBaseUrl(webSearchProvider) : savedBaseUrl;
    }

    private @Nullable String getCurrentApiKey(WebSearchProvider webSearchProvider, @Nullable Property property) {
        String savedApiKey = property == null ? null : getSavedValue(property, API_KEY);

        return savedApiKey == null ? getPropertiesApiKey(webSearchProvider) : savedApiKey;
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

        return isBlank(apiKey) ? null : apiKey;
    }

    private String getPropertiesBaseUrl(WebSearchProvider webSearchProvider) {
        ApplicationProperties.Ai ai = applicationProperties.getAi();

        return switch (webSearchProvider) {
            case BRAVE -> ai.getBrave()
                .getBaseUrl();
            case FIRECRAWL -> ai.getFirecrawl()
                .getBaseUrl();
        };
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

        String savedApiKey = property == null ? null : getSavedValue(property, API_KEY);
        String propertiesApiKey = getPropertiesApiKey(webSearchProvider);

        String apiKey = savedApiKey == null ? propertiesApiKey : savedApiKey;

        boolean enabled = property == null
            ? isEnabledByProperties(webSearchProvider) && propertiesApiKey != null
            : property.isEnabled() && apiKey != null;

        return new WebSearchProviderDTO(
            webSearchProvider.getName(), webSearchProvider.getLabel(), webSearchProvider.getDescription(),
            getIcon(webSearchProvider), enabled, mask(apiKey), savedApiKey == null && propertiesApiKey != null,
            getBaseUrl(webSearchProvider, property), getPropertiesBaseUrl(webSearchProvider));
    }
}
