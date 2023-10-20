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

package com.bytechef.task.dispatcher.each;

import com.bytechef.atlas.Constants;
import com.bytechef.atlas.context.domain.MapContext;
import com.bytechef.atlas.message.broker.MessageBroker;
import com.bytechef.atlas.message.broker.Queues;
import com.bytechef.atlas.service.context.ContextService;
import com.bytechef.atlas.service.counter.CounterService;
import com.bytechef.atlas.service.task.execution.TaskExecutionService;
import com.bytechef.atlas.task.Task;
import com.bytechef.atlas.task.dispatcher.TaskDispatcher;
import com.bytechef.atlas.task.dispatcher.TaskDispatcherResolver;
import com.bytechef.atlas.task.execution.TaskStatus;
import com.bytechef.atlas.task.execution.domain.SimpleTaskExecution;
import com.bytechef.atlas.task.execution.domain.TaskExecution;
import com.bytechef.atlas.task.execution.evaluator.TaskEvaluator;
import com.bytechef.atlas.uuid.UUIDGenerator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.springframework.util.Assert;

/**
 * A {@link TaskDispatcher} implementation which implements a parallel for-each construct. The
 * dispatcher works by executing the <code>iteratee</code> function on each item on the <code>list
 * </code>.
 *
 * @author Arik Cohen
 * @since Apr 25, 2017
 */
public class EachTaskDispatcher implements TaskDispatcher<TaskExecution>, TaskDispatcherResolver {

    private final TaskDispatcher taskDispatcher;
    private final TaskEvaluator taskEvaluator;
    private final TaskExecutionService taskExecutionService;
    private final MessageBroker messageBroker;
    private final ContextService contextService;
    private final CounterService counterService;

    public EachTaskDispatcher(
            TaskDispatcher taskDispatcher,
            TaskExecutionService taskExecutionService,
            MessageBroker messageBroker,
            ContextService contextService,
            CounterService counterService,
            TaskEvaluator taskEvaluator) {
        this.taskDispatcher = taskDispatcher;
        this.taskExecutionService = taskExecutionService;
        this.messageBroker = messageBroker;
        this.contextService = contextService;
        this.counterService = counterService;
        this.taskEvaluator = taskEvaluator;
    }

    @Override
    public void dispatch(TaskExecution taskExecution) {
        Map<String, Object> iteratee = taskExecution.getMap("iteratee");
        List<Object> list = taskExecution.getList("list", Object.class);

        Assert.notNull(iteratee, "'iteratee' property can't be null");
        Assert.notNull(list, "'list' property can't be null");

        SimpleTaskExecution eachTaskExecution = SimpleTaskExecution.of(taskExecution);

        eachTaskExecution.setStartTime(new Date());
        eachTaskExecution.setStatus(TaskStatus.STARTED);

        taskExecutionService.merge(eachTaskExecution);

        if (list.size() > 0) {
            counterService.set(taskExecution.getId(), list.size());

            for (int i = 0; i < list.size(); i++) {
                Object item = list.get(i);
                SimpleTaskExecution subTaskExecution = SimpleTaskExecution.of(iteratee);

                subTaskExecution.setCreateTime(new Date());
                subTaskExecution.setId(UUIDGenerator.generate());
                subTaskExecution.setJobId(taskExecution.getJobId());
                subTaskExecution.setParentId(taskExecution.getId());
                subTaskExecution.setPriority(taskExecution.getPriority());
                subTaskExecution.setStatus(TaskStatus.CREATED);
                subTaskExecution.setTaskNumber(i + 1);

                MapContext context = new MapContext(contextService.peek(taskExecution.getId()));

                context.set(taskExecution.getString("itemVar", "item"), item);
                context.set(taskExecution.getString("itemIndex", "itemIndex"), i);

                contextService.push(subTaskExecution.getId(), context);

                TaskExecution evaluatedSubtaskExecution = taskEvaluator.evaluate(subTaskExecution, context);

                taskExecutionService.create(evaluatedSubtaskExecution);
                taskDispatcher.dispatch(evaluatedSubtaskExecution);
            }
        } else {
            SimpleTaskExecution completionTaskExecution = SimpleTaskExecution.of(taskExecution);

            completionTaskExecution.setStartTime(new Date());
            completionTaskExecution.setEndTime(new Date());
            completionTaskExecution.setExecutionTime(0);

            messageBroker.send(Queues.COMPLETIONS, completionTaskExecution);
        }
    }

    @Override
    public TaskDispatcher resolve(Task aTask) {
        if (aTask.getType().equals(Constants.EACH)) {
            return this;
        }
        return null;
    }
}
