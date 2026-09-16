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

package com.bytechef.component.ai.agent.chat.memory.session.compaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.session.Session;
import org.springframework.ai.session.SessionEvent;
import org.springframework.ai.session.compaction.CompactionRequest;

/**
 * @author Ivica Cardic
 */
class EventCountTriggerTest {

    @Test
    void testConstructorRejectsZeroMaxEvents() {
        assertThatThrownBy(() -> new EventCountTrigger(0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("maxEvents must be greater than 0");
    }

    @Test
    void testConstructorRejectsNegativeMaxEvents() {
        assertThatThrownBy(() -> new EventCountTrigger(-1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("maxEvents must be greater than 0");
    }

    @Test
    void testShouldCompactOnlyOnceEventCountExceedsMaxEvents() {
        EventCountTrigger eventCountTrigger = new EventCountTrigger(1);

        assertThat(eventCountTrigger.shouldCompact(compactionRequest(1))).isFalse();
        assertThat(eventCountTrigger.shouldCompact(compactionRequest(2))).isTrue();
    }

    private static CompactionRequest compactionRequest(int eventCount) {
        Session session = Session.builder()
            .id("session-1")
            .userId("user-1")
            .createdAt(Instant.now())
            .build();

        List<SessionEvent> events = IntStream.range(0, eventCount)
            .mapToObj(index -> SessionEvent.builder()
                .sessionId("session-1")
                .message(new UserMessage("message " + index))
                .build())
            .toList();

        return CompactionRequest.of(session, events);
    }
}
