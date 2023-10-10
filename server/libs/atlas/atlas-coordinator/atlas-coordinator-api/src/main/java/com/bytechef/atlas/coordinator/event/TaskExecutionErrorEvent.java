
/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.atlas.coordinator.event;

import com.bytechef.atlas.coordinator.message.route.CoordinatorMessageRoute;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.error.ExecutionError;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.lang3.Validate;

/**
 * @author Ivica CardFic
 */
public class TaskExecutionErrorEvent extends AbstractEvent implements ErrorEvent {

    private TaskExecution taskExecution;

    private TaskExecutionErrorEvent() {
    }

    @SuppressFBWarnings("EI")
    public TaskExecutionErrorEvent(TaskExecution taskExecution) {
        super(CoordinatorMessageRoute.ERROR_EVENTS);

        Validate.notNull(taskExecution, "'taskExecution' must not be null");

        this.taskExecution = taskExecution;
    }

    @Override
    public ExecutionError getError() {
        return taskExecution.getError();
    }

    @SuppressFBWarnings("EI")
    public TaskExecution getTaskExecution() {
        return taskExecution;
    }

    @Override
    public String toString() {
        return "TaskExecutionErrorEvent{" +
            "taskExecution=" + taskExecution +
            ", createdDate=" + createDate +
            ", route=" + route +
            "} ";
    }
}
