
/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.atlas.sync.executor;

import com.bytechef.atlas.coordinator.Coordinator;
import com.bytechef.atlas.coordinator.error.TaskExecutionErrorHandler;
import com.bytechef.atlas.coordinator.job.executor.JobExecutor;
import com.bytechef.atlas.coordinator.task.completion.DefaultTaskCompletionHandler;
import com.bytechef.atlas.coordinator.task.completion.TaskCompletionHandler;
import com.bytechef.atlas.coordinator.task.completion.TaskCompletionHandlerChain;
import com.bytechef.atlas.coordinator.task.dispatcher.DefaultTaskDispatcher;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcherChain;
import com.bytechef.atlas.domain.Job;
import com.bytechef.atlas.domain.TaskExecution;
import com.bytechef.atlas.dto.JobParametersDTO;
import com.bytechef.atlas.error.ErrorHandler;
import com.bytechef.atlas.error.ExecutionError;
import com.bytechef.atlas.event.EventPublisher;
import com.bytechef.atlas.facade.JobFacade;
import com.bytechef.atlas.facade.JobFacadeImpl;
import com.bytechef.atlas.message.broker.MessageBroker;
import com.bytechef.atlas.message.broker.Queues;
import com.bytechef.atlas.message.broker.sync.SyncMessageBroker;
import com.bytechef.atlas.service.ContextService;
import com.bytechef.atlas.service.CounterService;
import com.bytechef.atlas.service.JobService;
import com.bytechef.atlas.service.TaskExecutionService;
import com.bytechef.atlas.service.WorkflowService;
import com.bytechef.atlas.task.Task;
import com.bytechef.atlas.task.dispatcher.TaskDispatcher;
import com.bytechef.atlas.task.dispatcher.TaskDispatcherResolver;
import com.bytechef.atlas.task.evaluator.TaskEvaluator;
import com.bytechef.atlas.worker.Worker;
import com.bytechef.atlas.worker.task.handler.DefaultTaskHandlerResolver;
import com.bytechef.atlas.worker.task.handler.TaskHandler;
import com.bytechef.atlas.worker.task.handler.TaskHandlerResolverChain;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.bytechef.commons.util.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WorkflowExecutor {
    private static final Logger logger = LoggerFactory.getLogger(WorkflowExecutor.class);

    private final ContextService contextService;
    private final CounterService counterService;
    private final JobService jobService;
    private final EventPublisher eventPublisher;
    private final TaskExecutionService taskExecutionService;
    private final Map<String, TaskHandler<?>> taskHandlerMap;
    private final WorkflowService workflowService;

    public WorkflowExecutor(
        ContextService contextService, CounterService counterService, JobService jobService,
        EventPublisher eventPublisher, TaskExecutionService taskExecutionService,
        Map<String, TaskHandler<?>> taskHandlerMap, WorkflowService workflowService) {

        this.contextService = contextService;
        this.counterService = counterService;
        this.jobService = jobService;
        this.eventPublisher = eventPublisher;
        this.taskExecutionService = taskExecutionService;
        this.taskHandlerMap = taskHandlerMap;
        this.workflowService = workflowService;
    }

    public Job execute(String workflowId) {
        return execute(workflowId, Map.of());
    }

    public Job execute(String workflowId, Map<String, Object> inputs) {
        return execute(
            workflowId, inputs,
            (counterService, taskCompletionHandler, taskDispatcher, taskEvaluator, taskExecutionService) -> Collections
                .emptyList(),
            (
                contextService, counterService, messageBroker, taskDispatcher, taskEvaluator,
                taskExecutionService) -> Collections.emptyList(),
            Collections::emptyMap);
    }

    public Job execute(String workflowId, Map<String, Object> inputs, Map<String, TaskHandler<?>> taskHandlerMap) {
        return execute(
            workflowId, inputs,
            (counterService, taskCompletionHandler, taskDispatcher, taskEvaluator, taskExecutionService) -> Collections
                .emptyList(),
            (
                contextService, counterService, messageBroker, taskDispatcher, taskEvaluator,
                taskExecutionService) -> Collections.emptyList(),
            () -> taskHandlerMap);
    }

    public Job execute(
        String workflowId, TaskCompletionHandlersFunction taskCompletionHandlersFunction,
        TaskDispatcherResolversFunction taskDispatcherResolversFunction,
        TaskHandlerMapSupplier taskHandlerMapSupplier) {

        return execute(
            workflowId, Map.of(), taskCompletionHandlersFunction, taskDispatcherResolversFunction,
            taskHandlerMapSupplier);
    }

    public Job execute(
        String workflowId, Map<String, Object> inputs, TaskCompletionHandlersFunction taskCompletionHandlersFunction,
        TaskDispatcherResolversFunction taskDispatcherResolversFunction,
        TaskHandlerMapSupplier taskHandlerMapSupplier) {

        SyncMessageBroker workerMessageBroker = new SyncMessageBroker();

        workerMessageBroker.receive(Queues.ERRORS, message -> {
            TaskExecution erroredTaskExecution = (TaskExecution) message;

            ExecutionError error = erroredTaskExecution.getError();

            logger.error(error.getMessage());
        });

        TaskHandlerResolverChain taskHandlerResolverChain = new TaskHandlerResolverChain();

        taskHandlerResolverChain.setTaskHandlerResolvers(
            List.of(
                new DefaultTaskHandlerResolver(
                    CollectionUtils.concat(taskHandlerMapSupplier.get(), taskHandlerMap))));

        TaskEvaluator taskEvaluator = TaskEvaluator.create();

        Worker worker = Worker.builder()
            .withTaskHandlerResolver(taskHandlerResolverChain)
            .withMessageBroker(workerMessageBroker)
            .withEventPublisher(eventPublisher)
            .withTaskEvaluator(taskEvaluator)
            .build();

        SyncMessageBroker coordinatorMessageBroker = new SyncMessageBroker();

        coordinatorMessageBroker.receive(Queues.TASKS, o -> worker.handle((TaskExecution) o));

        TaskDispatcherChain taskDispatcherChain = new TaskDispatcherChain();

        taskDispatcherChain.setTaskDispatcherResolvers(
            CollectionUtils.concat(
                taskDispatcherResolversFunction
                    .apply(
                        contextService, counterService, coordinatorMessageBroker, taskDispatcherChain, taskEvaluator,
                        taskExecutionService),
                Stream.of(new DefaultTaskDispatcher(coordinatorMessageBroker, List.of()))));

        JobExecutor jobExecutor = new JobExecutor(
            contextService, taskDispatcherChain, taskExecutionService, taskEvaluator, workflowService);

        DefaultTaskCompletionHandler defaultTaskCompletionHandler = new DefaultTaskCompletionHandler(
            contextService, e -> {}, jobExecutor, jobService, taskEvaluator, taskExecutionService, workflowService);

        TaskCompletionHandlerChain taskCompletionHandlerChain = new TaskCompletionHandlerChain();

        taskCompletionHandlerChain.setTaskCompletionHandlers(
            CollectionUtils.concat(
                taskCompletionHandlersFunction
                    .apply(
                        counterService, taskCompletionHandlerChain, taskDispatcherChain, taskEvaluator,
                        taskExecutionService),
                Stream.of(defaultTaskCompletionHandler)));

        JobFacade jobFacade = new JobFacadeImpl(contextService, eventPublisher, jobService, workerMessageBroker);

        @SuppressWarnings({
            "rawtypes", "unchecked"
        })
        Coordinator coordinator = new Coordinator(
            (ErrorHandler) getTaskExecutionErrorHandler(taskDispatcherChain), eventPublisher, jobExecutor,
            jobService, taskCompletionHandlerChain, taskDispatcherChain, taskExecutionService);

        workerMessageBroker.receive(Queues.COMPLETIONS, o -> coordinator.complete((TaskExecution) o));
        workerMessageBroker.receive(Queues.JOBS, jobId -> coordinator.start((Long) jobId));

        long jobId = jobFacade.create(new JobParametersDTO(inputs, workflowId));

        return jobService.getJob(jobId);
    }

    private TaskExecutionErrorHandler getTaskExecutionErrorHandler(TaskDispatcher<? super Task> taskDispatcher) {
        return new TaskExecutionErrorHandler(
            eventPublisher, jobService, taskDispatcher, taskExecutionService);
    }

    @FunctionalInterface
    public interface TaskCompletionHandlersFunction {
        List<TaskCompletionHandler> apply(
            CounterService counterService, TaskCompletionHandler taskCompletionHandler,
            TaskDispatcher<? super Task> taskDispatcher, TaskEvaluator taskEvaluator,
            TaskExecutionService taskExecutionService);
    }

    @FunctionalInterface
    public interface TaskDispatcherResolversFunction {
        List<TaskDispatcherResolver> apply(
            ContextService contextService, CounterService counterService, MessageBroker messageBroker,
            TaskDispatcher<? super Task> taskDispatcher, TaskEvaluator taskEvaluator,
            TaskExecutionService taskExecutionService);
    }

    @FunctionalInterface
    public interface TaskHandlerMapSupplier {
        Map<String, TaskHandler<?>> get();
    }
}
