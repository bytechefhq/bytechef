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
import static org.mockito.Mockito.when;

import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.platform.webhook.event.SseStreamEvent;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class JobCompletionAwaiterTest {

    @Mock
    private JobService jobService;

    @Test
    void testCompletesOnTerminalEventAfterAwait() throws Exception {
        Job startedJob = job(1L, Job.Status.STARTED);
        Job completedJob = job(1L, Job.Status.COMPLETED);

        when(jobService.fetchJob(1L)).thenReturn(Optional.of(startedJob));
        when(jobService.getJob(1L)).thenReturn(completedJob);

        JobCompletionAwaiterImpl awaiter = new JobCompletionAwaiterImpl(jobService);

        CompletableFuture<Job> future = awaiter.await(1L, Duration.ofSeconds(5));

        assertFalse(future.isDone());

        awaiter.onSseStreamEvent(jobStatusEvent(1L, "COMPLETED"));

        assertEquals(completedJob, future.get(2, TimeUnit.SECONDS));
    }

    @Test
    void testRaceGuardCompletesWhenJobAlreadyTerminalAtAwait() throws Exception {
        Job completedJob = job(2L, Job.Status.COMPLETED);

        when(jobService.fetchJob(2L)).thenReturn(Optional.of(completedJob));

        JobCompletionAwaiterImpl awaiter = new JobCompletionAwaiterImpl(jobService);

        CompletableFuture<Job> future = awaiter.await(2L, Duration.ofSeconds(5));

        assertEquals(completedJob, future.get(2, TimeUnit.SECONDS));
    }

    @Test
    void testStoppedIsTerminal() throws Exception {
        Job stoppedJob = job(3L, Job.Status.STOPPED);

        when(jobService.fetchJob(3L)).thenReturn(Optional.of(stoppedJob));

        JobCompletionAwaiterImpl awaiter = new JobCompletionAwaiterImpl(jobService);

        assertEquals(stoppedJob, awaiter.await(3L, Duration.ofSeconds(5))
            .get(2, TimeUnit.SECONDS));
    }

    @Test
    void testNonJobStatusEventIgnored() {
        Job startedJob = job(5L, Job.Status.STARTED);

        when(jobService.fetchJob(5L)).thenReturn(Optional.of(startedJob));

        JobCompletionAwaiterImpl awaiter = new JobCompletionAwaiterImpl(jobService);

        CompletableFuture<Job> future = awaiter.await(5L, Duration.ofSeconds(5));

        awaiter.onSseStreamEvent(new SseStreamEvent(5L, SseStreamEvent.EVENT_TYPE_DATA, "chunk"));

        assertFalse(future.isDone());
    }

    @Test
    void testNonTerminalJobStatusIgnored() {
        Job startedJob = job(6L, Job.Status.STARTED);

        when(jobService.fetchJob(6L)).thenReturn(Optional.of(startedJob));

        JobCompletionAwaiterImpl awaiter = new JobCompletionAwaiterImpl(jobService);

        CompletableFuture<Job> future = awaiter.await(6L, Duration.ofSeconds(5));

        awaiter.onSseStreamEvent(jobStatusEvent(6L, "STARTED"));

        assertFalse(future.isDone());
    }

    @Test
    void testTimeout() {
        Job startedJob = job(4L, Job.Status.STARTED);

        when(jobService.fetchJob(4L)).thenReturn(Optional.of(startedJob));

        JobCompletionAwaiterImpl awaiter = new JobCompletionAwaiterImpl(jobService);

        CompletableFuture<Job> future = awaiter.await(4L, Duration.ofMillis(100));

        ExecutionException executionException = assertThrows(
            ExecutionException.class, () -> future.get(2, TimeUnit.SECONDS));

        assertInstanceOf(TimeoutException.class, executionException.getCause());
    }

    private static Job job(long id, Job.Status status) {
        Job job = new Job();

        job.setId(id);
        job.setStatus(status);

        return job;
    }

    private static SseStreamEvent jobStatusEvent(long jobId, String status) {
        return new SseStreamEvent(jobId, SseStreamEvent.EVENT_TYPE_JOB_STATUS, status);
    }
}
