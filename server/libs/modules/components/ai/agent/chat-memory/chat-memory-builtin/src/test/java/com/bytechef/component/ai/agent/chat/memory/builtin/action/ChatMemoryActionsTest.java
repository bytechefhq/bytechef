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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.session.InMemorySessionRepository;
import org.springframework.ai.session.SessionRepository;

/**
 * Behavioral round-trip tests for the built-in chat memory actions over the session store: the same
 * {@link SessionRepository} the memory cluster element writes through.
 *
 * @author Ivica Cardic
 */
class ChatMemoryActionsTest {

    private SessionRepository sessionRepository;

    @BeforeEach
    void setUp() {
        sessionRepository = InMemorySessionRepository.builder()
            .build();
    }

    private Object addMessages(String conversationId) {
        return ChatMemoryAddMessagesAction.perform(
            MockParametersFactory.create(
                Map.of(
                    "conversationId", conversationId,
                    "messages", List.of(
                        Map.of("role", "user", "content", "hello"),
                        Map.of("role", "assistant", "content", "hi there")))),
            sessionRepository);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testAddMessagesCreatesSessionAndAppendsEvents() {
        Map<String, Object> result = (Map<String, Object>) addMessages("conversation-1");

        assertEquals("conversation-1", result.get("conversationId"));
        assertEquals(2, result.get("messageCount"));

        // Adding to the same conversation appends rather than replacing.
        Map<String, Object> secondResult = (Map<String, Object>) addMessages("conversation-1");

        assertEquals(4, secondResult.get("messageCount"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void testGetMessagesReturnsRoleAndContent() {
        addMessages("conversation-2");

        Map<String, Object> result = (Map<String, Object>) ChatMemoryGetMessagesAction.perform(
            MockParametersFactory.create(Map.of("conversationId", "conversation-2")), sessionRepository);

        List<Map<String, String>> messages = (List<Map<String, String>>) result.get("messages");

        assertEquals(2, messages.size());
        assertEquals("user", messages.get(0)
            .get("role"));
        assertEquals("hello", messages.get(0)
            .get("content"));
        assertEquals("assistant", messages.get(1)
            .get("role"));
        assertEquals("hi there", messages.get(1)
            .get("content"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void testGetMessagesForUnknownConversationReturnsEmptyList() {
        Map<String, Object> result = (Map<String, Object>) ChatMemoryGetMessagesAction.perform(
            MockParametersFactory.create(Map.of("conversationId", "unknown")), sessionRepository);

        assertTrue(((List<?>) result.get("messages")).isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testListConversationsReturnsBuiltInUserSessions() {
        addMessages("conversation-3");
        addMessages("conversation-4");

        Map<String, Object> result = (Map<String, Object>) ChatMemoryListConversationsAction.perform(
            sessionRepository);

        List<String> conversationIds = (List<String>) result.get("conversationIds");

        assertEquals(2, result.get("count"));
        assertTrue(conversationIds.contains("conversation-3"));
        assertTrue(conversationIds.contains("conversation-4"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void testDeleteConversationReportsExistence() {
        addMessages("conversation-5");

        Map<String, Object> deletedResult = (Map<String, Object>) ChatMemoryDeleteAction.perform(
            MockParametersFactory.create(Map.of("conversationId", "conversation-5")), sessionRepository);

        assertEquals(true, deletedResult.get("deleted"));
        assertTrue(sessionRepository.findById("conversation-5")
            .isEmpty());

        Map<String, Object> missingResult = (Map<String, Object>) ChatMemoryDeleteAction.perform(
            MockParametersFactory.create(Map.of("conversationId", "conversation-5")), sessionRepository);

        assertEquals(false, missingResult.get("deleted"));
    }
}
