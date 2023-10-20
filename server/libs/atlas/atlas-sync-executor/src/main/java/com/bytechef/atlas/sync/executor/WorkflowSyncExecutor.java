
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
import com.bytechef.atlas.worker.task.handler.TaskDispatcherAdapterFactory;
import com.bytechef.atlas.worker.task.handler.TaskDispatcherAdapterTaskHandlerResolver;
import com.bytechef.atlas.worker.task.handler.TaskHandlerAccessor;
import com.bytechef.atlas.worker.task.handler.TaskHandlerResolverChain;

import java.util.Collections;
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

    private final JobFactory jobFactory;
    private final JobService jobService;

    @SuppressFBWarnings("EI")
    private WorkflowSyncExecutor(Builder builder) {
        this.jobService = builder.jobService;

        SyncMessageBroker syncMessageBroker = builder.syncMessageBroker;

        if (syncMessageBroker == null) {
            syncMessageBroker = new SyncMessageBroker();
        }

        syncMessageBroker.receive(Queues.ERRORS, message -> {
            TaskExecution erroredTaskExecution = (TaskExecution) message;

            ExecutionError error = erroredTaskExecution.getError();

            logger.error(error.getMessage());
        });

        TaskEvaluator taskEvaluator = TaskEvaluator.create();
        TaskHandlerResolverChain taskHandlerResolverChain = new TaskHandlerResolverChain();

        taskHandlerResolverChain.setTaskHandlerResolvers(
            List.of(
                new TaskDispatcherAdapterTaskHandlerResolver(
                    builder.taskDispatcherAdapterFactories, taskHandlerResolverChain, taskEvaluator),
                new DefaultTaskHandlerResolver(builder.taskHandlerAccessor)));

        Worker worker = Worker.builder()
            .taskHandlerResolver(taskHandlerResolverChain)
            .messageBroker(syncMessageBroker)
            .eventPublisher(builder.eventPublisher)
            .taskEvaluator(taskEvaluator)
            .build();

        syncMessageBroker.receive(Queues.TASKS, o -> worker.handle((TaskExecution) o));

        TaskDispatcherChain taskDispatcherChain = new TaskDispatcherChain();

        taskDispatcherChain.setTaskDispatcherResolvers(
            CollectionUtils.concat(
                builder.taskDispatcherResolverFactories.stream()
                    .map(taskDispatcherFactory -> taskDispatcherFactory
                        .createTaskDispatcherResolver(taskDispatcherChain)),
                Stream.of(new DefaultTaskDispatcher(syncMessageBroker, List.of()))));

        JobExecutor jobExecutor = new JobExecutor(
            builder.contextService, taskDispatcherChain, builder.taskExecutionService, taskEvaluator,
            builder.workflowService);

        DefaultTaskCompletionHandler defaultTaskCompletionHandler = new DefaultTaskCompletionHandler(
            builder.contextService, builder.eventPublisher, jobExecutor, jobService, taskEvaluator,
            builder.taskExecutionService, builder.workflowService);

        TaskCompletionHandlerChain taskCompletionHandlerChain = new TaskCompletionHandlerChain();

        taskCompletionHandlerChain.setTaskCompletionHandlers(
            CollectionUtils.concat(
                builder.taskCompletionHandlerFactories.stream()
                    .map(taskCompletionHandlerFactory -> taskCompletionHandlerFactory.createTaskCompletionHandler(
                        taskCompletionHandlerChain, taskDispatcherChain)),
                Stream.of(defaultTaskCompletionHandler)));

        jobFactory = new JobFactoryImpl(builder.contextService, builder.eventPublisher, jobService, syncMessageBroker);

        @SuppressWarnings({
            "rawtypes", "unchecked"
        })
        Coordinator coordinator = Coordinator.builder()
            .errorHandler((ErrorHandler) new TaskExecutionErrorHandler(
                builder.eventPublisher, jobService, taskDispatcherChain, builder.taskExecutionService))
            .eventPublisher(builder.eventPublisher)
            .jobExecutor(jobExecutor)
            .jobFactory(jobFactory)
            .jobService(jobService)
            .taskCompletionHandler(taskCompletionHandlerChain)
            .taskDispatcher(taskDispatcherChain)
            .taskExecutionService(builder.taskExecutionService)
            .build();

        syncMessageBroker.receive(Queues.COMPLETIONS, o -> coordinator.complete((TaskExecution) o));
        syncMessageBroker.receive(Queues.JOBS, jobId -> coordinator.start((Long) jobId));
    }

    public static Builder builder() {
        return new Builder();
    }

    public Job execute(String workflowId) {
        return execute(workflowId, Map.of());
    }

    public Job execute(String workflowId, Map<String, Object> inputs) {
        long jobId = jobFactory.create(new JobParametersDTO(inputs, workflowId));

        return jobService.getJob(jobId);
    }

    @SuppressFBWarnings("EI")
    public static final class Builder {

        private ContextService contextService;
        private EventPublisher eventPublisher;
        private JobService jobService;
        private SyncMessageBroker syncMessageBroker;
        private List<TaskCompletionHandlerFactory> taskCompletionHandlerFactories = Collections.emptyList();
        public List<TaskDispatcherAdapterFactory> taskDispatcherAdapterFactories = Collections.emptyList();
        private List<TaskDispatcherResolverFactory> taskDispatcherResolverFactories = Collections.emptyList();
        private TaskExecutionService taskExecutionService;
        private TaskHandlerAccessor taskHandlerAccessor;
        private WorkflowService workflowService;

        private Builder() {
        }

        public Builder contextService(ContextService contextService) {
            this.contextService = contextService;
            return this;
        }

        public Builder eventPublisher(EventPublisher eventPublisher) {
            this.eventPublisher = eventPublisher;
            return this;
        }

        public Builder jobService(JobService jobService) {
            this.jobService = jobService;
            return this;
        }

        public Builder syncMessageBroker(SyncMessageBroker syncMessageBroker) {
            this.syncMessageBroker = syncMessageBroker;
            return this;
        }

        public Builder taskDispatcherAdapterFactories(
            List<TaskDispatcherAdapterFactory> taskDispatcherAdapterFactories) {
            this.taskDispatcherAdapterFactories = taskDispatcherAdapterFactories;
            return this;
        }

        public Builder taskCompletionHandlerFactories(
            List<TaskCompletionHandlerFactory> taskCompletionHandlerFactories) {
            this.taskCompletionHandlerFactories = taskCompletionHandlerFactories;
            return this;
        }

        public Builder taskExecutionService(TaskExecutionService taskExecutionService) {
            this.taskExecutionService = taskExecutionService;
            return this;
        }

        public Builder taskHandlerAccessor(TaskHandlerAccessor taskHandlerAccessor) {
            this.taskHandlerAccessor = taskHandlerAccessor;
            return this;
        }

        public Builder taskDispatcherResolverFactories(
            List<TaskDispatcherResolverFactory> taskDispatcherResolverFactories) {
            this.taskDispatcherResolverFactories = taskDispatcherResolverFactories;

            return this;
        }

        public Builder workflowService(WorkflowService workflowService) {
            this.workflowService = workflowService;
            return this;
        }

        public WorkflowSyncExecutor build() {
            return new WorkflowSyncExecutor(this);
        }
    }
}
