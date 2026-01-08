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

package com.bytechef.platform.job.sync.executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.coordinator.event.JobStatusApplicationEvent;
import com.bytechef.atlas.coordinator.event.StopJobEvent;
import com.bytechef.atlas.coordinator.event.TaskExecutionCompleteEvent;
import com.bytechef.atlas.coordinator.event.TaskExecutionErrorEvent;
import com.bytechef.atlas.coordinator.event.TaskStartedApplicationEvent;
import com.bytechef.atlas.coordinator.message.route.TaskCoordinatorMessageRoute;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.atlas.worker.task.handler.TaskHandlerRegistry;
import com.bytechef.commons.util.ConvertUtils;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.message.broker.memory.MemoryMessageBroker;
import com.bytechef.message.broker.memory.SyncMessageBroker;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.constant.TenantConstants;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author Ivica Cardic
 */
class JobSyncExecutorTest {

    private static final String TENANT = "t1";

    private MemoryMessageBroker memoryMessageBroker;
    private JobSyncExecutor jobSyncExecutor;
    private final JobService jobService = Mockito.mock(JobService.class);
    private final TaskExecutionService taskExecutionService = Mockito.mock(TaskExecutionService.class);

    @BeforeEach
    void beforeEach() {
        ObjectMapper objectMapper = JsonMapper.builder()
            .build();

        ConvertUtils.setObjectMapper(objectMapper);
        JsonUtils.setObjectMapper(objectMapper);
        MapUtils.setObjectMapper(objectMapper);

        TenantContext.setCurrentTenantId(TENANT);

        memoryMessageBroker = new SyncMessageBroker();

        ContextService contextService = Mockito.mock(ContextService.class);
        Evaluator evaluator = Mockito.mock(Evaluator.class);
        TaskHandlerRegistry taskHandlerRegistry = Mockito.mock(TaskHandlerRegistry.class);
        TaskFileStorage taskFileStorage = Mockito.mock(TaskFileStorage.class);
        WorkflowService workflowService = Mockito.mock(WorkflowService.class);

        jobSyncExecutor = new JobSyncExecutor(
            contextService, evaluator, jobService, -1, (type) -> memoryMessageBroker, List.of(), List.of(), List.of(),
            List.of(), taskExecutionService, new SyncTaskExecutor(), taskHandlerRegistry, taskFileStorage, 2L,
            workflowService);
    }

    @Test
    @SuppressWarnings("PMD.UnusedLocalVariable")
    void testNotifiesTaskExecutionCompleteListeners() throws Exception {
        long jobId = 101L;

        CountDownLatch countDownLatch = new CountDownLatch(1);
        AtomicBoolean called = new AtomicBoolean(false);

        try (AutoCloseable handle = jobSyncExecutor.addTaskExecutionCompleteListener(jobId, evt -> {
            called.set(true);
            countDownLatch.countDown();
        })) {
            TaskExecution taskExecution = new TaskExecution();

            taskExecution.setJobId(jobId);

            // Ensure retryDelay is parsable by Duration.parse used internally by getRetryDelayMillis
            taskExecution.setRetryDelay("1S");

            TaskExecutionCompleteEvent taskExecutionCompleteEvent = new TaskExecutionCompleteEvent(taskExecution);

            taskExecutionCompleteEvent.putMetadata(TenantConstants.CURRENT_TENANT_ID, TENANT);

            memoryMessageBroker.send(taskExecutionCompleteEvent.getRoute(), taskExecutionCompleteEvent);

            boolean ok = countDownLatch.await(1, TimeUnit.SECONDS);

            assertThat(ok).isTrue();
            assertThat(called.get()).isTrue();
        }
    }

    @Test
    @SuppressWarnings("PMD.UnusedLocalVariable")
    void testNotifiesErrorListeners() throws Exception {
        long jobId = 202L;

        CountDownLatch countDownLatch = new CountDownLatch(1);
        AtomicBoolean called = new AtomicBoolean(false);

        try (AutoCloseable handle = jobSyncExecutor.addErrorListener(jobId, evt -> {
            called.set(true);
            countDownLatch.countDown();
        })) {
            TaskExecution taskExecution = new TaskExecution();

            taskExecution.setJobId(jobId);
            taskExecution.setRetryDelay("1S");

            TaskExecutionErrorEvent taskExecutionErrorEvent = new TaskExecutionErrorEvent(taskExecution);

            taskExecutionErrorEvent.putMetadata(TenantConstants.CURRENT_TENANT_ID, TENANT);

            memoryMessageBroker.send(taskExecutionErrorEvent.getRoute(), taskExecutionErrorEvent);

            boolean ok = countDownLatch.await(1, TimeUnit.SECONDS);

            assertThat(ok).isTrue();
            assertThat(called.get()).isTrue();
        }
    }

    @Test
    @SuppressWarnings("PMD.UnusedLocalVariable")
    void testJobStatusListenerInvokedAndWaitUnblocksOnStopped() throws Exception {
        long jobId = 303L;

        // Return STARTED initially, so waitForJobCompletion does not short-circuit
        Job startedJob = new Job();

        startedJob.setId(jobId);
        startedJob.setStatus(Job.Status.STARTED);

        when(jobService.getJob(jobId)).thenReturn(startedJob);

        CountDownLatch statusLatch = new CountDownLatch(1);

        try (AutoCloseable handle = jobSyncExecutor.addJobStatusListener(jobId, evt -> {
            if (evt.getStatus() == Job.Status.STOPPED) {
                statusLatch.countDown();
            }
        })) {
            // Await in a separate thread
            CountDownLatch awaitReturnedLatch = new CountDownLatch(1);

            Thread thread = new Thread(() -> {
                try {
                    // Ensure TenantContext is set in the waiter thread so latch keys match
                    TenantContext.setCurrentTenantId(TENANT);
                    jobSyncExecutor.awaitJob(jobId, false);
                } finally {
                    awaitReturnedLatch.countDown();
                }
            });

            thread.start();

            // Wait until the await thread has registered the CountDownLatch so STOPPED can count it down
            waitForLatchRegistration(jobSyncExecutor, TENANT + "_" + jobId, Duration.ofMillis(250));

            JobStatusApplicationEvent jobStatusApplicationEvent = new JobStatusApplicationEvent(
                jobId, Job.Status.STOPPED);

            jobStatusApplicationEvent.putMetadata(TenantConstants.CURRENT_TENANT_ID, TENANT);

            memoryMessageBroker.send(TaskCoordinatorMessageRoute.APPLICATION_EVENTS, jobStatusApplicationEvent);

            // Listener invoked and waiter unblocked
            assertThat(statusLatch.await(1, TimeUnit.SECONDS)).isTrue();

            // Now when awaitJob fetches the job after latch, return STOPPED
            Job stoppedJob = new Job();

            stoppedJob.setId(jobId);
            stoppedJob.setStatus(Job.Status.STOPPED);

            when(jobService.getJob(jobId)).thenReturn(stoppedJob);

            assertThat(awaitReturnedLatch.await(1, TimeUnit.SECONDS)).isTrue();
        }
    }

    @Test
    @SuppressWarnings("PMD.UnusedLocalVariable")
    void testTaskStartedListenerInvokedAndJobIdDerivedFromTaskExecutionId() throws Exception {
        long jobId = 404L;
        long taskExecutionId = 9001L;

        TaskExecution taskExecution = new TaskExecution();

        taskExecution.setId(taskExecutionId);
        taskExecution.setStatus(TaskExecution.Status.CREATED);
        taskExecution.setStartDate(null);

        when(taskExecutionService.getTaskExecution(taskExecutionId)).thenReturn(taskExecution);

        Job job = new Job();

        job.setId(jobId);
        job.setStatus(Job.Status.STARTED);

        when(jobService.getTaskExecutionJob(taskExecutionId)).thenReturn(job);
        when(taskExecutionService.update(any(TaskExecution.class))).thenAnswer(inv -> inv.getArgument(0));

        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean called = new AtomicBoolean(false);

        try (AutoCloseable handle = jobSyncExecutor.addTaskStartedListener(jobId, evt -> {
            called.set(true);
            latch.countDown();
        })) {
            TaskStartedApplicationEvent taskStartedApplicationEvent = new TaskStartedApplicationEvent(taskExecutionId);

            taskStartedApplicationEvent.putMetadata(TenantConstants.CURRENT_TENANT_ID, TENANT);

            memoryMessageBroker.send(TaskCoordinatorMessageRoute.APPLICATION_EVENTS, taskStartedApplicationEvent);

            boolean ok = latch.await(1, TimeUnit.SECONDS);

            assertThat(ok).isTrue();
            assertThat(called.get()).isTrue();
        }
    }

    @Test
    void testStopJobEventForDeletedJobDoesNotThrowException() {
        long jobId = 505L;

        when(jobService.setStatusToStopped(jobId))
            .thenThrow(new java.util.NoSuchElementException("Unknown job " + jobId));

        StopJobEvent stopJobEvent = new StopJobEvent(jobId);

        stopJobEvent.putMetadata(TenantConstants.CURRENT_TENANT_ID, TENANT);

        // This should not throw an exception because it's caught in handleCoordinatorJobStopEvent
        memoryMessageBroker.send(TaskCoordinatorMessageRoute.JOB_STOP_EVENTS, stopJobEvent);
    }

    private static void waitForLatchRegistration(JobSyncExecutor jobSyncExecutor, String key, Duration timeout)
        throws Exception {
        long deadline = System.nanoTime() + timeout.toNanos();

        @SuppressWarnings("unchecked")
        Map<String, ?> latches = (Map<String, ?>) ReflectionTestUtils.getField(jobSyncExecutor, "jobCompletionLatches");

        while (System.nanoTime() < deadline) {
            if (Objects.requireNonNull(latches)
                .containsKey(key)) {

                return;
            }

            Thread.sleep(10);
        }
    }
}
