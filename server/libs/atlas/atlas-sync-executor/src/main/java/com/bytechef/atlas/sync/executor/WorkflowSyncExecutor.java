
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
import com.bytechef.atlas.coordinator.task.completion.TaskCompletionHandlerChain;
import com.bytechef.atlas.coordinator.task.completion.TaskCompletionHandlerFactory;
import com.bytechef.atlas.coordinator.task.dispatcher.DefaultTaskDispatcher;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcherChain;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcherResolverFactory;
import com.bytechef.atlas.domain.Job;
import com.bytechef.atlas.domain.TaskExecution;
import com.bytechef.atlas.dto.JobParametersDTO;
import com.bytechef.atlas.error.ErrorHandler;
import com.bytechef.atlas.error.ExecutionError;
import com.bytechef.atlas.event.EventPublisher;
import com.bytechef.atlas.job.JobFactory;
import com.bytechef.atlas.job.JobFactoryImpl;
import com.bytechef.atlas.message.broker.Queues;
import com.bytechef.atlas.message.broker.sync.SyncMessageBroker;
import com.bytechef.atlas.service.ContextService;
import com.bytechef.atlas.service.JobService;
import com.bytechef.atlas.service.TaskExecutionService;
import com.bytechef.atlas.service.WorkflowService;
import com.bytechef.atlas.task.evaluator.TaskEvaluator;
import com.bytechef.atlas.worker.Worker;
import com.bytechef.atlas.worker.task.handler.DefaultTaskHandlerResolver;
import com.bytechef.atlas.worker.task.handler.TaskHandler;
import com.bytechef.atlas.worker.task.handler.TaskHandlerResolverChain;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.bytechef.commons.util.CollectionUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Ivica Cardic
 */
public class WorkflowSyncExecutor {
    private static final Logger logger = LoggerFactory.getLogger(WorkflowSyncExecutor.class);

    private final ContextService contextService;
    private final JobService jobService;
    private final EventPublisher eventPublisher;
    private final List<TaskCompletionHandlerFactory> taskCompletionHandlerFactories;
    private final List<TaskDispatcherResolverFactory> taskDispatcherResolverFactories;
    private final TaskExecutionService taskExecutionService;
    private final Map<String, TaskHandler<?>> taskHandlerMap;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    public WorkflowSyncExecutor(
        ContextService contextService, JobService jobService, EventPublisher eventPublisher,
        List<TaskCompletionHandlerFactory> taskCompletionHandlerFactories,
        List<TaskDispatcherResolverFactory> taskDispatcherResolverFactories, TaskExecutionService taskExecutionService,
        Map<String, TaskHandler<?>> taskHandlerMap, WorkflowService workflowService) {

        this.contextService = contextService;
        this.jobService = jobService;
        this.eventPublisher = eventPublisher;
        this.taskCompletionHandlerFactories = taskCompletionHandlerFactories;
        this.taskDispatcherResolverFactories = taskDispatcherResolverFactories;
        this.taskExecutionService = taskExecutionService;
        this.taskHandlerMap = taskHandlerMap;
        this.workflowService = workflowService;
    }

    public Job execute(String workflowId) {
        return execute(workflowId, Map.of());
    }

    public Job execute(String workflowId, Map<String, Object> inputs) {
        SyncMessageBroker messageBroker = new SyncMessageBroker();

        messageBroker.receive(Queues.ERRORS, message -> {
            TaskExecution erroredTaskExecution = (TaskExecution) message;

            ExecutionError error = erroredTaskExecution.getError();

            logger.error(error.getMessage());
        });

        TaskHandlerResolverChain taskHandlerResolverChain = new TaskHandlerResolverChain();

        taskHandlerResolverChain.setTaskHandlerResolvers(List.of(new DefaultTaskHandlerResolver(taskHandlerMap)));

        TaskEvaluator taskEvaluator = TaskEvaluator.create();

        Worker worker = Worker.builder()
            .withTaskHandlerResolver(taskHandlerResolverChain)
            .withMessageBroker(messageBroker)
            .withEventPublisher(eventPublisher)
            .withTaskEvaluator(taskEvaluator)
            .build();

        SyncMessageBroker coordinatorMessageBroker = new SyncMessageBroker();

        coordinatorMessageBroker.receive(Queues.TASKS, o -> worker.handle((TaskExecution) o));

        TaskDispatcherChain taskDispatcherChain = new TaskDispatcherChain();

        taskDispatcherChain.setTaskDispatcherResolvers(
            CollectionUtils.concat(
                taskDispatcherResolverFactories.stream()
                    .map(taskDispatcherFactory -> taskDispatcherFactory
                        .createTaskDispatcherResolver(taskDispatcherChain)),
                Stream.of(new DefaultTaskDispatcher(coordinatorMessageBroker, List.of()))));

        JobExecutor jobExecutor = new JobExecutor(
            contextService, taskDispatcherChain, taskExecutionService, taskEvaluator, workflowService);

        DefaultTaskCompletionHandler defaultTaskCompletionHandler = new DefaultTaskCompletionHandler(
            contextService, e -> {}, jobExecutor, jobService, taskEvaluator, taskExecutionService, workflowService);

        TaskCompletionHandlerChain taskCompletionHandlerChain = new TaskCompletionHandlerChain();

        taskCompletionHandlerChain.setTaskCompletionHandlers(
            CollectionUtils.concat(
                taskCompletionHandlerFactories.stream()
                    .map(taskCompletionHandlerFactory -> taskCompletionHandlerFactory.createTaskCompletionHandler(
                        taskCompletionHandlerChain, taskDispatcherChain)),
                Stream.of(defaultTaskCompletionHandler)));

        JobFactory jobFactory = new JobFactoryImpl(contextService, eventPublisher, jobService, messageBroker);

        @SuppressWarnings({
            "rawtypes", "unchecked"
        })
        Coordinator coordinator = new Coordinator(
            (ErrorHandler) new TaskExecutionErrorHandler(
                eventPublisher, jobService, taskDispatcherChain, taskExecutionService),
            eventPublisher, jobExecutor, jobFactory, jobService, taskCompletionHandlerChain, taskDispatcherChain,
            taskExecutionService);

        messageBroker.receive(Queues.COMPLETIONS, o -> coordinator.complete((TaskExecution) o));
        messageBroker.receive(Queues.JOBS, jobId -> coordinator.start((Long) jobId));

        long jobId = jobFactory.create(new JobParametersDTO(inputs, workflowId));

        return jobService.getJob(jobId);
    }
}
