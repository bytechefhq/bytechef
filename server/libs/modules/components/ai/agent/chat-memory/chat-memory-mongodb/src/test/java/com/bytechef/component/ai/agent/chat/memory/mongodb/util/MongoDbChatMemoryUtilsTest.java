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

package com.bytechef.component.ai.agent.chat.memory.mongodb.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.bytechef.commons.util.ClientCacheSettings;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Scheduler;
import com.mongodb.client.MongoClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class MongoDbChatMemoryUtilsTest {

    private final List<Runnable> deferredCloseTasks = new ArrayList<>();
    private final List<Runnable> scheduledTasks = new ArrayList<>();
    private final AtomicLong tickerNanos = new AtomicLong();

    private final Scheduler manualScheduler = (executor, command, delay, timeUnit) -> {
        scheduledTasks.add(() -> executor.execute(command));

        return new CompletableFuture<>();
    };

    @Test
    void testSameConnectionReusesOneMongoClient() {
        MongoClient firstMongoClient = MongoDbChatMemoryUtils.getSharedMongoClient(connectionParameters("secret-a"));
        MongoClient secondMongoClient = MongoDbChatMemoryUtils.getSharedMongoClient(connectionParameters("secret-a"));

        assertThat(secondMongoClient).isSameAs(firstMongoClient);
    }

    @Test
    void testDifferentCredentialsGetDifferentMongoClients() {
        MongoClient firstMongoClient = MongoDbChatMemoryUtils.getSharedMongoClient(connectionParameters("secret-a"));
        MongoClient secondMongoClient = MongoDbChatMemoryUtils.getSharedMongoClient(connectionParameters("secret-b"));

        assertThat(secondMongoClient).isNotSameAs(firstMongoClient);
    }

    @Test
    void testIdleClientIsClosedWhenTheSchedulerExpiresIt() {
        Cache<String, MongoClient> mongoClients = MongoDbChatMemoryUtils.createClientCache(clientCacheSettings());

        MongoClient idleMongoClient = mock(MongoClient.class);

        mongoClients.put("idle", idleMongoClient);

        Duration expiredIdleTime = ClientCacheSettings.IDLE_TIMEOUT.plusSeconds(1);

        tickerNanos.addAndGet(expiredIdleTime.toNanos());

        runTasks(scheduledTasks);

        assertThat(mongoClients.asMap()).isEmpty();

        verify(idleMongoClient).close();
    }

    @Test
    void testSizeEvictedClientIsClosedOnlyAfterTheGracePeriod() {
        Cache<String, MongoClient> mongoClients = MongoDbChatMemoryUtils.createClientCache(clientCacheSettings());

        MongoClient firstMongoClient = mock(MongoClient.class);
        MongoClient secondMongoClient = mock(MongoClient.class);

        mongoClients.put("first", firstMongoClient);
        mongoClients.put("second", secondMongoClient);

        mongoClients.cleanUp();

        Map<String, MongoClient> remainingMongoClients = mongoClients.asMap();

        assertThat(remainingMongoClients).hasSize(1);

        MongoClient evictedMongoClient =
            remainingMongoClients.containsValue(firstMongoClient) ? secondMongoClient : firstMongoClient;
        MongoClient keptMongoClient = evictedMongoClient == firstMongoClient ? secondMongoClient : firstMongoClient;

        verify(evictedMongoClient, never()).close();

        runTasks(deferredCloseTasks);

        verify(evictedMongoClient).close();
        verify(keptMongoClient, never()).close();
    }

    private static Parameters connectionParameters(String password) {
        return MockParametersFactory.create(
            Map.of(
                "connectionString", "mongodb://localhost:27017", "databaseName", "spring_ai", "username", "user",
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
