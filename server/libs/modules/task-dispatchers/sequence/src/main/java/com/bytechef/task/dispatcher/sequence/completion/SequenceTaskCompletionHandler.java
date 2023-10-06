
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

import static com.bytechef.task.dispatcher.sequence.SequenceTaskDispatcher.TASKS;
import static com.bytechef.task.dispatcher.sequence.constant.SequenceTaskDispatcherConstants.SEQUENCE;

import com.bytechef.atlas.coordinator.task.completion.TaskCompletionHandler;
import com.bytechef.atlas.execution.domain.Context;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.file.storage.facade.WorkflowFileStorageFacade;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.configuration.task.Task;
import com.bytechef.atlas.configuration.task.WorkflowTask;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcher;
import com.bytechef.commons.util.MapUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author Ivica Cardic
 * @author Matija Petanjek
 */
public class SequenceTaskCompletionHandler implements TaskCompletionHandler {

    private final TaskExecutionService taskExecutionService;
    private final TaskCompletionHandler taskCompletionHandler;
    private final TaskDispatcher<? super Task> taskDispatcher;
    private final ContextService contextService;
    private final WorkflowFileStorageFacade workflowFileStorageFacade;

    @SuppressFBWarnings("EI")
    public SequenceTaskCompletionHandler(
        ContextService contextService, TaskCompletionHandler taskCompletionHandler,
        TaskDispatcher<? super Task> taskDispatcher, TaskExecutionService taskExecutionService,
        WorkflowFileStorageFacade workflowFileStorageFacade) {

        this.contextService = contextService;
        this.taskCompletionHandler = taskCompletionHandler;
        this.taskDispatcher = taskDispatcher;
        this.taskExecutionService = taskExecutionService;
        this.workflowFileStorageFacade = workflowFileStorageFacade;
    }

    @Override
    public boolean canHandle(TaskExecution taskExecution) {
        Long parentId = taskExecution.getParentId();

        if (parentId != null) {
            TaskExecution parentTaskExecution = taskExecutionService.getTaskExecution(parentId);

            String type = parentTaskExecution.getType();

            return type.equals(SEQUENCE + "/v1");
        }

        return false;
    }

    @Override
    @SuppressFBWarnings("NP")
    public void handle(TaskExecution taskExecution) {
        taskExecution.setStatus(TaskExecution.Status.COMPLETED);

        taskExecution = taskExecutionService.update(taskExecution);

        TaskExecution sequenceTaskExecution = taskExecutionService.getTaskExecution(
            Objects.requireNonNull(taskExecution.getParentId()));

        if (taskExecution.getOutput() != null && taskExecution.getName() != null) {
            Map<String, Object> newContext = new HashMap<>(
                workflowFileStorageFacade.readContextValue(
                    contextService.peek(
                        Objects.requireNonNull(sequenceTaskExecution.getId()), Context.Classname.TASK_EXECUTION)));

            newContext.put(
                taskExecution.getName(),
                workflowFileStorageFacade.readTaskExecutionOutput(taskExecution.getOutput()));

            contextService.push(
                sequenceTaskExecution.getId(), Context.Classname.TASK_EXECUTION,
                workflowFileStorageFacade.storeContextValue(
                    sequenceTaskExecution.getId(), Context.Classname.TASK_EXECUTION, newContext));
        }

        List<WorkflowTask> subWorkflowTasks = MapUtils.getList(
            sequenceTaskExecution.getParameters(), TASKS, WorkflowTask.class, Collections.emptyList());

        if (taskExecution.getTaskNumber() < subWorkflowTasks.size()) {
            WorkflowTask subWorkflowTask = subWorkflowTasks.get(taskExecution.getTaskNumber());

            TaskExecution subTaskExecution = TaskExecution.builder()
                .jobId(sequenceTaskExecution.getJobId())
                .parentId(sequenceTaskExecution.getId())
                .priority(sequenceTaskExecution.getPriority())
                .taskNumber(taskExecution.getTaskNumber() + 1)
                .workflowTask(subWorkflowTask)
                .build();

            Map<String, ?> context = workflowFileStorageFacade.readContextValue(
                contextService.peek(
                    Objects.requireNonNull(sequenceTaskExecution.getId()), Context.Classname.TASK_EXECUTION));

            subTaskExecution.evaluate(context);

            subTaskExecution = taskExecutionService.create(subTaskExecution);

            contextService.push(
                Objects.requireNonNull(subTaskExecution.getId()), Context.Classname.TASK_EXECUTION,
                workflowFileStorageFacade.storeContextValue(
                    subTaskExecution.getId(), Context.Classname.TASK_EXECUTION, context));

            taskDispatcher.dispatch(subTaskExecution);
        } else {
            sequenceTaskExecution.setEndDate(LocalDateTime.now());

            taskCompletionHandler.handle(sequenceTaskExecution);
        }
    }
}
