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

import static com.bytechef.component.ai.llm.constant.LLMConstants.MODEL;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.URL;
import static com.bytechef.component.definition.Authorization.TOKEN;

import com.bytechef.component.ai.llm.mistral.cluster.MistralAiEmbedding;
import com.bytechef.component.ai.llm.ollama.cluster.OllamaEmbedding;
import com.bytechef.component.ai.llm.openai.cluster.OpenAiEmbedding;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.ai.llm.Provider;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.definition.ai.vectorstore.EmbeddingFunction;
import java.util.HashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Component;

/**
 * Builds a Spring-AI {@link EmbeddingModel} for a catalog {@link Provider} + model name + platform API key, by reusing
 * the LLM component's existing embedding lambda. Mirrors {@code CatalogChatModelFactory}. Returns {@code null} for
 * providers that do not expose an embedding model.
 *
 * @author Ivica Cardic
 */
@Component
public class CatalogEmbeddingModelFactory {

    public @Nullable EmbeddingModel createEmbeddingModel(
        Provider provider, String model, @Nullable String apiKey, @Nullable String url) {

        EmbeddingFunction embeddingFunction = resolveFactory(provider);

        if (embeddingFunction == null) {
            return null;
        }

        Parameters inputParameters = ParametersFactory.create(Map.of(MODEL, model));
        Parameters connectionParameters = ParametersFactory.create(createConnectionParameters(apiKey, url));

        return embeddingFunction.apply(inputParameters, connectionParameters);
    }

    private static Map<String, Object> createConnectionParameters(@Nullable String apiKey, @Nullable String url) {
        Map<String, Object> connectionParameters = new HashMap<>();

        if (apiKey != null) {
            connectionParameters.put(TOKEN, apiKey);
        }

        if (url != null && !url.isBlank()) {
            connectionParameters.put(URL, url);
        }

        return connectionParameters;
    }

    private static @Nullable EmbeddingFunction resolveFactory(Provider provider) {
        return switch (provider) {
            case MISTRAL -> MistralAiEmbedding.EMBEDDING_MODEL;
            case OLLAMA -> OllamaEmbedding.EMBEDDING_MODEL;
            case OPEN_AI -> OpenAiEmbedding.EMBEDDING_MODEL;
            default -> null;
        };
    }
}
