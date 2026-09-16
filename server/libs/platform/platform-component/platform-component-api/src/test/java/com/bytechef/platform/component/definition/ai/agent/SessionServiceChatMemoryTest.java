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

package com.bytechef.platform.component.definition.ai.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.session.DefaultSessionService;
import org.springframework.ai.session.EventFilter;
import org.springframework.ai.session.InMemorySessionRepository;
import org.springframework.ai.session.Session;
import org.springframework.ai.session.SessionEvent;
import org.springframework.ai.session.SessionRepository;

/**
 * @author Ivica Cardic
 */
class SessionServiceChatMemoryTest {

    private static final String CONVERSATION_ID = "conversation-1";

    private SessionServiceChatMemory sessionServiceChatMemory;

    @BeforeEach
    void setUp() {
        SessionRepository sessionRepository = InMemorySessionRepository.builder()
            .build();

        sessionRepository.save(Session.builder()
            .id(CONVERSATION_ID)
            .userId("user-1")
            .createdAt(Instant.now())
            .build());

        sessionRepository.appendEvent(SessionEvent.builder()
            .sessionId(CONVERSATION_ID)
            .message(new UserMessage("archived turn"))
            .archived(true)
            .build());

        sessionRepository.appendEvent(SessionEvent.builder()
            .sessionId(CONVERSATION_ID)
            .message(new UserMessage("live turn"))
            .build());

        sessionServiceChatMemory = new SessionServiceChatMemory(
            DefaultSessionService.builder()
                .sessionRepository(sessionRepository)
                .build(),
            EventFilter.all());
    }

    @Test
    void testGetReturnsOnlyActiveEvents() {
        List<Message> messages = sessionServiceChatMemory.get(CONVERSATION_ID);

        assertThat(messages).extracting(Message::getText)
            .containsExactly("live turn");
    }

    @Test
    void testGetReturnsEmptyHistoryForUnknownConversation() {
        assertThat(sessionServiceChatMemory.get("unknown-conversation")).isEmpty();
    }

    @Test
    void testWritesAreRejected() {
        assertThatThrownBy(() -> sessionServiceChatMemory.add(CONVERSATION_ID, List.of(new UserMessage("x"))))
            .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> sessionServiceChatMemory.clear(CONVERSATION_ID))
            .isInstanceOf(UnsupportedOperationException.class);
    }
}
