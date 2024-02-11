/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.task.dispatcher.loop;

import static com.bytechef.task.dispatcher.loop.constant.LoopTaskDispatcherConstants.LOOP;
import static com.bytechef.task.dispatcher.loop.constant.LoopTaskDispatcherConstants.LOOP_BREAK;

import com.bytechef.atlas.configuration.domain.Task;
import com.bytechef.atlas.coordinator.event.TaskExecutionCompleteEvent;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcher;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcherResolver;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.LocalDateTime;
import java.util.Objects;
import org.apache.commons.lang3.Validate;
import org.springframework.context.ApplicationEventPublisher;

/**
 * @author Ivica Cardic
 */
public class LoopBreakTaskDispatcher implements TaskDispatcher<TaskExecution>, TaskDispatcherResolver {

    private final ApplicationEventPublisher eventPublisher;
    private final TaskExecutionService taskExecutionService;

    @SuppressFBWarnings("EI2")
    public LoopBreakTaskDispatcher(
        ApplicationEventPublisher eventPublisher, TaskExecutionService taskExecutionService) {

        this.eventPublisher = eventPublisher;
        this.taskExecutionService = taskExecutionService;
    }

    @Override
    public void dispatch(TaskExecution taskExecution) {
        TaskExecution loopTaskExecution = findLoopTaskExecution(taskExecution.getParentId());

        loopTaskExecution.setEndDate(LocalDateTime.now());

        eventPublisher.publishEvent(new TaskExecutionCompleteEvent(loopTaskExecution));
    }

    private TaskExecution findLoopTaskExecution(Long taskExecutionId) {
        Validate.notNull(taskExecutionId, "'taskExecutionId' must not be null");

        TaskExecution taskExecution = taskExecutionService.getTaskExecution(taskExecutionId);

        String type = taskExecution.getType();

        if (type.equals(LOOP + "/v1")) {
            return taskExecution;
        } else {
            if (taskExecution.getParentId() == null) {
                throw new IllegalStateException("Loop must be specified");
            }

            return findLoopTaskExecution(taskExecution.getParentId());
        }
    }

    @Override
    public TaskDispatcher<? extends Task> resolve(Task task) {
        if (Objects.equals(task.getType(), LOOP_BREAK + "/v1")) {
            return this;
        }

        return null;
    }
}
