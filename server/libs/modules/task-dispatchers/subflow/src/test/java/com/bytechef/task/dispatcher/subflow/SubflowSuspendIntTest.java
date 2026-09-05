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

package com.bytechef.task.dispatcher.subflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.coordinator.event.JobStatusApplicationEvent;
import com.bytechef.atlas.coordinator.event.listener.ApplicationEventListener;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcherResolverFactory;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.dto.JobParametersDTO;
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
import com.bytechef.component.definition.ActionContext;
import com.bytechef.evaluator.SpelEvaluator;
import com.bytechef.message.broker.memory.AsyncMessageBroker;
import com.bytechef.message.event.MessageEvent;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.job.sync.executor.JobSyncExecutor;
import com.bytechef.platform.workflow.execution.JobResumeId;
import com.bytechef.platform.workflow.execution.service.TaskStateService;
import com.bytechef.platform.workflow.task.dispatcher.subflow.ChildJobPrincipalFactory;
import com.bytechef.platform.workflow.task.dispatcher.subflow.SubflowResolver;
import com.bytechef.platform.workflow.task.dispatcher.test.annotation.TaskDispatcherIntTest;
import com.bytechef.task.dispatcher.subflow.event.listener.SubflowJobStatusEventListener;
import com.bytechef.task.dispatcher.suspend.SuspendTaskDispatcherPreSendProcessor;
import com.bytechef.task.dispatcher.suspend.completion.SuspendTaskCompletionHandler;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.constant.TenantConstants;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
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
class SubflowSuspendIntTest {

    private static final String PARENT_WORKFLOW = "subflow-parent-of-suspending-child";
    private static final String CHILD_WORKFLOW = "subflow-suspending-child";
    private static final Duration CHILD_LAUNCH_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration JOB_STATUS_TIMEOUT = Duration.ofSeconds(10);
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
    void testSuspendingChildLeavesParentRunningAndParentCompletesAfterChild() throws InterruptedException {
        Rig rig = new Rig();

        long parentJobId = rig.startParent();
        long childJobId = rig.awaitChildLaunched();

        Job childJob = rig.awaitJobStatus(childJobId, Job.Status.STOPPED);

        assertNotNull(childJob.getMetadata(MetadataConstants.JOB_RESUME_ID));

        rig.assertParentStaysStarted(parentJobId);

        rig.completeChild(childJobId, Map.of("result", "the human approved"));

        Job parentJob = rig.awaitJobStatus(parentJobId, Job.Status.COMPLETED);

        assertEquals(
            "the human approved",
            taskFileStorage.readJobOutputs(parentJob.getOutputs())
                .get("childResult"));
    }

    private final class Rig {

        private final InMemoryTaskExecutionRepository taskExecutionRepository = new InMemoryTaskExecutionRepository();
        private final JobService jobService = new JobServiceImpl(
            new InMemoryJobRepository(taskExecutionRepository, new JsonMapper()));
        private final TaskExecutionService taskExecutionService = new TaskExecutionServiceImpl(
            taskExecutionRepository);
        private final AtomicReference<JobSyncExecutor> jobSyncExecutorReference = new AtomicReference<>();
        private final AtomicReference<Long> childJobIdReference = new AtomicReference<>();
        private final SubflowJobStatusEventListener subflowJobStatusEventListener;
        private final JobSyncExecutor jobSyncExecutor;

        private Rig() {
            ContextService contextService = new ContextServiceImpl(new InMemoryContextRepository());
            AsyncMessageBroker messageBroker = new AsyncMessageBroker(environment);
            ApplicationEventPublisher eventPublisher = createEventPublisher(messageBroker);
            TaskStateService taskStateService = new InMemoryTaskStateService();

            subflowJobStatusEventListener = new SubflowJobStatusEventListener(
                SpelEvaluator.create(), eventPublisher, jobService, taskExecutionService, taskFileStorage);

            ChildJobPrincipalFactory childJobPrincipalFactory = new StartingChildJobPrincipalFactory(
                jobSyncExecutorReference, childJobIdReference);
            SubflowResolver subflowResolver =
                (workflowUuid, triggerName, editorEnvironment) -> new SubflowResolver.Subflow(
                    encodeWorkflowId(CHILD_WORKFLOW), "newWorkflowCall");

            TaskDispatcherResolverFactory subflowTaskDispatcherResolverFactory =
                taskDispatcher -> new SubflowTaskDispatcher(childJobPrincipalFactory, jobService, subflowResolver);

            Map<String, TaskHandler<?>> taskHandlerMap = Map.of(
                "var/v1/set", taskExecution -> taskExecution.getParameters()
                    .get("value"),
                "suspend/v1", taskExecution -> new ActionContext.Suspend(Map.of(), null));

            jobSyncExecutor = new JobSyncExecutor(
                contextService, SpelEvaluator.create(), jobService, -1, messageBroker,
                List.<ApplicationEventListener>of(subflowJobStatusEventListener),
                List.of((taskCompletionHandler, taskDispatcher) -> new SuspendTaskCompletionHandler(
                    contextService, eventPublisher, jobService, taskExecutionService, taskFileStorage,
                    taskStateService)),
                List.of(),
                List.of(new SuspendTaskDispatcherPreSendProcessor(jobService, taskStateService, null)),
                List.of(subflowTaskDispatcherResolverFactory),
                taskExecutionService, taskExecutor, taskHandlerMap::get, taskFileStorage, -1, workflowService);

            jobSyncExecutorReference.set(jobSyncExecutor);
        }

        long startParent() {
            return jobSyncExecutor.startJob(new JobParametersDTO(encodeWorkflowId(PARENT_WORKFLOW)));
        }

        long awaitChildLaunched() throws InterruptedException {
            Instant deadline = Instant.now()
                .plus(CHILD_LAUNCH_TIMEOUT);

            while (childJobIdReference.get() == null) {
                if (Instant.now()
                    .isAfter(deadline)) {

                    throw new AssertionError("The subflow dispatcher never launched the child job");
                }

                Thread.sleep(10);
            }

            return childJobIdReference.get();
        }

        Job awaitJobStatus(long jobId, Job.Status expectedStatus) throws InterruptedException {
            Instant deadline = Instant.now()
                .plus(JOB_STATUS_TIMEOUT);

            while (true) {
                Job job = jobService.getJob(jobId);

                if (job.getStatus() == expectedStatus) {
                    return job;
                }

                if (Instant.now()
                    .isAfter(deadline)) {

                    throw new AssertionError(
                        "Job %d never reached %s (last status %s)".formatted(jobId, expectedStatus, job.getStatus()));
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

        void completeChild(long childJobId, Map<String, ?> outputs) {
            Job childJob = jobService.getJob(childJobId);

            childJob.setOutputs(taskFileStorage.storeJobOutputs(childJobId, outputs));
            childJob.setStatus(Job.Status.COMPLETED);

            jobService.update(childJob);

            subflowJobStatusEventListener.onApplicationEvent(
                new JobStatusApplicationEvent(childJobId, Job.Status.COMPLETED));
        }
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

    private static final class StartingChildJobPrincipalFactory implements ChildJobPrincipalFactory {

        private final AtomicReference<JobSyncExecutor> jobSyncExecutorReference;
        private final AtomicReference<Long> childJobIdReference;

        private StartingChildJobPrincipalFactory(
            AtomicReference<JobSyncExecutor> jobSyncExecutorReference, AtomicReference<Long> childJobIdReference) {

            this.jobSyncExecutorReference = jobSyncExecutorReference;
            this.childJobIdReference = childJobIdReference;
        }

        @Override
        public long createChildJob(long parentJobId, JobParametersDTO jobParametersDTO) {
            long childJobId = Objects.requireNonNull(jobSyncExecutorReference.get())
                .startJob(jobParametersDTO);

            childJobIdReference.set(childJobId);

            return childJobId;
        }

        @Override
        public long createPrincipalLinkedJob(long referenceJobId, JobParametersDTO jobParametersDTO) {
            throw new UnsupportedOperationException();
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
}
