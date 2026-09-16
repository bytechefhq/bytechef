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

package com.bytechef.component.ai.agent.chat.memory.cassandra.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.commons.util.ClientCacheSettings;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.CqlSessionBuilder;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Scheduler;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * @author Ivica Cardic
 */
class CassandraChatMemoryUtilsTest {

    private final List<Runnable> deferredCloseTasks = new ArrayList<>();
    private final List<Runnable> scheduledTasks = new ArrayList<>();
    private final AtomicLong tickerNanos = new AtomicLong();

    private final Scheduler manualScheduler = (executor, command, delay, timeUnit) -> {
        scheduledTasks.add(() -> executor.execute(command));

        return new CompletableFuture<>();
    };

    @Test
    void testSameConnectionReusesOneCqlSession() {
        try (MockedStatic<CqlSession> cqlSessionMockedStatic = mockStatic(CqlSession.class)) {
            cqlSessionMockedStatic.when(CqlSession::builder)
                .thenAnswer(invocation -> newCqlSessionBuilder());

            CqlSession firstCqlSession = CassandraChatMemoryUtils.getSharedCqlSession(
                connectionParameters("reuse-cluster", "secret-a"));
            CqlSession secondCqlSession = CassandraChatMemoryUtils.getSharedCqlSession(
                connectionParameters("reuse-cluster", "secret-a"));

            assertThat(secondCqlSession).isSameAs(firstCqlSession);
        }
    }

    @Test
    void testDifferentCredentialsGetDifferentCqlSessions() {
        try (MockedStatic<CqlSession> cqlSessionMockedStatic = mockStatic(CqlSession.class)) {
            cqlSessionMockedStatic.when(CqlSession::builder)
                .thenAnswer(invocation -> newCqlSessionBuilder());

            CqlSession firstCqlSession = CassandraChatMemoryUtils.getSharedCqlSession(
                connectionParameters("distinct-cluster", "secret-a"));
            CqlSession secondCqlSession = CassandraChatMemoryUtils.getSharedCqlSession(
                connectionParameters("distinct-cluster", "secret-b"));

            assertThat(secondCqlSession).isNotSameAs(firstCqlSession);
        }
    }

    @Test
    void testIdleClientIsClosedWhenTheSchedulerExpiresIt() {
        Cache<String, CqlSession> cqlSessions = CassandraChatMemoryUtils.createClientCache(clientCacheSettings());

        CqlSession idleCqlSession = mock(CqlSession.class);

        cqlSessions.put("idle", idleCqlSession);

        Duration expiredIdleTime = ClientCacheSettings.IDLE_TIMEOUT.plusSeconds(1);

        tickerNanos.addAndGet(expiredIdleTime.toNanos());

        runTasks(scheduledTasks);

        assertThat(cqlSessions.asMap()).isEmpty();

        verify(idleCqlSession).close();
    }

    @Test
    void testSizeEvictedClientIsClosedOnlyAfterTheGracePeriod() {
        Cache<String, CqlSession> cqlSessions = CassandraChatMemoryUtils.createClientCache(clientCacheSettings());

        CqlSession firstCqlSession = mock(CqlSession.class);
        CqlSession secondCqlSession = mock(CqlSession.class);

        cqlSessions.put("first", firstCqlSession);
        cqlSessions.put("second", secondCqlSession);

        cqlSessions.cleanUp();

        Map<String, CqlSession> remainingCqlSessions = cqlSessions.asMap();

        assertThat(remainingCqlSessions).hasSize(1);

        CqlSession evictedCqlSession =
            remainingCqlSessions.containsValue(firstCqlSession) ? secondCqlSession : firstCqlSession;
        CqlSession keptCqlSession = evictedCqlSession == firstCqlSession ? secondCqlSession : firstCqlSession;

        verify(evictedCqlSession, never()).close();

        runTasks(deferredCloseTasks);

        verify(evictedCqlSession).close();
        verify(keptCqlSession, never()).close();
    }

    private static CqlSessionBuilder newCqlSessionBuilder() {
        CqlSessionBuilder cqlSessionBuilder = mock(CqlSessionBuilder.class, RETURNS_SELF);

        when(cqlSessionBuilder.build()).thenReturn(mock(CqlSession.class));

        return cqlSessionBuilder;
    }

    private static Parameters connectionParameters(String datacenter, String password) {
        return MockParametersFactory.create(
            Map.of(
                "contactPoints", "localhost", "port", 9042, "datacenter", datacenter, "username", "cassandra",
                "password", password));
    }

    private ClientCacheSettings clientCacheSettings() {
        return new ClientCacheSettings(1, tickerNanos::get, manualScheduler, Runnable::run, deferredCloseTasks::add);
    }

    private static void runTasks(List<Runnable> tasks) {
        List<Runnable> pendingTasks = new ArrayList<>(tasks);

        tasks.clear();

        pendingTasks.forEach(Runnable::run);
    }
}
