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
public class TaskProgressedWorkflowEvent extends WorkflowEvent {

    public static final String TASK_PROGRESSED = "task.progressed";

    private int progress;
    private String taskId;

    public TaskProgressedWorkflowEvent() {
        this.type = TASK_PROGRESSED;
    }

    public TaskProgressedWorkflowEvent(String taskId, int progress) {
        Assert.notNull(taskId, "taskId must not be null");
        Assert.notNull(progress, "progress must not be null");

        this.progress = progress;
        this.taskId = taskId;
        this.type = TASK_PROGRESSED;
    }

    public int getProgress() {
        return progress;
    }

    public String getTaskId() {
        return taskId;
    }

    @Override
    public String toString() {
        return "TaskProgressedWorkflowEvent{" + "progress="
                + progress + ", taskId='"
                + taskId + '\'' + "} "
                + super.toString();
    }
}
