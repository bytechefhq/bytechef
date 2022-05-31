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

package com.integri.atlas.task.dispatcher.parallel;

import com.integri.atlas.context.service.ContextService;
import com.integri.atlas.engine.Constants;
import com.integri.atlas.engine.MapObject;
import com.integri.atlas.engine.context.MapContext;
import com.integri.atlas.engine.counter.service.CounterService;
import com.integri.atlas.engine.message.broker.MessageBroker;
import com.integri.atlas.engine.message.broker.Queues;
import com.integri.atlas.engine.task.Task;
import com.integri.atlas.engine.task.dispatcher.TaskDispatcher;
import com.integri.atlas.engine.task.dispatcher.TaskDispatcherResolver;
import com.integri.atlas.engine.task.execution.SimpleTaskExecution;
import com.integri.atlas.engine.task.execution.TaskExecution;
import com.integri.atlas.engine.task.execution.TaskStatus;
import com.integri.atlas.engine.task.execution.servic.TaskExecutionService;
import com.integri.atlas.engine.uuid.UUIDGenerator;
import com.integri.atlas.task.dispatcher.parallel.completion.ParallelTaskCompletionHandler;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.springframework.util.Assert;

/**
 * A {@link TaskDispatcher} implementation which implements the parallel
 * construct. Providing a list of <code>tasks</code> the dispatcher will
 * execute these in parallel. As each task is complete it will be caught
 * by the {@link ParallelTaskCompletionHandler}.
 *
 * @author Arik Cohen
 * @since May 12, 2017
 */
public class ParallelTaskDispatcher implements TaskDispatcher<TaskExecution>, TaskDispatcherResolver {

    private TaskDispatcher taskDispatcher;
    private TaskExecutionService taskExecutionService;
    private MessageBroker messageBroker;
    private ContextService contextService;
    private CounterService counterService;

    @Override
    public void dispatch(TaskExecution aTask) {
        List<MapObject> tasks = aTask.getList("tasks", MapObject.class);
        Assert.notNull(tasks, "'tasks' property can't be null");
        if (tasks.size() > 0) {
            counterService.set(aTask.getId(), tasks.size());
            for (Map<String, Object> task : tasks) {
                SimpleTaskExecution parallelTask = SimpleTaskExecution.of(task);
                parallelTask.setId(UUIDGenerator.generate());
                parallelTask.setParentId(aTask.getId());
                parallelTask.setStatus(TaskStatus.CREATED);
                parallelTask.setJobId(aTask.getJobId());
                parallelTask.setCreateTime(new Date());
                parallelTask.setPriority(aTask.getPriority());
                MapContext context = new MapContext(contextService.peek(aTask.getId()));
                contextService.push(parallelTask.getId(), context);
                taskExecutionService.create(parallelTask);
                taskDispatcher.dispatch(parallelTask);
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
        if (aTask.getType().equals(Constants.PARALLEL)) {
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

    public void setTaskExecutionService(TaskExecutionService taskExecutionService) {
        this.taskExecutionService = taskExecutionService;
    }
}
