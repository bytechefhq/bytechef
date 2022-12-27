
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

package com.bytechef.atlas.event;

import org.springframework.util.Assert;

/**
 * @author Ivica Cardic
 */
public class TaskStartedWorkflowEvent extends WorkflowEvent {

    public static final String TASK_STARTED = "task.started";

    private String jobId;
    private String taskId;

    public TaskStartedWorkflowEvent() {
        this.type = TASK_STARTED;
    }

    public TaskStartedWorkflowEvent(String taskId) {
        Assert.notNull(taskId, "taskId must not be null");

        this.taskId = taskId;
        this.type = TASK_STARTED;
    }

    public TaskStartedWorkflowEvent(String jobId, String taskId) {
        Assert.notNull(jobId, "jobId must not be null");
        Assert.notNull(taskId, "taskId must not be null");

        this.jobId = jobId;
        this.taskId = taskId;
        this.type = TASK_STARTED;
    }

    public String getJobId() {
        return jobId;
    }

    public String getTaskId() {
        return taskId;
    }

    @Override
    public String toString() {
        return "TaskStartedWorkflowEvent{" + "jobId='"
            + jobId + '\'' + ", taskId='"
            + taskId + '\'' + "} "
            + super.toString();
    }
}
