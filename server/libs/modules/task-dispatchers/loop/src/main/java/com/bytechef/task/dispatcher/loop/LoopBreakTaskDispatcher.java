
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

import static com.bytechef.hermes.task.dispatcher.constants.Versions.VERSION_1;
import static com.bytechef.task.dispatcher.loop.constants.LoopTaskConstants.LOOP;
import static com.bytechef.task.dispatcher.loop.constants.LoopTaskConstants.LOOP_BREAK;

import com.bytechef.atlas.domain.TaskExecution;
import com.bytechef.atlas.message.broker.MessageBroker;
import com.bytechef.atlas.message.broker.Queues;
import com.bytechef.atlas.service.TaskExecutionService;
import com.bytechef.atlas.task.Task;
import com.bytechef.atlas.task.dispatcher.TaskDispatcher;
import com.bytechef.atlas.task.dispatcher.TaskDispatcherResolver;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.LocalDateTime;
import org.springframework.util.Assert;

/**
 * @author Ivica Cardic
 */
public class LoopBreakTaskDispatcher implements TaskDispatcher<TaskExecution>, TaskDispatcherResolver {

    private final MessageBroker messageBroker;
    private final TaskExecutionService taskExecutionService;

    @SuppressFBWarnings("EI2")
    public LoopBreakTaskDispatcher(MessageBroker messageBroker, TaskExecutionService taskExecutionService) {
        this.messageBroker = messageBroker;
        this.taskExecutionService = taskExecutionService;
    }

    @Override
    public void dispatch(TaskExecution taskExecution) {
        TaskExecution loopTaskExecution = findLoopTaskExecution(taskExecution.getParentId());

        loopTaskExecution.setEndTime(LocalDateTime.now());

        messageBroker.send(Queues.COMPLETIONS, loopTaskExecution);
    }

    private TaskExecution findLoopTaskExecution(String taskExecutionId) {
        Assert.notNull(taskExecutionId, "Cannot be null");

        TaskExecution taskExecution = new TaskExecution(taskExecutionService.getTaskExecution(taskExecutionId));

        if (taskExecution.getType()
            .equals(LOOP + "/v" + VERSION_1)) {
            return taskExecution;
        } else {
            if (taskExecution.getParentId() == null) {
                throw new IllegalStateException("Loop must be specified.");
            }

            return findLoopTaskExecution(taskExecution.getParentId());
        }
    }

    @Override
    public TaskDispatcher resolve(Task task) {
        if (task.getType()
            .equals(LOOP_BREAK + "/v" + VERSION_1)) {
            return this;
        }

        return null;
    }
}
