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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.ContextConsumer;
import com.bytechef.component.definition.Context.Log;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.session.DefaultSessionService;
import org.springframework.ai.session.EventFilter;
import org.springframework.ai.session.Session;
import org.springframework.ai.session.SessionEvent;
import org.springframework.ai.session.SessionRepository;
import org.springframework.ai.session.SessionService;
import org.springframework.ai.session.compaction.CompactionResult;
import org.springframework.ai.session.compaction.CompactionStrategy;
import org.springframework.ai.session.compaction.SlidingWindowCompactionStrategy;

/**
 * @author Ivica Cardic
 */
class ContextLoggingSessionServiceTest {

    private static final String SESSION_ID = "conversation-1";

    @Test
    void testFailedEventReadDuringCompactionIsLoggedToTheContextAndRethrown() throws Exception {
        IllegalStateException storageFailure = new IllegalStateException("Redis connection refused");

        SessionRepository sessionRepository = sessionRepository();

        when(sessionRepository.findEvents(eq(SESSION_ID), any(EventFilter.class))).thenThrow(storageFailure);

        assertCompactionFailureIsLoggedAndRethrown(sessionRepository, storageFailure, slidingWindowStrategy());
    }

    @Test
    void testFailedCompactionWriteIsLoggedToTheContextAndRethrown() throws Exception {
        IllegalStateException storageFailure = new IllegalStateException("S3 access denied");

        SessionRepository sessionRepository = sessionRepository();

        when(sessionRepository.findEvents(eq(SESSION_ID), any(EventFilter.class))).thenReturn(threeEvents());
        when(sessionRepository.applyCompaction(eq(SESSION_ID), any(), anyLong())).thenThrow(storageFailure);

        assertCompactionFailureIsLoggedAndRethrown(sessionRepository, storageFailure, slidingWindowStrategy());
    }

    @Test
    void testFailedCompactionStrategyIsLoggedToTheContextAndRethrown() throws Exception {
        IllegalStateException summarizerFailure = new IllegalStateException("401 Unauthorized: invalid api key");

        SessionRepository sessionRepository = sessionRepository();

        when(sessionRepository.findEvents(eq(SESSION_ID), any(EventFilter.class))).thenReturn(threeEvents());

        assertCompactionFailureIsLoggedAndRethrown(
            sessionRepository, summarizerFailure, compactionRequest -> {
                throw summarizerFailure;
            });
    }

    @Test
    void testSuccessfulCompactionIsNotLogged() {
        CompactionResult compactionResult = mock(CompactionResult.class);
        SessionService sessionService = mock(SessionService.class);

        EventCountTrigger eventCountTrigger = new EventCountTrigger(1);
        CompactionStrategy compactionStrategy = slidingWindowStrategy();

        when(sessionService.compact(SESSION_ID, eventCountTrigger, compactionStrategy)).thenReturn(compactionResult);

        Context context = mock(Context.class);

        ContextLoggingSessionService contextLoggingSessionService = new ContextLoggingSessionService(
            sessionService, context);

        assertThat(contextLoggingSessionService.compact(SESSION_ID, eventCountTrigger, compactionStrategy))
            .isSameAs(compactionResult);

        verify(context, never()).log(any());
    }

    @SuppressWarnings("unchecked")
    private static void assertCompactionFailureIsLoggedAndRethrown(
        SessionRepository sessionRepository, RuntimeException expectedFailure,
        CompactionStrategy compactionStrategy) throws Exception {

        SessionService sessionService = DefaultSessionService.builder()
            .sessionRepository(sessionRepository)
            .build();

        Context context = mock(Context.class);

        ContextLoggingSessionService contextLoggingSessionService = new ContextLoggingSessionService(
            sessionService, context);

        assertThatThrownBy(
            () -> contextLoggingSessionService.compact(SESSION_ID, new EventCountTrigger(1), compactionStrategy))
                .isSameAs(expectedFailure);

        ArgumentCaptor<ContextConsumer<Log>> logConsumerCaptor = ArgumentCaptor.forClass(ContextConsumer.class);

        verify(context).log(logConsumerCaptor.capture());

        Log log = mock(Log.class);

        ContextConsumer<Log> logConsumer = logConsumerCaptor.getValue();

        logConsumer.accept(log);

        verify(log).warn(contains(SESSION_ID), same(expectedFailure));
        verify(log).warn(contains(expectedFailure.getMessage()), same(expectedFailure));
    }

    private static SessionRepository sessionRepository() {
        SessionRepository sessionRepository = mock(SessionRepository.class);

        when(sessionRepository.findById(SESSION_ID)).thenReturn(
            Session.builder()
                .id(SESSION_ID)
                .userId("user-1")
                .createdAt(Instant.now())
                .build());

        return sessionRepository;
    }

    private static CompactionStrategy slidingWindowStrategy() {
        return SlidingWindowCompactionStrategy.builder()
            .maxEvents(1)
            .build();
    }

    private static List<SessionEvent> threeEvents() {
        return List.of(
            sessionEvent(new UserMessage("first question")),
            sessionEvent(new AssistantMessage("first answer")),
            sessionEvent(new UserMessage("second question")));
    }

    private static SessionEvent sessionEvent(Message message) {
        return SessionEvent.builder()
            .sessionId(SESSION_ID)
            .message(message)
            .build();
    }
}
