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

package com.bytechef.component.ai.agent.chat.memory.redis.session.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.bytechef.commons.util.ClientCacheSettings;
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
import org.springframework.ai.session.redis.RedisSessionRepository;
import redis.clients.jedis.RedisClient;

/**
 * @author Ivica Cardic
 */
class RedisSessionChatMemoryUtilsTest {

    private final List<Runnable> deferredCloseTasks = new ArrayList<>();
    private final List<Runnable> scheduledTasks = new ArrayList<>();
    private final AtomicLong tickerNanos = new AtomicLong();

    private final Scheduler manualScheduler = (executor, command, delay, timeUnit) -> {
        scheduledTasks.add(() -> executor.execute(command));

        return new CompletableFuture<>();
    };

    @Test
    void testGetSessionRepositoryBuildsRedisSessionRepository() {
        try (RedisClient redisClient = RedisSessionChatMemoryUtils.getRedisClient(
            MockParametersFactory.create(Map.of("host", "localhost", "port", 6379)))) {

            assertInstanceOf(RedisClient.class, redisClient);
        }

        assertInstanceOf(
            RedisSessionRepository.class,
            RedisSessionChatMemoryUtils.getSessionRepository(
                MockParametersFactory.create(Map.of("host", "localhost", "port", 6379))));
    }

    @Test
    void testGetRedisClientRequiresPasswordWhenUsernameIsProvided() {
        assertThrows(
            IllegalArgumentException.class,
            () -> RedisSessionChatMemoryUtils.getRedisClient(
                MockParametersFactory.create(Map.of("host", "localhost", "port", 6379, "username", "user"))));
    }

    @Test
    void testSameConnectionReusesOneRedisClient() {
        RedisClient firstRedisClient = RedisSessionChatMemoryUtils.getSharedRedisClient(
            MockParametersFactory.create(Map.of("host", "localhost", "port", 6379)));
        RedisClient secondRedisClient = RedisSessionChatMemoryUtils.getSharedRedisClient(
            MockParametersFactory.create(Map.of("host", "localhost", "port", 6379)));

        assertSame(firstRedisClient, secondRedisClient);
    }

    @Test
    void testDifferentConnectionsGetDifferentRedisClients() {
        RedisClient firstRedisClient = RedisSessionChatMemoryUtils.getSharedRedisClient(
            MockParametersFactory.create(Map.of("host", "localhost", "port", 6379)));
        RedisClient secondRedisClient = RedisSessionChatMemoryUtils.getSharedRedisClient(
            MockParametersFactory.create(Map.of("host", "localhost", "port", 6380)));

        assertNotSame(firstRedisClient, secondRedisClient);
    }

    @Test
    void testIdleClientIsClosedWhenTheSchedulerExpiresIt() {
        Cache<String, RedisClient> redisClients = RedisSessionChatMemoryUtils.createClientCache(clientCacheSettings());

        RedisClient idleRedisClient = mock(RedisClient.class);

        redisClients.put("idle", idleRedisClient);

        Duration expiredIdleTime = ClientCacheSettings.IDLE_TIMEOUT.plusSeconds(1);

        tickerNanos.addAndGet(expiredIdleTime.toNanos());

        runTasks(scheduledTasks);

        assertThat(redisClients.asMap()).isEmpty();

        verify(idleRedisClient).close();
    }

    @Test
    void testSizeEvictedClientIsClosedOnlyAfterTheGracePeriod() {
        Cache<String, RedisClient> redisClients = RedisSessionChatMemoryUtils.createClientCache(clientCacheSettings());

        RedisClient firstRedisClient = mock(RedisClient.class);
        RedisClient secondRedisClient = mock(RedisClient.class);

        redisClients.put("first", firstRedisClient);
        redisClients.put("second", secondRedisClient);

        redisClients.cleanUp();

        Map<String, RedisClient> remainingRedisClients = redisClients.asMap();

        assertThat(remainingRedisClients).hasSize(1);

        RedisClient evictedRedisClient =
            remainingRedisClients.containsValue(firstRedisClient) ? secondRedisClient : firstRedisClient;
        RedisClient keptRedisClient = evictedRedisClient == firstRedisClient ? secondRedisClient : firstRedisClient;

        verify(evictedRedisClient, never()).close();

        runTasks(deferredCloseTasks);

        verify(evictedRedisClient).close();
        verify(keptRedisClient, never()).close();
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
