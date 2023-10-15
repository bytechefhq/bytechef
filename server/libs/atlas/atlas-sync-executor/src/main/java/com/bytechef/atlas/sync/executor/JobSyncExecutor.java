/*
 * Copyright 2023-present ByteChef Inc.
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

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.coordinator.TaskCoordinator;
import com.bytechef.atlas.coordinator.event.ApplicationEvent;
import com.bytechef.atlas.coordinator.event.JobStartEvent;
import com.bytechef.atlas.coordinator.event.TaskExecutionCompleteEvent;
import com.bytechef.atlas.coordinator.event.TaskExecutionErrorEvent;
import com.bytechef.atlas.coordinator.event.listener.ApplicationEventListener;
import com.bytechef.atlas.coordinator.job.JobExecutor;
import com.bytechef.atlas.coordinator.message.route.CoordinatorMessageRoute;
import com.bytechef.atlas.coordinator.task.completion.DefaultTaskCompletionHandler;
import com.bytechef.atlas.coordinator.task.completion.TaskCompletionHandlerChain;
import com.bytechef.atlas.coordinator.task.completion.TaskCompletionHandlerFactory;
import com.bytechef.atlas.coordinator.task.dispatcher.DefaultTaskDispatcher;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcherChain;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcherResolver;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcherResolverFactory;
import com.bytechef.atlas.execution.domain.Context;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.dto.JobParameters;
import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.atlas.execution.facade.JobFacadeImpl;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.atlas.worker.TaskWorker;
import com.bytechef.atlas.worker.event.TaskExecutionEvent;
import com.bytechef.atlas.worker.message.route.WorkerMessageRoute;
import com.bytechef.atlas.worker.task.factory.TaskDispatcherAdapterFactory;
import com.bytechef.atlas.worker.task.handler.DefaultTaskHandlerResolver;
import com.bytechef.atlas.worker.task.handler.TaskDispatcherAdapterTaskHandlerResolver;
import com.bytechef.atlas.worker.task.handler.TaskHandlerRegistry;
import com.bytechef.atlas.worker.task.handler.TaskHandlerResolverChain;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.error.ExecutionError;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.message.broker.sync.SyncMessageBroker;
import com.bytechef.message.event.MessageEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.lang.NonNull;

/**
 * @author Ivica Cardic
 */
public class JobSyncExecutor {

    private static final Logger logger = LoggerFactory.getLogger(JobSyncExecutor.class);

    private final JobFacade jobFacade;
    private final JobService jobService;

    public JobSyncExecutor(
        @NonNull ContextService contextService, @NonNull JobService jobService,
        @NonNull ObjectMapper objectMapper, @NonNull TaskExecutionService taskExecutionService,
        @NonNull TaskHandlerRegistry taskHandlerRegistry, @NonNull TaskFileStorage taskFileStorage,
        @NonNull WorkflowService workflowService) {

        this(
            List.of(), contextService, jobService, new SyncMessageBroker(objectMapper), List.of(),
            List.of(), List.of(), taskExecutionService, taskHandlerRegistry, taskFileStorage,
            workflowService);
    }

    @SuppressFBWarnings("EI")
    public JobSyncExecutor(
        @NonNull List<ApplicationEventListener> applicationEventListeners, @NonNull ContextService contextService,
        @NonNull JobService jobService, @NonNull SyncMessageBroker syncMessageBroker,
        @NonNull List<TaskCompletionHandlerFactory> taskCompletionHandlerFactories,
        @NonNull List<TaskDispatcherAdapterFactory> taskDispatcherAdapterFactories,
        @NonNull List<TaskDispatcherResolverFactory> taskDispatcherResolverFactories,
        @NonNull TaskExecutionService taskExecutionService, @NonNull TaskHandlerRegistry taskHandlerRegistry,
        @NonNull TaskFileStorage taskFileStorage, @NonNull WorkflowService workflowService) {

        this.jobService = jobService;
        this.jobFacade = new JobFacadeImpl(
            getEventPublisher(syncMessageBroker), new ContextServiceImpl(contextService),
            new JobServiceImpl(jobService), taskFileStorage, workflowService);

        syncMessageBroker.receive(
            CoordinatorMessageRoute.ERROR_EVENTS, event -> {
                TaskExecution erroredTaskExecution = ((TaskExecutionErrorEvent) event).getTaskExecution();

                ExecutionError error = erroredTaskExecution.getError();

                logger.error(error.getMessage());
            });

        TaskHandlerResolverChain taskHandlerResolverChain = new TaskHandlerResolverChain();

        taskHandlerResolverChain.setTaskHandlerResolvers(
            List.of(
                new TaskDispatcherAdapterTaskHandlerResolver(taskDispatcherAdapterFactories, taskHandlerResolverChain),
                new DefaultTaskHandlerResolver(taskHandlerRegistry)));

        TaskWorker worker = new TaskWorker(
            getEventPublisher(syncMessageBroker), Executors.newCachedThreadPool(), taskHandlerResolverChain,
            taskFileStorage);

        syncMessageBroker.receive(
            WorkerMessageRoute.TASK_EXECUTION_EVENTS, e -> worker.onTaskExecutionEvent((TaskExecutionEvent) e));

        TaskDispatcherChain taskDispatcherChain = new TaskDispatcherChain();

        taskDispatcherChain.setTaskDispatcherResolvers(
            CollectionUtils.concat(
                getTaskDispatcherResolverStream(taskDispatcherResolverFactories, taskDispatcherChain),
                Stream.of(new DefaultTaskDispatcher(getEventPublisher(syncMessageBroker), List.of()))));

        JobExecutor jobExecutor = new JobExecutor(
            contextService, taskDispatcherChain, taskExecutionService, taskFileStorage, workflowService);

        DefaultTaskCompletionHandler defaultTaskCompletionHandler = new DefaultTaskCompletionHandler(
            contextService, getEventPublisher(syncMessageBroker), jobExecutor, jobService, taskExecutionService,
            taskFileStorage,
            workflowService);

        TaskCompletionHandlerChain taskCompletionHandlerChain = new TaskCompletionHandlerChain();

        taskCompletionHandlerChain.setTaskCompletionHandlers(
            CollectionUtils.concat(
                taskCompletionHandlerFactories.stream()
                    .map(taskCompletionHandlerFactory -> taskCompletionHandlerFactory.createTaskCompletionHandler(
                        taskCompletionHandlerChain, taskDispatcherChain)),
                Stream.of(defaultTaskCompletionHandler)));

        TaskCoordinator taskCoordinator = new TaskCoordinator(
            applicationEventListeners, List.of(), getEventPublisher(syncMessageBroker), jobExecutor, jobService,
            taskCompletionHandlerChain, taskDispatcherChain, taskExecutionService);

        syncMessageBroker.receive(CoordinatorMessageRoute.APPLICATION_EVENTS,
            event -> taskCoordinator.onApplicationEvent((ApplicationEvent) event));
        syncMessageBroker.receive(
            CoordinatorMessageRoute.TASK_EXECUTION_COMPLETE_EVENTS,
            e -> taskCoordinator.onTaskExecutionCompleteEvent((TaskExecutionCompleteEvent) e));
        syncMessageBroker.receive(CoordinatorMessageRoute.JOB_START_EVENTS,
            e -> taskCoordinator.onJobStartEvent((JobStartEvent) e));
    }

    public Job execute(JobParameters jobParameters) {
        long jobId = jobFacade.createJob(jobParameters);

        return jobService.getJob(jobId);
    }

    private static ApplicationEventPublisher getEventPublisher(SyncMessageBroker syncMessageBroker) {
        return event -> syncMessageBroker.send(((MessageEvent<?>) event).getRoute(), event);
    }

    private static Stream<TaskDispatcherResolver> getTaskDispatcherResolverStream(
        List<TaskDispatcherResolverFactory> taskDispatcherResolverFactories, TaskDispatcherChain taskDispatcherChain) {

        return taskDispatcherResolverFactories.stream()
            .map(taskDispatcherFactory -> taskDispatcherFactory.createTaskDispatcherResolver(taskDispatcherChain));
    }

    private record ContextServiceImpl(ContextService contextService) implements ContextService {

        @Override
        public FileEntry peek(long stackId, Context.Classname classname) {
            throw new UnsupportedOperationException();
        }

        @Override
        public FileEntry peek(long stackId, int subStackId, Context.Classname classname) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void push(long stackId, Context.Classname classname, FileEntry value) {
            contextService.push(stackId, classname, value);
        }

        @Override
        public void push(long stackId, int subStackId, Context.Classname classname, FileEntry value) {
            throw new UnsupportedOperationException();
        }
    }

    private record JobServiceImpl(JobService JobService) implements JobService {

        @Override
        public Job getJob(long id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Job> getJobs(
            String status, LocalDateTime startDate, LocalDateTime endDate, List<String> workflowIds) {

            throw new UnsupportedOperationException();
        }

        @Override
        public Page<Job> getJobsPage(int pageNumber) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Job getTaskExecutionJob(long taskExecutionId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public long countJobs(
            String jobStatus, LocalDateTime jobStartDate, LocalDateTime jobEndDate, List<String> projectWorkflowIds) {

            throw new UnsupportedOperationException();
        }

        @Override
        public Job create(JobParameters jobParameters, Workflow workflow) {
            return JobService.create(jobParameters, workflow);
        }

        @Override
        public Optional<Job> fetchLatestJob() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Page<Job> getJobsPage(
            String status, LocalDateTime startDate, LocalDateTime endDate,
            List<String> workflowIds, Integer pageNumber) {

            throw new UnsupportedOperationException();
        }

        @Override
        public Job resumeToStatusStarted(long id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Job setStatusToStarted(long id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Job setStatusToStopped(long id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Job update(Job job) {
            throw new UnsupportedOperationException();
        }
    }
}
