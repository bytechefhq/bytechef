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

package com.bytechef.component.ai.agent.chat.memory.neo4j.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.bytechef.commons.util.ClientCacheSettings;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Scheduler;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;

/**
 * @author Ivica Cardic
 */
class Neo4jChatMemoryUtilsTest {

    private final List<Runnable> deferredCloseTasks = new ArrayList<>();
    private final List<Runnable> scheduledTasks = new ArrayList<>();
    private final AtomicLong tickerNanos = new AtomicLong();

    private final Scheduler manualScheduler = (executor, command, delay, timeUnit) -> {
        scheduledTasks.add(() -> executor.execute(command));

        return new CompletableFuture<>();
    };

    @Test
    void testSameConnectionReusesOneDriver() {
        Driver firstDriver = Neo4jChatMemoryUtils.getSharedDriver(connectionParameters("secret-a"));
        Driver secondDriver = Neo4jChatMemoryUtils.getSharedDriver(connectionParameters("secret-a"));

        assertThat(secondDriver).isSameAs(firstDriver);
    }

    @Test
    void testDifferentCredentialsGetDifferentDrivers() {
        Driver firstDriver = Neo4jChatMemoryUtils.getSharedDriver(connectionParameters("secret-a"));
        Driver secondDriver = Neo4jChatMemoryUtils.getSharedDriver(connectionParameters("secret-b"));

        assertThat(secondDriver).isNotSameAs(firstDriver);
    }

    @Test
    void testIdleClientIsClosedWhenTheSchedulerExpiresIt() {
        Cache<String, Driver> drivers = Neo4jChatMemoryUtils.createClientCache(clientCacheSettings());

        Driver idleDriver = mock(Driver.class);

        drivers.put("idle", idleDriver);

        Duration expiredIdleTime = ClientCacheSettings.IDLE_TIMEOUT.plusSeconds(1);

        tickerNanos.addAndGet(expiredIdleTime.toNanos());

        runTasks(scheduledTasks);

        assertThat(drivers.asMap()).isEmpty();

        verify(idleDriver).close();
    }

    @Test
    void testSizeEvictedClientIsClosedOnlyAfterTheGracePeriod() {
        Cache<String, Driver> drivers = Neo4jChatMemoryUtils.createClientCache(clientCacheSettings());

        Driver firstDriver = mock(Driver.class);
        Driver secondDriver = mock(Driver.class);

        drivers.put("first", firstDriver);
        drivers.put("second", secondDriver);

        drivers.cleanUp();

        Map<String, Driver> remainingDrivers = drivers.asMap();

        assertThat(remainingDrivers).hasSize(1);

        Driver evictedDriver = remainingDrivers.containsValue(firstDriver) ? secondDriver : firstDriver;
        Driver keptDriver = evictedDriver == firstDriver ? secondDriver : firstDriver;

        verify(evictedDriver, never()).close();

        runTasks(deferredCloseTasks);

        verify(evictedDriver).close();
        verify(keptDriver, never()).close();
    }

    @Test
    void testDriverKeyDescriptionOmitsCredentials() {
        Neo4jChatMemoryUtils.DriverKey driverKey = new Neo4jChatMemoryUtils.DriverKey(
            "neo4j://admin:uri-secret@localhost:7687", "admin", "secret");

        assertThat(driverKey.toString()).isEqualTo("DriverKey{uri=neo4j://localhost:7687, username=admin}");
    }

    private static Parameters connectionParameters(String password) {
        return MockParametersFactory.create(
            Map.of("uri", "bolt://localhost:7687", "username", "neo4j", "password", password));
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
