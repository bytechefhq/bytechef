
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

package com.bytechef.atlas.coordinator.event;

import com.bytechef.atlas.coordinator.message.route.CoordinatorMessageRoute;

/**
 * @author Ivica Cardic
 */
public class TaskStartedApplicationEvent extends AbstractEvent implements ApplicationEvent {

    public static final String TASK_STARTED = "task.started";

    private Long jobId;
    private long taskExecutionId;

    private TaskStartedApplicationEvent() {
    }

    public TaskStartedApplicationEvent(long taskExecutionId) {
        super(CoordinatorMessageRoute.APPLICATION_EVENTS);

        this.taskExecutionId = taskExecutionId;
    }

    public TaskStartedApplicationEvent(long jobId, long taskExecutionId) {
        super(CoordinatorMessageRoute.APPLICATION_EVENTS);

        this.jobId = jobId;
        this.taskExecutionId = taskExecutionId;
    }

    public Long getJobId() {
        return jobId;
    }

    public long getTaskExecutionId() {
        return taskExecutionId;
    }

    @Override
    public String toString() {
        return "TaskStartedEvent{" +
            "jobId=" + jobId +
            ", taskExecutionId=" + taskExecutionId +
            ", createdDate=" + createDate +
            ", route=" + route +
            "} ";
    }
}
