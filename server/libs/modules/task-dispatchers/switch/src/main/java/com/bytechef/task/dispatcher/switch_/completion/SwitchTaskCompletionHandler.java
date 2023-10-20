
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

import static com.bytechef.hermes.task.dispatcher.constant.TaskDispatcherConstants.Versions.VERSION_1;
import static com.bytechef.task.dispatcher.switch_.constant.SwitchTaskDispatcherConstants.CASES;
import static com.bytechef.task.dispatcher.switch_.constant.SwitchTaskDispatcherConstants.DEFAULT;
import static com.bytechef.task.dispatcher.switch_.constant.SwitchTaskDispatcherConstants.EXPRESSION;
import static com.bytechef.task.dispatcher.switch_.constant.SwitchTaskDispatcherConstants.KEY;
import static com.bytechef.task.dispatcher.switch_.constant.SwitchTaskDispatcherConstants.SWITCH;
import static com.bytechef.task.dispatcher.switch_.constant.SwitchTaskDispatcherConstants.TASKS;

import com.bytechef.atlas.coordinator.task.completion.TaskCompletionHandler;
import com.bytechef.atlas.domain.Context;
import com.bytechef.atlas.domain.TaskExecution;
import com.bytechef.atlas.service.ContextService;
import com.bytechef.atlas.service.TaskExecutionService;
import com.bytechef.atlas.task.Task;
import com.bytechef.atlas.task.WorkflowTask;
import com.bytechef.atlas.task.dispatcher.TaskDispatcher;
import com.bytechef.atlas.task.evaluator.TaskEvaluator;
import com.bytechef.atlas.task.execution.TaskStatus;
import com.bytechef.commons.util.MapValueUtils;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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
    private final TaskDispatcher<? super Task> taskDispatcher;
    private final TaskEvaluator taskEvaluator;

    public SwitchTaskCompletionHandler(
        ContextService contextService,
        TaskExecutionService taskExecutionService,
        TaskCompletionHandler taskCompletionHandler,
        TaskDispatcher<? super Task> taskDispatcher,
        TaskEvaluator taskEvaluator) {
        this.contextService = contextService;
        this.taskExecutionService = taskExecutionService;
        this.taskCompletionHandler = taskCompletionHandler;
        this.taskDispatcher = taskDispatcher;
        this.taskEvaluator = taskEvaluator;
    }

    @Override
    public boolean canHandle(TaskExecution taskExecution) {
        Long parentId = taskExecution.getParentId();

        if (parentId != null) {
            TaskExecution parentExecution = taskExecutionService.getTaskExecution(parentId);

            String type = parentExecution.getType();

            return type.equals(SWITCH + "/v" + VERSION_1);
        }

        return false;
    }

    @Override
    @SuppressFBWarnings("NP")
    public void handle(TaskExecution taskExecution) {
        taskExecution.setStatus(TaskStatus.COMPLETED);

        taskExecution = taskExecutionService.update(taskExecution);

        TaskExecution switchTaskExecution = taskExecutionService.getTaskExecution(taskExecution.getParentId());

        if (taskExecution.getOutput() != null && taskExecution.getName() != null) {
            Map<String, Object> newContext = new HashMap<>(
                contextService.peek(switchTaskExecution.getId(), Context.Classname.TASK_EXECUTION));

            newContext.put(taskExecution.getName(), taskExecution.getOutput());

            contextService.push(switchTaskExecution.getId(), Context.Classname.TASK_EXECUTION, newContext);
        }

        List<WorkflowTask> subWorkflowTasks = resolveCase(switchTaskExecution);

        if (taskExecution.getTaskNumber() < subWorkflowTasks.size()) {
            WorkflowTask workflowTask = subWorkflowTasks.get(taskExecution.getTaskNumber());

            TaskExecution subTaskExecution = TaskExecution.of(
                switchTaskExecution.getJobId(), switchTaskExecution.getId(), switchTaskExecution.getPriority(),
                taskExecution.getTaskNumber() + 1, workflowTask);

            Map<String, Object> context = contextService.peek(
                switchTaskExecution.getId(), Context.Classname.TASK_EXECUTION);

            subTaskExecution = taskEvaluator.evaluate(subTaskExecution, context);

            subTaskExecution = taskExecutionService.create(subTaskExecution);

            contextService.push(subTaskExecution.getId(), Context.Classname.TASK_EXECUTION, context);

            taskDispatcher.dispatch(subTaskExecution);
        }
        // no more tasks to execute -- complete the switch
        else {
            switchTaskExecution.setEndDate(LocalDateTime.now());

            taskCompletionHandler.handle(switchTaskExecution);
        }
    }

    private List<WorkflowTask> resolveCase(TaskExecution taskExecution) {
        Object expression = MapValueUtils.getRequired(taskExecution.getParameters(), EXPRESSION);
        List<WorkflowTask> caseWorkflowTasks = MapValueUtils
            .getList(taskExecution.getParameters(), CASES, Map.class, Collections.emptyList())
            .stream()
            .map(WorkflowTask::of)
            .toList();

        Assert.notNull(caseWorkflowTasks, "you must specify 'cases' in a switch statement");

        for (WorkflowTask caseWorkflowTask : caseWorkflowTasks) {
            Object key = MapValueUtils.getRequired(caseWorkflowTask.getParameters(), KEY);
            List<WorkflowTask> subWorkflowTasks = MapValueUtils
                .getList(caseWorkflowTask.getParameters(), TASKS, Map.class, Collections.emptyList())
                .stream()
                .map(WorkflowTask::of)
                .toList();

            if (key.equals(expression)) {
                return subWorkflowTasks;
            }
        }

        return MapValueUtils.getList(taskExecution.getParameters(), DEFAULT, Map.class, Collections.emptyList())
            .stream()
            .map(WorkflowTask::of)
            .toList();
    }
}
