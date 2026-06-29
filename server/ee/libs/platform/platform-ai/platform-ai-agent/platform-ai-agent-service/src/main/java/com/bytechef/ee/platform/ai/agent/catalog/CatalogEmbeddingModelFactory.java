/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.agent.catalog;

import static com.bytechef.component.ai.llm.constant.LLMConstants.MODEL;
import static com.bytechef.component.definition.Authorization.TOKEN;

import com.bytechef.component.ai.llm.Provider;
import com.bytechef.component.ai.llm.openai.cluster.OpenAiEmbedding;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.definition.ai.vectorstore.EmbeddingFunction;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Component;

/**
 * Builds a Spring-AI {@link EmbeddingModel} for a catalog {@link Provider} + model name + platform API key, by reusing
 * the LLM component's existing embedding lambda. Mirrors {@code CatalogChatModelFactory}. Returns {@code null} for
 * providers that do not expose an embedding model.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class CatalogEmbeddingModelFactory {

    public @Nullable EmbeddingModel createEmbeddingModel(Provider provider, String model, String apiKey) {
        EmbeddingFunction embeddingFunction = resolveFactory(provider);

        if (embeddingFunction == null) {
            return null;
        }

        Parameters inputParameters = ParametersFactory.create(Map.of(MODEL, model));
        Parameters connectionParameters = ParametersFactory.create(Map.of(TOKEN, apiKey));

        return embeddingFunction.apply(inputParameters, connectionParameters);
    }

    private static @Nullable EmbeddingFunction resolveFactory(Provider provider) {
        if (!provider.isEmbeddingSupported()) {
            return null;
        }

        return switch (provider) {
            case OPEN_AI -> OpenAiEmbedding.EMBEDDING_MODEL;
            default -> null;
        };
    }
}
