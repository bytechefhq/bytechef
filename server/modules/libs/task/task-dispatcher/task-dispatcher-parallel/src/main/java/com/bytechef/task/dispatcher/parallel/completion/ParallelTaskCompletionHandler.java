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

package com.bytechef.task.dispatcher.parallel.completion;

import com.bytechef.atlas.Constants;
import com.bytechef.atlas.coordinator.task.completion.TaskCompletionHandler;
import com.bytechef.atlas.counter.service.CounterService;
import com.bytechef.atlas.task.execution.TaskStatus;
import com.bytechef.atlas.task.execution.domain.SimpleTaskExecution;
import com.bytechef.atlas.task.execution.domain.TaskExecution;
import com.bytechef.task.dispatcher.parallel.ParallelTaskDispatcher;
import com.bytechef.task.execution.service.TaskExecutionService;

/**
 * A {@link TaskCompletionHandler} implementation which handles completions of parallel construct
 * tasks.
 *
 * <p>This handler keeps track of how many tasks were completed so far and when all parallel tasks
 * completed for a given task it will then complete the overall <code>parallel</code> task.
 *
 * @author Arik Cohen
 * @since May 12, 2017
 * @see ParallelTaskDispatcher
 */
public class ParallelTaskCompletionHandler implements TaskCompletionHandler {

    private TaskExecutionService taskExecutionService;
    private TaskCompletionHandler taskCompletionHandler;
    private CounterService counterService;

    @Override
    public void handle(TaskExecution aTaskExecution) {
        SimpleTaskExecution mtask = SimpleTaskExecution.of(aTaskExecution);
        mtask.setStatus(TaskStatus.COMPLETED);
        taskExecutionService.merge(mtask);
        long tasksLeft = counterService.decrement(aTaskExecution.getParentId());
        if (tasksLeft == 0) {
            taskCompletionHandler.handle(taskExecutionService.getTaskExecution(aTaskExecution.getParentId()));
            counterService.delete(aTaskExecution.getParentId());
        }
    }

    @Override
    public boolean canHandle(TaskExecution aTaskExecution) {
        String parentId = aTaskExecution.getParentId();
        if (parentId != null) {
            TaskExecution parentExecution = taskExecutionService.getTaskExecution(parentId);
            return parentExecution.getType().equals(Constants.PARALLEL);
        }
        return false;
    }

    public void setTaskExecutionService(TaskExecutionService taskExecutionService) {
        this.taskExecutionService = taskExecutionService;
    }

    public void setTaskCompletionHandler(TaskCompletionHandler taskCompletionHandler) {
        this.taskCompletionHandler = taskCompletionHandler;
    }

    public void setCounterService(CounterService counterService) {
        this.counterService = counterService;
    }
}
