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
import static com.bytechef.component.ai.agent.chat.memory.session.constant.SessionChatMemoryConstants.INCLUDE_COMPACTED_HISTORY;
import static com.bytechef.component.ai.agent.chat.memory.session.constant.SessionChatMemoryConstants.MESSAGES;
import static com.bytechef.component.ai.agent.chat.memory.session.constant.SessionChatMemoryConstants.MESSAGE_CONTENT;
import static com.bytechef.component.ai.agent.chat.memory.session.constant.SessionChatMemoryConstants.MESSAGE_ROLE;
import static com.bytechef.component.ai.agent.chat.memory.session.constant.SessionChatMemoryConstants.USER_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ActionDefinition.PerformFunction;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.session.InMemorySessionRepository;
import org.springframework.ai.session.Session;
import org.springframework.ai.session.SessionEvent;
import org.springframework.ai.session.SessionRepository;

/**
 * @author Ivica Cardic
 */
class SessionChatMemoryActionsTest {

    private final SessionRepository sessionRepository = InMemorySessionRepository.builder()
        .build();
    private final List<ActionDefinition> actionDefinitions = SessionChatMemoryActions.of(
        "test-chat-memory",
        (inputParameters, connectionParameters, extensions, componentConnections) -> sessionRepository, false);

    @Test
    void testAddMessagesCreatesTheSessionForTheUserAndAppends() throws Exception {
        Map<String, Object> result = performForMap(
            "addMessages",
            Map.of(
                CONVERSATION_ID, "conversation-1", USER_ID, "user-1",
                MESSAGES, List.of(
                    Map.of(MESSAGE_ROLE, "user", MESSAGE_CONTENT, "hello"),
                    Map.of(MESSAGE_ROLE, "assistant", MESSAGE_CONTENT, "hi"))));

        assertThat(result).containsEntry(CONVERSATION_ID, "conversation-1")
            .containsEntry("messageCount", 2);

        Session session = sessionRepository.findById("conversation-1");

        assertThat(session).isNotNull();
        assertThat(session.userId()).isEqualTo("user-1");

        Map<String, Object> appended = performForMap(
            "addMessages",
            Map.of(
                CONVERSATION_ID, "conversation-1",
                MESSAGES, List.of(Map.of(MESSAGE_ROLE, "user", MESSAGE_CONTENT, "again"))));

        assertThat(appended).containsEntry("messageCount", 3);

        Map<String, Object> listed = performForMap("listConversations", Map.of(USER_ID, "user-1"));

        assertThat(listed).containsEntry("conversationIds", List.of("conversation-1"))
            .containsEntry("count", 1);
    }

    @Test
    void testAddMessagesRejectsAnUnsupportedRole() {
        Map<String, Object> inputParameters = Map.of(
            CONVERSATION_ID, "conversation-1",
            MESSAGES, List.of(Map.of(MESSAGE_ROLE, "system", MESSAGE_CONTENT, "instructions")));

        assertThatThrownBy(() -> perform("addMessages", inputParameters))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unsupported role: system. Supported roles are: user, assistant.");
    }

    @Test
    void testGetMessagesOfAnUnknownConversationIsEmpty() throws Exception {
        Map<String, Object> result = performForMap("getMessages", Map.of(CONVERSATION_ID, "missing"));

        assertThat(result).containsEntry(CONVERSATION_ID, "missing")
            .containsEntry("messages", List.of());
    }

    @Test
    void testGetMessagesReturnsRolesAndContentInOrder() throws Exception {
        perform(
            "addMessages",
            Map.of(
                CONVERSATION_ID, "conversation-1",
                MESSAGES, List.of(
                    Map.of(MESSAGE_ROLE, "user", MESSAGE_CONTENT, "hello"),
                    Map.of(MESSAGE_ROLE, "assistant", MESSAGE_CONTENT, "hi"))));

        Map<String, Object> result = performForMap("getMessages", Map.of(CONVERSATION_ID, "conversation-1"));

        assertThat(result.get("messages")).isEqualTo(
            List.of(Map.of("role", "user", "content", "hello"), Map.of("role", "assistant", "content", "hi")));
    }

    @Test
    void testGetMessagesWithoutCompactedHistoryLeavesOutArchivedMessages() throws Exception {
        perform(
            "addMessages",
            Map.of(
                CONVERSATION_ID, "conversation-1",
                MESSAGES, List.of(Map.of(MESSAGE_ROLE, "user", MESSAGE_CONTENT, "live"))));

        sessionRepository.appendEvent(
            SessionEvent.builder()
                .sessionId("conversation-1")
                .message(new UserMessage("archived"))
                .archived(true)
                .build());

        Map<String, Object> all = performForMap("getMessages", Map.of(CONVERSATION_ID, "conversation-1"));
        Map<String, Object> active = performForMap(
            "getMessages", Map.of(CONVERSATION_ID, "conversation-1", INCLUDE_COMPACTED_HISTORY, false));

        assertThat((List<?>) all.get("messages")).hasSize(2);
        assertThat(active.get("messages")).isEqualTo(List.of(Map.of("role", "user", "content", "live")));
    }

    @Test
    void testDeleteConversationReportsWhetherItExisted() throws Exception {
        perform(
            "addMessages",
            Map.of(
                CONVERSATION_ID, "conversation-1",
                MESSAGES, List.of(Map.of(MESSAGE_ROLE, "user", MESSAGE_CONTENT, "hello"))));

        assertThat(performForMap("deleteConversation", Map.of(CONVERSATION_ID, "conversation-1")))
            .containsEntry("deleted", true);
        assertThat(performForMap("deleteConversation", Map.of(CONVERSATION_ID, "conversation-1")))
            .containsEntry("deleted", false);
        assertThat(sessionRepository.findById("conversation-1")).isNull();
    }

    @Test
    void testListConversationsDefaultsToTheDefaultUser() throws Exception {
        perform(
            "addMessages",
            Map.of(
                CONVERSATION_ID, "conversation-1",
                MESSAGES, List.of(Map.of(MESSAGE_ROLE, "user", MESSAGE_CONTENT, "hello"))));

        assertThat(performForMap("listConversations", Map.of()))
            .containsEntry("conversationIds", List.of("conversation-1"));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> performForMap(String actionName, Map<String, Object> inputParameters)
        throws Exception {

        return (Map<String, Object>) perform(actionName, inputParameters);
    }

    private Object perform(String actionName, Map<String, Object> inputParameters) throws Exception {
        ActionDefinition actionDefinition = actionDefinitions.stream()
            .filter(definition -> actionName.equals(definition.getName()))
            .findFirst()
            .orElseThrow();

        PerformFunction performFunction = (PerformFunction) actionDefinition.getPerform()
            .orElseThrow();

        return performFunction.apply(
            MockParametersFactory.create(inputParameters), MockParametersFactory.create(Map.of()),
            mock(ActionContext.class));
    }
}
