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
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.tenant.TenantContext;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.task.TaskExecutionProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
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
        environment.setProperty("bytechef.worker.task.sync-concurrency-limit", "2");
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
    void testSyncWorkerExecutorLimitMatchesSyncConcurrencyProperty() {
        SimpleAsyncTaskExecutor syncWorkerExecutor = (SimpleAsyncTaskExecutor) asyncConfiguration.syncWorkerExecutor();

        assertEquals(2, syncWorkerExecutor.getConcurrencyLimit());
    }

    @Test
    void testSyncWorkerExecutorIsADistinctBeanWithItsOwnLimit() {
        new ApplicationContextRunner()
            .withUserConfiguration(AsyncConfiguration.class)
            .withBean(ContextPropagatingTaskDecorator.class, ContextPropagatingTaskDecorator::new)
            .withBean(TaskExecutionProperties.class, TaskExecutionProperties::new)
            .withPropertyValues(
                "bytechef.worker.task.subscriptions.default=3",
                "bytechef.worker.task.sync-concurrency-limit=2",
                "spring.threads.virtual.enabled=true")
            .run(context -> {
                assertNull(context.getStartupFailure(), "context failed to start");

                SimpleAsyncTaskExecutor workerExecutor = context.getBean(
                    "workerExecutor", SimpleAsyncTaskExecutor.class);
                SimpleAsyncTaskExecutor syncWorkerExecutor = context.getBean(
                    "syncWorkerExecutor", SimpleAsyncTaskExecutor.class);

                assertNotSame(
                    workerExecutor, syncWorkerExecutor,
                    "the sync lane must be its own executor, or a saturated worker lane starves it");
                assertEquals(3, workerExecutor.getConcurrencyLimit());
                assertEquals(2, syncWorkerExecutor.getConcurrencyLimit());
            });
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

    @Test
    @Timeout(15)
    void testEveryExecutorPropagatesTenantId() throws Exception {
        List<TaskExecutor> executors = List.of(
            asyncConfiguration.getAsyncExecutor(), asyncConfiguration.messageEventExecutor(),
            asyncConfiguration.syncWorkerExecutor(), asyncConfiguration.workerExecutor());

        for (TaskExecutor executor : executors) {
            CompletableFuture<String> tenantIdSeenByTask = new CompletableFuture<>();

            TenantContext.setCurrentTenantId("tenant_1");

            executor.execute(() -> tenantIdSeenByTask.complete(TenantContext.getCurrentTenantId()));

            assertEquals("tenant_1", tenantIdSeenByTask.get(5, TimeUnit.SECONDS));
        }
    }

    @Test
    @Timeout(15)
    void testContextPropagatingTaskDecoratorAppliedByEveryExecutor() throws Exception {
        MockEnvironment recordingEnvironment = new MockEnvironment();

        recordingEnvironment.setProperty("bytechef.worker.task.subscriptions.default", "3");
        recordingEnvironment.setProperty("spring.threads.virtual.enabled", "true");

        RecordingContextPropagatingTaskDecorator recordingContextPropagatingTaskDecorator =
            new RecordingContextPropagatingTaskDecorator();

        AsyncConfiguration recordingAsyncConfiguration = new AsyncConfiguration(
            recordingContextPropagatingTaskDecorator, recordingEnvironment, new TaskExecutionProperties());

        List<TaskExecutor> executors = List.of(
            recordingAsyncConfiguration.getAsyncExecutor(), recordingAsyncConfiguration.messageEventExecutor(),
            recordingAsyncConfiguration.syncWorkerExecutor(), recordingAsyncConfiguration.workerExecutor());

        for (TaskExecutor executor : executors) {
            CompletableFuture<Boolean> taskRan = new CompletableFuture<>();

            executor.execute(() -> taskRan.complete(true));

            assertTrue(taskRan.get(5, TimeUnit.SECONDS));
        }

        assertEquals(4, recordingContextPropagatingTaskDecorator.decorateCount.get());
    }

    @Test
    void testWorkerExecutorResolvesForNarrowerAsyncTaskExecutorQualifiedInjection() {
        new ApplicationContextRunner()
            .withUserConfiguration(WorkerExecutorConsumerConfiguration.class, AsyncConfiguration.class)
            .withBean(ContextPropagatingTaskDecorator.class, ContextPropagatingTaskDecorator::new)
            .withBean(TaskExecutionProperties.class, TaskExecutionProperties::new)
            .withPropertyValues(
                "bytechef.worker.task.subscriptions.default=3",
                "spring.threads.virtual.enabled=true")
            .run(context -> {
                assertTrue(context.getStartupFailure() == null, "context failed to start");

                WorkerExecutorConsumer workerExecutorConsumer = context.getBean(WorkerExecutorConsumer.class);

                assertTrue(workerExecutorConsumer.getWorkerExecutor() instanceof SimpleAsyncTaskExecutor);
            });
    }

    private static final class RecordingContextPropagatingTaskDecorator extends ContextPropagatingTaskDecorator {

        private final AtomicInteger decorateCount = new AtomicInteger();

        @Override
        public Runnable decorate(Runnable runnable) {
            decorateCount.incrementAndGet();

            return super.decorate(runnable);
        }
    }

    @Configuration
    static class WorkerExecutorConsumerConfiguration {

        @Bean
        WorkerExecutorConsumer workerExecutorConsumer(@Qualifier("workerExecutor") AsyncTaskExecutor workerExecutor) {
            return new WorkerExecutorConsumer(workerExecutor);
        }
    }

    private static final class WorkerExecutorConsumer {

        private final AsyncTaskExecutor workerExecutor;

        WorkerExecutorConsumer(AsyncTaskExecutor workerExecutor) {
            this.workerExecutor = workerExecutor;
        }

        AsyncTaskExecutor getWorkerExecutor() {
            return workerExecutor;
        }
    }
}
