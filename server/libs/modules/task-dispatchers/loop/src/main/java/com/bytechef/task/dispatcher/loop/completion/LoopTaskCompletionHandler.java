/*
 * Copyright 2025 ByteChef
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

package com.bytechef.task.dispatcher.loop.completion;

import static com.bytechef.task.dispatcher.loop.constant.LoopTaskDispatcherConstants.INDEX;
import static com.bytechef.task.dispatcher.loop.constant.LoopTaskDispatcherConstants.ITEM;
import static com.bytechef.task.dispatcher.loop.constant.LoopTaskDispatcherConstants.ITEMS;
import static com.bytechef.task.dispatcher.loop.constant.LoopTaskDispatcherConstants.ITERATEE;
import static com.bytechef.task.dispatcher.loop.constant.LoopTaskDispatcherConstants.LOOP;
import static com.bytechef.task.dispatcher.loop.constant.LoopTaskDispatcherConstants.LOOP_FOREVER;

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
import com.bytechef.evaluator.Evaluator;
import com.bytechef.file.storage.domain.FileEntry;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.lang3.Validate;

/**
 * @author Ivica Cardic
 * @author Igor Beslic
 */
public class LoopTaskCompletionHandler implements TaskCompletionHandler {

    private final ContextService contextService;
    private final Evaluator evaluator;
    private final TaskDispatcher<? super Task> taskDispatcher;
    private final TaskExecutionService taskExecutionService;
    private final TaskCompletionHandler taskCompletionHandler;
    private final TaskFileStorage taskFileStorage;

    @SuppressFBWarnings("EI")
    public LoopTaskCompletionHandler(
        ContextService contextService, Evaluator evaluator, TaskCompletionHandler taskCompletionHandler,
        TaskDispatcher<? super Task> taskDispatcher, TaskExecutionService taskExecutionService,
        TaskFileStorage taskFileStorage) {

        this.contextService = contextService;
        this.evaluator = evaluator;
        this.taskCompletionHandler = taskCompletionHandler;
        this.taskDispatcher = taskDispatcher;
        this.taskExecutionService = taskExecutionService;
        this.taskFileStorage = taskFileStorage;
    }

    @Override
    public boolean canHandle(TaskExecution taskExecution) {
        Long parentId = taskExecution.getParentId();

        if (parentId != null) {
            TaskExecution parentExecution = taskExecutionService.getTaskExecution(parentId);

            String type = parentExecution.getType();

            return type.equals(LOOP + "/v1");
        }

        return false;
    }

    @Override
    public void handle(TaskExecution taskExecution) {
        TaskExecution loopTaskExecution = taskExecutionService.getTaskExecution(
            Validate.notNull(taskExecution.getParentId(), "parentId"));

        taskExecution.setStatus(TaskExecution.Status.COMPLETED);

        taskExecution = taskExecutionService.update(taskExecution);

        List<WorkflowTask> iterateeWorkflowTasks = MapUtils.getRequiredList(
            loopTaskExecution.getParameters(), ITERATEE, WorkflowTask.class);

        Map<String, Object> parentContextValue = updateParentContextValue(taskExecution, loopTaskExecution);

        int nextTaskIndex = taskExecution.getTaskNumber() + 1;

        if (nextTaskIndex < iterateeWorkflowTasks.size()) {
            handleCurrentIterationNextChildTaskExecution(
                taskExecution, loopTaskExecution, nextTaskIndex, iterateeWorkflowTasks.get(nextTaskIndex),
                parentContextValue);

            return;
        }

        boolean loopForever = MapUtils.getBoolean(loopTaskExecution.getParameters(), LOOP_FOREVER, false);
        List<?> items = MapUtils.getList(loopTaskExecution.getParameters(), ITEMS, Collections.emptyList());

        FileEntry contextValueFileEntry = contextService.peek(
            Validate.notNull(taskExecution.getId(), "task execution id"), Classname.TASK_EXECUTION);

        Map<String, Object> newTaskExecutionContext = new HashMap<>(
            taskFileStorage.readContextValue(contextValueFileEntry));

        WorkflowTask loopWorkflowTask = loopTaskExecution.getWorkflowTask();

        Map<String, ?> loopWorkflowTaskNameMap = MapUtils.getRequiredMap(
            newTaskExecutionContext, loopWorkflowTask.getName());

        int index = (Integer) loopWorkflowTaskNameMap.get(INDEX) + 1;

        if (loopForever || index < items.size()) {
            handleNewIterationFirstChildTaskExecution(loopTaskExecution, iterateeWorkflowTasks, items, index);
        } else {
            loopTaskExecution.setEndDate(Instant.now());

            loopTaskExecution = taskExecutionService.update(loopTaskExecution);

            taskCompletionHandler.handle(loopTaskExecution);
        }
    }

    private void handleNewIterationFirstChildTaskExecution(
        TaskExecution parentTaskExecution, List<WorkflowTask> iterateeWorkflowTasks, List<?> items, Integer index) {

        TaskExecution firstChildTaskExecution = TaskExecution.builder()
            .jobId(parentTaskExecution.getJobId())
            .parentId(parentTaskExecution.getId())
            .priority(parentTaskExecution.getPriority())
            .taskNumber(0)
            .workflowTask(iterateeWorkflowTasks.getFirst())
            .build();

        FileEntry contextValueFileEntry = contextService.peek(
            Validate.notNull(parentTaskExecution.getId(), "parent id"), Classname.TASK_EXECUTION);

        Map<String, Object> firstChildContextValue = new HashMap<>(
            taskFileStorage.readContextValue(contextValueFileEntry));

        Map<String, Object> workflowTaskNameMap = new HashMap<>();

        if (!items.isEmpty()) {
            workflowTaskNameMap.put(ITEM, items.get(index));
        }

        workflowTaskNameMap.put(INDEX, index);

        WorkflowTask parentWorkflowTask = parentTaskExecution.getWorkflowTask();

        firstChildContextValue.put(parentWorkflowTask.getName(), workflowTaskNameMap);

        firstChildTaskExecution = taskExecutionService.create(
            firstChildTaskExecution.evaluate(firstChildContextValue, evaluator));

        contextService.push(
            Validate.notNull(firstChildTaskExecution.getId(), "id"), Classname.TASK_EXECUTION,
            taskFileStorage.storeContextValue(
                Validate.notNull(firstChildTaskExecution.getId(), "id"), Classname.TASK_EXECUTION,
                firstChildContextValue));

        taskDispatcher.dispatch(firstChildTaskExecution);
    }

    private void handleCurrentIterationNextChildTaskExecution(
        TaskExecution previousChildTaskExecution, TaskExecution parentTaskExecution, int nextTaskIndex,
        WorkflowTask nextIterateeWorkflowTask, Map<String, Object> parentContextValue) {

        TaskExecution nextChildTaskExecution = TaskExecution.builder()
            .jobId(parentTaskExecution.getJobId())
            .parentId(parentTaskExecution.getId())
            .priority(parentTaskExecution.getPriority())
            .taskNumber(nextTaskIndex)
            .workflowTask(nextIterateeWorkflowTask)
            .build();

        Map<String, Object> nextChildContextValue = new HashMap<>(
            taskFileStorage.readContextValue(
                contextService.peek(
                    Validate.notNull(previousChildTaskExecution.getId(), "id"), Classname.TASK_EXECUTION)));

        if (parentContextValue.containsKey(previousChildTaskExecution.getName())) {
            nextChildContextValue.put(previousChildTaskExecution.getName(),
                parentContextValue.get(previousChildTaskExecution.getName()));
        }

        nextChildTaskExecution = taskExecutionService.create(
            nextChildTaskExecution.evaluate(nextChildContextValue, evaluator));

        contextService.push(
            Validate.notNull(nextChildTaskExecution.getId(), "id"), Classname.TASK_EXECUTION,
            taskFileStorage.storeContextValue(
                Validate.notNull(nextChildTaskExecution.getId(), "id"), Classname.TASK_EXECUTION,
                nextChildContextValue));

        taskDispatcher.dispatch(nextChildTaskExecution);
    }

    private Map<String, Object> updateParentContextValue(
        TaskExecution taskExecution, TaskExecution parentTaskExecution) {

        long parentTaskExecutionId = Objects.requireNonNull(parentTaskExecution.getId());

        Map<String, Object> contextValue = new HashMap<>(
            taskFileStorage.readContextValue(contextService.peek(parentTaskExecutionId, Classname.TASK_EXECUTION)));

        if (taskExecution.getName() != null) {
            if (taskExecution.getOutput() != null) {
                contextValue.put(
                    taskExecution.getName(), taskFileStorage.readTaskExecutionOutput(taskExecution.getOutput()));
            } else {
                contextValue.put(taskExecution.getName(), null);
            }

            contextService.push(
                parentTaskExecutionId, Classname.TASK_EXECUTION,
                taskFileStorage.storeContextValue(parentTaskExecutionId, Classname.TASK_EXECUTION, contextValue));
        }

        return contextValue;
    }
}
