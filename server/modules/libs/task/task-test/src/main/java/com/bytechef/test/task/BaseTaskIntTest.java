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

package com.bytechef.test.task;

import com.bytechef.atlas.MapObject;
import com.bytechef.atlas.context.service.ContextService;
import com.bytechef.atlas.coordinator.CoordinatorImpl;
import com.bytechef.atlas.coordinator.error.TaskExecutionErrorHandler;
import com.bytechef.atlas.coordinator.job.executor.DefaultJobExecutor;
import com.bytechef.atlas.coordinator.task.completion.DefaultTaskCompletionHandler;
import com.bytechef.atlas.coordinator.task.completion.TaskCompletionHandler;
import com.bytechef.atlas.coordinator.task.completion.TaskCompletionHandlerChain;
import com.bytechef.atlas.coordinator.task.dispatcher.DefaultTaskDispatcher;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcherChain;
import com.bytechef.atlas.error.Error;
import com.bytechef.atlas.event.EventPublisher;
import com.bytechef.atlas.job.domain.Job;
import com.bytechef.atlas.job.service.JobService;
import com.bytechef.atlas.message.broker.MessageBroker;
import com.bytechef.atlas.message.broker.Queues;
import com.bytechef.atlas.message.broker.sync.SyncMessageBroker;
import com.bytechef.atlas.task.dispatcher.TaskDispatcher;
import com.bytechef.atlas.task.dispatcher.TaskDispatcherResolver;
import com.bytechef.atlas.task.execution.domain.TaskExecution;
import com.bytechef.atlas.task.execution.evaluator.spel.SpelTaskEvaluator;
import com.bytechef.atlas.worker.Worker;
import com.bytechef.atlas.worker.WorkerImpl;
import com.bytechef.atlas.worker.task.handler.DefaultTaskHandlerResolver;
import com.bytechef.atlas.worker.task.handler.TaskHandler;
import com.bytechef.atlas.worker.task.handler.TaskHandlerResolverChain;
import com.bytechef.atlas.workflow.service.WorkflowService;
import com.bytechef.task.commons.json.JSONHelper;
import com.bytechef.task.execution.service.TaskExecutionService;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Ivica Cardic
 */
public abstract class BaseTaskIntTest {

    private static final Logger logger = LoggerFactory.getLogger(BaseTaskIntTest.class);

    @Autowired
    protected ContextService contextService;

    @Autowired
    protected JobService jobService;

    @Autowired
    protected JSONHelper jsonHelper;

    @Autowired
    private EventPublisher eventPublisher;

    @Autowired
    protected TaskExecutionService taskExecutionService;

    @Autowired(required = false)
    protected Map<String, TaskHandler<?>> taskHandlerMap = Map.of();

    @Autowired
    protected WorkflowService workflowService;

    protected List<TaskCompletionHandler> getTaskCompletionHandlers(
            TaskCompletionHandler taskCompletionHandler, TaskDispatcher<?> taskDispatcher) {
        return List.of();
    }

    protected List<TaskDispatcherResolver> getTaskDispatcherResolvers(
            MessageBroker coordinatorMessageBroker, TaskDispatcher<?> taskDispatcher) {
        return List.of();
    }

    protected Map<String, TaskHandler<?>> getTaskHandlerMap() {
        return taskHandlerMap;
    }

    protected Job startJob(String workflowId, Map<String, ?> inputs) {
        CoordinatorImpl coordinator = new CoordinatorImpl();

        SyncMessageBroker messageBroker = new SyncMessageBroker();

        messageBroker.receive(Queues.COMPLETIONS, o -> coordinator.complete((TaskExecution) o));
        messageBroker.receive(Queues.JOBS, o -> coordinator.start((Job) o));
        messageBroker.receive(Queues.ERRORS, message -> {
            TaskExecution erringTask = (TaskExecution) message;

            Error error = erringTask.getError();

            logger.error(error.getMessage());
        });

        coordinator.setMessageBroker(messageBroker);

        TaskHandlerResolverChain taskHandlerResolverChain = new TaskHandlerResolverChain();

        taskHandlerResolverChain.setTaskHandlerResolvers(List.of(new DefaultTaskHandlerResolver(getTaskHandlerMap())));

        Worker worker = WorkerImpl.builder()
                .withTaskHandlerResolver(taskHandlerResolverChain)
                .withMessageBroker(messageBroker)
                .withEventPublisher(eventPublisher)
                .withTaskEvaluator(SpelTaskEvaluator.create())
                .build();

        coordinator.setContextService(contextService);
        coordinator.setJobService(jobService);

        SyncMessageBroker coordinatorMessageBroker = new SyncMessageBroker();

        coordinatorMessageBroker.receive(Queues.TASKS, o -> worker.handle((TaskExecution) o));

        TaskDispatcherChain taskDispatcherChain = new TaskDispatcherChain();

        taskDispatcherChain.setTaskDispatcherResolvers(Stream.concat(
                        getTaskDispatcherResolvers(coordinatorMessageBroker, taskDispatcherChain).stream(),
                        Stream.of(new DefaultTaskDispatcher(coordinatorMessageBroker, List.of())))
                .toList());

        coordinator.setErrorHandler(getJobTaskErrorHandler(taskDispatcherChain));
        coordinator.setEventPublisher(eventPublisher);

        DefaultJobExecutor jobExecutor = new DefaultJobExecutor();

        jobExecutor.setContextService(contextService);
        jobExecutor.setTaskExecutionService(taskExecutionService);
        jobExecutor.setWorkflowService(workflowService);
        jobExecutor.setTaskDispatcher(taskDispatcherChain);
        jobExecutor.setTaskEvaluator(SpelTaskEvaluator.create());

        coordinator.setJobExecutor(jobExecutor);

        DefaultTaskCompletionHandler defaultTaskCompletionHandler = new DefaultTaskCompletionHandler();

        defaultTaskCompletionHandler.setContextService(contextService);
        defaultTaskCompletionHandler.setJobExecutor(jobExecutor);
        defaultTaskCompletionHandler.setJobService(jobService);
        defaultTaskCompletionHandler.setTaskExecutionService(taskExecutionService);
        defaultTaskCompletionHandler.setWorkflowService(workflowService);
        defaultTaskCompletionHandler.setEventPublisher(eventPublisher);
        defaultTaskCompletionHandler.setTaskEvaluator(SpelTaskEvaluator.create());

        TaskCompletionHandlerChain taskCompletionHandlerChain = new TaskCompletionHandlerChain();

        taskCompletionHandlerChain.setTaskCompletionHandlers(Stream.concat(
                        getTaskCompletionHandlers(taskCompletionHandlerChain, taskDispatcherChain).stream(),
                        Stream.of(defaultTaskCompletionHandler))
                .toList());

        coordinator.setTaskCompletionHandler(taskCompletionHandlerChain);

        Job job = coordinator.create(MapObject.of(Map.of("workflowId", workflowId, "inputs", inputs)));

        return jobService.getJob(job.getId());
    }

    private TaskExecutionErrorHandler getJobTaskErrorHandler(TaskDispatcher<?> taskDispatcher) {
        TaskExecutionErrorHandler jobTaskErrorHandler = new TaskExecutionErrorHandler();

        jobTaskErrorHandler.setJobService(jobService);
        jobTaskErrorHandler.setTaskExecutionService(taskExecutionService);
        jobTaskErrorHandler.setTaskDispatcher(taskDispatcher);
        jobTaskErrorHandler.setEventPublisher(e -> {});

        return jobTaskErrorHandler;
    }
}
