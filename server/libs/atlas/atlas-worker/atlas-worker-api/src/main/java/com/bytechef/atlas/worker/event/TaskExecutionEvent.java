
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

package com.bytechef.atlas.worker.event;

import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.worker.message.route.WorkerMessageRoute;
import com.bytechef.message.Prioritizable;
import com.bytechef.message.Retryable;
import com.bytechef.message.event.MessageEvent;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.lang3.Validate;

import java.time.LocalDateTime;

/**
 * @author Ivica CardFic
 */
public class TaskExecutionEvent implements Prioritizable, Retryable, MessageEvent<WorkerMessageRoute> {

    private LocalDateTime createdDate;
    private WorkerMessageRoute route;
    private TaskExecution taskExecution;

    private TaskExecutionEvent() {
    }

    @SuppressFBWarnings("EI")
    public TaskExecutionEvent(TaskExecution taskExecution) {
        this(WorkerMessageRoute.TASK_EXECUTION_EVENTS, taskExecution);
    }

    @SuppressFBWarnings("EI")
    public TaskExecutionEvent(WorkerMessageRoute route, TaskExecution taskExecution) {
        Validate.notNull(taskExecution, "'taskExecution' must not be null");

        this.createdDate = LocalDateTime.now();
        this.route = route;
        this.taskExecution = taskExecution;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    @Override
    public int getPriority() {
        return taskExecution.getPriority();
    }

    @Override
    public int getMaxRetries() {
        return taskExecution.getMaxRetries();
    }

    @Override
    public int getRetryAttempts() {
        return taskExecution.getRetryAttempts();
    }

    @Override
    public String getRetryDelay() {
        return taskExecution.getRetryDelay();
    }

    @Override
    public long getRetryDelayMillis() {
        return taskExecution.getRetryDelayMillis();
    }

    @Override
    public int getRetryDelayFactor() {
        return taskExecution.getRetryDelayFactor();
    }

    @Override
    public WorkerMessageRoute getRoute() {
        return route;
    }

    @Override
    public LocalDateTime getCreateDate() {
        return createdDate;
    }

    @SuppressFBWarnings("EI")
    public TaskExecution getTaskExecution() {
        return taskExecution;
    }

    @Override
    public String toString() {
        return "TaskExecutionEvent{" +
            "taskExecution=" + taskExecution +
            ", route=" + route +
            ", createdDate=" + createdDate +
            "} ";
    }
}
