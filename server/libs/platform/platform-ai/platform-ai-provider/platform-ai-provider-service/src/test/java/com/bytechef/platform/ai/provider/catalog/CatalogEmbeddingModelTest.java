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

package com.bytechef.platform.ai.provider.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.platform.ai.llm.Provider;
import com.bytechef.platform.ai.provider.dto.AiDefaultModelWithApiKeyDTO;
import com.bytechef.platform.ai.provider.facade.AiProviderFacade;
import com.bytechef.platform.configuration.context.EnvironmentContext;
import com.bytechef.platform.configuration.domain.Environment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;

/**
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
