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
 * Modifications copyright (C) 2023 ByteChef Inc.
 */

package com.bytechef.task.dispatcher.map.completion;

import static com.bytechef.task.dispatcher.map.constant.MapTaskDispatcherConstants.MAP;

import com.bytechef.atlas.coordinator.task.completion.TaskCompletionHandler;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.service.CounterService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.Validate;

/**
 * @author Arik Cohen
 * @since June 4, 2017
 */
public class MapTaskCompletionHandler implements TaskCompletionHandler {

    private final TaskExecutionService taskExecutionService;
    private final TaskCompletionHandler taskCompletionHandler;
    private final CounterService counterService;
    private final TaskFileStorage taskFileStorage;

    @SuppressFBWarnings("EI")
    public MapTaskCompletionHandler(
        TaskExecutionService taskExecutionService, TaskCompletionHandler taskCompletionHandler,
        CounterService counterService, TaskFileStorage taskFileStorage) {

        this.taskExecutionService = taskExecutionService;
        this.taskCompletionHandler = taskCompletionHandler;
        this.counterService = counterService;
        this.taskFileStorage = taskFileStorage;
    }

    @Override
    public boolean canHandle(TaskExecution taskExecution) {
        Long parentId = taskExecution.getParentId();

        if (parentId != null) {
            TaskExecution parentExecution = taskExecutionService.getTaskExecution(parentId);

            String type = parentExecution.getType();

            return type.equals(MAP + "/v1");
        }
        return false;
    }

    @Override
    public void handle(TaskExecution taskExecution) {
        taskExecution.setStatus(TaskExecution.Status.COMPLETED);

        taskExecution = taskExecutionService.update(taskExecution);

        long subtasksLeft = counterService.decrement(Validate.notNull(taskExecution.getParentId(), "parentId"));

        if (subtasksLeft == 0) {
            List<TaskExecution> childTaskExecutions = taskExecutionService
                .getParentTaskExecutions(taskExecution.getParentId());
            TaskExecution mapTaskExecution = taskExecutionService.getTaskExecution(taskExecution.getParentId());

            mapTaskExecution.setEndDate(Instant.now());

            mapTaskExecution.setOutput(
                taskFileStorage.storeTaskExecutionOutput(
                    Validate.notNull(mapTaskExecution.getId(), "id"),
                    childTaskExecutions.stream()
                        .map(output -> taskFileStorage.readTaskExecutionOutput(output.getOutput()))
                        .collect(Collectors.toList())));

            taskCompletionHandler.handle(mapTaskExecution);
            counterService.delete(taskExecution.getParentId());
        }
    }
}
