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

package com.bytechef.component.ai.agent.chat.memory.builtin.session.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.bytechef.platform.component.definition.ai.agent.TenantRoutingSessionRepository;
import com.bytechef.tenant.TenantContext;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.ai.session.InMemorySessionRepository;
import org.springframework.ai.session.Session;
import org.springframework.ai.session.SessionRepository;

/**
 * @author Ivica Cardic
 */
class ExpiredSessionCleanerTest {

    @Test
    void testDeletesExpiredSessionsOfEveryTenant() {
        SessionRepository sessionRepository = new TenantRoutingSessionRepository(
            tenantId -> InMemorySessionRepository.builder()
                .build());

        Instant now = Instant.now();

        TenantContext.runWithTenantId("tenanta", () -> saveSessions(sessionRepository, now));
        TenantContext.runWithTenantId("tenantb", () -> saveSessions(sessionRepository, now));

        ExpiredSessionCleaner expiredSessionCleaner = new ExpiredSessionCleaner(
            sessionRepository, () -> List.of("tenanta", "tenantb"));

        assertThat(expiredSessionCleaner.deleteExpiredSessions(now)).isEqualTo(2);

        assertThat(findSession(sessionRepository, "tenanta", "expired-session")).isNull();
        assertThat(findSession(sessionRepository, "tenantb", "expired-session")).isNull();
        assertThat(findSession(sessionRepository, "tenanta", "live-session")).isNotNull();
        assertThat(findSession(sessionRepository, "tenantb", "live-session")).isNotNull();
    }

    @Test
    void testFailureForOneTenantDoesNotStopTheOthers() {
        SessionRepository tenantASessionRepository = mock(SessionRepository.class);
        SessionRepository tenantBSessionRepository = mock(SessionRepository.class);

        when(tenantASessionRepository.deleteExpiredSessions(any(Instant.class)))
            .thenThrow(new IllegalStateException("unavailable"));
        when(tenantBSessionRepository.deleteExpiredSessions(any(Instant.class)))
            .thenReturn(1);

        SessionRepository sessionRepository = new TenantRoutingSessionRepository(
            tenantId -> tenantId.equals("tenanta") ? tenantASessionRepository : tenantBSessionRepository);

        ExpiredSessionCleaner expiredSessionCleaner = new ExpiredSessionCleaner(
            sessionRepository, () -> List.of("tenanta", "tenantb"));

        assertThat(expiredSessionCleaner.deleteExpiredSessions(Instant.now())).isEqualTo(1);

        verify(tenantBSessionRepository).deleteExpiredSessions(any(Instant.class));
    }

    @Test
    void testLogsDeletionsAtInfoOnlyForTenantsWithDeletedSessions() {
        SessionRepository tenantASessionRepository = mock(SessionRepository.class);
        SessionRepository tenantBSessionRepository = mock(SessionRepository.class);

        when(tenantASessionRepository.deleteExpiredSessions(any(Instant.class)))
            .thenReturn(2);
        when(tenantBSessionRepository.deleteExpiredSessions(any(Instant.class)))
            .thenReturn(0);

        SessionRepository sessionRepository = new TenantRoutingSessionRepository(
            tenantId -> tenantId.equals("tenanta") ? tenantASessionRepository : tenantBSessionRepository);

        ExpiredSessionCleaner expiredSessionCleaner = new ExpiredSessionCleaner(
            sessionRepository, () -> List.of("tenanta", "tenantb"));

        Logger logger = (Logger) LoggerFactory.getLogger(ExpiredSessionCleaner.class);

        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();

        listAppender.start();

        logger.addAppender(listAppender);

        try {
            assertThat(expiredSessionCleaner.deleteExpiredSessions(Instant.now())).isEqualTo(2);
        } finally {
            logger.detachAppender(listAppender);
        }

        List<String> infoMessages = listAppender.list.stream()
            .filter(loggingEvent -> loggingEvent.getLevel() == Level.INFO)
            .map(ILoggingEvent::getFormattedMessage)
            .toList();

        assertThat(infoMessages).containsExactly("Deleted 2 expired sessions for tenant tenanta");
    }

    @Test
    void testUnavailableRepositoryDeletesNothing() {
        UnavailableSessionRepository unavailableSessionRepository = new UnavailableSessionRepository("unavailable");

        assertThat(unavailableSessionRepository.deleteExpiredSessions(Instant.now())).isZero();

        ExpiredSessionCleaner expiredSessionCleaner = new ExpiredSessionCleaner(
            unavailableSessionRepository, () -> List.of("tenanta", "tenantb"));

        assertThat(expiredSessionCleaner.deleteExpiredSessions(Instant.now())).isZero();
    }

    private static @Nullable Session findSession(
        SessionRepository sessionRepository, String tenantId, String sessionId) {

        return TenantContext.callWithTenantId(tenantId, () -> sessionRepository.findById(sessionId));
    }

    private static void saveSessions(SessionRepository sessionRepository, Instant now) {
        sessionRepository.save(Session.builder()
            .id("expired-session")
            .userId("user-1")
            .createdAt(now.minus(Duration.ofDays(2)))
            .expiresAt(now.minus(Duration.ofDays(1)))
            .build());
        sessionRepository.save(Session.builder()
            .id("live-session")
            .userId("user-1")
            .createdAt(now)
            .expiresAt(now.plus(Duration.ofDays(1)))
            .build());
    }
}
