/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.agent.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.configuration.dto.AiDefaultModelWithApiKeyDTO;
import com.bytechef.ee.platform.configuration.facade.AiProviderFacade;
import com.bytechef.platform.ai.llm.Provider;
import com.bytechef.platform.configuration.context.EnvironmentContext;
import com.bytechef.platform.configuration.domain.Environment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class CatalogEmbeddingModelTest {

    private static final AiDefaultModelWithApiKeyDTO OPEN_AI_DEFAULT_MODEL =
        new AiDefaultModelWithApiKeyDTO(Provider.OPEN_AI, "text-embedding-3-small", "sk-test", null);

    private final AiProviderFacade aiProviderFacade = mock(AiProviderFacade.class);
    private final CatalogEmbeddingModelFactory catalogEmbeddingModelFactory =
        mock(CatalogEmbeddingModelFactory.class);

    private final CatalogEmbeddingModel catalogEmbeddingModel = new CatalogEmbeddingModel(
        aiProviderFacade, catalogEmbeddingModelFactory);

    @AfterEach
    void tearDown() {
        EnvironmentContext.clear();
    }

    @Test
    void testDelegatesEmbedWhenModelResolved() {
        EmbeddingModel delegate = mock(EmbeddingModel.class);
        Document document = new Document("hello");

        when(delegate.embed(document)).thenReturn(new float[] {
            0.1f
        });
        when(aiProviderFacade.getAiDefaultEmbeddingModelApiKey(Environment.PRODUCTION.ordinal()))
            .thenReturn(OPEN_AI_DEFAULT_MODEL);
        when(catalogEmbeddingModelFactory.createEmbeddingModel(Provider.OPEN_AI, "text-embedding-3-small", "sk-test",
            null))
                .thenReturn(delegate);

        float[] result = catalogEmbeddingModel.embed(document);

        assertThat(result).containsExactly(0.1f);
    }

    @Test
    void testThrowsActionableErrorWhenNoProviderActivated() {
        when(aiProviderFacade.getAiDefaultEmbeddingModelApiKey(Environment.PRODUCTION.ordinal()))
            .thenReturn(null);

        assertThatThrownBy(() -> catalogEmbeddingModel.embed(new Document("hello")))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("No embedding provider is activated")
            .hasMessageContaining("PRODUCTION")
            .hasMessageContaining("bytechef.ai.provider.openai.api-key");
    }

    @Test
    void testReadsEnvironmentFromContext() {
        EmbeddingModel delegate = mock(EmbeddingModel.class);
        Document document = new Document("hello");

        EnvironmentContext.set(Environment.STAGING);

        when(delegate.embed(document)).thenReturn(new float[] {
            0.2f
        });
        when(aiProviderFacade.getAiDefaultEmbeddingModelApiKey(Environment.STAGING.ordinal()))
            .thenReturn(OPEN_AI_DEFAULT_MODEL);
        when(catalogEmbeddingModelFactory.createEmbeddingModel(
            Provider.OPEN_AI, "text-embedding-3-small", "sk-test", null))
                .thenReturn(delegate);

        catalogEmbeddingModel.embed(document);

        verify(aiProviderFacade).getAiDefaultEmbeddingModelApiKey(Environment.STAGING.ordinal());
    }

    @Test
    void testCachesDelegatePerEnvironmentAndKey() {
        EmbeddingModel delegate = mock(EmbeddingModel.class);

        when(delegate.embed(org.mockito.ArgumentMatchers.any(Document.class))).thenReturn(new float[] {
            0.3f
        });
        when(aiProviderFacade.getAiDefaultEmbeddingModelApiKey(Environment.PRODUCTION.ordinal()))
            .thenReturn(OPEN_AI_DEFAULT_MODEL);
        when(catalogEmbeddingModelFactory.createEmbeddingModel(Provider.OPEN_AI, "text-embedding-3-small", "sk-test",
            null))
                .thenReturn(delegate);

        catalogEmbeddingModel.embed(new Document("a"));
        catalogEmbeddingModel.embed(new Document("b"));

        verify(catalogEmbeddingModelFactory, times(1))
            .createEmbeddingModel(Provider.OPEN_AI, "text-embedding-3-small", "sk-test", null);
    }
}
