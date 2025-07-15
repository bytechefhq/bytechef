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

package com.bytechef.component.ai.chat.memory.cluster;

import static com.bytechef.platform.component.definition.ai.agent.ChatMemoryFunction.CHAT_MEMORY;
import static com.bytechef.platform.component.definition.ai.agent.VectorStoreFunction.VECTOR_STORE;

import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.definition.ai.agent.ChatMemoryFunction;
import com.bytechef.platform.component.definition.ai.agent.VectorStoreFunction;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.configuration.domain.ClusterElement;
import com.bytechef.platform.configuration.domain.ClusterElementMap;
import java.util.Map;
import org.springframework.ai.chat.client.advisor.vectorstore.VectorStoreChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.VectorStoreChatMemoryAdvisor.Builder;

/**
 * @author Ivica Cardic
 */
public class VectorStoreChatMemory {

    private static final String CHAT_MEMORY_RETRIEVE_SIZE = "chatMemoryRetrieveSize";

    public final ClusterElementDefinition<ChatMemoryFunction> clusterElementDefinition =
        ComponentDsl.<ChatMemoryFunction>clusterElement("chatMemory")
            .title("Vector Store Chat Memory")
            .description(
                "Memory is retrieved from a VectorStore added into the prompt's system text. This only works for " +
                    "text based exchanges with the models, not multi-modal exchanges.")
            .type(CHAT_MEMORY)
            .properties(
                ComponentDsl.integer(CHAT_MEMORY_RETRIEVE_SIZE)
                    .label("Chat Memory Retrieve Size")
                    .description("The number of messages to retrieve from the vector store.")
                    .defaultValue(20))
            .object(() -> this::apply);

    private final ClusterElementDefinitionService clusterElementDefinitionService;

    public VectorStoreChatMemory(ClusterElementDefinitionService clusterElementDefinitionService) {
        this.clusterElementDefinitionService = clusterElementDefinitionService;
    }

    protected VectorStoreChatMemoryAdvisor apply(
        Parameters inputParameters, Parameters connectionParameters, Parameters extensions,
        Map<String, ComponentConnection> componentConnections) throws Exception {

        ClusterElement clusterElement = ClusterElementMap.of(extensions)
            .getClusterElement(VECTOR_STORE);

        VectorStoreFunction vectorStoreFunction = clusterElementDefinitionService.getClusterElement(
            clusterElement.getComponentName(), clusterElement.getComponentVersion(),
            clusterElement.getClusterElementName());

        ComponentConnection componentConnection = componentConnections.get(clusterElement.getWorkflowNodeName());

        Map<String, ?> componentConnectionConnectionParameters = componentConnection.getParameters();

        Builder builder = VectorStoreChatMemoryAdvisor
            .builder(
                vectorStoreFunction.apply(
                    ParametersFactory.createParameters(clusterElement.getParameters()),
                    ParametersFactory.createParameters(componentConnectionConnectionParameters),
                    ParametersFactory.createParameters(clusterElement.getExtensions()), componentConnections))
            .defaultTopK(
                inputParameters.getInteger(CHAT_MEMORY_RETRIEVE_SIZE, 20));

        return builder.build();
    }
}
