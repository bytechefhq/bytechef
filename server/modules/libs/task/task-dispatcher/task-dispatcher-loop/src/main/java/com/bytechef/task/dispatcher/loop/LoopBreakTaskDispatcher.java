/*
 * Copyright 2021 <your company/name>.
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

import com.bytechef.atlas.Constants;
import com.bytechef.atlas.message.broker.MessageBroker;
import com.bytechef.atlas.message.broker.Queues;
import com.bytechef.atlas.service.task.execution.TaskExecutionService;
import com.bytechef.atlas.task.Task;
import com.bytechef.atlas.task.dispatcher.TaskDispatcher;
import com.bytechef.atlas.task.dispatcher.TaskDispatcherResolver;
import com.bytechef.atlas.task.execution.domain.SimpleTaskExecution;
import com.bytechef.atlas.task.execution.domain.TaskExecution;
import java.util.Date;
import org.springframework.util.Assert;

/**
 * @author Ivica Cardic
 */
public class LoopBreakTaskDispatcher implements TaskDispatcher<TaskExecution>, TaskDispatcherResolver {

    private final MessageBroker messageBroker;
    private final TaskExecutionService taskExecutionService;

    public LoopBreakTaskDispatcher(MessageBroker messageBroker, TaskExecutionService taskExecutionService) {
        this.messageBroker = messageBroker;
        this.taskExecutionService = taskExecutionService;
    }

    @Override
    public void dispatch(TaskExecution taskExecution) {
        SimpleTaskExecution loopTaskExecution = findLoopTaskExecution(taskExecution.getParentId());

        loopTaskExecution.setEndTime(new Date());
        loopTaskExecution.setExecutionTime(loopTaskExecution.getEndTime().getTime()
                - loopTaskExecution.getStartTime().getTime());

        messageBroker.send(Queues.COMPLETIONS, loopTaskExecution);
    }

    private SimpleTaskExecution findLoopTaskExecution(String taskExecutionId) {
        Assert.notNull(taskExecutionId, "Cannot be null");

        SimpleTaskExecution taskExecution =
                SimpleTaskExecution.of(taskExecutionService.getTaskExecution(taskExecutionId));

        if (Constants.LOOP.equals(taskExecution.getType())) {
            return taskExecution;
        } else {
            return findLoopTaskExecution(taskExecution.getParentId());
        }
    }

    @Override
    public TaskDispatcher resolve(Task task) {
        if (task.getType().equals(Constants.LOOP_BREAK)) {
            return this;
        }

        return null;
    }
}
