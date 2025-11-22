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

package com.bytechef.platform.workflow.task.dispatcher.test.workflow;

import static com.bytechef.tenant.constant.TenantConstants.CURRENT_TENANT_ID;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.coordinator.task.completion.TaskCompletionHandlerFactory;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcherResolverFactory;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.dto.JobParametersDTO;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.CounterService;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.atlas.worker.task.handler.TaskHandler;
import com.bytechef.error.ExecutionError;
import com.bytechef.evaluator.SpelEvaluator;
import com.bytechef.message.broker.memory.SyncMessageBroker;
import com.bytechef.message.event.MessageEvent;
import com.bytechef.platform.coordinator.job.JobSyncExecutor;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.task.TaskExecutor;

public class TaskDispatcherJobTestExecutor {

    private static final String TEST_TENANT_ID = "test";

    private final ContextService contextService;
    private final CounterService counterService;
    private final JobService jobService;
    private final TaskExecutionService taskExecutionService;
    private final TaskExecutor taskExecutor;
    private final TaskFileStorage taskFileStorage;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    public TaskDispatcherJobTestExecutor(
        ContextService contextService, CounterService counterService, TaskExecutor taskExecutor, JobService jobService,
        TaskExecutionService taskExecutionService, TaskFileStorage taskFileStorage, WorkflowService workflowService) {

        this.contextService = contextService;
        this.counterService = counterService;
        this.jobService = jobService;
        this.taskExecutionService = taskExecutionService;
        this.taskExecutor = taskExecutor;
        this.taskFileStorage = taskFileStorage;
        this.workflowService = workflowService;
    }

    public Job execute(
        String workflowId, TaskCompletionHandlerFactoriesFunction taskCompletionHandlerFactoriesFunction,
        TaskDispatcherResolverFactoriesFunction taskDispatcherResolverFactoriesFunction,
        TaskHandlerMapSupplier taskHandlerMapSupplier) {

        return execute(
            workflowId, Map.of(), taskCompletionHandlerFactoriesFunction, taskDispatcherResolverFactoriesFunction,
            taskHandlerMapSupplier);
    }

    public Job execute(
        String workflowId, Map<String, Object> inputs,
        TaskCompletionHandlerFactoriesFunction taskCompletionHandlerFactoriesFunction,
        TaskDispatcherResolverFactoriesFunction taskDispatcherResolverFactoriesFunction,
        TaskHandlerMapSupplier taskHandlerMapSupplier) {

        SyncMessageBroker syncMessageBroker = new SyncMessageBroker();

        JobSyncExecutor jobSyncExecutor = new JobSyncExecutor(
            contextService, SpelEvaluator.create(), jobService, -1, () -> syncMessageBroker,
            taskCompletionHandlerFactoriesFunction.apply(counterService, taskExecutionService), List.of(), List.of(),
            taskDispatcherResolverFactoriesFunction.apply(
                createEventPublisher(syncMessageBroker), contextService, counterService, taskExecutionService),
            taskExecutionService, taskExecutor, taskHandlerMapSupplier.get()::get, taskFileStorage, -1,
            workflowService);

        return jobSyncExecutor.execute(new JobParametersDTO(workflowId, inputs), true);
    }

    private static ApplicationEventPublisher createEventPublisher(SyncMessageBroker messageBroker) {
        return event -> {
            MessageEvent<?> messageEvent = (MessageEvent<?>) event;

            if (messageEvent.getMetadata(CURRENT_TENANT_ID) == null) {
                messageEvent.putMetadata(CURRENT_TENANT_ID, TEST_TENANT_ID);
            }

            messageBroker.send(messageEvent.getRoute(), messageEvent);
        };
    }

    @FunctionalInterface
    public interface TaskCompletionHandlerFactoriesFunction {

        List<TaskCompletionHandlerFactory> apply(
            CounterService counterService, TaskExecutionService taskExecutionService);
    }

    @FunctionalInterface
    public interface TaskDispatcherResolverFactoriesFunction {

        List<TaskDispatcherResolverFactory> apply(
            ApplicationEventPublisher eventPublisher, ContextService contextService,
            CounterService counterService, TaskExecutionService taskExecutionService);
    }

    @FunctionalInterface
    public interface TaskHandlerMapSupplier {

        Map<String, TaskHandler<?>> get();
    }

    public List<ExecutionError> getExecutionErrors(long jobId) {
        List<TaskExecution> jobTaskExecutions = taskExecutionService.getJobTaskExecutions(jobId);

        return jobTaskExecutions.stream()
            .map(TaskExecution::getError)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }
}
