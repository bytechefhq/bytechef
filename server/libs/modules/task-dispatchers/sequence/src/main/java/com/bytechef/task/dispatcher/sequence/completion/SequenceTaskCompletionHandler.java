
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

package com.bytechef.task.dispatcher.sequence.completion;

import static com.bytechef.hermes.task.dispatcher.constants.TaskDispatcherConstants.Versions.VERSION_1;
import static com.bytechef.task.dispatcher.sequence.SequenceTaskDispatcher.TASKS;
import static com.bytechef.task.dispatcher.sequence.constants.SequenceTaskDispatcherConstants.SEQUENCE;

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
import com.bytechef.commons.utils.MapUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 * @author Matija Petanjek
 */
public class SequenceTaskCompletionHandler implements TaskCompletionHandler {

    private final TaskExecutionService taskExecutionService;
    private final TaskCompletionHandler taskCompletionHandler;
    private final TaskDispatcher<? super Task> taskDispatcher;
    private final ContextService contextService;
    private final TaskEvaluator taskEvaluator;

    public SequenceTaskCompletionHandler(
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
    public boolean canHandle(TaskExecution aTaskExecution) {
        Long parentId = aTaskExecution.getParentId();

        if (parentId != null) {
            TaskExecution parentTaskExecution = taskExecutionService.getTaskExecution(parentId);

            String type = parentTaskExecution.getType();

            return type.equals(SEQUENCE + "/v" + VERSION_1);
        }

        return false;
    }

    @Override
    @SuppressFBWarnings("NP")
    public void handle(TaskExecution taskExecution) {
        taskExecutionService.updateStatus(taskExecution.getId(), TaskStatus.COMPLETED, null, null);

        TaskExecution sequenceTaskExecution = taskExecutionService.getTaskExecution(taskExecution.getParentId());

        if (taskExecution.getOutput() != null && taskExecution.getName() != null) {
            Map<String, Object> newContext = new HashMap<>(
                contextService.peek(sequenceTaskExecution.getId(), Context.Classname.TASK_EXECUTION));

            newContext.put(taskExecution.getName(), taskExecution.getOutput());

            contextService.push(sequenceTaskExecution.getId(), Context.Classname.TASK_EXECUTION, newContext);
        }

        List<WorkflowTask> subWorkflowTasks = MapUtils
            .getList(sequenceTaskExecution.getParameters(), TASKS, Map.class, Collections.emptyList())
            .stream()
            .map(WorkflowTask::new)
            .toList();

        if (taskExecution.getTaskNumber() < subWorkflowTasks.size()) {
            WorkflowTask subWorkflowTask = subWorkflowTasks.get(taskExecution.getTaskNumber());

            TaskExecution subTaskExecution = TaskExecution.of(
                sequenceTaskExecution.getJobId(), sequenceTaskExecution.getId(), sequenceTaskExecution.getPriority(),
                taskExecution.getTaskNumber() + 1, subWorkflowTask);

            subTaskExecution = taskExecutionService.create(subTaskExecution);

            Map<String, Object> context = contextService.peek(
                sequenceTaskExecution.getId(), Context.Classname.TASK_EXECUTION);

            contextService.push(subTaskExecution.getId(), Context.Classname.TASK_EXECUTION, context);

            subTaskExecution.evaluate(taskEvaluator, context);

            taskDispatcher.dispatch(subTaskExecution);
        } else {
            sequenceTaskExecution.setEndTime(LocalDateTime.now());

            taskCompletionHandler.handle(sequenceTaskExecution);
        }
    }
}
