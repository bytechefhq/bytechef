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

package com.bytechef.component.ai.chat.memory.action;

import static com.bytechef.component.ai.chat.memory.constant.VectorStoreChatMemoryConstants.CONVERSATION_ID;
import static com.bytechef.component.ai.chat.memory.constant.VectorStoreChatMemoryConstants.METADATA_CONVERSATION_ID;
import static com.bytechef.component.ai.chat.memory.constant.VectorStoreChatMemoryConstants.METADATA_MESSAGE_TYPE;
import static com.bytechef.component.ai.chat.memory.constant.VectorStoreChatMemoryConstants.TOP_K;
import static com.bytechef.component.ai.chat.memory.util.VectorStoreChatMemoryUtils.getVectorStore;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.MultipleConnectionsPerformFunction;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;

/**
 * @author Ivica Cardic
 */
public class VectorStoreChatMemoryGetMessagesAction {

    public static ActionDefinition getActionDefinition(
        ClusterElementDefinitionService clusterElementDefinitionService) {

        return action("getMessages")
            .title("Get Messages")
            .description("Retrieves messages from the vector store chat memory for a conversation.")
            .properties(
                string(CONVERSATION_ID)
                    .label("Conversation ID")
                    .description("The unique identifier for the conversation.")
                    .required(true),
                integer(TOP_K)
                    .label("Top K")
                    .description("The maximum number of messages to retrieve.")
                    .defaultValue(100)
                    .required(false))
            .output(
                outputSchema(
                    object()
                        .properties(
                            string(CONVERSATION_ID),
                            array("messages")
                                .items(
                                    object()
                                        .properties(
                                            string("role"),
                                            string("content"))))))
            .perform(
                (MultipleConnectionsPerformFunction) (
                    inputParameters, componentConnections, extensions, context) -> perform(inputParameters,
                        componentConnections, extensions, clusterElementDefinitionService));
    }

    private VectorStoreChatMemoryGetMessagesAction() {
    }

    protected static Object perform(
        Parameters inputParameters, Map<String, ComponentConnection> componentConnections,
        Parameters extensions, ClusterElementDefinitionService clusterElementDefinitionService) throws Exception {

        String conversationId = inputParameters.getRequiredString(CONVERSATION_ID);
        int topK = inputParameters.getInteger(TOP_K, 100);

        VectorStore vectorStore = getVectorStore(extensions, componentConnections, clusterElementDefinitionService);

        FilterExpressionBuilder filterExpressionBuilder = new FilterExpressionBuilder();

        Filter.Expression filterExpression = filterExpressionBuilder
            .eq(METADATA_CONVERSATION_ID, conversationId)
            .build();

        SearchRequest searchRequest = SearchRequest.builder()
            .query("")
            .topK(topK)
            .filterExpression(filterExpression)
            .build();

        List<Document> documents = vectorStore.similaritySearch(searchRequest);

        List<Map<String, String>> messageList = documents.stream()
            .map(VectorStoreChatMemoryGetMessagesAction::toMessageMap)
            .toList();

        return Map.of(
            CONVERSATION_ID, conversationId,
            "messages", messageList);
    }

    private static Map<String, String> toMessageMap(Document document) {
        Map<String, String> map = new HashMap<>();

        Map<String, Object> metadata = document.getMetadata();

        Object messageType = metadata.get(METADATA_MESSAGE_TYPE);

        if (messageType != null) {
            map.put("role", messageType.toString());
        }

        map.put("content", document.getText());

        return map;
    }
}
