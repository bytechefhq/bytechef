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

package com.bytechef.component.ai.agent.chat.memory.in.memory.session;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.platform.component.definition.ai.agent.SessionRepositoryFunction;
import com.bytechef.tenant.TenantContext;
import com.bytechef.test.jsonasssert.JsonFileAssert;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.ai.session.Session;
import org.springframework.ai.session.SessionRepository;
import org.springframework.mock.env.MockEnvironment;

/**
 * @author Ivica Cardic
 */
public class InMemorySessionChatMemoryComponentHandlerTest {

    private static final Instant START = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    public void testGetComponentDefinition() {
        JsonFileAssert.assertEquals(
            "definition/in-memory-session-chat-memory_v1.json",
            new InMemorySessionChatMemoryComponentHandler(createEnvironment()).getDefinition());
    }

    @Test
    public void testDeleteExpiredSessionsCoversEveryTenant() throws Exception {
        MutableClock clock = new MutableClock(START);

        InMemorySessionChatMemoryComponentHandler componentHandler = new InMemorySessionChatMemoryComponentHandler(
            createEnvironment(), clock);

        SessionRepository sessionRepository = getSessionRepository(componentHandler);

        TenantContext.runWithTenantId("tenanta", () -> saveSession(sessionRepository, "expired-session"));
        TenantContext.runWithTenantId("tenantb", () -> saveSession(sessionRepository, "expired-session"));

        clock.advance(Duration.ofDays(2));

        TenantContext.runWithTenantId("tenanta", () -> saveSession(sessionRepository, "live-session"));
        TenantContext.runWithTenantId("tenantb", () -> saveSession(sessionRepository, "live-session"));

        componentHandler.deleteExpiredSessions();

        clock.advance(Duration.ofDays(-2));

        assertThat(findSession(sessionRepository, "tenanta", "expired-session")).isNull();
        assertThat(findSession(sessionRepository, "tenantb", "expired-session")).isNull();
        assertThat(findSession(sessionRepository, "tenanta", "live-session")).isNotNull();
        assertThat(findSession(sessionRepository, "tenantb", "live-session")).isNotNull();
    }

    @Test
    public void testSessionInUseOutlivesTheTimeToLive() throws Exception {
        MutableClock clock = new MutableClock(START);

        InMemorySessionChatMemoryComponentHandler componentHandler = new InMemorySessionChatMemoryComponentHandler(
            createEnvironment(), clock);

        SessionRepository sessionRepository = getSessionRepository(componentHandler);

        TenantContext.runWithTenantId("tenanta", () -> {
            saveSession(sessionRepository, "active-session");
            saveSession(sessionRepository, "idle-session");
        });

        for (int turn = 0; turn < 10; turn++) {
            clock.advance(Duration.ofHours(13));

            assertThat(findSession(sessionRepository, "tenanta", "active-session")).isNotNull();
        }

        componentHandler.deleteExpiredSessions();

        clock.advance(Duration.ofHours(-120));

        assertThat(findSession(sessionRepository, "tenanta", "active-session")).isNotNull();
        assertThat(findSession(sessionRepository, "tenanta", "idle-session")).isNull();
    }

    @Test
    public void testDeleteExpiredSessionsLogsDeletionsAtInfoOnlyForTenantsWithDeletedSessions() throws Exception {
        MutableClock clock = new MutableClock(START);

        InMemorySessionChatMemoryComponentHandler componentHandler = new InMemorySessionChatMemoryComponentHandler(
            createEnvironment(), clock);

        SessionRepository sessionRepository = getSessionRepository(componentHandler);

        TenantContext.runWithTenantId("tenanta", () -> saveSession(sessionRepository, "expired-session"));

        clock.advance(Duration.ofDays(2));

        TenantContext.runWithTenantId("tenantb", () -> saveSession(sessionRepository, "live-session"));

        Logger logger = (Logger) LoggerFactory.getLogger(InMemorySessionChatMemoryComponentHandler.class);

        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();

        listAppender.start();

        logger.addAppender(listAppender);

        try {
            componentHandler.deleteExpiredSessions();
        } finally {
            logger.detachAppender(listAppender);
        }

        List<String> infoMessages = listAppender.list.stream()
            .filter(loggingEvent -> loggingEvent.getLevel() == Level.INFO)
            .map(ILoggingEvent::getFormattedMessage)
            .toList();

        assertThat(infoMessages).containsExactly("Deleted 1 expired in-memory sessions for tenant tenanta");
    }

    @Test
    public void testComponentHandlersDoNotShareSessions() throws Exception {
        SessionRepository firstSessionRepository = getSessionRepository(
            new InMemorySessionChatMemoryComponentHandler(createEnvironment()));
        SessionRepository secondSessionRepository = getSessionRepository(
            new InMemorySessionChatMemoryComponentHandler(createEnvironment()));

        TenantContext.runWithTenantId("tenanta", () -> saveSession(firstSessionRepository, "live-session"));

        assertThat(findSession(firstSessionRepository, "tenanta", "live-session")).isNotNull();
        assertThat(findSession(secondSessionRepository, "tenanta", "live-session")).isNull();
    }

    private static MockEnvironment createEnvironment() {
        return new MockEnvironment().withProperty("bytechef.ai.memory.session-time-to-live", "P1D");
    }

    private static @Nullable Session findSession(
        SessionRepository sessionRepository, String tenantId, String sessionId) {

        return TenantContext.callWithTenantId(tenantId, () -> sessionRepository.findById(sessionId));
    }

    @SuppressWarnings("unchecked")
    private static SessionRepository getSessionRepository(
        InMemorySessionChatMemoryComponentHandler componentHandler) throws Exception {

        List<ClusterElementDefinition<?>> clusterElementDefinitions = componentHandler.getDefinition()
            .getClusterElements()
            .orElseThrow();

        ClusterElementDefinition<SessionRepositoryFunction> clusterElementDefinition =
            (ClusterElementDefinition<SessionRepositoryFunction>) clusterElementDefinitions.getFirst();

        SessionRepositoryFunction sessionRepositoryFunction = clusterElementDefinition.getElement();

        return sessionRepositoryFunction.apply(null, null, null, null);
    }

    private static void saveSession(SessionRepository sessionRepository, String sessionId) {
        sessionRepository.saveIfAbsent(Session.builder()
            .id(sessionId)
            .userId("user-1")
            .build());
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
