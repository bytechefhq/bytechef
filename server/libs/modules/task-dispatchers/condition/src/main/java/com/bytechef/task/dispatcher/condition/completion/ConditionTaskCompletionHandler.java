
/*
 * Copyright 2021 <your company/name>.
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

import static com.bytechef.hermes.task.dispatcher.constant.TaskDispatcherConstants.Versions.VERSION_1;
import static com.bytechef.task.dispatcher.condition.constant.ConditionTaskDispatcherConstants.CASE_FALSE;
import static com.bytechef.task.dispatcher.condition.constant.ConditionTaskDispatcherConstants.CASE_TRUE;
import static com.bytechef.task.dispatcher.condition.constant.ConditionTaskDispatcherConstants.CONDITION;

import com.bytechef.atlas.coordinator.task.completion.TaskCompletionHandler;
import com.bytechef.atlas.domain.Context;
import com.bytechef.atlas.domain.TaskExecution;
import com.bytechef.atlas.service.ContextService;
import com.bytechef.atlas.service.TaskExecutionService;
import com.bytechef.atlas.task.Task;
import com.bytechef.atlas.task.WorkflowTask;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcher;
import com.bytechef.atlas.task.evaluator.TaskEvaluator;
import com.bytechef.atlas.task.execution.TaskStatus;
import com.bytechef.commons.util.MapValueUtils;
import com.bytechef.task.dispatcher.condition.util.ConditionTaskUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.core.ParameterizedTypeReference;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Matija Petanjek
 */
public class ConditionTaskCompletionHandler implements TaskCompletionHandler {

    private final ContextService contextService;
    private final TaskCompletionHandler taskCompletionHandler;
    private final TaskDispatcher<? super Task> taskDispatcher;
    private final TaskEvaluator taskEvaluator;
    private final TaskExecutionService taskExecutionService;

    public ConditionTaskCompletionHandler(
        ContextService contextService,
        TaskCompletionHandler taskCompletionHandler,
        TaskDispatcher<? super Task> taskDispatcher,
        TaskEvaluator taskEvaluator,
        TaskExecutionService taskExecutionService) {
        this.contextService = contextService;
        this.taskCompletionHandler = taskCompletionHandler;
        this.taskDispatcher = taskDispatcher;
        this.taskEvaluator = taskEvaluator;
        this.taskExecutionService = taskExecutionService;
    }

    @Override
    public boolean canHandle(TaskExecution taskExecution) {
        Long parentId = taskExecution.getParentId();

        if (parentId != null) {
            TaskExecution parentTaskExecution = taskExecutionService.getTaskExecution(parentId);

            String type = parentTaskExecution.getType();

            return type.equals(CONDITION + "/v" + VERSION_1);
        }

        return false;
    }

    @Override
    @SuppressFBWarnings("NP")
    public void handle(TaskExecution taskExecution) {
        taskExecution.setStatus(TaskStatus.COMPLETED);

        taskExecution = taskExecutionService.update(taskExecution);

        TaskExecution conditionTaskExecution = taskExecutionService.getTaskExecution(taskExecution.getParentId());

        if (taskExecution.getOutput() != null && taskExecution.getName() != null) {
            Map<String, Object> newContext = new HashMap<>(
                contextService.peek(conditionTaskExecution.getId(), Context.Classname.TASK_EXECUTION));

            newContext.put(taskExecution.getName(), taskExecution.getOutput());

            contextService.push(conditionTaskExecution.getId(), Context.Classname.TASK_EXECUTION, newContext);
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

            Map<String, Object> context = contextService.peek(
                conditionTaskExecution.getId(), Context.Classname.TASK_EXECUTION);

            subTaskExecution = taskEvaluator.evaluate(subTaskExecution, context);

            subTaskExecution = taskExecutionService.create(subTaskExecution);

            contextService.push(subTaskExecution.getId(), Context.Classname.TASK_EXECUTION, context);

            taskDispatcher.dispatch(subTaskExecution);
        }
        // no more tasks to execute -- complete the condition
        else {
            conditionTaskExecution.setEndDate(LocalDateTime.now());

            taskCompletionHandler.handle(conditionTaskExecution);
        }
    }

    private static List<WorkflowTask> getSubWorkflowTasks(TaskExecution conditionTaskExecution, String caseTrue) {
        return MapValueUtils.getList(
            conditionTaskExecution.getParameters(), caseTrue, new ParameterizedTypeReference<>() {},
            Collections.emptyList());
    }
}
