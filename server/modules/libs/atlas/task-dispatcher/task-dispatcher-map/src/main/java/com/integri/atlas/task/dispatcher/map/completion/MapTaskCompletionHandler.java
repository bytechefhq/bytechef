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

package com.integri.atlas.task.dispatcher.map.completion;

import com.integri.atlas.engine.Constants;
import com.integri.atlas.engine.coordinator.task.completion.TaskCompletionHandler;
import com.integri.atlas.engine.counter.service.CounterService;
import com.integri.atlas.engine.task.execution.SimpleTaskExecution;
import com.integri.atlas.engine.task.execution.TaskExecution;
import com.integri.atlas.engine.task.execution.TaskStatus;
import com.integri.atlas.engine.task.execution.servic.TaskExecutionService;
import java.util.Date;
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
        CounterService counterService
    ) {
        this.taskExecutionService = taskExecutionService;
        this.taskCompletionHandler = taskCompletionHandler;
        this.counterService = counterService;
    }

    @Override
    public void handle(TaskExecution aTaskExecution) {
        SimpleTaskExecution mtask = SimpleTaskExecution.of(aTaskExecution);
        mtask.setStatus(TaskStatus.COMPLETED);
        taskExecutionService.merge(mtask);
        long subtasksLeft = counterService.decrement(aTaskExecution.getParentId());
        if (subtasksLeft == 0) {
            List<TaskExecution> children = taskExecutionService.getParentTaskExecutions(aTaskExecution.getParentId());
            SimpleTaskExecution parentExecution = SimpleTaskExecution.of(
                taskExecutionService.getTaskExecution(aTaskExecution.getParentId())
            );
            parentExecution.setEndTime(new Date());
            parentExecution.setExecutionTime(
                parentExecution.getEndTime().getTime() - parentExecution.getStartTime().getTime()
            );
            parentExecution.setOutput(children.stream().map(c -> c.getOutput()).collect(Collectors.toList()));
            taskCompletionHandler.handle(parentExecution);
            counterService.delete(aTaskExecution.getParentId());
        }
    }

    @Override
    public boolean canHandle(TaskExecution aTaskExecution) {
        String parentId = aTaskExecution.getParentId();
        if (parentId != null) {
            TaskExecution parentExecution = taskExecutionService.getTaskExecution(parentId);
            return parentExecution.getType().equals(Constants.MAP);
        }
        return false;
    }
}
