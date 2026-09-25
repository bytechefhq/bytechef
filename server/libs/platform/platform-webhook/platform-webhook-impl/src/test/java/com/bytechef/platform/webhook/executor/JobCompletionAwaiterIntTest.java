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

package com.bytechef.platform.webhook.executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.bytechef.atlas.coordinator.event.JobStatusApplicationEvent;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.repository.memory.InMemoryJobRepository;
import com.bytechef.atlas.execution.repository.memory.InMemoryTaskExecutionRepository;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.JobServiceImpl;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.message.broker.MessageBroker;
import com.bytechef.message.broker.memory.config.MemoryMessageBrokerConfiguration;
import com.bytechef.message.broker.memory.config.MemoryMessageBrokerListenerRegistrarConfiguration;
import com.bytechef.platform.coordinator.event.listener.SseStreamApplicationEventListener;
import com.bytechef.platform.webhook.event.SseStreamEvent;
import com.bytechef.platform.webhook.executor.config.JobCompletionAwaiterConfiguration;
import com.bytechef.platform.webhook.executor.config.SseStreamMessageBrokerConfigurerConfiguration;
import com.bytechef.platform.webhook.message.route.SseStreamMessageRoute;
import com.bytechef.tenant.TenantContext;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(properties = "bytechef.message-broker.provider=memory")
class JobCompletionAwaiterIntTest {

    private static final Duration EVENT_AWAIT_TIMEOUT = Duration.ofSeconds(60);
    private static final long FUTURE_WAIT_SECONDS = 30;

    @Autowired
    private JobCompletionAwaiterImpl jobCompletionAwaiter;

    @Autowired
    private JobService jobService;

    @Autowired
    private MessageBroker messageBroker;

    @Autowired
    private SseStreamApplicationEventListener sseStreamApplicationEventListener;

    @Autowired
    private SseStreamBridgeRegistry sseStreamBridgeRegistry;

    @Test
    void testCompletesOnTerminalEventAfterAwait() throws Exception {
        Job job = createJob(Job.Status.STARTED);

        long jobId = getJobId(job);

        SseStreamBridgeRegistry.Registration registration = sseStreamBridgeRegistry.register(jobId, payload -> {});

        CompletableFuture<Job> future = jobCompletionAwaiter.await(jobId, EVENT_AWAIT_TIMEOUT);

        assertFalse(future.isDone());

        finishJob(job, Job.Status.COMPLETED);

        Job completedJob = future.get(FUTURE_WAIT_SECONDS, TimeUnit.SECONDS);

        assertEquals(job, completedJob);
        assertEquals(Job.Status.COMPLETED, completedJob.getStatus());

        CompletableFuture<Void> streamCompletion = registration.completion()
            .toCompletableFuture();

        streamCompletion.get(FUTURE_WAIT_SECONDS, TimeUnit.SECONDS);
    }

    @Test
    void testRaceGuardCompletesWhenJobAlreadyTerminalAtAwait() throws Exception {
        Job completedJob = createJob(Job.Status.COMPLETED);

        CompletableFuture<Job> future = jobCompletionAwaiter.await(getJobId(completedJob), EVENT_AWAIT_TIMEOUT);

        Job awaitedJob = future.get(FUTURE_WAIT_SECONDS, TimeUnit.SECONDS);

        assertEquals(completedJob, awaitedJob);
        assertEquals(Job.Status.COMPLETED, awaitedJob.getStatus());
    }

    @Test
    void testStoppedIsTerminal() throws Exception {
        Job startedJob = createJob(Job.Status.STARTED);

        Job stoppedJob = jobService.setStatusToStopped(getJobId(startedJob));

        CompletableFuture<Job> future = jobCompletionAwaiter.await(getJobId(stoppedJob), EVENT_AWAIT_TIMEOUT);

        Job awaitedJob = future.get(FUTURE_WAIT_SECONDS, TimeUnit.SECONDS);

        assertEquals(stoppedJob, awaitedJob);
        assertEquals(Job.Status.STOPPED, awaitedJob.getStatus());
    }

    @Test
    void testNonJobStatusEventIgnored() throws Exception {
        Job job = createJob(Job.Status.STARTED);
        Job sentinelJob = createJob(Job.Status.STARTED);

        CompletableFuture<Job> future = jobCompletionAwaiter.await(getJobId(job), EVENT_AWAIT_TIMEOUT);
        CompletableFuture<Job> sentinelFuture = jobCompletionAwaiter.await(getJobId(sentinelJob), EVENT_AWAIT_TIMEOUT);

        SseStreamEvent dataEvent = new SseStreamEvent(getJobId(job), SseStreamEvent.EVENT_TYPE_DATA, "COMPLETED");

        dataEvent.putMetadata(TenantContext.CURRENT_TENANT_ID, TenantContext.getCurrentTenantId());

        messageBroker.send(SseStreamMessageRoute.SSE_STREAM_EVENTS, dataEvent);

        finishJob(sentinelJob, Job.Status.COMPLETED);

        sentinelFuture.get(FUTURE_WAIT_SECONDS, TimeUnit.SECONDS);

        assertFalse(future.isDone());
    }

    @Test
    void testNonTerminalJobStatusIgnored() throws Exception {
        Job job = createJob(Job.Status.STARTED);
        Job sentinelJob = createJob(Job.Status.STARTED);

        CompletableFuture<Job> future = jobCompletionAwaiter.await(getJobId(job), EVENT_AWAIT_TIMEOUT);
        CompletableFuture<Job> sentinelFuture = jobCompletionAwaiter.await(getJobId(sentinelJob), EVENT_AWAIT_TIMEOUT);

        sseStreamApplicationEventListener.onApplicationEvent(
            new JobStatusApplicationEvent(getJobId(job), Job.Status.STARTED));

        finishJob(sentinelJob, Job.Status.COMPLETED);

        sentinelFuture.get(FUTURE_WAIT_SECONDS, TimeUnit.SECONDS);

        assertFalse(future.isDone());
    }

    @Test
    void testTimeout() {
        Job startedJob = createJob(Job.Status.STARTED);

        CompletableFuture<Job> future = jobCompletionAwaiter.await(getJobId(startedJob), Duration.ofMillis(100));

        ExecutionException executionException = assertThrows(
            ExecutionException.class, () -> future.get(FUTURE_WAIT_SECONDS, TimeUnit.SECONDS));

        assertInstanceOf(TimeoutException.class, executionException.getCause());
    }

    @Test
    void testTimeoutErrorNamesTheJob() {
        Job startedJob = createJob(Job.Status.STARTED);

        CompletableFuture<Job> future = jobCompletionAwaiter.await(getJobId(startedJob), Duration.ofMillis(100));

        ExecutionException executionException = assertThrows(
            ExecutionException.class, () -> future.get(FUTURE_WAIT_SECONDS, TimeUnit.SECONDS));

        Throwable cause = executionException.getCause();

        assertInstanceOf(TimeoutException.class, cause);
        assertEquals("Job %d did not finish within PT0.1S".formatted(getJobId(startedJob)), cause.getMessage());
    }

    @Test
    void testTimeoutCompletesWithTheJobWhenItFinishedWithoutAnEvent() throws Exception {
        Job job = createJob(Job.Status.STARTED);
        Duration timeout = Duration.ofSeconds(1);

        long startNanos = System.nanoTime();

        CompletableFuture<Job> future = jobCompletionAwaiter.await(getJobId(job), timeout);

        job.setStatus(Job.Status.COMPLETED);

        jobService.update(job);

        Job awaitedJob = future.get(FUTURE_WAIT_SECONDS, TimeUnit.SECONDS);

        long elapsedNanos = System.nanoTime() - startNanos;

        assertEquals(job, awaitedJob);
        assertEquals(Job.Status.COMPLETED, awaitedJob.getStatus());
        assertTrue(elapsedNanos >= timeout.toNanos());
    }

    private Job createJob(Job.Status status) {
        Job job = new Job();

        job.setStatus(status);

        return jobService.update(job);
    }

    private static long getJobId(Job job) {
        return Objects.requireNonNull(job.getId());
    }

    private void finishJob(Job job, Job.Status status) {
        job.setStatus(status);

        jobService.update(job);

        sseStreamApplicationEventListener.onApplicationEvent(new JobStatusApplicationEvent(getJobId(job), status));
    }

    @Configuration
    @Import({
        JobCompletionAwaiterConfiguration.class, MemoryMessageBrokerConfiguration.class,
        MemoryMessageBrokerListenerRegistrarConfiguration.class, SseStreamMessageBrokerConfigurerConfiguration.class
    })
    static class JobCompletionAwaiterIntTestConfiguration {

        @Bean
        JobService jobService() {
            return new JobServiceImpl(
                new InMemoryJobRepository(
                    new InMemoryTaskExecutionRepository(), JsonMapper.builder()
                        .build()));
        }

        @Bean
        SseStreamApplicationEventListener sseStreamApplicationEventListener(MessageBroker messageBroker) {
            return new SseStreamApplicationEventListener(messageBroker, mock(TaskExecutionService.class));
        }

        @Bean
        SseStreamBridgeRegistry sseStreamBridgeRegistry() {
            return new SseStreamBridgeRegistry();
        }
    }
}
