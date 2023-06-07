
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

package com.bytechef.atlas.coordinator.event;

import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.event.TaskProgressedWorkflowEvent;
import com.bytechef.event.listener.EventListener;
import com.bytechef.event.WorkflowEvent;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * @author Arik Cohen
 * @since Sep 06, 2018
 */
public class TaskProgressedEventListener implements EventListener {

    private final TaskExecutionService taskExecutionService;

    @SuppressFBWarnings("EI2")
    public TaskProgressedEventListener(TaskExecutionService taskExecutionService) {
        this.taskExecutionService = taskExecutionService;
    }

    @Override
    public void onApplicationEvent(WorkflowEvent workflowEvent) {
        if (TaskProgressedWorkflowEvent.TASK_PROGRESSED.equals(workflowEvent.getType())) {
            TaskProgressedWorkflowEvent taskProgressedWorkflowEvent = (TaskProgressedWorkflowEvent) workflowEvent;

            TaskExecution taskExecution = taskExecutionService.getTaskExecution(
                taskProgressedWorkflowEvent.getTaskExecutionId());

            taskExecution.setProgress(taskProgressedWorkflowEvent.getProgress());

            taskExecutionService.update(taskExecution);
        }
    }
}
