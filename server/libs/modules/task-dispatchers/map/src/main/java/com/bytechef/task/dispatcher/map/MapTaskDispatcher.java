
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

import static com.bytechef.hermes.task.dispatcher.constant.TaskDispatcherConstants.Versions.VERSION_1;
import static com.bytechef.task.dispatcher.map.constant.MapTaskDispatcherConstants.ITEM;
import static com.bytechef.task.dispatcher.map.constant.MapTaskDispatcherConstants.ITEM_INDEX;
import static com.bytechef.task.dispatcher.map.constant.MapTaskDispatcherConstants.ITEM_VAR;
import static com.bytechef.task.dispatcher.map.constant.MapTaskDispatcherConstants.ITERATEE;
import static com.bytechef.task.dispatcher.map.constant.MapTaskDispatcherConstants.LIST;
import static com.bytechef.task.dispatcher.map.constant.MapTaskDispatcherConstants.MAP;

import com.bytechef.atlas.domain.Context;
import com.bytechef.atlas.domain.TaskExecution;
import com.bytechef.atlas.message.broker.MessageBroker;
import com.bytechef.atlas.message.broker.TaskQueues;
import com.bytechef.atlas.service.ContextService;
import com.bytechef.atlas.service.CounterService;
import com.bytechef.atlas.service.TaskExecutionService;
import com.bytechef.atlas.task.Task;
import com.bytechef.atlas.task.WorkflowTask;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcher;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcherResolver;
import com.bytechef.atlas.task.evaluator.TaskEvaluator;
import com.bytechef.atlas.task.execution.TaskStatus;
import com.bytechef.commons.util.MapValueUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
    @SuppressFBWarnings("NP")
    public void dispatch(TaskExecution taskExecution) {
        List<Object> list = MapValueUtils.getRequiredList(taskExecution.getParameters(), LIST, Object.class);
        Map<String, Object> iteratee = MapValueUtils.getRequiredMap(taskExecution.getParameters(), ITERATEE);

        taskExecution.setStartDate(LocalDateTime.now());
        taskExecution.setStatus(TaskStatus.STARTED);

        taskExecution = taskExecutionService.update(taskExecution);

        if (list.isEmpty()) {
            taskExecution.setStartDate(LocalDateTime.now());
            taskExecution.setEndDate(LocalDateTime.now());
            taskExecution.setExecutionTime(0);

            messageBroker.send(TaskQueues.TASKS_COMPLETIONS, taskExecution);
        } else {
            counterService.set(taskExecution.getId(), list.size());

            for (int i = 0; i < list.size(); i++) {
                Object item = list.get(i);
                TaskExecution iterateeTaskExecution = TaskExecution.builder()
                    .jobId(taskExecution.getJobId())
                    .parentId(taskExecution.getId())
                    .priority(taskExecution.getPriority())
                    .taskNumber(i + 1)
                    .workflowTask(WorkflowTask.of(iteratee))
                    .build();

                Map<String, Object> newContext = new HashMap<>(
                    contextService.peek(taskExecution.getId(), Context.Classname.TASK_EXECUTION));

                newContext.put(MapValueUtils.getString(taskExecution.getParameters(), ITEM_VAR, ITEM), item);
                newContext.put(MapValueUtils.getString(taskExecution.getParameters(), ITEM_INDEX, ITEM_INDEX), i);

                iterateeTaskExecution = taskEvaluator.evaluate(iterateeTaskExecution, newContext);

                iterateeTaskExecution = taskExecutionService.create(iterateeTaskExecution);

                contextService.push(iterateeTaskExecution.getId(), Context.Classname.TASK_EXECUTION, newContext);

                taskDispatcher.dispatch(iterateeTaskExecution);
            }
        }
    }

    @Override
    public TaskDispatcher<? extends Task> resolve(Task task) {
        if (Objects.equals(task.getType(), MAP + "/v" + VERSION_1)) {
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
