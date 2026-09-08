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

package com.bytechef.async.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.tenant.TenantContext;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.boot.autoconfigure.task.TaskExecutionProperties;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.support.ContextPropagatingTaskDecorator;
import org.springframework.mock.env.MockEnvironment;

/**
 * @author Ivica Cardic
 */
class AsyncConfigurationTest {

    private AsyncConfiguration asyncConfiguration;
    private MockEnvironment environment;

    @BeforeEach
    void setUp() {
        environment = new MockEnvironment();

        environment.setProperty("bytechef.worker.task.subscriptions.default", "3");
        environment.setProperty("spring.threads.virtual.enabled", "true");

        TaskExecutionProperties taskExecutionProperties = new TaskExecutionProperties();

        TaskExecutionProperties.Simple simple = taskExecutionProperties.getSimple();

        simple.setConcurrencyLimit(7);

        asyncConfiguration = new AsyncConfiguration(
            new ContextPropagatingTaskDecorator(), environment, taskExecutionProperties);
    }

    @AfterEach
    void tearDown() {
        TenantContext.resetCurrentTenantId();
    }

    @Test
    void testWorkerExecutorLimitMatchesSubscriptionsProperty() {
        SimpleAsyncTaskExecutor workerExecutor = (SimpleAsyncTaskExecutor) asyncConfiguration.workerExecutor();

        assertEquals(3, workerExecutor.getConcurrencyLimit());
    }

    @Test
    void testMessageEventExecutorIsUnbounded() {
        SimpleAsyncTaskExecutor messageEventExecutor =
            (SimpleAsyncTaskExecutor) asyncConfiguration.messageEventExecutor();

        assertFalse(messageEventExecutor.isThrottleActive());
    }

    @Test
    void testTaskExecutorLimitMatchesSimpleConcurrencyLimit() {
        SimpleAsyncTaskExecutor taskExecutor = (SimpleAsyncTaskExecutor) asyncConfiguration.getAsyncExecutor();

        assertEquals(7, taskExecutor.getConcurrencyLimit());
    }

    @Test
    @Timeout(15)
    void testTenantIdReachesSubmittedTask() throws Exception {
        TaskExecutor workerExecutor = asyncConfiguration.workerExecutor();

        CompletableFuture<String> tenantIdSeenByTask = new CompletableFuture<>();

        TenantContext.setCurrentTenantId("tenant_1");

        workerExecutor.execute(() -> tenantIdSeenByTask.complete(TenantContext.getCurrentTenantId()));

        assertEquals("tenant_1", tenantIdSeenByTask.get(5, TimeUnit.SECONDS));
    }

    @Test
    @Timeout(15)
    void testVirtualThreadsWhenEnabled() throws Exception {
        CompletableFuture<Boolean> virtual = new CompletableFuture<>();

        asyncConfiguration.workerExecutor()
            .execute(() -> virtual.complete(Thread.currentThread()
                .isVirtual()));

        assertTrue(virtual.get(5, TimeUnit.SECONDS));
    }

    @Test
    @Timeout(15)
    void testPlatformThreadsWhenVirtualDisabled() throws Exception {
        environment.setProperty("spring.threads.virtual.enabled", "false");

        CompletableFuture<Boolean> virtual = new CompletableFuture<>();

        asyncConfiguration.workerExecutor()
            .execute(() -> virtual.complete(Thread.currentThread()
                .isVirtual()));

        assertFalse(virtual.get(5, TimeUnit.SECONDS));
    }

    @Test
    @Timeout(30)
    void testSaturatedWorkerExecutorDoesNotStarveMessageEventExecutor() throws Exception {
        TaskExecutor workerExecutor = asyncConfiguration.workerExecutor();
        TaskExecutor messageEventExecutor = asyncConfiguration.messageEventExecutor();

        CountDownLatch releaseLatch = new CountDownLatch(1);
        CountDownLatch blockedLatch = new CountDownLatch(3);

        for (int index = 0; index < 3; index++) {
            workerExecutor.execute(() -> {
                blockedLatch.countDown();

                try {
                    releaseLatch.await();
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread()
                        .interrupt();
                }
            });
        }

        assertTrue(blockedLatch.await(5, TimeUnit.SECONDS));

        CompletableFuture<Boolean> fourthTaskStarted = new CompletableFuture<>();

        Thread submitterThread = Thread.ofVirtual()
            .start(() -> workerExecutor.execute(() -> fourthTaskStarted.complete(true)));

        assertFalse(fourthTaskStarted.isDone(), "fourth worker task must wait for a permit");

        CompletableFuture<Boolean> bridgeTaskRan = new CompletableFuture<>();

        messageEventExecutor.execute(() -> bridgeTaskRan.complete(true));

        assertTrue(bridgeTaskRan.get(5, TimeUnit.SECONDS), "message event executor must not be starved");

        releaseLatch.countDown();

        assertTrue(fourthTaskStarted.get(5, TimeUnit.SECONDS));

        submitterThread.join(5000);
    }
}
