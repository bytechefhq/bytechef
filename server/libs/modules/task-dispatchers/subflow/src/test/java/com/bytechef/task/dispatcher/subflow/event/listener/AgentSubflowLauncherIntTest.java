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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import com.bytechef.platform.workflow.execution.JobResumeId;
import com.bytechef.platform.workflow.execution.service.TaskStateService;
import com.bytechef.platform.workflow.task.dispatcher.subflow.ChildJobPrincipalFactory;
import com.bytechef.platform.workflow.task.dispatcher.subflow.PendingSubflowRequest;
import com.bytechef.platform.workflow.task.dispatcher.subflow.SubflowRequestConstants;
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
import java.util.concurrent.ConcurrentHashMap;
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
class AgentSubflowLauncherIntTest {

    private static final String AGENT_WORKFLOW = "agent-subflow-bridge-agent";
    private static final String AGENT_TWO_CALLS_WORKFLOW = "agent-subflow-bridge-agent-two-calls";
    private static final String FAILING_SUBFLOW = "agent-subflow-bridge-failing";
    private static final String FAST_SUBFLOW = "agent-subflow-bridge-fast";
    private static final Duration JOB_STATUS_TIMEOUT = Duration.ofSeconds(20);
    private static final int MAX_DEPTH = 3;
    private static final String MISSING_SUBFLOW = "agent-subflow-bridge-missing";
    private static final String NESTING_SUBFLOW = "agent-subflow-bridge-nesting";
    private static final String SUSPENDING_SUBFLOW = "agent-subflow-bridge-suspending";

    @Autowired
    private Environment environment;

    @Autowired
    private TaskExecutor taskExecutor;

    @Autowired
    private TaskFileStorage taskFileStorage;

    @Autowired
    private WorkflowService workflowService;

    @Test
    void testAgentToolSuspendLaunchesSubflowAndResumesAgentWithItsOutput() throws InterruptedException {
        Rig rig = new Rig();

        long agentJobId = rig.startAgent(AGENT_WORKFLOW, Map.of("subflow", FAST_SUBFLOW), Map.of());

        rig.awaitJobStatus(agentJobId, Job.Status.COMPLETED);

        Map<String, ?> toolResult = MapUtils.getMap(rig.awaitJobOutputs(agentJobId), "toolResult");

        assertEquals("completed", toolResult.get("status"));
        assertEquals(Map.of("result", "done"), toolResult.get("result"));
        assertEquals(1, rig.launchedSubflowJobIds.size());
    }

    @Test
    void testFailedSubflowResumesAgentWithTheFailedTaskErrorMessage() throws InterruptedException {
        Rig rig = new Rig();

        long agentJobId = rig.startAgent(AGENT_WORKFLOW, Map.of("subflow", FAILING_SUBFLOW), Map.of());

        rig.awaitJobStatus(agentJobId, Job.Status.COMPLETED);

        Map<String, ?> toolResult = MapUtils.getMap(rig.awaitJobOutputs(agentJobId), "toolResult");

        assertEquals("failed", toolResult.get("status"));
        assertTrue(
            ((String) toolResult.get("error")).contains("Sub-workflow task failed on purpose"),
            "Expected the failed task's message in the tool result, got: " + toolResult);
    }

    @Test
    void testSuspendedSubflowKeepsAgentStoppedUntilTheSubflowResumes() throws InterruptedException {
        Rig rig = new Rig();

        long agentJobId = rig.startAgent(AGENT_WORKFLOW, Map.of("subflow", SUSPENDING_SUBFLOW), Map.of());

        long subflowJobId = rig.awaitLaunchedSubflowJob(0);

        rig.awaitJobStatus(subflowJobId, Job.Status.STOPPED);

        assertEquals(Job.Status.STOPPED, rig.jobService.getJob(agentJobId)
            .getStatus(), "The agent must wait while its sub-workflow is suspended");

        rig.resumeJob(subflowJobId, Map.of("decision", "approved"));

        rig.awaitJobStatus(agentJobId, Job.Status.COMPLETED);

        Map<String, ?> toolResult = MapUtils.getMap(rig.awaitJobOutputs(agentJobId), "toolResult");

        assertEquals("completed", toolResult.get("status"));
    }

    @Test
    void testAgentBridgesTwoSequentialSubflowCalls() throws InterruptedException {
        Rig rig = new Rig();

        long agentJobId = rig.startAgent(AGENT_TWO_CALLS_WORKFLOW, Map.of(), Map.of());

        rig.awaitJobStatus(agentJobId, Job.Status.COMPLETED);

        Map<String, ?> outputs = rig.awaitJobOutputs(agentJobId);

        Map<String, ?> firstToolResult = MapUtils.getRequiredMap(outputs, "firstToolResult");
        Map<String, ?> secondToolResult = MapUtils.getRequiredMap(outputs, "secondToolResult");

        assertEquals("completed", firstToolResult.get("status"));
        assertEquals("completed", secondToolResult.get("status"));
        assertEquals(2, rig.launchedSubflowJobIds.size());
    }

    @Test
    void testCallBeyondTheMaximumDepthResumesAgentWithAnError() throws InterruptedException {
        Rig rig = new Rig();

        long agentJobId = rig.startAgent(
            AGENT_WORKFLOW, Map.of("subflow", FAST_SUBFLOW), Map.of(SubflowRequestConstants.SUBFLOW_DEPTH, MAX_DEPTH));

        rig.awaitJobStatus(agentJobId, Job.Status.COMPLETED);

        Map<String, ?> toolResult = MapUtils.getMap(rig.awaitJobOutputs(agentJobId), "toolResult");

        assertEquals("failed", toolResult.get("status"));
        assertEquals(AgentSubflowLauncher.tooDeepError(MAX_DEPTH), toolResult.get("error"));
        assertTrue(rig.launchedSubflowJobIds.isEmpty());
    }

    @Test
    void testCallJustBelowTheMaximumDepthLaunchesTheSubflowOneLevelDeeper() throws InterruptedException {
        Rig rig = new Rig();

        long agentJobId = rig.startAgent(
            AGENT_WORKFLOW, Map.of("subflow", FAST_SUBFLOW),
            Map.of(SubflowRequestConstants.SUBFLOW_DEPTH, MAX_DEPTH - 1));

        rig.awaitJobStatus(agentJobId, Job.Status.COMPLETED);

        Map<String, ?> toolResult = MapUtils.getMap(rig.awaitJobOutputs(agentJobId), "toolResult");

        assertEquals("completed", toolResult.get("status"));

        Map<String, ?> subflowMetadata = rig.jobService.getJob(rig.awaitLaunchedSubflowJob(0))
            .getMetadata();

        assertEquals(MAX_DEPTH, MapUtils.getInteger(subflowMetadata, SubflowRequestConstants.SUBFLOW_DEPTH));
    }

    @Test
    void testRepeatedSuspendEventLaunchesTheSubflowOnce() throws InterruptedException {
        Rig rig = new Rig();

        long agentJobId = rig.startAgent(AGENT_WORKFLOW, Map.of("subflow", SUSPENDING_SUBFLOW), Map.of());

        long subflowJobId = rig.awaitLaunchedSubflowJob(0);

        rig.awaitJobStatus(subflowJobId, Job.Status.STOPPED);

        rig.taskCoordinator.onApplicationEvent(JobStatusApplicationEvent.suspended(agentJobId));

        assertEquals(List.of(subflowJobId), rig.launchedSubflowJobIds);

        rig.resumeJob(subflowJobId, Map.of("decision", "approved"));

        rig.awaitJobStatus(agentJobId, Job.Status.COMPLETED);

        assertEquals(List.of(subflowJobId), rig.launchedSubflowJobIds);
    }

    @Test
    void testSubflowThatCannotBeStartedResumesAgentWithAnError() throws InterruptedException {
        Rig rig = new Rig();

        long agentJobId = rig.startAgent(AGENT_WORKFLOW, Map.of("subflow", MISSING_SUBFLOW), Map.of());

        rig.awaitJobStatus(agentJobId, Job.Status.COMPLETED);

        Map<String, ?> toolResult = MapUtils.getMap(rig.awaitJobOutputs(agentJobId), "toolResult");

        assertEquals("failed", toolResult.get("status"));
        assertTrue(
            ((String) toolResult.get("error")).contains("no principal found"),
            "Expected the launch failure in the tool result, got: " + toolResult);
    }

    @Test
    void testGrandchildSubflowDoesNotInheritTheAgentLink() throws InterruptedException {
        Rig rig = new Rig();

        long agentJobId = rig.startAgent(AGENT_WORKFLOW, Map.of("subflow", NESTING_SUBFLOW), Map.of());

        rig.awaitJobStatus(agentJobId, Job.Status.COMPLETED);

        Long grandchildJobId = rig.grandchildJobIdReference.get();

        assertNotNull(grandchildJobId);

        Map<String, ?> grandchildMetadata = rig.jobService.getJob(grandchildJobId)
            .getMetadata();

        assertFalse(grandchildMetadata.containsKey(SubflowRequestConstants.AGENT_JOB_ID));
        assertFalse(grandchildMetadata.containsKey(SubflowRequestConstants.AGENT_JOB_RESUME_ID));
        assertFalse(grandchildMetadata.containsKey(SubflowRequestConstants.LAUNCHED_SUBFLOW_RESUME_ID));
        assertEquals(1, MapUtils.getInteger(grandchildMetadata, SubflowRequestConstants.SUBFLOW_DEPTH));

        Map<String, ?> toolResult = MapUtils.getMap(rig.awaitJobOutputs(agentJobId), "toolResult");

        assertEquals(Map.of("result", "nesting done"), toolResult.get("result"));
    }

    private static Object callSubflowAsAgentTool(TaskExecution taskExecution) {
        Object resumeData = taskExecution.getMetadata()
            .get(MetadataConstants.RESUME_DATA);

        if (resumeData != null) {
            return resumeData;
        }

        String subflowName = MapUtils.getRequiredString(taskExecution.getParameters(), "subflow");

        return new ActionContext.Suspend(
            Map.of(
                SubflowRequestConstants.PENDING_SUBFLOW,
                new PendingSubflowRequest(encodeWorkflowId(subflowName), "newWorkflowCall", Map.of("amount", 5))),
            null);
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

    private final class Rig {

        private final AtomicReference<String> firstErrorReference = new AtomicReference<>();
        private final AtomicReference<Long> grandchildJobIdReference = new AtomicReference<>();
        private final JobFacade jobFacade;
        private final JobService jobService;
        private final List<Long> launchedSubflowJobIds = new CopyOnWriteArrayList<>();
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

            ChildJobPrincipalFactory childJobPrincipalFactory = new RecordingChildJobPrincipalFactory(
                jobFacade, grandchildJobIdReference, launchedSubflowJobIds);
            SubflowResolver subflowResolver =
                (workflowUuid, triggerName, editorEnvironment) -> new SubflowResolver.Subflow(
                    encodeWorkflowId(workflowUuid), "newWorkflowCall");

            Map<String, TaskHandler<?>> taskHandlerMap = Map.of(
                "agentTool/v1", AgentSubflowLauncherIntTest::callSubflowAsAgentTool,
                "suspend/v1", AgentSubflowLauncherIntTest::suspendUntilResumed,
                "var/v1/fail", taskExecution -> {
                    throw new IllegalStateException("Sub-workflow task failed on purpose");
                },
                "var/v1/set", taskExecution -> taskExecution.getParameters()
                    .get("value"));

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
                new AgentSubflowLauncher(childJobPrincipalFactory, jobFacade, jobService, MAX_DEPTH, taskStateService),
                new AgentSubflowResumeListener(jobFacade, jobService, taskExecutionService, taskFileStorage),
                new SubflowJobStatusEventListener(
                    SpelEvaluator.create(), eventPublisher, jobService, taskExecutionService, taskFileStorage),
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
                TaskCoordinatorMessageRoute.JOB_STOP_EVENTS,
                event -> taskCoordinator.onStopJobEvent((StopJobEvent) event));
            receive(
                TaskCoordinatorMessageRoute.TASK_EXECUTION_COMPLETE_EVENTS,
                event -> taskCoordinator.onTaskExecutionCompleteEvent((TaskExecutionCompleteEvent) event));
        }

        long startAgent(String workflowName, Map<String, ?> inputs, Map<String, ?> metadata) {
            return jobFacade.createJob(
                new JobParametersDTO(encodeWorkflowId(workflowName), inputs, metadata));
        }

        long awaitLaunchedSubflowJob(int index) throws InterruptedException {
            Instant deadline = Instant.now()
                .plus(JOB_STATUS_TIMEOUT);

            while (launchedSubflowJobIds.size() <= index) {
                if (Instant.now()
                    .isAfter(deadline)) {

                    throw new AssertionError(
                        "The launcher never started sub-workflow #" + index + describeFirstError());
                }

                Thread.sleep(10);
            }

            return launchedSubflowJobIds.get(index);
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
                FileEntry outputs = jobService.getJob(jobId)
                    .getOutputs();

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

        void resumeJob(long jobId, Map<String, ?> data) {
            jobFacade.resumeJob(
                jobId,
                MapUtils.getRequiredLong(
                    jobService.getJob(jobId)
                        .getMetadata(),
                    MetadataConstants.TASK_EXECUTION_RESUME_ID),
                data);
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
    }

    private static final class NonPersistentHandledFlagTaskExecutionRepository extends InMemoryTaskExecutionRepository {

        @Override
        public TaskExecution save(TaskExecution taskExecution) {
            boolean handled = taskExecution.isHandled();

            taskExecution.setHandled(false);

            try {
                return super.save(taskExecution);
            } finally {
                taskExecution.setHandled(handled);
            }
        }
    }

    private static final class RecordingChildJobPrincipalFactory implements ChildJobPrincipalFactory {

        private final AtomicReference<Long> grandchildJobIdReference;
        private final JobFacade jobFacade;
        private final List<Long> launchedSubflowJobIds;

        private RecordingChildJobPrincipalFactory(
            JobFacade jobFacade, AtomicReference<Long> grandchildJobIdReference, List<Long> launchedSubflowJobIds) {

            this.grandchildJobIdReference = grandchildJobIdReference;
            this.jobFacade = jobFacade;
            this.launchedSubflowJobIds = launchedSubflowJobIds;
        }

        @Override
        public long createChildJob(long parentJobId, JobParametersDTO jobParametersDTO) {
            long childJobId = jobFacade.createJob(jobParametersDTO);

            grandchildJobIdReference.set(childJobId);

            return childJobId;
        }

        @Override
        public long createPrincipalLinkedJob(long referenceJobId, JobParametersDTO jobParametersDTO) {
            if (encodeWorkflowId(MISSING_SUBFLOW).equals(jobParametersDTO.getWorkflowId())) {
                throw new IllegalStateException("no principal found for reference job " + referenceJobId);
            }

            long subflowJobId = jobFacade.createJob(jobParametersDTO);

            launchedSubflowJobIds.add(subflowJobId);

            return subflowJobId;
        }
    }

    private static final class InMemoryTaskStateService implements TaskStateService {

        private final Map<JobResumeId, Object> store = new ConcurrentHashMap<>();

        @Override
        public void delete(JobResumeId jobResumeId) {
            store.remove(jobResumeId);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> Optional<T> fetchValue(JobResumeId jobResumeId) {
            return Optional.ofNullable((T) store.get(jobResumeId));
        }

        @Override
        public void save(JobResumeId jobResumeId, Object value) {
            store.put(jobResumeId, value);
        }
    }
}
