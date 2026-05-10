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
import static com.bytechef.component.ai.chat.memory.constant.VectorStoreChatMemoryConstants.METADATA_TIMESTAMP;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.platform.component.definition.ai.agent.VectorStoreFunction.VECTOR_STORE;

import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.ClusterElementContextAware;
import com.bytechef.platform.component.definition.MultipleConnectionsOptionsFunction;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.definition.ai.agent.VectorStoreFunction;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.configuration.domain.ClusterElement;
import com.bytechef.platform.configuration.domain.ClusterElementMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

/**
 * @author Ivica Cardic
 */
public class VectorStoreChatMemoryUtils {

    private VectorStoreChatMemoryUtils() {
    }

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
            ParametersFactory.create(clusterElement.getExtensions()), componentConnections);
    }

    public static ClusterElementDefinition.OptionsFunction<String> getClusterElementFirstMessages() {
        return (inputParameters, connectionParameters, lookupDependsOnPaths, searchText, context) -> {
            VectorStore vectorStore = ((ClusterElementContextAware) context).resolveClusterElement(
                VECTOR_STORE,
                (
                    vectorStoreFn, elementInputParams, elementConnectionParams, elementExtensions,
                    elementComponentConnections, ctx) -> {
                    try {
                        return ((VectorStoreFunction) vectorStoreFn).apply(
                            elementInputParams, elementConnectionParams,
                            elementExtensions, elementComponentConnections);
                    } catch (Exception exception) {
                        context.log(
                            log -> log.error("Failed to resolve VectorStore for conversation ID options", exception));

                        return null;
                    }
                });

            if (vectorStore == null) {
                return List.of();
            }

            SearchRequest searchRequest = SearchRequest.builder()
                .query(" ")
                .topK(10000)
                .build();

            List<Document> documents = vectorStore.similaritySearch(searchRequest);

            Map<String, Long> maxTimestampByConversation = new HashMap<>();
            Map<String, String> firstTextByConversation = new HashMap<>();

            for (Document document : documents) {
                Map<String, Object> metadata = document.getMetadata();
                Object conversationIdObj = metadata.get(METADATA_CONVERSATION_ID);

                if (conversationIdObj == null) {
                    continue;
                }

                String conversationId = conversationIdObj.toString();
                Object timestampObj = metadata.get(METADATA_TIMESTAMP);
                long timestamp = timestampObj instanceof Number number ? number.longValue() : 0L;

                if (!maxTimestampByConversation.containsKey(conversationId) ||
                    timestamp > maxTimestampByConversation.get(conversationId)) {

                    maxTimestampByConversation.put(conversationId, timestamp);
                    firstTextByConversation.put(conversationId, document.getText());
                }
            }

            List<ComponentDsl.ModifiableOption<String>> options = new ArrayList<>();

            maxTimestampByConversation.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .forEach(entry -> options.add(
                    option(entry.getKey(), entry.getKey(), firstTextByConversation.get(entry.getKey()))));

            return options;
        };
    }

    public static MultipleConnectionsOptionsFunction<String> getFirstMessages(
        ClusterElementDefinitionService clusterElementDefinitionService) {

        return (inputParameters, componentConnections, extensions, context) -> {
            VectorStore vectorStore = getVectorStore(extensions, componentConnections, clusterElementDefinitionService);

            SearchRequest searchRequest = SearchRequest.builder()
                .query(" ")
                .topK(10000)
                .build();

            List<Document> documents = vectorStore.similaritySearch(searchRequest);

            Map<String, Long> maxTimestampByConversation = new HashMap<>();
            Map<String, String> firstTextByConversation = new HashMap<>();

            for (Document document : documents) {
                Map<String, Object> metadata = document.getMetadata();
                Object conversationIdObj = metadata.get(METADATA_CONVERSATION_ID);

                if (conversationIdObj == null) {
                    continue;
                }

                String conversationId = conversationIdObj.toString();
                Object timestampObj = metadata.get(METADATA_TIMESTAMP);

                long timestamp = timestampObj instanceof Number number ? number.longValue() : 0L;

                if (!maxTimestampByConversation.containsKey(conversationId) ||
                    timestamp > maxTimestampByConversation.get(conversationId)) {

                    maxTimestampByConversation.put(conversationId, timestamp);
                    firstTextByConversation.put(conversationId, document.getText());
                }
            }

            List<ComponentDsl.ModifiableOption<String>> options = new ArrayList<>();

            maxTimestampByConversation.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .forEach(entry -> options.add(
                    option(entry.getKey(), entry.getKey(), firstTextByConversation.get(entry.getKey()))));

            return options;
        };
    }
}
