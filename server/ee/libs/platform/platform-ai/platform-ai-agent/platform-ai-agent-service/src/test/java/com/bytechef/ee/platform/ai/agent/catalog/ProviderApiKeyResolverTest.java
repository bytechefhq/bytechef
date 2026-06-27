/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.agent.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.ai.llm.Provider;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.configuration.domain.Property;
import com.bytechef.platform.configuration.domain.Property.Scope;
import com.bytechef.platform.configuration.service.PropertyService;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ProviderApiKeyResolverTest {

    private final PropertyService propertyService = mock(PropertyService.class);
    private final ApplicationProperties applicationProperties = mock(ApplicationProperties.class, RETURNS_DEEP_STUBS);
    private final ProviderApiKeyResolver resolver =
        new ProviderApiKeyResolver(propertyService, applicationProperties);

    @Test
    void testReturnsEnabledPropertyApiKey() {
        Property property = mock(Property.class);

        when(property.isEnabled()).thenReturn(true);
        when(property.get("apiKey")).thenReturn("sk-from-ui");
        when(propertyService.fetchProperty(Provider.OPEN_AI.getKey(), Scope.PLATFORM, null, 2L))
            .thenReturn(Optional.of(property));

        assertThat(resolver.resolve(Provider.OPEN_AI, 2)).isEqualTo("sk-from-ui");
    }

    @Test
    void testFallsBackToConfigWhenNoEnabledProperty() {
        when(propertyService.fetchProperty(Provider.OPEN_AI.getKey(), Scope.PLATFORM, null, 2L))
            .thenReturn(Optional.empty());
        when(applicationProperties.getAi()
            .getProvider()
            .getOpenAi()
            .getApiKey()).thenReturn("sk-from-config");

        assertThat(resolver.resolve(Provider.OPEN_AI, 2)).isEqualTo("sk-from-config");
    }

    @Test
    void testFallsBackToConfigWhenPropertyDisabled() {
        Property property = mock(Property.class);

        when(property.isEnabled()).thenReturn(false);
        when(propertyService.fetchProperty(Provider.OPEN_AI.getKey(), Scope.PLATFORM, null, 2L))
            .thenReturn(Optional.of(property));
        when(applicationProperties.getAi()
            .getProvider()
            .getOpenAi()
            .getApiKey()).thenReturn("sk-from-config");

        assertThat(resolver.resolve(Provider.OPEN_AI, 2)).isEqualTo("sk-from-config");
    }

    @Test
    void testReturnsNullWhenNeitherPresent() {
        when(propertyService.fetchProperty(Provider.OPEN_AI.getKey(), Scope.PLATFORM, null, 2L))
            .thenReturn(Optional.empty());
        when(applicationProperties.getAi()
            .getProvider()
            .getOpenAi()
            .getApiKey()).thenReturn(null);

        assertThat(resolver.resolve(Provider.OPEN_AI, 2)).isNull();
    }
}
