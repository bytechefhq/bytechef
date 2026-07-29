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
import static com.bytechef.component.ai.agent.chat.memory.builtin.constant.ChatMemoryConstants.DEFAULT_USER_ID;
import static com.bytechef.component.ai.agent.chat.memory.builtin.constant.ChatMemoryConstants.MESSAGES;
import static com.bytechef.component.ai.agent.chat.memory.builtin.constant.ChatMemoryConstants.MESSAGE_CONTENT;
import static com.bytechef.component.ai.agent.chat.memory.builtin.constant.ChatMemoryConstants.MESSAGE_ROLE;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.ai.agent.chat.memory.builtin.util.ChatMemoryUtils;
import com.bytechef.component.definition.ActionDefinition.PerformFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import java.time.Instant;
import java.util.Map;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.session.EventFilter;
import org.springframework.ai.session.Session;
import org.springframework.ai.session.SessionEvent;
import org.springframework.ai.session.SessionRepository;

/**
 * @author Ivica Cardic
 */
public class ChatMemoryAddMessagesAction {

    public static ModifiableActionDefinition of(SessionRepository sessionRepository) {
        return action("addMessages")
            .title("Add Messages")
            .description("Adds messages to the chat memory for a conversation.")
            .properties(
                string(CONVERSATION_ID)
                    .label("Conversation ID")
                    .description("The unique identifier for the conversation.")
                    .options(ChatMemoryUtils.getFirstMessages(sessionRepository))
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
            .output(
                outputSchema(
                    object()
                        .properties(
                            string(CONVERSATION_ID),
                            integer("messageCount"))))
            .perform((PerformFunction) (inputParameters, connectionParameters, context) -> perform(
                inputParameters, sessionRepository));
    }

    private ChatMemoryAddMessagesAction() {
    }

    protected static Object perform(Parameters inputParameters, SessionRepository sessionRepository) {
        String conversationId = inputParameters.getRequiredString(CONVERSATION_ID);
        Object[] messagesArray = inputParameters.getRequiredArray(MESSAGES);

        if (sessionRepository.findById(conversationId)
            .isEmpty()) {

            sessionRepository.save(Session.builder()
                .id(conversationId)
                .userId(DEFAULT_USER_ID)
                .createdAt(Instant.now())
                .build());
        }

        for (Object messageObject : messagesArray) {
            if (messageObject instanceof Map<?, ?> messageMap) {
                String role = (String) messageMap.get(MESSAGE_ROLE);
                String content = (String) messageMap.get(MESSAGE_CONTENT);

                sessionRepository.appendEvent(SessionEvent.builder()
                    .sessionId(conversationId)
                    .message(createMessage(role, content))
                    .build());
            }
        }

        int messageCount = sessionRepository.findEvents(conversationId, EventFilter.all())
            .size();

        return Map.of(
            CONVERSATION_ID, conversationId,
            "messageCount", messageCount);
    }

    private static Message createMessage(String role, String content) {
        return switch (role) {
            case "user" -> new UserMessage(content);
            case "assistant" -> new AssistantMessage(content);
            default -> throw new IllegalArgumentException(
                "Unsupported role: " + role + ". Supported roles are: user, assistant.");
        };
    }
}
