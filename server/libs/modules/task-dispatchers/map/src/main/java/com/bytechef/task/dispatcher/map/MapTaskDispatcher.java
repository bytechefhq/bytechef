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

import static com.bytechef.hermes.task.dispatcher.constants.Versions.VERSION_1;
import static com.bytechef.task.dispatcher.map.constants.MapTaskDispatcherConstants.MAP;

import com.bytechef.atlas.domain.Context;
import com.bytechef.atlas.domain.TaskExecution;
import com.bytechef.atlas.message.broker.MessageBroker;
import com.bytechef.atlas.message.broker.Queues;
import com.bytechef.atlas.service.ContextService;
import com.bytechef.atlas.service.CounterService;
import com.bytechef.atlas.service.TaskExecutionService;
import com.bytechef.atlas.task.Task;
import com.bytechef.atlas.task.WorkflowTask;
import com.bytechef.atlas.task.dispatcher.TaskDispatcher;
import com.bytechef.atlas.task.dispatcher.TaskDispatcherResolver;
import com.bytechef.atlas.task.evaluator.TaskEvaluator;
import com.bytechef.atlas.task.execution.TaskStatus;
import java.time.LocalDateTime;
import java.util.List;
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
        WorkflowTask iteratee = taskExecution.getWorkflowTask("iteratee");

        Assert.notNull(list, "'list' property can't be null");
        Assert.notNull(iteratee, "'iteratee' property can't be null");

        TaskExecution mapTaskExecution = new TaskExecution(taskExecution);

        mapTaskExecution.setStartTime(LocalDateTime.now());
        mapTaskExecution.setStatus(TaskStatus.STARTED);

        taskExecutionService.update(mapTaskExecution);

        if (list.size() > 0) {
            counterService.set(taskExecution.getId(), list.size());

            for (int i = 0; i < list.size(); i++) {
                Object item = list.get(i);
                TaskExecution iterateeTaskExecution = TaskExecution.of(
                        iteratee, taskExecution.getJobId(), taskExecution.getId(), taskExecution.getPriority(), i + 1);

                Context context = new Context(contextService.peek(taskExecution.getId()));

                context.put(taskExecution.getString("itemVar", "item"), item);
                context.put(taskExecution.getString("itemIndex", "itemIndex"), i);

                contextService.push(iterateeTaskExecution.getId(), context);

                TaskExecution evaluatedEachTask = taskEvaluator.evaluate(iterateeTaskExecution, context);

                evaluatedEachTask = taskExecutionService.add(evaluatedEachTask);

                taskDispatcher.dispatch(evaluatedEachTask);
            }
        } else {
            TaskExecution completion = new TaskExecution(taskExecution);

            completion.setStartTime(LocalDateTime.now());
            completion.setEndTime(LocalDateTime.now());
            completion.setExecutionTime(0);

            messageBroker.send(Queues.COMPLETIONS, completion);
        }
    }

    @Override
    public TaskDispatcher<?> resolve(Task task) {
        if (task.getType().equals(MAP + "/v" + VERSION_1)) {
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
