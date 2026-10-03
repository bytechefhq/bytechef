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

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.session.DefaultSessionService;
import org.springframework.ai.session.InMemorySessionRepository;
import org.springframework.ai.session.Session;
import org.springframework.ai.session.SessionEvent;
import org.springframework.ai.session.SessionRepository;

/**
 * @author Ivica Cardic
 */
class SessionConversationHistoryReaderTest {

    private static final String CONVERSATION_ID = "conversation-1";

    private SessionConversationHistoryReader sessionConversationHistoryReader;

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

        sessionConversationHistoryReader = new SessionConversationHistoryReader(
            DefaultSessionService.builder()
                .sessionRepository(sessionRepository)
                .build());
    }

    @Test
    void testReadReturnsOnlyActiveEvents() {
        List<Message> messages = sessionConversationHistoryReader.read(CONVERSATION_ID);

        assertThat(messages).extracting(Message::getText)
            .containsExactly("live turn");
    }

    @Test
    void testReadReturnsEmptyHistoryForUnknownConversation() {
        assertThat(sessionConversationHistoryReader.read("unknown-conversation")).isEmpty();
    }

    @Test
    void testReadReturnsEmptyHistoryForExpiredConversation() {
        InMemorySessionRepository delegate = InMemorySessionRepository.builder()
            .build();
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");

        SessionRepository createdAtRepository = new SlidingExpirySessionRepository(
            delegate, Duration.ofDays(1), Clock.fixed(createdAt, ZoneOffset.UTC));

        createdAtRepository.saveIfAbsent(Session.builder()
            .id("expired-conversation")
            .userId("user-1")
            .createdAt(createdAt)
            .build());

        delegate.appendEvent(SessionEvent.builder()
            .sessionId("expired-conversation")
            .message(new UserMessage("old turn"))
            .build());

        SessionRepository expiredRepository = new SlidingExpirySessionRepository(
            delegate, Duration.ofDays(1), Clock.fixed(createdAt.plus(Duration.ofDays(2)), ZoneOffset.UTC));

        SessionConversationHistoryReader expiredHistoryReader = new SessionConversationHistoryReader(
            DefaultSessionService.builder()
                .sessionRepository(expiredRepository)
                .build());

        assertThat(expiredHistoryReader.read("expired-conversation")).isEmpty();
    }

    @Test
    void testSessionServiceIsRequired() {
        assertThatThrownBy(() -> new SessionConversationHistoryReader(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("sessionService");
    }
}
