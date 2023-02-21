
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

import static com.bytechef.hermes.task.dispatcher.constant.TaskDispatcherConstants.Versions.VERSION_1;
import static com.bytechef.task.dispatcher.parallel.constants.ParallelTaskDispatcherConstants.PARALLEL;

import com.bytechef.atlas.coordinator.task.completion.TaskCompletionHandler;
import com.bytechef.atlas.domain.TaskExecution;
import com.bytechef.atlas.service.CounterService;
import com.bytechef.atlas.service.TaskExecutionService;
import com.bytechef.atlas.task.execution.TaskStatus;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * A {@link TaskCompletionHandler} implementation which handles completions of parallel construct tasks.
 *
 * <p>
 * This handler keeps track of how many tasks were completed so far and when all parallel tasks completed for a given
 * task it will then complete the overall <code>parallel</code> task.
 *
 * @author Arik Cohen
 * @since May 12, 2017
 * @see com.bytechef.task.dispatcher.parallel.ParallelTaskDispatcher
 */
public class ParallelTaskCompletionHandler implements TaskCompletionHandler {

    private final CounterService counterService;
    private final TaskCompletionHandler taskCompletionHandler;
    private final TaskExecutionService taskExecutionService;

    public ParallelTaskCompletionHandler(
        CounterService counterService,
        TaskCompletionHandler taskCompletionHandler,
        TaskExecutionService taskExecutionService) {
        this.counterService = counterService;
        this.taskCompletionHandler = taskCompletionHandler;
        this.taskExecutionService = taskExecutionService;
    }

    @Override
    public boolean canHandle(TaskExecution taskExecution) {
        Long parentId = taskExecution.getParentId();

        if (parentId == null) {
            return false;
        } else {
            TaskExecution parentTaskExecution = taskExecutionService.getTaskExecution(parentId);

            String type = parentTaskExecution.getType();

            return type.equals(PARALLEL + "/v" + VERSION_1);
        }
    }

    @Override
    @SuppressFBWarnings("NP")
    public void handle(TaskExecution taskExecution) {
        taskExecution.setStatus(TaskStatus.COMPLETED);

        taskExecution = taskExecutionService.update(taskExecution);

        long tasksLeft = counterService.decrement(taskExecution.getParentId());

        if (tasksLeft == 0) {
            taskCompletionHandler.handle(taskExecutionService.getTaskExecution(taskExecution.getParentId()));
            counterService.delete(taskExecution.getParentId());
        }
    }
}
