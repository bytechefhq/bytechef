/*
 * Copyright 2016-2020 the original author or authors.
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
 * Modifications copyright (C) 2023 ByteChef Inc.
 */

package com.bytechef.task.dispatcher.each;

import static com.bytechef.task.dispatcher.each.constant.EachTaskDispatcherConstants.ITERATEE;
import static com.bytechef.task.dispatcher.each.constant.EachTaskDispatcherConstants.LIST;

import com.bytechef.atlas.configuration.domain.Task;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.coordinator.event.TaskExecutionCompleteEvent;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcher;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcherResolver;
import com.bytechef.atlas.execution.domain.Context;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.CounterService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.task.dispatcher.each.constant.EachTaskDispatcherConstants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.lang3.Validate;
import org.springframework.context.ApplicationEventPublisher;

/**
 * A {@link TaskDispatcher} implementation which implements a parallel for-each construct. The dispatcher works by
 * executing the <code>iteratee</code> function on each item on the <code>list</code>.
 *
 * @author Arik Cohen
 * @since Apr 25, 2017
 */
public class EachTaskDispatcher implements TaskDispatcher<TaskExecution>, TaskDispatcherResolver {

    private final ApplicationEventPublisher eventPublisher;
    private final ContextService contextService;
    private final CounterService counterService;
    private final TaskDispatcher<? super Task> taskDispatcher;
    private final TaskExecutionService taskExecutionService;
    private final TaskFileStorage taskFileStorage;

    @SuppressFBWarnings("EI")
    public EachTaskDispatcher(
        ApplicationEventPublisher eventPublisher, ContextService contextService,
        CounterService counterService, TaskDispatcher<? super Task> taskDispatcher,
        TaskExecutionService taskExecutionService, TaskFileStorage taskFileStorage) {

        this.eventPublisher = eventPublisher;
        this.taskDispatcher = taskDispatcher;
        this.taskExecutionService = taskExecutionService;
        this.contextService = contextService;
        this.counterService = counterService;
        this.taskFileStorage = taskFileStorage;
    }

    @Override
    public void dispatch(TaskExecution taskExecution) {
        WorkflowTask iteratee = MapUtils.getRequired(taskExecution.getParameters(), ITERATEE, WorkflowTask.class);
        List<Object> list = MapUtils.getRequiredList(taskExecution.getParameters(), LIST, Object.class);

        taskExecution.setStartDate(LocalDateTime.now());
        taskExecution.setStatus(TaskExecution.Status.STARTED);

        taskExecution = taskExecutionService.update(taskExecution);

        if (list.isEmpty()) {
            taskExecution.setStartDate(LocalDateTime.now());
            taskExecution.setEndDate(LocalDateTime.now());
            taskExecution.setExecutionTime(0);

            eventPublisher.publishEvent(new TaskExecutionCompleteEvent(taskExecution));
        } else {
            counterService.set(Validate.notNull(taskExecution.getId(), "id"), list.size());

            for (int i = 0; i < list.size(); i++) {
                Object item = list.get(i);
                TaskExecution iterateeTaskExecution = TaskExecution.builder()
                    .jobId(taskExecution.getJobId())
                    .parentId(taskExecution.getId())
                    .priority(taskExecution.getPriority())
                    .taskNumber(i + 1)
                    .workflowTask(iteratee)
                    .build();

                Map<String, Object> newContext = new HashMap<>(
                    taskFileStorage.readContextValue(
                        contextService.peek(
                            Validate.notNull(taskExecution.getId(), "id"), Context.Classname.TASK_EXECUTION)));

                WorkflowTask workflowTask = taskExecution.getWorkflowTask();

                newContext.put(workflowTask.getName(),
                    Map.of(EachTaskDispatcherConstants.ITEM, item, EachTaskDispatcherConstants.ITEM_INDEX, i));

                iterateeTaskExecution = taskExecutionService.create(iterateeTaskExecution.evaluate(newContext));

                contextService.push(
                    Validate.notNull(iterateeTaskExecution.getId(), "id"), Context.Classname.TASK_EXECUTION,
                    taskFileStorage.storeContextValue(
                        Validate.notNull(iterateeTaskExecution.getId(), "id"), Context.Classname.TASK_EXECUTION,
                        newContext));

                taskDispatcher.dispatch(iterateeTaskExecution);
            }
        }
    }

    @Override
    public TaskDispatcher<? extends TaskExecution> resolve(Task task) {
        if (Objects.equals(task.getType(), EachTaskDispatcherConstants.EACH + "/v1")) {
            return this;
        }

        return null;
    }
}
