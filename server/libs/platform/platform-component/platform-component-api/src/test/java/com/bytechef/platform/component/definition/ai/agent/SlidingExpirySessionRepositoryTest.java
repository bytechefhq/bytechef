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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.session.DefaultSessionService;
import org.springframework.ai.session.EventFilter;
import org.springframework.ai.session.InMemorySessionRepository;
import org.springframework.ai.session.Session;
import org.springframework.ai.session.SessionEvent;
import org.springframework.ai.session.SessionRepository;
import org.springframework.ai.session.advisor.SessionMemoryAdvisor;
import org.springframework.ai.session.compaction.CompactionPlan;

/**
 * @author Ivica Cardic
 */
class SlidingExpirySessionRepositoryTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final String SESSION_ID = "session-1";
    private static final Duration TIME_TO_LIVE = Duration.ofDays(10);
    private static final String USER_ID = "user-1";

    @Test
    void testSaveIfAbsentStampsTimeToLiveFromNow() {
        InMemorySessionRepository delegate = InMemorySessionRepository.builder()
            .build();

        SlidingExpirySessionRepository sessionRepository = createSessionRepository(delegate, NOW);

        Session session = createSession(NOW.plus(Duration.ofDays(60)));

        assertThat(sessionRepository.saveIfAbsent(session)).isTrue();

        Session storedSession = delegate.findById(SESSION_ID);

        assertThat(storedSession).isNotNull();
        assertThat(storedSession.expiresAt()).isEqualTo(NOW.plus(TIME_TO_LIVE));
        assertThat(storedSession.createdAt()).isEqualTo(session.createdAt());
        assertThat(storedSession.userId()).isEqualTo(USER_ID);
        assertThat(storedSession.metadata()).isEqualTo(Map.of("key", "value"));
    }

    @Test
    void testSaveStampsTimeToLiveFromNow() {
        InMemorySessionRepository delegate = InMemorySessionRepository.builder()
            .build();

        SlidingExpirySessionRepository sessionRepository = createSessionRepository(delegate, NOW);

        Session savedSession = sessionRepository.save(createSession(null));

        assertThat(savedSession.expiresAt()).isEqualTo(NOW.plus(TIME_TO_LIVE));

        Session storedSession = delegate.findById(SESSION_ID);

        assertThat(storedSession).isNotNull();
        assertThat(storedSession.expiresAt()).isEqualTo(NOW.plus(TIME_TO_LIVE));
        assertThat(storedSession.metadata()).isEqualTo(Map.of("key", "value"));
    }

    @Test
    void testFindByIdRefreshesExpiryOncePastTheRefreshGranularity() {
        InMemorySessionRepository delegate = InMemorySessionRepository.builder()
            .build();

        createSessionRepository(delegate, NOW).saveIfAbsent(createSession(null));

        Instant readAt = NOW.plus(Duration.ofDays(1))
            .plusSeconds(1);

        Session session = createSessionRepository(delegate, readAt).findById(SESSION_ID);

        assertThat(session).isNotNull();
        assertThat(session.expiresAt()).isEqualTo(readAt.plus(TIME_TO_LIVE));

        Session storedSession = delegate.findById(SESSION_ID);

        assertThat(storedSession).isNotNull();
        assertThat(storedSession.expiresAt()).isEqualTo(readAt.plus(TIME_TO_LIVE));
        assertThat(storedSession.createdAt()).isEqualTo(NOW.minus(Duration.ofHours(1)));
        assertThat(storedSession.metadata()).isEqualTo(Map.of("key", "value"));
    }

    @Test
    void testFindByIdDoesNotRefreshWithinTheRefreshGranularity() {
        SessionRepository delegate = mock(SessionRepository.class);
        Session session = createSession(NOW.plus(TIME_TO_LIVE));

        when(delegate.findById(SESSION_ID)).thenReturn(session);

        Instant readAt = NOW.plus(Duration.ofDays(1));

        assertThat(createSessionRepository(delegate, readAt).findById(SESSION_ID)).isSameAs(session);

        verify(delegate, never()).save(any(Session.class));
    }

    @Test
    void testRefreshGranularityIsHalfOfAShortTimeToLive() {
        InMemorySessionRepository delegate = InMemorySessionRepository.builder()
            .build();
        Duration timeToLive = Duration.ofHours(2);

        new SlidingExpirySessionRepository(delegate, timeToLive, fixedClock(NOW)).saveIfAbsent(createSession(null));

        Instant earlyReadAt = NOW.plus(Duration.ofMinutes(59));

        Session earlySession = new SlidingExpirySessionRepository(delegate, timeToLive, fixedClock(earlyReadAt))
            .findById(SESSION_ID);

        assertThat(earlySession).isNotNull();
        assertThat(earlySession.expiresAt()).isEqualTo(NOW.plus(timeToLive));

        Instant lateReadAt = NOW.plus(Duration.ofMinutes(61));

        Session lateSession = new SlidingExpirySessionRepository(delegate, timeToLive, fixedClock(lateReadAt))
            .findById(SESSION_ID);

        assertThat(lateSession).isNotNull();
        assertThat(lateSession.expiresAt()).isEqualTo(lateReadAt.plus(timeToLive));
    }

    @Test
    void testFindByIdDoesNotRefreshAtTheRefreshGranularityBoundary() {
        SessionRepository delegate = mock(SessionRepository.class);
        Session session = createSession(NOW.plus(TIME_TO_LIVE));

        when(delegate.findById(SESSION_ID)).thenReturn(session);

        Instant readAt = NOW.plus(Duration.ofDays(1));

        Instant refreshThreshold = readAt.plus(TIME_TO_LIVE)
            .minus(Duration.ofDays(1));

        assertThat(session.expiresAt()).isEqualTo(refreshThreshold);
        assertThat(createSessionRepository(delegate, readAt).findById(SESSION_ID)).isSameAs(session);

        verify(delegate, never()).save(any(Session.class));
    }

    @Test
    void testFindByIdDeletesAnExpiredSessionNotYetSwept() {
        SessionRepository delegate = mock(SessionRepository.class);

        when(delegate.findById(SESSION_ID)).thenReturn(createSession(NOW.minusSeconds(1)));

        assertThat(createSessionRepository(delegate, NOW).findById(SESSION_ID)).isNull();

        verify(delegate).findById(SESSION_ID);
        verify(delegate).delete(SESSION_ID);
        verifyNoMoreInteractions(delegate);
    }

    @Test
    void testFindByIdOfAnExpiredSessionLetsTheSessionBeCreatedAgain() {
        InMemorySessionRepository delegate = InMemorySessionRepository.builder()
            .build();

        createSessionRepository(delegate, NOW).saveIfAbsent(createSession(null));

        Instant readAt = NOW.plus(TIME_TO_LIVE)
            .plusSeconds(1);

        SlidingExpirySessionRepository sessionRepository = createSessionRepository(delegate, readAt);

        assertThat(sessionRepository.findById(SESSION_ID)).isNull();
        assertThat(delegate.findById(SESSION_ID)).isNull();
        assertThat(sessionRepository.saveIfAbsent(createSession(null))).isTrue();

        Session storedSession = delegate.findById(SESSION_ID);

        assertThat(storedSession).isNotNull();
        assertThat(storedSession.expiresAt()).isEqualTo(readAt.plus(TIME_TO_LIVE));
    }

    @Test
    void testSessionMemoryAdvisorStartsAFreshSessionOnceTheOldOneExpired() {
        InMemorySessionRepository delegate = InMemorySessionRepository.builder()
            .build();
        MutableClock clock = new MutableClock(NOW);

        DefaultSessionService sessionService = DefaultSessionService.builder()
            .sessionRepository(new SlidingExpirySessionRepository(delegate, TIME_TO_LIVE, clock))
            .build();

        SessionMemoryAdvisor sessionMemoryAdvisor = SessionMemoryAdvisor.builder(sessionService)
            .build();
        AdvisorChain advisorChain = mock(AdvisorChain.class);

        sessionMemoryAdvisor.before(createChatClientRequest("first"), advisorChain);

        clock.advance(TIME_TO_LIVE.plusSeconds(1));

        ChatClientRequest chatClientRequest = sessionMemoryAdvisor.before(
            createChatClientRequest("second"), advisorChain);

        Prompt prompt = chatClientRequest.prompt();

        assertThat(prompt.getInstructions()).extracting(Message::getText)
            .containsExactly("second");

        Session storedSession = delegate.findById(SESSION_ID);

        assertThat(storedSession).isNotNull();
        assertThat(storedSession.expiresAt()).isEqualTo(NOW.plus(TIME_TO_LIVE)
            .plusSeconds(1)
            .plus(TIME_TO_LIVE));
        assertThat(delegate.findEvents(SESSION_ID, EventFilter.all()))
            .extracting(sessionEvent -> sessionEvent.getMessage()
                .getText())
            .containsExactly("second");
    }

    @Test
    void testFindEventsByUserIdSkipsEventsOfExpiredSessions() {
        InMemorySessionRepository delegate = InMemorySessionRepository.builder()
            .build();

        createSessionRepository(delegate, NOW).saveIfAbsent(createSession("expired-session", null));
        createSessionRepository(delegate, NOW.plus(Duration.ofDays(5)))
            .saveIfAbsent(createSession("live-session", null));

        delegate.appendEvent(createSessionEvent("live-session", NOW.plusSeconds(1), "live-1"));
        delegate.appendEvent(createSessionEvent("expired-session", NOW.plusSeconds(2), "expired-1"));
        delegate.appendEvent(createSessionEvent("live-session", NOW.plusSeconds(3), "live-2"));
        delegate.appendEvent(createSessionEvent("expired-session", NOW.plusSeconds(4), "expired-2"));

        Instant readAt = NOW.plus(TIME_TO_LIVE)
            .plusSeconds(1);

        SlidingExpirySessionRepository sessionRepository = createSessionRepository(delegate, readAt);

        assertThat(sessionRepository.findEventsByUserId(USER_ID, EventFilter.all()))
            .extracting(sessionEvent -> sessionEvent.getMessage()
                .getText())
            .containsExactly("live-1", "live-2");
        assertThat(sessionRepository.findEventsByUserId(USER_ID, EventFilter.lastN(1)))
            .extracting(sessionEvent -> sessionEvent.getMessage()
                .getText())
            .containsExactly("live-2");
    }

    @Test
    void testEventReadsOfALiveSessionGoToTheDelegate() {
        SessionRepository delegate = mock(SessionRepository.class);
        Session session = createSession(NOW.plus(Duration.ofDays(1)));
        SessionEvent sessionEvent = createSessionEvent(SESSION_ID, NOW, "hello");
        EventFilter eventFilter = EventFilter.lastN(3);

        when(delegate.findByUserId(USER_ID)).thenReturn(List.of(session));
        when(delegate.getEventVersion(SESSION_ID)).thenReturn(9L);
        when(delegate.findEvents(SESSION_ID, eventFilter)).thenReturn(List.of(sessionEvent));
        when(delegate.findEventsByUserId(USER_ID, eventFilter)).thenReturn(List.of(sessionEvent));

        SlidingExpirySessionRepository sessionRepository = createSessionRepository(delegate, NOW);

        assertThat(sessionRepository.getEventVersion(SESSION_ID)).isEqualTo(9L);
        assertThat(sessionRepository.findEvents(SESSION_ID, eventFilter)).containsExactly(sessionEvent);
        assertThat(sessionRepository.findEventsByUserId(USER_ID, eventFilter)).containsExactly(sessionEvent);

        verify(delegate, never()).save(any(Session.class));
        verify(delegate, never()).delete(any());
    }

    @Test
    void testFindEventsAndGetEventVersionDoNotReadTheSession() {
        SessionRepository delegate = mock(SessionRepository.class);
        EventFilter eventFilter = EventFilter.all();

        SlidingExpirySessionRepository sessionRepository = createSessionRepository(delegate, NOW);

        assertThat(sessionRepository.getEventVersion(SESSION_ID)).isZero();
        assertThat(sessionRepository.findEvents(SESSION_ID, eventFilter)).isEmpty();

        verify(delegate).getEventVersion(SESSION_ID);
        verify(delegate).findEvents(SESSION_ID, eventFilter);
        verifyNoMoreInteractions(delegate);
    }

    @Test
    void testFindByIdReturnsASessionExpiringExactlyNow() {
        SessionRepository delegate = mock(SessionRepository.class);
        Session session = createSession(NOW);

        when(delegate.findById(SESSION_ID)).thenReturn(session);
        when(delegate.save(any(Session.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Session foundSession = createSessionRepository(delegate, NOW).findById(SESSION_ID);

        assertThat(foundSession).isNotNull();
        assertThat(foundSession.expiresAt()).isEqualTo(NOW.plus(TIME_TO_LIVE));
    }

    @Test
    void testFindByUserIdSkipsExpiredSessionsNotYetDeleted() {
        SessionRepository delegate = mock(SessionRepository.class);
        Session expiredSession = createSession("expired-session", NOW.minusSeconds(1));
        Session liveSession = createSession("live-session", NOW.plus(Duration.ofDays(1)));
        Session sessionWithoutExpiry = createSession("session-without-expiry", null);

        when(delegate.findByUserId(USER_ID)).thenReturn(List.of(expiredSession, liveSession, sessionWithoutExpiry));

        assertThat(createSessionRepository(delegate, NOW).findByUserId(USER_ID))
            .containsExactly(liveSession, sessionWithoutExpiry);
    }

    @Test
    void testFindByIdDoesNotRefreshASessionWithoutExpiry() {
        SessionRepository delegate = mock(SessionRepository.class);
        Session session = createSession(null);

        when(delegate.findById(SESSION_ID)).thenReturn(session);

        Instant readAt = NOW.plus(Duration.ofDays(365));

        assertThat(createSessionRepository(delegate, readAt).findById(SESSION_ID)).isSameAs(session);

        verify(delegate, never()).save(any(Session.class));
    }

    @Test
    void testFindByIdReturnsNullForAMissingSession() {
        SessionRepository delegate = mock(SessionRepository.class);

        assertThat(createSessionRepository(delegate, NOW).findById(SESSION_ID)).isNull();

        verify(delegate, never()).save(any(Session.class));
    }

    @Test
    void testRefreshKeepsTheSessionEvents() {
        InMemorySessionRepository delegate = InMemorySessionRepository.builder()
            .build();

        createSessionRepository(delegate, NOW).saveIfAbsent(createSession(null));

        delegate.appendEvent(SessionEvent.builder()
            .sessionId(SESSION_ID)
            .timestamp(NOW)
            .message(new UserMessage("hello"))
            .build());

        Instant readAt = NOW.plus(Duration.ofDays(5));

        Session session = createSessionRepository(delegate, readAt).findById(SESSION_ID);

        assertThat(session).isNotNull();
        assertThat(session.expiresAt()).isEqualTo(readAt.plus(TIME_TO_LIVE));
        assertThat(delegate.findEvents(SESSION_ID, EventFilter.all())).hasSize(1);
    }

    @Test
    void testSessionInDailyUseSurvivesTheExpiredSessionCleanup() {
        InMemorySessionRepository delegate = InMemorySessionRepository.builder()
            .build();

        createSessionRepository(delegate, NOW).saveIfAbsent(createSession(null));
        createSessionRepository(delegate, NOW).saveIfAbsent(createSession("unused-session", null));

        Instant day = NOW;

        for (int dayIndex = 1; dayIndex <= 30; dayIndex++) {
            day = NOW.plus(Duration.ofDays(dayIndex));

            createSessionRepository(delegate, day).findById(SESSION_ID);
        }

        assertThat(delegate.deleteExpiredSessions(day)).isEqualTo(1);
        assertThat(delegate.findById(SESSION_ID)).isNotNull();
        assertThat(delegate.findById("unused-session")).isNull();
    }

    @Test
    void testDelegatesEveryOtherMethod() {
        SessionRepository delegate = mock(SessionRepository.class);
        Session session = createSession(null);
        SessionEvent sessionEvent = SessionEvent.builder()
            .sessionId(SESSION_ID)
            .message(new UserMessage("hello"))
            .build();
        CompactionPlan compactionPlan = new CompactionPlan(Set.of("event-1"), List.of());

        when(delegate.findByUserId(USER_ID)).thenReturn(List.of(session));
        when(delegate.deleteExpiredSessions(NOW)).thenReturn(3);
        when(delegate.applyCompaction(SESSION_ID, compactionPlan, 7L)).thenReturn(true);

        SlidingExpirySessionRepository sessionRepository = createSessionRepository(delegate, NOW);

        assertThat(sessionRepository.findByUserId(USER_ID)).containsExactly(session);
        assertThat(sessionRepository.deleteExpiredSessions(NOW)).isEqualTo(3);
        assertThat(sessionRepository.applyCompaction(SESSION_ID, compactionPlan, 7L)).isTrue();

        sessionRepository.delete(SESSION_ID);
        sessionRepository.appendEvent(sessionEvent);

        verify(delegate).findByUserId(USER_ID);
        verify(delegate).deleteExpiredSessions(NOW);
        verify(delegate).applyCompaction(SESSION_ID, compactionPlan, 7L);
        verify(delegate).delete(SESSION_ID);
        verify(delegate).appendEvent(sessionEvent);
        verifyNoMoreInteractions(delegate);
    }

    @Test
    void testSavePassesTheStampedSessionToTheDelegate() {
        SessionRepository delegate = mock(SessionRepository.class);
        ArgumentCaptor<Session> sessionArgumentCaptor = ArgumentCaptor.forClass(Session.class);

        createSessionRepository(delegate, NOW).save(createSession(null));

        verify(delegate).save(sessionArgumentCaptor.capture());

        Session savedSession = sessionArgumentCaptor.getValue();

        assertThat(savedSession.id()).isEqualTo(SESSION_ID);
        assertThat(savedSession.expiresAt()).isEqualTo(NOW.plus(TIME_TO_LIVE));
    }

    @Test
    void testConstructorRejectsANonPositiveTimeToLive() {
        SessionRepository delegate = mock(SessionRepository.class);
        Clock clock = fixedClock(NOW);

        assertThatThrownBy(() -> new SlidingExpirySessionRepository(delegate, Duration.ZERO, clock))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("timeToLive must be positive");
        assertThatThrownBy(() -> new SlidingExpirySessionRepository(delegate, Duration.ofDays(-1), clock))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("timeToLive must be positive");
        assertThatThrownBy(() -> new SlidingExpirySessionRepository(delegate, null, clock))
            .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new SlidingExpirySessionRepository(null, TIME_TO_LIVE, clock))
            .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new SlidingExpirySessionRepository(delegate, TIME_TO_LIVE, null))
            .isInstanceOf(NullPointerException.class);
    }

    private static ChatClientRequest createChatClientRequest(String text) {
        return ChatClientRequest.builder()
            .prompt(new Prompt(new UserMessage(text)))
            .context(SessionMemoryAdvisor.SESSION_ID_CONTEXT_KEY, SESSION_ID)
            .build();
    }

    private static SessionEvent createSessionEvent(String sessionId, Instant timestamp, String text) {
        return SessionEvent.builder()
            .sessionId(sessionId)
            .timestamp(timestamp)
            .message(new UserMessage(text))
            .build();
    }

    private static Session createSession(@Nullable Instant expiresAt) {
        return createSession(SESSION_ID, expiresAt);
    }

    private static Session createSession(String sessionId, @Nullable Instant expiresAt) {
        return Session.builder()
            .id(sessionId)
            .userId(USER_ID)
            .createdAt(NOW.minus(Duration.ofHours(1)))
            .expiresAt(expiresAt)
            .metadata(Map.of("key", "value"))
            .build();
    }

    private static SlidingExpirySessionRepository createSessionRepository(SessionRepository delegate, Instant now) {
        return new SlidingExpirySessionRepository(delegate, TIME_TO_LIVE, fixedClock(now));
    }

    private static Clock fixedClock(Instant instant) {
        return Clock.fixed(instant, ZoneOffset.UTC);
    }

    private static final class MutableClock extends Clock {

        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        private void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
