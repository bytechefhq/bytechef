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

import static com.bytechef.tenant.constant.TenantConstants.CURRENT_TENANT_ID;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.coordinator.TaskCoordinator;
import com.bytechef.atlas.coordinator.event.ApplicationEvent;
import com.bytechef.atlas.coordinator.event.ErrorEvent;
import com.bytechef.atlas.coordinator.event.JobStatusApplicationEvent;
import com.bytechef.atlas.coordinator.event.StartJobEvent;
import com.bytechef.atlas.coordinator.event.TaskExecutionCompleteEvent;
import com.bytechef.atlas.coordinator.event.TaskExecutionErrorEvent;
import com.bytechef.atlas.coordinator.event.listener.ApplicationEventListener;
import com.bytechef.atlas.coordinator.event.listener.TaskExecutionErrorEventListener;
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
import com.bytechef.message.broker.MessageBroker;
import com.bytechef.message.broker.memory.MemoryMessageBroker;
import com.bytechef.message.broker.memory.MemoryMessageBroker.Receiver;
import com.bytechef.message.event.MessageEvent;
import com.bytechef.message.route.MessageRoute;
import com.bytechef.platform.coordinator.job.exception.TaskExecutionErrorType;
import com.bytechef.platform.webhook.executor.constant.WebhookConstants;
import com.bytechef.platform.worker.task.WebhookResponseTaskExecutionPostOutputProcessor;
import com.bytechef.tenant.TenantContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.domain.Page;

/**
 * @author Ivica Cardic
 */
public class JobSyncExecutor {

    private static final Logger logger = LoggerFactory.getLogger(JobSyncExecutor.class);

    private static final int NO_TIMEOUT = -1;
    private static final int UNLIMITED_TASK_EXECUTIONS = -1;

    private final ContextService contextService;
    private final Map<String, CountDownLatch> jobCompletionLatches = new ConcurrentHashMap<>();
    private final ApplicationEventPublisher eventPublisher;
    private final JobFacade jobFacade;
    private final JobService jobService;
    private final TaskExecutionService taskExecutionService;
    private final TaskFileStorage taskFileStorage;
    private final WorkflowService workflowService;
    private final long timeout;

    public JobSyncExecutor(
        ContextService contextService, Evaluator evaluator, JobService jobService, int maxTaskExecutions,
        Supplier<MemoryMessageBroker> memoryMessageBrokerSupplier,
        List<TaskDispatcherPreSendProcessor> taskDispatcherPreSendProcessors, TaskExecutionService taskExecutionService,
        TaskExecutor taskExecutor, TaskHandlerRegistry taskHandlerRegistry, TaskFileStorage taskFileStorage,
        long timeout, WorkflowService workflowService) {

        this(
            contextService, evaluator, jobService, maxTaskExecutions, memoryMessageBrokerSupplier, List.of(), List.of(),
            taskDispatcherPreSendProcessors, List.of(), taskExecutionService, taskExecutor, taskHandlerRegistry,
            taskFileStorage, timeout, workflowService);
    }

    @SuppressFBWarnings("EI")
    public JobSyncExecutor(
        ContextService contextService, Evaluator evaluator, JobService jobService, int maxTaskExecutions,
        Supplier<MemoryMessageBroker> memoryMessageBrokerSupplier,
        List<TaskCompletionHandlerFactory> taskCompletionHandlerFactories,
        List<TaskDispatcherAdapterFactory> taskDispatcherAdapterFactories,
        List<TaskDispatcherPreSendProcessor> taskDispatcherPreSendProcessors,
        List<TaskDispatcherResolverFactory> taskDispatcherResolverFactories, TaskExecutionService taskExecutionService,
        TaskExecutor taskExecutor, TaskHandlerRegistry taskHandlerRegistry, TaskFileStorage taskFileStorage,
        long timeout, WorkflowService workflowService) {

        this.contextService = contextService;

        MemoryMessageBroker memoryMessageBroker = memoryMessageBrokerSupplier.get();

        this.eventPublisher = createEventPublisher(memoryMessageBroker);

        this.jobFacade = new JobFacadeImpl(
            eventPublisher, contextService, jobService, taskExecutionService, taskFileStorage, workflowService);

        this.jobService = jobService;
        this.taskExecutionService = taskExecutionService;
        this.taskFileStorage = taskFileStorage;
        this.timeout = timeout;
        this.workflowService = workflowService;

        receive(memoryMessageBroker, TaskCoordinatorMessageRoute.JOB_STOP_EVENTS, event -> {});

        TaskHandlerResolverChain taskHandlerResolverChain = new TaskHandlerResolverChain();

        taskHandlerResolverChain.setTaskHandlerResolvers(
            List.of(
                new TaskDispatcherAdapterTaskHandlerResolver(taskDispatcherAdapterFactories, taskHandlerResolverChain),
                new DefaultTaskHandlerResolver(taskHandlerRegistry)));

        JobSyncAsyncTaskExecutor jobSyncAsyncTaskExecutor = new JobSyncAsyncTaskExecutor(
            taskExecutor, maxTaskExecutions);

        TaskWorker taskWorker = new TaskWorker(
            evaluator, eventPublisher, jobSyncAsyncTaskExecutor, taskHandlerResolverChain, taskFileStorage,
            List.of(new WebhookResponseTaskExecutionPostOutputProcessor()));

        MemoryMessageBroker coordinatorMessageBroker = memoryMessageBrokerSupplier.get();

        receive(
            coordinatorMessageBroker, TaskWorkerMessageRoute.TASK_EXECUTION_EVENTS, event -> {
                TaskExecutionEvent taskExecutionEvent = (TaskExecutionEvent) event;

                if (maxTaskExecutions != UNLIMITED_TASK_EXECUTIONS) {
                    jobSyncAsyncTaskExecutor.incrementAndCheck(taskExecutionEvent);
                }

                TaskExecution taskExecution = taskExecutionEvent.getTaskExecution();

                long jobId = Validate.notNull(taskExecution.getJobId(), "jobId");

                Job job = jobService.getJob(jobId);

                if (job.getStatus() == Job.Status.FAILED) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Skipping task scheduling for FAILED job: {}", jobId);
                    }

                    return;
                }

                taskWorker.onTaskExecutionEvent(taskExecutionEvent);
            });

        TaskDispatcherChain taskDispatcherChain = new TaskDispatcherChain();

        taskDispatcherChain.setTaskDispatcherResolvers(
            CollectionUtils.concat(
                getTaskDispatcherResolverStream(taskDispatcherResolverFactories, taskDispatcherChain),
                Stream.of(
                    new DefaultTaskDispatcher(
                        createEventPublisher(coordinatorMessageBroker), taskDispatcherPreSendProcessors))));

        TaskExecutionErrorEventListener taskExecutionErrorEventListener = new TaskExecutionErrorEventListener(
            eventPublisher, jobService, taskDispatcherChain, taskExecutionService);

        receive(memoryMessageBroker, TaskCoordinatorMessageRoute.ERROR_EVENTS,
            event -> taskExecutionErrorEventListener.onErrorEvent((ErrorEvent) event));

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

        receive(
            memoryMessageBroker, TaskCoordinatorMessageRoute.APPLICATION_EVENTS,
            event -> taskCoordinator.onApplicationEvent((ApplicationEvent) event));
        receive(
            memoryMessageBroker, TaskCoordinatorMessageRoute.APPLICATION_EVENTS, event -> {
                if (event instanceof JobStatusApplicationEvent jobStatusEvent) {
                    long jobId = jobStatusEvent.getJobId();

                    Job.Status status = jobStatusEvent.getStatus();

                    if (status == Job.Status.COMPLETED || status == Job.Status.FAILED || status == Job.Status.STOPPED) {
                        CountDownLatch latch = jobCompletionLatches.get(getKey(jobId));

                        if (latch != null) {
                            latch.countDown();
                        }

                        jobSyncAsyncTaskExecutor.clearCounter(jobId);
                    }
                }
            });
        receive(
            memoryMessageBroker, TaskCoordinatorMessageRoute.TASK_EXECUTION_COMPLETE_EVENTS,
            event -> taskCoordinator.onTaskExecutionCompleteEvent((TaskExecutionCompleteEvent) event));
        receive(
            memoryMessageBroker, TaskCoordinatorMessageRoute.JOB_START_EVENTS,
            event -> taskCoordinator.onStartJobEvent((StartJobEvent) event));
    }

    public Job execute(JobParametersDTO jobParametersDTO, boolean checkForError) {
        return execute(jobParametersDTO, jobFacade, checkForError);
    }

    public Job execute(
        JobParametersDTO jobParametersDTO, JobFactoryFunction jobFactoryFunction, boolean checkForError) {

        JobFacade jobFacade = new JobFacadeImpl(
            eventPublisher, contextService, new JobServiceWrapper(jobFactoryFunction), taskExecutionService,
            taskFileStorage, workflowService);

        return execute(jobParametersDTO, jobFacade, checkForError);
    }

    private static ApplicationEventPublisher createEventPublisher(MessageBroker messageBroker) {
        return event -> {
            MessageEvent<?> messageEvent = (MessageEvent<?>) event;

            messageEvent.putMetadata(CURRENT_TENANT_ID, TenantContext.getCurrentTenantId());

            messageBroker.send(messageEvent.getRoute(), messageEvent);
        };
    }

    private Job execute(JobParametersDTO jobParametersDTO, JobFacade jobFacade, boolean checkForError) {
        Job job = jobService.getJob(jobFacade.createJob(jobParametersDTO));

        long jobId = Validate.notNull(job.getId(), "id");

        waitForJobCompletion(jobId);

        job = jobService.getJob(jobId);

        if (checkForError) {
            checkForError(job);
        }

        return checkForWebhookResponse(job);
    }

    private List<ApplicationEventListener> getApplicationEventListeners(
        TaskExecutionService taskExecutionService, JobService jobService) {

        return List.of(new TaskStartedApplicationEventListener(taskExecutionService, task -> {}, jobService));
    }

    private static String getKey(long jobId) {
        return TenantContext.getCurrentTenantId() + "_" + jobId;
    }

    private static Stream<TaskDispatcherResolver> getTaskDispatcherResolverStream(
        List<TaskDispatcherResolverFactory> taskDispatcherResolverFactories, TaskDispatcherChain taskDispatcherChain) {

        return taskDispatcherResolverFactories.stream()
            .map(taskDispatcherFactory -> taskDispatcherFactory.createTaskDispatcherResolver(taskDispatcherChain));
    }

    public void receive(MemoryMessageBroker messageBroker, MessageRoute messageRoute, Receiver receiver) {
        messageBroker.receive(messageRoute, message -> {
            TenantContext.setCurrentTenantId((String) ((MessageEvent<?>) message).getMetadata(CURRENT_TENANT_ID));

            receiver.receive(message);
        });
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
                Map<String, ?> metadata = lastTaskExecution.getMetadata();

                if (metadata.containsKey(WebhookConstants.WEBHOOK_RESPONSE)) {
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

    private void waitForJobCompletion(long jobId) {
        Job job = jobService.getJob(jobId);

        if (job.getStatus() == Job.Status.COMPLETED || job.getStatus() == Job.Status.FAILED) {
            return;
        }

        CountDownLatch latch = jobCompletionLatches.computeIfAbsent(getKey(jobId), id -> new CountDownLatch(1));

        try {
            Optional<TaskExecution> last = taskExecutionService.fetchLastJobTaskExecution(jobId);

            if (last.isPresent()) {
                TaskExecution taskExecution = last.get();

                TaskExecution.Status status = taskExecution.getStatus();

                if (status.isTerminated()) {
                    jobCompletionLatches.remove(getKey(jobId));

                    return;
                }
            }
        } catch (Exception e) {
            if (logger.isTraceEnabled()) {
                logger.trace(e.getMessage(), e);
            }
        }

        try {
            if (timeout == NO_TIMEOUT) {
                latch.await();
            } else {
                if (!latch.await(timeout, TimeUnit.SECONDS)) {
                    throw new TimeoutException("Timeout waiting for job completion: " + jobId);
                }
            }
        } catch (InterruptedException | TimeoutException exception) {
            if (logger.isTraceEnabled()) {
                logger.trace(exception.getMessage());
            }

            job.setStatus(Job.Status.FAILED);

            jobService.update(job);
        } finally {
            jobCompletionLatches.remove(getKey(jobId));
        }
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
        public Optional<Job> fetchJob(Long id) {
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

    private class JobSyncAsyncTaskExecutor implements AsyncTaskExecutor {

        private final int maxTaskExecutions;
        private final Map<String, AtomicInteger> taskExecutionCounters = new ConcurrentHashMap<>();
        private final TaskExecutor taskExecutor;

        private JobSyncAsyncTaskExecutor(TaskExecutor taskExecutor, int maxTaskExecutions) {
            this.maxTaskExecutions = maxTaskExecutions;
            this.taskExecutor = taskExecutor;
        }

        @Override
        public void execute(Runnable task) {
            taskExecutor.execute(task);
        }

        private void incrementAndCheck(TaskExecutionEvent taskExecutionEvent) {
            TaskExecution taskExecution = taskExecutionEvent.getTaskExecution();

            AtomicInteger taskExecutionCounter = taskExecutionCounters.computeIfAbsent(
                getKey(Validate.notNull(taskExecution.getJobId(), "jobId")), (key) -> new AtomicInteger(0));

            if (taskExecutionCounter.incrementAndGet() > maxTaskExecutions) {
                taskExecution.setError(
                    new ExecutionError(
                        String.format(
                            "Maximum number of task executions (%d) exceeded in the workflow builder",
                            maxTaskExecutions),
                        List.of()));
                taskExecution.setStatus(TaskExecution.Status.FAILED);

                eventPublisher.publishEvent(new TaskExecutionErrorEvent(taskExecution));
            }
        }

        private void clearCounter(long jobId) {
            taskExecutionCounters.remove(getKey(jobId));
        }
    }
}
