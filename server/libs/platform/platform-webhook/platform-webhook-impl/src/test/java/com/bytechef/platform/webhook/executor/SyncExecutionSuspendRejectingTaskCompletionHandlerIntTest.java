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

import static com.bytechef.tenant.constant.TenantConstants.CURRENT_TENANT_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.atlas.configuration.repository.resource.ClassPathResourceWorkflowRepository;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.configuration.service.WorkflowServiceImpl;
import com.bytechef.atlas.coordinator.task.completion.TaskCompletionHandlerFactory;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcherResolverFactory;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
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
import com.bytechef.atlas.file.storage.TaskFileStorageImpl;
import com.bytechef.atlas.worker.task.handler.TaskHandler;
import com.bytechef.commons.util.ConvertUtils;
import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.component.definition.ActionContext.Suspend;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.evaluator.SpelEvaluator;
import com.bytechef.exception.ExecutionException;
import com.bytechef.file.storage.base64.service.Base64FileStorageService;
import com.bytechef.message.broker.memory.AsyncMessageBroker;
import com.bytechef.message.event.MessageEvent;
import com.bytechef.platform.job.sync.executor.JobSyncExecutor;
import com.bytechef.platform.job.sync.executor.SyncExecutionSuspendRejectingTaskCompletionHandler;
import com.bytechef.task.dispatcher.condition.ConditionTaskDispatcher;
import com.bytechef.task.dispatcher.condition.completion.ConditionTaskCompletionHandler;
import com.bytechef.tenant.TenantContext;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.task.SyncTaskExecutor;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author Ivica Cardic
 */
class SyncExecutionSuspendRejectingTaskCompletionHandlerIntTest {

    private static final String TENANT = "public";

    private final AtomicBoolean nextStepRan = new AtomicBoolean();

    private JobService jobService;
    private TaskExecutionService taskExecutionService;
    private JobSyncExecutor jobSyncExecutor;
    private WorkflowService workflowService;

    @BeforeEach
    void beforeEach() {
        ObjectMapper objectMapper = JsonMapper.builder()
            .build();

        ConvertUtils.setObjectMapper(objectMapper);
        JsonUtils.setObjectMapper(objectMapper);
        MapUtils.setObjectMapper(objectMapper);

        TenantContext.setCurrentTenantId(TENANT);

        InMemoryTaskExecutionRepository taskExecutionRepository = new InMemoryTaskExecutionRepository();

        ContextService contextService = new ContextServiceImpl(new InMemoryContextRepository());
        taskExecutionService = new TaskExecutionServiceImpl(taskExecutionRepository);
        TaskFileStorage taskFileStorage = new TaskFileStorageImpl(new Base64FileStorageService());

        jobService = new JobServiceImpl(new InMemoryJobRepository(taskExecutionRepository, objectMapper));
        workflowService = new WorkflowServiceImpl(
            new ConcurrentMapCacheManager(), List.of(),
            List.of(
                new ClassPathResourceWorkflowRepository(
                    "workflows/**/*.{json|yml|yaml}", new PathMatchingResourcePatternResolver())));

        Map<String, TaskHandler<?>> taskHandlerMap = Map.of(
            "test/v1/suspend", taskExecution -> new Suspend(Map.of(), null),
            "test/v1/next", taskExecution -> {
                nextStepRan.set(true);

                return null;
            });

        AsyncMessageBroker asyncMessageBroker = new AsyncMessageBroker(new StandardEnvironment());

        ApplicationEventPublisher eventPublisher = event -> {
            MessageEvent<?> messageEvent = (MessageEvent<?>) event;

            messageEvent.putMetadata(CURRENT_TENANT_ID, TenantContext.getCurrentTenantId());

            asyncMessageBroker.send(messageEvent.getRoute(), messageEvent);
        };

        Evaluator evaluator = SpelEvaluator.create();

        List<TaskCompletionHandlerFactory> taskCompletionHandlerFactories = List.of(
            (taskCompletionHandler, taskDispatcher) -> new SyncExecutionSuspendRejectingTaskCompletionHandler(
                eventPublisher, jobService, taskExecutionService, WebhookWorkflowExecutorImpl::suspendingTaskMessage),
            (taskCompletionHandler, taskDispatcher) -> new ConditionTaskCompletionHandler(
                contextService, evaluator, taskCompletionHandler, taskDispatcher, taskExecutionService,
                taskFileStorage));

        List<TaskDispatcherResolverFactory> taskDispatcherResolverFactories = List.of(
            taskDispatcher -> new ConditionTaskDispatcher(
                contextService, evaluator, eventPublisher, taskDispatcher, taskExecutionService, taskFileStorage));

        jobSyncExecutor = new JobSyncExecutor(
            contextService, evaluator, jobService, -1, asyncMessageBroker, List.of(), taskCompletionHandlerFactories,
            List.of(), List.of(), taskDispatcherResolverFactories, taskExecutionService,
            new SyncTaskExecutor(), taskHandlerMap::get, taskFileStorage, 10, workflowService);
    }

    @Test
    void testSuspendingStepFailsTheRunWithExplanation() {
        String workflowId = EncodingUtils.base64EncodeToString("syncSuspend");

        assertThatThrownBy(() -> jobSyncExecutor.execute(
            new JobParametersDTO(workflowId, Map.of()),
            jobParametersDTO -> jobService.create(jobParametersDTO, workflowService.getWorkflow(workflowId)), true,
            taskExecutionCompleteEvent -> {}))
                .isInstanceOf(ExecutionException.class)
                .hasMessage(
                    WebhookWorkflowExecutorImpl.suspendingTaskMessage("approve", "test/v1/suspend"));

        assertThat(nextStepRan).isFalse();
    }

    @Test
    void testSuspendingStepInsideAConditionFailsTheRunAndItsParent() {
        String workflowId = EncodingUtils.base64EncodeToString("syncSuspendNested");

        Job job = jobSyncExecutor.execute(
            new JobParametersDTO(workflowId, Map.of()),
            jobParametersDTO -> jobService.create(jobParametersDTO, workflowService.getWorkflow(workflowId)), false,
            taskExecutionCompleteEvent -> {});

        assertThat(job.getStatus()).isEqualTo(Job.Status.FAILED);
        assertThat(job.getError()
            .getMessage()).isEqualTo(
                WebhookWorkflowExecutorImpl.suspendingTaskMessage("approve", "test/v1/suspend"));
        assertThat(nextStepRan).isFalse();
        assertThat(taskExecutionService.getJobTaskExecutions(Objects.requireNonNull(job.getId())))
            .filteredOn(taskExecution -> "condition_1".equals(taskExecution.getName()))
            .singleElement()
            .extracting(TaskExecution::getStatus)
            .isEqualTo(TaskExecution.Status.FAILED);
    }

    @Test
    void testSuspendingStepLeavesJobFailed() {
        String workflowId = EncodingUtils.base64EncodeToString("syncSuspend");

        Job job = jobSyncExecutor.execute(
            new JobParametersDTO(workflowId, Map.of()),
            jobParametersDTO -> jobService.create(jobParametersDTO, workflowService.getWorkflow(workflowId)), false,
            taskExecutionCompleteEvent -> {});

        assertThat(job.getStatus()).isEqualTo(Job.Status.FAILED);
        assertThat(job.getError()).isNotNull();
        assertThat(job.getError()
            .getMessage()).isEqualTo(
                WebhookWorkflowExecutorImpl.suspendingTaskMessage("approve", "test/v1/suspend"));
        assertThat(nextStepRan).isFalse();
    }
}
