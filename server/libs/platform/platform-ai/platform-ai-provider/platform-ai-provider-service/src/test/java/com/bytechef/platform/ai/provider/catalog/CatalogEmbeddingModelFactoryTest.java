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

import com.bytechef.platform.ai.llm.Provider;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.ai.embedding.EmbeddingModel;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class CatalogEmbeddingModelFactoryTest {

    private final CatalogEmbeddingModelFactory factory = new CatalogEmbeddingModelFactory();

    @Test
    void testBuildsOpenAiEmbeddingModel() {
        EmbeddingModel embeddingModel = factory.createEmbeddingModel(
            Provider.OPEN_AI, "text-embedding-3-small", "sk-test", null);

        assertThat(embeddingModel).isNotNull();
    }

    @Test
    void testReturnsNullForProviderWithoutEmbeddingSupport() {
        assertThat(factory.createEmbeddingModel(Provider.ANTHROPIC, "irrelevant", "sk-test", null)).isNull();
    }

    @Test
    void testBuildsOllamaEmbeddingModelWithCustomUrl() {
        // Ollama needs no API key and accepts a custom base URL threaded through the connection parameters.
        EmbeddingModel embeddingModel = factory.createEmbeddingModel(
            Provider.OLLAMA, "nomic-embed-text", null, "http://remote-host:11434");

        assertThat(embeddingModel).isNotNull();
    }

    @Test
    void testEmbeddingSupportMatchesProviderCapability() {
        for (Provider provider : Provider.values()) {
            EmbeddingModel embeddingModel = factory.createEmbeddingModel(
                provider, "text-embedding-3-small", "sk-test", null);

            assertThat(embeddingModel != null)
                .as("Provider %s: factory builds an embedding model iff Provider.isEmbeddingSupported()", provider)
                .isEqualTo(provider.isEmbeddingSupported());
        }
    }
}
