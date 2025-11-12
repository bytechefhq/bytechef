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

package com.bytechef.component.ai.llm.mistral.cluster;

import static com.bytechef.component.ai.llm.constant.LLMConstants.MODEL;
import static com.bytechef.component.ai.llm.mistral.constant.MistralConstants.EMBEDDING_MODELS;
import static com.bytechef.component.definition.Authorization.TOKEN;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.platform.component.definition.ai.vectorstore.EmbeddingFunction.EMBEDDING;

import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.definition.ai.vectorstore.EmbeddingFunction;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.mistralai.MistralAiEmbeddingModel;
import org.springframework.ai.mistralai.MistralAiEmbeddingOptions;
import org.springframework.ai.mistralai.api.MistralAiApi;

/**
 * @author Ivica Cardic
 */
public class MistralAiEmbedding {

    public static final ClusterElementDefinition<?> CLUSTER_ELEMENT_DEFINITION =
        ComponentDsl.<EmbeddingFunction>clusterElement("embedding")
            .title("Mistral Embedding")
            .description("Mistral embedding.")
            .type(EMBEDDING)
            .object(() -> MistralAiEmbedding::apply)
            .properties(
                string(MODEL)
                    .label("Model")
                    .description("ID of the model to use.")
                    .required(true)
                    .options(EMBEDDING_MODELS));

    protected static EmbeddingModel apply(Parameters inputParameters, Parameters connectionParameters) {
        return MistralAiEmbeddingModel.builder()
            .mistralAiApi(
                MistralAiApi.builder()
                    .apiKey(connectionParameters.getRequiredString(TOKEN))
                    .build())
            .options(
                MistralAiEmbeddingOptions.builder()
                    .withModel(inputParameters.getRequiredString(MODEL))
                    .build())
            .build();
    }
}
