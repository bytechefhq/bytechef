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

import com.bytechef.component.ai.llm.Provider;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class CatalogEmbeddingProviderStatusProviderTest {

    private final ProviderApiKeyResolver providerApiKeyResolver = mock(ProviderApiKeyResolver.class);
    private final CatalogEmbeddingProviderStatusProvider statusProvider =
        new CatalogEmbeddingProviderStatusProvider(providerApiKeyResolver);

    @Test
    void testActiveWhenKeyResolves() {
        when(providerApiKeyResolver.resolve(Provider.OPEN_AI, 2)).thenReturn("sk-test");

        assertThat(statusProvider.isEmbeddingActive(2)).isTrue();
    }

    @Test
    void testInactiveWhenNoKey() {
        when(providerApiKeyResolver.resolve(Provider.OPEN_AI, 2)).thenReturn(null);

        assertThat(statusProvider.isEmbeddingActive(2)).isFalse();
    }

    @Test
    void testInactiveWhenBlankKey() {
        when(providerApiKeyResolver.resolve(Provider.OPEN_AI, 2)).thenReturn("   ");

        assertThat(statusProvider.isEmbeddingActive(2)).isFalse();
    }
}
