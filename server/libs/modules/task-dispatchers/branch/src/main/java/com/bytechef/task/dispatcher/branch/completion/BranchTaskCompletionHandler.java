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

package com.bytechef.task.dispatcher.branch.completion;

import static com.bytechef.task.dispatcher.branch.constant.BranchTaskDispatcherConstants.BRANCH;
import static com.bytechef.task.dispatcher.branch.constant.BranchTaskDispatcherConstants.CASES;
import static com.bytechef.task.dispatcher.branch.constant.BranchTaskDispatcherConstants.DEFAULT;
import static com.bytechef.task.dispatcher.branch.constant.BranchTaskDispatcherConstants.EXPRESSION;
import static com.bytechef.task.dispatcher.branch.constant.BranchTaskDispatcherConstants.KEY;
import static com.bytechef.task.dispatcher.branch.constant.BranchTaskDispatcherConstants.TASKS;

import com.bytechef.atlas.configuration.domain.Task;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.coordinator.task.completion.TaskCompletionHandler;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcher;
import com.bytechef.atlas.execution.domain.Context.Classname;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.commons.util.MapUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.Validate;

/**
 * @author Arik Cohen
 * @author Ivica Cardic
 * @since Jun 3, 2017
 */
public class BranchTaskCompletionHandler implements TaskCompletionHandler {

    private final ContextService contextService;
    private final TaskExecutionService taskExecutionService;
    private final TaskCompletionHandler taskCompletionHandler;
    private final TaskDispatcher<? super Task> taskDispatcher;
    private final TaskFileStorage taskFileStorage;

    @SuppressFBWarnings("EI")
    public BranchTaskCompletionHandler(
        ContextService contextService, TaskCompletionHandler taskCompletionHandler,
        TaskDispatcher<? super Task> taskDispatcher, TaskExecutionService taskExecutionService,
        TaskFileStorage taskFileStorage) {

        this.contextService = contextService;
        this.taskExecutionService = taskExecutionService;
        this.taskCompletionHandler = taskCompletionHandler;
        this.taskDispatcher = taskDispatcher;
        this.taskFileStorage = taskFileStorage;
    }

    @Override
    public boolean canHandle(TaskExecution taskExecution) {
        Long parentId = taskExecution.getParentId();

        if (parentId != null) {
            TaskExecution parentExecution = taskExecutionService.getTaskExecution(parentId);

            String type = parentExecution.getType();

            return type.equals(BRANCH + "/v1");
        }

        return false;
    }

    @Override
    public void handle(TaskExecution taskExecution) {
        taskExecution.setStatus(TaskExecution.Status.COMPLETED);

        taskExecution = taskExecutionService.update(taskExecution);

        TaskExecution branchTaskExecution = taskExecutionService.getTaskExecution(
            Validate.notNull(taskExecution.getParentId(), "parentId"));

        if (taskExecution.getOutput() != null && taskExecution.getName() != null) {
            Map<String, Object> newContext = new HashMap<>(
                taskFileStorage.readContextValue(
                    contextService.peek(Validate.notNull(branchTaskExecution.getId(), "id"),
                        Classname.TASK_EXECUTION)));

            newContext.put(
                taskExecution.getName(),
                taskFileStorage.readTaskExecutionOutput(taskExecution.getOutput()));

            contextService.push(
                Validate.notNull(branchTaskExecution.getId(), "id"), Classname.TASK_EXECUTION,
                taskFileStorage.storeContextValue(
                    Validate.notNull(branchTaskExecution.getId(), "id"), Classname.TASK_EXECUTION, newContext));
        }

        List<WorkflowTask> subWorkflowTasks = resolveCase(branchTaskExecution);

        if (taskExecution.getTaskNumber() < subWorkflowTasks.size()) {
            WorkflowTask workflowTask = subWorkflowTasks.get(taskExecution.getTaskNumber());

            TaskExecution subTaskExecution = TaskExecution.builder()
                .jobId(branchTaskExecution.getJobId())
                .parentId(branchTaskExecution.getId())
                .priority(branchTaskExecution.getPriority())
                .taskNumber(taskExecution.getTaskNumber() + 1)
                .workflowTask(workflowTask)
                .build();

            Map<String, ?> context = taskFileStorage.readContextValue(
                contextService.peek(Validate.notNull(branchTaskExecution.getId(), "id"), Classname.TASK_EXECUTION));

            subTaskExecution.evaluate(context);

            subTaskExecution = taskExecutionService.create(subTaskExecution);

            contextService.push(
                Validate.notNull(taskExecution.getId(), "id"), Classname.TASK_EXECUTION,
                taskFileStorage.storeContextValue(
                    Validate.notNull(taskExecution.getId(), "id"), Classname.TASK_EXECUTION, context));

            taskDispatcher.dispatch(subTaskExecution);
        }
        // no more tasks to execute -- complete the branch
        else {
            branchTaskExecution.setEndDate(LocalDateTime.now());

            taskCompletionHandler.handle(branchTaskExecution);
        }
    }

    private List<WorkflowTask> resolveCase(TaskExecution taskExecution) {
        Object expression = MapUtils.getRequired(taskExecution.getParameters(), EXPRESSION);
        List<WorkflowTask> caseWorkflowTasks = MapUtils.getList(
            taskExecution.getParameters(), CASES, WorkflowTask.class, Collections.emptyList());

        Validate.notNull(caseWorkflowTasks, "you must specify 'cases' in a branch statement");

        for (WorkflowTask caseWorkflowTask : caseWorkflowTasks) {
            Object key = MapUtils.getRequired(caseWorkflowTask.getParameters(), KEY);
            List<WorkflowTask> subWorkflowTasks = MapUtils.getList(
                caseWorkflowTask.getParameters(), TASKS, WorkflowTask.class, Collections.emptyList());

            if (key.equals(expression)) {
                return subWorkflowTasks;
            }
        }

        return MapUtils.getList(
            taskExecution.getParameters(), DEFAULT, WorkflowTask.class, Collections.emptyList());
    }
}
