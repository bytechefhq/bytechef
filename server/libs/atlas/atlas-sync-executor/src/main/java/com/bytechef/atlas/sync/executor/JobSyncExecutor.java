
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

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.coordinator.TaskCoordinator;
import com.bytechef.atlas.coordinator.job.JobExecutor;
import com.bytechef.atlas.coordinator.task.completion.DefaultTaskCompletionHandler;
import com.bytechef.atlas.coordinator.task.completion.TaskCompletionHandlerChain;
import com.bytechef.atlas.coordinator.task.completion.TaskCompletionHandlerFactory;
import com.bytechef.atlas.coordinator.task.dispatcher.DefaultTaskDispatcher;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcherChain;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcherResolverFactory;
import com.bytechef.atlas.execution.domain.Context;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.dto.JobParameters;
import com.bytechef.atlas.execution.facade.RemoteJobFacade;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.file.storage.facade.WorkflowFileStorageFacade;
import com.bytechef.atlas.execution.message.broker.TaskMessageRoute;
import com.bytechef.atlas.worker.TaskWorker;
import com.bytechef.error.ExecutionError;
import com.bytechef.event.EventPublisher;
import com.bytechef.atlas.execution.facade.JobFacadeImpl;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.message.broker.SystemMessageRoute;
import com.bytechef.message.broker.sync.SyncMessageBroker;
import com.bytechef.atlas.execution.service.RemoteContextService;
import com.bytechef.atlas.execution.service.RemoteJobService;
import com.bytechef.atlas.execution.service.RemoteTaskExecutionService;
import com.bytechef.atlas.configuration.service.RemoteWorkflowService;
import com.bytechef.atlas.worker.task.handler.DefaultTaskHandlerResolver;
import com.bytechef.atlas.worker.task.factory.TaskDispatcherAdapterFactory;
import com.bytechef.atlas.worker.task.handler.TaskDispatcherAdapterTaskHandlerResolver;
import com.bytechef.atlas.worker.task.handler.TaskHandlerRegistry;
import com.bytechef.atlas.worker.task.handler.TaskHandlerResolverChain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import com.bytechef.commons.util.CollectionUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;

/**
 * @author Ivica Cardic
 */
public class JobSyncExecutor {

    private static final Logger logger = LoggerFactory.getLogger(JobSyncExecutor.class);

    private final RemoteJobFacade jobFacade;
    private final RemoteJobService jobService;

    public JobSyncExecutor(
        RemoteContextService contextService, EventPublisher eventPublisher, RemoteJobService jobService,
        ObjectMapper objectMapper, RemoteTaskExecutionService taskExecutionService,
        TaskHandlerRegistry taskHandlerRegistry, WorkflowFileStorageFacade workflowFileStorageFacade,
        RemoteWorkflowService workflowService) {

        this(
            contextService, eventPublisher, jobService, objectMapper, null, List.of(), List.of(), List.of(),
            taskExecutionService, taskHandlerRegistry, workflowFileStorageFacade, workflowService);
    }

    @SuppressFBWarnings("EI")
    public JobSyncExecutor(
        RemoteContextService contextService, EventPublisher eventPublisher, RemoteJobService jobService,
        ObjectMapper objectMapper, SyncMessageBroker syncMessageBroker,
        List<TaskCompletionHandlerFactory> taskCompletionHandlerFactories,
        List<TaskDispatcherAdapterFactory> taskDispatcherAdapterFactories,
        List<TaskDispatcherResolverFactory> taskDispatcherResolverFactories,
        RemoteTaskExecutionService taskExecutionService,
        TaskHandlerRegistry taskHandlerRegistry, WorkflowFileStorageFacade workflowFileStorageFacade,
        RemoteWorkflowService workflowService) {

        this.jobService = jobService;

        if (syncMessageBroker == null) {
            syncMessageBroker = new SyncMessageBroker(objectMapper);
        }

        this.jobFacade = new JobFacadeImpl(
            new ContextServiceImpl(contextService), eventPublisher, new JobServiceImpl(jobService), syncMessageBroker,
            workflowFileStorageFacade, workflowService);

        syncMessageBroker.receive(SystemMessageRoute.ERRORS, message -> {
            TaskExecution erroredTaskExecution = (TaskExecution) message;

            ExecutionError error = erroredTaskExecution.getError();

            logger.error(error.getMessage());
        });

        TaskHandlerResolverChain taskHandlerResolverChain = new TaskHandlerResolverChain();

        taskHandlerResolverChain.setTaskHandlerResolvers(
            List.of(
                new TaskDispatcherAdapterTaskHandlerResolver(taskDispatcherAdapterFactories, taskHandlerResolverChain),
                new DefaultTaskHandlerResolver(taskHandlerRegistry)));

        TaskWorker worker = new TaskWorker(
            eventPublisher, syncMessageBroker, taskHandlerResolverChain, workflowFileStorageFacade);

        syncMessageBroker.receive(TaskMessageRoute.TASKS, o -> worker.handle((TaskExecution) o));

        TaskDispatcherChain taskDispatcherChain = new TaskDispatcherChain();

        taskDispatcherChain.setTaskDispatcherResolvers(
            CollectionUtils.concat(
                taskDispatcherResolverFactories.stream()
                    .map(taskDispatcherFactory -> taskDispatcherFactory
                        .createTaskDispatcherResolver(taskDispatcherChain)),
                Stream.of(new DefaultTaskDispatcher(syncMessageBroker, List.of()))));

        JobExecutor jobExecutor = new JobExecutor(
            contextService, taskDispatcherChain, taskExecutionService, workflowFileStorageFacade, workflowService);

        DefaultTaskCompletionHandler defaultTaskCompletionHandler = new DefaultTaskCompletionHandler(
            contextService, eventPublisher, jobExecutor, jobService, taskExecutionService, workflowFileStorageFacade,
            workflowService);

        TaskCompletionHandlerChain taskCompletionHandlerChain = new TaskCompletionHandlerChain();

        taskCompletionHandlerChain.setTaskCompletionHandlers(
            CollectionUtils.concat(
                taskCompletionHandlerFactories.stream()
                    .map(taskCompletionHandlerFactory -> taskCompletionHandlerFactory.createTaskCompletionHandler(
                        taskCompletionHandlerChain, taskDispatcherChain)),
                Stream.of(defaultTaskCompletionHandler)));

        TaskCoordinator taskCoordinator = new TaskCoordinator(
            eventPublisher, jobExecutor, jobFacade, jobService, syncMessageBroker, taskCompletionHandlerChain,
            taskDispatcherChain, taskExecutionService);

        syncMessageBroker.receive(
            TaskMessageRoute.TASKS_COMPLETE, o -> taskCoordinator.handleTasksComplete((TaskExecution) o));
        syncMessageBroker.receive(TaskMessageRoute.JOBS_START, jobId -> taskCoordinator.handleJobsStart((Long) jobId));
    }

    public Job execute(JobParameters jobParameters) {
        long jobId = jobFacade.createJob(jobParameters);

        return jobService.getJob(jobId);
    }

    private record ContextServiceImpl(RemoteContextService remoteContextService) implements ContextService {

        @Override
        public void push(long stackId, Context.Classname classname, FileEntry value) {
            remoteContextService.push(stackId, classname, value);
        }
    }

    private record JobServiceImpl(RemoteJobService remoteJobService) implements JobService {

        @Override
        public Job getJob(long id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Page<Job> getJobs(int pageNumber) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Job create(JobParameters jobParameters, Workflow workflow) {
            return remoteJobService.create(jobParameters, workflow);
        }

        @Override
        public Optional<Job> fetchLatestJob() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Page<Job> getJobs(
            String status, LocalDateTime startDate, LocalDateTime endDate, String workflowId,
            List<String> workflowIds, Integer pageNumber) {

            throw new UnsupportedOperationException();
        }
    }
}
