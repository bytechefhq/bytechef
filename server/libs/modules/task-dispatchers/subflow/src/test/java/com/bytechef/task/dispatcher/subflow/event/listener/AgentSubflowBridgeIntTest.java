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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.atlas.configuration.constant.WorkflowConstants;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.coordinator.event.JobStatusApplicationEvent;
import com.bytechef.atlas.coordinator.event.listener.ApplicationEventListener;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.dto.JobParametersDTO;
import com.bytechef.atlas.execution.facade.JobFacade;
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
import com.bytechef.atlas.worker.task.handler.TaskHandler;
import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.component.definition.ActionContext.Suspend;
import com.bytechef.error.ExecutionError;
import com.bytechef.evaluator.SpelEvaluator;
import com.bytechef.message.broker.memory.AsyncMessageBroker;
import com.bytechef.message.event.MessageEvent;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.job.sync.executor.JobSyncExecutor;
import com.bytechef.platform.workflow.execution.JobResumeId;
import com.bytechef.platform.workflow.execution.service.TaskStateService;
import com.bytechef.platform.workflow.task.dispatcher.subflow.ChildJobPrincipalFactory;
import com.bytechef.platform.workflow.task.dispatcher.subflow.PendingSubflowRequest;
import com.bytechef.platform.workflow.task.dispatcher.subflow.SubflowRequestConstants;
import com.bytechef.platform.workflow.task.dispatcher.test.annotation.TaskDispatcherIntTest;
import com.bytechef.task.dispatcher.suspend.SuspendTaskDispatcherPreSendProcessor;
import com.bytechef.task.dispatcher.suspend.completion.SuspendTaskCompletionHandler;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.constant.TenantConstants;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.env.Environment;
import org.springframework.core.task.TaskExecutor;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author Ivica Cardic
 */
@TaskDispatcherIntTest
class AgentSubflowBridgeIntTest {

    private static final String SUSPENDING_SUBFLOW = "agent-subflow-bridge-suspending";
    private static final String FAST_SUBFLOW = "agent-subflow-bridge-fast";
    private static final String FAILING_SUBFLOW = "agent-subflow-bridge-failing";
    private static final String WAIT_SUBFLOW = "agent-subflow-bridge-wait";

    @Autowired
    private Environment environment;

    @Autowired
    private TaskExecutor taskExecutor;

    @Autowired
    private TaskFileStorage taskFileStorage;

    @Autowired
    private WorkflowService workflowService;

    @Test
    void testSuspendingSubflowParksAgentThenResumesWithApprovalResult() {
        BridgeRun bridgeRun = new BridgeRun();

        Job subflowJob = bridgeRun.runSubflow(SUSPENDING_SUBFLOW);

        assertEquals(
            Job.Status.STOPPED, subflowJob.getStatus(),
            "A sub-workflow containing a suspending step must park (STOPPED), not silently complete");
        assertNotEquals(
            Job.Status.COMPLETED, subflowJob.getStatus(),
            "Silent-swallow regression: the suspending sub-workflow collapsed into COMPLETED");

        assertFalse(bridgeRun.wasAgentResumed(), "The agent must stay parked while the sub-workflow is suspended");

        bridgeRun.completeSubflowJobAndDispatchTerminalEvent(Job.Status.COMPLETED);

        assertTrue(bridgeRun.wasAgentResumed(), "The agent must resume once the sub-workflow terminates");
        assertEquals(
            bridgeRun.agentJobId(), bridgeRun.resumedJobId(),
            "The bridge must resume the agent job that requested the sub-workflow");

        Map<String, ?> resumeData = bridgeRun.resumeData();

        assertNotNull(resumeData, "The agent must be resumed with sub-workflow result data");
        assertNoSuspendPayloadIn(resumeData);
    }

    @Test
    void testTrivialFastSubflowStillResumesAgent() {
        BridgeRun bridgeRun = new BridgeRun();

        Job subflowJob = bridgeRun.runSubflow(FAST_SUBFLOW);

        assertEquals(Job.Status.COMPLETED, subflowJob.getStatus());
        assertTrue(bridgeRun.wasAgentResumed(), "A fast sub-workflow must still resume the agent");
        assertEquals(bridgeRun.agentJobId(), bridgeRun.resumedJobId());
        assertNotNull(bridgeRun.resumeData(), "The agent must be resumed with the sub-workflow result");

        assertNoSuspendPayloadIn(bridgeRun.resumeData());
    }

    @Test
    void testFailedSubflowResumesAgentWithError() {
        BridgeRun bridgeRun = new BridgeRun();

        Job subflowJob = bridgeRun.runSubflow(FAILING_SUBFLOW);

        assertEquals(Job.Status.FAILED, subflowJob.getStatus());
        assertTrue(bridgeRun.wasAgentResumed(), "A failed sub-workflow must still resume the agent");

        Map<String, ?> resumeData = bridgeRun.resumeData();

        assertNotNull(resumeData);
        assertTrue(
            resumeData.containsKey("error"),
            "The resumed agent must receive an LLM-readable error tool-result");

        Job agentJob = bridgeRun.getAgentJob();

        assertNotEquals(
            Job.Status.FAILED, agentJob.getStatus(), "A failed sub-workflow must not fail the agent job");
    }

    @Test
    void testFailedSubflowResumeIncludesExecutionErrorMessage() {
        BridgeRun bridgeRun = new BridgeRun();

        Job subflowJob = bridgeRun.runSubflow(FAST_SUBFLOW);

        bridgeRun.injectSubflowErrorAndDispatchFailedEvent("approval task threw", subflowJob.getId());

        Map<String, ?> resumeData = bridgeRun.resumeData();

        assertNotNull(resumeData);

        Object errorValue = resumeData.get("error");

        assertInstanceOf(String.class, errorValue);
        assertTrue(
            ((String) errorValue).contains("approval task threw"),
            "Expected the ExecutionError message in the failed-resume payload, got: " + errorValue);
    }

    @Test
    void testWaitSubflowParksAgent() {
        BridgeRun bridgeRun = new BridgeRun();

        Job subflowJob = bridgeRun.runSubflow(WAIT_SUBFLOW);

        assertEquals(
            Job.Status.STOPPED, subflowJob.getStatus(),
            "A sub-workflow containing a Wait step must park (STOPPED), not silently complete");
        assertFalse(
            bridgeRun.wasAgentResumed(), "The agent must stay parked while the Wait sub-workflow is suspended");

        bridgeRun.completeSubflowJobAndDispatchTerminalEvent(Job.Status.COMPLETED);

        assertTrue(bridgeRun.wasAgentResumed(), "The agent must resume once the Wait sub-workflow terminates");
    }

    @Test
    void testLauncherCreatesSubflowJobWhenAgentStops() {
        InMemoryTaskExecutionRepository taskExecutionRepository = new InMemoryTaskExecutionRepository();
        JobService jobService = new JobServiceImpl(
            new InMemoryJobRepository(taskExecutionRepository, new JsonMapper()));
        TaskExecutionService taskExecutionService = new TaskExecutionServiceImpl(taskExecutionRepository);

        String subflowWorkflowId = encodeWorkflowId(FAST_SUBFLOW);

        long agentJobId = persistStoppedJob(jobService, "agent-job");

        PendingSubflowRequest request = new PendingSubflowRequest(
            subflowWorkflowId, "newWorkflowCall", Map.of("amount", 5), false, PlatformType.AUTOMATION);

        TaskExecution agentTaskExecution = TaskExecution.builder()
            .jobId(agentJobId)
            .workflowTask(
                new WorkflowTask(
                    Map.of(WorkflowConstants.NAME, "callWorkflow", WorkflowConstants.TYPE, "aiAgent/v1/chat")))
            .build();

        agentTaskExecution.putMetadata(
            MetadataConstants.SUSPEND,
            new Suspend(Map.of(SubflowRequestConstants.PENDING_SUBFLOW, request), null));

        taskExecutionService.create(agentTaskExecution);

        AtomicReference<JobParametersDTO> launchedJobParameters = new AtomicReference<>();

        ChildJobPrincipalFactory childJobPrincipalFactory = new RecordingChildJobPrincipalFactory(
            jobService, launchedJobParameters);

        AgentSubflowLauncher launcher = new AgentSubflowLauncher(
            childJobPrincipalFactory, jobService, taskExecutionService);

        launcher.onApplicationEvent(new JobStatusApplicationEvent(agentJobId, Job.Status.STOPPED));

        JobParametersDTO jobParametersDTO = launchedJobParameters.get();

        assertNotNull(jobParametersDTO, "The launcher must create the sub-workflow job when the agent stops");
        assertEquals(subflowWorkflowId, jobParametersDTO.getWorkflowId());
        assertEquals(
            agentJobId, ((Number) jobParametersDTO.getMetadata()
                .get(SubflowRequestConstants.AGENT_JOB_ID)).longValue(),
            "The sub-workflow job must carry the agent job id so the resume listener can find it");

        Job reloadedAgentJob = jobService.getJob(agentJobId);

        assertTrue(
            reloadedAgentJob.getMetadata()
                .containsKey(SubflowRequestConstants.LAUNCHED_SUBFLOW_JOB_ID),
            "The launcher must record the launched sub-workflow job id for idempotency");

        launchedJobParameters.set(null);

        launcher.onApplicationEvent(new JobStatusApplicationEvent(agentJobId, Job.Status.STOPPED));

        assertNull(
            launchedJobParameters.get(), "A redelivered STOPPED event must not launch a second sub-workflow job");
    }

    @Test
    void testAgentCanBridgeTwoSequentialSubflowCalls() {
        InMemoryTaskExecutionRepository taskExecutionRepository = new InMemoryTaskExecutionRepository();
        JobService jobService = new JobServiceImpl(
            new InMemoryJobRepository(taskExecutionRepository, new JsonMapper()));
        TaskExecutionService taskExecutionService = new TaskExecutionServiceImpl(taskExecutionRepository);

        long agentJobId = persistStoppedJob(jobService, "agent-job");

        AtomicReference<JobParametersDTO> launchedJobParameters = new AtomicReference<>();

        ChildJobPrincipalFactory childJobPrincipalFactory = new RecordingChildJobPrincipalFactory(
            jobService, launchedJobParameters);

        AgentSubflowLauncher launcher = new AgentSubflowLauncher(
            childJobPrincipalFactory, jobService, taskExecutionService);
        AgentSubflowResumeListener resumeListener = new AgentSubflowResumeListener(
            new NoOpJobFacade(), jobService, taskExecutionService, taskFileStorage);

        suspendAgentForSubflow(taskExecutionService, agentJobId, encodeWorkflowId(FAST_SUBFLOW), "first");

        launcher.onApplicationEvent(new JobStatusApplicationEvent(agentJobId, Job.Status.STOPPED));

        assertNotNull(launchedJobParameters.get(), "The first bridged call must launch");

        long firstSubflowJobId = ((Number) jobService.getJob(agentJobId)
            .getMetadata()
            .get(SubflowRequestConstants.LAUNCHED_SUBFLOW_JOB_ID)).longValue();

        Job firstSubflowJob = jobService.getJob(firstSubflowJobId);

        firstSubflowJob.setStatus(Job.Status.COMPLETED);

        jobService.update(firstSubflowJob);

        resumeListener.onApplicationEvent(new JobStatusApplicationEvent(firstSubflowJobId, Job.Status.COMPLETED));

        assertFalse(
            jobService.getJob(agentJobId)
                .getMetadata()
                .containsKey(SubflowRequestConstants.LAUNCHED_SUBFLOW_JOB_ID),
            "Resuming the agent must clear the launched-sub-workflow marker");

        launchedJobParameters.set(null);

        suspendAgentForSubflow(taskExecutionService, agentJobId, encodeWorkflowId(FAST_SUBFLOW), "second");

        launcher.onApplicationEvent(new JobStatusApplicationEvent(agentJobId, Job.Status.STOPPED));

        assertNotNull(launchedJobParameters.get(), "The second bridged call on the same agent job must launch");
    }

    private static void suspendAgentForSubflow(
        TaskExecutionService taskExecutionService, long agentJobId, String subflowWorkflowId, String callName) {

        PendingSubflowRequest request = new PendingSubflowRequest(
            subflowWorkflowId, "newWorkflowCall", Map.of("call", callName), false, PlatformType.AUTOMATION);

        TaskExecution agentTaskExecution = TaskExecution.builder()
            .jobId(agentJobId)
            .workflowTask(
                new WorkflowTask(
                    Map.of(WorkflowConstants.NAME, "callWorkflow_" + callName, WorkflowConstants.TYPE,
                        "aiAgent/v1/chat")))
            .build();

        agentTaskExecution.putMetadata(
            MetadataConstants.SUSPEND,
            new Suspend(Map.of(SubflowRequestConstants.PENDING_SUBFLOW, request), null));

        taskExecutionService.create(agentTaskExecution);
    }

    private static final class NoOpJobFacade implements JobFacade {

        @Override
        public long createJob(JobParametersDTO jobParametersDTO) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void deleteJob(long id) {
            throw new UnsupportedOperationException();
        }

        @Override
        @Deprecated
        public void resumeApproval(long jobId, String uuid, boolean approved) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void resumeJob(long id) {
        }

        @Override
        public void resumeJob(long id, long taskExecutionId, Map<String, ?> data) {
        }

        @Override
        public void stopJob(long id) {
            throw new UnsupportedOperationException();
        }
    }

    private static long persistStoppedJob(JobService jobService, String workflowId) {
        Job job = new Job();

        job.setWorkflowId(workflowId);
        job.setStatus(Job.Status.STOPPED);

        Map<String, Object> metadata = new HashMap<>();

        metadata.put(MetadataConstants.TASK_EXECUTION_RESUME_ID, 1L);

        job.setMetadata(metadata);

        return Objects.requireNonNull(
            jobService.update(job)
                .getId());
    }

    private static ApplicationEventPublisher createEventPublisher(AsyncMessageBroker messageBroker) {
        return event -> {
            MessageEvent<?> messageEvent = (MessageEvent<?>) event;

            messageEvent.putMetadata(TenantConstants.CURRENT_TENANT_ID, TenantContext.getCurrentTenantId());

            messageBroker.send(messageEvent.getRoute(), messageEvent);
        };
    }

    private static String encodeWorkflowId(String workflowName) {
        return EncodingUtils.base64EncodeToString(workflowName.getBytes(StandardCharsets.UTF_8));
    }

    private static void assertNoSuspendPayloadIn(Map<String, ?> resumeData) {
        for (Map.Entry<String, ?> entry : resumeData.entrySet()) {
            assertFalse(
                entry.getValue() instanceof Suspend,
                "resumeData['" + entry.getKey() + "'] is a raw Suspend payload -- the LLM would reason on the "
                    + "suspend envelope instead of the real sub-workflow result (issue #5055 regression)");
        }
    }

    private final class BridgeRun {

        private final InMemoryTaskExecutionRepository taskExecutionRepository = new InMemoryTaskExecutionRepository();
        private final JobService jobService = new JobServiceImpl(
            new InMemoryJobRepository(taskExecutionRepository, new JsonMapper()));
        private final TaskExecutionService taskExecutionService = new TaskExecutionServiceImpl(
            taskExecutionRepository);
        private final TaskStateService taskStateService = new InMemoryTaskStateService();
        private final long agentJobId = persistStoppedJob(jobService, "agent-job");
        private final AgentSubflowResumeListener resumeListener;

        private Long resumedJobId;
        private Map<String, ?> resumeData;
        private Long subflowJobId;

        private BridgeRun() {
            resumeListener = new AgentSubflowResumeListener(
                new RecordingJobFacade(), jobService, taskExecutionService, taskFileStorage);
        }

        long agentJobId() {
            return agentJobId;
        }

        Job getAgentJob() {
            return jobService.getJob(agentJobId);
        }

        boolean wasAgentResumed() {
            return resumedJobId != null;
        }

        Long resumedJobId() {
            return resumedJobId;
        }

        Map<String, ?> resumeData() {
            return resumeData;
        }

        Job runSubflow(String subflowWorkflowName) {
            ContextService contextService = new ContextServiceImpl(new InMemoryContextRepository());
            AsyncMessageBroker messageBroker = new AsyncMessageBroker(environment);

            Map<String, TaskHandler<?>> taskHandlerMap = Map.of(
                "var/v1/set", taskExecution -> taskExecution.getParameters()
                    .get("value"),
                "var/v1/fail", taskExecution -> {
                    throw new IllegalStateException("Sub-workflow task failed on purpose");
                },
                "suspend/v1", taskExecution -> new com.bytechef.component.definition.ActionContext.Suspend(
                    Map.of(), null));

            JobSyncExecutor jobSyncExecutor = new JobSyncExecutor(
                contextService, SpelEvaluator.create(), jobService, -1, messageBroker,
                List.<ApplicationEventListener>of(resumeListener),
                List.of((taskCompletionHandler, taskDispatcher) -> new SuspendTaskCompletionHandler(
                    contextService, createEventPublisher(messageBroker), jobService, taskExecutionService,
                    taskFileStorage, taskStateService)),
                List.of(),
                List.of(new SuspendTaskDispatcherPreSendProcessor(jobService, taskStateService, null)),
                List.of(),
                taskExecutionService, taskExecutor, taskHandlerMap::get, taskFileStorage, -1, workflowService);

            Job job = jobSyncExecutor.execute(
                new JobParametersDTO(
                    encodeWorkflowId(subflowWorkflowName), Map.of("amount", 5),
                    Map.of(SubflowRequestConstants.AGENT_JOB_ID, agentJobId)),
                false);

            subflowJobId = job.getId();

            return job;
        }

        void completeSubflowJobAndDispatchTerminalEvent(Job.Status terminalStatus) {
            long jobId = Objects.requireNonNull(subflowJobId);

            Job subflowJob = jobService.getJob(jobId);

            subflowJob.setStatus(terminalStatus);

            jobService.update(subflowJob);

            resumeListener.onApplicationEvent(new JobStatusApplicationEvent(jobId, terminalStatus));
        }

        void injectSubflowErrorAndDispatchFailedEvent(String errorMessage, long subflowJobIdToFail) {
            Job subflowJob = jobService.getJob(subflowJobIdToFail);

            subflowJob.setStatus(Job.Status.FAILED);
            subflowJob.setError(new ExecutionError(errorMessage, List.of()));

            jobService.update(subflowJob);

            resumeListener.onApplicationEvent(
                new JobStatusApplicationEvent(subflowJobIdToFail, Job.Status.FAILED));
        }

        private final class RecordingJobFacade implements JobFacade {

            @Override
            public long createJob(JobParametersDTO jobParametersDTO) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void deleteJob(long id) {
                throw new UnsupportedOperationException();
            }

            @Override
            @Deprecated
            public void resumeApproval(long jobId, String uuid, boolean approved) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void resumeJob(long id) {
                resumeJob(id, 0L, null);
            }

            @Override
            public void resumeJob(long id, long taskExecutionId, Map<String, ?> data) {
                resumedJobId = id;
                resumeData = data;
            }

            @Override
            public void stopJob(long id) {
                throw new UnsupportedOperationException();
            }
        }
    }

    private static final class InMemoryTaskStateService implements TaskStateService {

        private final Map<JobResumeId, Object> store = new HashMap<>();

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

    private static final class RecordingChildJobPrincipalFactory implements ChildJobPrincipalFactory {

        private final JobService jobService;
        private final AtomicReference<JobParametersDTO> launchedJobParameters;

        private RecordingChildJobPrincipalFactory(
            JobService jobService, AtomicReference<JobParametersDTO> launchedJobParameters) {

            this.jobService = jobService;
            this.launchedJobParameters = launchedJobParameters;
        }

        @Override
        public long createChildJob(long parentJobId, JobParametersDTO jobParametersDTO) {
            throw new UnsupportedOperationException();
        }

        @Override
        public long createPrincipalLinkedJob(long referenceJobId, JobParametersDTO jobParametersDTO) {
            launchedJobParameters.set(jobParametersDTO);

            Job subflowJob = new Job();

            subflowJob.setWorkflowId(jobParametersDTO.getWorkflowId());
            subflowJob.setStatus(Job.Status.CREATED);
            subflowJob.setMetadata(jobParametersDTO.getMetadata());

            return Objects.requireNonNull(
                jobService.update(subflowJob)
                    .getId());
        }
    }
}
