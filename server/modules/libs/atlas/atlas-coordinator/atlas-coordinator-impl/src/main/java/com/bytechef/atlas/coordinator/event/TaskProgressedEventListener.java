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

import com.bytechef.atlas.event.Events;
import com.bytechef.atlas.event.WorkflowEvent;
import com.bytechef.atlas.task.execution.domain.SimpleTaskExecution;
import com.bytechef.atlas.task.execution.domain.TaskExecution;
import com.bytechef.task.execution.service.TaskExecutionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Arik Cohen
 * @since Sep 06, 2018
 */
public class TaskProgressedEventListener implements EventListener {

    private final TaskExecutionService taskExecutionService;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public TaskProgressedEventListener(TaskExecutionService taskExecutionService) {
        this.taskExecutionService = taskExecutionService;
    }

    @Override
    public void onApplicationEvent(WorkflowEvent aEvent) {
        if (Events.TASK_PROGRESSED.equals(aEvent.getType())) {
            String taskId = aEvent.getString("taskId");
            int progress = aEvent.getInteger("progress", 0);

            TaskExecution task = taskExecutionService.getTaskExecution(taskId);

            if (task == null) {
                logger.error("Unknown task: {}", taskId);
            } else {
                SimpleTaskExecution mtask = SimpleTaskExecution.of(task);

                mtask.setProgress(progress);

                taskExecutionService.merge(mtask);
            }
        }
    }
}
