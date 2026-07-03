/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.agent.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.configuration.facade.AiProviderFacade;
import com.bytechef.platform.ai.llm.Provider;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class CatalogChatClientResolverTest {

    private static final String OPEN_AI_KEY = Provider.OPEN_AI.getKey();

    private final CatalogChatModelFactory catalogChatModelFactory = mock(CatalogChatModelFactory.class);
    private final AiProviderFacade aiProviderFacade = mock(AiProviderFacade.class);
    private final CatalogChatClientResolver resolver =
        new CatalogChatClientResolverImpl(catalogChatModelFactory, aiProviderFacade);

    @Test
    void testResolveReturnsNullForUnknownProviderKey() {
        assertThat(resolver.resolve("notAProvider", "x", 1)).isNull();
    }

    @Test
    void testResolveReturnsNullForOutOfRangeEnvironment() {
        // The range check short-circuits before any API-key lookup, so no stubbing is needed (and a forged or
        // out-of-range ordinal can never drive platform-API-key selection — fail closed).
        assertThat(resolver.resolve(OPEN_AI_KEY, "gpt-4o", 99)).isNull();
        assertThat(resolver.resolve(OPEN_AI_KEY, "gpt-4o", -1)).isNull();
    }

    @Test
    void testResolveReturnsNullWhenNoApiKey() {
        when(aiProviderFacade.getApiKey(OPEN_AI_KEY, 1)).thenReturn(null);

        assertThat(resolver.resolve(OPEN_AI_KEY, "gpt-4o", 1)).isNull();
    }

    @Test
    void testResolveBuildsClientWhenApiKeyResolves() {
        when(aiProviderFacade.getApiKey(OPEN_AI_KEY, 1)).thenReturn("sk-test");
        when(catalogChatModelFactory.createChatModel(Provider.OPEN_AI, "gpt-4o", "sk-test", null))
            .thenReturn(mock(org.springframework.ai.chat.model.ChatModel.class));

        assertThat(resolver.resolve(OPEN_AI_KEY, "gpt-4o", 1)).isNotNull();
    }

    @Test
    void testResolveBuildsClientWhenApiKeyIsConfigFallback() {
        when(aiProviderFacade.getApiKey(OPEN_AI_KEY, 1)).thenReturn("sk-config");
        when(catalogChatModelFactory.createChatModel(Provider.OPEN_AI, "gpt-4o", "sk-config", null))
            .thenReturn(mock(org.springframework.ai.chat.model.ChatModel.class));

        assertThat(resolver.resolve(OPEN_AI_KEY, "gpt-4o", 1)).isNotNull();
    }
}
