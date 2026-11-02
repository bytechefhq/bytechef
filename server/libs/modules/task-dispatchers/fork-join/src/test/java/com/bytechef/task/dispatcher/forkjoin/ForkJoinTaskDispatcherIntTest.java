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

package com.bytechef.task.dispatcher.forkjoin;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.coordinator.TaskCoordinator;
import com.bytechef.atlas.coordinator.event.ApplicationEvent;
import com.bytechef.atlas.coordinator.event.ErrorEvent;
import com.bytechef.atlas.coordinator.event.ResumeJobEvent;
import com.bytechef.atlas.coordinator.event.StartJobEvent;
import com.bytechef.atlas.coordinator.event.TaskExecutionCompleteEvent;
import com.bytechef.atlas.coordinator.event.listener.TaskExecutionErrorEventListener;
import com.bytechef.atlas.coordinator.event.listener.TaskStartedApplicationEventListener;
import com.bytechef.atlas.coordinator.job.JobExecutor;
import com.bytechef.atlas.coordinator.message.route.TaskCoordinatorMessageRoute;
import com.bytechef.atlas.coordinator.task.completion.DefaultTaskCompletionHandler;
import com.bytechef.atlas.coordinator.task.completion.TaskCompletionHandlerChain;
import com.bytechef.atlas.coordinator.task.dispatcher.ControlTaskDispatcher;
import com.bytechef.atlas.coordinator.task.dispatcher.DefaultTaskDispatcher;
import com.bytechef.atlas.coordinator.task.dispatcher.StopJobTaskDispatcherPreSendProcessor;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcherChain;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.SuspendedJobs;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.dto.JobParametersDTO;
import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.atlas.execution.facade.JobFacadeImpl;
import com.bytechef.atlas.execution.repository.memory.InMemoryContextRepository;
import com.bytechef.atlas.execution.repository.memory.InMemoryCounterRepository;
import com.bytechef.atlas.execution.repository.memory.InMemoryJobRepository;
import com.bytechef.atlas.execution.repository.memory.InMemoryTaskExecutionRepository;
import com.bytechef.atlas.execution.repository.memory.NonPersistentHandledFlagTaskExecutionRepository;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.ContextServiceImpl;
import com.bytechef.atlas.execution.service.CounterService;
import com.bytechef.atlas.execution.service.CounterServiceImpl;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.JobServiceImpl;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.execution.service.TaskExecutionServiceImpl;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.atlas.worker.TaskWorker;
import com.bytechef.atlas.worker.event.TaskExecutionEvent;
import com.bytechef.atlas.worker.exception.TaskExecutionException;
import com.bytechef.atlas.worker.message.route.TaskWorkerMessageRoute;
import com.bytechef.atlas.worker.task.handler.DefaultTaskHandlerResolver;
import com.bytechef.atlas.worker.task.handler.TaskHandler;
import com.bytechef.atlas.worker.task.handler.TaskHandlerResolverChain;
import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.evaluator.SpelEvaluator;
import com.bytechef.exception.ExecutionException;
import com.bytechef.message.broker.memory.AsyncMessageBroker;
import com.bytechef.message.broker.memory.MemoryMessageBroker;
import com.bytechef.message.event.MessageEvent;
import com.bytechef.message.route.MessageRoute;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.worker.task.SuspendTaskExecutionPostOutputProcessor;
import com.bytechef.platform.workflow.execution.service.InMemoryTaskStateService;
import com.bytechef.platform.workflow.execution.service.TaskStateService;
import com.bytechef.platform.workflow.task.dispatcher.test.annotation.TaskDispatcherIntTest;
import com.bytechef.platform.workflow.task.dispatcher.test.task.handler.TestVarTaskHandler;
import com.bytechef.platform.workflow.task.dispatcher.test.workflow.TaskDispatcherJobTestExecutor;
import com.bytechef.platform.workflow.task.dispatcher.test.workflow.TaskDispatcherJobTestExecutor.TaskDispatcherJobExecution;
import com.bytechef.task.dispatcher.fork.join.ForkJoinTaskDispatcher;
import com.bytechef.task.dispatcher.fork.join.completion.ForkJoinTaskCompletionHandler;
import com.bytechef.task.dispatcher.suspend.SuspendTaskDispatcherPreSendProcessor;
import com.bytechef.task.dispatcher.suspend.completion.SuspendTaskCompletionHandler;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.constant.TenantConstants;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.env.Environment;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author Ivica Cardic
 */
@TaskDispatcherIntTest
public class ForkJoinTaskDispatcherIntTest {

    private static final Evaluator EVALUATOR = SpelEvaluator.create();

    private TestVarTaskHandler<Object, Object> testVarTaskHandler;

    @Autowired
    private Environment environment;

    @Autowired
    private TaskDispatcherJobTestExecutor taskDispatcherJobTestExecutor;

    @Autowired
    private TaskExecutor taskExecutor;

    @Autowired
    private TaskFileStorage taskFileStorage;

    @Autowired
    private WorkflowService workflowService;

    @BeforeEach
    void beforeEach() {
        testVarTaskHandler = new TestVarTaskHandler<>(Map::put);
    }

    @Test
    public void testDispatch() {
        taskDispatcherJobTestExecutor.execute(
            EncodingUtils.base64EncodeToString("fork-join_v1"),
            (
                contextService, counterService, taskExecutionService) -> List.of(
                    (taskCompletionHandler, taskDispatcher) -> new ForkJoinTaskCompletionHandler(
                        contextService, counterService, EVALUATOR, taskExecutionService,
                        taskCompletionHandler, taskDispatcher, taskFileStorage)),
            (
                eventPublisher, contextService, counterService, taskExecutionService) -> List.of(
                    taskDispatcher -> new ForkJoinTaskDispatcher(
                        contextService, counterService, EVALUATOR, eventPublisher, taskDispatcher,
                        taskExecutionService, taskFileStorage)),
            () -> Map.of("var/v1/set", testVarTaskHandler));

        Assertions.assertEquals(85, testVarTaskHandler.get("sumVar1"));
        Assertions.assertEquals(112, testVarTaskHandler.get("sumVar2"));
    }

    @Test
    @Timeout(60)
    public void testDispatchWithFailedBranchCancelsOtherBranch() throws InterruptedException {
        CountDownLatch releaseSlowTaskLatch = new CountDownLatch(1);
        CountDownLatch slowTaskFinishedLatch = new CountDownLatch(1);
        CountDownLatch slowTaskStartedLatch = new CountDownLatch(1);
        AtomicBoolean slowTaskInterrupted = new AtomicBoolean();

        TaskHandler<Object> failTaskHandler = taskExecution -> {
            try {
                slowTaskStartedLatch.await(30, TimeUnit.SECONDS);
            } catch (InterruptedException _) {
                Thread.currentThread()
                    .interrupt();
            }

            throw new TaskExecutionException("branch failed");
        };

        TaskHandler<Object> slowTaskHandler = taskExecution -> {
            slowTaskStartedLatch.countDown();

            try {
                releaseSlowTaskLatch.await(30, TimeUnit.SECONDS);
            } catch (InterruptedException _) {
                slowTaskInterrupted.set(true);

                Thread.currentThread()
                    .interrupt();
            } finally {
                slowTaskFinishedLatch.countDown();
            }

            return "slow task output";
        };

        try {
            ExecutionException executionException = Assertions.assertThrows(
                ExecutionException.class,
                () -> taskDispatcherJobTestExecutor.execute(
                    EncodingUtils.base64EncodeToString("fork-join_v1-failed-branch"),
                    (
                        contextService, counterService, taskExecutionService) -> List.of(
                            (taskCompletionHandler, taskDispatcher) -> new ForkJoinTaskCompletionHandler(
                                contextService, counterService, EVALUATOR, taskExecutionService,
                                taskCompletionHandler, taskDispatcher, taskFileStorage)),
                    (
                        eventPublisher, contextService, counterService, taskExecutionService) -> List.of(
                            taskDispatcher -> new ForkJoinTaskDispatcher(
                                contextService, counterService, EVALUATOR, eventPublisher, taskDispatcher,
                                taskExecutionService, taskFileStorage)),
                    () -> Map.of(
                        "fail/v1", failTaskHandler, "slow/v1", slowTaskHandler, "var/v1/set", testVarTaskHandler)));

            Assertions.assertEquals("branch failed", executionException.getMessage());

            Assertions.assertTrue(slowTaskFinishedLatch.await(20, TimeUnit.SECONDS));
            Assertions.assertTrue(slowTaskInterrupted.get());
            Assertions.assertNull(testVarTaskHandler.get("afterSlowTask"));
        } finally {
            releaseSlowTaskLatch.countDown();
        }
    }

    @Test
    @Timeout(60)
    public void testDispatchWithFullyDisabledBranch() {
        TaskDispatcherJobExecution jobExecution = taskDispatcherJobTestExecutor.execute(
            EncodingUtils.base64EncodeToString("fork-join_v1-disabled-branch"),
            (
                contextService, counterService, taskExecutionService) -> List.of(
                    (taskCompletionHandler, taskDispatcher) -> new ForkJoinTaskCompletionHandler(
                        contextService, counterService, EVALUATOR, taskExecutionService,
                        taskCompletionHandler, taskDispatcher, taskFileStorage)),
            (
                eventPublisher, contextService, counterService, taskExecutionService) -> List.of(
                    taskDispatcher -> new ForkJoinTaskDispatcher(
                        contextService, counterService, EVALUATOR, eventPublisher, taskDispatcher,
                        taskExecutionService, taskFileStorage)),
            () -> Map.of("var/v1/set", testVarTaskHandler));

        Job job = jobExecution.job();

        Assertions.assertEquals(Job.Status.COMPLETED, job.getStatus());
        Assertions.assertTrue(jobExecution.getExecutionErrors()
            .isEmpty());

        Assertions.assertNull(testVarTaskHandler.get("skippedVar"));

        Assertions.assertEquals(100, testVarTaskHandler.get("var2"));
        Assertions.assertEquals(112, testVarTaskHandler.get("sumVar2"));
    }

    @Test
    @Timeout(60)
    public void testDispatchWithEmptyBranch() {
        TaskDispatcherJobExecution jobExecution = taskDispatcherJobTestExecutor.execute(
            EncodingUtils.base64EncodeToString("fork-join_v1-empty-branch"),
            (
                contextService, counterService, taskExecutionService) -> List.of(
                    (taskCompletionHandler, taskDispatcher) -> new ForkJoinTaskCompletionHandler(
                        contextService, counterService, EVALUATOR, taskExecutionService,
                        taskCompletionHandler, taskDispatcher, taskFileStorage)),
            (
                eventPublisher, contextService, counterService, taskExecutionService) -> List.of(
                    taskDispatcher -> new ForkJoinTaskDispatcher(
                        contextService, counterService, EVALUATOR, eventPublisher, taskDispatcher,
                        taskExecutionService, taskFileStorage)),
            () -> Map.of("var/v1/set", testVarTaskHandler));

        Job job = jobExecution.job();

        Assertions.assertEquals(Job.Status.COMPLETED, job.getStatus());
        Assertions.assertTrue(jobExecution.getExecutionErrors()
            .isEmpty());

        Assertions.assertEquals(100, testVarTaskHandler.get("var2"));
        Assertions.assertEquals(112, testVarTaskHandler.get("sumVar2"));
    }

    @Nested
    class SuspendingBranch {

        private static final Duration TIMEOUT = Duration.ofSeconds(20);

        @Test
        void testSiblingBranchKeepsRunningWhileTheOtherBranchIsSuspendedAndTheJobCompletesAfterResume()
            throws InterruptedException {

            Rig rig = new Rig();

            long jobId = rig.jobFacade.createJob(
                new JobParametersDTO(EncodingUtils.base64EncodeToString("fork-join_v1-suspending-branch")));

            rig.awaitSuspended(jobId);

            TaskExecution siblingTaskExecution = rig.awaitCompletedTaskExecution(jobId, "siblingAfterSuspend");

            Assertions.assertEquals(TaskExecution.Status.COMPLETED, siblingTaskExecution.getStatus());
            Assertions.assertTrue(
                SuspendedJobs.isSuspended(rig.jobService.getJob(jobId)),
                "The job must still wait for the approval after its sibling branch ran");

            Job suspendedJob = rig.jobService.getJob(jobId);

            rig.taskCoordinator.onResumeJobEvent(
                new ResumeJobEvent(
                    jobId,
                    MapUtils.getRequiredLong(
                        suspendedJob.getMetadata(), MetadataConstants.TASK_EXECUTION_RESUME_ID),
                    Map.of("approved", true)));

            rig.awaitJobStatus(jobId, Job.Status.COMPLETED);

            for (TaskExecution taskExecution : rig.taskExecutionService.getJobTaskExecutions(jobId)) {
                Assertions.assertEquals(
                    TaskExecution.Status.COMPLETED, taskExecution.getStatus(),
                    "Task execution " + taskExecution.getName() + " did not complete");
            }
        }

        @Test
        void testSecondSuspendingBranchWaitsForTheFirstSuspendToResumeInsteadOfReplacingItsResumeState()
            throws InterruptedException {

            Rig rig = new Rig();

            long jobId = rig.jobFacade.createJob(
                new JobParametersDTO(EncodingUtils.base64EncodeToString("fork-join_v1-two-suspending-branches")));

            rig.awaitSuspended(jobId);

            Job firstSuspendedJob = rig.jobService.getJob(jobId);

            Object firstJobResumeId = firstSuspendedJob.getMetadata(MetadataConstants.JOB_RESUME_ID);
            long firstTaskExecutionId = MapUtils.getRequiredLong(
                firstSuspendedJob.getMetadata(), MetadataConstants.TASK_EXECUTION_RESUME_ID);

            TaskExecution secondApprovalTaskExecution = rig.awaitCompletedTaskExecution(jobId, "secondApproval");

            rig.awaitJob(
                jobId, job -> job.getMetadata(MetadataConstants.QUEUED_SUSPENDS) != null,
                "queue the second suspend");

            Job jobWithQueuedSuspend = rig.jobService.getJob(jobId);

            Assertions.assertTrue(SuspendedJobs.isSuspended(jobWithQueuedSuspend));
            Assertions.assertEquals(
                firstJobResumeId, jobWithQueuedSuspend.getMetadata(MetadataConstants.JOB_RESUME_ID),
                "The second suspend must not replace the resume id of the first");
            Assertions.assertEquals(
                firstTaskExecutionId,
                MapUtils.getRequiredLong(
                    jobWithQueuedSuspend.getMetadata(), MetadataConstants.TASK_EXECUTION_RESUME_ID));

            rig.taskCoordinator.onResumeJobEvent(
                new ResumeJobEvent(jobId, firstTaskExecutionId, Map.of("approved", true)));

            long secondTaskExecutionId = Objects.requireNonNull(secondApprovalTaskExecution.getId());

            rig.awaitJob(
                jobId,
                job -> SuspendedJobs.isSuspended(job) &&
                    Objects.equals(
                        secondTaskExecutionId,
                        MapUtils.getLong(job.getMetadata(), MetadataConstants.TASK_EXECUTION_RESUME_ID)),
                "suspend on the second approval once the first resumed");

            Assertions.assertEquals(
                TaskExecution.Status.COMPLETED,
                rig.awaitCompletedTaskExecution(jobId, "afterApproval")
                    .getStatus());

            Job secondSuspendedJob = rig.jobService.getJob(jobId);

            Assertions.assertNull(secondSuspendedJob.getMetadata(MetadataConstants.QUEUED_SUSPENDS));
            Assertions.assertNotEquals(
                firstJobResumeId, secondSuspendedJob.getMetadata(MetadataConstants.JOB_RESUME_ID));

            rig.taskCoordinator.onResumeJobEvent(
                new ResumeJobEvent(jobId, secondTaskExecutionId, Map.of("approved", true)));

            rig.awaitJobStatus(jobId, Job.Status.COMPLETED);

            for (TaskExecution taskExecution : rig.taskExecutionService.getJobTaskExecutions(jobId)) {
                Assertions.assertEquals(
                    TaskExecution.Status.COMPLETED, taskExecution.getStatus(),
                    "Task execution " + taskExecution.getName() + " did not complete");
            }

            Assertions.assertNull(rig.firstErrorReference.get());
        }

        private final class Rig {

            private final AtomicReference<String> firstErrorReference = new AtomicReference<>();
            private final JobFacade jobFacade;
            private final JobService jobService;
            private final AsyncMessageBroker messageBroker;
            private final TaskCoordinator taskCoordinator;
            private final TaskExecutionService taskExecutionService;

            private Rig() {
                InMemoryTaskExecutionRepository taskExecutionRepository =
                    new NonPersistentHandledFlagTaskExecutionRepository();

                jobService = new JobServiceImpl(new InMemoryJobRepository(taskExecutionRepository, new JsonMapper()));
                taskExecutionService = new TaskExecutionServiceImpl(taskExecutionRepository);

                ContextService contextService = new ContextServiceImpl(new InMemoryContextRepository());
                CounterService counterService = new CounterServiceImpl(new InMemoryCounterRepository());

                messageBroker = new AsyncMessageBroker(environment);

                ApplicationEventPublisher eventPublisher = createEventPublisher(messageBroker);
                TaskStateService taskStateService = new InMemoryTaskStateService();

                jobFacade = new JobFacadeImpl(
                    eventPublisher, contextService, jobService, taskExecutionService, taskFileStorage,
                    workflowService);

                Map<String, TaskHandler<?>> taskHandlerMap = Map.of(
                    "suspend/v1", ForkJoinTaskDispatcherIntTest::suspendUntilResumed,
                    "var/v1/set", taskExecution -> taskExecution.getParameters()
                        .get("value"),
                    "waitForSuspend/v1", taskExecution -> waitForSuspend(taskExecution.getJobId()));

                TaskHandlerResolverChain taskHandlerResolverChain = new TaskHandlerResolverChain();

                taskHandlerResolverChain.setTaskHandlerResolvers(
                    List.of(new DefaultTaskHandlerResolver(taskHandlerMap::get)));

                AsyncTaskExecutor asyncTaskExecutor = taskExecutor::execute;

                TaskWorker taskWorker = new TaskWorker(
                    null, EVALUATOR, eventPublisher, asyncTaskExecutor, taskHandlerResolverChain, taskFileStorage,
                    List.of(new SuspendTaskExecutionPostOutputProcessor(null)));

                TaskDispatcherChain taskDispatcherChain = new TaskDispatcherChain();

                taskDispatcherChain.setTaskDispatcherResolvers(
                    List.of(
                        new ForkJoinTaskDispatcher(
                            contextService, counterService, EVALUATOR, eventPublisher, taskDispatcherChain,
                            taskExecutionService, taskFileStorage),
                        new ControlTaskDispatcher(eventPublisher),
                        new DefaultTaskDispatcher(
                            eventPublisher,
                            List.of(
                                new StopJobTaskDispatcherPreSendProcessor(jobService),
                                new SuspendTaskDispatcherPreSendProcessor(eventPublisher, jobService, taskStateService,
                                    null)))));

                JobExecutor jobExecutor = new JobExecutor(
                    contextService, EVALUATOR, eventPublisher, jobService, taskDispatcherChain,
                    taskExecutionService, taskFileStorage, workflowService);

                TaskCompletionHandlerChain taskCompletionHandlerChain = new TaskCompletionHandlerChain();

                taskCompletionHandlerChain.setTaskCompletionHandlers(
                    List.of(
                        new SuspendTaskCompletionHandler(
                            contextService, eventPublisher, jobService, taskExecutionService, taskFileStorage,
                            taskStateService),
                        new ForkJoinTaskCompletionHandler(
                            contextService, counterService, EVALUATOR, taskExecutionService,
                            taskCompletionHandlerChain, taskDispatcherChain, taskFileStorage),
                        new DefaultTaskCompletionHandler(
                            contextService, jobExecutor, jobService, taskExecutionService, taskFileStorage,
                            workflowService)));

                taskCoordinator = new TaskCoordinator(
                    List.of(
                        new TaskStartedApplicationEventListener(taskExecutionService, taskDispatcherChain, jobService)),
                    List.of(), eventPublisher, jobExecutor, jobService, taskCompletionHandlerChain, taskDispatcherChain,
                    taskExecutionService);

                TaskExecutionErrorEventListener taskExecutionErrorEventListener = new TaskExecutionErrorEventListener(
                    eventPublisher, contextService, jobService, taskDispatcherChain, taskExecutionService,
                    taskFileStorage);

                receive(
                    TaskWorkerMessageRoute.TASK_EXECUTION_EVENTS,
                    event -> dispatchToWorker((TaskExecutionEvent) event, taskWorker));
                receive(
                    TaskCoordinatorMessageRoute.APPLICATION_EVENTS,
                    event -> taskCoordinator.onApplicationEvent((ApplicationEvent) event));
                receive(
                    TaskCoordinatorMessageRoute.ERROR_EVENTS,
                    event -> handleErrorEvent((ErrorEvent) event, taskExecutionErrorEventListener));
                receive(
                    TaskCoordinatorMessageRoute.JOB_RESUME_EVENTS,
                    event -> taskCoordinator.onResumeJobEvent((ResumeJobEvent) event));
                receive(
                    TaskCoordinatorMessageRoute.JOB_START_EVENTS,
                    event -> taskCoordinator.onStartJobEvent((StartJobEvent) event));
                receive(
                    TaskCoordinatorMessageRoute.TASK_EXECUTION_COMPLETE_EVENTS,
                    event -> taskCoordinator.onTaskExecutionCompleteEvent((TaskExecutionCompleteEvent) event));
            }

            void awaitSuspended(long jobId) throws InterruptedException {
                Instant deadline = Instant.now()
                    .plus(TIMEOUT);

                while (!SuspendedJobs.isSuspended(jobService.getJob(jobId))) {
                    if (Instant.now()
                        .isAfter(deadline)) {

                        Job job = jobService.getJob(jobId);

                        throw new AssertionError(
                            "Job %d never suspended, it is %s with metadata %s and tasks %s%s".formatted(
                                jobId, job.getStatus(), job.getMetadata(), describeTaskExecutions(jobId),
                                describeFirstError()));
                    }

                    Thread.sleep(10);
                }
            }

            TaskExecution awaitCompletedTaskExecution(long jobId, String name) throws InterruptedException {
                Instant deadline = Instant.now()
                    .plus(TIMEOUT);

                while (true) {
                    Optional<TaskExecution> taskExecutionOptional = taskExecutionService.getJobTaskExecutions(jobId)
                        .stream()
                        .filter(taskExecution -> name.equals(taskExecution.getName()))
                        .findFirst();

                    if (taskExecutionOptional.isPresent() && taskExecutionOptional.get()
                        .getStatus()
                        .isTerminated()) {

                        return taskExecutionOptional.get();
                    }

                    if (Instant.now()
                        .isAfter(deadline)) {

                        throw new AssertionError(
                            "Task %s of job %d never finished, job is %s, tasks %s%s".formatted(
                                name, jobId, jobService.getJob(jobId)
                                    .getStatus(),
                                describeTaskExecutions(jobId), describeFirstError()));
                    }

                    Thread.sleep(10);
                }
            }

            void awaitJob(long jobId, Predicate<Job> condition, String description) throws InterruptedException {
                Instant deadline = Instant.now()
                    .plus(TIMEOUT);

                while (!condition.test(jobService.getJob(jobId))) {
                    if (Instant.now()
                        .isAfter(deadline)) {

                        Job job = jobService.getJob(jobId);

                        throw new AssertionError(
                            "Job %d did not %s, it is %s with metadata %s and tasks %s%s".formatted(
                                jobId, description, job.getStatus(), job.getMetadata(), describeTaskExecutions(jobId),
                                describeFirstError()));
                    }

                    Thread.sleep(10);
                }
            }

            void awaitJobStatus(long jobId, Job.Status status) throws InterruptedException {
                Instant deadline = Instant.now()
                    .plus(TIMEOUT);

                while (true) {
                    Job job = jobService.getJob(jobId);

                    if (job.getStatus() == status) {
                        return;
                    }

                    if (Instant.now()
                        .isAfter(deadline)) {

                        throw new AssertionError(
                            "Job %d never reached %s, it is %s%s".formatted(
                                jobId, status, job.getStatus(), describeFirstError()));
                    }

                    Thread.sleep(10);
                }
            }

            private String describeTaskExecutions(long jobId) {
                return taskExecutionService.getJobTaskExecutions(jobId)
                    .stream()
                    .map(taskExecution -> taskExecution.getName() + "=" + taskExecution.getStatus())
                    .toList()
                    .toString();
            }

            private String describeFirstError() {
                String firstError = firstErrorReference.get();

                return firstError == null ? "" : "; first error event: " + firstError;
            }

            private void dispatchToWorker(TaskExecutionEvent taskExecutionEvent, TaskWorker taskWorker) {
                TaskExecution taskExecution = taskExecutionEvent.getTaskExecution();

                Job job = jobService.getJob(Objects.requireNonNull(taskExecution.getJobId()));

                if (!SuspendedJobs.canRunTasks(job)) {
                    return;
                }

                taskWorker.onTaskExecutionEvent(taskExecutionEvent);
            }

            private void handleErrorEvent(
                ErrorEvent errorEvent, TaskExecutionErrorEventListener taskExecutionErrorEventListener) {

                firstErrorReference.compareAndSet(null, String.valueOf(errorEvent.getError()));

                taskExecutionErrorEventListener.onErrorEvent(errorEvent);
            }

            private void receive(MessageRoute messageRoute, MemoryMessageBroker.Receiver receiver) {
                messageBroker.receive(messageRoute, message -> {
                    String tenantId = (String) ((MessageEvent<?>) message).getMetadata(
                        TenantConstants.CURRENT_TENANT_ID);

                    TenantContext.runWithTenantId(tenantId, () -> receiver.receive(message));
                });
            }

            private Object waitForSuspend(Long jobId) throws TaskExecutionException {
                try {
                    awaitSuspended(Objects.requireNonNull(jobId));
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread()
                        .interrupt();

                    throw new TaskExecutionException("Interrupted while waiting for the job to suspend");
                }

                return "suspended";
            }
        }
    }

    private static Object suspendUntilResumed(TaskExecution taskExecution) {
        Map<String, ?> metadata = taskExecution.getMetadata();

        Object resumeData = metadata.get(MetadataConstants.RESUME_DATA);

        if (resumeData != null) {
            return resumeData;
        }

        if (metadata.containsKey(MetadataConstants.SUSPEND)) {
            return Map.of();
        }

        return new ActionContext.Suspend(Map.of(), null);
    }

    private static ApplicationEventPublisher createEventPublisher(AsyncMessageBroker messageBroker) {
        return event -> {
            if (!(event instanceof MessageEvent<?> messageEvent)) {
                return;
            }

            messageEvent.putMetadata(TenantConstants.CURRENT_TENANT_ID, TenantContext.getCurrentTenantId());

            messageBroker.send(messageEvent.getRoute(), messageEvent);
        };
    }
}
