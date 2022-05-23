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

package com.integri.atlas.task.dispatcher.each;

import com.integri.atlas.engine.Constants;
import com.integri.atlas.engine.context.MapContext;
import com.integri.atlas.engine.context.repository.ContextRepository;
import com.integri.atlas.engine.counter.repository.CounterRepository;
import com.integri.atlas.engine.message.broker.MessageBroker;
import com.integri.atlas.engine.message.broker.Queues;
import com.integri.atlas.engine.task.Task;
import com.integri.atlas.engine.task.dispatcher.TaskDispatcher;
import com.integri.atlas.engine.task.dispatcher.TaskDispatcherResolver;
import com.integri.atlas.engine.task.execution.SimpleTaskExecution;
import com.integri.atlas.engine.task.execution.TaskExecution;
import com.integri.atlas.engine.task.execution.TaskStatus;
import com.integri.atlas.engine.task.execution.evaluator.TaskEvaluator;
import com.integri.atlas.engine.task.execution.repository.TaskExecutionRepository;
import com.integri.atlas.engine.uuid.UUIDGenerator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.springframework.util.Assert;

/**
 * A {@link TaskDispatcher} implementation which implements a parallel
 * for-each construct. The dispatcher works by executing
 * the <code>iteratee</code> function on each item on the <code>list</code>.
 *
 * @author Arik Cohen
 * @since Apr 25, 2017
 */
public class EachTaskDispatcher implements TaskDispatcher<TaskExecution>, TaskDispatcherResolver {

    private final TaskDispatcher taskDispatcher;
    private final TaskEvaluator taskEvaluator;
    private final TaskExecutionRepository taskExecutionRepository;
    private final MessageBroker messageBroker;
    private final ContextRepository contextRepository;
    private final CounterRepository counterRepository;

    public EachTaskDispatcher(
        TaskDispatcher taskDispatcher,
        TaskExecutionRepository taskExecutionRepository,
        MessageBroker messageBroker,
        ContextRepository contextRepository,
        CounterRepository counterRepository,
        TaskEvaluator taskEvaluator
    ) {
        this.taskDispatcher = taskDispatcher;
        this.taskExecutionRepository = taskExecutionRepository;
        this.messageBroker = messageBroker;
        this.contextRepository = contextRepository;
        this.counterRepository = counterRepository;
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

        taskExecutionRepository.merge(eachTaskExecution);

        if (list.size() > 0) {
            counterRepository.set(taskExecution.getId(), list.size());

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

                MapContext context = new MapContext(contextRepository.peek(taskExecution.getId()));

                context.set(taskExecution.getString("itemVar", "item"), item);
                context.set(taskExecution.getString("itemIndex", "itemIndex"), i);

                contextRepository.push(subTaskExecution.getId(), context);

                TaskExecution evaluatedSubtaskExecution = taskEvaluator.evaluate(subTaskExecution, context);

                taskExecutionRepository.create(evaluatedSubtaskExecution);
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
