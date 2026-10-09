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

package com.bytechef.component.ai.agent.chat.memory.session.action;

import static com.bytechef.component.ai.agent.chat.memory.session.constant.SessionChatMemoryConstants.CONVERSATION_ID;
import static com.bytechef.component.ai.agent.chat.memory.session.constant.SessionChatMemoryConstants.DEFAULT_USER_ID_VALUE;
import static com.bytechef.component.ai.agent.chat.memory.session.constant.SessionChatMemoryConstants.INCLUDE_COMPACTED_HISTORY;
import static com.bytechef.component.ai.agent.chat.memory.session.constant.SessionChatMemoryConstants.MESSAGES;
import static com.bytechef.component.ai.agent.chat.memory.session.constant.SessionChatMemoryConstants.MESSAGE_CONTENT;
import static com.bytechef.component.ai.agent.chat.memory.session.constant.SessionChatMemoryConstants.MESSAGE_ROLE;
import static com.bytechef.component.ai.agent.chat.memory.session.constant.SessionChatMemoryConstants.USER_ID;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.ai.agent.chat.memory.session.SessionRepositoryResolver;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ActionDefinition.PerformFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableOption;
import com.bytechef.component.definition.ComponentDsl.ModifiableStringProperty;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.definition.MultipleConnectionsOptionsFunction;
import com.bytechef.platform.component.definition.MultipleConnectionsPerformFunction;
import com.bytechef.platform.component.definition.ParametersFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.session.CreateSessionRequest;
import org.springframework.ai.session.DefaultSessionService;
import org.springframework.ai.session.Session;
import org.springframework.ai.session.SessionRepository;
import org.springframework.ai.session.SessionService;

/**
 * @author Ivica Cardic
 */
public final class SessionChatMemoryActions {

    private SessionChatMemoryActions() {
    }

    public static List<ActionDefinition> of(
        String componentKebabName, SessionRepositoryResolver sessionRepositoryResolver, boolean multipleConnections) {

        return List.of(
            addMessages(componentKebabName, sessionRepositoryResolver, multipleConnections),
            getMessages(componentKebabName, sessionRepositoryResolver, multipleConnections),
            deleteConversation(componentKebabName, sessionRepositoryResolver, multipleConnections),
            listConversations(componentKebabName, sessionRepositoryResolver, multipleConnections));
    }

    private static ActionDefinition addMessages(
        String componentKebabName, SessionRepositoryResolver sessionRepositoryResolver, boolean multipleConnections) {

        ModifiableActionDefinition actionDefinition = action("addMessages")
            .title("Add Messages")
            .description("Adds messages to a conversation, creating the conversation if it does not exist.")
            .properties(
                conversationIdProperty(sessionRepositoryResolver, multipleConnections),
                userIdProperty("User id assigned to the conversation when it is created."),
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
            .help("", getHelpUrl(componentKebabName, "add-messages"));

        return withPerform(
            actionDefinition, sessionRepositoryResolver, multipleConnections,
            SessionChatMemoryActions::performAddMessages);
    }

    private static ActionDefinition getMessages(
        String componentKebabName, SessionRepositoryResolver sessionRepositoryResolver, boolean multipleConnections) {

        ModifiableActionDefinition actionDefinition = action("getMessages")
            .title("Get Messages")
            .description("Retrieves the messages of a conversation.")
            .properties(
                conversationIdProperty(sessionRepositoryResolver, multipleConnections),
                bool(INCLUDE_COMPACTED_HISTORY)
                    .label("Include compacted history")
                    .description("When off, only the messages the agent currently sees are returned.")
                    .defaultValue(true)
                    .required(false))
            .output(
                outputSchema(
                    object()
                        .properties(
                            string(CONVERSATION_ID),
                            array(MESSAGES)
                                .items(
                                    object()
                                        .properties(
                                            string(MESSAGE_ROLE),
                                            string(MESSAGE_CONTENT))))))
            .help("", getHelpUrl(componentKebabName, "get-messages"));

        return withPerform(
            actionDefinition, sessionRepositoryResolver, multipleConnections,
            SessionChatMemoryActions::performGetMessages);
    }

    private static ActionDefinition deleteConversation(
        String componentKebabName, SessionRepositoryResolver sessionRepositoryResolver, boolean multipleConnections) {

        ModifiableActionDefinition actionDefinition = action("deleteConversation")
            .title("Delete Conversation")
            .description("Deletes a conversation and all of its messages.")
            .properties(conversationIdProperty(sessionRepositoryResolver, multipleConnections))
            .output(
                outputSchema(
                    object()
                        .properties(
                            string(CONVERSATION_ID),
                            bool("deleted"))))
            .help("", getHelpUrl(componentKebabName, "delete-conversation"));

        return withPerform(
            actionDefinition, sessionRepositoryResolver, multipleConnections,
            SessionChatMemoryActions::performDeleteConversation);
    }

    private static ActionDefinition listConversations(
        String componentKebabName, SessionRepositoryResolver sessionRepositoryResolver, boolean multipleConnections) {

        ModifiableActionDefinition actionDefinition = action("listConversations")
            .title("List Conversations")
            .description("Lists the conversation IDs of a user.")
            .properties(userIdProperty("Lists the conversations of this user."))
            .output(
                outputSchema(
                    object()
                        .properties(
                            array("conversationIds")
                                .items(string()),
                            integer("count"))))
            .help("", getHelpUrl(componentKebabName, "list-conversations"));

        return withPerform(
            actionDefinition, sessionRepositoryResolver, multipleConnections,
            SessionChatMemoryActions::performListConversations);
    }

    static Object performAddMessages(Parameters inputParameters, SessionRepository sessionRepository) {
        String conversationId = inputParameters.getRequiredString(CONVERSATION_ID);

        List<Message> newMessages = new ArrayList<>();

        for (Object messageObject : inputParameters.getRequiredArray(MESSAGES)) {
            if (messageObject instanceof Map<?, ?> messageMap) {
                newMessages.add(
                    createMessage((String) messageMap.get(MESSAGE_ROLE), (String) messageMap.get(MESSAGE_CONTENT)));
            }
        }

        SessionService sessionService = createSessionService(sessionRepository);

        if (sessionService.findById(conversationId) == null) {
            sessionService.create(
                CreateSessionRequest.builder()
                    .id(conversationId)
                    .userId(inputParameters.getString(USER_ID, DEFAULT_USER_ID_VALUE))
                    .build());
        }

        for (Message message : newMessages) {
            sessionService.appendMessage(conversationId, message);
        }

        List<Message> messages = sessionService.getMessages(conversationId);

        return Map.of(CONVERSATION_ID, conversationId, "messageCount", messages.size());
    }

    static Object performGetMessages(Parameters inputParameters, SessionRepository sessionRepository) {
        String conversationId = inputParameters.getRequiredString(CONVERSATION_ID);

        SessionService sessionService = createSessionService(sessionRepository);

        if (sessionService.findById(conversationId) == null) {
            return Map.of(CONVERSATION_ID, conversationId, MESSAGES, List.of());
        }

        List<Message> messages = inputParameters.getBoolean(INCLUDE_COMPACTED_HISTORY, true)
            ? sessionService.getMessages(conversationId) : sessionService.getActiveMessages(conversationId);

        List<Map<String, String>> messageMaps = messages.stream()
            .map(SessionChatMemoryActions::toMessageMap)
            .toList();

        return Map.of(CONVERSATION_ID, conversationId, MESSAGES, messageMaps);
    }

    static Object performDeleteConversation(Parameters inputParameters, SessionRepository sessionRepository) {
        String conversationId = inputParameters.getRequiredString(CONVERSATION_ID);

        boolean deleted = sessionRepository.findById(conversationId) != null;

        if (deleted) {
            sessionRepository.delete(conversationId);
        }

        return Map.of(CONVERSATION_ID, conversationId, "deleted", deleted);
    }

    static Object performListConversations(Parameters inputParameters, SessionRepository sessionRepository) {
        List<Session> sessions = sessionRepository.findByUserId(
            inputParameters.getString(USER_ID, DEFAULT_USER_ID_VALUE));

        List<String> conversationIds = sessions.stream()
            .map(Session::id)
            .toList();

        return Map.of("conversationIds", conversationIds, "count", conversationIds.size());
    }

    private static ActionDefinition withPerform(
        ModifiableActionDefinition actionDefinition, SessionRepositoryResolver sessionRepositoryResolver,
        boolean multipleConnections, SessionPerformFunction sessionPerformFunction) {

        if (multipleConnections) {
            return actionDefinition.perform(
                (MultipleConnectionsPerformFunction) (
                    inputParameters, componentConnections, extensions, context) -> sessionPerformFunction.apply(
                        inputParameters,
                        sessionRepositoryResolver.resolve(
                            inputParameters, ParametersFactory.create(Map.of()), extensions, componentConnections)));
        }

        return actionDefinition.perform(
            (PerformFunction) (inputParameters, connectionParameters, context) -> sessionPerformFunction.apply(
                inputParameters,
                sessionRepositoryResolver.resolve(
                    inputParameters, connectionParameters, ParametersFactory.create(Map.of()), Map.of())));
    }

    private static ModifiableStringProperty conversationIdProperty(
        SessionRepositoryResolver sessionRepositoryResolver, boolean multipleConnections) {

        ModifiableStringProperty conversationIdProperty = string(CONVERSATION_ID)
            .label("Conversation ID")
            .description("The unique identifier for the conversation.")
            .required(true);

        if (multipleConnections) {
            return conversationIdProperty.options(
                (MultipleConnectionsOptionsFunction<String>) (
                    inputParameters, componentConnections, extensions, context) -> getConversationOptions(
                        sessionRepositoryResolver.resolve(
                            inputParameters, ParametersFactory.create(Map.of()), extensions, componentConnections)));
        }

        return conversationIdProperty.options(
            (ActionDefinition.OptionsFunction<String>) (
                inputParameters, connectionParameters, lookupDependsOnPaths, searchText,
                context) -> getConversationOptions(
                    sessionRepositoryResolver.resolve(
                        inputParameters, connectionParameters, ParametersFactory.create(Map.of()), Map.of())));
    }

    private static List<ModifiableOption<String>> getConversationOptions(SessionRepository sessionRepository) {
        SessionService sessionService = createSessionService(sessionRepository);

        List<ModifiableOption<String>> options = new ArrayList<>();

        for (Session session : sessionRepository.findByUserId(DEFAULT_USER_ID_VALUE)) {
            String conversationId = session.id();

            Optional<Message> firstUserMessage = sessionService.getMessages(conversationId)
                .stream()
                .filter(message -> message.getMessageType() == MessageType.USER)
                .findFirst();

            firstUserMessage.ifPresent(
                message -> options.add(option(conversationId, conversationId, message.getText())));
        }

        return options;
    }

    private static ModifiableStringProperty userIdProperty(String description) {
        return string(USER_ID)
            .label("User ID")
            .description(description)
            .defaultValue(DEFAULT_USER_ID_VALUE)
            .required(false);
    }

    private static Message createMessage(String role, String content) {
        return switch (role) {
            case "user" -> new UserMessage(content);
            case "assistant" -> new AssistantMessage(content);
            default -> throw new IllegalArgumentException(
                "Unsupported role: " + role + ". Supported roles are: user, assistant.");
        };
    }

    private static Map<String, String> toMessageMap(Message message) {
        Map<String, String> messageMap = new HashMap<>();

        MessageType messageType = message.getMessageType();

        String role = switch (messageType) {
            case USER -> "user";
            case ASSISTANT -> "assistant";
            case SYSTEM -> "system";
            default -> messageType.getValue();
        };

        messageMap.put(MESSAGE_ROLE, role);
        messageMap.put(MESSAGE_CONTENT, message.getText());

        return messageMap;
    }

    private static String getHelpUrl(String componentKebabName, String actionKebabName) {
        return "https://docs.bytechef.io/reference/components/" + componentKebabName + "_v2#" + actionKebabName;
    }

    private static SessionService createSessionService(SessionRepository sessionRepository) {
        return DefaultSessionService.builder()
            .sessionRepository(sessionRepository)
            .build();
    }

    @FunctionalInterface
    private interface SessionPerformFunction {

        Object apply(Parameters inputParameters, SessionRepository sessionRepository) throws Exception;
    }
}
