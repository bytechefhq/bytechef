/*
 * Copyright 2016-2020 the original author or authors.
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
 * Modifications copyright (C) 2025 ByteChef
 */

package com.bytechef.atlas.coordinator.event.listener;

import com.bytechef.atlas.coordinator.event.ApplicationEvent;
import com.bytechef.atlas.coordinator.event.TaskProgressedApplicationEvent;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * @author Arik Cohen
 * @since Sep 06, 2018
 */
public class TaskProgressedApplicationEventListener implements ApplicationEventListener {

    private final TaskExecutionService taskExecutionService;

    @SuppressFBWarnings("EI2")
    public TaskProgressedApplicationEventListener(TaskExecutionService taskExecutionService) {
        this.taskExecutionService = taskExecutionService;
    }

    @Override
    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        if (applicationEvent instanceof TaskProgressedApplicationEvent taskProgressedWorkflowEvent) {
            TaskExecution taskExecution = taskExecutionService.getTaskExecution(
                taskProgressedWorkflowEvent.getTaskExecutionId());

            taskExecution.setProgress(taskProgressedWorkflowEvent.getProgress());

            taskExecutionService.update(taskExecution);
        }
    }
}
