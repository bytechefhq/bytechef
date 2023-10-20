
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

package com.bytechef.task.dispatcher.map.completion;

import static com.bytechef.hermes.task.dispatcher.constants.TaskDispatcherConstants.Versions.VERSION_1;
import static com.bytechef.task.dispatcher.map.constants.MapTaskDispatcherConstants.MAP;

import com.bytechef.atlas.coordinator.task.completion.TaskCompletionHandler;
import com.bytechef.atlas.domain.TaskExecution;
import com.bytechef.atlas.service.CounterService;
import com.bytechef.atlas.service.TaskExecutionService;
import com.bytechef.atlas.task.execution.TaskStatus;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Arik Cohen
 * @since June 4, 2017
 */
public class MapTaskCompletionHandler implements TaskCompletionHandler {

    private final TaskExecutionService taskExecutionService;
    private final TaskCompletionHandler taskCompletionHandler;
    private final CounterService counterService;

    public MapTaskCompletionHandler(
        TaskExecutionService taskExecutionService,
        TaskCompletionHandler taskCompletionHandler,
        CounterService counterService) {
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

            return type.equals(MAP + "/v" + VERSION_1);
        }
        return false;
    }

    @Override
    @SuppressFBWarnings("NP")
    public void handle(TaskExecution taskExecution) {
        taskExecutionService.updateStatus(taskExecution.getId(), TaskStatus.COMPLETED, null, null);

        long subtasksLeft = counterService.decrement(taskExecution.getParentId());

        if (subtasksLeft == 0) {
            List<TaskExecution> childTaskExecutions = taskExecutionService
                .getParentTaskExecutions(taskExecution.getParentId());
            TaskExecution mapTaskExecution = taskExecutionService.getTaskExecution(taskExecution.getParentId());

            mapTaskExecution.setEndTime(LocalDateTime.now());

            mapTaskExecution.setOutput(
                childTaskExecutions.stream()
                    .map(TaskExecution::getOutput)
                    .collect(Collectors.toList()));

            taskCompletionHandler.handle(mapTaskExecution);
            counterService.delete(taskExecution.getParentId());
        }
    }
}
