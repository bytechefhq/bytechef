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
import com.bytechef.atlas.context.domain.MapContext;
import com.bytechef.atlas.message.broker.MessageBroker;
import com.bytechef.atlas.message.broker.Queues;
import com.bytechef.atlas.service.context.ContextService;
import com.bytechef.atlas.service.task.execution.TaskExecutionService;
import com.bytechef.atlas.task.Task;
import com.bytechef.atlas.task.dispatcher.TaskDispatcher;
import com.bytechef.atlas.task.dispatcher.TaskDispatcherResolver;
import com.bytechef.atlas.task.execution.TaskStatus;
import com.bytechef.atlas.task.execution.domain.SimpleTaskExecution;
import com.bytechef.atlas.task.execution.domain.TaskExecution;
import com.bytechef.atlas.task.execution.evaluator.TaskEvaluator;
import com.bytechef.atlas.uuid.UUIDGenerator;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
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
        boolean endlessLoop = taskExecution.getBoolean("endlessLoop", false);
        Map<String, Object> iteratee = taskExecution.getMap("iteratee");
        List<Object> list = taskExecution.getList("list", Object.class, Collections.emptyList());

        Assert.notNull(iteratee, "'iteratee' property can't be null");

        SimpleTaskExecution loopTaskExecution = SimpleTaskExecution.of(taskExecution);

        loopTaskExecution.setStartTime(new Date());
        loopTaskExecution.setStatus(TaskStatus.STARTED);

        taskExecutionService.merge(loopTaskExecution);

        if (endlessLoop || !list.isEmpty()) {
            SimpleTaskExecution subTaskExecution = SimpleTaskExecution.of(iteratee);

            subTaskExecution.setCreateTime(new Date());
            subTaskExecution.setId(UUIDGenerator.generate());
            subTaskExecution.setJobId(taskExecution.getJobId());
            subTaskExecution.setParentId(taskExecution.getId());
            subTaskExecution.setPriority(taskExecution.getPriority());
            subTaskExecution.setStatus(TaskStatus.CREATED);
            subTaskExecution.setTaskNumber(1);

            MapContext context = new MapContext(contextService.peek(taskExecution.getId()));

            if (!list.isEmpty()) {
                context.set(taskExecution.getString("itemVar", "item"), list.get(0));
            }

            context.set(taskExecution.getString("itemIndex", "itemIndex"), 0);

            contextService.push(subTaskExecution.getId(), context);

            TaskExecution evaluatedSubTaskExecution = taskEvaluator.evaluate(subTaskExecution, context);

            taskExecutionService.create(evaluatedSubTaskExecution);
            taskDispatcher.dispatch(evaluatedSubTaskExecution);
        } else {
            SimpleTaskExecution completionTaskExecution = SimpleTaskExecution.of(taskExecution);

            completionTaskExecution.setStartTime(new Date());
            completionTaskExecution.setEndTime(new Date());
            completionTaskExecution.setExecutionTime(0);

            messageBroker.send(Queues.COMPLETIONS, completionTaskExecution);
        }
    }

    @Override
    public TaskDispatcher resolve(Task task) {
        if (Constants.LOOP.equals(task.getType())) {
            return this;
        }

        return null;
    }
}
