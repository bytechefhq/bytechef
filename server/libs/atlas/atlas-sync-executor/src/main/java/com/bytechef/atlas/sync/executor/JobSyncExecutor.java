
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

import com.bytechef.atlas.coordinator.TaskCoordinator;
import com.bytechef.atlas.coordinator.job.JobExecutor;
import com.bytechef.atlas.coordinator.task.completion.DefaultTaskCompletionHandler;
import com.bytechef.atlas.coordinator.task.completion.TaskCompletionHandlerChain;
import com.bytechef.atlas.coordinator.task.completion.TaskCompletionHandlerFactory;
import com.bytechef.atlas.coordinator.task.dispatcher.DefaultTaskDispatcher;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcherChain;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcherResolverFactory;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.dto.JobParameters;
import com.bytechef.atlas.file.storage.WorkflowFileStorage;
import com.bytechef.atlas.execution.message.broker.TaskMessageRoute;
import com.bytechef.atlas.worker.TaskWorker;
import com.bytechef.error.ExecutionError;
import com.bytechef.event.EventPublisher;
import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.atlas.execution.facade.JobFacadeImpl;
import com.bytechef.message.broker.SystemMessageRoute;
import com.bytechef.message.broker.sync.SyncMessageBroker;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.worker.task.handler.DefaultTaskHandlerResolver;
import com.bytechef.atlas.worker.task.factory.TaskDispatcherAdapterFactory;
import com.bytechef.atlas.worker.task.handler.TaskDispatcherAdapterTaskHandlerResolver;
import com.bytechef.atlas.worker.task.handler.TaskHandlerRegistry;
import com.bytechef.atlas.worker.task.handler.TaskHandlerResolverChain;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import com.bytechef.commons.util.CollectionUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Ivica Cardic
 */
public class JobSyncExecutor {

    private static final Logger logger = LoggerFactory.getLogger(JobSyncExecutor.class);

    private final JobFacade jobFacade;
    private final JobService jobService;

    @SuppressFBWarnings("EI")
    private JobSyncExecutor(Builder builder) {
        this.jobService = builder.jobService;

        SyncMessageBroker syncMessageBroker = builder.syncMessageBroker;

        if (syncMessageBroker == null) {
            syncMessageBroker = new SyncMessageBroker();
        }

        this.jobFacade = new JobFacadeImpl(
            builder.contextService, builder.eventPublisher, jobService, syncMessageBroker,
            builder.workflowFileStorage, builder.workflowService);

        syncMessageBroker.receive(SystemMessageRoute.ERRORS, message -> {
            TaskExecution erroredTaskExecution = (TaskExecution) message;

            ExecutionError error = erroredTaskExecution.getError();

            logger.error(error.getMessage());
        });

        TaskHandlerResolverChain taskHandlerResolverChain = new TaskHandlerResolverChain();

        taskHandlerResolverChain.setTaskHandlerResolvers(
            List.of(
                new TaskDispatcherAdapterTaskHandlerResolver(
                    builder.taskDispatcherAdapterFactories, taskHandlerResolverChain),
                new DefaultTaskHandlerResolver(builder.taskHandlerRegistry)));

        TaskWorker worker = new TaskWorker(
            builder.eventPublisher, syncMessageBroker, taskHandlerResolverChain, builder.workflowFileStorage);

        syncMessageBroker.receive(TaskMessageRoute.TASKS, o -> worker.handle((TaskExecution) o));

        TaskDispatcherChain taskDispatcherChain = new TaskDispatcherChain();

        taskDispatcherChain.setTaskDispatcherResolvers(
            CollectionUtils.concat(
                builder.taskDispatcherResolverFactories.stream()
                    .map(taskDispatcherFactory -> taskDispatcherFactory
                        .createTaskDispatcherResolver(taskDispatcherChain)),
                Stream.of(new DefaultTaskDispatcher(syncMessageBroker, List.of()))));

        JobExecutor jobExecutor = new JobExecutor(
            builder.contextService, taskDispatcherChain, builder.taskExecutionService,
            builder.workflowFileStorage, builder.workflowService);

        DefaultTaskCompletionHandler defaultTaskCompletionHandler = new DefaultTaskCompletionHandler(
            builder.contextService, builder.eventPublisher, jobExecutor, jobService,
            builder.taskExecutionService, builder.workflowFileStorage, builder.workflowService);

        TaskCompletionHandlerChain taskCompletionHandlerChain = new TaskCompletionHandlerChain();

        taskCompletionHandlerChain.setTaskCompletionHandlers(
            CollectionUtils.concat(
                builder.taskCompletionHandlerFactories.stream()
                    .map(taskCompletionHandlerFactory -> taskCompletionHandlerFactory.createTaskCompletionHandler(
                        taskCompletionHandlerChain, taskDispatcherChain)),
                Stream.of(defaultTaskCompletionHandler)));

        TaskCoordinator taskCoordinator = new TaskCoordinator(
            builder.eventPublisher, jobExecutor, jobFacade, jobService, syncMessageBroker,
            taskCompletionHandlerChain, taskDispatcherChain, builder.taskExecutionService);

        syncMessageBroker.receive(
            TaskMessageRoute.TASKS_COMPLETE, o -> taskCoordinator.handleTasksComplete((TaskExecution) o));
        syncMessageBroker.receive(TaskMessageRoute.JOBS_START, jobId -> taskCoordinator.handleJobsStart((Long) jobId));
    }

    public static Builder builder() {
        return new Builder();
    }

    public Job execute(JobParameters jobParameters) {
        long jobId = jobFacade.createJob(jobParameters);

        return jobService.getJob(jobId);
    }

    @SuppressFBWarnings("EI")
    public static final class Builder {

        private ContextService contextService;
        private EventPublisher eventPublisher;
        private JobService jobService;
        private SyncMessageBroker syncMessageBroker;
        private List<TaskCompletionHandlerFactory> taskCompletionHandlerFactories = Collections.emptyList();
        private List<TaskDispatcherAdapterFactory> taskDispatcherAdapterFactories = Collections.emptyList();
        private List<TaskDispatcherResolverFactory> taskDispatcherResolverFactories = Collections.emptyList();
        private TaskExecutionService taskExecutionService;
        private TaskHandlerRegistry taskHandlerRegistry;
        private WorkflowFileStorage workflowFileStorage;
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

        public Builder taskHandlerRegistry(TaskHandlerRegistry taskHandlerRegistry) {
            this.taskHandlerRegistry = taskHandlerRegistry;

            return this;
        }

        public Builder taskDispatcherResolverFactories(
            List<TaskDispatcherResolverFactory> taskDispatcherResolverFactories) {

            this.taskDispatcherResolverFactories = taskDispatcherResolverFactories;

            return this;
        }

        public Builder workflowFileStorageFacade(WorkflowFileStorage workflowFileStorage) {
            this.workflowFileStorage = workflowFileStorage;

            return this;
        }

        public Builder workflowService(WorkflowService workflowService) {
            this.workflowService = workflowService;

            return this;
        }

        public JobSyncExecutor build() {
            return new JobSyncExecutor(this);
        }
    }
}
