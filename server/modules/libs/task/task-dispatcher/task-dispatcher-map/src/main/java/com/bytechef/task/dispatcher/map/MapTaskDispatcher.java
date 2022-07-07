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

package com.bytechef.task.dispatcher.map;

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
 * @author Arik Cohen
 * @since Jun 4, 2017
 */
public class MapTaskDispatcher implements TaskDispatcher<TaskExecution>, TaskDispatcherResolver {

    private final TaskDispatcher<? super TaskExecution> taskDispatcher;
    private final TaskEvaluator taskEvaluator;
    private final TaskExecutionService taskExecutionService;
    private final MessageBroker messageBroker;
    private final ContextService contextService;
    private final CounterService counterService;

    private MapTaskDispatcher(Builder builder) {
        taskDispatcher = builder.taskDispatcher;
        taskExecutionService = builder.taskExecutionService;
        messageBroker = builder.messageBroker;
        contextService = builder.contextService;
        counterService = builder.counterService;
        taskEvaluator = builder.taskEvaluator;
    }

    @Override
    public void dispatch(TaskExecution taskExecution) {
        List<Object> list = taskExecution.getList("list", Object.class);
        Map<String, Object> iteratee = taskExecution.getMap("iteratee");

        Assert.notNull(list, "'list' property can't be null");
        Assert.notNull(iteratee, "'iteratee' property can't be null");

        SimpleTaskExecution parentMapTask = SimpleTaskExecution.of(taskExecution);

        parentMapTask.setStartTime(new Date());
        parentMapTask.setStatus(TaskStatus.STARTED);

        taskExecutionService.merge(parentMapTask);

        if (list.size() > 0) {
            counterService.set(taskExecution.getId(), list.size());

            for (int i = 0; i < list.size(); i++) {
                Object item = list.get(i);
                SimpleTaskExecution mapTask = SimpleTaskExecution.of(iteratee);

                mapTask.setId(UUIDGenerator.generate());
                mapTask.setParentId(taskExecution.getId());
                mapTask.setStatus(TaskStatus.CREATED);
                mapTask.setJobId(taskExecution.getJobId());
                mapTask.setCreateTime(new Date());
                mapTask.setPriority(taskExecution.getPriority());
                mapTask.setTaskNumber(i + 1);

                MapContext context = new MapContext(contextService.peek(taskExecution.getId()));

                context.set(taskExecution.getString("itemVar", "item"), item);
                context.set(taskExecution.getString("itemIndex", "itemIndex"), i);

                contextService.push(mapTask.getId(), context);

                TaskExecution evaluatedEachTask = taskEvaluator.evaluate(mapTask, context);

                taskExecutionService.create(evaluatedEachTask);
                taskDispatcher.dispatch(evaluatedEachTask);
            }
        } else {
            SimpleTaskExecution completion = SimpleTaskExecution.of(taskExecution);

            completion.setStartTime(new Date());
            completion.setEndTime(new Date());
            completion.setExecutionTime(0);

            messageBroker.send(Queues.COMPLETIONS, completion);
        }
    }

    @Override
    public TaskDispatcher<?> resolve(Task task) {
        if (task.getType().equals(Constants.MAP)) {
            return this;
        }

        return null;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private TaskDispatcher<? super TaskExecution> taskDispatcher;
        private TaskEvaluator taskEvaluator;
        private TaskExecutionService taskExecutionService;
        private MessageBroker messageBroker;
        private ContextService contextService;
        private CounterService counterService;

        public Builder taskDispatcher(TaskDispatcher<? super TaskExecution> taskDispatcher) {
            this.taskDispatcher = taskDispatcher;

            return this;
        }

        public Builder taskEvaluator(TaskEvaluator taskEvaluator) {
            this.taskEvaluator = taskEvaluator;

            return this;
        }

        public Builder taskExecutionService(TaskExecutionService taskExecutionService) {
            this.taskExecutionService = taskExecutionService;

            return this;
        }

        public Builder messageBroker(MessageBroker messageBroker) {
            this.messageBroker = messageBroker;

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
