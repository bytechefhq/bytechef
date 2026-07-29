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

package com.bytechef.atlas.coordinator.event;

import com.bytechef.atlas.coordinator.message.route.TaskCoordinatorMessageRoute;

/**
 * Periodic liveness signal published by a worker while a task execution is in flight. The coordinator uses it to keep
 * the task execution row's last-modified timestamp fresh, so orphan detection can distinguish a long-running task on a
 * live worker from a task whose worker died.
 *
 * @author Ivica Cardic
 */
public class TaskHeartbeatApplicationEvent extends AbstractEvent implements ApplicationEvent {

    private long taskExecutionId;

    private TaskHeartbeatApplicationEvent() {
    }

    public TaskHeartbeatApplicationEvent(long taskExecutionId) {
        super(TaskCoordinatorMessageRoute.APPLICATION_EVENTS);

        this.taskExecutionId = taskExecutionId;
    }

    public long getTaskExecutionId() {
        return taskExecutionId;
    }

    @Override
    public String toString() {
        return "TaskHeartbeatApplicationEvent{" +
            "taskExecutionId=" + taskExecutionId +
            ", createdDate=" + createDate +
            ", route=" + route +
            "} ";
    }
}
