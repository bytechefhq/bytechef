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

package com.bytechef.task.dispatcher.each.completion;

import static com.bytechef.task.dispatcher.each.constant.EachTaskDispatcherConstants.EACH;

import com.bytechef.atlas.coordinator.task.completion.TaskCompletionHandler;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.service.CounterService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import org.apache.commons.lang3.Validate;

/**
 * @author Arik Cohen
 * @since Apr 24, 2017
 */
public class EachTaskCompletionHandler implements TaskCompletionHandler {

    private final TaskExecutionService taskExecutionService;
    private final TaskCompletionHandler taskCompletionHandler;
    private final CounterService counterService;

    @SuppressFBWarnings("EI")
    public EachTaskCompletionHandler(
        CounterService counterService, TaskCompletionHandler taskCompletionHandler,
        TaskExecutionService taskExecutionService) {

        this.taskExecutionService = taskExecutionService;
        this.taskCompletionHandler = taskCompletionHandler;
        this.counterService = counterService;
    }

    @Override
    public boolean canHandle(TaskExecution taskExecution) {
        Long parentId = taskExecution.getParentId();

        if (parentId != null) {
            TaskExecution parentExecution = taskExecutionService.getTaskExecution(parentId);

            String type = parentExecution.getType();

            return type.equals(EACH + "/v1");
        }

        return false;
    }

    @Override
    public void handle(TaskExecution taskExecution) {
        taskExecution.setStatus(TaskExecution.Status.COMPLETED);

        taskExecution = taskExecutionService.update(taskExecution);

        long subTasksLeft = counterService.decrement(Validate.notNull(taskExecution.getParentId(), "parentId"));

        if (subTasksLeft == 0) {
            TaskExecution eachTaskExecution = taskExecutionService.getTaskExecution(taskExecution.getParentId());

            eachTaskExecution.setEndDate(Instant.now());

            eachTaskExecution = taskExecutionService.update(eachTaskExecution);

            taskCompletionHandler.handle(eachTaskExecution);
            counterService.delete(taskExecution.getParentId());
        }
    }
}
