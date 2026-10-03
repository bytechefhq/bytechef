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

package com.bytechef.automation.ai.a2a.server.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

/**
 * @author Ivica Cardic
 */
class AutomationA2AServerConfigurationTest {

    @Test
    void testRunCompletionExecutorRunsOnTheSyncWorkerLaneWithoutBlockingTheSubmitter() throws InterruptedException {
        SimpleAsyncTaskExecutor syncWorkerExecutor = new SimpleAsyncTaskExecutor("sync-worker-");

        syncWorkerExecutor.setConcurrencyLimit(1);

        CountDownLatch releaseLatch = new CountDownLatch(1);

        syncWorkerExecutor.execute(() -> awaitQuietly(releaseLatch));

        Executor runCompletionExecutor = AutomationA2AServerConfiguration.runCompletionExecutor(syncWorkerExecutor);

        CountDownLatch completionLatch = new CountDownLatch(1);
        AtomicReference<String> threadName = new AtomicReference<>();

        long start = System.nanoTime();

        runCompletionExecutor.execute(() -> {
            threadName.set(Thread.currentThread()
                .getName());

            completionLatch.countDown();
        });

        assertThat(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start)).isLessThan(1000);
        assertThat(completionLatch.await(300, TimeUnit.MILLISECONDS)).isFalse();

        releaseLatch.countDown();

        assertThat(completionLatch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(threadName.get()).startsWith("sync-worker-");
    }

    private static void awaitQuietly(CountDownLatch latch) {
        try {
            assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
        } catch (InterruptedException interruptedException) {
            Thread.currentThread()
                .interrupt();
        }
    }
}
