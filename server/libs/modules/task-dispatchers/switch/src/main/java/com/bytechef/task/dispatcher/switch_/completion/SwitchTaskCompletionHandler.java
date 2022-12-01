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

package com.bytechef.task.dispatcher.switch_.completion;

import static com.bytechef.hermes.task.dispatcher.constants.Versions.VERSION_1;
import static com.bytechef.task.dispatcher.switch_.constants.SwitchTaskDispatcherConstants.CASES;
import static com.bytechef.task.dispatcher.switch_.constants.SwitchTaskDispatcherConstants.DEFAULT;
import static com.bytechef.task.dispatcher.switch_.constants.SwitchTaskDispatcherConstants.EXPRESSION;
import static com.bytechef.task.dispatcher.switch_.constants.SwitchTaskDispatcherConstants.KEY;
import static com.bytechef.task.dispatcher.switch_.constants.SwitchTaskDispatcherConstants.SWITCH;
import static com.bytechef.task.dispatcher.switch_.constants.SwitchTaskDispatcherConstants.TASKS;

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
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.util.Assert;

/**
 * @author Arik Cohen
 * @author Ivica Cardic
 * @since Jun 3, 2017
 */
public class SwitchTaskCompletionHandler implements TaskCompletionHandler {

    private final ContextService contextService;
    private final TaskExecutionService taskExecutionService;
    private final TaskCompletionHandler taskCompletionHandler;
    private final TaskDispatcher taskDispatcher;
    private final TaskEvaluator taskEvaluator;

    public SwitchTaskCompletionHandler(
            ContextService contextService,
            TaskExecutionService taskExecutionService,
            TaskCompletionHandler taskCompletionHandler,
            TaskDispatcher taskDispatcher,
            TaskEvaluator taskEvaluator) {
        this.contextService = contextService;
        this.taskExecutionService = taskExecutionService;
        this.taskCompletionHandler = taskCompletionHandler;
        this.taskDispatcher = taskDispatcher;
        this.taskEvaluator = taskEvaluator;
    }

    @Override
    public boolean canHandle(TaskExecution taskExecution) {
        String parentId = taskExecution.getParentId();

        if (parentId != null) {
            TaskExecution parentExecution = taskExecutionService.getTaskExecution(parentId);

            return parentExecution.getType().equals(SWITCH + "/v" + VERSION_1);
        }

        return false;
    }

    @Override
    public void handle(TaskExecution taskExecution) {
        TaskExecution completedSubTaskExecution = new TaskExecution(taskExecution);

        completedSubTaskExecution.setStatus(TaskStatus.COMPLETED);

        taskExecutionService.update(completedSubTaskExecution);

        TaskExecution switchTaskExecution =
                new TaskExecution(taskExecutionService.getTaskExecution(taskExecution.getParentId()));

        if (taskExecution.getOutput() != null && taskExecution.getName() != null) {
            Context context = contextService.peek(switchTaskExecution.getId());
            Context newContext = new Context(context);

            newContext.put(taskExecution.getName(), taskExecution.getOutput());

            contextService.push(switchTaskExecution.getId(), newContext);
        }

        List<WorkflowTask> subWorkflowTasks = resolveCase(switchTaskExecution);

        if (taskExecution.getTaskNumber() < subWorkflowTasks.size()) {
            WorkflowTask workflowTask = subWorkflowTasks.get(taskExecution.getTaskNumber());

            TaskExecution subTaskExecution = new TaskExecution(
                    workflowTask,
                    switchTaskExecution.getJobId(),
                    switchTaskExecution.getId(),
                    switchTaskExecution.getPriority(),
                    taskExecution.getTaskNumber() + 1);

            Context context = new Context(contextService.peek(switchTaskExecution.getId()));

            contextService.push(subTaskExecution.getId(), context);

            TaskExecution evaluatedSubTaskExecution = taskEvaluator.evaluate(subTaskExecution, context);

            evaluatedSubTaskExecution = taskExecutionService.add(evaluatedSubTaskExecution);

            taskDispatcher.dispatch(evaluatedSubTaskExecution);
        }
        // no more tasks to execute -- complete the switch
        else {
            switchTaskExecution.setEndTime(LocalDateTime.now());
            switchTaskExecution.setExecutionTime(LocalDateTimeUtils.getTime(switchTaskExecution.getEndTime())
                    - LocalDateTimeUtils.getTime(switchTaskExecution.getStartTime()));

            taskCompletionHandler.handle(switchTaskExecution);
        }
    }

    private List<WorkflowTask> resolveCase(TaskExecution taskExecution) {
        Object expression = taskExecution.getRequired(EXPRESSION);
        List<WorkflowTask> caseWorkflowTasks = taskExecution.getWorkflowTasks(CASES);

        Assert.notNull(caseWorkflowTasks, "you must specify 'cases' in a switch statement");

        for (WorkflowTask caseWorkflowTask : caseWorkflowTasks) {
            Object key = caseWorkflowTask.getRequired(KEY);
            List<WorkflowTask> subWorkflowTasks = caseWorkflowTask.getWorkflowTasks(TASKS);

            if (key.equals(expression)) {
                return subWorkflowTasks;
            }
        }

        return taskExecution.getWorkflowTasks(DEFAULT);
    }
}
