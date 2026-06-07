/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.agent.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
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
class CatalogChatClientResolverTest {

    private final PropertyService propertyService = mock(PropertyService.class);
    private final CatalogChatModelFactory catalogChatModelFactory = mock(CatalogChatModelFactory.class);
    private final ApplicationProperties applicationProperties = mock(ApplicationProperties.class, RETURNS_DEEP_STUBS);
    private final CatalogChatClientResolver resolver =
        new CatalogChatClientResolverImpl(propertyService, catalogChatModelFactory, applicationProperties);

    @Test
    void testResolveReturnsNullForUnknownProviderKey() {
        assertThat(resolver.resolve(1, "notAProvider", "x")).isNull();
    }

    @Test
    void testResolveReturnsNullForOutOfRangeEnvironment() {
        // The range check short-circuits before any propertyService lookup, so no stubbing is needed (and a forged
        // or out-of-range ordinal can never drive platform-API-key selection — fail closed).
        assertThat(resolver.resolve(99, "ai.provider.openAi", "gpt-4o")).isNull();
        assertThat(resolver.resolve(-1, "ai.provider.openAi", "gpt-4o")).isNull();
    }

    @Test
    void testResolveReturnsNullWhenProviderDisabledAndNoConfigKey() {
        Property property = mock(Property.class);

        when(property.isEnabled()).thenReturn(false);
        when(propertyService.fetchProperty(eq("ai.provider.openAi"), eq(Scope.PLATFORM), eq(null), anyLong()))
            .thenReturn(Optional.of(property));
        when(applicationProperties.getAi()
            .getProvider()
            .getOpenAi()
            .getApiKey()).thenReturn(null);

        assertThat(resolver.resolve(1, "ai.provider.openAi", "gpt-4o")).isNull();
    }

    @Test
    void testResolveBuildsClientWhenEnabledWithKey() {
        Property property = mock(Property.class);

        when(property.isEnabled()).thenReturn(true);
        when(property.get("apiKey")).thenReturn("sk-test");
        when(propertyService.fetchProperty(eq("ai.provider.openAi"), eq(Scope.PLATFORM), eq(null), anyLong()))
            .thenReturn(Optional.of(property));
        when(catalogChatModelFactory.createChatModel(eq(Provider.OPEN_AI), eq("gpt-4o"), eq("sk-test")))
            .thenReturn(mock(org.springframework.ai.chat.model.ChatModel.class));

        assertThat(resolver.resolve(1, "ai.provider.openAi", "gpt-4o")).isNotNull();
    }

    @Test
    void testResolveFallsBackToConfigApiKeyWhenPropertyAbsent() {
        when(propertyService.fetchProperty(eq("ai.provider.openAi"), eq(Scope.PLATFORM), eq(null), anyLong()))
            .thenReturn(Optional.empty());
        when(applicationProperties.getAi()
            .getProvider()
            .getOpenAi()
            .getApiKey()).thenReturn("sk-config");
        when(catalogChatModelFactory.createChatModel(eq(Provider.OPEN_AI), eq("gpt-4o"), eq("sk-config")))
            .thenReturn(mock(org.springframework.ai.chat.model.ChatModel.class));

        assertThat(resolver.resolve(1, "ai.provider.openAi", "gpt-4o")).isNotNull();
    }
}
