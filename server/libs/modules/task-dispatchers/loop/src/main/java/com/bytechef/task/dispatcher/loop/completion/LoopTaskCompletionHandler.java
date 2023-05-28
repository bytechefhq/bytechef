
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
import static com.bytechef.task.dispatcher.loop.constant.LoopTaskDispatcherConstants.ITEM_VAR;
import static com.bytechef.task.dispatcher.loop.constant.LoopTaskDispatcherConstants.ITERATEE;
import static com.bytechef.task.dispatcher.loop.constant.LoopTaskDispatcherConstants.LIST;
import static com.bytechef.task.dispatcher.loop.constant.LoopTaskDispatcherConstants.LOOP;
import static com.bytechef.task.dispatcher.loop.constant.LoopTaskDispatcherConstants.LOOP_FOREVER;

import com.bytechef.atlas.coordinator.task.completion.TaskCompletionHandler;
import com.bytechef.atlas.domain.Context;
import com.bytechef.atlas.domain.TaskExecution;
import com.bytechef.atlas.service.ContextService;
import com.bytechef.atlas.service.TaskExecutionService;
import com.bytechef.atlas.task.Task;
import com.bytechef.atlas.task.WorkflowTask;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcher;
import com.bytechef.commons.util.MapValueUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        TaskExecution loopTaskExecution = taskExecutionService.getTaskExecution(taskExecution.getParentId());

        boolean loopForever = MapValueUtils.getBoolean(loopTaskExecution.getParameters(), LOOP_FOREVER, false);
        Map<String, ?> iteratee = MapValueUtils.getRequiredMap(loopTaskExecution.getParameters(), ITERATEE);
        List<?> list = MapValueUtils.getList(loopTaskExecution.getParameters(), LIST, Collections.emptyList());

        if (loopForever || taskExecution.getTaskNumber() < list.size()) {
            TaskExecution subTaskExecution = TaskExecution.builder()
                .jobId(loopTaskExecution.getJobId())
                .parentId(loopTaskExecution.getId())
                .priority(loopTaskExecution.getPriority())
                .taskNumber(taskExecution.getTaskNumber() + 1)
                .workflowTask(WorkflowTask.of(iteratee))
                .build();

            Map<String, Object> newContext = new HashMap<>(
                contextService.peek(loopTaskExecution.getId(), Context.Classname.TASK_EXECUTION));

            if (!list.isEmpty()) {
                newContext.put(
                    MapValueUtils.getString(loopTaskExecution.getParameters(), ITEM_VAR, ITEM),
                    list.get(taskExecution.getTaskNumber()));
            }

            newContext.put(
                MapValueUtils.getString(loopTaskExecution.getParameters(), ITEM_INDEX, ITEM_INDEX),
                taskExecution.getTaskNumber());

            subTaskExecution.evaluate(newContext);

            subTaskExecution = taskExecutionService.create(subTaskExecution);

            contextService.push(subTaskExecution.getId(), Context.Classname.TASK_EXECUTION, newContext);

            taskDispatcher.dispatch(subTaskExecution);
        } else {
            loopTaskExecution.setEndDate(LocalDateTime.now());

            taskCompletionHandler.handle(loopTaskExecution);
        }
    }
}
