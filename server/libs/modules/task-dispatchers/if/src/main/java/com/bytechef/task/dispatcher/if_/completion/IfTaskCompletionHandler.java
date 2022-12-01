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

package com.bytechef.task.dispatcher.if_.completion;

import static com.bytechef.hermes.task.dispatcher.constants.Versions.VERSION_1;
import static com.bytechef.task.dispatcher.if_.constants.IfTaskDispatcherConstants.CASE_FALSE;
import static com.bytechef.task.dispatcher.if_.constants.IfTaskDispatcherConstants.CASE_TRUE;
import static com.bytechef.task.dispatcher.if_.constants.IfTaskDispatcherConstants.IF;

import com.bytechef.atlas.coordinator.task.completion.TaskCompletionHandler;
import com.bytechef.atlas.domain.Context;
import com.bytechef.atlas.domain.TaskExecution;
import com.bytechef.atlas.service.ContextService;
import com.bytechef.atlas.service.TaskExecutionService;
import com.bytechef.atlas.task.WorkflowTask;
import com.bytechef.atlas.task.dispatcher.TaskDispatcher;
import com.bytechef.atlas.task.evaluator.TaskEvaluator;
import com.bytechef.atlas.task.execution.TaskStatus;
import com.bytechef.commons.date.LocalDateTimeUtils;
import com.bytechef.task.dispatcher.if_.util.IfTaskUtils;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @author Matija Petanjek
 */
public class IfTaskCompletionHandler implements TaskCompletionHandler {

    private final ContextService contextService;
    private final TaskCompletionHandler taskCompletionHandler;
    private final TaskDispatcher taskDispatcher;
    private final TaskEvaluator taskEvaluator;
    private final TaskExecutionService taskExecutionService;

    public IfTaskCompletionHandler(
            ContextService contextService,
            TaskCompletionHandler taskCompletionHandler,
            TaskDispatcher taskDispatcher,
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
        String parentId = taskExecution.getParentId();

        if (parentId != null) {
            TaskExecution parentTaskExecution = taskExecutionService.getTaskExecution(parentId);

            return parentTaskExecution.getType().equals(IF + "/v" + VERSION_1);
        }

        return false;
    }

    @Override
    public void handle(TaskExecution taskExecution) {
        TaskExecution completedSubTaskExecution = new TaskExecution(taskExecution);

        completedSubTaskExecution.setStatus(TaskStatus.COMPLETED);

        taskExecutionService.update(completedSubTaskExecution);

        TaskExecution ifTaskExecution =
                new TaskExecution(taskExecutionService.getTaskExecution(taskExecution.getParentId()));

        if (taskExecution.getOutput() != null && taskExecution.getName() != null) {
            Context context = contextService.peek(ifTaskExecution.getId());

            Context newContext = new Context(context);

            newContext.put(taskExecution.getName(), taskExecution.getOutput());

            contextService.push(ifTaskExecution.getId(), newContext);
        }

        List<WorkflowTask> subWorkflowTasks;

        if (IfTaskUtils.resolveCase(ifTaskExecution)) {
            subWorkflowTasks = ifTaskExecution.getWorkflowTasks(CASE_TRUE);
        } else {
            subWorkflowTasks = ifTaskExecution.getWorkflowTasks(CASE_FALSE);
        }

        if (taskExecution.getTaskNumber() < subWorkflowTasks.size()) {
            WorkflowTask subWorkflowTask = subWorkflowTasks.get(taskExecution.getTaskNumber());

            TaskExecution subTaskExecution = new TaskExecution(
                    subWorkflowTask,
                    ifTaskExecution.getJobId(),
                    ifTaskExecution.getId(),
                    ifTaskExecution.getPriority(),
                    taskExecution.getTaskNumber() + 1);

            Context context = new Context(contextService.peek(ifTaskExecution.getId()));

            contextService.push(subTaskExecution.getId(), context);

            TaskExecution evaluatedTaskExecution = taskEvaluator.evaluate(subTaskExecution, context);

            evaluatedTaskExecution = taskExecutionService.add(evaluatedTaskExecution);

            taskDispatcher.dispatch(evaluatedTaskExecution);
        }
        // no more tasks to execute -- complete the If
        else {
            ifTaskExecution.setEndTime(LocalDateTime.now());
            ifTaskExecution.setExecutionTime(LocalDateTimeUtils.getTime(ifTaskExecution.getEndTime())
                    - LocalDateTimeUtils.getTime(ifTaskExecution.getStartTime()));

            taskCompletionHandler.handle(ifTaskExecution);
        }
    }
}
