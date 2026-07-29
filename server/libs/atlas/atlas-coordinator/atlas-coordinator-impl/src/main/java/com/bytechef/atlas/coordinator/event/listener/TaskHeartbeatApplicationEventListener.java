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

package com.bytechef.atlas.coordinator.event.listener;

import com.bytechef.atlas.coordinator.event.ApplicationEvent;
import com.bytechef.atlas.coordinator.event.TaskHeartbeatApplicationEvent;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Ivica Cardic
 */
public class TaskHeartbeatApplicationEventListener implements ApplicationEventListener {

    private static final Logger log = LoggerFactory.getLogger(TaskHeartbeatApplicationEventListener.class);

    private final TaskExecutionService taskExecutionService;

    @SuppressFBWarnings("EI2")
    public TaskHeartbeatApplicationEventListener(TaskExecutionService taskExecutionService) {
        this.taskExecutionService = taskExecutionService;
    }

    @Override
    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        if (applicationEvent instanceof TaskHeartbeatApplicationEvent taskHeartbeatApplicationEvent) {
            long taskExecutionId = taskHeartbeatApplicationEvent.getTaskExecutionId();

            try {
                TaskExecution taskExecution = taskExecutionService.getTaskExecution(taskExecutionId);

                if (taskExecution.getStatus() == TaskExecution.Status.STARTED) {
                    taskExecutionService.update(taskExecution);
                }
            } catch (Exception exception) {
                if (log.isDebugEnabled()) {
                    log.debug(
                        "Failed to record heartbeat for task execution {}: {}", taskExecutionId,
                        exception.getMessage());
                }
            }
        }
    }
}
