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
 * @author Ivica Cardic
 */
public class TaskProgressedApplicationEvent extends AbstractEvent implements ApplicationEvent {

    private int progress;
    private long taskExecutionId;

    private TaskProgressedApplicationEvent() {
    }

    public TaskProgressedApplicationEvent(long taskExecutionId, int progress) {
        super(TaskCoordinatorMessageRoute.APPLICATION_EVENTS);

        this.progress = progress;
        this.taskExecutionId = taskExecutionId;
    }

    public int getProgress() {
        return progress;
    }

    public long getTaskExecutionId() {
        return taskExecutionId;
    }

    @Override
    public String toString() {
        return "TaskProgressedEvent{" +
            "progress=" + progress +
            ", taskExecutionId=" + taskExecutionId +
            ", createdDate=" + createDate +
            ", route=" + route +
            "} ";
    }
}
