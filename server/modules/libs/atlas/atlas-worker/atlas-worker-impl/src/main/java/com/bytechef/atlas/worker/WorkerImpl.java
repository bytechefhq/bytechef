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

import com.bytechef.atlas.context.domain.MapContext;
import com.bytechef.atlas.error.ErrorObject;
import com.bytechef.atlas.event.EventPublisher;
import com.bytechef.atlas.event.Events;
import com.bytechef.atlas.event.WorkflowEvent;
import com.bytechef.atlas.message.broker.MessageBroker;
import com.bytechef.atlas.message.broker.Queues;
import com.bytechef.atlas.task.ControlTask;
import com.bytechef.atlas.task.WorkflowTask;
import com.bytechef.atlas.task.execution.TaskStatus;
import com.bytechef.atlas.task.execution.domain.SimpleTaskExecution;
import com.bytechef.atlas.task.execution.domain.TaskExecution;
import com.bytechef.atlas.task.execution.evaluator.TaskEvaluator;
import com.bytechef.atlas.uuid.UUIDGenerator;
import com.bytechef.atlas.worker.task.handler.TaskHandler;
import com.bytechef.atlas.worker.task.handler.TaskHandlerResolver;
import java.time.Duration;
import java.util.Collections;
import java.util.Date;
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
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The class responsible for executing tasks spawned by the {@link
 * com.bytechef.atlas.coordinator.Coordinator}.
 *
 * <p>Worker threads typically execute on a different process than the {@link
 * com.bytechef.atlas.coordinator.Coordinator} process and most likely on a seperate node
 * altogether.
 *
 * <p>Communication between the two is decoupled through the {@link MessageBroker} interface.
 *
 * @author Arik Cohen
 * @since Jun 12, 2016
 */
public class WorkerImpl implements Worker {

    private final Map<String, TaskExecutionFuture<?>> taskExecutions = new ConcurrentHashMap<>();
    private final TaskEvaluator taskEvaluator;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final TaskHandlerResolver taskHandlerResolver;
    private final MessageBroker messageBroker;
    private final EventPublisher eventPublisher;
    private final ExecutorService executors;

    private static final long DEFAULT_TIME_OUT = 24 * 60 * 60 * 1000; // 24 hours

    private WorkerImpl(BuilderImpl aBuilder) {
        taskHandlerResolver = Objects.requireNonNull(aBuilder.taskHandlerResolver);
        messageBroker = Objects.requireNonNull(aBuilder.messageBroker);
        eventPublisher = Objects.requireNonNull(aBuilder.eventPublisher);
        executors = Objects.requireNonNull(aBuilder.executors);
        taskEvaluator = Objects.requireNonNull(aBuilder.taskEvaluator);
    }

    /**
     * Handle the execution of a {@link TaskExecution}. Implementors are expected to execute the
     * task asynchronously.
     *
     * @param taskExecution The task to execute.
     * @throws InterruptedException
     */
    @Override
    public void handle(TaskExecution taskExecution) {
        CountDownLatch latch = new CountDownLatch(1);
        Future<?> future = executors.submit(() -> {
            try {
                eventPublisher.publishEvent(WorkflowEvent.of(
                        Events.TASK_STARTED, "taskId", taskExecution.getId(), "jobId", taskExecution.getJobId()));
                SimpleTaskExecution completion = doExecuteTask(taskExecution);
                messageBroker.send(Queues.COMPLETIONS, completion);
            } catch (InterruptedException e) {
                // ignore
            } catch (Exception e) {
                TaskExecutionFuture<?> myFuture = taskExecutions.get(taskExecution.getId());
                if (myFuture == null || !myFuture.isCancelled()) {
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
     * Handle control tasks. Control tasks are used by the Coordinator to control Worker instances.
     * For example to stop an ongoing task or to adjust something on a worker outside the context of
     * a job.
     */
    public void handle(ControlTask aControlTask) {
        logger.debug("received control task: {}", aControlTask);
        if (ControlTask.TYPE_CANCEL.equals(aControlTask.getType())) {
            String jobId = aControlTask.getRequiredString("jobId");
            for (TaskExecutionFuture<?> tef : taskExecutions.values()) {
                if (tef.taskExecution.getJobId().equals(jobId)) {
                    logger.info("Cancelling task {}->{}", jobId, tef.taskExecution.getId());
                    tef.cancel(true);
                }
            }
        }
    }

    Map<String, TaskExecutionFuture<?>> getTaskExecutions() {
        return Collections.unmodifiableMap(taskExecutions);
    }

    private SimpleTaskExecution doExecuteTask(TaskExecution aTask) throws Exception {
        MapContext context = new MapContext();
        try {
            long startTime = System.currentTimeMillis();
            logger.debug("Recived task: {}", aTask);

            // pre tasks
            executeSubTasks(aTask, aTask.getPre(), context);

            TaskExecution evaluatedTask = taskEvaluator.evaluate(aTask, context);

            TaskHandler<?> taskHandler = taskHandlerResolver.resolve(evaluatedTask);
            Object output = taskHandler.handle(evaluatedTask);
            SimpleTaskExecution completion = SimpleTaskExecution.of(evaluatedTask);
            if (output != null) {
                completion.setOutput(output);
            }
            completion.setStatus(TaskStatus.COMPLETED);
            completion.setProgress(100);
            completion.setEndTime(new Date());
            completion.setExecutionTime(System.currentTimeMillis() - startTime);

            // post tasks
            executeSubTasks(aTask, aTask.getPost(), context);

            return completion;
        } finally {
            // finalize tasks
            executeSubTasks(aTask, aTask.getFinalize(), context);
        }
    }

    private void executeSubTasks(TaskExecution aTask, List<WorkflowTask> aSubTasks, MapContext aContext)
            throws Exception {
        for (WorkflowTask subTask : aSubTasks) {
            SimpleTaskExecution subTaskExecution = new SimpleTaskExecution(subTask.asMap());
            subTaskExecution.setId(UUIDGenerator.generate());
            subTaskExecution.setJobId(aTask.getJobId());
            TaskExecution evaluatedTask = taskEvaluator.evaluate(subTaskExecution, aContext);
            SimpleTaskExecution completion = doExecuteTask(evaluatedTask);
            if (completion.getName() != null) {
                aContext.set(completion.getName(), completion.getOutput());
            }
        }
    }

    private void handleException(TaskExecution aTask, Exception aException) {
        logger.error(aException.getMessage(), aException);
        SimpleTaskExecution task = SimpleTaskExecution.of(aTask);
        task.setError(new ErrorObject(aException.getMessage(), ExceptionUtils.getStackFrames(aException)));
        task.setStatus(TaskStatus.FAILED);
        messageBroker.send(Queues.ERRORS, task);
    }

    private long calculateTimeout(TaskExecution aTask) {
        if (aTask.getTimeout() != null) {
            return Duration.parse("PT" + aTask.getTimeout()).toMillis();
        }
        return DEFAULT_TIME_OUT;
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    public static class BuilderImpl implements Worker.Builder {

        private TaskHandlerResolver taskHandlerResolver;
        private MessageBroker messageBroker;
        private EventPublisher eventPublisher;
        private ExecutorService executors = Executors.newCachedThreadPool();
        private TaskEvaluator taskEvaluator;

        public Builder withTaskHandlerResolver(TaskHandlerResolver taskHandlerResolver) {
            this.taskHandlerResolver = taskHandlerResolver;
            return this;
        }

        public Builder withTaskEvaluator(TaskEvaluator taskEvaluator) {
            this.taskEvaluator = taskEvaluator;
            return this;
        }

        public Builder withMessageBroker(MessageBroker messageBroker) {
            this.messageBroker = messageBroker;
            return this;
        }

        public Builder withEventPublisher(EventPublisher eventPublisher) {
            this.eventPublisher = eventPublisher;
            return this;
        }

        public Builder withExecutors(ExecutorService executorService) {
            executors = executorService;
            return this;
        }

        public WorkerImpl build() {
            return new WorkerImpl(this);
        }
    }

    private static class TaskExecutionFuture<T> implements Future<T> {

        private final Future<T> future;
        private final TaskExecution taskExecution;

        TaskExecutionFuture(TaskExecution aTaskExecution, Future<T> aFuture) {
            taskExecution = Objects.requireNonNull(aTaskExecution);
            future = Objects.requireNonNull(aFuture);
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
        public T get(long aTimeout, TimeUnit aUnit) throws InterruptedException, ExecutionException, TimeoutException {
            return future.get(aTimeout, aUnit);
        }
    }
}
