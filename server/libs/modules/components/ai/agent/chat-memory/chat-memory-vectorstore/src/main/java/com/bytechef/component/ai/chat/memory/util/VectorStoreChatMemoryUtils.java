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

package com.bytechef.component.ai.chat.memory.util;

import static com.bytechef.component.ai.chat.memory.constant.VectorStoreChatMemoryConstants.METADATA_CONVERSATION_ID;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.platform.component.definition.ai.agent.VectorStoreFunction.VECTOR_STORE;

import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.MultipleConnectionsOptionsFunction;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.definition.ai.agent.VectorStoreFunction;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.configuration.domain.ClusterElement;
import com.bytechef.platform.configuration.domain.ClusterElementMap;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

/**
 * @author Ivica Cardic
 */
public class VectorStoreChatMemoryUtils {

    public static VectorStore getVectorStore(
        Parameters extensions, Map<String, ComponentConnection> componentConnections,
        ClusterElementDefinitionService clusterElementDefinitionService) throws Exception {

        ClusterElement clusterElement = ClusterElementMap.of(extensions)
            .getClusterElement(VECTOR_STORE);

        VectorStoreFunction vectorStoreFunction = clusterElementDefinitionService.getClusterElement(
            clusterElement.getComponentName(), clusterElement.getComponentVersion(),
            clusterElement.getClusterElementName());

        ComponentConnection componentConnection = componentConnections.get(clusterElement.getWorkflowNodeName());

        Map<String, ?> componentConnectionParameters = componentConnection.getParameters();

        return vectorStoreFunction.apply(
            ParametersFactory.create(clusterElement.getParameters()),
            ParametersFactory.create(componentConnectionParameters),
            ParametersFactory.create(clusterElement.getExtensions()),
            componentConnections);
    }

    public static MultipleConnectionsOptionsFunction<String> getFirstMessages(ClusterElementDefinitionService clusterElementDefinitionService) {
        return (inputParameters, componentConnections, extensions, context) -> {
            VectorStore vectorStore = getVectorStore(extensions, componentConnections, clusterElementDefinitionService);

            SearchRequest searchRequest = SearchRequest.builder()
                .query(" ")
                .topK(10000)
                .build();

            List<Document> documents = vectorStore.similaritySearch(searchRequest);

            List<ComponentDsl.ModifiableOption<String>> options = new ArrayList<>();
            Set<String> seenConversationIds = new LinkedHashSet<>();

            for (Document document : documents) {
                Map<String, Object> metadata = document.getMetadata();
                Object conversationIdObj = metadata.get(METADATA_CONVERSATION_ID);

                if (conversationIdObj != null) {
                    String conversationId = conversationIdObj.toString();

                    if (seenConversationIds.add(conversationId)) {
                        options.add(option(conversationId, conversationId, document.getText()));
                    }
                }
            }

            return options;
        };
    }

    private VectorStoreChatMemoryUtils() {
    }
}
