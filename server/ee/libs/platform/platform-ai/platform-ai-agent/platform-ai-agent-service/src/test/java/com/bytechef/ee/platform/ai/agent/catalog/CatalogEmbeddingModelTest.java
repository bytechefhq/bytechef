/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.agent.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.ai.llm.Provider;
import com.bytechef.config.ApplicationProperties;
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

    private final ProviderApiKeyResolver providerApiKeyResolver = mock(ProviderApiKeyResolver.class);
    private final CatalogEmbeddingModelFactory catalogEmbeddingModelFactory =
        mock(CatalogEmbeddingModelFactory.class);
    private final ApplicationProperties applicationProperties = mock(ApplicationProperties.class, RETURNS_DEEP_STUBS);

    private final CatalogEmbeddingModel catalogEmbeddingModel = new CatalogEmbeddingModel(
        providerApiKeyResolver, catalogEmbeddingModelFactory, applicationProperties);

    @AfterEach
    void tearDown() {
        EnvironmentContext.clear();
    }

    @Test
    void testDelegatesEmbedWhenKeyResolved() {
        stubModelName();
        EmbeddingModel delegate = mock(EmbeddingModel.class);
        Document document = new Document("hello");

        when(delegate.embed(document)).thenReturn(new float[] {
            0.1f
        });
        when(providerApiKeyResolver.resolve(Provider.OPEN_AI, Environment.PRODUCTION.ordinal()))
            .thenReturn("sk-test");
        when(catalogEmbeddingModelFactory.createEmbeddingModel(Provider.OPEN_AI, "text-embedding-3-small", "sk-test"))
            .thenReturn(delegate);

        float[] result = catalogEmbeddingModel.embed(document);

        assertThat(result).containsExactly(0.1f);
    }

    @Test
    void testThrowsActionableErrorWhenNoKey() {
        stubModelName();

        when(providerApiKeyResolver.resolve(Provider.OPEN_AI, Environment.PRODUCTION.ordinal()))
            .thenReturn(null);

        assertThatThrownBy(() -> catalogEmbeddingModel.embed(new Document("hello")))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("No embedding provider is activated")
            .hasMessageContaining("PRODUCTION")
            .hasMessageContaining("bytechef.ai.provider.openai.api-key");
    }

    @Test
    void testReadsEnvironmentFromContext() {
        stubModelName();
        EmbeddingModel delegate = mock(EmbeddingModel.class);
        Document document = new Document("hello");

        EnvironmentContext.set(Environment.STAGING);

        when(delegate.embed(document)).thenReturn(new float[] {
            0.2f
        });
        when(providerApiKeyResolver.resolve(Provider.OPEN_AI, Environment.STAGING.ordinal()))
            .thenReturn("sk-staging");
        when(catalogEmbeddingModelFactory.createEmbeddingModel(
            Provider.OPEN_AI, "text-embedding-3-small", "sk-staging"))
                .thenReturn(delegate);

        catalogEmbeddingModel.embed(document);

        verify(providerApiKeyResolver).resolve(Provider.OPEN_AI, Environment.STAGING.ordinal());
    }

    @Test
    void testCachesDelegatePerEnvironmentAndKey() {
        stubModelName();
        EmbeddingModel delegate = mock(EmbeddingModel.class);

        when(delegate.embed(org.mockito.ArgumentMatchers.any(Document.class))).thenReturn(new float[] {
            0.3f
        });
        when(providerApiKeyResolver.resolve(Provider.OPEN_AI, Environment.PRODUCTION.ordinal()))
            .thenReturn("sk-test");
        when(catalogEmbeddingModelFactory.createEmbeddingModel(Provider.OPEN_AI, "text-embedding-3-small", "sk-test"))
            .thenReturn(delegate);

        catalogEmbeddingModel.embed(new Document("a"));
        catalogEmbeddingModel.embed(new Document("b"));

        verify(catalogEmbeddingModelFactory, times(1))
            .createEmbeddingModel(Provider.OPEN_AI, "text-embedding-3-small", "sk-test");
    }

    private void stubModelName() {
        when(applicationProperties.getAi()
            .getProvider()
            .getEmbedding()
            .getOpenAi()
            .getOptions()
            .getModel()).thenReturn("text-embedding-3-small");
    }
}
