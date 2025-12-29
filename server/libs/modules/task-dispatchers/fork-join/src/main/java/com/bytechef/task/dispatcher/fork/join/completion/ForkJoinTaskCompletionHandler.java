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

package com.bytechef.task.dispatcher.fork.join.completion;

import static com.bytechef.task.dispatcher.fork.join.constant.ForkJoinTaskDispatcherConstants.BRANCH;
import static com.bytechef.task.dispatcher.fork.join.constant.ForkJoinTaskDispatcherConstants.FORK_JOIN;

import com.bytechef.atlas.configuration.constant.WorkflowConstants;
import com.bytechef.atlas.configuration.domain.Task;
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
import com.bytechef.task.dispatcher.fork.join.constant.ForkJoinTaskDispatcherConstants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import tools.jackson.core.type.TypeReference;

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

    private final ContextService contextService;
    private final CounterService counterService;
    private final Evaluator evaluator;
    private final TaskExecutionService taskExecutionService;
    private final TaskCompletionHandler taskCompletionHandler;
    private final TaskDispatcher<? super Task> taskDispatcher;
    private final TaskFileStorage taskFileStorage;

    @SuppressFBWarnings("EI")
    public ForkJoinTaskCompletionHandler(
        ContextService contextService, CounterService counterService, Evaluator evaluator,
        TaskExecutionService taskExecutionService, TaskCompletionHandler taskCompletionHandler,
        TaskDispatcher<? super Task> taskDispatcher, TaskFileStorage taskFileStorage) {

        this.contextService = contextService;
        this.counterService = counterService;
        this.evaluator = evaluator;
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

            return type.equals(FORK_JOIN + "/v1") && MapUtils.get(taskExecution.getParameters(), BRANCH) != null;
        }

        return false;
    }

    @Override
    public void handle(TaskExecution taskExecution) {
        taskExecution.setStatus(TaskExecution.Status.COMPLETED);

        taskExecution = taskExecutionService.update(taskExecution);

        long taskExecutionParentId = Objects.requireNonNull(taskExecution.getParentId());

        if (taskExecution.getName() != null) {
            int branch = MapUtils.getInteger(taskExecution.getParameters(), BRANCH);

            Map<String, Object> newContext = new HashMap<>(
                taskFileStorage.readContextValue(
                    contextService.peek(taskExecutionParentId, branch, Context.Classname.TASK_EXECUTION)));

            if (taskExecution.getOutput() != null) {
                newContext.put(
                    taskExecution.getName(), taskFileStorage.readTaskExecutionOutput(taskExecution.getOutput()));
            } else {
                newContext.put(taskExecution.getName(), null);
            }

            contextService.push(
                taskExecutionParentId, branch, Context.Classname.TASK_EXECUTION,
                taskFileStorage.storeContextValue(
                    taskExecutionParentId, branch, Context.Classname.TASK_EXECUTION, newContext));
        }

        TaskExecution forkJoinTaskExecution = taskExecutionService.getTaskExecution(taskExecutionParentId);

        List<List<WorkflowTask>> branchesWorkflowTasks = MapUtils.getRequiredList(
            forkJoinTaskExecution.getParameters(), ForkJoinTaskDispatcherConstants.BRANCHES, new TypeReference<>() {});

        List<WorkflowTask> branchWorkflowTasks = branchesWorkflowTasks.get(
            MapUtils.getInteger(taskExecution.getParameters(), BRANCH));

        if (taskExecution.getTaskNumber() < branchWorkflowTasks.size()) {
            int branch = MapUtils.getInteger(taskExecution.getParameters(), BRANCH);

            WorkflowTask branchWorkflowTask = branchWorkflowTasks.get(taskExecution.getTaskNumber());

            long taskExecutionJobId = Objects.requireNonNull(taskExecution.getJobId());

            TaskExecution branchTaskExecution = TaskExecution.builder()
                .jobId(taskExecutionJobId)
                .parentId(taskExecution.getParentId())
                .priority(taskExecution.getPriority())
                .taskNumber(taskExecution.getTaskNumber() + 1)
                .workflowTask(
                    new WorkflowTask(
                        MapUtils.append(
                            branchWorkflowTask.toMap(), WorkflowConstants.PARAMETERS, Map.of(BRANCH, branch))))
                .build();

            Map<String, ?> context = taskFileStorage.readContextValue(
                contextService.peek(taskExecutionParentId, branch, Context.Classname.TASK_EXECUTION));

            branchTaskExecution.evaluate(context, evaluator);

            branchTaskExecution = taskExecutionService.create(branchTaskExecution);

            long branchTaskExecutionId = Objects.requireNonNull(branchTaskExecution.getId(), "id");

            contextService.push(
                branchTaskExecutionId, Context.Classname.TASK_EXECUTION,
                taskFileStorage.storeContextValue(branchTaskExecutionId, Context.Classname.TASK_EXECUTION, context));

            taskDispatcher.dispatch(branchTaskExecution);
        } else {
            long branchesLeft = counterService.decrement(taskExecutionParentId);

            if (branchesLeft == 0) {
                forkJoinTaskExecution.setEndDate(Instant.now());

                forkJoinTaskExecution = taskExecutionService.update(forkJoinTaskExecution);

                taskCompletionHandler.handle(forkJoinTaskExecution);
            }
        }
    }
}
