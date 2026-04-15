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

package com.bytechef.component.ai.vectorstore.knowledgebase.cluster;

import static com.bytechef.component.ai.vectorstore.constant.VectorStoreConstants.QUERY_PROPERTY;
import static com.bytechef.component.ai.vectorstore.constant.VectorStoreConstants.SEARCH_PROPERTIES;
import static com.bytechef.component.ai.vectorstore.knowledgebase.cluster.KnowledgeBaseVectorStore.createVectorStore;
import static com.bytechef.component.ai.vectorstore.knowledgebase.constant.KnowledgeBaseVectorStoreConstants.KNOWLEDGE_BASE;
import static com.bytechef.component.definition.ai.agent.BaseToolFunction.TOOLS;

import com.bytechef.component.ai.vectorstore.VectorStore;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.definition.ai.agent.MultipleConnectionsToolFunction;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import java.util.Map;
import java.util.stream.Stream;

/**
 * @author Ivica Cardic
 */
public class KnowledgeBaseSearchTool {

    @SuppressWarnings("PMD.UnusedFormalParameter")
    public static ClusterElementDefinition<MultipleConnectionsToolFunction> of(
        org.springframework.ai.vectorstore.VectorStore vectorStore,
        ClusterElementDefinitionService clusterElementDefinitionService) {

        VectorStore kbVectorStore = createVectorStore(vectorStore);

        return ComponentDsl.<MultipleConnectionsToolFunction>clusterElement("search")
            .title("Knowledge Base Search")
            .description("Search data from the knowledge base.")
            .type(TOOLS)
            .properties(
                Stream
                    .of(Stream.of(QUERY_PROPERTY), SEARCH_PROPERTIES.stream())
                    .flatMap(stream -> stream)
                    .toList())
            .object(() -> (MultipleConnectionsToolFunction) (
                inputParameters, connectionParameters, extensions, componentConnections, context) -> {
                ComponentConnection vectorStoreComponentConnection = componentConnections.get(KNOWLEDGE_BASE);

                return kbVectorStore.search(
                    inputParameters,
                    ParametersFactory.create(
                        vectorStoreComponentConnection == null
                            ? Map.of() : vectorStoreComponentConnection.getParameters()),
                    null);
            });
    }
}
