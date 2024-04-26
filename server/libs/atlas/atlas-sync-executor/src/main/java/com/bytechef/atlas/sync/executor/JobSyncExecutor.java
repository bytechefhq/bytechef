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
import com.bytechef.atlas.coordinator.event.StartJobEvent;
import com.bytechef.atlas.coordinator.event.TaskExecutionCompleteEvent;
import com.bytechef.atlas.coordinator.event.TaskExecutionErrorEvent;
import com.bytechef.atlas.coordinator.event.listener.ApplicationEventListener;
import com.bytechef.atlas.coordinator.event.listener.TaskStartedApplicationEventListener;
import com.bytechef.atlas.coordinator.job.JobExecutor;
import com.bytechef.atlas.coordinator.message.route.TaskCoordinatorMessageRoute;
import com.bytechef.atlas.coordinator.task.completion.DefaultTaskCompletionHandler;
import com.bytechef.atlas.coordinator.task.completion.TaskCompletionHandlerChain;
import com.bytechef.atlas.coordinator.task.completion.TaskCompletionHandlerFactory;
import com.bytechef.atlas.coordinator.task.dispatcher.DefaultTaskDispatcher;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcherChain;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcherPreSendProcessor;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcherResolver;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcherResolverFactory;
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
import com.bytechef.atlas.worker.message.route.TaskWorkerMessageRoute;
import com.bytechef.atlas.worker.task.factory.TaskDispatcherAdapterFactory;
import com.bytechef.atlas.worker.task.handler.DefaultTaskHandlerResolver;
import com.bytechef.atlas.worker.task.handler.TaskDispatcherAdapterTaskHandlerResolver;
import com.bytechef.atlas.worker.task.handler.TaskHandlerRegistry;
import com.bytechef.atlas.worker.task.handler.TaskHandlerResolverChain;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.error.ExecutionError;
import com.bytechef.message.broker.sync.SyncMessageBroker;
import com.bytechef.message.event.MessageEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;

/**
 * @author Ivica Cardic
 */
public class JobSyncExecutor {

    private static final Logger logger = LoggerFactory.getLogger(JobSyncExecutor.class);

    private final ContextService contextService;
    private final ApplicationEventPublisher eventPublisher;
    private final JobFacade jobFacade;
    private final JobService jobService;
    private final TaskExecutionService taskExecutionService;
    private final TaskFileStorage taskFileStorage;
    private final WorkflowService workflowService;

    public JobSyncExecutor(
        ContextService contextService, JobService jobService, ObjectMapper objectMapper,
        List<TaskDispatcherPreSendProcessor> taskDispatcherPreSendProcessors, TaskExecutionService taskExecutionService,
        TaskHandlerRegistry taskHandlerRegistry, TaskFileStorage taskFileStorage, WorkflowService workflowService) {

        this(
            contextService, jobService, new SyncMessageBroker(objectMapper), List.of(), List.of(),
            taskDispatcherPreSendProcessors, List.of(), taskExecutionService, taskHandlerRegistry, taskFileStorage,
            workflowService);
    }

    @SuppressFBWarnings("EI")
    public JobSyncExecutor(
        ContextService contextService, JobService jobService, SyncMessageBroker syncMessageBroker,
        List<TaskCompletionHandlerFactory> taskCompletionHandlerFactories,
        List<TaskDispatcherAdapterFactory> taskDispatcherAdapterFactories,
        List<TaskDispatcherPreSendProcessor> taskDispatcherPreSendProcessors,
        List<TaskDispatcherResolverFactory> taskDispatcherResolverFactories,
        TaskExecutionService taskExecutionService, TaskHandlerRegistry taskHandlerRegistry,
        TaskFileStorage taskFileStorage, WorkflowService workflowService) {

        this.contextService = contextService;
        this.eventPublisher = createEventPublisher(syncMessageBroker);

        this.jobFacade = new JobFacadeImpl(
            eventPublisher, contextService, jobService, taskExecutionService, taskFileStorage, workflowService);

        this.jobService = jobService;
        this.taskExecutionService = taskExecutionService;
        this.taskFileStorage = taskFileStorage;
        this.workflowService = workflowService;

        syncMessageBroker.receive(
            TaskCoordinatorMessageRoute.ERROR_EVENTS, event -> {
                TaskExecution erroredTaskExecution = ((TaskExecutionErrorEvent) event).getTaskExecution();

                taskExecutionService.update(erroredTaskExecution);

                ExecutionError error = erroredTaskExecution.getError();

                logger.error(error.getMessage());
            });

        TaskHandlerResolverChain taskHandlerResolverChain = new TaskHandlerResolverChain();

        taskHandlerResolverChain.setTaskHandlerResolvers(
            List.of(
                new TaskDispatcherAdapterTaskHandlerResolver(taskDispatcherAdapterFactories, taskHandlerResolverChain),
                new DefaultTaskHandlerResolver(taskHandlerRegistry)));

        TaskWorker worker = new TaskWorker(
            eventPublisher, Executors.newCachedThreadPool(), taskHandlerResolverChain,
            taskFileStorage);

        syncMessageBroker.receive(
            TaskWorkerMessageRoute.TASK_EXECUTION_EVENTS, e -> worker.onTaskExecutionEvent((TaskExecutionEvent) e));

        TaskDispatcherChain taskDispatcherChain = new TaskDispatcherChain();

        taskDispatcherChain.setTaskDispatcherResolvers(
            CollectionUtils.concat(
                getTaskDispatcherResolverStream(taskDispatcherResolverFactories, taskDispatcherChain),
                Stream.of(new DefaultTaskDispatcher(eventPublisher, taskDispatcherPreSendProcessors))));

        JobExecutor jobExecutor = new JobExecutor(
            contextService, taskDispatcherChain, taskExecutionService, taskFileStorage, workflowService);

        DefaultTaskCompletionHandler defaultTaskCompletionHandler = new DefaultTaskCompletionHandler(
            contextService, eventPublisher, jobExecutor, jobService, taskExecutionService,
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
            getApplicationEventListeners(taskExecutionService, jobService), List.of(), eventPublisher, jobExecutor,
            jobService, taskCompletionHandlerChain, taskDispatcherChain, taskExecutionService);

        syncMessageBroker.receive(TaskCoordinatorMessageRoute.APPLICATION_EVENTS,
            event -> taskCoordinator.onApplicationEvent((ApplicationEvent) event));
        syncMessageBroker.receive(
            TaskCoordinatorMessageRoute.TASK_EXECUTION_COMPLETE_EVENTS,
            e -> taskCoordinator.onTaskExecutionCompleteEvent((TaskExecutionCompleteEvent) e));
        syncMessageBroker.receive(TaskCoordinatorMessageRoute.JOB_START_EVENTS,
            e -> taskCoordinator.onStartJobEvent((StartJobEvent) e));
    }

    public Job execute(JobParameters jobParameters) {
        return jobService.getJob(jobFacade.createJob(jobParameters));
    }

    public Job execute(JobParameters jobParameters, JobFactoryFunction jobFactoryFunction) {
        JobFacade jobFacade = new JobFacadeImpl(
            eventPublisher, contextService, new JobServiceWrapper(jobFactoryFunction), taskExecutionService,
            taskFileStorage, workflowService);

        return jobService.getJob(jobFacade.createJob(jobParameters));
    }

    private static ApplicationEventPublisher createEventPublisher(SyncMessageBroker syncMessageBroker) {
        return event -> syncMessageBroker.send(((MessageEvent<?>) event).getRoute(), event);
    }

    private List<ApplicationEventListener> getApplicationEventListeners(
        TaskExecutionService taskExecutionService, JobService jobService) {

        return List.of(new TaskStartedApplicationEventListener(taskExecutionService, task -> {}, jobService));
    }

    private static Stream<TaskDispatcherResolver> getTaskDispatcherResolverStream(
        List<TaskDispatcherResolverFactory> taskDispatcherResolverFactories, TaskDispatcherChain taskDispatcherChain) {

        return taskDispatcherResolverFactories.stream()
            .map(taskDispatcherFactory -> taskDispatcherFactory.createTaskDispatcherResolver(taskDispatcherChain));
    }

    @FunctionalInterface
    public interface JobFactoryFunction {

        Job apply(JobParameters jobParameters);
    }

    private record JobServiceWrapper(JobFactoryFunction jobFactoryFunction)
        implements JobService {

        @Override
        public Job getJob(long id) {
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
        public List<Job> getWorkflowJobs(String workflowId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Job create(JobParameters jobParameters, Workflow workflow) {
            return jobFactoryFunction.apply(jobParameters);
        }

        @Override
        public void deleteJob(long id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<Job> fetchLastJob() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<Job> fetchLastWorkflowJob(String workflowId) {
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

        @Override
        public void updateWorkflowId(String curWorkflowId, String newWorkflowId) {
            throw new UnsupportedOperationException();
        }
    }
}
