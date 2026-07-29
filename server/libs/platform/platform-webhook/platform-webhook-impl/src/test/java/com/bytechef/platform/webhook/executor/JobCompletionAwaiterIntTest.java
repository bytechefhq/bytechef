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
import static org.mockito.Mockito.when;

import com.bytechef.atlas.coordinator.event.JobStatusApplicationEvent;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.message.broker.memory.AsyncMessageBroker;
import com.bytechef.platform.coordinator.event.listener.SseStreamApplicationEventListener;
import com.bytechef.platform.webhook.event.SseStreamEvent;
import com.bytechef.platform.webhook.message.route.SseStreamMessageRoute;
import com.bytechef.tenant.TenantContext;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.StandardEnvironment;

/**
 * Exercises the distributed-coordinator completion seam end to end through the real message broker: a terminal
 * {@link JobStatusApplicationEvent} (published exactly as the coordinator publishes it) flows through the real
 * {@link SseStreamApplicationEventListener} onto the {@code SSE_STREAM_EVENTS} broker route and completes the
 * {@code JobCompletionAwaiter}'s future on a different thread. This is the one link the awaiter unit tests cannot
 * cover, and the same broker-crossing path that makes completion work across processes in a microservices deployment.
 * The coordinator/worker job execution itself is pre-existing, separately-tested infrastructure and is represented here
 * by the terminal event the worker fleet ultimately emits.
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class JobCompletionAwaiterIntTest {

    @Mock
    private JobService jobService;

    @Mock
    private TaskExecutionService taskExecutionService;

    @Mock
    private TaskFileStorage taskFileStorage;

    private AsyncMessageBroker messageBroker;

    @AfterEach
    void tearDown() throws Exception {
        if (messageBroker != null) {
            messageBroker.destroy();
        }
    }

    @Test
    void testFutureCompletesWhenJobStatusEventFlowsThroughBroker() throws Exception {
        long jobId = 42L;

        Job startedJob = job(jobId, Job.Status.STARTED);
        Job completedJob = job(jobId, Job.Status.COMPLETED);

        when(jobService.fetchJob(jobId)).thenReturn(Optional.of(startedJob));
        when(jobService.getJob(jobId)).thenReturn(completedJob);

        messageBroker = new AsyncMessageBroker(new StandardEnvironment());

        JobCompletionAwaiterImpl jobCompletionAwaiter = new JobCompletionAwaiterImpl(jobService);

        // Mirror JobCompletionAwaiterConfiguration: deliver SSE_STREAM_EVENTS to the awaiter, restoring tenant context
        // from the event metadata on the broker thread.
        messageBroker.receive(
            SseStreamMessageRoute.SSE_STREAM_EVENTS,
            message -> {
                SseStreamEvent sseStreamEvent = (SseStreamEvent) message;

                TenantContext.runWithTenantId(
                    (String) sseStreamEvent.getMetadata(TenantContext.CURRENT_TENANT_ID),
                    () -> jobCompletionAwaiter.onSseStreamEvent(sseStreamEvent));
            });

        SseStreamApplicationEventListener sseStreamApplicationEventListener = new SseStreamApplicationEventListener(
            messageBroker, taskExecutionService, taskFileStorage);

        CompletableFuture<Job> future = jobCompletionAwaiter.await(jobId, Duration.ofSeconds(5));

        // The coordinator publishes this on terminal status; it must flow listener -> broker -> awaiter and complete
        // the future on the broker thread.
        sseStreamApplicationEventListener.onApplicationEvent(
            new JobStatusApplicationEvent(jobId, Job.Status.COMPLETED));

        assertEquals(completedJob, future.get(5, TimeUnit.SECONDS));
    }

    private static Job job(long id, Job.Status status) {
        Job job = new Job();

        job.setId(id);
        job.setStatus(status);

        return job;
    }
}
