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

package com.bytechef.component.ai.agent.chat.memory.session.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.session.SessionEvent;
import org.springframework.ai.session.SessionService;

/**
 * @author Marko Kriskovic
 */
class BranchStampingSessionServiceTest {

    private static final String BRANCH = "orch.researcher";
    private static final String SESSION_ID = "session-1";

    private final SessionService sessionService = mock(SessionService.class);
    private final BranchStampingSessionService branchStampingSessionService =
        new BranchStampingSessionService(sessionService, BRANCH);

    @Test
    void testAppendEventStampsBranchAndKeepsOtherFields() {
        Instant timestamp = Instant.parse("2026-01-01T00:00:00Z");

        SessionEvent event = SessionEvent.builder()
            .id("event-1")
            .sessionId(SESSION_ID)
            .timestamp(timestamp)
            .message(new UserMessage("question"))
            .metadata(Map.of("key", "value"))
            .build();

        branchStampingSessionService.appendEvent(event);

        SessionEvent appendedEvent = captureAppendedEvent();

        assertThat(appendedEvent.getBranch()).isEqualTo(BRANCH);
        assertThat(appendedEvent.getId()).isEqualTo("event-1");
        assertThat(appendedEvent.getSessionId()).isEqualTo(SESSION_ID);
        assertThat(appendedEvent.getTimestamp()).isEqualTo(timestamp);
        assertThat(appendedEvent.getMessage()).isEqualTo(event.getMessage());
        assertThat(appendedEvent.getMetadata()).isEqualTo(Map.of("key", "value"));
        assertThat(appendedEvent.isArchived()).isFalse();
    }

    @Test
    void testAppendEventKeepsAnExplicitBranch() {
        SessionEvent event = SessionEvent.builder()
            .sessionId(SESSION_ID)
            .message(new UserMessage("question"))
            .branch("orch")
            .build();

        branchStampingSessionService.appendEvent(event);

        assertThat(captureAppendedEvent()).isSameAs(event);
    }

    @Test
    void testAppendEventKeepsSyntheticEventsRootLevel() {
        SessionEvent event = SessionEvent.builder()
            .sessionId(SESSION_ID)
            .message(new AssistantMessage("summary"))
            .metadata(SessionEvent.METADATA_SYNTHETIC, true)
            .build();

        branchStampingSessionService.appendEvent(event);

        SessionEvent appendedEvent = captureAppendedEvent();

        assertThat(appendedEvent).isSameAs(event);
        assertThat(appendedEvent.getBranch()).isNull();
    }

    @Test
    void testAppendEventWithoutBranchPassesEventThrough() {
        BranchStampingSessionService rootSessionService = new BranchStampingSessionService(sessionService, null);

        SessionEvent event = SessionEvent.builder()
            .sessionId(SESSION_ID)
            .message(new UserMessage("question"))
            .build();

        rootSessionService.appendEvent(event);

        assertThat(captureAppendedEvent()).isSameAs(event);
    }

    private SessionEvent captureAppendedEvent() {
        ArgumentCaptor<SessionEvent> sessionEventArgumentCaptor = ArgumentCaptor.forClass(SessionEvent.class);

        verify(sessionService).appendEvent(sessionEventArgumentCaptor.capture());

        return sessionEventArgumentCaptor.getValue();
    }
}
