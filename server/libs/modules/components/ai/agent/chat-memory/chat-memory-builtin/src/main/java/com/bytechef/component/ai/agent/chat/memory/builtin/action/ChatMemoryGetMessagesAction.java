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

package com.bytechef.component.ai.agent.chat.memory.builtin.action;

import static com.bytechef.component.ai.agent.chat.memory.builtin.constant.ChatMemoryConstants.CONVERSATION_ID;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.ai.agent.chat.memory.builtin.util.ChatMemoryUtils;
import com.bytechef.component.definition.ActionDefinition.PerformFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.session.EventFilter;
import org.springframework.ai.session.SessionEvent;
import org.springframework.ai.session.SessionRepository;

/**
 * @author Ivica Cardic
 */
public class ChatMemoryGetMessagesAction {

    public static ModifiableActionDefinition of(SessionRepository sessionRepository) {
        return action("getMessages")
            .title("Get Messages")
            .description("Retrieves all messages from a conversation.")
            .properties(
                string(CONVERSATION_ID)
                    .label("Conversation ID")
                    .description("The unique identifier for the conversation.")
                    .options(ChatMemoryUtils.getFirstMessages(sessionRepository))
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
            .perform((PerformFunction) (inputParameters, connectionParameters, context) -> perform(
                inputParameters, sessionRepository));
    }

    private ChatMemoryGetMessagesAction() {
    }

    protected static Object perform(Parameters inputParameters, SessionRepository sessionRepository) {
        String conversationId = inputParameters.getRequiredString(CONVERSATION_ID);

        List<SessionEvent> events = sessionRepository.findEvents(conversationId, EventFilter.all());

        List<Map<String, String>> messageList = events.stream()
            .map(SessionEvent::getMessage)
            .map(ChatMemoryGetMessagesAction::toMessageMap)
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
