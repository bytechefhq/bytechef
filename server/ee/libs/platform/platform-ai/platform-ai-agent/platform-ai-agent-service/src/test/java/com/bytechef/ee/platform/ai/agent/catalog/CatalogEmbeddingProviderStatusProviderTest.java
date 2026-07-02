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

import com.bytechef.ee.platform.configuration.dto.AiDefaultModelWithApiKeyDTO;
import com.bytechef.ee.platform.configuration.facade.AiProviderFacade;
import com.bytechef.platform.ai.llm.Provider;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class CatalogEmbeddingProviderStatusProviderTest {

    private static final AiDefaultModelWithApiKeyDTO OPEN_AI_DEFAULT_MODEL =
        new AiDefaultModelWithApiKeyDTO(Provider.OPEN_AI, "text-embedding-3-small", "sk-test");

    private final AiProviderFacade aiProviderFacade = mock(AiProviderFacade.class);
    private final CatalogEmbeddingProviderStatusProvider statusProvider =
        new CatalogEmbeddingProviderStatusProvider(aiProviderFacade);

    @Test
    void testActiveWhenDefaultModelResolves() {
        when(aiProviderFacade.getAiDefaultEmbeddingModelApiKey(2)).thenReturn(OPEN_AI_DEFAULT_MODEL);

        assertThat(statusProvider.isEmbeddingActive(2)).isTrue();
    }

    @Test
    void testInactiveWhenNoDefaultModel() {
        when(aiProviderFacade.getAiDefaultEmbeddingModelApiKey(2)).thenReturn(null);

        assertThat(statusProvider.isEmbeddingActive(2)).isFalse();
    }
}
