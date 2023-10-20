
/*
 * Copyright 2016-2018 the original author or authors.
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
 * Modifications copyright (C) 2021 <your company/name>
 */

package com.bytechef.atlas.worker;

import com.bytechef.atlas.domain.TaskExecution;
import com.bytechef.atlas.message.broker.TaskMessageRoute;
import com.bytechef.atlas.task.ControlTask;
import com.bytechef.error.ExecutionError;
import com.bytechef.event.EventPublisher;
import com.bytechef.atlas.event.TaskStartedWorkflowEvent;
import com.bytechef.message.broker.MessageBroker;
import com.bytechef.message.broker.SystemMessageRoute;
import com.bytechef.atlas.task.CancelControlTask;
import com.bytechef.atlas.task.WorkflowTask;
import com.bytechef.atlas.task.evaluator.TaskEvaluator;
import com.bytechef.atlas.worker.task.handler.TaskHandler;
import com.bytechef.atlas.worker.task.handler.TaskHandlerResolver;
import com.bytechef.commons.util.ExceptionUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Duration;
import java.time.LocalDateTime;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The class responsible for executing tasks spawned by the {@link com.bytechef.atlas.coordinator.TaskCoordinator}.
 *
 * <p>
 * Worker threads typically execute on a different process than the
 * {@link com.bytechef.atlas.coordinator.TaskCoordinator} process and most likely on a seperate node altogether.
 *
 * <p>
 * Communication between the two is decoupled through the {@link MessageBroker} interface.
 *
 * @author Arik Cohen
 * @since Jun 12, 2016
 */
public class TaskWorker {

    private static final Logger logger = LoggerFactory.getLogger(TaskWorker.class);

    private static final long DEFAULT_TIME_OUT = 24 * 60 * 60 * 1000; // 24 hours

    private final EventPublisher eventPublisher;
    private final ExecutorService executorService;
    private final MessageBroker messageBroker;
    private final TaskHandlerResolver taskHandlerResolver;
    private final TaskEvaluator taskEvaluator;
    private final Map<Long, TaskExecutionFuture<?>> taskExecutions = new ConcurrentHashMap<>();

    private TaskWorker(Builder builder) {
        taskHandlerResolver = Objects.requireNonNull(builder.taskHandlerResolver);
        messageBroker = Objects.requireNonNull(builder.messageBroker);
        eventPublisher = Objects.requireNonNull(builder.eventPublisher);
        executorService = Objects.requireNonNull(builder.executorService);
        taskEvaluator = Objects.requireNonNull(builder.taskEvaluator);
    }

    /**
     * Handle the execution of a {@link TaskExecution}. Implementors are expected to execute the task asynchronously.
     *
     * @param taskExecution The task to execute.
     */
    @SuppressFBWarnings("NP")
    public void handle(TaskExecution taskExecution) {
        logger.debug("Received task: {}", taskExecution);

        CountDownLatch latch = new CountDownLatch(1);

        Future<?> future = executorService.submit(() -> {
            try {
                eventPublisher.publishEvent(
                    new TaskStartedWorkflowEvent(taskExecution.getJobId(), taskExecution.getId()));

                TaskExecution completedTaskExecution = doExecuteTask(taskExecution);

                messageBroker.send(TaskMessageRoute.TASKS_COMPLETIONS, completedTaskExecution);
            } catch (InterruptedException e) {
                // ignore
            } catch (Exception e) {
                TaskExecutionFuture<?> taskExecutionFuture = taskExecutions.get(taskExecution.getId());

                if (taskExecutionFuture == null || !taskExecutionFuture.isCancelled()) {
                    handleException(taskExecution, e);
                }
            } finally {
                latch.countDown();
            }
        });

        taskExecutions.put(taskExecution.getId(), new TaskExecutionFuture<>(taskExecution, future));

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

            taskExecutions.remove(taskExecution.getId());
        }
    }

    /**
     * Handle control tasks. Control tasks are used by the Coordinator to control Worker instances. For example, to stop
     * an ongoing task or to adjust something on a worker outside the context of a job.
     */
    public void handle(ControlTask controlTask) {
        logger.debug("Received control task: {}", controlTask);

        if (CancelControlTask.TYPE_CANCEL.equals(controlTask.getType())) {
            Long jobId = ((CancelControlTask) controlTask).getJobId();

            for (TaskExecutionFuture<?> taskExecutionFuture : taskExecutions.values()) {
                if (Objects.equals(taskExecutionFuture.taskExecution.getJobId(), jobId)) {
                    logger.info("Cancelling task {}->{}", jobId, taskExecutionFuture.taskExecution.getId());

                    taskExecutionFuture.cancel(true);
                }
            }
        }
    }

    Map<Long, TaskExecutionFuture<?>> getTaskExecutions() {
        return Collections.unmodifiableMap(taskExecutions);
    }

    private TaskExecution doExecuteTask(TaskExecution taskExecution) throws Exception {
        Map<String, Object> context = new HashMap<>();

        try {
            long startTime = System.currentTimeMillis();

            // pre tasks
            executeSubTasks(taskExecution, taskExecution.getPre(), context);

            taskExecution = taskEvaluator.evaluate(taskExecution, context);

            TaskHandler<?> taskHandler = taskHandlerResolver.resolve(taskExecution);

            Object output = taskHandler.handle(
                TaskExecution.builder()
                    .id(taskExecution.getId())
                    .jobId(taskExecution.getJobId())
                    .maxRetries(taskExecution.getMaxRetries())
                    .parentId(taskExecution.getParentId())
                    .priority(taskExecution.getPriority())
                    .progress(taskExecution.getProgress())
                    .retryAttempts(taskExecution.getRetryAttempts())
                    .retryDelay(taskExecution.getRetryDelay())
                    .retryDelayFactor(taskExecution.getRetryDelayFactor())
                    .status(taskExecution.getStatus())
                    .startDate(taskExecution.getStartDate())
                    .taskNumber(taskExecution.getTaskNumber())
                    .workflowTask(taskExecution.getWorkflowTask())
                    .build());

            if (output != null) {
                taskExecution.setOutput(output);
            }

            taskExecution.setEndDate(LocalDateTime.now());
            taskExecution.setExecutionTime(System.currentTimeMillis() - startTime);
            taskExecution.setProgress(100);
            taskExecution.setStatus(TaskExecution.Status.COMPLETED);

            // post tasks
            executeSubTasks(taskExecution, taskExecution.getPost(), context);

            return taskExecution;
        } finally {
            // finalize tasks
            executeSubTasks(taskExecution, taskExecution.getFinalize(), context);
        }
    }

    private void executeSubTasks(
        TaskExecution taskExecution, List<WorkflowTask> subWorkflowTasks, Map<String, Object> context)
        throws Exception {

        for (WorkflowTask subWorkflowTask : subWorkflowTasks) {
            TaskExecution subTaskExecution = TaskExecution.builder()
                .jobId(taskExecution.getJobId())
                .workflowTask(subWorkflowTask)
                .build();

            subTaskExecution = taskEvaluator.evaluate(subTaskExecution, context);

            TaskExecution completionTaskExecution = doExecuteTask(subTaskExecution);

            if (completionTaskExecution.getName() != null) {
                context.put(completionTaskExecution.getName(), completionTaskExecution.getOutput());
            }
        }
    }

    private void handleException(TaskExecution taskExecution, Exception exception) {
        logger.error(exception.getMessage(), exception);

        taskExecution.setError(
            new ExecutionError(exception.getMessage(), Arrays.asList(ExceptionUtils.getStackFrames(exception))));
        taskExecution.setStatus(TaskExecution.Status.FAILED);

        messageBroker.send(SystemMessageRoute.ERRORS, taskExecution);
    }

    private long calculateTimeout(TaskExecution taskExecution) {
        if (taskExecution.getTimeout() != null) {
            Duration duration = Duration.parse("PT" + taskExecution.getTimeout());

            return duration.toMillis();
        }

        return DEFAULT_TIME_OUT;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private TaskHandlerResolver taskHandlerResolver;
        private MessageBroker messageBroker;
        private EventPublisher eventPublisher;
        private ExecutorService executorService = Executors.newCachedThreadPool();
        private TaskEvaluator taskEvaluator;

        public Builder taskHandlerResolver(TaskHandlerResolver taskHandlerResolver) {
            this.taskHandlerResolver = taskHandlerResolver;

            return this;
        }

        public Builder taskEvaluator(TaskEvaluator taskEvaluator) {
            this.taskEvaluator = taskEvaluator;

            return this;
        }

        public Builder messageBroker(MessageBroker messageBroker) {
            this.messageBroker = messageBroker;

            return this;
        }

        public Builder eventPublisher(EventPublisher eventPublisher) {
            this.eventPublisher = eventPublisher;

            return this;
        }

        public Builder executorService(ExecutorService executorService) {
            this.executorService = executorService;

            return this;
        }

        public TaskWorker build() {
            return new TaskWorker(this);
        }
    }

    private static class TaskExecutionFuture<T> implements Future<T> {

        private final Future<T> future;
        private final TaskExecution taskExecution;

        TaskExecutionFuture(TaskExecution taskExecution, Future<T> future) {
            this.taskExecution = Objects.requireNonNull(taskExecution);
            this.future = Objects.requireNonNull(future);
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
