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
import static com.bytechef.task.dispatcher.loop.constants.LoopTaskConstants.ITEM;
import static com.bytechef.task.dispatcher.loop.constants.LoopTaskConstants.ITEM_INDEX;
import static com.bytechef.task.dispatcher.loop.constants.LoopTaskConstants.ITEM_VAR;
import static com.bytechef.task.dispatcher.loop.constants.LoopTaskConstants.ITERATEE;
import static com.bytechef.task.dispatcher.loop.constants.LoopTaskConstants.LIST;
import static com.bytechef.task.dispatcher.loop.constants.LoopTaskConstants.LOOP;
import static com.bytechef.task.dispatcher.loop.constants.LoopTaskConstants.LOOP_FOREVER;

import com.bytechef.atlas.domain.Context;
import com.bytechef.atlas.domain.TaskExecution;
import com.bytechef.atlas.message.broker.MessageBroker;
import com.bytechef.atlas.message.broker.Queues;
import com.bytechef.atlas.service.ContextService;
import com.bytechef.atlas.service.TaskExecutionService;
import com.bytechef.atlas.task.Task;
import com.bytechef.atlas.task.WorkflowTask;
import com.bytechef.atlas.task.dispatcher.TaskDispatcher;
import com.bytechef.atlas.task.dispatcher.TaskDispatcherResolver;
import com.bytechef.atlas.task.evaluator.TaskEvaluator;
import com.bytechef.atlas.task.execution.TaskStatus;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import org.springframework.util.Assert;

/**
 * A {@link TaskDispatcher} implementation which implements a loop construct. The dispatcher works
 * by executing the <code>iteratee</code> function on each item on the <code>stream</code>.
 *
 * @author Ivica Cardic
 */
public class LoopTaskDispatcher implements TaskDispatcher<TaskExecution>, TaskDispatcherResolver {

    private final ContextService contextService;
    private final MessageBroker messageBroker;
    private final TaskDispatcher taskDispatcher;
    private final TaskEvaluator taskEvaluator;
    private final TaskExecutionService taskExecutionService;

    public LoopTaskDispatcher(
            ContextService contextService,
            MessageBroker messageBroker,
            TaskDispatcher taskDispatcher,
            TaskEvaluator taskEvaluator,
            TaskExecutionService taskExecutionService) {
        this.contextService = contextService;
        this.taskDispatcher = taskDispatcher;
        this.messageBroker = messageBroker;
        this.taskEvaluator = taskEvaluator;
        this.taskExecutionService = taskExecutionService;
    }

    @Override
    public void dispatch(TaskExecution taskExecution) {
        boolean loopForever = taskExecution.getBoolean(LOOP_FOREVER, false);
        WorkflowTask iteratee = taskExecution.getWorkflowTask(ITERATEE);
        List<Object> list = taskExecution.getList(LIST, Object.class, Collections.emptyList());

        Assert.notNull(iteratee, "'iteratee' property can't be null");

        TaskExecution loopTaskExecution = new TaskExecution(taskExecution);

        loopTaskExecution.setStartTime(LocalDateTime.now());
        loopTaskExecution.setStatus(TaskStatus.STARTED);

        taskExecutionService.update(loopTaskExecution);

        if (loopForever || !list.isEmpty()) {
            TaskExecution subTaskExecution = TaskExecution.of(
                    iteratee, taskExecution.getJobId(), taskExecution.getId(), taskExecution.getPriority(), 1);

            Context context = new Context(contextService.peek(taskExecution.getId()));

            if (!list.isEmpty()) {
                context.put(taskExecution.getString(ITEM_VAR, ITEM), list.get(0));
            }

            context.put(taskExecution.getString(ITEM_INDEX, ITEM_INDEX), 0);

            contextService.push(subTaskExecution.getId(), context);

            TaskExecution evaluatedTaskExecution = taskEvaluator.evaluate(subTaskExecution, context);

            evaluatedTaskExecution = taskExecutionService.add(evaluatedTaskExecution);

            taskDispatcher.dispatch(evaluatedTaskExecution);
        } else {
            TaskExecution completionTaskExecution = new TaskExecution(taskExecution);

            completionTaskExecution.setStartTime(LocalDateTime.now());
            completionTaskExecution.setEndTime(LocalDateTime.now());
            completionTaskExecution.setExecutionTime(0);

            messageBroker.send(Queues.COMPLETIONS, completionTaskExecution);
        }
    }

    @Override
    public TaskDispatcher resolve(Task task) {
        if (task.getType().equals(LOOP + "/v" + VERSION_1)) {
            return this;
        }

        return null;
    }
}
