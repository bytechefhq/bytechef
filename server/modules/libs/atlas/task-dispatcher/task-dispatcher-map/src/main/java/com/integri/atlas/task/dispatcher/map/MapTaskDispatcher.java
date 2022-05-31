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

package com.integri.atlas.task.dispatcher.map;

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
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.springframework.util.Assert;

/**
 * @author Arik Cohen
 * @since Jun 4, 2017
 */
public class MapTaskDispatcher implements TaskDispatcher<TaskExecution>, TaskDispatcherResolver {

    private final TaskDispatcher<TaskExecution> taskDispatcher;
    private final TaskEvaluator taskEvaluator;
    private final TaskExecutionService taskExecutionService;
    private final MessageBroker messageBroker;
    private final ContextService contextService;
    private final CounterService counterService;

    private MapTaskDispatcher(Builder aBuilder) {
        taskDispatcher = aBuilder.taskDispatcher;
        taskExecutionService = aBuilder.taskExecutionService;
        messageBroker = aBuilder.messageBroker;
        contextService = aBuilder.contextService;
        counterService = aBuilder.counterService;
        taskEvaluator = aBuilder.taskEvaluator;
    }

    @Override
    public void dispatch(TaskExecution aTask) {
        List<Object> list = aTask.getList("list", Object.class);
        Assert.notNull(list, "'list' property can't be null");
        Map<String, Object> iteratee = aTask.getMap("iteratee");
        Assert.notNull(iteratee, "'iteratee' property can't be null");

        SimpleTaskExecution parentMapTask = SimpleTaskExecution.of(aTask);
        parentMapTask.setStartTime(new Date());
        parentMapTask.setStatus(TaskStatus.STARTED);
        taskExecutionService.merge(parentMapTask);

        if (list.size() > 0) {
            counterService.set(aTask.getId(), list.size());
            for (int i = 0; i < list.size(); i++) {
                Object item = list.get(i);
                SimpleTaskExecution mapTask = SimpleTaskExecution.of(iteratee);
                mapTask.setId(UUIDGenerator.generate());
                mapTask.setParentId(aTask.getId());
                mapTask.setStatus(TaskStatus.CREATED);
                mapTask.setJobId(aTask.getJobId());
                mapTask.setCreateTime(new Date());
                mapTask.setPriority(aTask.getPriority());
                mapTask.setTaskNumber(i + 1);
                MapContext context = new MapContext(contextService.peek(aTask.getId()));
                context.set(aTask.getString("itemVar", "item"), item);
                context.set(aTask.getString("itemIndex", "itemIndex"), i);
                contextService.push(mapTask.getId(), context);
                TaskExecution evaluatedEachTask = taskEvaluator.evaluate(mapTask, context);
                taskExecutionService.create(evaluatedEachTask);
                taskDispatcher.dispatch(evaluatedEachTask);
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
    public TaskDispatcher<?> resolve(Task aTask) {
        if (aTask.getType().equals(Constants.MAP)) {
            return this;
        }
        return null;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private TaskDispatcher<TaskExecution> taskDispatcher;
        private TaskEvaluator taskEvaluator;
        private TaskExecutionService taskExecutionService;
        private MessageBroker messageBroker;
        private ContextService contextService;
        private CounterService counterService;

        public Builder taskDispatcher(TaskDispatcher<TaskExecution> aTaskDispatcher) {
            taskDispatcher = aTaskDispatcher;
            return this;
        }

        public Builder taskEvaluator(TaskEvaluator aTaskEvaluator) {
            taskEvaluator = aTaskEvaluator;
            return this;
        }

        public Builder taskExecutionService(TaskExecutionService taskExecutionService) {
            this.taskExecutionService = taskExecutionService;
            return this;
        }

        public Builder messageBroker(MessageBroker aMessageBroker) {
            messageBroker = aMessageBroker;
            return this;
        }

        public Builder contextService(ContextService contextService) {
            this.contextService = contextService;
            return this;
        }

        public Builder counterService(CounterService counterService) {
            this.counterService = counterService;
            return this;
        }

        public MapTaskDispatcher build() {
            return new MapTaskDispatcher(this);
        }
    }
}
