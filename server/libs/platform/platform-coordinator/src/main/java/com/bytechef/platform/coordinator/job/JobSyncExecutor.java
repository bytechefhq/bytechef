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

package com.bytechef.platform.coordinator.job;

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
import com.bytechef.atlas.execution.dto.JobParametersDTO;
import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.atlas.execution.facade.JobFacadeImpl;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.atlas.worker.TaskWorker;
import com.bytechef.atlas.worker.event.TaskExecutionEvent;
import com.bytechef.atlas.worker.message.route.TaskWorkerMessageRoute;
import com.bytechef.atlas.worker.task.handler.DefaultTaskHandlerResolver;
import com.bytechef.atlas.worker.task.handler.TaskDispatcherAdapterFactory;
import com.bytechef.atlas.worker.task.handler.TaskDispatcherAdapterTaskHandlerResolver;
import com.bytechef.atlas.worker.task.handler.TaskHandlerRegistry;
import com.bytechef.atlas.worker.task.handler.TaskHandlerResolverChain;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.error.ExecutionError;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.exception.ExecutionException;
import com.bytechef.message.broker.sync.SyncMessageBroker;
import com.bytechef.message.event.MessageEvent;
import com.bytechef.platform.coordinator.job.exception.TaskExecutionErrorType;
import com.bytechef.platform.definition.WorkflowNodeType;
import com.bytechef.platform.webhook.executor.constant.WebhookConstants;
import com.bytechef.tenant.TenantContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Stream;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.thread.Threading;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.env.Environment;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.data.domain.Page;

/**
 * @author Ivica Cardic
 */
public class JobSyncExecutor {

    private static final Logger logger = LoggerFactory.getLogger(JobSyncExecutor.class);

    private static final List<String> WEBHOOK_COMPONENTS = List.of("apiPlatform", "chat", "webhook");

    private final ContextService contextService;
    private final ApplicationEventPublisher eventPublisher;
    private final JobFacade jobFacade;
    private final JobService jobService;
    private final TaskExecutionService taskExecutionService;
    private final TaskFileStorage taskFileStorage;
    private final WorkflowService workflowService;

    public JobSyncExecutor(
        ContextService contextService, Environment environment, Evaluator evaluator, JobService jobService,
        List<TaskDispatcherPreSendProcessor> taskDispatcherPreSendProcessors, TaskExecutionService taskExecutionService,
        TaskHandlerRegistry taskHandlerRegistry, TaskFileStorage taskFileStorage, WorkflowService workflowService) {

        this(
            contextService, environment, evaluator, jobService, new SyncMessageBroker(), List.of(), List.of(),
            taskDispatcherPreSendProcessors, List.of(), taskExecutionService, taskHandlerRegistry, taskFileStorage,
            workflowService);
    }

    @SuppressFBWarnings("EI")
    public JobSyncExecutor(
        ContextService contextService, Environment environment, Evaluator evaluator, JobService jobService,
        SyncMessageBroker syncMessageBroker, List<TaskCompletionHandlerFactory> taskCompletionHandlerFactories,
        List<TaskDispatcherAdapterFactory> taskDispatcherAdapterFactories,
        List<TaskDispatcherPreSendProcessor> taskDispatcherPreSendProcessors,
        List<TaskDispatcherResolverFactory> taskDispatcherResolverFactories, TaskExecutionService taskExecutionService,
        TaskHandlerRegistry taskHandlerRegistry, TaskFileStorage taskFileStorage, WorkflowService workflowService) {

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
                if (erroredTaskExecution.getError() != null) {
                    erroredTaskExecution.setStatus(TaskExecution.Status.FAILED);
                }

                taskExecutionService.update(erroredTaskExecution);

                ExecutionError error = erroredTaskExecution.getError();

                logger.error(error.getMessage());
            });

        syncMessageBroker.receive(TaskCoordinatorMessageRoute.JOB_STOP_EVENTS, event -> {});

        TaskHandlerResolverChain taskHandlerResolverChain = new TaskHandlerResolverChain();

        taskHandlerResolverChain.setTaskHandlerResolvers(
            List.of(
                new TaskDispatcherAdapterTaskHandlerResolver(taskDispatcherAdapterFactories, taskHandlerResolverChain),
                new DefaultTaskHandlerResolver(taskHandlerRegistry)));

        TaskWorker worker = new TaskWorker(
            evaluator, eventPublisher, new JobSyncAsyncTaskExecutor(environment), taskHandlerResolverChain,
            taskFileStorage);

        syncMessageBroker.receive(
            TaskWorkerMessageRoute.TASK_EXECUTION_EVENTS, e -> worker.onTaskExecutionEvent((TaskExecutionEvent) e));

        TaskDispatcherChain taskDispatcherChain = new TaskDispatcherChain();

        taskDispatcherChain.setTaskDispatcherResolvers(
            CollectionUtils.concat(
                getTaskDispatcherResolverStream(taskDispatcherResolverFactories, taskDispatcherChain),
                Stream.of(new DefaultTaskDispatcher(eventPublisher, taskDispatcherPreSendProcessors))));

        JobExecutor jobExecutor = new JobExecutor(
            contextService, evaluator, taskDispatcherChain, taskExecutionService, taskFileStorage, workflowService);

        DefaultTaskCompletionHandler defaultTaskCompletionHandler = new DefaultTaskCompletionHandler(
            contextService, evaluator, eventPublisher, jobExecutor, jobService, taskExecutionService, taskFileStorage,
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

    public Job execute(JobParametersDTO jobParametersDTO) {
        return execute(jobParametersDTO, jobFacade);
    }

    public Job execute(JobParametersDTO jobParametersDTO, JobFactoryFunction jobFactoryFunction) {
        JobFacade jobFacade = new JobFacadeImpl(
            eventPublisher, contextService, new JobServiceWrapper(jobFactoryFunction), taskExecutionService,
            taskFileStorage, workflowService);

        return execute(jobParametersDTO, jobFacade);
    }

    private static ApplicationEventPublisher createEventPublisher(SyncMessageBroker syncMessageBroker) {
        return event -> syncMessageBroker.send(((MessageEvent<?>) event).getRoute(), event);
    }

    private Job execute(JobParametersDTO jobParametersDTO, JobFacade jobFacade) {
        Job job = jobService.getJob(jobFacade.createJob(jobParametersDTO));

        checkForError(job);

        return checkForWebhookResponse(job);
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

        Job apply(JobParametersDTO jobParametersDTO);
    }

    private void checkForError(Job job) {
        TaskExecution taskExecution = taskExecutionService
            .fetchLastJobTaskExecution(Validate.notNull(job.getId(), "id"))
            .orElse(null);

        if (taskExecution != null && taskExecution.getStatus() == TaskExecution.Status.FAILED) {
            ExecutionError error = taskExecution.getError();

            throw new ExecutionException(error.getMessage(), TaskExecutionErrorType.TASK_EXECUTION_FAILED);
        }
    }

    private Job checkForWebhookResponse(Job job) {
        long jobId = Validate.notNull(job.getId(), "id");

        return taskExecutionService.fetchLastJobTaskExecution(jobId)
            .map(lastTaskExecution -> {
                WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(lastTaskExecution.getType());

                if (WEBHOOK_COMPONENTS.contains(workflowNodeType.name())) {
                    job.setOutputs(
                        taskFileStorage.storeJobOutputs(
                            jobId,
                            Map.of(
                                WebhookConstants.WEBHOOK_RESPONSE,
                                taskFileStorage.readTaskExecutionOutput(lastTaskExecution.getOutput()))));
                }

                return job;
            })
            .orElse(job);
    }

    private record JobServiceWrapper(JobFactoryFunction jobFactoryFunction) implements JobService {

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
        public Job create(JobParametersDTO jobParametersDTO, Workflow workflow) {
            return jobFactoryFunction.apply(jobParametersDTO);
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
        public Optional<Job> fetchLastWorkflowJob(List<String> workflowIds) {
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

    private static class JobSyncAsyncTaskExecutor implements AsyncTaskExecutor {

        private final Executor executor;

        private JobSyncAsyncTaskExecutor(Environment environment) {
            if (Threading.VIRTUAL.isActive(environment)) {
                executor = Executors.newVirtualThreadPerTaskExecutor();
            } else {
                executor = Executors.newCachedThreadPool();
            }
        }

        @Override
        public void execute(Runnable task) {
            String tenantId = TenantContext.getCurrentTenantId();

            executor.execute(
                () -> {
                    String currentTenantId = TenantContext.getCurrentTenantId();

                    try {
                        TenantContext.setCurrentTenantId(tenantId);

                        task.run();
                    } finally {
                        TenantContext.setCurrentTenantId(currentTenantId);
                    }
                });
        }
    }
}
