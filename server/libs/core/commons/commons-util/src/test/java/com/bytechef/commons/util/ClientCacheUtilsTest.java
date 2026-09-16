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

package com.bytechef.commons.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Scheduler;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class ClientCacheUtilsTest {

    private final AtomicLong tickerNanos = new AtomicLong();
    private final List<Runnable> deferredCloseTasks = new ArrayList<>();
    private final ManualScheduler manualScheduler = new ManualScheduler();

    private Cache<String, FakeClient> clients;

    @BeforeEach
    void setUp() {
        clients = ClientCacheUtils.createClientCache(clientCacheSettings(1), FakeClient::close);
    }

    @Test
    void testIdleClientIsClosedWhenTheSchedulerExpiresIt() {
        FakeClient fakeClient = clients.get("idle", key -> new FakeClient());

        assertThat(manualScheduler.scheduledTasks).isNotEmpty();

        tickerNanos.addAndGet(ClientCacheSettings.IDLE_TIMEOUT.plus(Duration.ofSeconds(1))
            .toNanos());

        manualScheduler.runScheduledTasks();

        assertThat(clients.asMap()).isEmpty();
        assertThat(fakeClient.closed).isTrue();
        assertThat(deferredCloseTasks).isEmpty();
    }

    @Test
    void testSizeEvictedClientIsClosedOnlyAfterTheGracePeriod() {
        FakeClient firstFakeClient = clients.get("first", key -> new FakeClient());
        FakeClient secondFakeClient = clients.get("second", key -> new FakeClient());

        clients.cleanUp();

        Map<String, FakeClient> remainingClients = clients.asMap();

        assertThat(remainingClients).hasSize(1);

        FakeClient evictedFakeClient = remainingClients.containsValue(firstFakeClient)
            ? secondFakeClient : firstFakeClient;
        FakeClient keptFakeClient = evictedFakeClient == firstFakeClient ? secondFakeClient : firstFakeClient;

        assertThat(evictedFakeClient.closed).isFalse();
        assertThat(deferredCloseTasks).hasSize(1);

        runDeferredCloseTasks();

        assertThat(evictedFakeClient.closed).isTrue();
        assertThat(keptFakeClient.closed).isFalse();
    }

    @Test
    void testClientObtainedBeforeASizeEvictionStillWorksAfterIt() {
        FakeClient firstFakeClient = clients.get("first", key -> new FakeClient());
        FakeClient secondFakeClient = clients.get("second", key -> new FakeClient());

        clients.cleanUp();

        assertThat(clients.asMap()).hasSize(1);
        assertThat(firstFakeClient.use()).isEqualTo("used");
        assertThat(secondFakeClient.use()).isEqualTo("used");
    }

    @Test
    void testInvalidatedClientIsClosedImmediately() {
        FakeClient fakeClient = clients.get("invalidated", key -> new FakeClient());

        clients.invalidate("invalidated");

        assertThat(fakeClient.closed).isTrue();
        assertThat(deferredCloseTasks).isEmpty();
    }

    @Test
    void testFailingDeferredCloseDoesNotPropagate() {
        Cache<String, FakeClient> failingClients = ClientCacheUtils.createClientCache(
            clientCacheSettings(1), fakeClient -> {
                throw new IllegalStateException("close failed");
            });

        failingClients.put("first", new FakeClient());
        failingClients.put("second", new FakeClient());

        failingClients.cleanUp();

        assertThat(deferredCloseTasks).hasSize(1);
        assertThatCode(this::runDeferredCloseTasks).doesNotThrowAnyException();
    }

    private ClientCacheSettings clientCacheSettings(long maximumSize) {
        return new ClientCacheSettings(
            maximumSize, tickerNanos::get, manualScheduler, Runnable::run, deferredCloseTasks::add);
    }

    private void runDeferredCloseTasks() {
        List<Runnable> tasks = new ArrayList<>(deferredCloseTasks);

        deferredCloseTasks.clear();

        tasks.forEach(Runnable::run);
    }

    private static final class FakeClient {

        private boolean closed;

        void close() {
            closed = true;
        }

        String use() {
            if (closed) {
                throw new IllegalStateException("Client is closed");
            }

            return "used";
        }
    }

    private static final class ManualScheduler implements Scheduler {

        private final List<Runnable> scheduledTasks = new ArrayList<>();

        @Override
        public Future<?> schedule(Executor executor, Runnable command, long delay, TimeUnit unit) {
            scheduledTasks.add(() -> executor.execute(command));

            return new CompletableFuture<>();
        }

        void runScheduledTasks() {
            List<Runnable> tasks = new ArrayList<>(scheduledTasks);

            scheduledTasks.clear();

            tasks.forEach(Runnable::run);
        }
    }
}
