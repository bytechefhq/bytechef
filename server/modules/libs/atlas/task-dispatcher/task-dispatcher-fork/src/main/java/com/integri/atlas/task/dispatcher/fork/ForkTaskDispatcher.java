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

package com.integri.atlas.task.dispatcher.fork;

import com.integri.atlas.engine.Constants;
import com.integri.atlas.engine.context.MapContext;
import com.integri.atlas.engine.context.service.ContextService;
import com.integri.atlas.engine.counter.service.CounterService;
import com.integri.atlas.engine.message.broker.MessageBroker;
import com.integri.atlas.engine.message.broker.Queues;
import com.integri.atlas.engine.task.Task;
import com.integri.atlas.engine.task.dispatcher.TaskDispatcher;
import com.integri.atlas.engine.task.dispatcher.TaskDispatcherResolver;
import com.integri.atlas.engine.task.execution.SimpleTaskExecution;
import com.integri.atlas.engine.task.execution.TaskExecution;
import com.integri.atlas.engine.task.execution.TaskStatus;
import com.integri.atlas.engine.task.execution.evaluator.TaskEvaluator;
import com.integri.atlas.engine.task.execution.service.TaskExecutionService;
import com.integri.atlas.engine.uuid.UUIDGenerator;
import com.integri.atlas.task.dispatcher.fork.completion.ForkTaskCompletionHandler;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.springframework.util.Assert;

/**
 * Implements a Fork/Join construct.
 *
 * Fork/Join tasks are expected to have a "branches" property which contains a list of task list.
 *
 * Each branch executes in isolation, in parallel to the other branches in the fork and has its own context namespace.
 *
 * <pre>
 *   - type: fork
 *     branches:
 *       - - name: randomNumber
 *           label: Generate a random number
 *           type: randomInt
 *           startInclusive: 0
 *           endInclusive: 5000
 *
 *         - type: sleep
 *           millis: ${randomNumber}
 *
 *       - - name: randomNumber
 *           label: Generate a random number
 *           type: randomInt
 *           startInclusive: 0
 *           endInclusive: 5000
 *
 *         - type: sleep
 *           millis: ${randomNumber}
 * </pre>
 *
 * @author Arik Cohen
 * @since May 11, 2017
 * @see ForkTaskCompletionHandler
 */
public class ForkTaskDispatcher implements TaskDispatcher<TaskExecution>, TaskDispatcherResolver {

    private TaskDispatcher taskDispatcher;
    private TaskEvaluator taskEvaluator;
    private TaskExecutionService taskExecutionService;
    private MessageBroker messageBroker;
    private ContextService contextService;
    private CounterService counterService;

    @Override
    public void dispatch(TaskExecution aTask) {
        List<List> branches = aTask.getList("branches", List.class);
        Assert.notNull(branches, "'branches' property can't be null");
        SimpleTaskExecution forkTask = SimpleTaskExecution.of(aTask);
        forkTask.setStartTime(new Date());
        forkTask.setStatus(TaskStatus.STARTED);
        taskExecutionService.merge(forkTask);
        if (branches.size() > 0) {
            counterService.set(aTask.getId(), branches.size());
            for (int i = 0; i < branches.size(); i++) {
                List branch = branches.get(i);
                Assert.isTrue(branch.size() > 0, "branch " + i + " does not contain any tasks");
                Map<String, Object> task = (Map<String, Object>) branch.get(0);
                SimpleTaskExecution execution = SimpleTaskExecution.of(task);
                execution.setId(UUIDGenerator.generate());
                execution.setStatus(TaskStatus.CREATED);
                execution.setCreateTime(new Date());
                execution.set("branch", i);
                execution.setTaskNumber(1);
                execution.setJobId(aTask.getJobId());
                execution.setParentId(aTask.getId());
                execution.setPriority(aTask.getPriority());
                MapContext context = new MapContext(contextService.peek(aTask.getId()));
                contextService.push(aTask.getId() + "/" + i, context);
                contextService.push(execution.getId(), context);
                TaskExecution evaluatedExecution = taskEvaluator.evaluate(execution, context);
                taskExecutionService.create(evaluatedExecution);
                taskDispatcher.dispatch(evaluatedExecution);
            }
        } else {
            SimpleTaskExecution completion = SimpleTaskExecution.of(aTask);
            completion.setStartTime(new Date());
            completion.setEndTime(new Date());
            completion.setExecutionTime(0);
            messageBroker.send(Queues.COMPLETIONS, completion);
        }
    }

    @Override
    public TaskDispatcher resolve(Task aTask) {
        if (aTask.getType().equals(Constants.FORK)) {
            return this;
        }
        return null;
    }

    public void setContextService(ContextService contextService) {
        this.contextService = contextService;
    }

    public void setCounterService(CounterService counterService) {
        this.counterService = counterService;
    }

    public void setMessageBroker(MessageBroker messageBroker) {
        this.messageBroker = messageBroker;
    }

    public void setTaskDispatcher(TaskDispatcher taskDispatcher) {
        this.taskDispatcher = taskDispatcher;
    }

    public void setTaskEvaluator(TaskEvaluator taskEvaluator) {
        this.taskEvaluator = taskEvaluator;
    }

    public void setTaskExecutionService(TaskExecutionService taskExecutionService) {
        this.taskExecutionService = taskExecutionService;
    }
}
