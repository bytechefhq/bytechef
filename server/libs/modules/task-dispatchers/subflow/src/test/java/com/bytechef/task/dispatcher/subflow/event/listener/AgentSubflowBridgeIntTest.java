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
 * Wide unit test for the agent-tool durable sub-workflow bridge. Encodes the issue #5055 invariant at the bridge
 * boundary: a suspending sub-workflow must never collapse into an ordinary completed agent tool result.
 * <p>
 * Wiring (read this before assuming end-to-end coverage):
 * <ul>
 * <li>The bridge listeners ({@link AgentSubflowLauncher}, {@link AgentSubflowResumeListener}) are real, constructed
 * here.</li>
 * <li>The sub-workflow runs through an in-process {@link JobSyncExecutor} backed by an in-memory broker
 * ({@link AsyncMessageBroker}) and in-memory repositories. The real {@link SuspendTaskDispatcher} is registered, so a
 * sub-workflow {@code suspend/v1} step does park its job ({@code STOPPED}).</li>
 * <li>{@link JobFacade} and {@link ChildJobPrincipalFactory} are mocked via inner-class recorders — the production
 * {@code PrincipalJobFacadeImpl.createPrincipalLinkedJob} and {@code TaskCoordinator.onResumeJobEvent} paths are NOT
 * exercised here.</li>
 * <li>Terminal-state propagation in some scenarios is simulated by directly transitioning the sub-workflow job and
 * synthesizing a {@code JobStatusApplicationEvent}, rather than driving the real {@code JobResumeFacade} flow.</li>
 * </ul>
 * <p>
 * What this test covers: the bridge listeners' contract under realistic event-shape inputs — the launcher's
 * deferred-launch reaction to an agent job reaching {@code STOPPED}, the resume listener's response to a terminal
 * sub-workflow, and the parking invariant for suspending and wait-style sub-workflow steps under
 * {@link SuspendTaskDispatcher}.
 * <p>
 * What this test does NOT cover (handled separately or a known gap):
 * <ul>
 * <li>The real Testcontainers integration (real DB, real broker, real coordinator, real {@code JobResumeFacade}) — the
 * spec calls for this; it remains a follow-up.</li>
 * <li>The full agent-component re-entry ({@code AiAgentChatAction.resumePerform}) — exercised separately by
 * {@code AiAgentChatActionResumeIntTest}.</li>
 * <li>{@code PrincipalJobFacadeImpl.createPrincipalLinkedJob} — exercised by its own unit test.</li>
 * <li>The {@code MapUtils.get → Jackson convertValue} round-trip of {@code PendingSubflowRequest} through a real
 * persisted {@code TaskState} — exercised by {@code PendingSubflowRequestTest} in-memory only.</li>
 * </ul>
 *
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

    /**
     * Scenario 1 — a suspending sub-workflow (Request Approval style) parks instead of completing, then resuming the
     * sub-workflow resumes the agent with the real result.
     * <p>
     * This is the literal #5055 regression: with the real {@link SuspendTaskDispatcher}, a sub-workflow
     * {@code suspend/v1} step parks its job ({@code STOPPED}). The bridge's resume listener must NOT fire while the
     * sub-workflow is parked, so the agent stays alive. Once the sub-workflow reaches a terminal {@code COMPLETED}
     * state, the bridge resumes the agent with the sub-workflow result.
     */
    @Test
    void testSuspendingSubflowParksAgentThenResumesWithApprovalResult() {
        BridgeRun bridgeRun = new BridgeRun();

        Job subflowJob = bridgeRun.runSubflow(SUSPENDING_SUBFLOW);

        // The suspending sub-workflow must park -- it must NOT collapse into COMPLETED.

        assertEquals(
            Job.Status.STOPPED, subflowJob.getStatus(),
            "A sub-workflow containing a suspending step must park (STOPPED), not silently complete");
        assertNotEquals(
            Job.Status.COMPLETED, subflowJob.getStatus(),
            "Silent-swallow regression: the suspending sub-workflow collapsed into COMPLETED");

        // While the sub-workflow is parked the bridge must not have resumed the agent.

        assertFalse(bridgeRun.wasAgentResumed(), "The agent must stay parked while the sub-workflow is suspended");

        // The sub-workflow eventually reaches a terminal COMPLETED state -- the real AgentSubflowResumeListener then
        // resumes the agent with the sub-workflow result.

        bridgeRun.completeSubflowJobAndDispatchTerminalEvent(Job.Status.COMPLETED);

        assertTrue(bridgeRun.wasAgentResumed(), "The agent must resume once the sub-workflow terminates");
        assertEquals(
            bridgeRun.agentJobId(), bridgeRun.resumedJobId(),
            "The bridge must resume the agent job that requested the sub-workflow");

        // I2: the central #5055 regression assertion -- the LLM must see concrete sub-workflow output, NOT the raw
        // Suspend payload leaking through. The pre-fix bug shipped a Suspend instance as the tool result; this
        // assertion ensures that any future regression that re-introduces the leak is caught here.

        Map<String, ?> resumeData = bridgeRun.resumeData();

        assertNotNull(resumeData, "The agent must be resumed with sub-workflow result data");
        assertNoSuspendPayloadIn(resumeData);
    }

    /**
     * Scenario 2 — a trivial fast sub-workflow with no suspending step still resumes the agent.
     * <p>
     * Exercises the startup-race window: even when the sub-workflow completes quickly through the real coordinator, the
     * registered {@link AgentSubflowResumeListener} fires on the terminal {@code JobStatusApplicationEvent} and resumes
     * the agent with the sub-workflow output.
     */
    @Test
    void testTrivialFastSubflowStillResumesAgent() {
        BridgeRun bridgeRun = new BridgeRun();

        Job subflowJob = bridgeRun.runSubflow(FAST_SUBFLOW);

        assertEquals(Job.Status.COMPLETED, subflowJob.getStatus());
        assertTrue(bridgeRun.wasAgentResumed(), "A fast sub-workflow must still resume the agent");
        assertEquals(bridgeRun.agentJobId(), bridgeRun.resumedJobId());
        assertNotNull(bridgeRun.resumeData(), "The agent must be resumed with the sub-workflow result");

        // I2 regression assertion -- same as the suspending scenario, also applied to the no-suspend code path.

        assertNoSuspendPayloadIn(bridgeRun.resumeData());
    }

    /**
     * Scenario 3 — a FAILED sub-workflow resumes the agent with an error tool-result; the agent job is not failed.
     */
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

    /**
     * S2: when {@code Job.getError()} is populated (production wiring sets this on a failing job), the error payload
     * must include the underlying failure cause -- not just a generic "Sub-workflow $id failed" message that gives the
     * LLM nothing to reason about. The in-process {@link JobSyncExecutor} used by the other scenarios does not always
     * propagate the task exception into {@code Job.error}, so this test seeds the error explicitly and re-fires the
     * terminal event, exercising only the resume listener's payload-building path.
     */
    @Test
    void testFailedSubflowResumeIncludesExecutionErrorMessage() {
        BridgeRun bridgeRun = new BridgeRun();

        // Run a normal subflow first so the bridge is wired and the agent job exists in the run's job-service.

        Job subflowJob = bridgeRun.runSubflow(FAST_SUBFLOW);

        // Seed the failure cause that production wiring would have set on a FAILED job, then re-dispatch the
        // terminal event with FAILED status -- exercising the S2 message-propagation path through the bridge.

        Long subflowJobId = Objects.requireNonNull(subflowJob.getId(), "subflow job id");

        bridgeRun.injectSubflowErrorAndDispatchFailedEvent("approval task threw", subflowJobId);

        Map<String, ?> resumeData = bridgeRun.resumeData();

        assertNotNull(resumeData);

        Object errorValue = resumeData.get("error");

        assertInstanceOf(String.class, errorValue);
        assertTrue(
            ((String) errorValue).contains("approval task threw"),
            "Expected the ExecutionError message in the failed-resume payload, got: " + errorValue);
    }

    /**
     * Scenario 4 — a Wait-style suspending sub-workflow honors the same parking invariant as Request Approval.
     */
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

    /**
     * Scenario 5 — the launcher's deferred launch: an agent job reaching {@code STOPPED} with a pending sub-workflow
     * request triggers {@link AgentSubflowLauncher} to create the sub-workflow as a top-level job, idempotently.
     */
    @Test
    void testLauncherCreatesSubflowJobWhenAgentStops() {
        InMemoryTaskExecutionRepository taskExecutionRepository = new InMemoryTaskExecutionRepository();
        JobService jobService = new JobServiceImpl(
            new InMemoryJobRepository(taskExecutionRepository, new JsonMapper()));
        TaskExecutionService taskExecutionService = new TaskExecutionServiceImpl(taskExecutionRepository);

        String subflowWorkflowId = encodeWorkflowId(FAST_SUBFLOW);

        // A real agent job, STOPPED, whose last task execution carries a PendingSubflowRequest in its SUSPEND
        // metadata -- exactly the durable state the Call Workflow tool produces.

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

        // A redelivered STOPPED event must be idempotent.

        launchedJobParameters.set(null);

        launcher.onApplicationEvent(new JobStatusApplicationEvent(agentJobId, Job.Status.STOPPED));

        assertNull(
            launchedJobParameters.get(), "A redelivered STOPPED event must not launch a second sub-workflow job");
    }

    private static long persistStoppedJob(JobService jobService, String workflowId) {
        Job job = new Job();

        job.setWorkflowId(workflowId);
        job.setStatus(Job.Status.STOPPED);

        Map<String, Object> metadata = new HashMap<>();

        // Simulate what production does when the agent job suspends via ActionContext: the
        // SuspendTaskCompletionHandler sets TASK_EXECUTION_RESUME_ID on the job metadata so that
        // AgentSubflowResumeListener can pass it to the 3-arg resumeJob overload.
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

    /**
     * I2: encodes the central #5055 invariant -- the resumed agent must receive concrete data, not a raw
     * {@code Suspend} payload leaking through. A future regression that re-introduces the bug (e.g., extracting
     * {@code output} incorrectly so the {@code Suspend} envelope makes it into {@code resumeData}) would land here as a
     * failing assertion.
     */
    private static void assertNoSuspendPayloadIn(Map<String, ?> resumeData) {
        for (Map.Entry<String, ?> entry : resumeData.entrySet()) {
            assertFalse(
                entry.getValue() instanceof Suspend,
                "resumeData['" + entry.getKey() + "'] is a raw Suspend payload -- the LLM would reason on the "
                    + "suspend envelope instead of the real sub-workflow result (issue #5055 regression)");
        }
    }

    /**
     * One bridge scenario: a real persisted agent job and a sub-workflow job, both sharing a single {@link JobService}
     * (as in production), so the real {@link AgentSubflowResumeListener} can resolve both. The resume listener is
     * registered as a coordinator {@code ApplicationEventListener}, so a terminal sub-workflow job genuinely resumes
     * the agent through the bridge.
     */
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

        /**
         * Runs the sub-workflow as a real coordinator-executed top-level job, sharing this run's {@link JobService} and
         * {@link TaskExecutionService} with the bridge resume listener.
         */
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
                List.of(new SuspendTaskDispatcherPreSendProcessor(jobService, taskStateService)),
                List.of(),
                taskExecutionService, taskExecutor, taskHandlerMap::get, taskFileStorage, -1,
                workflowService);

            // The sub-workflow job carries the agent job id, exactly as AgentSubflowLauncher would set it.

            Job job = jobSyncExecutor.execute(
                new JobParametersDTO(
                    encodeWorkflowId(subflowWorkflowName), Map.of("amount", 5),
                    Map.of(SubflowRequestConstants.AGENT_JOB_ID, agentJobId)),
                false);

            subflowJobId = job.getId();

            return job;
        }

        /**
         * Marks a parked sub-workflow job as reaching a terminal state and dispatches the matching terminal
         * {@code JobStatusApplicationEvent} to the real {@link AgentSubflowResumeListener} — mirroring what the
         * coordinator does when the sub-workflow resumes and finishes on its own.
         */
        void completeSubflowJobAndDispatchTerminalEvent(Job.Status terminalStatus) {
            long jobId = Objects.requireNonNull(subflowJobId);

            Job subflowJob = jobService.getJob(jobId);

            subflowJob.setStatus(terminalStatus);

            jobService.update(subflowJob);

            resumeListener.onApplicationEvent(new JobStatusApplicationEvent(jobId, terminalStatus));
        }

        /**
         * Mimics production wiring that sets {@code Job.error} on a failing job. The in-process executor used by this
         * test harness does not always populate {@code error} from the underlying task exception, so the S2 assertion
         * (error message surfaces to the LLM payload) needs explicit seeding.
         */
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

    /**
     * Minimal in-memory {@link TaskStateService} for tests that need suspend/resume wiring without a real JDBC backend.
     */
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

    /**
     * A {@link ChildJobPrincipalFactory} that records the launched job parameters and persists the sub-workflow job,
     * standing in for the DB-backed production factory.
     */
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
