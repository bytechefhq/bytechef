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

import static com.bytechef.tenant.constant.TenantConstants.CURRENT_TENANT_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.InstanceOfAssertFactories.LIST;
import static org.awaitility.Awaitility.await;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.coordinator.event.ErrorEvent;
import com.bytechef.atlas.coordinator.event.TaskExecutionCompleteEvent;
import com.bytechef.atlas.coordinator.event.TaskExecutionErrorEvent;
import com.bytechef.atlas.coordinator.event.TaskStartedApplicationEvent;
import com.bytechef.atlas.coordinator.message.route.TaskCoordinatorMessageRoute;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.dto.JobParametersDTO;
import com.bytechef.atlas.execution.event.JobDeletedEvent;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.atlas.worker.event.TaskExecutionEvent;
import com.bytechef.atlas.worker.exception.TaskExecutionException;
import com.bytechef.atlas.worker.message.route.TaskWorkerMessageRoute;
import com.bytechef.atlas.worker.task.handler.TaskHandler;
import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.error.ExecutionError;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.exception.ExecutionException;
import com.bytechef.message.broker.memory.AsyncMessageBroker;
import com.bytechef.message.broker.memory.MemoryMessageBroker;
import com.bytechef.message.event.MessageEvent;
import com.bytechef.message.route.MessageRoute;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.component.definition.ActionContextAware;
import com.bytechef.platform.component.definition.SuspendAwareSseEmitterHandler;
import com.bytechef.platform.component.definition.ai.agent.ClusterElementCall;
import com.bytechef.platform.component.definition.ai.agent.ClusterElementCallRecorder;
import com.bytechef.platform.component.definition.ai.agent.ClusterElementCallRef;
import com.bytechef.platform.job.sync.SseStreamBridge;
import com.bytechef.platform.job.sync.config.JobSyncExecutorIntTestConfiguration;
import com.bytechef.platform.workflow.execution.constant.TaskExecutionMetadataConstants;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.util.TenantCacheKeyUtils;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.awaitility.core.ConditionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
@Import(PostgreSQLContainerConfiguration.class)
@SpringBootTest(
    classes = JobSyncExecutorIntTestConfiguration.class,
    properties = "bytechef.workflow.repository.classpath.enabled=true")
class JobSyncExecutorIntTest {

    private static final long CONFIGURED_TIMEOUT_SECONDS = 2;
    private static final String EXECUTOR_CLOSED_MESSAGE = "SimpleAsyncTaskExecutor is not active";
    private static final Duration EVENT_TIMEOUT = Duration.ofSeconds(10);
    private static final String GATE_THEN_FAIL_WORKFLOW_ID = EncodingUtils.base64EncodeToString("gate-then-fail");
    private static final String GATE_THEN_OUTPUT_WORKFLOW_ID = EncodingUtils.base64EncodeToString("gate-then-output");
    private static final String GATE_WORKFLOW_ID = EncodingUtils.base64EncodeToString("gate");
    private static final String OUTPUT_WORKFLOW_ID = EncodingUtils.base64EncodeToString("output");
    private static final String STREAM_SUSPEND_WORKFLOW_ID = EncodingUtils.base64EncodeToString("stream-suspend");
    private static final String STREAMED_TOOL_NAME = "tool_1";
    private static final String TASK_STARTED_FAILURE_MESSAGE = "fetchTaskExecutionJob failed";

    @Autowired
    private ContextService contextService;

    @Autowired
    private Environment environment;

    @Autowired
    private Evaluator evaluator;

    @Autowired
    private JobService jobService;

    @Autowired
    private TaskExecutionService taskExecutionService;

    @Autowired
    private TaskFileStorage taskFileStorage;

    @Autowired
    private WorkflowService workflowService;

    private final List<Long> executedJobIds = new CopyOnWriteArrayList<>();
    private JobSyncExecutor jobSyncExecutor;
    private ObservedMessageBroker observedMessageBroker;
    private final List<Object> publishedApplicationEvents = new CopyOnWriteArrayList<>();
    private JobService recordingJobService;
    private TaskExecutionService recordingTaskExecutionService;
    private final TaskGates taskGates = new TaskGates();
    private SimpleAsyncTaskExecutor workerTaskExecutor;

    @BeforeEach
    void beforeEach() {
        observedMessageBroker = new ObservedMessageBroker(new AsyncMessageBroker(environment));
        recordingJobService = mock(JobService.class, delegatesTo(jobService));
        recordingTaskExecutionService = mock(TaskExecutionService.class, delegatesTo(taskExecutionService));
        workerTaskExecutor = new SimpleAsyncTaskExecutor("job-sync-worker-");

        workerTaskExecutor.setVirtualThreads(true);

        Map<String, TaskHandler<?>> taskHandlers = Map.of(
            "test/v1/fail", this::handleFailTask,
            "test/v1/gate", this::handleGateTask,
            "test/v1/output", this::handleOutputTask,
            "test/v1/streamSuspend", this::handleStreamSuspendTask);

        jobSyncExecutor = new JobSyncExecutor(
            contextService, evaluator, publishedApplicationEvents::add, recordingJobService, -1, observedMessageBroker,
            List.of(), List.of(), List.of(), List.of(), List.of(), recordingTaskExecutionService, workerTaskExecutor,
            taskHandlers::get, taskFileStorage, CONFIGURED_TIMEOUT_SECONDS, workflowService);
    }

    @AfterEach
    void afterEach() {
        taskGates.releaseAll();

        observedMessageBroker.awaitQuiescence();

        observedMessageBroker.shutdown();

        workerTaskExecutor.close();
    }

    @Test
    void testDeleteJobPublishesJobDeletedEventToApplicationEventPublisher() {
        long jobId = runJobToCompletion(OUTPUT_WORKFLOW_ID);

        observedMessageBroker.awaitQuiescence();

        publishedApplicationEvents.clear();

        jobSyncExecutor.deleteJob(jobId);

        assertThat(publishedApplicationEvents).containsExactly(new JobDeletedEvent(jobId));
        assertThat(jobService.fetchJob(jobId)).isEmpty();
        assertThat(taskExecutionService.getJobTaskExecutions(jobId)).isEmpty();
    }

    @Test
    @SuppressWarnings("PMD.UnusedLocalVariable")
    void testNotifiesTaskExecutionCompleteListeners() throws Exception {
        long jobId = jobSyncExecutor.startJob(new JobParametersDTO(GATE_THEN_OUTPUT_WORKFLOW_ID, Map.of()));

        taskGates.awaitStarted(jobId);

        List<TaskExecutionCompleteEvent> taskExecutionCompleteEvents = new CopyOnWriteArrayList<>();

        try (AutoCloseable handle = jobSyncExecutor.addTaskExecutionCompleteListener(
            jobId, taskExecutionCompleteEvents::add)) {

            taskGates.release(jobId);

            Job job = jobSyncExecutor.awaitJob(jobId, true);

            assertThat(job.getStatus()).isEqualTo(Job.Status.COMPLETED);
        }

        assertThat(taskExecutionCompleteEvents)
            .extracting(TaskExecutionCompleteEvent::getTaskExecution)
            .allSatisfy(taskExecution -> {
                assertThat(taskExecution.getJobId()).isEqualTo(jobId);
                assertThat(taskExecution.getStatus()).isEqualTo(TaskExecution.Status.COMPLETED);
            })
            .extracting(TaskExecution::getName)
            .containsExactly("gate", "greeting");
    }

    @Test
    @SuppressWarnings("PMD.UnusedLocalVariable")
    void testNotifiesErrorListeners() throws Exception {
        long jobId = jobSyncExecutor.startJob(new JobParametersDTO(GATE_THEN_FAIL_WORKFLOW_ID, Map.of()));

        taskGates.awaitStarted(jobId);

        List<ErrorEvent> errorEvents = new CopyOnWriteArrayList<>();

        try (AutoCloseable handle = jobSyncExecutor.addErrorListener(jobId, errorEvents::add)) {
            taskGates.release(jobId);

            Job job = jobSyncExecutor.awaitJob(jobId, false);

            assertThat(job.getStatus()).isEqualTo(Job.Status.FAILED);
        }

        assertThat(errorEvents)
            .singleElement()
            .isInstanceOfSatisfying(TaskExecutionErrorEvent.class, taskExecutionErrorEvent -> {
                TaskExecution taskExecution = taskExecutionErrorEvent.getTaskExecution();

                assertThat(taskExecution.getJobId()).isEqualTo(jobId);
                assertThat(taskExecution.getName()).isEqualTo("failure");
                assertThat(taskExecution.getError())
                    .extracting(ExecutionError::getMessage)
                    .isEqualTo("Task failed on purpose");
            });
    }

    @Test
    @SuppressWarnings("PMD.UnusedLocalVariable")
    void testAwaitJobReturnsOnlyAfterJobStatusListenersHandledTheTerminalStatus() throws Exception {
        long jobId = jobSyncExecutor.startJob(new JobParametersDTO(GATE_WORKFLOW_ID, Map.of()));

        taskGates.awaitStarted(jobId);

        CountDownLatch listenerEnteredLatch = new CountDownLatch(1);
        CountDownLatch listenerReleaseLatch = new CountDownLatch(1);

        try (AutoCloseable handle = jobSyncExecutor.addJobStatusListener(jobId, jobStatusApplicationEvent -> {
            if (jobStatusApplicationEvent.getStatus() == Job.Status.COMPLETED) {
                listenerEnteredLatch.countDown();

                awaitLatch(listenerReleaseLatch);
            }
        })) {
            CompletableFuture<Job> awaitedJobFuture = CompletableFuture.supplyAsync(
                () -> jobSyncExecutor.awaitJob(jobId, false));

            awaitJobCompletionLatchRegistration(jobId);

            try {
                taskGates.release(jobId);

                assertThat(listenerEnteredLatch.await(EVENT_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)).isTrue();
                assertThatThrownBy(() -> awaitedJobFuture.get(300, TimeUnit.MILLISECONDS))
                    .isInstanceOf(TimeoutException.class);
            } finally {
                listenerReleaseLatch.countDown();
            }

            assertThat(awaitedJobFuture)
                .succeedsWithin(EVENT_TIMEOUT)
                .extracting(Job::getStatus)
                .isEqualTo(Job.Status.COMPLETED);
        }
    }

    @Test
    @SuppressWarnings("PMD.UnusedLocalVariable")
    void testJobStatusListenerInvokedAndWaitUnblocksOnStopped() throws Exception {
        long jobId = jobSyncExecutor.startJob(new JobParametersDTO(GATE_WORKFLOW_ID, Map.of()));

        taskGates.awaitStarted(jobId);

        CountDownLatch stoppedLatch = new CountDownLatch(1);

        try (AutoCloseable handle = jobSyncExecutor.addJobStatusListener(jobId, jobStatusApplicationEvent -> {
            if (jobStatusApplicationEvent.getStatus() == Job.Status.STOPPED) {
                stoppedLatch.countDown();
            }
        })) {
            CompletableFuture<Job> awaitedJobFuture = CompletableFuture.supplyAsync(
                () -> jobSyncExecutor.awaitJob(jobId, false));

            awaitJobCompletionLatchRegistration(jobId);

            jobSyncExecutor.stopJob(jobId);

            assertThat(stoppedLatch.await(EVENT_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)).isTrue();
            assertThat(awaitedJobFuture)
                .succeedsWithin(EVENT_TIMEOUT)
                .extracting(Job::getStatus)
                .isEqualTo(Job.Status.STOPPED);
        }

        assertThat(getJobStatus(jobId)).isEqualTo(Job.Status.STOPPED);
    }

    @Test
    @SuppressWarnings("PMD.UnusedLocalVariable")
    void testTaskStartedListenerInvokedAndJobIdDerivedFromTaskExecutionId() throws Exception {
        long jobId = jobSyncExecutor.startJob(new JobParametersDTO(GATE_WORKFLOW_ID, Map.of()));

        taskGates.awaitStarted(jobId);

        long taskExecutionId = getTaskExecutionId(jobId, "gate");

        TaskStartedApplicationEvent taskStartedApplicationEvent = new TaskStartedApplicationEvent(taskExecutionId);

        List<TaskStartedApplicationEvent> taskStartedApplicationEvents = new CopyOnWriteArrayList<>();

        try (AutoCloseable handle = jobSyncExecutor.addTaskStartedListener(
            jobId, taskStartedApplicationEvents::add)) {

            send(TaskCoordinatorMessageRoute.APPLICATION_EVENTS, taskStartedApplicationEvent);

            awaitCondition()
                .until(() -> taskStartedApplicationEvents.contains(taskStartedApplicationEvent));
        }

        assertThat(taskStartedApplicationEvent.getJobId()).isNull();
        assertThat(getTaskExecutionStatus(taskExecutionId)).isEqualTo(TaskExecution.Status.STARTED);
    }

    @Test
    @SuppressWarnings("PMD.UnusedLocalVariable")
    void testTaskStartedEventForDeletedTaskExecutionIsSkipped() throws Exception {
        long jobId = runJobToCompletion(OUTPUT_WORKFLOW_ID);
        long taskExecutionId = getTaskExecutionId(jobId, "greeting");

        jobSyncExecutor.deleteJob(jobId);

        observedMessageBroker.awaitQuiescence();

        clearInvocations(recordingTaskExecutionService);

        List<TaskStartedApplicationEvent> taskStartedApplicationEvents = new CopyOnWriteArrayList<>();

        try (AutoCloseable handle = jobSyncExecutor.addTaskStartedListener(
            jobId, taskStartedApplicationEvents::add)) {

            send(TaskCoordinatorMessageRoute.APPLICATION_EVENTS, new TaskStartedApplicationEvent(taskExecutionId));

            observedMessageBroker.awaitQuiescence();
        }

        assertThat(observedMessageBroker.getReceiverFailures()).isEmpty();
        assertThat(taskStartedApplicationEvents).isEmpty();

        verify(recordingTaskExecutionService, never()).getTaskExecution(anyLong());
    }

    @Test
    void testWorkerTaskExecutionEventForStartedJobIsExecuted() {
        long jobId = jobSyncExecutor.startJob(new JobParametersDTO(OUTPUT_WORKFLOW_ID, Map.of()));

        Job job = jobSyncExecutor.awaitJob(jobId, true);

        assertThat(job.getStatus()).isEqualTo(Job.Status.COMPLETED);
        assertThat(executedJobIds).containsExactly(jobId);

        Map<String, ?> outputs = taskFileStorage.readJobOutputs(Objects.requireNonNull(job.getOutputs()));

        assertThat(outputs.get("greeting")).isEqualTo("hello");
    }

    @Test
    void testWorkerTaskExecutionEventForDeletedJobIsSkipped() {
        long jobId = runJobToCompletion(OUTPUT_WORKFLOW_ID);
        long taskExecutionId = getTaskExecutionId(jobId, "greeting");

        jobSyncExecutor.deleteJob(jobId);

        observedMessageBroker.awaitQuiescence();

        executedJobIds.clear();

        TaskExecution taskExecution = TaskExecution.builder()
            .id(taskExecutionId)
            .jobId(jobId)
            .workflowTask(
                new WorkflowTask(
                    Map.of("name", "greeting", "type", "test/v1/output", "parameters", Map.of("value", "hello"))))
            .build();

        send(TaskWorkerMessageRoute.TASK_EXECUTION_EVENTS, new TaskExecutionEvent(taskExecution));

        observedMessageBroker.awaitQuiescence();

        assertThat(executedJobIds).isEmpty();
        assertThat(observedMessageBroker.getReceiverFailures()).isEmpty();
    }

    @Test
    void testTaskExecutionCompleteEventForDeletedJobIsDropped() {
        long jobId = runJobToCompletion(OUTPUT_WORKFLOW_ID);

        TaskExecution taskExecution = taskExecutionService.getTaskExecution(getTaskExecutionId(jobId, "greeting"));

        jobSyncExecutor.deleteJob(jobId);

        observedMessageBroker.awaitQuiescence();

        clearInvocations(recordingTaskExecutionService);

        send(TaskCoordinatorMessageRoute.TASK_EXECUTION_COMPLETE_EVENTS, new TaskExecutionCompleteEvent(taskExecution));

        observedMessageBroker.awaitQuiescence();

        verify(recordingTaskExecutionService, never()).completeIfNotCancelled(anyLong());
    }

    @Test
    void testTaskExecutionCompleteEventForStartedJobIsHandled() {
        long jobId = runJobToCompletion(OUTPUT_WORKFLOW_ID);
        long taskExecutionId = getTaskExecutionId(jobId, "greeting");

        verify(recordingTaskExecutionService).completeIfNotCancelled(taskExecutionId);

        assertThat(getTaskExecutionStatus(taskExecutionId)).isEqualTo(TaskExecution.Status.COMPLETED);
    }

    @Test
    void testTaskExecutionCompleteEventForStoppedJobIsHandled() {
        long jobId = jobSyncExecutor.startJob(new JobParametersDTO(GATE_WORKFLOW_ID, Map.of()));

        taskGates.awaitStarted(jobId);

        jobSyncExecutor.stopJob(jobId);

        awaitCondition()
            .until(() -> getJobStatus(jobId) == Job.Status.STOPPED);

        observedMessageBroker.awaitQuiescence();

        long taskExecutionId = getTaskExecutionId(jobId, "gate");

        TaskExecution taskExecution = taskExecutionService.getTaskExecution(taskExecutionId);

        clearInvocations(recordingTaskExecutionService);

        send(TaskCoordinatorMessageRoute.TASK_EXECUTION_COMPLETE_EVENTS, new TaskExecutionCompleteEvent(taskExecution));

        observedMessageBroker.awaitQuiescence();

        verify(recordingTaskExecutionService).completeIfNotCancelled(taskExecutionId);

        assertThat(getTaskExecutionStatus(taskExecutionId)).isEqualTo(TaskExecution.Status.CANCELLED);
        assertThat(getJobStatus(jobId)).isEqualTo(Job.Status.STOPPED);
    }

    @Test
    void testStopJobEventForDeletedJobDoesNotThrowException() {
        long jobId = runJobToCompletion(OUTPUT_WORKFLOW_ID);

        jobSyncExecutor.deleteJob(jobId);

        observedMessageBroker.awaitQuiescence();

        jobSyncExecutor.stopJob(jobId);

        observedMessageBroker.awaitQuiescence();

        assertThat(observedMessageBroker.getReceiverFailures()).isEmpty();
        assertThat(jobService.fetchJob(jobId)).isEmpty();
    }

    @Test
    void testCheckForErrorThrowsWhenJobFailedWithNoTaskExecutions() {
        long jobId = createFailedJob(new ExecutionError("Invalid expression: function perform(input) {}", List.of()));

        assertThatThrownBy(() -> jobSyncExecutor.awaitJob(jobId, true))
            .isInstanceOf(ExecutionException.class)
            .hasMessageContaining("Invalid expression");
    }

    @Test
    void testCheckForErrorReportsTheFailedTaskWhenALaterTaskExecutionDidNotFail() {
        long jobId = createFailedJob(new ExecutionError("Job failed", List.of()));

        TaskExecution failedTaskExecution = TaskExecution.builder()
            .jobId(jobId)
            .status(TaskExecution.Status.FAILED)
            .workflowTask(new WorkflowTask(Map.of("name", "branch", "type", "test/v1/fail")))
            .build();

        failedTaskExecution.setError(new ExecutionError("Branch task failed", List.of()));

        taskExecutionService.create(failedTaskExecution);

        TaskExecution completedTaskExecution = TaskExecution.builder()
            .jobId(jobId)
            .status(TaskExecution.Status.COMPLETED)
            .workflowTask(new WorkflowTask(Map.of("name", "greeting", "type", "test/v1/output")))
            .build();

        taskExecutionService.create(completedTaskExecution);

        assertThat(taskExecutionService.fetchLastJobTaskExecution(jobId))
            .hasValueSatisfying(
                taskExecution -> assertThat(taskExecution.getStatus()).isEqualTo(TaskExecution.Status.COMPLETED));
        assertThatThrownBy(() -> jobSyncExecutor.awaitJob(jobId, true))
            .isInstanceOf(ExecutionException.class)
            .hasMessage("Branch task failed");
    }

    @Test
    void testCheckForErrorThrowsWhenJobFailedWithNoErrorDetails() {
        long jobId = createFailedJob(null);

        assertThatThrownBy(() -> jobSyncExecutor.awaitJob(jobId, true))
            .isInstanceOf(ExecutionException.class)
            .hasMessageContaining("Job " + jobId + " failed");
    }

    @Test
    void testAwaitJobStopsWaitingAfterPerCallTimeout() {
        long jobId = jobSyncExecutor.startJob(new JobParametersDTO(GATE_WORKFLOW_ID, Map.of()));

        taskGates.awaitStarted(jobId);

        long startNanos = System.nanoTime();

        Job job = jobSyncExecutor.awaitJob(jobId, false, taskExecutionCompleteEvent -> {}, Duration.ofMillis(100));

        assertThat(Duration.ofNanos(System.nanoTime() - startNanos)).isLessThan(Duration.ofMillis(1500));
        assertThat(job.getStatus()).isEqualTo(Job.Status.FAILED);
        assertThat(getJobStatus(jobId)).isEqualTo(Job.Status.FAILED);
    }

    @Test
    void testAwaitJobCapsPerCallTimeoutAtConfiguredTimeout() {
        long jobId = jobSyncExecutor.startJob(new JobParametersDTO(GATE_WORKFLOW_ID, Map.of()));

        taskGates.awaitStarted(jobId);

        long startNanos = System.nanoTime();

        jobSyncExecutor.awaitJob(jobId, false, taskExecutionCompleteEvent -> {}, Duration.ofSeconds(30));

        assertThat(Duration.ofNanos(System.nanoTime() - startNanos))
            .isGreaterThanOrEqualTo(Duration.ofMillis(1900))
            .isLessThan(Duration.ofSeconds(10));
    }

    @Test
    void testStartJobMarksTheJobAsASyncExecution() {
        long jobId = jobSyncExecutor.startJob(
            new JobParametersDTO(OUTPUT_WORKFLOW_ID, Map.of(), Map.of("existing", "value")));

        jobSyncExecutor.awaitJob(jobId, true);

        Job job = jobService.getJob(jobId);

        Map<String, Object> metadata = new HashMap<>(job.getMetadata());

        assertThat(metadata)
            .containsEntry(MetadataConstants.SYNC_EXECUTION, true)
            .containsEntry("existing", "value");
    }

    @Test
    void testStartJobWithAJobFactoryMarksTheJobAsASyncExecution() {
        AtomicReference<JobParametersDTO> jobParametersDTOReference = new AtomicReference<>();

        assertThatThrownBy(
            () -> jobSyncExecutor.startJob(
                new JobParametersDTO(OUTPUT_WORKFLOW_ID, Map.of(), Map.of("existing", "value")),
                jobParametersDTO -> {
                    jobParametersDTOReference.set(jobParametersDTO);

                    throw new IllegalStateException("job created");
                }))
                    .hasMessage("job created");

        JobParametersDTO jobParametersDTO = jobParametersDTOReference.get();

        Map<String, Object> metadata = new HashMap<>(jobParametersDTO.getMetadata());

        assertThat(metadata)
            .containsEntry(MetadataConstants.SYNC_EXECUTION, true)
            .containsEntry("existing", "value");
    }

    @Test
    void testExecuteWithAJobFactoryMarksTheJobAsASyncExecution() {
        AtomicReference<JobParametersDTO> jobParametersDTOReference = new AtomicReference<>();

        assertThatThrownBy(
            () -> jobSyncExecutor.execute(
                new JobParametersDTO(OUTPUT_WORKFLOW_ID, Map.of()),
                jobParametersDTO -> {
                    jobParametersDTOReference.set(jobParametersDTO);

                    throw new IllegalStateException("job created");
                },
                false, taskExecutionCompleteEvent -> {}))
                    .hasMessage("job created");

        JobParametersDTO jobParametersDTO = jobParametersDTOReference.get();

        Map<String, Object> metadata = new HashMap<>(jobParametersDTO.getMetadata());

        assertThat(metadata).containsEntry(MetadataConstants.SYNC_EXECUTION, true);
    }

    @Test
    void testTimedOutJobIsFailedEvenWhenTheFirstUpdateLosesToAConcurrentUpdate() {
        long jobId = jobSyncExecutor.startJob(new JobParametersDTO(GATE_WORKFLOW_ID, Map.of()));

        taskGates.awaitStarted(jobId);

        doThrow(new OptimisticLockingFailureException("concurrent update"))
            .doAnswer(invocation -> jobService.update(invocation.getArgument(0)))
            .when(recordingJobService)
            .update(any(Job.class));

        assertThatThrownBy(
            () -> jobSyncExecutor.awaitJob(jobId, true, taskExecutionCompleteEvent -> {}, Duration.ofMillis(100)))
                .isInstanceOf(ExecutionException.class)
                .hasMessage("Job " + jobId + " did not finish within 100 milliseconds");

        assertThat(getJobStatus(jobId)).isEqualTo(Job.Status.FAILED);
    }

    @Test
    void testJobThatFinishesAsTheWaitTimesOutIsNotFailed() {
        long jobId = jobSyncExecutor.startJob(new JobParametersDTO(GATE_WORKFLOW_ID, Map.of()));

        taskGates.awaitStarted(jobId);

        Job startedJob = jobService.getJob(jobId);
        Job completedJob = jobService.getJob(jobId);

        completedJob.setStatus(Job.Status.COMPLETED);

        doReturn(startedJob, completedJob).when(recordingJobService)
            .getJob(jobId);

        clearInvocations(recordingJobService);

        Job job = jobSyncExecutor.awaitJob(jobId, false, taskExecutionCompleteEvent -> {}, Duration.ofMillis(100));

        doAnswer(invocation -> jobService.getJob(jobId)).when(recordingJobService)
            .getJob(jobId);

        assertThat(job.getStatus()).isEqualTo(Job.Status.COMPLETED);

        verify(recordingJobService, never()).update(any(Job.class));
    }

    @Test
    void testInterruptedWaitKeepsTheInterruptFlag() throws Exception {
        long jobId = jobSyncExecutor.startJob(new JobParametersDTO(GATE_WORKFLOW_ID, Map.of()));

        taskGates.awaitStarted(jobId);

        AtomicBoolean interrupted = new AtomicBoolean();

        Thread waitingThread = Thread.ofVirtual()
            .start(() -> {
                jobSyncExecutor.awaitJob(jobId, false, taskExecutionCompleteEvent -> {}, Duration.ofSeconds(30));

                interrupted.set(Thread.currentThread()
                    .isInterrupted());
            });

        awaitJobCompletionLatchRegistration(jobId);

        awaitCondition()
            .until(() -> isParkedOnALatch(waitingThread));

        waitingThread.interrupt();

        assertThat(waitingThread.join(EVENT_TIMEOUT)).isTrue();
        assertThat(interrupted).isTrue();
    }

    @Test
    void testSseStreamRunsBeforeSuspend() throws Exception {
        RecordingSseStreamBridge recordingSseStreamBridge = new RecordingSseStreamBridge();

        long jobId = runStreamSuspendJob(recordingSseStreamBridge);

        assertThat(recordingSseStreamBridge.payloads).containsExactly(Map.of("text", "streamed"));
        assertThat(recordingSseStreamBridge.suspended).isTrue();
        assertThat(recordingSseStreamBridge.completed).isFalse();
        assertThat(getTaskExecutionMetadata(jobId, "stream"))
            .hasEntrySatisfying(
                MetadataConstants.JOB_RESUME_ID, jobResumeId -> assertThat(jobResumeId).asString()
                    .isNotBlank());
    }

    @Test
    void testClusterElementCallsAreStoredAfterStreamingAndBeforeSuspend() throws Exception {
        long jobId = runStreamSuspendJob(new RecordingSseStreamBridge());

        Map<String, ?> metadata = getTaskExecutionMetadata(jobId, "stream");

        assertThat(metadata).containsKey(MetadataConstants.JOB_RESUME_ID);
        assertThat(metadata.get(TaskExecutionMetadataConstants.CLUSTER_ELEMENT_CALLS))
            .asInstanceOf(LIST)
            .singleElement()
            .satisfies(call -> {
                @SuppressWarnings("unchecked")
                ClusterElementCall clusterElementCall = ClusterElementCall.fromMap((Map<String, ?>) call);

                assertThat(clusterElementCall.workflowNodeName()).isEqualTo(STREAMED_TOOL_NAME);
                assertThat(clusterElementCall.status()).isEqualTo(ClusterElementCall.Status.COMPLETED);
            });
    }

    @Test
    void testTimedOutJobFailsWithAnErrorThatNamesTheJob() {
        long jobId = jobSyncExecutor.startJob(new JobParametersDTO(GATE_WORKFLOW_ID, Map.of()));

        taskGates.awaitStarted(jobId);

        assertThatThrownBy(() -> jobSyncExecutor.awaitJob(jobId, true))
            .isInstanceOf(ExecutionException.class)
            .hasMessage("Job " + jobId + " did not finish within 2000 milliseconds");
    }

    @Test
    void testEventHandlerFailureFailsTheWaitingJobWithoutWaitingForTheTimeout() {
        long jobId = jobSyncExecutor.startJob(new JobParametersDTO(GATE_THEN_OUTPUT_WORKFLOW_ID, Map.of()));

        taskGates.awaitStarted(jobId);

        CompletableFuture<Job> awaitedJobFuture = CompletableFuture.supplyAsync(
            () -> jobSyncExecutor.awaitJob(jobId, true));

        awaitJobCompletionLatchRegistration(jobId);

        workerTaskExecutor.close();

        taskGates.release(jobId);

        assertThat(awaitedJobFuture)
            .failsWithin(Duration.ofSeconds(1))
            .withThrowableThat()
            .havingCause()
            .isInstanceOf(ExecutionException.class)
            .withMessageContaining(EXECUTOR_CLOSED_MESSAGE);
        assertThat(observedMessageBroker.awaitReceiverFailure(TaskWorkerMessageRoute.TASK_EXECUTION_EVENTS, jobId))
            .hasRootCauseInstanceOf(TaskRejectedException.class);
    }

    @Test
    void testAwaitJobWaitsWhileTheJobIsStartedWithATerminatedLastTaskExecution() throws Exception {
        long jobId = jobSyncExecutor.startJob(new JobParametersDTO(GATE_THEN_OUTPUT_WORKFLOW_ID, Map.of()));

        taskGates.awaitStarted(jobId);

        TaskExecution completedTaskExecution = new TaskExecution();

        completedTaskExecution.setStatus(TaskExecution.Status.COMPLETED);

        doReturn(Optional.of(completedTaskExecution))
            .when(recordingTaskExecutionService)
            .fetchLastJobTaskExecution(jobId);

        CompletableFuture<Job> awaitedJobFuture = CompletableFuture.supplyAsync(
            () -> jobSyncExecutor.awaitJob(jobId, true));

        assertThatThrownBy(() -> awaitedJobFuture.get(500, TimeUnit.MILLISECONDS))
            .isInstanceOf(TimeoutException.class);

        taskGates.release(jobId);

        assertThat(awaitedJobFuture)
            .succeedsWithin(EVENT_TIMEOUT)
            .extracting(Job::getStatus)
            .isEqualTo(Job.Status.COMPLETED);
    }

    @Test
    void testEventHandlerFailureBeforeTheCallerWaitsFailsTheJobAtOnce() {
        workerTaskExecutor.close();

        long jobId = jobSyncExecutor.startJob(new JobParametersDTO(OUTPUT_WORKFLOW_ID, Map.of()));

        assertThat(observedMessageBroker.awaitReceiverFailure(TaskWorkerMessageRoute.TASK_EXECUTION_EVENTS, jobId))
            .hasRootCauseInstanceOf(TaskRejectedException.class);

        observedMessageBroker.awaitQuiescence();

        long startNanos = System.nanoTime();

        assertThatThrownBy(() -> jobSyncExecutor.awaitJob(jobId, true))
            .isInstanceOf(ExecutionException.class)
            .hasMessageContaining(EXECUTOR_CLOSED_MESSAGE);
        assertThat(Duration.ofNanos(System.nanoTime() - startNanos)).isLessThan(Duration.ofMillis(1000));
        assertThat(executedJobIds).doesNotContain(jobId);
    }

    @Test
    void testTaskStartedHandlerFailureFailsTheJobAtOnce() {
        doThrow(new IllegalStateException(TASK_STARTED_FAILURE_MESSAGE))
            .when(recordingJobService)
            .fetchTaskExecutionJob(anyLong());

        long jobId = jobSyncExecutor.startJob(new JobParametersDTO(OUTPUT_WORKFLOW_ID, Map.of()));

        assertThat(observedMessageBroker.awaitReceiverFailure(TaskCoordinatorMessageRoute.APPLICATION_EVENTS, jobId))
            .hasRootCauseMessage(TASK_STARTED_FAILURE_MESSAGE);

        observedMessageBroker.awaitQuiescence();

        assertThatThrownBy(
            () -> jobSyncExecutor.awaitJob(jobId, true, taskExecutionCompleteEvent -> {}, Duration.ofSeconds(2)))
                .isInstanceOf(ExecutionException.class)
                .hasMessageContaining(TASK_STARTED_FAILURE_MESSAGE);
    }

    @Test
    void testEventHandlerFailureFailsAnAlreadyStoppedJob() {
        long jobId = jobSyncExecutor.startJob(new JobParametersDTO(GATE_THEN_OUTPUT_WORKFLOW_ID, Map.of()));

        taskGates.awaitStarted(jobId);

        workerTaskExecutor.close();

        taskGates.release(jobId);

        assertThat(observedMessageBroker.awaitReceiverFailure(TaskWorkerMessageRoute.TASK_EXECUTION_EVENTS, jobId))
            .hasRootCauseInstanceOf(TaskRejectedException.class);

        jobSyncExecutor.stopJob(jobId);

        awaitCondition()
            .until(() -> getJobStatus(jobId) == Job.Status.STOPPED);

        observedMessageBroker.awaitQuiescence();

        assertThatThrownBy(() -> jobSyncExecutor.awaitJob(jobId, true))
            .isInstanceOf(ExecutionException.class)
            .hasMessageContaining(EXECUTOR_CLOSED_MESSAGE);
        assertThat(getJobStatus(jobId)).isEqualTo(Job.Status.FAILED);
    }

    @Test
    void testEventHandlerFailureFailsTheCurrentJobInsteadOfTheOneLoadedBeforeWaiting() {
        long jobId = jobSyncExecutor.startJob(new JobParametersDTO(GATE_THEN_OUTPUT_WORKFLOW_ID, Map.of()));

        taskGates.awaitStarted(jobId);

        CompletableFuture<Job> awaitedJobFuture = CompletableFuture.supplyAsync(
            () -> jobSyncExecutor.awaitJob(jobId, true));

        awaitJobCompletionLatchRegistration(jobId);

        Job loadedJob = jobService.getJob(jobId);

        workerTaskExecutor.close();

        taskGates.release(jobId);

        assertThat(awaitedJobFuture)
            .failsWithin(EVENT_TIMEOUT)
            .withThrowableThat()
            .havingCause()
            .isInstanceOf(ExecutionException.class)
            .withMessageContaining(EXECUTOR_CLOSED_MESSAGE);

        Job currentJob = jobService.getJob(jobId);

        assertThat(currentJob.getStatus()).isEqualTo(Job.Status.FAILED);
        assertThat(currentJob.getCurrentTask()).isGreaterThan(loadedJob.getCurrentTask());
        assertThat(currentJob.getError())
            .extracting(ExecutionError::getMessage)
            .isEqualTo("Job " + jobId + " failed: " + EXECUTOR_CLOSED_MESSAGE);
    }

    private static ConditionFactory awaitCondition() {
        return await()
            .dontCatchUncaughtExceptions()
            .atMost(EVENT_TIMEOUT);
    }

    private void awaitJobCompletionLatchRegistration(long jobId) {
        @SuppressWarnings("unchecked")
        Map<String, ?> jobCompletionLatches = (Map<String, ?>) Objects.requireNonNull(
            ReflectionTestUtils.getField(jobSyncExecutor, "jobCompletionLatches"));

        String key = TenantCacheKeyUtils.getKey(jobId);

        awaitCondition()
            .until(() -> jobCompletionLatches.containsKey(key));
    }

    private static boolean isParkedOnALatch(Thread thread) {
        return Arrays.stream(thread.getStackTrace())
            .anyMatch(stackTraceElement -> Objects.equals(
                stackTraceElement.getClassName(), CountDownLatch.class.getName()) &&
                Objects.equals(stackTraceElement.getMethodName(), "await"));
    }

    private static void awaitLatch(CountDownLatch countDownLatch) {
        try {
            if (!countDownLatch.await(EVENT_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)) {
                throw new IllegalStateException("The latch was not released in time");
            }
        } catch (InterruptedException interruptedException) {
            Thread currentThread = Thread.currentThread();

            currentThread.interrupt();
        }
    }

    private long createFailedJob(ExecutionError executionError) {
        Workflow workflow = workflowService.getWorkflow(OUTPUT_WORKFLOW_ID);

        Job job = jobService.create(new JobParametersDTO(OUTPUT_WORKFLOW_ID, Map.of()), workflow);

        job.setError(executionError);
        job.setStatus(Job.Status.FAILED);

        job = jobService.update(job);

        return Objects.requireNonNull(job.getId());
    }

    private Job.Status getJobStatus(long jobId) {
        Job job = jobService.getJob(jobId);

        return job.getStatus();
    }

    private TaskExecution.Status getTaskExecutionStatus(long taskExecutionId) {
        TaskExecution taskExecution = taskExecutionService.getTaskExecution(taskExecutionId);

        return taskExecution.getStatus();
    }

    private Map<String, ?> getTaskExecutionMetadata(long jobId, String taskName) {
        TaskExecution taskExecution = taskExecutionService.getTaskExecution(getTaskExecutionId(jobId, taskName));

        return taskExecution.getMetadata();
    }

    private long getTaskExecutionId(long jobId, String taskName) {
        return taskExecutionService.getJobTaskExecutions(jobId)
            .stream()
            .filter(taskExecution -> Objects.equals(taskExecution.getName(), taskName))
            .map(TaskExecution::getId)
            .filter(Objects::nonNull)
            .findFirst()
            .orElseThrow();
    }

    private Object handleFailTask(TaskExecution taskExecution) {
        throw new IllegalStateException(MapUtils.getString(taskExecution.getParameters(), "message"));
    }

    private Object handleGateTask(TaskExecution taskExecution) throws TaskExecutionException {
        taskGates.pass(Objects.requireNonNull(taskExecution.getJobId()));

        return null;
    }

    private Object handleOutputTask(TaskExecution taskExecution) {
        executedJobIds.add(Objects.requireNonNull(taskExecution.getJobId()));

        return MapUtils.getString(taskExecution.getParameters(), "value");
    }

    private Object handleStreamSuspendTask(TaskExecution taskExecution) throws TaskExecutionException {
        long jobId = Objects.requireNonNull(taskExecution.getJobId());
        long taskExecutionId = Objects.requireNonNull(taskExecution.getId());

        taskGates.pass(jobId);

        ActionContextAware actionContext = mock(ActionContextAware.class);

        Instant now = Instant.now();

        when(actionContext.getSuspend()).thenReturn(
            new ActionContext.Suspend(Map.of(), now.plus(1, ChronoUnit.HOURS)));

        ClusterElementCallRecorder clusterElementCallRecorder = ClusterElementCallRecorder.getInstance();

        return new SuspendAwareSseEmitterHandler(
            sseEmitter -> {
                ClusterElementCallRef clusterElementCallRef = clusterElementCallRecorder.callStarted(
                    jobId, taskExecutionId, STREAMED_TOOL_NAME, "component", "tool", null, "input");

                sseEmitter.send(Map.of("text", "streamed"));

                clusterElementCallRecorder.callCompleted(jobId, taskExecutionId, clusterElementCallRef, "output");

                sseEmitter.complete();
            },
            actionContext);
    }

    private long runJobToCompletion(String workflowId) {
        long jobId = jobSyncExecutor.startJob(new JobParametersDTO(workflowId, Map.of()));

        Job job = jobSyncExecutor.awaitJob(jobId, true);

        assertThat(job.getStatus()).isEqualTo(Job.Status.COMPLETED);

        observedMessageBroker.awaitQuiescence();

        return jobId;
    }

    @SuppressWarnings("PMD.UnusedLocalVariable")
    private long runStreamSuspendJob(SseStreamBridge sseStreamBridge) throws Exception {
        long jobId = jobSyncExecutor.startJob(new JobParametersDTO(STREAM_SUSPEND_WORKFLOW_ID, Map.of()));

        taskGates.awaitStarted(jobId);

        try (AutoCloseable handle = jobSyncExecutor.addSseStreamBridge(jobId, sseStreamBridge)) {
            taskGates.release(jobId);

            Job job = jobSyncExecutor.awaitJob(jobId, true);

            assertThat(job.getStatus()).isEqualTo(Job.Status.COMPLETED);
        }

        return jobId;
    }

    private void send(MessageRoute messageRoute, MessageEvent<?> messageEvent) {
        messageEvent.putMetadata(CURRENT_TENANT_ID, TenantContext.getCurrentTenantId());

        observedMessageBroker.send(messageRoute, messageEvent);
    }

    private static final class ObservedMessageBroker implements MemoryMessageBroker {

        private final AsyncMessageBroker asyncMessageBroker;
        private final AtomicInteger pendingDeliveryCount = new AtomicInteger();
        private final Map<MessageRoute, Integer> receiverCounts = new ConcurrentHashMap<>();
        private final List<ReceiverFailure> receiverFailures = new CopyOnWriteArrayList<>();

        private ObservedMessageBroker(AsyncMessageBroker asyncMessageBroker) {
            this.asyncMessageBroker = asyncMessageBroker;
        }

        @Override
        public void receive(MessageRoute messageRoute, Receiver receiver) {
            receiverCounts.merge(messageRoute, 1, Integer::sum);

            asyncMessageBroker.receive(messageRoute, message -> {
                try {
                    receiver.receive(message);
                } catch (RuntimeException runtimeException) {
                    receiverFailures.add(new ReceiverFailure(messageRoute, message, runtimeException));

                    throw runtimeException;
                } finally {
                    pendingDeliveryCount.decrementAndGet();
                }
            });
        }

        @Override
        public void send(MessageRoute messageRoute, Object message) {
            int receiverCount = receiverCounts.getOrDefault(messageRoute, 0);

            pendingDeliveryCount.addAndGet(receiverCount);

            try {
                asyncMessageBroker.send(messageRoute, message);
            } catch (RuntimeException runtimeException) {
                pendingDeliveryCount.addAndGet(-receiverCount);

                throw runtimeException;
            }
        }

        private void awaitQuiescence() {
            awaitCondition()
                .until(() -> pendingDeliveryCount.get() == 0);
        }

        private RuntimeException awaitReceiverFailure(MessageRoute messageRoute, long jobId) {
            AtomicReference<RuntimeException> exceptionReference = new AtomicReference<>();

            awaitCondition()
                .until(() -> {
                    for (ReceiverFailure receiverFailure : receiverFailures) {
                        if (receiverFailure.messageRoute() == messageRoute &&
                            Objects.equals(getJobId(receiverFailure.message()), jobId)) {

                            exceptionReference.set(receiverFailure.exception());

                            return true;
                        }
                    }

                    return false;
                });

            return exceptionReference.get();
        }

        private static Long getJobId(TaskExecution taskExecution) {
            return taskExecution.getJobId();
        }

        private List<ReceiverFailure> getReceiverFailures() {
            return List.copyOf(receiverFailures);
        }

        private void shutdown() {
            asyncMessageBroker.shutdown();
        }

        private static Long getJobId(Object message) {
            return switch (message) {
                case TaskExecutionEvent taskExecutionEvent -> getJobId(taskExecutionEvent.getTaskExecution());
                case TaskStartedApplicationEvent taskStartedApplicationEvent -> taskStartedApplicationEvent.getJobId();
                default -> null;
            };
        }
    }

    private record ReceiverFailure(MessageRoute messageRoute, Object message, RuntimeException exception) {
    }

    private static final class RecordingSseStreamBridge implements SseStreamBridge {

        private volatile boolean completed;
        private final List<Object> payloads = new CopyOnWriteArrayList<>();
        private volatile boolean suspended;

        @Override
        public void onEvent(Object payload) {
            payloads.add(payload);
        }

        @Override
        public void onComplete() {
            completed = true;
        }

        @Override
        public void onSuspend() {
            suspended = true;
        }
    }

    private static final class TaskGates {

        private final Map<Long, CountDownLatch> releasedLatches = new ConcurrentHashMap<>();
        private final Map<Long, CountDownLatch> startedLatches = new ConcurrentHashMap<>();

        private void awaitStarted(long jobId) {
            CountDownLatch startedLatch = getLatch(startedLatches, jobId);

            awaitCondition()
                .until(() -> startedLatch.getCount() == 0);
        }

        private void pass(long jobId) throws TaskExecutionException {
            CountDownLatch startedLatch = getLatch(startedLatches, jobId);

            startedLatch.countDown();

            try {
                CountDownLatch releasedLatch = getLatch(releasedLatches, jobId);

                releasedLatch.await();
            } catch (InterruptedException interruptedException) {
                Thread currentThread = Thread.currentThread();

                currentThread.interrupt();

                throw new TaskExecutionException("Interrupted while waiting at the gate", interruptedException);
            }
        }

        private void release(long jobId) {
            CountDownLatch releasedLatch = getLatch(releasedLatches, jobId);

            releasedLatch.countDown();
        }

        private void releaseAll() {
            for (CountDownLatch releasedLatch : releasedLatches.values()) {
                releasedLatch.countDown();
            }
        }

        private static CountDownLatch getLatch(Map<Long, CountDownLatch> latches, long jobId) {
            return latches.computeIfAbsent(jobId, key -> new CountDownLatch(1));
        }
    }
}
