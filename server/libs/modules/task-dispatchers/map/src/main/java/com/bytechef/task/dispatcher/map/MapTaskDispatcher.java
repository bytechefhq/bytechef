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
 * Modifications copyright (C) 2025 ByteChef
 */

package com.bytechef.task.dispatcher.map;

import static com.bytechef.task.dispatcher.map.constant.MapTaskDispatcherConstants.INDEX;
import static com.bytechef.task.dispatcher.map.constant.MapTaskDispatcherConstants.ITEM;
import static com.bytechef.task.dispatcher.map.constant.MapTaskDispatcherConstants.ITEMS;
import static com.bytechef.task.dispatcher.map.constant.MapTaskDispatcherConstants.ITERATEE;
import static com.bytechef.task.dispatcher.map.constant.MapTaskDispatcherConstants.MAP;

import com.bytechef.atlas.configuration.constant.WorkflowConstants;
import com.bytechef.atlas.configuration.domain.Task;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.coordinator.event.TaskExecutionCompleteEvent;
import com.bytechef.atlas.coordinator.task.dispatcher.ErrorHandlingTaskDispatcher;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcher;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcherResolver;
import com.bytechef.atlas.execution.domain.Context.Classname;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.CounterService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.task.dispatcher.map.constant.MapTaskDispatcherConstants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.lang3.Validate;
import org.springframework.context.ApplicationEventPublisher;
import tools.jackson.core.type.TypeReference;

/**
 * @author Arik Cohen
 * @since Jun 4, 2017
 */
public class MapTaskDispatcher extends ErrorHandlingTaskDispatcher implements TaskDispatcherResolver {

    private final ContextService contextService;
    private final CounterService counterService;
    private final Evaluator evaluator;
    private final ApplicationEventPublisher eventPublisher;
    private final TaskDispatcher<? super TaskExecution> taskDispatcher;
    private final TaskExecutionService taskExecutionService;
    private final TaskFileStorage taskFileStorage;

    @SuppressFBWarnings("EI")
    public MapTaskDispatcher(
        ContextService contextService, CounterService counterService, Evaluator evaluator,
        ApplicationEventPublisher eventPublisher, TaskDispatcher<? super TaskExecution> taskDispatcher,
        TaskExecutionService taskExecutionService, TaskFileStorage taskFileStorage) {

        super(eventPublisher);

        this.contextService = contextService;
        this.counterService = counterService;
        this.evaluator = evaluator;
        this.eventPublisher = eventPublisher;
        this.taskDispatcher = taskDispatcher;
        this.taskExecutionService = taskExecutionService;
        this.taskFileStorage = taskFileStorage;
    }

    @Override
    public void doDispatch(TaskExecution taskExecution) {
        List<?> items = MapUtils.getRequiredList(taskExecution.getParameters(), ITEMS, Object.class);
        List<WorkflowTask> iterateeWorkflowTasks = MapUtils
            .getList(
                taskExecution.getParameters(), ITERATEE, new TypeReference<Map<String, ?>>() {}, List.of())
            .stream()
            .map(WorkflowTask::new)
            .toList();
        taskExecution.setStartDate(Instant.now());
        taskExecution.setStatus(TaskExecution.Status.STARTED);

        taskExecution = taskExecutionService.update(taskExecution);

        if (items.isEmpty()) {
            taskExecution.setStartDate(Instant.now());
            taskExecution.setEndDate(Instant.now());
            taskExecution.setExecutionTime(0);

            eventPublisher.publishEvent(new TaskExecutionCompleteEvent(taskExecution));
        } else {
            long taskExecutionId = Validate.notNull(taskExecution.getId(), "id");

            counterService.set(taskExecutionId, items.size());

            long taskExecutionJobId = Validate.notNull(
                taskExecution.getJobId(), "'taskExecution.jobId' must not be null");

            for (int i = 0; i < items.size(); i++) {
                Object item = items.get(i);
                WorkflowTask iterateeWorkflowTask = iterateeWorkflowTasks.getFirst();

                TaskExecution iterateeTaskExecution = TaskExecution.builder()
                    .jobId(taskExecutionJobId)
                    .parentId(taskExecution.getId())
                    .priority(taskExecution.getPriority())
                    .taskNumber(1)
                    .workflowTask(
                        new WorkflowTask(
                            MapUtils.append(
                                iterateeWorkflowTask.toMap(), WorkflowConstants.PARAMETERS,
                                Map.of(MapTaskDispatcherConstants.ITERATION, i))))
                    .build();

                Map<String, Object> context = new HashMap<>(
                    taskFileStorage
                        .readContextValue(contextService.peek(taskExecutionId, Classname.TASK_EXECUTION)));

                WorkflowTask workflowTask = taskExecution.getWorkflowTask();

                context.put(workflowTask.getName(), Map.of(ITEM, item, INDEX, i));

                iterateeTaskExecution.evaluate(context, evaluator);

                iterateeTaskExecution = taskExecutionService.create(iterateeTaskExecution);

                long iterateeTaskExecutionId = Validate.notNull(iterateeTaskExecution.getId(), "id");

                contextService.push(
                    iterateeTaskExecutionId, Classname.TASK_EXECUTION,
                    taskFileStorage.storeContextValue(iterateeTaskExecutionId, Classname.TASK_EXECUTION, context));

                contextService.push(
                    taskExecutionId, i, Classname.TASK_EXECUTION,
                    taskFileStorage.storeContextValue(taskExecutionId, i, Classname.TASK_EXECUTION, context));
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
