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

package com.integri.atlas.task.dispatcher.each.completion;

import com.integri.atlas.engine.coordinator.task.completion.TaskCompletionHandler;
import com.integri.atlas.engine.core.DSL;
import com.integri.atlas.engine.core.counter.repository.CounterRepository;
import com.integri.atlas.engine.core.task.SimpleTaskExecution;
import com.integri.atlas.engine.core.task.TaskExecution;
import com.integri.atlas.engine.core.task.TaskStatus;
import com.integri.atlas.engine.core.task.repository.TaskExecutionRepository;
import java.util.Date;

/**
 *
 * @author Arik Cohen
 * @since Apr 24, 2017
 */
public class EachTaskCompletionHandler implements TaskCompletionHandler {

    private final TaskExecutionRepository taskExecutionRepository;
    private final TaskCompletionHandler taskCompletionHandler;
    private final CounterRepository counterRepository;

    public EachTaskCompletionHandler(
        TaskExecutionRepository taskExecutionRepository,
        TaskCompletionHandler taskCompletionHandler,
        CounterRepository counterRepository
    ) {
        this.taskExecutionRepository = taskExecutionRepository;
        this.taskCompletionHandler = taskCompletionHandler;
        this.counterRepository = counterRepository;
    }

    @Override
    public boolean canHandle(TaskExecution taskExecution) {
        String parentId = taskExecution.getParentId();

        if (parentId != null) {
            TaskExecution parentExecution = taskExecutionRepository.findOne(parentId);

            return parentExecution.getType().equals(DSL.EACH);
        }

        return false;
    }

    @Override
    public void handle(TaskExecution taskExecution) {
        SimpleTaskExecution completedSubtaskExecution = SimpleTaskExecution.of(taskExecution);

        completedSubtaskExecution.setStatus(TaskStatus.COMPLETED);

        taskExecutionRepository.merge(completedSubtaskExecution);

        long subtasksLeft = counterRepository.decrement(taskExecution.getParentId());

        if (subtasksLeft == 0) {
            SimpleTaskExecution eachTaskExecution = SimpleTaskExecution.of(
                taskExecutionRepository.findOne(taskExecution.getParentId())
            );

            eachTaskExecution.setEndTime(new Date());
            eachTaskExecution.setExecutionTime(
                eachTaskExecution.getEndTime().getTime() - eachTaskExecution.getStartTime().getTime()
            );

            taskCompletionHandler.handle(eachTaskExecution);
            counterRepository.delete(taskExecution.getParentId());
        }
    }
}
