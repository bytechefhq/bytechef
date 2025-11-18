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

package com.bytechef.task.dispatcher.map.completion;

import static com.bytechef.task.dispatcher.map.constant.MapTaskDispatcherConstants.ITERATEE;
import static com.bytechef.task.dispatcher.map.constant.MapTaskDispatcherConstants.ITERATION;
import static com.bytechef.task.dispatcher.map.constant.MapTaskDispatcherConstants.MAP;

import com.bytechef.atlas.configuration.constant.WorkflowConstants;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.coordinator.task.completion.TaskCompletionHandler;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcher;
import com.bytechef.atlas.execution.domain.Context;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.CounterService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.evaluator.Evaluator;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.lang3.Validate;

/**
 * @author Arik Cohen
 * @since June 4, 2017
 */
public class MapTaskCompletionHandler implements TaskCompletionHandler {

    private final ContextService contextService;
    private final CounterService counterService;
    private final Evaluator evaluator;
    private final TaskDispatcher<? super TaskExecution> taskDispatcher;
    private final TaskExecutionService taskExecutionService;
    private final TaskCompletionHandler taskCompletionHandler;
    private final TaskFileStorage taskFileStorage;

    @SuppressFBWarnings("EI")
    public MapTaskCompletionHandler(
        ContextService contextService, CounterService counterService, Evaluator evaluator,
        TaskDispatcher<? super TaskExecution> taskDispatcher, TaskCompletionHandler taskCompletionHandler,
        TaskExecutionService taskExecutionService, TaskFileStorage taskFileStorage) {

        this.contextService = contextService;
        this.counterService = counterService;
        this.evaluator = evaluator;
        this.taskDispatcher = taskDispatcher;
        this.taskExecutionService = taskExecutionService;
        this.taskCompletionHandler = taskCompletionHandler;
        this.taskFileStorage = taskFileStorage;
    }

    @Override
    public boolean canHandle(TaskExecution taskExecution) {
        Long parentId = taskExecution.getParentId();

        if (parentId != null) {
            TaskExecution parentExecution = taskExecutionService.getTaskExecution(parentId);

            String type = parentExecution.getType();

            return type.equals(MAP + "/v1") && MapUtils.get(taskExecution.getParameters(), ITERATION) != null;
        }

        return false;
    }

    @Override
    public void handle(TaskExecution taskExecution) {
        taskExecution.setStatus(TaskExecution.Status.COMPLETED);

        taskExecution = taskExecutionService.update(taskExecution);

        long taskExecutionParentId = Objects.requireNonNull(taskExecution.getParentId());

        int iterationIndex = MapUtils.getInteger(taskExecution.getParameters(), ITERATION);

        if (taskExecution.getName() != null) {
            Map<String, Object> newContext = new HashMap<>(
                taskFileStorage.readContextValue(
                    contextService.peek(taskExecutionParentId, iterationIndex, Context.Classname.TASK_EXECUTION)));

            if (taskExecution.getOutput() != null) {
                newContext.put(
                    taskExecution.getName(), taskFileStorage.readTaskExecutionOutput(taskExecution.getOutput()));
            } else {
                newContext.put(taskExecution.getName(), null);
            }

            contextService.push(
                taskExecutionParentId, iterationIndex, Context.Classname.TASK_EXECUTION,
                taskFileStorage.storeContextValue(
                    taskExecutionParentId, iterationIndex, Context.Classname.TASK_EXECUTION, newContext));
        }

        TaskExecution mapTaskExecution = taskExecutionService.getTaskExecution(taskExecutionParentId);

        List<WorkflowTask> iterateeWorkflowTasks = MapUtils.getRequiredList(
            mapTaskExecution.getParameters(), ITERATEE, WorkflowTask.class);

        if (taskExecution.getTaskNumber() < iterateeWorkflowTasks.size()) {
            WorkflowTask iterationWorkflowTask = iterateeWorkflowTasks.get(taskExecution.getTaskNumber());

            long taskExecutionJobId = Objects.requireNonNull(taskExecution.getJobId());

            TaskExecution iterationTaskExecution = TaskExecution.builder()
                .jobId(taskExecutionJobId)
                .parentId(taskExecution.getParentId())
                .priority(taskExecution.getPriority())
                .taskNumber(taskExecution.getTaskNumber() + 1)
                .workflowTask(
                    new WorkflowTask(
                        MapUtils.append(
                            iterationWorkflowTask.toMap(), WorkflowConstants.PARAMETERS,
                            Map.of(ITERATION, iterationIndex))))
                .build();

            Map<String, ?> context = taskFileStorage.readContextValue(
                contextService.peek(taskExecutionParentId, iterationIndex, Context.Classname.TASK_EXECUTION));

            iterationTaskExecution.evaluate(context, evaluator);

            iterationTaskExecution = taskExecutionService.create(iterationTaskExecution);

            long iterationTaskExecutionId = Objects.requireNonNull(iterationTaskExecution.getId(), "id");

            contextService.push(
                iterationTaskExecutionId, Context.Classname.TASK_EXECUTION,
                taskFileStorage.storeContextValue(iterationTaskExecutionId, Context.Classname.TASK_EXECUTION, context));

            taskDispatcher.dispatch(iterationTaskExecution);
        } else {
            if (taskExecution.getOutput() != null) {
                List<Object> outputs = new ArrayList<>(iterateeWorkflowTasks.size());

                if (mapTaskExecution.getOutput() != null) {
                    outputs.addAll((List<?>) taskFileStorage.readTaskExecutionOutput(mapTaskExecution.getOutput()));
                }

                outputs.add(iterationIndex, taskFileStorage.readTaskExecutionOutput(taskExecution.getOutput()));

                mapTaskExecution.setOutput(
                    taskFileStorage.storeTaskExecutionOutput(
                        Validate.notNull(mapTaskExecution.getId(), "id"), outputs));
            }

            long iterationsLeft = counterService.decrement(taskExecutionParentId);

            if (iterationsLeft == 0) {
                mapTaskExecution.setEndDate(Instant.now());

                mapTaskExecution = taskExecutionService.update(mapTaskExecution);

                taskCompletionHandler.handle(mapTaskExecution);
            }
        }
    }
}
