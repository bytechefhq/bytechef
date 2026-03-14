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

package com.bytechef.component.ai.agent.chat.memory.jdbc.action;

import static com.bytechef.component.ai.agent.chat.memory.jdbc.constant.JdbcChatMemoryConstants.CONVERSATION_ID;
import static com.bytechef.component.ai.agent.chat.memory.jdbc.util.JdbcChatMemoryUtils.getChatMemoryRepository;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
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
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;

/**
 * @author Ivica Cardic
 */
public class JdbcChatMemoryGetMessagesAction {

    public static ActionDefinition getActionDefinition(
        ClusterElementDefinitionService clusterElementDefinitionService) {

        return action("getMessages")
            .title("Get Messages")
            .description("Retrieves all messages from a conversation.")
            .properties(
                string(CONVERSATION_ID)
                    .label("Conversation ID")
                    .description("The unique identifier for the conversation.")
                    .required(true))
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

    private JdbcChatMemoryGetMessagesAction() {
    }

    protected static Object perform(
        Parameters inputParameters, Map<String, ComponentConnection> componentConnections,
        Parameters extensions, ClusterElementDefinitionService clusterElementDefinitionService) throws Exception {

        String conversationId = inputParameters.getRequiredString(CONVERSATION_ID);

        ChatMemoryRepository repository = getChatMemoryRepository(
            extensions, componentConnections, clusterElementDefinitionService);

        List<Message> messages = repository.findByConversationId(conversationId);

        List<Map<String, String>> messageList = messages.stream()
            .map(JdbcChatMemoryGetMessagesAction::toMessageMap)
            .toList();

        return Map.of(
            CONVERSATION_ID, conversationId,
            "messages", messageList);
    }

    private static Map<String, String> toMessageMap(Message message) {
        Map<String, String> map = new HashMap<>();

        MessageType messageType = message.getMessageType();

        if (messageType == MessageType.USER) {
            map.put("role", "user");
        } else if (messageType == MessageType.ASSISTANT) {
            map.put("role", "assistant");
        } else if (messageType == MessageType.SYSTEM) {
            map.put("role", "system");
        } else {
            map.put("role", messageType.getValue());
        }

        map.put("content", message.getText());

        return map;
    }
}
