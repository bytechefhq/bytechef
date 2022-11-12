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
import com.bytechef.atlas.error.ExecutionError;
import com.bytechef.atlas.event.EventPublisher;
import com.bytechef.atlas.message.broker.MessageBroker;
import com.bytechef.atlas.message.broker.Queues;
import com.bytechef.atlas.message.broker.sync.SyncMessageBroker;
import com.bytechef.atlas.service.ContextService;
import com.bytechef.atlas.service.CounterService;
import com.bytechef.atlas.service.JobService;
import com.bytechef.atlas.service.TaskExecutionService;
import com.bytechef.atlas.service.WorkflowService;
import com.bytechef.atlas.task.dispatcher.TaskDispatcher;
import com.bytechef.atlas.task.dispatcher.TaskDispatcherResolver;
import com.bytechef.atlas.task.evaluator.TaskEvaluator;
import com.bytechef.atlas.task.evaluator.spel.SpelTaskEvaluator;
import com.bytechef.atlas.worker.Worker;
import com.bytechef.atlas.worker.task.handler.DefaultTaskHandlerResolver;
import com.bytechef.atlas.worker.task.handler.TaskHandler;
import com.bytechef.atlas.worker.task.handler.TaskHandlerResolverChain;
import com.bytechef.commons.uuid.UUIDGenerator;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
            ContextService contextService,
            CounterService counterService,
            JobService jobService,
            EventPublisher eventPublisher,
            TaskExecutionService taskExecutionService,
            Map<String, TaskHandler<?>> taskHandlerMap,
            WorkflowService workflowService) {
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
                workflowId,
                inputs,
                (counterService, taskCompletionHandler, taskDispatcher, taskEvaluator, taskExecutionService) ->
                        Collections.emptyList(),
                (contextService, counterService, messageBroker, taskDispatcher, taskEvaluator, taskExecutionService) ->
                        Collections.emptyList(),
                Collections::emptyMap);
    }

    public Job execute(String workflowId, Map<String, Object> inputs, Map<String, TaskHandler<?>> taskHandlerMap) {
        return execute(
                workflowId,
                inputs,
                (counterService, taskCompletionHandler, taskDispatcher, taskEvaluator, taskExecutionService) ->
                        Collections.emptyList(),
                (contextService, counterService, messageBroker, taskDispatcher, taskEvaluator, taskExecutionService) ->
                        Collections.emptyList(),
                () -> taskHandlerMap);
    }

    public Job execute(
            String workflowId,
            GetTaskCompletionHandlersFunction getTaskCompletionHandlersFunction,
            GetTaskDispatcherResolversFunction getTaskDispatcherResolversFunction,
            GetTaskHandlerMapSupplier getTaskHandlerMapSupplier) {
        return execute(
                workflowId,
                Map.of(),
                getTaskCompletionHandlersFunction,
                getTaskDispatcherResolversFunction,
                getTaskHandlerMapSupplier);
    }

    public Job execute(
            String workflowId,
            Map<String, Object> inputs,
            GetTaskCompletionHandlersFunction getTaskCompletionHandlersFunction,
            GetTaskDispatcherResolversFunction getTaskDispatcherResolversFunction,
            GetTaskHandlerMapSupplier getTaskHandlerMapSupplier) {

        SyncMessageBroker messageBroker = new SyncMessageBroker();

        messageBroker.receive(Queues.ERRORS, message -> {
            TaskExecution erroredTaskExecution = (TaskExecution) message;

            ExecutionError error = erroredTaskExecution.getError();

            logger.error(error.getMessage());
        });

        TaskHandlerResolverChain taskHandlerResolverChain = new TaskHandlerResolverChain();

        taskHandlerResolverChain.setTaskHandlerResolvers(List.of(new DefaultTaskHandlerResolver(
                Stream.concat(getTaskHandlerMapSupplier.get().entrySet().stream(), taskHandlerMap.entrySet().stream())
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))));

        TaskEvaluator taskEvaluator = SpelTaskEvaluator.create();

        Worker worker = Worker.builder()
                .withTaskHandlerResolver(taskHandlerResolverChain)
                .withMessageBroker(messageBroker)
                .withEventPublisher(eventPublisher)
                .withTaskEvaluator(taskEvaluator)
                .build();

        SyncMessageBroker coordinatorMessageBroker = new SyncMessageBroker();

        coordinatorMessageBroker.receive(Queues.TASKS, o -> worker.handle((TaskExecution) o));

        TaskDispatcherChain taskDispatcherChain = new TaskDispatcherChain();

        taskDispatcherChain.setTaskDispatcherResolvers(Stream.concat(
                        getTaskDispatcherResolversFunction
                                .apply(
                                        contextService,
                                        counterService,
                                        coordinatorMessageBroker,
                                        taskDispatcherChain,
                                        taskEvaluator,
                                        taskExecutionService)
                                .stream(),
                        Stream.of(new DefaultTaskDispatcher(coordinatorMessageBroker, List.of())))
                .toList());

        JobExecutor jobExecutor = new JobExecutor(
                contextService, taskDispatcherChain, taskExecutionService, taskEvaluator, workflowService);

        DefaultTaskCompletionHandler defaultTaskCompletionHandler = new DefaultTaskCompletionHandler(
                contextService,
                eventPublisher,
                jobExecutor,
                jobService,
                taskEvaluator,
                taskExecutionService,
                workflowService);

        TaskCompletionHandlerChain taskCompletionHandlerChain = new TaskCompletionHandlerChain();

        taskCompletionHandlerChain.setTaskCompletionHandlers(Stream.concat(
                        getTaskCompletionHandlersFunction
                                .apply(
                                        counterService,
                                        taskCompletionHandlerChain,
                                        taskDispatcherChain,
                                        taskEvaluator,
                                        taskExecutionService)
                                .stream(),
                        Stream.of(defaultTaskCompletionHandler))
                .toList());

        Coordinator coordinator = new Coordinator(
                contextService,
                getTaskExecutionErrorHandler(taskDispatcherChain),
                eventPublisher,
                jobExecutor,
                jobService,
                messageBroker,
                taskCompletionHandlerChain,
                taskDispatcherChain,
                taskExecutionService);

        messageBroker.receive(Queues.COMPLETIONS, o -> coordinator.complete((TaskExecution) o));
        messageBroker.receive(Queues.JOBS, jobId -> coordinator.start((String) jobId));

        String jobId = UUIDGenerator.generate();

        JobParametersDTO jobParametersDTO = new JobParametersDTO();

        jobParametersDTO.setInputs(inputs);
        jobParametersDTO.setJobId(jobId);
        jobParametersDTO.setWorkflowId(workflowId);

        coordinator.create(jobParametersDTO);

        return jobService.getJob(jobId);
    }

    @SuppressWarnings("unchecked")
    private TaskExecutionErrorHandler getTaskExecutionErrorHandler(TaskDispatcher<?> taskDispatcher) {
        return new TaskExecutionErrorHandler(
                eventPublisher, jobService, (TaskDispatcher<TaskExecution>) taskDispatcher, taskExecutionService);
    }

    public interface GetTaskCompletionHandlersFunction {
        List<TaskCompletionHandler> apply(
                CounterService counterService,
                TaskCompletionHandler taskCompletionHandler,
                TaskDispatcher<?> taskDispatcher,
                TaskEvaluator taskEvaluator,
                TaskExecutionService taskExecutionService);
    }

    public interface GetTaskDispatcherResolversFunction {
        List<TaskDispatcherResolver> apply(
                ContextService contextService,
                CounterService counterService,
                MessageBroker messageBroker,
                TaskDispatcher<?> taskDispatcher,
                TaskEvaluator taskEvaluator,
                TaskExecutionService taskExecutionService);
    }

    public interface GetTaskHandlerMapSupplier {
        Map<String, TaskHandler<?>> get();
    }
}
