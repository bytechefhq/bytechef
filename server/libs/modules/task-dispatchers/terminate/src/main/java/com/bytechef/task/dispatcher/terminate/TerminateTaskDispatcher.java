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

package com.bytechef.task.dispatcher.terminate;

import static com.bytechef.task.dispatcher.terminate.constants.TerminateTaskDispatcherConstants.TERMINATE;

import com.bytechef.atlas.configuration.domain.Task;
import com.bytechef.atlas.coordinator.event.StopJobEvent;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcher;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcherResolver;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.Objects;
import org.springframework.context.ApplicationEventPublisher;

/**
 * @author Matija Petanjek
 */
public class TerminateTaskDispatcher implements TaskDispatcher<TaskExecution>, TaskDispatcherResolver {

    private final ApplicationEventPublisher eventPublisher;
    private final TaskExecutionService taskExecutionService;

    @SuppressFBWarnings("EI")
    public TerminateTaskDispatcher(ApplicationEventPublisher eventPublisher,
        TaskExecutionService taskExecutionService) {
        this.eventPublisher = eventPublisher;
        this.taskExecutionService = taskExecutionService;
    }

    @Override
    public void dispatch(TaskExecution taskExecution) {
        updateParentTaskExecutions(taskExecution);

        taskExecution.setStatus(TaskExecution.Status.COMPLETED);

        taskExecutionService.update(taskExecution);

        eventPublisher.publishEvent(new StopJobEvent(taskExecution.getJobId()));
    }

    @Override
    public TaskDispatcher<? extends Task> resolve(Task task) {
        if (Objects.equals(task.getType(), TERMINATE + "/v1")) {
            return this;
        }

        return null;
    }

    private void updateParentTaskExecutions(TaskExecution taskExecution) {
        while (taskExecution.getParentId() != null) {
            taskExecution = taskExecutionService.getTaskExecution(taskExecution.getParentId());

            taskExecution.setEndDate(Instant.now());
            taskExecution.setStatus(TaskExecution.Status.CANCELLED);

            taskExecution = taskExecutionService.update(taskExecution);
        }
    }
}
