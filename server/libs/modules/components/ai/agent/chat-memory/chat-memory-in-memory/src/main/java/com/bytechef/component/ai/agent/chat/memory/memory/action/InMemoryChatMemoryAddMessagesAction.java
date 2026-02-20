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

package com.bytechef.component.ai.agent.chat.memory.memory.action;

import static com.bytechef.component.ai.agent.chat.memory.memory.constant.InMemoryChatMemoryConstants.CONVERSATION_ID;
import static com.bytechef.component.ai.agent.chat.memory.memory.constant.InMemoryChatMemoryConstants.MESSAGES;
import static com.bytechef.component.ai.agent.chat.memory.memory.constant.InMemoryChatMemoryConstants.MESSAGE_CONTENT;
import static com.bytechef.component.ai.agent.chat.memory.memory.constant.InMemoryChatMemoryConstants.MESSAGE_ROLE;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.ai.agent.chat.memory.memory.InMemoryChatMemoryRepositoryHolder;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

/**
 * @author Ivica Cardic
 */
public class InMemoryChatMemoryAddMessagesAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("addMessages")
        .title("Add Messages")
        .description("Adds messages to the chat memory for a conversation.")
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
        .perform(InMemoryChatMemoryAddMessagesAction::perform);

    private InMemoryChatMemoryAddMessagesAction() {
    }

    @SuppressWarnings("unchecked")
    protected static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        String conversationId = inputParameters.getRequiredString(CONVERSATION_ID);
        Object[] messagesArray = inputParameters.getRequiredArray(MESSAGES);

        ChatMemoryRepository repository = InMemoryChatMemoryRepositoryHolder.getInstance();
        List<Message> existingMessages = new ArrayList<>(repository.findByConversationId(conversationId));

        for (Object messageObj : messagesArray) {
            if (messageObj instanceof Map<?, ?> messageMap) {
                String role = (String) messageMap.get(MESSAGE_ROLE);
                String content = (String) messageMap.get(MESSAGE_CONTENT);
                Message message = createMessage(role, content);

                existingMessages.add(message);
            }
        }

        repository.saveAll(conversationId, existingMessages);

        return Map.of(
            "conversationId", conversationId,
            "messageCount", existingMessages.size());
    }

    private static Message createMessage(String role, String content) {
        return switch (role) {
            case "user" -> new UserMessage(content);
            case "assistant" -> new AssistantMessage(content);
            default -> throw new IllegalArgumentException("Unsupported role: " + role);
        };
    }
}
