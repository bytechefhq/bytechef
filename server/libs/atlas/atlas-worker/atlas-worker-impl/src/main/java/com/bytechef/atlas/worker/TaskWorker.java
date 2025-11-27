/*
 * Copyright 2016-2020 the original author or authors.
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
 *
 * Modifications copyright (C) 2025 ByteChef
 */

package com.bytechef.atlas.worker;

import com.bytechef.atlas.configuration.domain.CancelControlTask;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.coordinator.event.TaskExecutionCompleteEvent;
import com.bytechef.atlas.coordinator.event.TaskExecutionErrorEvent;
import com.bytechef.atlas.coordinator.event.TaskStartedApplicationEvent;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.domain.TaskExecution.Status;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.atlas.worker.event.CancelControlTaskEvent;
import com.bytechef.atlas.worker.event.TaskExecutionEvent;
import com.bytechef.atlas.worker.task.handler.TaskExecutionPostOutputProcessor;
import com.bytechef.atlas.worker.task.handler.TaskHandler;
import com.bytechef.atlas.worker.task.handler.TaskHandlerResolver;
import com.bytechef.error.ExecutionError;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.message.event.MessageEvent;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.task.AsyncTaskExecutor;

/**
 * The class responsible for executing tasks spawned by the {@link com.bytechef.atlas.coordinator.TaskCoordinator}.
 *
 * <p>
 * Worker threads typically execute on a different process than the
 * {@link com.bytechef.atlas.coordinator.TaskCoordinator} process and most likely on a seperate node altogether.
 *
 * <p>
 * Communication between the two is decoupled through the {@link MessageBroker} interface via
 * {@link ApplicationEventPublisher}.
 *
 * @author Arik Cohen
 * @since Jun 12, 2016
 */
public class TaskWorker {

    private static final Logger logger = LoggerFactory.getLogger(TaskWorker.class);

    private static final long DEFAULT_TIME_OUT = 24 * 60 * 60 * 1000; // 24 hours

    private final Evaluator evaluator;
    private final ApplicationEventPublisher eventPublisher;
    private final AsyncTaskExecutor taskExecutor;
    private final TaskHandlerResolver taskHandlerResolver;
    private final Map<Long, TaskExecutionFuture<?>> taskExecutionFutureMap = new ConcurrentHashMap<>();
    private final TaskFileStorage taskFileStorage;
    private final List<TaskExecutionPostOutputProcessor> taskExecutionPostOutputProcessors;

    @SuppressFBWarnings("EI")
    public TaskWorker(
        Evaluator evaluator, ApplicationEventPublisher eventPublisher, AsyncTaskExecutor taskExecutor,
        TaskHandlerResolver taskHandlerResolver, TaskFileStorage taskFileStorage,
        List<TaskExecutionPostOutputProcessor> taskExecutionPostOutputProcessors) {

        this.evaluator = evaluator;
        this.eventPublisher = eventPublisher;
        this.taskExecutor = taskExecutor;
        this.taskHandlerResolver = taskHandlerResolver;
        this.taskFileStorage = taskFileStorage;
        this.taskExecutionPostOutputProcessors = taskExecutionPostOutputProcessors;
    }

    /**
     * Handle the execution of a {@link TaskExecution}. Implementors are expected to execute the task asynchronously.
     *
     * @param taskExecutionEvent The task event which contains task to execute.
     */
    public void onTaskExecutionEvent(TaskExecutionEvent taskExecutionEvent) {
        if (logger.isTraceEnabled()) {
            logger.trace("onTaskExecutionEvent: taskExecutionEvent={}", taskExecutionEvent);
        }

        TaskExecution taskExecution = taskExecutionEvent.getTaskExecution();
        CountDownLatch latch = new CountDownLatch(1);

        Future<?> future = taskExecutor.submit(() -> {
            try {
                eventPublisher.publishEvent(
                    new TaskStartedApplicationEvent(
                        Validate.notNull(taskExecution.getJobId(), "id"),
                        Validate.notNull(taskExecution.getId(), "id")));

                TaskExecution completedTaskExecution = doExecuteTask(taskExecution);

                eventPublisher.publishEvent(new TaskExecutionCompleteEvent(completedTaskExecution));
            } catch (InterruptedException e) {
                if (logger.isTraceEnabled()) {
                    logger.trace(e.getMessage(), e);
                }
            } catch (Exception e) {
                TaskExecutionFuture<?> taskExecutionFuture = taskExecutionFutureMap.get(taskExecution.getId());

                if (taskExecutionFuture == null || !taskExecutionFuture.isCancelled()) {
                    handleException(taskExecution, e);
                }
            } finally {
                latch.countDown();
            }
        });

        taskExecutionFutureMap.put(taskExecution.getId(), new TaskExecutionFuture<>(taskExecution, future));

        try {
            future.get(calculateTimeout(taskExecution), TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            handleException(taskExecution, e);
        } catch (CancellationException e) {
            logger.debug("Cancelled task: {}", taskExecution.getId());
        } finally {
            try {
                latch.await();
            } catch (InterruptedException e) {
                handleException(taskExecution, e);
            }

            taskExecutionFutureMap.remove(taskExecution.getId());
        }
    }

    /**
     * Handle cancel control tasks to stop an ongoing task or to adjust something on a worker outside the context of a
     * job.
     */
    public void onCancelControlTaskEvent(MessageEvent<?> event) {
        if (event instanceof CancelControlTaskEvent cancelControlTaskEvent) {
            CancelControlTask cancelControlTask = cancelControlTaskEvent.getControlTask();

            logger.debug("onCancelControlTaskEvent: cancelControlTask={}", cancelControlTask);

            Long jobId = cancelControlTask.getJobId();

            for (TaskExecutionFuture<?> taskExecutionFuture : taskExecutionFutureMap.values()) {
                if (Objects.equals(taskExecutionFuture.taskExecution.getJobId(), jobId)) {
                    logger.info(
                        "Cancelling task jobId={}->taskExecutionId={}", jobId,
                        taskExecutionFuture.taskExecution.getId());

                    taskExecutionFuture.cancel(true);
                }
            }
        }
    }

    Map<Long, TaskExecutionFuture<?>> getTaskExecutions() {
        return Collections.unmodifiableMap(taskExecutionFutureMap);
    }

    private TaskExecution doExecuteTask(TaskExecution taskExecution) throws Exception {
        Map<String, Object> context = new HashMap<>();

        try {
            long startTime = System.currentTimeMillis();

            // pre tasks
            executeSubTasks(Validate.notNull(taskExecution.getJobId(), "id"), taskExecution.getPre(), context);

            taskExecution.evaluate(context, evaluator);

            TaskHandler<?> taskHandler = taskHandlerResolver.resolve(taskExecution);

            Object output = taskHandler.handle(taskExecution.clone());

            if (output != null) {
                for (TaskExecutionPostOutputProcessor taskExecutionPostOutputProcessor : taskExecutionPostOutputProcessors) {
                    output = taskExecutionPostOutputProcessor.process(taskExecution, output);
                }

                taskExecution.setOutput(
                    taskFileStorage.storeTaskExecutionOutput(Validate.notNull(taskExecution.getId(), "id"), output));
            }

            taskExecution.setEndDate(Instant.now());
            taskExecution.setExecutionTime(System.currentTimeMillis() - startTime);
            taskExecution.setProgress(100);
            taskExecution.setStatus(Status.COMPLETED);

            // post tasks
            executeSubTasks(taskExecution.getJobId(), taskExecution.getPost(), context);

            return taskExecution;
        } finally {
            // finalize tasks
            executeSubTasks(Validate.notNull(taskExecution.getJobId(), "id"), taskExecution.getFinalize(), context);
        }
    }

    private Object doExecuteSubTask(TaskExecution taskExecution) throws Exception {
        Map<String, Object> context = new HashMap<>();

        try {
            long startTime = System.currentTimeMillis();

            // pre tasks
            executeSubTasks(Validate.notNull(taskExecution.getJobId(), "id"), taskExecution.getPre(), context);

            taskExecution.evaluate(context, evaluator);

            TaskHandler<?> taskHandler = taskHandlerResolver.resolve(taskExecution);

            Object output = taskHandler.handle(taskExecution.clone());

            taskExecution.setEndDate(Instant.now());
            taskExecution.setExecutionTime(System.currentTimeMillis() - startTime);
            taskExecution.setProgress(100);
            taskExecution.setStatus(Status.COMPLETED);

            // post tasks
            executeSubTasks(taskExecution.getJobId(), taskExecution.getPost(), context);

            return output;
        } finally {
            // finalize tasks
            executeSubTasks(Validate.notNull(taskExecution.getJobId(), "id"), taskExecution.getFinalize(), context);
        }
    }

    private void executeSubTasks(
        long jobId, List<WorkflowTask> subWorkflowTasks, Map<String, Object> context) throws Exception {

        for (WorkflowTask subWorkflowTask : subWorkflowTasks) {
            TaskExecution subTaskExecution = TaskExecution.builder()
                .jobId(jobId)
                .workflowTask(subWorkflowTask)
                .build();

            subTaskExecution.evaluate(context, evaluator);

            Object output = doExecuteSubTask(subTaskExecution);

            if (subTaskExecution.getName() != null && output != null) {
                context.put(subTaskExecution.getName(), output);
            }
        }
    }

    private void handleException(TaskExecution taskExecution, Exception exception) {
        while (exception.getCause() != null && exception.getCause() instanceof Exception cause) {
            exception = cause;
        }

        logger.error(exception.getMessage(), exception);

        taskExecution.setError(
            new ExecutionError(exception.getMessage(), Arrays.asList(ExceptionUtils.getStackFrames(exception))));
        taskExecution.setStatus(Status.FAILED);

        eventPublisher.publishEvent(new TaskExecutionErrorEvent(taskExecution));
    }

    private long calculateTimeout(TaskExecution taskExecution) {
        if (taskExecution.getTimeout() != null) {
            Duration duration = Duration.parse("PT" + taskExecution.getTimeout());

            return duration.toMillis();
        }

        return DEFAULT_TIME_OUT;
    }

    private static class TaskExecutionFuture<T> implements Future<T> {

        private final Future<T> future;
        private final TaskExecution taskExecution;

        TaskExecutionFuture(TaskExecution taskExecution, Future<T> future) {
            this.taskExecution = Validate.notNull(taskExecution, "taskExecution");
            this.future = Validate.notNull(future, "future");
        }

        @Override
        public boolean cancel(boolean aMayInterruptIfRunning) {
            return future.cancel(aMayInterruptIfRunning);
        }

        @Override
        public boolean isCancelled() {
            return future.isCancelled();
        }

        @Override
        public boolean isDone() {
            return future.isDone();
        }

        @Override
        public T get() throws InterruptedException, ExecutionException {
            return future.get();
        }

        @Override
        public T get(long timeout, TimeUnit timeUnit)
            throws InterruptedException, ExecutionException, TimeoutException {

            return future.get(timeout, timeUnit);
        }
    }
}
