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

package com.bytechef.task.dispatcher.subflow.event.listener;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.coordinator.TaskCoordinator;
import com.bytechef.atlas.coordinator.event.ApplicationEvent;
import com.bytechef.atlas.coordinator.event.ErrorEvent;
import com.bytechef.atlas.coordinator.event.JobStatusApplicationEvent;
import com.bytechef.atlas.coordinator.event.ResumeJobEvent;
import com.bytechef.atlas.coordinator.event.StartJobEvent;
import com.bytechef.atlas.coordinator.event.StopJobEvent;
import com.bytechef.atlas.coordinator.event.TaskExecutionCompleteEvent;
import com.bytechef.atlas.coordinator.event.listener.ApplicationEventListener;
import com.bytechef.atlas.coordinator.event.listener.TaskExecutionErrorEventListener;
import com.bytechef.atlas.coordinator.event.listener.TaskStartedApplicationEventListener;
import com.bytechef.atlas.coordinator.job.JobExecutor;
import com.bytechef.atlas.coordinator.message.route.TaskCoordinatorMessageRoute;
import com.bytechef.atlas.coordinator.task.completion.DefaultTaskCompletionHandler;
import com.bytechef.atlas.coordinator.task.completion.TaskCompletionHandlerChain;
import com.bytechef.atlas.coordinator.task.dispatcher.ControlTaskDispatcher;
import com.bytechef.atlas.coordinator.task.dispatcher.DefaultTaskDispatcher;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcherChain;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.dto.JobParametersDTO;
import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.atlas.execution.facade.JobFacadeImpl;
import com.bytechef.atlas.execution.repository.memory.InMemoryContextRepository;
import com.bytechef.atlas.execution.repository.memory.InMemoryJobRepository;
import com.bytechef.atlas.execution.repository.memory.InMemoryTaskExecutionRepository;
import com.bytechef.atlas.execution.repository.memory.NonPersistentHandledFlagTaskExecutionRepository;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.ContextServiceImpl;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.JobServiceImpl;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.execution.service.TaskExecutionServiceImpl;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.atlas.worker.TaskWorker;
import com.bytechef.atlas.worker.event.TaskExecutionEvent;
import com.bytechef.atlas.worker.message.route.TaskWorkerMessageRoute;
import com.bytechef.atlas.worker.task.handler.DefaultTaskHandlerResolver;
import com.bytechef.atlas.worker.task.handler.TaskHandler;
import com.bytechef.atlas.worker.task.handler.TaskHandlerResolverChain;
import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.evaluator.SpelEvaluator;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.message.broker.memory.AsyncMessageBroker;
import com.bytechef.message.broker.memory.MemoryMessageBroker;
import com.bytechef.message.event.MessageEvent;
import com.bytechef.message.route.MessageRoute;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.worker.task.SuspendTaskExecutionPostOutputProcessor;
import com.bytechef.platform.workflow.execution.service.InMemoryTaskStateService;
import com.bytechef.platform.workflow.execution.service.TaskStateService;
import com.bytechef.platform.workflow.task.dispatcher.subflow.ChildJobPrincipalFactory;
import com.bytechef.platform.workflow.task.dispatcher.subflow.SubflowResolver;
import com.bytechef.platform.workflow.task.dispatcher.test.annotation.TaskDispatcherIntTest;
import com.bytechef.task.dispatcher.subflow.SubflowTaskDispatcher;
import com.bytechef.task.dispatcher.suspend.SuspendTaskDispatcherPreSendProcessor;
import com.bytechef.task.dispatcher.suspend.completion.SuspendTaskCompletionHandler;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.constant.TenantConstants;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
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
class SubflowJobStatusEventListenerIntTest {

    private static final String APPROVAL_DECISION = "the human approved";
    private static final String CHILD_WORKFLOW = "subflow-resumable-child";
    private static final String DECISION = "decision";
    private static final Duration CHILD_LAUNCH_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration JOB_STATUS_TIMEOUT = Duration.ofSeconds(20);
    private static final String PARENT_WORKFLOW = "subflow-parent-of-suspending-child";
    private static final Duration PARENT_MUST_STAY_STARTED_FOR = Duration.ofSeconds(2);

    @Autowired
    private Environment environment;

    @Autowired
    private TaskExecutor taskExecutor;

    @Autowired
    private TaskFileStorage taskFileStorage;

    @Autowired
    private WorkflowService workflowService;

    @Test
    void testSuspendedChildResumesThroughCoordinatorAndParentCompletes() throws InterruptedException {
        Rig rig = new Rig();

        long parentJobId = rig.startParent();
        long childJobId = rig.awaitChildLaunched();

        Job suspendedChildJob = rig.awaitJobStatus(childJobId, Job.Status.STOPPED);

        assertNotNull(suspendedChildJob.getMetadata(MetadataConstants.JOB_RESUME_ID));

        rig.assertParentStaysStarted(parentJobId);

        rig.resumeThroughCoordinator(childJobId, Map.of(DECISION, APPROVAL_DECISION));

        rig.awaitJobStatus(childJobId, Job.Status.COMPLETED);

        rig.awaitJobStatus(parentJobId, Job.Status.COMPLETED);

        Map<String, ?> parentOutputs = rig.awaitJobOutputs(parentJobId);

        assertEquals(APPROVAL_DECISION, parentOutputs.get("childResult"));
    }

    @Test
    void testSuspendedChildResumesThroughJobFacadeAndParentCompletes() throws InterruptedException {
        Rig rig = new Rig();

        long parentJobId = rig.startParent();
        long childJobId = rig.awaitChildLaunched();

        Job suspendedChildJob = rig.awaitJobStatus(childJobId, Job.Status.STOPPED);

        assertNotNull(suspendedChildJob.getMetadata(MetadataConstants.JOB_RESUME_ID));

        rig.assertParentStaysStarted(parentJobId);

        rig.resumeThroughJobFacade(childJobId, Map.of(DECISION, APPROVAL_DECISION, "approved", true));

        rig.awaitJobStatus(childJobId, Job.Status.COMPLETED);

        rig.awaitJobStatus(parentJobId, Job.Status.COMPLETED);

        Map<String, ?> parentOutputs = rig.awaitJobOutputs(parentJobId);

        assertEquals(APPROVAL_DECISION, parentOutputs.get("childResult"));
    }

    @Test
    void testSuspendedChildStoppedByHandStopsTheParent() throws InterruptedException {
        Rig rig = new Rig();

        long parentJobId = rig.startParent();
        long childJobId = rig.awaitChildLaunched();

        rig.awaitJobStatus(childJobId, Job.Status.STOPPED);

        rig.taskCoordinator.onApplicationEvent(new JobStatusApplicationEvent(childJobId, Job.Status.STOPPED));

        rig.awaitJobStatus(parentJobId, Job.Status.STOPPED);
    }

    @Test
    void testCancelledChildStopsTheParent() throws InterruptedException {
        Rig rig = new Rig();

        long parentJobId = rig.startParent();
        long childJobId = rig.awaitChildLaunched();

        rig.awaitJobStatus(childJobId, Job.Status.STOPPED);

        rig.taskCoordinator.onApplicationEvent(new JobStatusApplicationEvent(childJobId, Job.Status.CANCELLED));

        rig.awaitJobStatus(parentJobId, Job.Status.STOPPED);
    }

    private final class Rig {

        private final AtomicReference<Long> childJobIdReference = new AtomicReference<>();
        private final AtomicReference<String> firstErrorReference = new AtomicReference<>();
        private final JobFacade jobFacade;
        private final JobService jobService;
        private final AsyncMessageBroker messageBroker;
        private final TaskCoordinator taskCoordinator;

        private Rig() {
            InMemoryTaskExecutionRepository taskExecutionRepository =
                new NonPersistentHandledFlagTaskExecutionRepository();

            jobService = new JobServiceImpl(new InMemoryJobRepository(taskExecutionRepository, new JsonMapper()));

            TaskExecutionService taskExecutionService = new TaskExecutionServiceImpl(taskExecutionRepository);

            ContextService contextService = new ContextServiceImpl(new InMemoryContextRepository());

            messageBroker = new AsyncMessageBroker(environment);

            ApplicationEventPublisher eventPublisher = createEventPublisher(messageBroker);
            TaskStateService taskStateService = new InMemoryTaskStateService();

            jobFacade = new JobFacadeImpl(
                eventPublisher, contextService, jobService, taskExecutionService, taskFileStorage, workflowService);

            SubflowJobStatusEventListener subflowJobStatusEventListener = new SubflowJobStatusEventListener(
                SpelEvaluator.create(), eventPublisher, jobService, taskExecutionService, taskFileStorage);

            ChildJobPrincipalFactory childJobPrincipalFactory = new StartingChildJobPrincipalFactory(
                jobFacade, childJobIdReference);
            SubflowResolver subflowResolver =
                (workflowUuid, triggerName, editorEnvironment) -> new SubflowResolver.Subflow(
                    encodeWorkflowId(CHILD_WORKFLOW), "newWorkflowCall");

            Map<String, TaskHandler<?>> taskHandlerMap = Map.of(
                "var/v1/set", taskExecution -> taskExecution.getParameters()
                    .get("value"),
                "suspend/v1", SubflowJobStatusEventListenerIntTest::suspendUntilResumed);

            TaskHandlerResolverChain taskHandlerResolverChain = new TaskHandlerResolverChain();

            taskHandlerResolverChain.setTaskHandlerResolvers(
                List.of(new DefaultTaskHandlerResolver(taskHandlerMap::get)));

            AsyncTaskExecutor asyncTaskExecutor = taskExecutor::execute;

            TaskWorker taskWorker = new TaskWorker(
                null, SpelEvaluator.create(), eventPublisher, asyncTaskExecutor, taskHandlerResolverChain,
                taskFileStorage, List.of(new SuspendTaskExecutionPostOutputProcessor(null)));

            TaskDispatcherChain taskDispatcherChain = new TaskDispatcherChain();

            taskDispatcherChain.setTaskDispatcherResolvers(
                List.of(
                    new SubflowTaskDispatcher(childJobPrincipalFactory, jobService, subflowResolver),
                    new ControlTaskDispatcher(eventPublisher),
                    new DefaultTaskDispatcher(
                        eventPublisher,
                        List.of(new SuspendTaskDispatcherPreSendProcessor(jobService, taskStateService, null)))));

            JobExecutor jobExecutor = new JobExecutor(
                contextService, SpelEvaluator.create(), taskDispatcherChain, taskExecutionService, taskFileStorage,
                workflowService);

            TaskCompletionHandlerChain taskCompletionHandlerChain = new TaskCompletionHandlerChain();

            taskCompletionHandlerChain.setTaskCompletionHandlers(
                List.of(
                    new SuspendTaskCompletionHandler(
                        contextService, eventPublisher, jobService, taskExecutionService, taskFileStorage,
                        taskStateService),
                    new DefaultTaskCompletionHandler(
                        contextService, SpelEvaluator.create(), eventPublisher, jobExecutor, jobService,
                        taskExecutionService, taskFileStorage, workflowService)));

            List<ApplicationEventListener> applicationEventListeners = List.of(
                subflowJobStatusEventListener,
                new TaskStartedApplicationEventListener(taskExecutionService, taskDispatcherChain, jobService));

            taskCoordinator = new TaskCoordinator(
                applicationEventListeners, List.of(), eventPublisher, jobExecutor, jobService,
                taskCompletionHandlerChain, taskDispatcherChain, taskExecutionService);

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
                TaskCoordinatorMessageRoute.JOB_STOP_EVENTS, event -> stopStartedJob((StopJobEvent) event));
            receive(
                TaskCoordinatorMessageRoute.TASK_EXECUTION_COMPLETE_EVENTS,
                event -> taskCoordinator.onTaskExecutionCompleteEvent((TaskExecutionCompleteEvent) event));
        }

        long startParent() {
            return jobFacade.createJob(new JobParametersDTO(encodeWorkflowId(PARENT_WORKFLOW)));
        }

        long awaitChildLaunched() throws InterruptedException {
            Instant deadline = Instant.now()
                .plus(CHILD_LAUNCH_TIMEOUT);

            while (childJobIdReference.get() == null) {
                if (Instant.now()
                    .isAfter(deadline)) {

                    throw new AssertionError(
                        "The subflow dispatcher never launched the child job" + describeFirstError());
                }

                Thread.sleep(10);
            }

            return childJobIdReference.get();
        }

        Job awaitJobStatus(long jobId, Job.Status status) throws InterruptedException {
            Instant deadline = Instant.now()
                .plus(JOB_STATUS_TIMEOUT);

            while (true) {
                Job job = jobService.getJob(jobId);

                if (job.getStatus() == status) {
                    return job;
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

        Map<String, ?> awaitJobOutputs(long jobId) throws InterruptedException {
            Instant deadline = Instant.now()
                .plus(JOB_STATUS_TIMEOUT);

            while (true) {
                Job job = jobService.getJob(jobId);

                FileEntry outputs = job.getOutputs();

                if (outputs != null) {
                    return taskFileStorage.readJobOutputs(outputs);
                }

                if (Instant.now()
                    .isAfter(deadline)) {

                    throw new AssertionError(
                        "Job %d never stored its outputs%s".formatted(jobId, describeFirstError()));
                }

                Thread.sleep(10);
            }
        }

        void assertParentStaysStarted(long parentJobId) throws InterruptedException {
            Instant deadline = Instant.now()
                .plus(PARENT_MUST_STAY_STARTED_FOR);

            while (Instant.now()
                .isBefore(deadline)) {

                Job parentJob = jobService.getJob(parentJobId);

                assertEquals(
                    Job.Status.STARTED, parentJob.getStatus(),
                    "The parent must keep running while its child is suspended");

                Thread.sleep(50);
            }
        }

        void resumeThroughCoordinator(long childJobId, Map<String, ?> data) {
            taskCoordinator.onResumeJobEvent(
                new ResumeJobEvent(childJobId, getTaskExecutionResumeId(childJobId), data));
        }

        void resumeThroughJobFacade(long childJobId, Map<String, ?> data) {
            jobFacade.resumeJob(childJobId, getTaskExecutionResumeId(childJobId), data);
        }

        private long getTaskExecutionResumeId(long childJobId) {
            Job childJob = jobService.getJob(childJobId);

            return MapUtils.getRequiredLong(
                childJob.getMetadata(), MetadataConstants.TASK_EXECUTION_RESUME_ID);
        }

        private String describeFirstError() {
            String firstError = firstErrorReference.get();

            return firstError == null ? "" : "; first error event: " + firstError;
        }

        private void dispatchToWorker(TaskExecutionEvent taskExecutionEvent, TaskWorker taskWorker) {
            TaskExecution taskExecution = taskExecutionEvent.getTaskExecution();

            Job job = jobService.getJob(Objects.requireNonNull(taskExecution.getJobId()));

            if (job.getStatus() != Job.Status.STARTED) {
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

        private void stopStartedJob(StopJobEvent stopJobEvent) {
            Optional<Job> jobOptional = jobService.fetchJob(stopJobEvent.getJobId());

            if (jobOptional.isEmpty()) {
                return;
            }

            Job job = jobOptional.get();

            if (job.getStatus() != Job.Status.STARTED) {
                return;
            }

            taskCoordinator.onStopJobEvent(stopJobEvent);
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

    private static String encodeWorkflowId(String workflowName) {
        return EncodingUtils.base64EncodeToString(workflowName.getBytes(StandardCharsets.UTF_8));
    }

    private static final class StartingChildJobPrincipalFactory implements ChildJobPrincipalFactory {

        private final AtomicReference<Long> childJobIdReference;
        private final JobFacade jobFacade;

        private StartingChildJobPrincipalFactory(JobFacade jobFacade, AtomicReference<Long> childJobIdReference) {
            this.childJobIdReference = childJobIdReference;
            this.jobFacade = jobFacade;
        }

        @Override
        public long createChildJob(long parentJobId, JobParametersDTO jobParametersDTO) {
            long childJobId = jobFacade.createJob(jobParametersDTO);

            childJobIdReference.set(childJobId);

            return childJobId;
        }

        @Override
        public long createPrincipalLinkedJob(long referenceJobId, JobParametersDTO jobParametersDTO) {
            throw new UnsupportedOperationException();
        }
    }
}
