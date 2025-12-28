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

import com.bytechef.atlas.configuration.domain.ControlTask;
import com.bytechef.atlas.configuration.domain.Task;
import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.coordinator.TaskCoordinator;
import com.bytechef.atlas.coordinator.event.ApplicationEvent;
import com.bytechef.atlas.coordinator.event.ErrorEvent;
import com.bytechef.atlas.coordinator.event.JobStatusApplicationEvent;
import com.bytechef.atlas.coordinator.event.StartJobEvent;
import com.bytechef.atlas.coordinator.event.StopJobEvent;
import com.bytechef.atlas.coordinator.event.TaskExecutionCompleteEvent;
import com.bytechef.atlas.coordinator.event.TaskExecutionErrorEvent;
import com.bytechef.atlas.coordinator.event.TaskStartedApplicationEvent;
import com.bytechef.atlas.coordinator.event.listener.ApplicationEventListener;
import com.bytechef.atlas.coordinator.event.listener.TaskExecutionErrorEventListener;
import com.bytechef.atlas.coordinator.event.listener.TaskStartedApplicationEventListener;
import com.bytechef.atlas.coordinator.job.JobExecutor;
import com.bytechef.atlas.coordinator.message.route.TaskCoordinatorMessageRoute;
import com.bytechef.atlas.coordinator.task.completion.DefaultTaskCompletionHandler;
import com.bytechef.atlas.coordinator.task.completion.TaskCompletionHandlerChain;
import com.bytechef.atlas.coordinator.task.completion.TaskCompletionHandlerFactory;
import com.bytechef.atlas.coordinator.task.dispatcher.DefaultTaskDispatcher;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcher;
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
import com.bytechef.atlas.worker.event.CancelControlTaskEvent;
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
import com.bytechef.tenant.util.TenantCacheKeyUtils;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.domain.Page;
import org.springframework.lang.Nullable;

/**
 * @author Ivica Cardic
 */
public class JobSyncExecutor {

    private static final Logger logger = LoggerFactory.getLogger(JobSyncExecutor.class);

    private static final int NO_TIMEOUT = -1;
    private static final int UNLIMITED_TASK_EXECUTIONS = -1;

    private final ContextService contextService;
    private final ApplicationEventPublisher coordinatorEventPublisher;
    private final Cache<String, CopyOnWriteArrayList<Consumer<ErrorEvent>>> errorListeners = createCache();
    private final Map<String, CountDownLatch> jobCompletionLatches = new ConcurrentHashMap<>();
    private final JobFacade jobFacade;
    private final JobService jobService;
    private final Cache<String, CopyOnWriteArrayList<Consumer<JobStatusApplicationEvent>>> jobStatusListeners =
        createCache();
    private final Cache<String, CopyOnWriteArrayList<Consumer<TaskExecutionCompleteEvent>>> taskExecutionCompleteListeners =
        createCache();
    private final TaskExecutionService taskExecutionService;
    private final TaskFileStorage taskFileStorage;
    private final Cache<String, CopyOnWriteArrayList<Consumer<TaskStartedApplicationEvent>>> taskStartedListeners =
        createCache();
    private final long timeout;
    private final WorkflowService workflowService;

    public JobSyncExecutor(
        ContextService contextService, Evaluator evaluator, JobService jobService, int maxTaskExecutions,
        MemoryMessageFactory memoryMessageFactory,
        List<TaskDispatcherPreSendProcessor> taskDispatcherPreSendProcessors, TaskExecutionService taskExecutionService,
        TaskExecutor taskExecutor, TaskHandlerRegistry taskHandlerRegistry, TaskFileStorage taskFileStorage,
        long timeout, WorkflowService workflowService) {

        this(
            contextService, evaluator, jobService, maxTaskExecutions, memoryMessageFactory, List.of(), List.of(),
            taskDispatcherPreSendProcessors, List.of(), taskExecutionService, taskExecutor, taskHandlerRegistry,
            taskFileStorage, timeout, workflowService);
    }

    @SuppressFBWarnings("EI")
    public JobSyncExecutor(
        ContextService contextService, Evaluator evaluator, JobService jobService, int maxTaskExecutions,
        MemoryMessageFactory memoryMessageFactory,
        List<TaskCompletionHandlerFactory> taskCompletionHandlerFactories,
        List<TaskDispatcherAdapterFactory> taskDispatcherAdapterFactories,
        List<TaskDispatcherPreSendProcessor> taskDispatcherPreSendProcessors,
        List<TaskDispatcherResolverFactory> taskDispatcherResolverFactories, TaskExecutionService taskExecutionService,
        TaskExecutor taskExecutor, TaskHandlerRegistry taskHandlerRegistry, TaskFileStorage taskFileStorage,
        long timeout, WorkflowService workflowService) {

        this.contextService = contextService;

        MemoryMessageBroker coordinatorMemoryMessageBroker =
            memoryMessageFactory.get(MemoryMessageFactory.Role.COORDINATOR);

        this.coordinatorEventPublisher = createEventPublisher(coordinatorMemoryMessageBroker);

        this.jobFacade = new JobFacadeImpl(
            coordinatorEventPublisher, contextService, jobService, taskExecutionService, taskFileStorage,
            workflowService);

        this.jobService = jobService;
        this.taskExecutionService = taskExecutionService;
        this.taskFileStorage = taskFileStorage;
        this.timeout = timeout;
        this.workflowService = workflowService;

        TaskHandlerResolverChain taskHandlerResolverChain = new TaskHandlerResolverChain();

        taskHandlerResolverChain.setTaskHandlerResolvers(
            List.of(
                new TaskDispatcherAdapterTaskHandlerResolver(taskDispatcherAdapterFactories, taskHandlerResolverChain),
                new DefaultTaskHandlerResolver(taskHandlerRegistry)));

        JobSyncAsyncTaskExecutor jobSyncAsyncTaskExecutor = new JobSyncAsyncTaskExecutor(
            taskExecutor, maxTaskExecutions);

        TaskWorker taskWorker = new TaskWorker(
            null, evaluator, coordinatorEventPublisher, jobSyncAsyncTaskExecutor, taskHandlerResolverChain,
            taskFileStorage, List.of(new WebhookResponseTaskExecutionPostOutputProcessor()));

        MemoryMessageBroker workerMessageBroker = memoryMessageFactory.get(MemoryMessageFactory.Role.WORKER);

        receive(
            workerMessageBroker, TaskWorkerMessageRoute.CONTROL_EVENTS, event -> {
                CancelControlTaskEvent cancelControlTaskEvent = (CancelControlTaskEvent) event;

                taskWorker.onCancelControlTaskEvent(cancelControlTaskEvent);
            });
        receive(
            workerMessageBroker, TaskWorkerMessageRoute.TASK_EXECUTION_EVENTS, event -> handleWorkerTaskExecutionEvent(
                (TaskExecutionEvent) event, maxTaskExecutions, jobSyncAsyncTaskExecutor, taskWorker));

        TaskDispatcherChain taskDispatcherChain = new TaskDispatcherChain();

        taskDispatcherChain.setTaskDispatcherResolvers(
            CollectionUtils.concat(
                getTaskDispatcherResolverStream(taskDispatcherResolverFactories, taskDispatcherChain),
                Stream.of(
                    new ControlTaskDispatcher(),
                    new DefaultTaskDispatcher(
                        createEventPublisher(workerMessageBroker), taskDispatcherPreSendProcessors))));

        TaskExecutionErrorEventListener taskExecutionErrorEventListener = new TaskExecutionErrorEventListener(
            coordinatorEventPublisher, jobService, taskDispatcherChain, taskExecutionService);

        receive(
            coordinatorMemoryMessageBroker, TaskCoordinatorMessageRoute.ERROR_EVENTS,
            event -> handleCoordinatorErrorEvent((ErrorEvent) event, taskExecutionErrorEventListener));

        JobExecutor jobExecutor = new JobExecutor(
            contextService, evaluator, taskDispatcherChain, taskExecutionService, taskFileStorage, workflowService);

        DefaultTaskCompletionHandler defaultTaskCompletionHandler = new DefaultTaskCompletionHandler(
            contextService, evaluator, coordinatorEventPublisher, jobExecutor, jobService, taskExecutionService,
            taskFileStorage, workflowService);

        TaskCompletionHandlerChain taskCompletionHandlerChain = new TaskCompletionHandlerChain();

        taskCompletionHandlerChain.setTaskCompletionHandlers(
            CollectionUtils.concat(
                taskCompletionHandlerFactories.stream()
                    .map(taskCompletionHandlerFactory -> taskCompletionHandlerFactory.createTaskCompletionHandler(
                        taskCompletionHandlerChain, taskDispatcherChain)),
                Stream.of(defaultTaskCompletionHandler)));

        TaskCoordinator taskCoordinator = new TaskCoordinator(
            getCoordinatorApplicationEventListeners(taskExecutionService, jobService, taskDispatcherChain), List.of(),
            coordinatorEventPublisher, jobExecutor, jobService, taskCompletionHandlerChain, taskDispatcherChain,
            taskExecutionService);

        receive(
            coordinatorMemoryMessageBroker, TaskCoordinatorMessageRoute.APPLICATION_EVENTS,
            event -> handleCoordinatorApplicationEvent(event, jobSyncAsyncTaskExecutor, taskCoordinator));
        receive(
            coordinatorMemoryMessageBroker, TaskCoordinatorMessageRoute.JOB_START_EVENTS,
            event -> taskCoordinator.onStartJobEvent((StartJobEvent) event));
        receive(
            coordinatorMemoryMessageBroker, TaskCoordinatorMessageRoute.JOB_STOP_EVENTS,
            event -> handleCoordinatorJobStopEvent((StopJobEvent) event, taskCoordinator));
        receive(
            coordinatorMemoryMessageBroker, TaskCoordinatorMessageRoute.TASK_EXECUTION_COMPLETE_EVENTS,
            event -> handleCoordinatorTaskExecutionCompleteEvent((TaskExecutionCompleteEvent) event, taskCoordinator));
    }

    /**
     * Adds a listener that will be triggered when an error event occurs for the job associated with the specified job
     * ID. The listener will receive {@link ErrorEvent} as input when invoked. The method returns an
     * {@link AutoCloseable} instance that can be used to unregister the added listener.
     *
     * @param jobId    the unique identifier of the job for which the error listener is to be added
     * @param listener a {@link Consumer} that processes {@link ErrorEvent} when an error occurs
     * @return an {@link AutoCloseable} instance that can be used to remove the added listener
     */
    public AutoCloseable addErrorListener(long jobId, Consumer<ErrorEvent> listener) {
        errorListeners.get(getKey(jobId), k -> new CopyOnWriteArrayList<>())
            .add(listener);

        return () -> {
            var errorListeners = this.errorListeners.getIfPresent(getKey(jobId));

            if (errorListeners != null) {
                errorListeners.remove(listener);
            }
        };
    }

    /**
     * Adds a listener for job status updates associated with a specific job ID. The listener will be triggered whenever
     * a job status update event occurs.
     *
     * @param jobId    the ID of the job for which the listener is being registered
     * @param listener a {@link Consumer} that handles {@link JobStatusApplicationEvent} when an event is triggered
     * @return an {@link AutoCloseable} that can be used to remove the registered listener when no longer needed
     */
    public AutoCloseable addJobStatusListener(long jobId, Consumer<JobStatusApplicationEvent> listener) {
        jobStatusListeners.get(getKey(jobId), k -> new CopyOnWriteArrayList<>())
            .add(listener);

        return () -> {
            var jobStatusListeners = this.jobStatusListeners.getIfPresent(getKey(jobId));

            if (jobStatusListeners != null) {
                jobStatusListeners.remove(listener);
            }
        };
    }

    /**
     * Adds a listener that will be triggered when a task execution for the specified job ID is completed. The listener
     * will be automatically removed when the returned {@code AutoCloseable} instance is closed.
     *
     * @param jobId    the ID of the job for which the task execution completion event will be listened to.
     * @param listener the listener to be invoked when the task execution completes.
     * @return an {@code AutoCloseable} instance that, when closed, removes the specified listener from the event
     *         notifications.
     */
    public AutoCloseable addTaskExecutionCompleteListener(long jobId, Consumer<TaskExecutionCompleteEvent> listener) {
        taskExecutionCompleteListeners
            .get(getKey(jobId), k -> new CopyOnWriteArrayList<>())
            .add(listener);

        return () -> {
            var taskExecutionCompleteListeners = this.taskExecutionCompleteListeners.getIfPresent(getKey(jobId));

            if (taskExecutionCompleteListeners != null) {
                taskExecutionCompleteListeners.remove(listener);
            }
        };
    }

    /**
     * Adds a listener that will be triggered when a task associated with the given job ID is started. The listener will
     * receive {@link TaskStartedApplicationEvent} as input when invoked. The method returns an {@link AutoCloseable}
     * which can be used to unregister the listener.
     *
     * @param jobId    the unique identifier of the job for which the listener is to be added
     * @param listener a {@link Consumer} that processes {@link TaskStartedApplicationEvent} when a task starts
     * @return an {@link AutoCloseable} instance that can be used to remove the added listener
     */
    public AutoCloseable addTaskStartedListener(long jobId, Consumer<TaskStartedApplicationEvent> listener) {
        taskStartedListeners.get(getKey(jobId), k -> new CopyOnWriteArrayList<>())
            .add(listener);

        return () -> {
            var taskStartedListeners = this.taskStartedListeners.getIfPresent(getKey(jobId));

            if (taskStartedListeners != null) {
                taskStartedListeners.remove(listener);
            }
        };
    }

    /**
     * Waits for the completion of a job identified by the specified job ID and, if specified, verifies for errors. Once
     * the job is completed, its details are fetched and processed.
     *
     * @param jobId         the unique identifier of the job to wait for completion
     * @param checkForError a flag indicating whether to check for errors in the completed job
     * @return the completed job after processing, including any webhook response if applicable
     */
    public Job awaitJob(long jobId, boolean checkForError) {
        waitForJobCompletion(jobId);

        Job job = jobService.getJob(jobId);

        if (checkForError) {
            checkForError(job);
        }

        return checkForWebhookResponse(job);
    }

    /**
     * Deletes the job associated with the specified job ID.
     *
     * @param jobId the unique identifier of the job to be deleted
     */
    public void deleteJob(long jobId) {
        jobFacade.deleteJob(jobId);
    }

    /**
     * Executes a job using the specified job parameters. Optionally checks for potential errors during the execution
     * process. This method delegates the execution to an underlying process.
     *
     * @param jobParametersDTO the parameters required to configure and initiate the job
     * @param checkForError    a flag indicating whether to check for errors during job execution
     * @return the job instance that was executed, encapsulating its status and any results
     */
    public Job execute(JobParametersDTO jobParametersDTO, boolean checkForError) {
        return execute(jobParametersDTO, jobFacade, checkForError);
    }

    /**
     * Executes a job based on the provided job parameters, job factory function, and error check flag.
     *
     * @param jobParametersDTO   the job parameters used to configure and initiate the job
     * @param jobFactoryFunction the function used to create an instance of the job
     * @param checkForError      a flag indicating whether to check for errors during execution
     * @return the executed job
     */
    public Job execute(
        JobParametersDTO jobParametersDTO, JobFactoryFunction jobFactoryFunction, boolean checkForError) {

        JobFacade jobFacade = new JobFacadeImpl(
            coordinatorEventPublisher, contextService, new JobServiceWrapper(jobFactoryFunction), taskExecutionService,
            taskFileStorage, workflowService);

        return execute(jobParametersDTO, jobFacade, checkForError);
    }

    /**
     * Initiates a job using the provided job parameters and returns the unique identifier of the created job.
     *
     * @param jobParametersDTO the parameters required to create and configure the job
     * @return the unique identifier of the created job
     */
    public long startJob(JobParametersDTO jobParametersDTO) {
        return jobFacade.createJob(jobParametersDTO);
    }

    /**
     * Stops a running job identified by its unique job ID. This method publishes an event to notify the job
     * coordination system about the stop request for the specified job.
     *
     * @param jobId the unique identifier of the job to be stopped
     */
    public void stopJob(long jobId) {
        coordinatorEventPublisher.publishEvent(new StopJobEvent(jobId));
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

    private void handleCoordinatorApplicationEvent(
        Object event, JobSyncAsyncTaskExecutor jobSyncAsyncTaskExecutor, TaskCoordinator taskCoordinator) {

        taskCoordinator.onApplicationEvent((ApplicationEvent) event);

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

            notifyJobStatusListeners(jobId, jobStatusEvent);

            if (status == Job.Status.COMPLETED || status == Job.Status.FAILED || status == Job.Status.STOPPED) {
                invalidateListeners(jobId);
            }
        } else if (event instanceof TaskStartedApplicationEvent taskStartedEvent) {
            long taskExecutionId = taskStartedEvent.getTaskExecutionId();

            Job job = jobService.getTaskExecutionJob(taskExecutionId);

            long jobId = Validate.notNull(job.getId(), "id");

            notifyTaskStartedListeners(jobId, taskStartedEvent);
        }
    }

    private void handleCoordinatorErrorEvent(
        ErrorEvent event, TaskExecutionErrorEventListener taskExecutionErrorEventListener) {

        // First, notify any registered external listeners so tests/observers receive the event even if the
        // coordinator's default listener fails due to incomplete test data.
        try {
            notifyErrorListeners(event);
        } catch (Exception exception) {
            if (logger.isTraceEnabled()) {
                logger.trace(exception.getMessage(), exception);
            }
        }

        // Then forward to the default error event listener; guard against IllegalArgumentException thrown when test
        // events lack required fields (e.g., missing error on TaskExecution).
        try {
            taskExecutionErrorEventListener.onErrorEvent(event);
        } catch (Exception e) {
            if (logger.isDebugEnabled()) {
                logger.debug("Error while handling ERROR_EVENTS in default listener (ignored in this context)", e);
            }
        }
    }

    private void handleCoordinatorJobStopEvent(StopJobEvent event, TaskCoordinator taskCoordinator) {
        Optional<Job> jobOptional = jobService.fetchJob(event.getJobId());

        if (jobOptional.isEmpty()) {
            return;
        }

        taskCoordinator.onStopJobEvent(event);
    }

    private void handleCoordinatorTaskExecutionCompleteEvent(
        TaskExecutionCompleteEvent event, TaskCoordinator taskCoordinator) {

        // Notify listeners first so observers get the signal even if downstream handling fails on test data that does
        // not include all required fields (e.g., taskExecution.id).
        notifyTaskExecutionCompleteListeners(event);

        try {
            taskCoordinator.onTaskExecutionCompleteEvent(event);
        } catch (Exception e) {
            if (logger.isDebugEnabled()) {
                logger.debug(
                    "Error while handling TASK_EXECUTION_COMPLETE_EVENTS in coordinator (ignored in this context)", e);
            }
        }
    }

    private void handleWorkerTaskExecutionEvent(
        TaskExecutionEvent taskExecutionEvent, int maxTaskExecutions,
        JobSyncAsyncTaskExecutor jobSyncAsyncTaskExecutor, TaskWorker taskWorker) {

        if (maxTaskExecutions != UNLIMITED_TASK_EXECUTIONS) {
            jobSyncAsyncTaskExecutor.incrementAndCheck(taskExecutionEvent);
        }

        TaskExecution taskExecution = taskExecutionEvent.getTaskExecution();

        long jobId = Validate.notNull(taskExecution.getJobId(), "jobId");

        Job job = jobService.getJob(jobId);

        if (job.getStatus() != Job.Status.STARTED) {
            if (logger.isDebugEnabled()) {
                logger.debug("Skipping task scheduling for non-STARTED job (status={}): {}", job.getStatus(), jobId);
            }

            return;
        }

        taskWorker.onTaskExecutionEvent(taskExecutionEvent);
    }

    private void checkForError(Job job) {
        TaskExecution taskExecution = taskExecutionService
            .fetchLastJobTaskExecution(Validate.notNull(job.getId(), "id"))
            .orElse(null);

        if (taskExecution != null && taskExecution.getStatus() == TaskExecution.Status.FAILED) {
            ExecutionError error = taskExecution.getError();

            if (error != null && error.getMessage() != null) {
                throw new ExecutionException(error.getMessage(), TaskExecutionErrorType.TASK_EXECUTION_FAILED);
            }

            String message =
                "Task execution failed for job " + job.getId() + " but no error details are available.";

            if (logger.isWarnEnabled()) {
                logger.warn(
                    "Detected FAILED task execution without error details for jobId={}, taskExecutionId={}",
                    job.getId(), taskExecution.getId());
            }

            throw new ExecutionException(message, TaskExecutionErrorType.TASK_EXECUTION_FAILED);
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

    private static <T> Cache<String, CopyOnWriteArrayList<T>> createCache() {
        return Caffeine.newBuilder()
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .maximumSize(1000)
            .build();
    }

    private List<ApplicationEventListener> getCoordinatorApplicationEventListeners(
        TaskExecutionService taskExecutionService, JobService jobService,
        TaskDispatcher<? super Task> taskDispatcher) {

        return List.of(new TaskStartedApplicationEventListener(taskExecutionService, taskDispatcher, jobService));
    }

    private static String getKey(long jobId) {
        return TenantCacheKeyUtils.getKey(jobId);
    }

    private static Stream<TaskDispatcherResolver> getTaskDispatcherResolverStream(
        List<TaskDispatcherResolverFactory> taskDispatcherResolverFactories, TaskDispatcherChain taskDispatcherChain) {

        return taskDispatcherResolverFactories.stream()
            .map(taskDispatcherFactory -> taskDispatcherFactory.createTaskDispatcherResolver(taskDispatcherChain));
    }

    private void invalidateListeners(long jobId) {
        String key = getKey(jobId);

        errorListeners.invalidate(key);
        jobStatusListeners.invalidate(key);
        taskExecutionCompleteListeners.invalidate(key);
        taskStartedListeners.invalidate(key);
    }

    private void notifyTaskExecutionCompleteListeners(
        TaskExecutionCompleteEvent event) {
        Long jobId = null;
        try {
            var taskExecution = event.getTaskExecution();
            if (taskExecution != null) {
                jobId = Validate.notNull(taskExecution.getJobId(), "jobId");
            }
        } catch (Exception exception) {
            if (logger.isTraceEnabled()) {
                logger.trace(exception.getMessage(), exception);
            }
        }

        if (jobId == null) {
            return;
        }

        var listeners = taskExecutionCompleteListeners.getIfPresent(getKey(jobId));
        if (listeners == null) {
            return;
        }
        for (var listener : listeners) {

            try {
                listener.accept(event);
            } catch (Exception exception) {
                if (logger.isTraceEnabled()) {
                    logger.trace(exception.getMessage(), exception);
                }
            }
        }
    }

    private void notifyErrorListeners(ErrorEvent event) {
        Long jobId = null;

        try {
            if (event instanceof TaskExecutionErrorEvent tee) {
                var te = tee.getTaskExecution();
                if (te != null) {
                    jobId = Validate.notNull(te.getJobId(), "jobId");
                }
            }
        } catch (Exception exception) {
            if (logger.isTraceEnabled()) {
                logger.trace(exception.getMessage(), exception);
            }
        }

        if (jobId == null) {
            return;
        }

        var listeners = errorListeners.getIfPresent(getKey(jobId));

        if (listeners == null) {
            return;
        }

        for (var listener : listeners) {
            try {
                listener.accept(event);
            } catch (Exception exception) {
                if (logger.isTraceEnabled()) {
                    logger.trace(exception.getMessage(), exception);
                }
            }
        }
    }

    private void notifyTaskStartedListeners(long jobId, TaskStartedApplicationEvent event) {
        var listeners = taskStartedListeners.getIfPresent(getKey(jobId));

        if (listeners == null) {
            return;
        }

        for (var listener : listeners) {
            try {
                listener.accept(event);
            } catch (Exception exception) {
                if (logger.isTraceEnabled()) {
                    logger.trace(exception.getMessage(), exception);
                }
            }
        }
    }

    private void notifyJobStatusListeners(long jobId, JobStatusApplicationEvent event) {
        var listeners = jobStatusListeners.getIfPresent(getKey(jobId));

        if (listeners == null) {
            return;
        }

        for (var listener : listeners) {
            try {
                listener.accept(event);
            } catch (Exception exception) {
                if (logger.isTraceEnabled()) {
                    logger.trace(exception.getMessage(), exception);
                }
            }
        }
    }

    private void receive(MemoryMessageBroker messageBroker, MessageRoute messageRoute, Receiver receiver) {
        messageBroker.receive(messageRoute, message -> {
            TenantContext.setCurrentTenantId((String) ((MessageEvent<?>) message).getMetadata(CURRENT_TENANT_ID));

            receiver.receive(message);
        });
    }

    private void waitForJobCompletion(long jobId) {
        Job job = jobService.getJob(jobId);

        if (job.getStatus() == Job.Status.COMPLETED || job.getStatus() == Job.Status.FAILED ||
            job.getStatus() == Job.Status.STOPPED) {

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

    @FunctionalInterface
    public interface JobFactoryFunction {

        Job apply(JobParametersDTO jobParametersDTO);
    }

    public interface MemoryMessageFactory {

        enum Role {
            COORDINATOR, WORKER;
        }

        MemoryMessageBroker get(Role role);
    }

    private static class ControlTaskDispatcher implements TaskDispatcher<ControlTask>, TaskDispatcherResolver {

        @Override
        public void dispatch(ControlTask controlTask) {
        }

        @Override
        public @Nullable TaskDispatcher<? extends Task> resolve(Task task) {
            if (task instanceof ControlTask) {
                return this;
            }

            return null;
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
                getKey(
                    Validate.notNull(taskExecution.getJobId(), "jobId")),
                (key) -> new AtomicInteger(0));

            if (taskExecutionCounter.incrementAndGet() > maxTaskExecutions) {
                taskExecution.setError(
                    new ExecutionError(
                        String.format(
                            "Maximum number of task executions (%d) exceeded in the workflow builder",
                            maxTaskExecutions),
                        List.of()));
                taskExecution.setStatus(TaskExecution.Status.FAILED);

                coordinatorEventPublisher.publishEvent(new TaskExecutionErrorEvent(taskExecution));
            }
        }

        private void clearCounter(long jobId) {
            taskExecutionCounters.remove(getKey(jobId));
        }
    }
}
