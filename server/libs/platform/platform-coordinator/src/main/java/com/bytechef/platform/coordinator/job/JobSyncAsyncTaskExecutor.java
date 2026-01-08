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

import com.bytechef.atlas.coordinator.event.TaskExecutionErrorEvent;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.worker.event.TaskExecutionEvent;
import com.bytechef.error.ExecutionError;
import com.bytechef.tenant.util.TenantCacheKeyUtils;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;

/**
 * An implementation of the {@link AsyncTaskExecutor} interface that provides a mechanism for executing tasks with a
 * synchronization layer ensuring task execution limits and coordination via an event publisher. <br />
 * This class is designed to maintain counters for task executions associated with specific job identifiers and enforce
 * a maximum execution limit for tasks. It integrates with a {@link ApplicationEventPublisher} to publish error events
 * when task execution limits are exceeded.
 *
 * @author Ivica Cardic
 */
class JobSyncAsyncTaskExecutor implements AsyncTaskExecutor {

    private final ApplicationEventPublisher coordinatorEventPublisher;
    private final int maxTaskExecutions;
    private final Map<String, AtomicInteger> taskExecutionCounters = new ConcurrentHashMap<>();
    private final TaskExecutor taskExecutor;

    JobSyncAsyncTaskExecutor(
        ApplicationEventPublisher coordinatorEventPublisher, TaskExecutor taskExecutor, int maxTaskExecutions) {

        this.coordinatorEventPublisher = coordinatorEventPublisher;
        this.maxTaskExecutions = maxTaskExecutions;
        this.taskExecutor = taskExecutor;
    }

    @Override
    public void execute(Runnable task) {
        taskExecutor.execute(task);
    }

    /**
     * Increments the execution counter for the task associated with the provided {@link TaskExecutionEvent} and checks
     * if the maximum allowed task executions for the associated job has been exceeded. If the limit is exceeded, the
     * task's status is set to {@code FAILED}, an error is recorded, and a {@link TaskExecutionErrorEvent} is published.
     *
     * @param taskExecutionEvent the event containing the task execution details. Must not be null.
     */
    public void incrementAndCheck(TaskExecutionEvent taskExecutionEvent) {
        TaskExecution taskExecution = taskExecutionEvent.getTaskExecution();

        AtomicInteger taskExecutionCounter = taskExecutionCounters.computeIfAbsent(
            TenantCacheKeyUtils.getKey(Objects.requireNonNull(taskExecution.getJobId())),
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

    /**
     * Clears the execution counter associated with the specified job ID. This method removes the counter entry from the
     * internal task execution counter map, effectively resetting the count for the given job.
     *
     * @param jobId the unique identifier of the job for which the execution counter should be cleared. This value must
     *              correspond to a valid job ID used in the system.
     */
    public void clearCounter(long jobId) {
        taskExecutionCounters.remove(TenantCacheKeyUtils.getKey(jobId));
    }
}
