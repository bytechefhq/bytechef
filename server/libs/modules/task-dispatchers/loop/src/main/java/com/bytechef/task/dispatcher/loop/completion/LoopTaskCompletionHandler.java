
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

package com.bytechef.task.dispatcher.loop.completion;

import static com.bytechef.task.dispatcher.loop.constant.LoopTaskDispatcherConstants.ITEM;
import static com.bytechef.task.dispatcher.loop.constant.LoopTaskDispatcherConstants.ITEM_INDEX;
import static com.bytechef.task.dispatcher.loop.constant.LoopTaskDispatcherConstants.ITERATEE;
import static com.bytechef.task.dispatcher.loop.constant.LoopTaskDispatcherConstants.LIST;
import static com.bytechef.task.dispatcher.loop.constant.LoopTaskDispatcherConstants.LOOP;
import static com.bytechef.task.dispatcher.loop.constant.LoopTaskDispatcherConstants.LOOP_FOREVER;

import com.bytechef.atlas.coordinator.task.completion.TaskCompletionHandler;
import com.bytechef.atlas.execution.domain.Context;
import com.bytechef.atlas.execution.domain.TaskExecution;
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
 */
public class LoopTaskCompletionHandler implements TaskCompletionHandler {

    private final ContextService contextService;
    private final TaskDispatcher<? super Task> taskDispatcher;
    private final TaskExecutionService taskExecutionService;
    private final TaskCompletionHandler taskCompletionHandler;

    @SuppressFBWarnings("EI")
    public LoopTaskCompletionHandler(
        ContextService contextService, TaskCompletionHandler taskCompletionHandler,
        TaskDispatcher<? super Task> taskDispatcher, TaskExecutionService taskExecutionService) {

        this.contextService = contextService;
        this.taskCompletionHandler = taskCompletionHandler;
        this.taskDispatcher = taskDispatcher;
        this.taskExecutionService = taskExecutionService;
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
    @SuppressFBWarnings("NP")
    public void handle(TaskExecution taskExecution) {
        taskExecution.setStatus(TaskExecution.Status.COMPLETED);

        taskExecution = taskExecutionService.update(taskExecution);

        TaskExecution loopTaskExecution = taskExecutionService.getTaskExecution(
            Objects.requireNonNull(taskExecution.getParentId()));

        boolean loopForever = MapUtils.getBoolean(loopTaskExecution.getParameters(), LOOP_FOREVER, false);
        Map<String, ?> iteratee = MapUtils.getRequiredMap(loopTaskExecution.getParameters(), ITERATEE);
        List<?> list = MapUtils.getList(loopTaskExecution.getParameters(), LIST, Collections.emptyList());

        if (loopForever || taskExecution.getTaskNumber() < list.size()) {
            TaskExecution subTaskExecution = TaskExecution.builder()
                .jobId(loopTaskExecution.getJobId())
                .parentId(loopTaskExecution.getId())
                .priority(loopTaskExecution.getPriority())
                .taskNumber(taskExecution.getTaskNumber() + 1)
                .workflowTask(WorkflowTask.of(iteratee))
                .build();

            Map<String, Object> newContext = new HashMap<>(
                contextService.peek(
                    Objects.requireNonNull(loopTaskExecution.getId()), Context.Classname.TASK_EXECUTION));

            WorkflowTask workflowTask = loopTaskExecution.getWorkflowTask();

            Map<String, Object> workflowTaskNameMap = new HashMap<>();

            if (!list.isEmpty()) {
                workflowTaskNameMap.put(ITEM, list.get(taskExecution.getTaskNumber()));
            }

            workflowTaskNameMap.put(ITEM_INDEX, taskExecution.getTaskNumber());

            newContext.put(workflowTask.getName(), workflowTaskNameMap);

            subTaskExecution = taskExecutionService.create(subTaskExecution.evaluate(newContext));

            contextService.push(
                Objects.requireNonNull(subTaskExecution.getId()), Context.Classname.TASK_EXECUTION, newContext);

            taskDispatcher.dispatch(subTaskExecution);
        } else {
            loopTaskExecution.setEndDate(LocalDateTime.now());

            taskCompletionHandler.handle(loopTaskExecution);
        }
    }
}
