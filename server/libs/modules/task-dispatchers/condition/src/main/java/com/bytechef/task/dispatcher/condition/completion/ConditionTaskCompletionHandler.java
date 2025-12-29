/*
 * Copyright 2025 ByteChef
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
 */

package com.bytechef.task.dispatcher.condition.completion;

import static com.bytechef.task.dispatcher.condition.constant.ConditionTaskDispatcherConstants.CASE_FALSE;
import static com.bytechef.task.dispatcher.condition.constant.ConditionTaskDispatcherConstants.CASE_TRUE;
import static com.bytechef.task.dispatcher.condition.constant.ConditionTaskDispatcherConstants.CONDITION;

import com.bytechef.atlas.configuration.domain.Task;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.coordinator.task.completion.TaskCompletionHandler;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcher;
import com.bytechef.atlas.execution.domain.Context;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.task.dispatcher.condition.util.ConditionTaskUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import tools.jackson.core.type.TypeReference;

/**
 * @author Matija Petanjek
 */
public class ConditionTaskCompletionHandler implements TaskCompletionHandler {

    private final ContextService contextService;
    private final Evaluator evaluator;
    private final TaskCompletionHandler taskCompletionHandler;
    private final TaskDispatcher<? super Task> taskDispatcher;
    private final TaskExecutionService taskExecutionService;
    private final TaskFileStorage taskFileStorage;

    @SuppressFBWarnings("EI")
    public ConditionTaskCompletionHandler(
        ContextService contextService, Evaluator evaluator, TaskCompletionHandler taskCompletionHandler,
        TaskDispatcher<? super Task> taskDispatcher, TaskExecutionService taskExecutionService,
        TaskFileStorage taskFileStorage) {

        this.contextService = contextService;
        this.evaluator = evaluator;
        this.taskCompletionHandler = taskCompletionHandler;
        this.taskDispatcher = taskDispatcher;
        this.taskExecutionService = taskExecutionService;
        this.taskFileStorage = taskFileStorage;
    }

    @Override
    public boolean canHandle(TaskExecution taskExecution) {
        Long parentId = taskExecution.getParentId();

        if (parentId != null) {
            TaskExecution parentTaskExecution = taskExecutionService.getTaskExecution(parentId);

            String type = parentTaskExecution.getType();

            return type.equals(CONDITION + "/v1");
        }

        return false;
    }

    @Override
    public void handle(TaskExecution taskExecution) {
        taskExecution.setStatus(TaskExecution.Status.COMPLETED);

        taskExecution = taskExecutionService.update(taskExecution);

        TaskExecution conditionTaskExecution = taskExecutionService.getTaskExecution(
            Objects.requireNonNull(taskExecution.getParentId()));

        long id = Objects.requireNonNull(conditionTaskExecution.getId());

        if (taskExecution.getName() != null) {
            Map<String, Object> newContext = new HashMap<>(
                taskFileStorage.readContextValue(contextService.peek(id, Context.Classname.TASK_EXECUTION)));

            if (taskExecution.getOutput() != null) {
                newContext.put(
                    taskExecution.getName(), taskFileStorage.readTaskExecutionOutput(taskExecution.getOutput()));
            } else {
                newContext.put(taskExecution.getName(), null);
            }

            contextService.push(
                id, Context.Classname.TASK_EXECUTION,
                taskFileStorage.storeContextValue(id, Context.Classname.TASK_EXECUTION, newContext));
        }

        List<WorkflowTask> subWorkflowTasks;

        if (ConditionTaskUtils.resolveCase(conditionTaskExecution)) {
            subWorkflowTasks = getSubWorkflowTasks(conditionTaskExecution, CASE_TRUE);
        } else {
            subWorkflowTasks = getSubWorkflowTasks(conditionTaskExecution, CASE_FALSE);
        }

        if (taskExecution.getTaskNumber() < subWorkflowTasks.size()) {
            WorkflowTask subWorkflowTask = subWorkflowTasks.get(taskExecution.getTaskNumber());

            TaskExecution subTaskExecution = TaskExecution.builder()
                .jobId(conditionTaskExecution.getJobId())
                .parentId(conditionTaskExecution.getId())
                .priority(conditionTaskExecution.getPriority())
                .taskNumber(taskExecution.getTaskNumber() + 1)
                .workflowTask(subWorkflowTask)
                .build();

            Map<String, ?> context = taskFileStorage.readContextValue(
                contextService.peek(id, Context.Classname.TASK_EXECUTION));

            subTaskExecution.evaluate(context, evaluator);

            subTaskExecution = taskExecutionService.create(subTaskExecution);

            long subTaskExecutionId = Objects.requireNonNull(subTaskExecution.getId());

            contextService.push(
                subTaskExecutionId, Context.Classname.TASK_EXECUTION,
                taskFileStorage.storeContextValue(subTaskExecutionId, Context.Classname.TASK_EXECUTION, context));

            taskDispatcher.dispatch(subTaskExecution);
        }
        // no more tasks to execute -- complete the condition
        else {
            conditionTaskExecution.setEndDate(Instant.now());

            conditionTaskExecution = taskExecutionService.update(conditionTaskExecution);

            taskCompletionHandler.handle(conditionTaskExecution);
        }
    }

    private static List<WorkflowTask> getSubWorkflowTasks(TaskExecution conditionTaskExecution, String caseTrue) {
        return MapUtils
            .getList(
                conditionTaskExecution.getParameters(), caseTrue, new TypeReference<Map<String, ?>>() {}, List.of())
            .stream()
            .map(WorkflowTask::new)
            .toList();
    }
}
