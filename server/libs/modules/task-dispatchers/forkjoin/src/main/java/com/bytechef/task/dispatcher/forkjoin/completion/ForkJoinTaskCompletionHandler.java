
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

package com.bytechef.task.dispatcher.forkjoin.completion;

import com.bytechef.atlas.coordinator.task.completion.TaskCompletionHandler;
import com.bytechef.atlas.domain.Context;
import com.bytechef.atlas.domain.TaskExecution;
import com.bytechef.atlas.service.ContextService;
import com.bytechef.atlas.service.CounterService;
import com.bytechef.atlas.service.TaskExecutionService;
import com.bytechef.atlas.task.Task;
import com.bytechef.atlas.task.WorkflowTask;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcher;
import com.bytechef.atlas.task.evaluator.TaskEvaluator;
import com.bytechef.atlas.task.execution.TaskStatus;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.MapValueUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.util.Assert;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.bytechef.task.dispatcher.forkjoin.constant.ForkJoinTaskDispatcherConstants.BRANCH;
import static com.bytechef.task.dispatcher.forkjoin.constant.ForkJoinTaskDispatcherConstants.BRANCHES;

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
    private final TaskDispatcher<? super Task> taskDispatcher;
    private final ContextService contextService;
    private final TaskEvaluator taskEvaluator;

    public ForkJoinTaskCompletionHandler(
        TaskExecutionService taskExecutionService, TaskCompletionHandler taskCompletionHandler,
        CounterService counterService, TaskDispatcher<? super Task> taskDispatcher, ContextService contextService,
        TaskEvaluator taskEvaluator) {

        this.taskExecutionService = taskExecutionService;
        this.taskCompletionHandler = taskCompletionHandler;
        this.counterService = counterService;
        this.taskDispatcher = taskDispatcher;
        this.contextService = contextService;
        this.taskEvaluator = taskEvaluator;
    }

    @Override
    public boolean canHandle(TaskExecution taskExecution) {
        return taskExecution.getParentId() != null && MapValueUtils.get(taskExecution.getParameters(), BRANCH) != null;
    }

    @Override
    @SuppressFBWarnings("NP")
    public void handle(TaskExecution taskExecution) {
        taskExecution.setStatus(TaskStatus.COMPLETED);

        taskExecution = taskExecutionService.update(taskExecution);

        Assert.notNull(taskExecution.getParentId(), "'taskExecution.parentId' must not be null");

        if (taskExecution.getOutput() != null && taskExecution.getName() != null) {
            int branch = MapValueUtils.getInteger(taskExecution.getParameters(), BRANCH);

            Map<String, Object> newContext = new HashMap<>(
                contextService.peek(taskExecution.getParentId(), branch, Context.Classname.TASK_EXECUTION));

            newContext.put(taskExecution.getName(), taskExecution.getOutput());

            contextService.push(taskExecution.getParentId(), branch, Context.Classname.TASK_EXECUTION, newContext);
        }

        TaskExecution forkJoinTaskExecution = taskExecutionService.getTaskExecution(taskExecution.getParentId());

        List<List<Map<String, Object>>> branches = MapValueUtils.getRequiredList(
            forkJoinTaskExecution.getParameters(), BRANCHES, new ParameterizedTypeReference<>() {});

        List<List<WorkflowTask>> branchesWorkflowTasks = branches.stream()
            .map(curList -> CollectionUtils.map(curList, WorkflowTask::of))
            .toList();

        List<WorkflowTask> branchWorkflowTasks = branchesWorkflowTasks.get(
            MapValueUtils.getInteger(taskExecution.getParameters(), BRANCH));

        if (taskExecution.getTaskNumber() < branchWorkflowTasks.size()) {
            int branch = MapValueUtils.getInteger(taskExecution.getParameters(), BRANCH);

            WorkflowTask branchWorkflowTask = branchWorkflowTasks.get(taskExecution.getTaskNumber());

            Assert.notNull(taskExecution.getJobId(), "'taskExecution.jobId' must not be null");

            TaskExecution branchTaskExecution = TaskExecution.builder()
                .jobId(taskExecution.getJobId())
                .parentId(taskExecution.getId())
                .priority(taskExecution.getPriority())
                .taskNumber(taskExecution.getTaskNumber() + 1)
                .workflowTask(branchWorkflowTask.putParameter(BRANCH, branch))
                .build();

            Map<String, Object> context = contextService.peek(
                taskExecution.getParentId(), branch, Context.Classname.TASK_EXECUTION);

            branchTaskExecution = taskEvaluator.evaluate(branchTaskExecution, context);

            branchTaskExecution = taskExecutionService.create(branchTaskExecution);

            Assert.notNull(branchTaskExecution.getId(), "'branchTaskExecution.id' must not be null");

            contextService.push(branchTaskExecution.getId(), Context.Classname.TASK_EXECUTION, context);

            taskDispatcher.dispatch(branchTaskExecution);
        } else {
            long branchesLeft = counterService.decrement(taskExecution.getParentId());

            if (branchesLeft == 0) {
                forkJoinTaskExecution.setEndDate(LocalDateTime.now());

                taskCompletionHandler.handle(forkJoinTaskExecution);
            }
        }
    }
}
