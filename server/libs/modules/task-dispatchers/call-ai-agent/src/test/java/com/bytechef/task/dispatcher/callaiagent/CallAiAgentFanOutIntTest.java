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

package com.bytechef.task.dispatcher.callaiagent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.coordinator.TaskCoordinator;
import com.bytechef.atlas.coordinator.event.ApplicationEvent;
import com.bytechef.atlas.coordinator.event.ErrorEvent;
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
import com.bytechef.platform.workflow.execution.facade.InMemoryTaskStateFacade;
import com.bytechef.platform.workflow.execution.facade.TaskStateFacade;
import com.bytechef.platform.workflow.task.dispatcher.subflow.CallableAiAgentDataSource;
import com.bytechef.platform.workflow.task.dispatcher.subflow.ChildJobPrincipalFactory;
import com.bytechef.platform.workflow.task.dispatcher.subflow.SubflowResolver;
import com.bytechef.platform.workflow.task.dispatcher.test.annotation.TaskDispatcherIntTest;
import com.bytechef.task.dispatcher.map.MapTaskDispatcher;
import com.bytechef.task.dispatcher.map.completion.MapTaskCompletionHandler;
import com.bytechef.task.dispatcher.parallel.ParallelTaskDispatcher;
import com.bytechef.task.dispatcher.parallel.completion.ParallelTaskCompletionHandler;
import com.bytechef.task.dispatcher.subflow.event.listener.SubflowJobStatusEventListener;
import com.bytechef.task.dispatcher.suspend.SuspendTaskDispatcherPreSendProcessor;
import com.bytechef.task.dispatcher.suspend.completion.SuspendTaskCompletionHandler;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.constant.TenantConstants;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
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
class CallAiAgentFanOutIntTest {

    private static final String ALPHA_AGENT_UUID = "alpha-agent";
    private static final String ALPHA_CHILD_WORKFLOW = "call-ai-agent-child-alpha";
    private static final String BETA_CHILD_WORKFLOW = "call-ai-agent-child-beta";
    private static final Duration CHILD_LAUNCH_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration JOB_STATUS_TIMEOUT = Duration.ofSeconds(20);
    private static final String MAP_PARENT_WORKFLOW = "call-ai-agent-map-fan-out-parent";
    private static final String PARENT_WORKFLOW = "call-ai-agent-fan-out-parent";
    private static final String WORKFLOW_SUFFIX = "-workflow";

    @Autowired
    private Environment environment;

    @Autowired
    private TaskExecutor taskExecutor;

    @Autowired
    private TaskFileStorage taskFileStorage;

    @Autowired
    private WorkflowService workflowService;

    @Test
    void testTwoAgentsRunConcurrentlyAndBothRepliesReachTheParent() throws InterruptedException {
        Rig rig = new Rig();

        long parentJobId = rig.startParent(PARENT_WORKFLOW);

        List<Long> childJobIds = rig.awaitChildJobsLaunched(2);

        for (long childJobId : childJobIds) {
            rig.awaitJobStatus(childJobId, Job.Status.STOPPED);
        }

        Set<Long> parentTaskExecutionIds = new HashSet<>();

        for (long childJobId : childJobIds) {
            assertEquals(Job.Status.STOPPED, rig.jobStatus(childJobId));

            Long parentTaskExecutionId = rig.parentTaskExecutionId(childJobId);

            assertNotNull(parentTaskExecutionId);

            parentTaskExecutionIds.add(parentTaskExecutionId);
        }

        assertEquals(childJobIds.size(), parentTaskExecutionIds.size());

        assertEquals(Job.Status.STARTED, rig.jobStatus(parentJobId));

        for (long childJobId : childJobIds) {
            rig.resume(childJobId);
        }

        for (long childJobId : childJobIds) {
            rig.awaitJobStatus(childJobId, Job.Status.COMPLETED);
        }

        rig.awaitJobStatus(parentJobId, Job.Status.COMPLETED);

        for (long childJobId : childJobIds) {
            Map<String, ?> childOutputs = rig.awaitJobOutputs(childJobId);

            assertNotNull(childOutputs.get("reply"));
        }

        assertEquals("alpha replied", rig.agentReply(parentJobId, "callAlpha"));
        assertEquals("beta replied", rig.agentReply(parentJobId, "callBeta"));
    }

    @Test
    void testOneAgentNodeOverAnItemListFansOutAndAggregatesRepliesInOrder() throws InterruptedException {
        Rig rig = new Rig();

        long parentJobId = rig.startParent(MAP_PARENT_WORKFLOW);

        List<Long> childJobIds = rig.awaitChildJobsLaunched(2);

        for (long childJobId : childJobIds) {
            rig.awaitJobStatus(childJobId, Job.Status.STOPPED);
        }

        Set<Long> parentTaskExecutionIds = new HashSet<>();

        for (long childJobId : childJobIds) {
            assertEquals(Job.Status.STOPPED, rig.jobStatus(childJobId));

            Long parentTaskExecutionId = rig.parentTaskExecutionId(childJobId);

            assertNotNull(parentTaskExecutionId);

            parentTaskExecutionIds.add(parentTaskExecutionId);
        }

        assertEquals(childJobIds.size(), parentTaskExecutionIds.size());

        assertEquals(Job.Status.STARTED, rig.jobStatus(parentJobId));

        for (long childJobId : childJobIds) {
            rig.resume(childJobId);
        }

        rig.awaitJobStatus(parentJobId, Job.Status.COMPLETED);

        List<?> replies = rig.aggregatedOutput(parentJobId, "mapFanOut");

        assertEquals(2, replies.size());
        assertEquals("alpha replied", ((Map<?, ?>) replies.get(0)).get("reply"));
        assertEquals("beta replied", ((Map<?, ?>) replies.get(1)).get("reply"));
    }

    private final class Rig {

        private final List<Long> childJobIds = new CopyOnWriteArrayList<>();
        private final ApplicationEventPublisher eventPublisher;
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

            eventPublisher = createEventPublisher(messageBroker);

            TaskStateFacade taskStateFacade = new InMemoryTaskStateFacade();

            jobFacade = new JobFacadeImpl(
                eventPublisher, contextService, jobService, taskExecutionService, taskFileStorage, workflowService);

            SubflowJobStatusEventListener subflowJobStatusEventListener = new SubflowJobStatusEventListener(
                SpelEvaluator.create(), eventPublisher, jobService, taskExecutionService, taskFileStorage);

            ChildJobPrincipalFactory childJobPrincipalFactory = new RecordingChildJobPrincipalFactory(
                jobFacade, childJobIds);
            CallableAiAgentDataSource callableAiAgentDataSource = new StubCallableAiAgentDataSource();
            SubflowResolver subflowResolver =
                (workflowUuid, triggerName, editorEnvironment) -> new SubflowResolver.Subflow(
                    encodeWorkflowId(
                        Objects.equals(workflowUuid, ALPHA_AGENT_UUID + WORKFLOW_SUFFIX) ? ALPHA_CHILD_WORKFLOW
                            : BETA_CHILD_WORKFLOW),
                    "workflowCall");

            Map<String, TaskHandler<?>> taskHandlerMap = Map.of(
                "var/v1/set", taskExecution -> taskExecution.getParameters()
                    .get("value"),
                "suspend/v1", CallAiAgentFanOutIntTest::suspendUntilResumed);

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
                    new MapTaskDispatcher(
                        contextService, counterService, SpelEvaluator.create(), eventPublisher, taskDispatcherChain,
                        taskExecutionService, taskFileStorage),
                    new ParallelTaskDispatcher(
                        contextService, counterService, eventPublisher, taskDispatcherChain, taskExecutionService,
                        taskFileStorage),
                    new CallAiAgentTaskDispatcher(
                        childJobPrincipalFactory, callableAiAgentDataSource, jobService, subflowResolver),
                    new ControlTaskDispatcher(eventPublisher),
                    new DefaultTaskDispatcher(
                        eventPublisher,
                        List.of(
                            new SuspendTaskDispatcherPreSendProcessor(
                                eventPublisher, jobService, taskStateFacade, null)))));

            JobExecutor jobExecutor = new JobExecutor(
                contextService, SpelEvaluator.create(), eventPublisher, jobService, taskDispatcherChain,
                taskExecutionService, taskFileStorage, workflowService);

            TaskCompletionHandlerChain taskCompletionHandlerChain = new TaskCompletionHandlerChain();

            taskCompletionHandlerChain.setTaskCompletionHandlers(
                List.of(
                    new SuspendTaskCompletionHandler(
                        contextService, eventPublisher, jobService, taskExecutionService, taskFileStorage,
                        taskStateFacade),
                    new MapTaskCompletionHandler(
                        contextService, counterService, SpelEvaluator.create(), taskDispatcherChain,
                        taskCompletionHandlerChain, taskExecutionService, taskFileStorage),
                    new ParallelTaskCompletionHandler(
                        counterService, taskCompletionHandlerChain, taskExecutionService),
                    new DefaultTaskCompletionHandler(
                        contextService, jobExecutor, jobService, taskExecutionService, taskFileStorage,
                        workflowService)));

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

        long startParent(String workflowName) {
            return jobFacade.createJob(new JobParametersDTO(encodeWorkflowId(workflowName)));
        }

        List<Long> awaitChildJobsLaunched(int expectedCount) throws InterruptedException {
            Instant deadline = Instant.now()
                .plus(CHILD_LAUNCH_TIMEOUT);

            while (childJobIds.size() < expectedCount) {
                if (Instant.now()
                    .isAfter(deadline)) {

                    throw new AssertionError(
                        "Only %d of %d child jobs were launched%s".formatted(
                            childJobIds.size(), expectedCount, describeFirstError()));
                }

                Thread.sleep(10);
            }

            return List.copyOf(childJobIds);
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

        Object agentReply(long parentJobId, String taskName) {
            TaskExecution agentTaskExecution = taskExecutionService.getJobTaskExecutions(parentJobId)
                .stream()
                .filter(taskExecution -> Objects.equals(taskExecution.getName(), taskName))
                .findFirst()
                .orElseThrow(
                    () -> new AssertionError(
                        "Parent job %d has no task execution named '%s'".formatted(parentJobId, taskName)));

            Map<?, ?> output = (Map<?, ?>) taskFileStorage.readTaskExecutionOutput(agentTaskExecution.getOutput());

            return output.get("reply");
        }

        List<?> aggregatedOutput(long parentJobId, String taskName) {
            TaskExecution mapTaskExecution = taskExecutionService.getJobTaskExecutions(parentJobId)
                .stream()
                .filter(taskExecution -> Objects.equals(taskExecution.getName(), taskName))
                .findFirst()
                .orElseThrow(
                    () -> new AssertionError(
                        "Parent job %d has no task execution named '%s'".formatted(parentJobId, taskName)));

            return (List<?>) taskFileStorage.readTaskExecutionOutput(mapTaskExecution.getOutput());
        }

        Job.Status jobStatus(long jobId) {
            Job job = jobService.getJob(jobId);

            return job.getStatus();
        }

        Long parentTaskExecutionId(long jobId) {
            Job job = jobService.getJob(jobId);

            return job.getParentTaskExecutionId();
        }

        void resume(long childJobId) {
            Job childJob = jobService.getJob(childJobId);

            long taskExecutionResumeId = MapUtils.getRequiredLong(
                childJob.getMetadata(), MetadataConstants.TASK_EXECUTION_RESUME_ID);

            eventPublisher.publishEvent(
                new ResumeJobEvent(childJobId, taskExecutionResumeId, Map.of("approved", true)));
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

    private static final class RecordingChildJobPrincipalFactory implements ChildJobPrincipalFactory {

        private final List<Long> childJobIds;
        private final JobFacade jobFacade;

        private RecordingChildJobPrincipalFactory(JobFacade jobFacade, List<Long> childJobIds) {
            this.childJobIds = childJobIds;
            this.jobFacade = jobFacade;
        }

        @Override
        public long createChildJob(long parentJobId, JobParametersDTO jobParametersDTO) {
            long childJobId = jobFacade.createJob(jobParametersDTO);

            childJobIds.add(childJobId);

            return childJobId;
        }

        @Override
        public long createPrincipalLinkedJob(long referenceJobId, JobParametersDTO jobParametersDTO) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class StubCallableAiAgentDataSource implements CallableAiAgentDataSource {

        @Override
        public List<CallableAiAgentEntry> getCallableAgents(String search) {
            return List.of();
        }

        @Override
        public ResolvedAiAgent resolveAgent(String agentUuid, boolean editorEnvironment) {
            return new ResolvedAiAgent(agentUuid + WORKFLOW_SUFFIX, agentUuid, null);
        }
    }
}
