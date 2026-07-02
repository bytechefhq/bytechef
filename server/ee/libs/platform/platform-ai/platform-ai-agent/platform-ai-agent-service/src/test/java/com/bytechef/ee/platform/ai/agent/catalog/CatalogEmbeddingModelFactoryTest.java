/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.agent.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.platform.ai.llm.Provider;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.ai.embedding.EmbeddingModel;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class CatalogEmbeddingModelFactoryTest {

    private final CatalogEmbeddingModelFactory factory = new CatalogEmbeddingModelFactory();

    @Test
    void testBuildsOpenAiEmbeddingModel() {
        EmbeddingModel embeddingModel = factory.createEmbeddingModel(
            Provider.OPEN_AI, "text-embedding-3-small", "sk-test");

        assertThat(embeddingModel).isNotNull();
    }

    @Test
    void testReturnsNullForProviderWithoutEmbeddingSupport() {
        assertThat(factory.createEmbeddingModel(Provider.ANTHROPIC, "irrelevant", "sk-test")).isNull();
    }

    @Test
    void testEmbeddingSupportMatchesProviderCapability() {
        for (Provider provider : Provider.values()) {
            EmbeddingModel embeddingModel = factory.createEmbeddingModel(
                provider, "text-embedding-3-small", "sk-test");

            assertThat(embeddingModel != null)
                .as("Provider %s: factory builds an embedding model iff Provider.isEmbeddingSupported()", provider)
                .isEqualTo(provider.isEmbeddingSupported());
        }
    }
}
