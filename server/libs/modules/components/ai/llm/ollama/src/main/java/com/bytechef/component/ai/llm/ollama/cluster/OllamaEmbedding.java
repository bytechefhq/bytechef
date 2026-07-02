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

package com.bytechef.component.ai.llm.ollama.cluster;

import static com.bytechef.component.ai.llm.constant.LLMConstants.MODEL;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.EMBEDDING_MODEL_PROPERTY;
import static com.bytechef.component.ai.llm.ollama.constant.OllamaConstants.URL;
import static com.bytechef.platform.component.definition.ai.vectorstore.EmbeddingFunction.EMBEDDING;

import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.definition.ai.vectorstore.EmbeddingFunction;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaEmbeddingOptions;

/**
 * @author Ivica Cardic
 */
public class OllamaEmbedding {

    public static final EmbeddingFunction EMBEDDING_MODEL = OllamaEmbedding::apply;

    public static final ClusterElementDefinition<?> CLUSTER_ELEMENT_DEFINITION =
        ComponentDsl.<EmbeddingFunction>clusterElement("embedding")
            .title("Ollama Embedding")
            .description("Ollama embedding.")
            .type(EMBEDDING)
            .object(() -> EMBEDDING_MODEL)
            .properties(EMBEDDING_MODEL_PROPERTY);

    protected static EmbeddingModel apply(Parameters inputParameters, Parameters connectionParameters) {
        String url = connectionParameters.getString(URL);

        OllamaApi ollamaApi = (url == null || url.isEmpty())
            ? OllamaApi.builder()
                .build()
            : OllamaApi.builder()
                .baseUrl(url)
                .build();

        return OllamaEmbeddingModel.builder()
            .ollamaApi(ollamaApi)
            .options(
                OllamaEmbeddingOptions.builder()
                    .model(inputParameters.getRequiredString(MODEL))
                    .build())
            .build();
    }
}
