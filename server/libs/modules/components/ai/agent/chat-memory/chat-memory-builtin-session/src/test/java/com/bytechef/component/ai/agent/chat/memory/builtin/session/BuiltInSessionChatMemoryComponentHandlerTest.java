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

package com.bytechef.component.ai.agent.chat.memory.builtin.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.platform.component.definition.ai.agent.SessionRepositoryFunction;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.service.TenantService;
import com.bytechef.test.jsonasssert.JsonFileAssert;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.ai.session.Session;
import org.springframework.ai.session.SessionRepository;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.mock.env.MockEnvironment;

/**
 * @author Ivica Cardic
 */
public class BuiltInSessionChatMemoryComponentHandlerTest {

    private static final Instant START = Instant.parse("2026-01-01T00:00:00Z");
    private static final String TENANT_A = "tenanta";
    private static final String TENANT_B = "tenantb";

    @Test
    public void testGetComponentDefinition() {
        JsonFileAssert.assertEquals(
            "definition/built-in-session-chat-memory_v1.json",
            new BuiltInSessionChatMemoryComponentHandler(
                null, createInMemoryEnvironment(),
                new StaticListableBeanFactory().getBeanProvider(TenantService.class))
                    .getDefinition());
    }

    @Test
    public void testDeleteExpiredSessionsWithoutTenantServiceCoversOnlyTheDefaultTenant() throws Exception {
        MutableClock clock = new MutableClock(START);

        BuiltInSessionChatMemoryComponentHandler componentHandler = createComponentHandler(
            createInMemoryEnvironment(), new StaticListableBeanFactory(), clock);

        SessionRepository sessionRepository = getSessionRepository(componentHandler);

        saveExpiredSession(sessionRepository, TenantContext.DEFAULT_TENANT_ID);
        saveExpiredSession(sessionRepository, TENANT_A);

        clock.advance(Duration.ofDays(2));

        componentHandler.deleteExpiredSessions();

        clock.advance(Duration.ofDays(-2));

        assertThat(findExpiredSession(sessionRepository, TenantContext.DEFAULT_TENANT_ID)).isNull();
        assertThat(findExpiredSession(sessionRepository, TENANT_A)).isNotNull();
    }

    @Test
    public void testDeleteExpiredSessionsWithMultiTenancyDisabledCoversOnlyTheDefaultTenant() throws Exception {
        TenantService tenantService = mock(TenantService.class);

        when(tenantService.isMultiTenantEnabled()).thenReturn(false);

        MutableClock clock = new MutableClock(START);

        BuiltInSessionChatMemoryComponentHandler componentHandler = createComponentHandler(
            createInMemoryEnvironment(), createBeanFactory(tenantService), clock);

        SessionRepository sessionRepository = getSessionRepository(componentHandler);

        saveExpiredSession(sessionRepository, TenantContext.DEFAULT_TENANT_ID);
        saveExpiredSession(sessionRepository, TENANT_A);

        clock.advance(Duration.ofDays(2));

        componentHandler.deleteExpiredSessions();

        clock.advance(Duration.ofDays(-2));

        assertThat(findExpiredSession(sessionRepository, TenantContext.DEFAULT_TENANT_ID)).isNull();
        assertThat(findExpiredSession(sessionRepository, TENANT_A)).isNotNull();

        verify(tenantService, never()).getTenantIds();
    }

    @Test
    public void testDeleteExpiredSessionsWithMultiTenancyEnabledCoversEveryTenant() throws Exception {
        TenantService tenantService = mock(TenantService.class);

        when(tenantService.isMultiTenantEnabled()).thenReturn(true);
        when(tenantService.getTenantIds()).thenReturn(List.of(TENANT_A, TENANT_B));

        MutableClock clock = new MutableClock(START);

        BuiltInSessionChatMemoryComponentHandler componentHandler = createComponentHandler(
            createInMemoryEnvironment().withProperty("bytechef.tenant.mode", "multi"),
            createBeanFactory(tenantService), clock);

        SessionRepository sessionRepository = getSessionRepository(componentHandler);

        saveExpiredSession(sessionRepository, TenantContext.DEFAULT_TENANT_ID);
        saveExpiredSession(sessionRepository, TENANT_A);
        saveExpiredSession(sessionRepository, TENANT_B);

        clock.advance(Duration.ofDays(2));

        componentHandler.deleteExpiredSessions();

        clock.advance(Duration.ofDays(-2));

        assertThat(findExpiredSession(sessionRepository, TENANT_A)).isNull();
        assertThat(findExpiredSession(sessionRepository, TENANT_B)).isNull();
        assertThat(findExpiredSession(sessionRepository, TenantContext.DEFAULT_TENANT_ID)).isNotNull();
    }

    @Test
    public void testSessionInUseOutlivesTheTimeToLive() throws Exception {
        MutableClock clock = new MutableClock(START);

        BuiltInSessionChatMemoryComponentHandler componentHandler = createComponentHandler(
            createInMemoryEnvironment(), new StaticListableBeanFactory(), clock);

        SessionRepository sessionRepository = getSessionRepository(componentHandler);

        TenantContext.runWithTenantId(TenantContext.DEFAULT_TENANT_ID, () -> {
            saveSession(sessionRepository, "active-session");
            saveSession(sessionRepository, "idle-session");
        });

        for (int turn = 0; turn < 10; turn++) {
            clock.advance(Duration.ofHours(13));

            assertThat(findSession(sessionRepository, TenantContext.DEFAULT_TENANT_ID, "active-session"))
                .isNotNull();
        }

        componentHandler.deleteExpiredSessions();

        clock.advance(Duration.ofHours(-120));

        assertThat(findSession(sessionRepository, TenantContext.DEFAULT_TENANT_ID, "active-session")).isNotNull();
        assertThat(findSession(sessionRepository, TenantContext.DEFAULT_TENANT_ID, "idle-session")).isNull();
    }

    @Test
    public void testMissingTenantServiceInMultiTenantModeIsReportedOnce() {
        BuiltInSessionChatMemoryComponentHandler componentHandler = createComponentHandler(
            createInMemoryEnvironment().withProperty("bytechef.tenant.mode", "multi"),
            new StaticListableBeanFactory());

        List<ILoggingEvent> errorEvents = captureErrorEvents(() -> {
            componentHandler.deleteExpiredSessions();
            componentHandler.deleteExpiredSessions();
        });

        assertThat(errorEvents).hasSize(1);
        assertThat(errorEvents.getFirst()
            .getFormattedMessage()).contains("bytechef.tenant.mode=multi but no TenantService is available");
    }

    @Test
    public void testMissingTenantServiceInSingleTenantModeIsNotReported() {
        BuiltInSessionChatMemoryComponentHandler componentHandler = createComponentHandler(
            createInMemoryEnvironment().withProperty("bytechef.tenant.mode", "single"),
            new StaticListableBeanFactory());

        List<ILoggingEvent> errorEvents = captureErrorEvents(componentHandler::deleteExpiredSessions);

        assertThat(errorEvents).isEmpty();
    }

    private static List<ILoggingEvent> captureErrorEvents(Runnable runnable) {
        Logger logger = (Logger) LoggerFactory.getLogger(BuiltInSessionChatMemoryComponentHandler.class);

        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();

        listAppender.start();

        logger.addAppender(listAppender);

        try {
            runnable.run();
        } finally {
            logger.detachAppender(listAppender);
        }

        return listAppender.list.stream()
            .filter(loggingEvent -> loggingEvent.getLevel() == Level.ERROR)
            .toList();
    }

    private static StaticListableBeanFactory createBeanFactory(TenantService tenantService) {
        StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();

        beanFactory.addBean("tenantService", tenantService);

        return beanFactory;
    }

    private static BuiltInSessionChatMemoryComponentHandler createComponentHandler(
        MockEnvironment environment, StaticListableBeanFactory beanFactory) {

        return createComponentHandler(environment, beanFactory, new MutableClock(START));
    }

    private static BuiltInSessionChatMemoryComponentHandler createComponentHandler(
        MockEnvironment environment, StaticListableBeanFactory beanFactory, Clock clock) {

        return new BuiltInSessionChatMemoryComponentHandler(
            null, environment, beanFactory.getBeanProvider(TenantService.class), clock);
    }

    private static MockEnvironment createEnvironment() {
        return new MockEnvironment().withProperty("bytechef.ai.memory.session-time-to-live", "P1D");
    }

    private static MockEnvironment createInMemoryEnvironment() {
        return createEnvironment().withProperty("bytechef.ai.memory.provider", "in_memory");
    }

    private static Session findExpiredSession(SessionRepository sessionRepository, String tenantId) {
        return findSession(sessionRepository, tenantId, "expired-session");
    }

    private static Session findSession(SessionRepository sessionRepository, String tenantId, String sessionId) {
        return TenantContext.callWithTenantId(tenantId, () -> sessionRepository.findById(sessionId));
    }

    @SuppressWarnings("unchecked")
    private static SessionRepository getSessionRepository(
        BuiltInSessionChatMemoryComponentHandler componentHandler) throws Exception {

        List<ClusterElementDefinition<?>> clusterElementDefinitions = componentHandler.getDefinition()
            .getClusterElements()
            .orElseThrow();

        ClusterElementDefinition<SessionRepositoryFunction> clusterElementDefinition =
            (ClusterElementDefinition<SessionRepositoryFunction>) clusterElementDefinitions.getFirst();

        SessionRepositoryFunction sessionRepositoryFunction = clusterElementDefinition.getElement();

        return sessionRepositoryFunction.apply(null, null, null, null);
    }

    private static void saveExpiredSession(SessionRepository sessionRepository, String tenantId) {
        TenantContext.runWithTenantId(tenantId, () -> saveSession(sessionRepository, "expired-session"));
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
