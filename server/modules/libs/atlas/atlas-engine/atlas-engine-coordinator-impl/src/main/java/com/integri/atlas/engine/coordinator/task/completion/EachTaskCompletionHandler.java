/*
 * Copyright 2016-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Modifications copyright (C) 2021 <your company/name>
 */

package com.integri.atlas.engine.coordinator.task.completion;

import com.integri.atlas.engine.coordinator.task.completion.TaskCompletionHandler;
import com.integri.atlas.engine.core.DSL;
import com.integri.atlas.engine.core.task.SimpleTaskExecution;
import com.integri.atlas.engine.core.task.TaskExecution;
import com.integri.atlas.engine.core.task.TaskStatus;
import com.integri.atlas.engine.core.task.repository.CounterRepository;
import com.integri.atlas.engine.core.task.repository.TaskExecutionRepository;
import java.util.Date;

/**
 *
 * @author Arik Cohen
 * @since Apr 24, 2017
 */
public class EachTaskCompletionHandler implements TaskCompletionHandler {

    private final TaskExecutionRepository taskExecutionRepo;
    private final TaskCompletionHandler taskCompletionHandler;
    private final CounterRepository counterRepository;

    public EachTaskCompletionHandler(
        TaskExecutionRepository aTaskExecutionRepo,
        TaskCompletionHandler aTaskCompletionHandler,
        CounterRepository aCounterRepository
    ) {
        taskExecutionRepo = aTaskExecutionRepo;
        taskCompletionHandler = aTaskCompletionHandler;
        counterRepository = aCounterRepository;
    }

    @Override
    public void handle(TaskExecution aTaskExecution) {
        SimpleTaskExecution mtask = SimpleTaskExecution.of(aTaskExecution);
        mtask.setStatus(TaskStatus.COMPLETED);
        taskExecutionRepo.merge(mtask);
        long subtasksLeft = counterRepository.decrement(aTaskExecution.getParentId());
        if (subtasksLeft == 0) {
            SimpleTaskExecution parentExecution = SimpleTaskExecution.of(
                taskExecutionRepo.findOne(aTaskExecution.getParentId())
            );
            parentExecution.setEndTime(new Date());
            parentExecution.setExecutionTime(
                parentExecution.getEndTime().getTime() - parentExecution.getStartTime().getTime()
            );
            taskCompletionHandler.handle(parentExecution);
            counterRepository.delete(aTaskExecution.getParentId());
        }
    }

    @Override
    public boolean canHandle(TaskExecution aTaskExecution) {
        String parentId = aTaskExecution.getParentId();
        if (parentId != null) {
            TaskExecution parentExecution = taskExecutionRepo.findOne(parentId);
            return parentExecution.getType().equals(DSL.EACH);
        }
        return false;
    }
}
