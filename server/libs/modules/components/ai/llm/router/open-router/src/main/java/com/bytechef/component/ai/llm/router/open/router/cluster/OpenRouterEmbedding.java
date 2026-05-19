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

package com.bytechef.component.ai.llm.router.open.router.cluster;

import static com.bytechef.component.ai.llm.router.constant.RouterConstants.DIMENSION;
import static com.bytechef.component.ai.llm.router.open.router.constant.OpenRouterConstants.BASE_URL;
import static com.bytechef.component.ai.llm.router.open.router.constant.OpenRouterConstants.EMBEDDING_MODEL_PROPERTY;
import static com.bytechef.component.definition.ComponentDsl.integer;

import com.bytechef.component.ai.llm.router.cluster.RouterEmbedding;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.definition.ai.vectorstore.EmbeddingFunction;
import org.springframework.ai.embedding.EmbeddingModel;

/**
 * @author Marko Kriskovic
 */
public class OpenRouterEmbedding {

    public static final ClusterElementDefinition<?> CLUSTER_ELEMENT_DEFINITION =
        ComponentDsl.<EmbeddingFunction>clusterElement("embedding")
            .title("Open Router Embedding")
            .description("Open Router embedding.")
            .type(EmbeddingFunction.EMBEDDING)
            .object(() -> OpenRouterEmbedding::apply)
            .properties(EMBEDDING_MODEL_PROPERTY,
                integer(DIMENSION)
                    .label("Dimension")
                    .description("The number of dimensions for the output embeddings")
                    .minValue(1)
                    .defaultValue(1536)
                    .required(false));

    protected static EmbeddingModel apply(Parameters inputParameters, Parameters connectionParameters) {
        return RouterEmbedding.createEmbeddingModel(BASE_URL, inputParameters, connectionParameters);
    }
}
