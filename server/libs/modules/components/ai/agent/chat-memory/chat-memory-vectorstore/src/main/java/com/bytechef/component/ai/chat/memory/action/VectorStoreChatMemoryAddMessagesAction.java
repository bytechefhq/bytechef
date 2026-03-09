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
import static com.bytechef.component.ai.chat.memory.constant.VectorStoreChatMemoryConstants.MESSAGES;
import static com.bytechef.component.ai.chat.memory.constant.VectorStoreChatMemoryConstants.MESSAGE_CONTENT;
import static com.bytechef.component.ai.chat.memory.constant.VectorStoreChatMemoryConstants.MESSAGE_ROLE;
import static com.bytechef.component.ai.chat.memory.constant.VectorStoreChatMemoryConstants.METADATA_CONVERSATION_ID;
import static com.bytechef.component.ai.chat.memory.constant.VectorStoreChatMemoryConstants.METADATA_MESSAGE_TYPE;
import static com.bytechef.component.ai.chat.memory.util.VectorStoreChatMemoryUtils.getVectorStore;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.MultipleConnectionsPerformFunction;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;

/**
 * @author Ivica Cardic
 */
public class VectorStoreChatMemoryAddMessagesAction {

    public static ActionDefinition getActionDefinition(
        ClusterElementDefinitionService clusterElementDefinitionService) {

        return action("addMessages")
            .title("Add Messages")
            .description("Adds messages to the vector store chat memory for a conversation.")
            .properties(
                string(CONVERSATION_ID)
                    .label("Conversation ID")
                    .description("The unique identifier for the conversation.")
                    .required(true),
                array(MESSAGES)
                    .label("Messages")
                    .description("The messages to add to the conversation.")
                    .required(true)
                    .items(
                        object()
                            .properties(
                                string(MESSAGE_ROLE)
                                    .label("Role")
                                    .description("The role of the message sender.")
                                    .required(true)
                                    .options(
                                        option("User", "user"),
                                        option("Assistant", "assistant")),
                                string(MESSAGE_CONTENT)
                                    .label("Content")
                                    .description("The content of the message.")
                                    .required(true))))
            .perform(
                (MultipleConnectionsPerformFunction) (
                    inputParameters, componentConnections, extensions, context) -> perform(inputParameters,
                        componentConnections, extensions, clusterElementDefinitionService));
    }

    private VectorStoreChatMemoryAddMessagesAction() {
    }

    protected static Object perform(
        Parameters inputParameters, Map<String, ComponentConnection> componentConnections,
        Parameters extensions, ClusterElementDefinitionService clusterElementDefinitionService) throws Exception {

        String conversationId = inputParameters.getRequiredString(CONVERSATION_ID);
        Object[] messagesArray = inputParameters.getRequiredArray(MESSAGES);

        VectorStore vectorStore = getVectorStore(extensions, componentConnections, clusterElementDefinitionService);

        List<Document> documents = new ArrayList<>();

        for (Object messageObj : messagesArray) {
            if (messageObj instanceof Map<?, ?> messageMap) {
                String role = (String) messageMap.get(MESSAGE_ROLE);
                String content = (String) messageMap.get(MESSAGE_CONTENT);

                Document document = new Document(
                    content,
                    Map.of(
                        METADATA_CONVERSATION_ID, conversationId,
                        METADATA_MESSAGE_TYPE, role));

                documents.add(document);
            }
        }

        vectorStore.add(documents);

        return Map.of(
            "conversationId", conversationId,
            "messageCount", documents.size());
    }
}
