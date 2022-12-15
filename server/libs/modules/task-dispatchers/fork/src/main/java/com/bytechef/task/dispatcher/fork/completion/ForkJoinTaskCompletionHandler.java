
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

package com.bytechef.task.dispatcher.fork.completion;

import com.bytechef.atlas.coordinator.task.completion.TaskCompletionHandler;
import com.bytechef.atlas.domain.Context;
import com.bytechef.atlas.domain.TaskExecution;
import com.bytechef.atlas.service.ContextService;
import com.bytechef.atlas.service.CounterService;
import com.bytechef.atlas.service.TaskExecutionService;
import com.bytechef.atlas.task.WorkflowTask;
import com.bytechef.atlas.task.dispatcher.TaskDispatcher;
import com.bytechef.atlas.task.evaluator.TaskEvaluator;
import com.bytechef.atlas.task.execution.TaskStatus;
import com.bytechef.commons.utils.MapUtils;
import com.bytechef.commons.utils.UUIDUtils;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Handles {@link TaskExecution} completions which are the child execution tasks of a parent <code>
 * fork</code> {@link TaskExecution}.
 *
 * <p>
 * This handler will either execute the next task in the branch, or if arrived at the last task, it will complete the
 * branch and check if all branches are now complete. If so, it will complete the overall <code>fork</code> task.
 *
 * @author Arik Cohen
 * @since May 11, 2017
 */
public class ForkJoinTaskCompletionHandler implements TaskCompletionHandler {

    private final TaskExecutionService taskExecutionService;
    private final TaskCompletionHandler taskCompletionHandler;
    private final CounterService counterService;
    private final TaskDispatcher taskDispatcher;
    private final ContextService contextService;
    private final TaskEvaluator taskEvaluator;

    public ForkJoinTaskCompletionHandler(
        TaskExecutionService taskExecutionService,
        TaskCompletionHandler taskCompletionHandler,
        CounterService counterService,
        TaskDispatcher taskDispatcher,
        ContextService contextService,
        TaskEvaluator taskEvaluator) {
        this.taskExecutionService = taskExecutionService;
        this.taskCompletionHandler = taskCompletionHandler;
        this.counterService = counterService;
        this.taskDispatcher = taskDispatcher;
        this.contextService = contextService;
        this.taskEvaluator = taskEvaluator;
    }

    @Override
    public void handle(TaskExecution taskExecution) {
        TaskExecution completedSubTaskExecution = new TaskExecution(taskExecution);

        completedSubTaskExecution.setStatus(TaskStatus.COMPLETED);

        taskExecutionService.update(completedSubTaskExecution);

        if (taskExecution.getOutput() != null && taskExecution.getName() != null) {
            Context context = contextService.peek(
                taskExecution.getParentId() + "/" + MapUtils.getInteger(taskExecution.getParameters(), "branch"));

            Context newContext = new Context(context);

            newContext.put(taskExecution.getName(), taskExecution.getOutput());

            contextService.push(
                taskExecution.getParentId() + "/" + MapUtils.getInteger(taskExecution.getParameters(), "branch"),
                newContext);
        }

        TaskExecution forkTaskExecution = taskExecutionService.getTaskExecution(taskExecution.getParentId());

        @SuppressWarnings("unchecked")
        List<List<WorkflowTask>> branchesWorkflowTasks = MapUtils
            .getList(forkTaskExecution.getParameters(), "branches", List.class)
            .stream()
            .map(curList -> ((List<Map<String, Object>>) curList)
                .stream()
                .map(WorkflowTask::new)
                .toList())
            .toList();

        List<WorkflowTask> branchWorkflowTasks = branchesWorkflowTasks
            .get(MapUtils.getInteger(taskExecution.getParameters(), "branch"));

        if (taskExecution.getTaskNumber() < branchWorkflowTasks.size()) {
            TaskExecution branchTaskExecution = new TaskExecution(
                branchWorkflowTasks.get(taskExecution.getTaskNumber()),
                Map.of("branch", MapUtils.getInteger(taskExecution.getParameters(), "branch")));

            branchTaskExecution.setId(UUIDUtils.generate());
            branchTaskExecution.setStatus(TaskStatus.CREATED);
            branchTaskExecution.setTaskNumber(taskExecution.getTaskNumber() + 1);
            branchTaskExecution.setJobId(taskExecution.getJobId());
            branchTaskExecution.setParentId(taskExecution.getParentId());
            branchTaskExecution.setPriority(taskExecution.getPriority());

            Context context = contextService.peek(
                taskExecution.getParentId() + "/" + MapUtils.getInteger(taskExecution.getParameters(), "branch"));

            contextService.push(branchTaskExecution.getId(), context);

            TaskExecution evaluatedTaskExecution = taskEvaluator.evaluate(branchTaskExecution, context);

            evaluatedTaskExecution = taskExecutionService.add(evaluatedTaskExecution);

            taskDispatcher.dispatch(evaluatedTaskExecution);
        } else {
            long branchesLeft = counterService.decrement(taskExecution.getParentId());
            if (branchesLeft == 0) {
                TaskExecution forkTask = new TaskExecution(
                    taskExecutionService.getTaskExecution(taskExecution.getParentId()));

                forkTask.setEndTime(LocalDateTime.now());

                taskCompletionHandler.handle(forkTask);
            }
        }
    }

    @Override
    public boolean canHandle(TaskExecution taskExecution) {
        return taskExecution.getParentId() != null && MapUtils.get(taskExecution.getParameters(), "branch") != null;
    }
}
