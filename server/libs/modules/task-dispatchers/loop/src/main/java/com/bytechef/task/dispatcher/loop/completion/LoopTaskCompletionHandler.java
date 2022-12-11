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

import static com.bytechef.hermes.task.dispatcher.constants.Versions.VERSION_1;
import static com.bytechef.task.dispatcher.loop.constants.LoopTaskConstants.ITEM;
import static com.bytechef.task.dispatcher.loop.constants.LoopTaskConstants.ITEM_INDEX;
import static com.bytechef.task.dispatcher.loop.constants.LoopTaskConstants.ITEM_VAR;
import static com.bytechef.task.dispatcher.loop.constants.LoopTaskConstants.ITERATEE;
import static com.bytechef.task.dispatcher.loop.constants.LoopTaskConstants.LIST;
import static com.bytechef.task.dispatcher.loop.constants.LoopTaskConstants.LOOP;
import static com.bytechef.task.dispatcher.loop.constants.LoopTaskConstants.LOOP_FOREVER;

import com.bytechef.atlas.coordinator.task.completion.TaskCompletionHandler;
import com.bytechef.atlas.domain.Context;
import com.bytechef.atlas.domain.TaskExecution;
import com.bytechef.atlas.service.ContextService;
import com.bytechef.atlas.service.TaskExecutionService;
import com.bytechef.atlas.task.WorkflowTask;
import com.bytechef.atlas.task.dispatcher.TaskDispatcher;
import com.bytechef.atlas.task.evaluator.TaskEvaluator;
import com.bytechef.atlas.task.execution.TaskStatus;
import com.bytechef.commons.utils.MapUtils;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * @author Ivica Cardic
 */
public class LoopTaskCompletionHandler implements TaskCompletionHandler {

    private final ContextService contextService;
    private final TaskDispatcher taskDispatcher;
    private final TaskEvaluator taskEvaluator;
    private final TaskExecutionService taskExecutionService;
    private final TaskCompletionHandler taskCompletionHandler;

    public LoopTaskCompletionHandler(
            ContextService contextService,
            TaskCompletionHandler taskCompletionHandler,
            TaskDispatcher taskDispatcher,
            TaskEvaluator taskEvaluator,
            TaskExecutionService taskExecutionService) {
        this.contextService = contextService;
        this.taskCompletionHandler = taskCompletionHandler;
        this.taskDispatcher = taskDispatcher;
        this.taskEvaluator = taskEvaluator;
        this.taskExecutionService = taskExecutionService;
    }

    @Override
    public boolean canHandle(TaskExecution taskExecution) {
        String parentId = taskExecution.getParentId();

        if (parentId != null) {
            TaskExecution parentExecution = taskExecutionService.getTaskExecution(parentId);

            return parentExecution.getType().equals(LOOP + "/v" + VERSION_1);
        }

        return false;
    }

    @Override
    public void handle(TaskExecution taskExecution) {
        TaskExecution completedSubTaskExecution = new TaskExecution(taskExecution);

        completedSubTaskExecution.setStatus(TaskStatus.COMPLETED);

        taskExecutionService.update(completedSubTaskExecution);

        TaskExecution loopTaskExecution =
                new TaskExecution(taskExecutionService.getTaskExecution(taskExecution.getParentId()));

        boolean loopForever = MapUtils.getBoolean(loopTaskExecution.getParameters(), LOOP_FOREVER, false);
        WorkflowTask iterateeWorkflowTask =
                new WorkflowTask(MapUtils.getMap(loopTaskExecution.getParameters(), ITERATEE));
        List<Object> list =
                MapUtils.getList(loopTaskExecution.getParameters(), LIST, Object.class, Collections.emptyList());

        if (loopForever || taskExecution.getTaskNumber() < list.size()) {
            TaskExecution subTaskExecution = new TaskExecution(
                    iterateeWorkflowTask,
                    loopTaskExecution.getJobId(),
                    loopTaskExecution.getId(),
                    loopTaskExecution.getPriority(),
                    taskExecution.getTaskNumber() + 1);

            Context context = new Context(contextService.peek(loopTaskExecution.getId()));

            if (!list.isEmpty()) {
                context.put(
                        MapUtils.getString(loopTaskExecution.getParameters(), ITEM_VAR, ITEM),
                        list.get(taskExecution.getTaskNumber()));
            }

            context.put(
                    MapUtils.getString(loopTaskExecution.getParameters(), ITEM_INDEX, ITEM_INDEX),
                    taskExecution.getTaskNumber());

            contextService.push(subTaskExecution.getId(), context);

            TaskExecution evaluatedTaskExecution = taskEvaluator.evaluate(subTaskExecution, context);

            evaluatedTaskExecution = taskExecutionService.add(evaluatedTaskExecution);

            taskDispatcher.dispatch(evaluatedTaskExecution);
        } else {
            loopTaskExecution.setEndTime(LocalDateTime.now());

            taskCompletionHandler.handle(loopTaskExecution);
        }
    }
}
