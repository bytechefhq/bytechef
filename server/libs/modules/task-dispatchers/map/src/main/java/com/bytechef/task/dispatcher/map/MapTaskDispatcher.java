
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

import static com.bytechef.task.dispatcher.map.constant.MapTaskDispatcherConstants.ITEM;
import static com.bytechef.task.dispatcher.map.constant.MapTaskDispatcherConstants.ITEM_INDEX;
import static com.bytechef.task.dispatcher.map.constant.MapTaskDispatcherConstants.ITEM_VAR;
import static com.bytechef.task.dispatcher.map.constant.MapTaskDispatcherConstants.ITERATEE;
import static com.bytechef.task.dispatcher.map.constant.MapTaskDispatcherConstants.LIST;
import static com.bytechef.task.dispatcher.map.constant.MapTaskDispatcherConstants.MAP;

import com.bytechef.atlas.execution.domain.Context;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.file.storage.facade.WorkflowFileStorageFacade;
import com.bytechef.atlas.execution.message.broker.TaskMessageRoute;
import com.bytechef.message.broker.MessageBroker;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.CounterService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.configuration.task.Task;
import com.bytechef.atlas.configuration.task.WorkflowTask;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcher;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcherResolver;
import com.bytechef.commons.util.MapUtils;
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
    private final TaskExecutionService taskExecutionService;
    private final MessageBroker messageBroker;
    private final ContextService contextService;
    private final CounterService counterService;
    private final WorkflowFileStorageFacade workflowFileStorageFacade;

    @SuppressFBWarnings("EI")
    public MapTaskDispatcher(
        ContextService contextService, CounterService counterService, MessageBroker messageBroker,
        TaskDispatcher<? super TaskExecution> taskDispatcher, TaskExecutionService taskExecutionService,
        WorkflowFileStorageFacade workflowFileStorageFacade) {

        this.contextService = contextService;
        this.counterService = counterService;
        this.messageBroker = messageBroker;
        this.taskDispatcher = taskDispatcher;
        this.taskExecutionService = taskExecutionService;
        this.workflowFileStorageFacade = workflowFileStorageFacade;
    }

    @Override
    @SuppressFBWarnings("NP")
    public void dispatch(TaskExecution taskExecution) {
        List<Object> list = MapUtils.getRequiredList(taskExecution.getParameters(), LIST, Object.class);
        Map<String, ?> iteratee = MapUtils.getRequiredMap(taskExecution.getParameters(), ITERATEE);

        taskExecution.setStartDate(LocalDateTime.now());
        taskExecution.setStatus(TaskExecution.Status.STARTED);

        taskExecution = taskExecutionService.update(taskExecution);

        if (list.isEmpty()) {
            taskExecution.setStartDate(LocalDateTime.now());
            taskExecution.setEndDate(LocalDateTime.now());
            taskExecution.setExecutionTime(0);

            messageBroker.send(TaskMessageRoute.TASKS_COMPLETE, taskExecution);
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
                    workflowFileStorageFacade.readContextValue(
                        contextService.peek(taskExecution.getId(), Context.Classname.TASK_EXECUTION)));

                newContext.put(MapUtils.getString(taskExecution.getParameters(), ITEM_VAR, ITEM), item);
                newContext.put(MapUtils.getString(taskExecution.getParameters(), ITEM_INDEX, ITEM_INDEX), i);

                iterateeTaskExecution.evaluate(newContext);

                iterateeTaskExecution = taskExecutionService.create(iterateeTaskExecution);

                contextService.push(
                    Objects.requireNonNull(iterateeTaskExecution.getId()), Context.Classname.TASK_EXECUTION,
                    workflowFileStorageFacade.storeTaskExecutionOutput(iterateeTaskExecution.getId(), newContext));

                taskDispatcher.dispatch(iterateeTaskExecution);
            }
        }
    }

    @Override
    public TaskDispatcher<? extends Task> resolve(Task task) {
        if (Objects.equals(task.getType(), MAP + "/v1")) {
            return this;
        }

        return null;
    }
}
