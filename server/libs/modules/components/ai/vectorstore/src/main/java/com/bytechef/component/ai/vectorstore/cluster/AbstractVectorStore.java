/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.component.ai.vectorstore.cluster;

import static com.bytechef.platform.component.definition.ai.agent.VectorStoreFunction.VECTOR_STORE;

import com.bytechef.component.ai.vectorstore.VectorStore;
import com.bytechef.component.ai.vectorstore.util.VectorStoreUtils;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.definition.ai.agent.VectorStoreFunction;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import java.util.Map;
import org.springframework.ai.embedding.EmbeddingModel;

/**
 * @author Ivica Cardic
 */
public abstract class AbstractVectorStore {

    public final ClusterElementDefinition<VectorStoreFunction> clusterElementDefinition;

    private final VectorStore vectorStore;
    private final ClusterElementDefinitionService clusterElementDefinitionService;

    public AbstractVectorStore(
        String title, VectorStore vectorStore, ClusterElementDefinitionService clusterElementDefinitionService) {

        this.clusterElementDefinitionService = clusterElementDefinitionService;
        this.vectorStore = vectorStore;

        this.clusterElementDefinition = ComponentDsl.<VectorStoreFunction>clusterElement("vectorStore")
            .title("%s VectorStore".formatted(title))
            .description("%s VectorStore.".formatted(title))
            .type(VECTOR_STORE)
            .object(() -> this::apply);
    }

    protected org.springframework.ai.vectorstore.VectorStore apply(
        Parameters inputParameters, Parameters connectionParameters, Parameters extensions,
        Map<String, ComponentConnection> componentConnections) {

        EmbeddingModel embeddingModel = VectorStoreUtils.getEmbeddingModel(
            extensions, componentConnections, clusterElementDefinitionService);

        return vectorStore.createVectorStore(
            ParametersFactory.createParameters(connectionParameters),
            embeddingModel);
    }
}
