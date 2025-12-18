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
import com.bytechef.atlas.execution.repository.memory.InMemoryContextRepository;
import com.bytechef.atlas.execution.repository.memory.InMemoryCounterRepository;
import com.bytechef.atlas.execution.repository.memory.InMemoryJobRepository;
import com.bytechef.atlas.execution.repository.memory.InMemoryTaskExecutionRepository;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.ContextServiceImpl;
import com.bytechef.atlas.execution.service.CounterService;
import com.bytechef.atlas.execution.service.CounterServiceImpl;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.JobServiceImpl;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.execution.service.TaskExecutionServiceImpl;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.atlas.worker.task.handler.TaskHandler;
import com.bytechef.error.ExecutionError;
import com.bytechef.evaluator.SpelEvaluator;
import com.bytechef.message.broker.memory.AsyncMessageBroker;
import com.bytechef.message.event.MessageEvent;
import com.bytechef.platform.coordinator.job.JobSyncExecutor;
import com.bytechef.tenant.TenantContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.env.Environment;
import org.springframework.core.task.TaskExecutor;

public class TaskDispatcherJobTestExecutor {

    private final Environment environment;
    private final ObjectMapper objectMapper;
    private final TaskExecutor taskExecutor;
    private final TaskFileStorage taskFileStorage;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    public TaskDispatcherJobTestExecutor(
        Environment environment, ObjectMapper objectMapper, TaskExecutor taskExecutor, TaskFileStorage taskFileStorage,
        WorkflowService workflowService) {

        this.environment = environment;
        this.objectMapper = objectMapper;
        this.taskExecutor = taskExecutor;
        this.taskFileStorage = taskFileStorage;
        this.workflowService = workflowService;
    }

    public TaskDispatcherJobExecution execute(
        String workflowId, TaskCompletionHandlerFactoriesFunction taskCompletionHandlerFactoriesFunction,
        TaskDispatcherResolverFactoriesFunction taskDispatcherResolverFactoriesFunction,
        TaskHandlerMapSupplier taskHandlerMapSupplier) {

        return execute(
            workflowId, Map.of(), taskCompletionHandlerFactoriesFunction, taskDispatcherResolverFactoriesFunction,
            taskHandlerMapSupplier);
    }

    public TaskDispatcherJobExecution execute(
        String workflowId, Map<String, Object> inputs,
        TaskCompletionHandlerFactoriesFunction taskCompletionHandlerFactoriesFunction,
        TaskDispatcherResolverFactoriesFunction taskDispatcherResolverFactoriesFunction,
        TaskHandlerMapSupplier taskHandlerMapSupplier) {

        ContextService contextService = new ContextServiceImpl(new InMemoryContextRepository());
        CounterService counterService = new CounterServiceImpl(new InMemoryCounterRepository());
        AsyncMessageBroker asyncMessageBroker = new AsyncMessageBroker(environment);

        InMemoryTaskExecutionRepository taskExecutionRepository = new InMemoryTaskExecutionRepository();

        JobService jobService = new JobServiceImpl(new InMemoryJobRepository(taskExecutionRepository, objectMapper));
        TaskExecutionService taskExecutionService = new TaskExecutionServiceImpl(taskExecutionRepository);

        JobSyncExecutor jobSyncExecutor = new JobSyncExecutor(
            contextService, SpelEvaluator.create(), jobService, -1,
            role -> (role == JobSyncExecutor.MemoryMessageFactory.Role.COORDINATOR)
                ? asyncMessageBroker : new AsyncMessageBroker(environment),
            taskCompletionHandlerFactoriesFunction.apply(contextService, counterService, taskExecutionService),
            List.of(), List.of(),
            taskDispatcherResolverFactoriesFunction.apply(
                createEventPublisher(asyncMessageBroker), contextService, counterService, taskExecutionService),
            taskExecutionService, taskExecutor, taskHandlerMapSupplier.get()::get, taskFileStorage, -1,
            workflowService);

        Job job = jobSyncExecutor.execute(new JobParametersDTO(workflowId, inputs), true);

        return new TaskDispatcherJobExecution(
            job, taskExecutionService.getJobTaskExecutions(Objects.requireNonNull(job.getId())),
            taskExecutionRepository.findAll());
    }

    private static ApplicationEventPublisher createEventPublisher(AsyncMessageBroker messageBroker) {
        return event -> {
            MessageEvent<?> messageEvent = (MessageEvent<?>) event;

            messageEvent.putMetadata(CURRENT_TENANT_ID, TenantContext.getCurrentTenantId());

            messageBroker.send(messageEvent.getRoute(), messageEvent);
        };
    }

    @SuppressFBWarnings("EI")
    public record TaskDispatcherJobExecution(
        Job job, List<TaskExecution> jobTaskExecutions, List<TaskExecution> taskExecutions) {

        public List<ExecutionError> getExecutionErrors() {
            return jobTaskExecutions.stream()
                .map(TaskExecution::getError)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        }

        public TaskExecution getTaskExecution(Long taskExecutionId) {
            return taskExecutions.stream()
                .filter(taskExecution -> Objects.equals(taskExecution.getId(), taskExecutionId))
                .findFirst()
                .orElseThrow();
        }
    }

    @FunctionalInterface
    public interface TaskCompletionHandlerFactoriesFunction {

        List<TaskCompletionHandlerFactory> apply(
            ContextService contextService, CounterService counterService, TaskExecutionService taskExecutionService);
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
}
