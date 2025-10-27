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

package com.bytechef.atlas.worker.event;

import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.worker.message.route.TaskWorkerMessageRoute;
import com.bytechef.message.Prioritizable;
import com.bytechef.message.Retryable;
import com.bytechef.message.event.MessageEvent;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.util.Assert;

/**
 * @author Ivica CardFic
 */
public class TaskExecutionEvent extends AbstractEvent
    implements Prioritizable, Retryable, MessageEvent<TaskWorkerMessageRoute> {

    private TaskWorkerMessageRoute route;
    private TaskExecution taskExecution;

    private TaskExecutionEvent() {
    }

    public TaskExecutionEvent(TaskExecution taskExecution) {
        this(TaskWorkerMessageRoute.TASK_EXECUTION_EVENTS, taskExecution);
    }

    @SuppressFBWarnings("EI")
    public TaskExecutionEvent(TaskWorkerMessageRoute route, TaskExecution taskExecution) {
        Assert.notNull(taskExecution, "'taskExecution' must not be null");

        this.route = route;
        this.taskExecution = taskExecution;
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
    public TaskWorkerMessageRoute getRoute() {
        return route;
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
            ", createDate=" + createDate +
            "} ";
    }
}
