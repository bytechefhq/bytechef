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

package com.integri.atlas.task.dispatcher.loop.completion;

import com.integri.atlas.engine.Constants;
import com.integri.atlas.engine.context.MapContext;
import com.integri.atlas.engine.context.service.ContextService;
import com.integri.atlas.engine.coordinator.task.completion.TaskCompletionHandler;
import com.integri.atlas.engine.task.dispatcher.TaskDispatcher;
import com.integri.atlas.engine.task.execution.SimpleTaskExecution;
import com.integri.atlas.engine.task.execution.TaskExecution;
import com.integri.atlas.engine.task.execution.TaskStatus;
import com.integri.atlas.engine.task.execution.evaluator.TaskEvaluator;
import com.integri.atlas.engine.task.execution.service.TaskExecutionService;
import com.integri.atlas.engine.uuid.UUIDGenerator;
import java.util.Date;
import java.util.List;
import java.util.Map;

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
        TaskExecutionService taskExecutionService
    ) {
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

            return parentExecution.getType().equals(Constants.LOOP);
        }

        return false;
    }

    @Override
    public void handle(TaskExecution taskExecution) {
        SimpleTaskExecution completedSubtaskExecution = SimpleTaskExecution.of(taskExecution);

        completedSubtaskExecution.setStatus(TaskStatus.COMPLETED);

        taskExecutionService.merge(completedSubtaskExecution);

        SimpleTaskExecution loopTaskExecution = SimpleTaskExecution.of(
            taskExecutionService.getTaskExecution(taskExecution.getParentId())
        );

        Map<String, Object> iteratee = loopTaskExecution.getMap("iteratee");
        List<Object> list = loopTaskExecution.getList("list", Object.class);

        if (list == null || taskExecution.getTaskNumber() < list.size()) {
            SimpleTaskExecution subTaskExecution = SimpleTaskExecution.of(iteratee);

            subTaskExecution.setCreateTime(new Date());
            subTaskExecution.setId(UUIDGenerator.generate());
            subTaskExecution.setJobId(loopTaskExecution.getJobId());
            subTaskExecution.setParentId(loopTaskExecution.getId());
            subTaskExecution.setPriority(loopTaskExecution.getPriority());
            subTaskExecution.setStatus(TaskStatus.CREATED);
            subTaskExecution.setTaskNumber(taskExecution.getTaskNumber() + 1);

            MapContext context = new MapContext(contextService.peek(loopTaskExecution.getId()));

            if (list != null) {
                Object item = list.get(taskExecution.getTaskNumber());

                context.set(loopTaskExecution.getString("itemVar", "item"), item);
            }

            context.set(loopTaskExecution.getString("itemIndex", "itemIndex"), taskExecution.getTaskNumber());

            contextService.push(subTaskExecution.getId(), context);

            TaskExecution evaluatedSubTaskExecution = taskEvaluator.evaluate(subTaskExecution, context);

            taskExecutionService.create(evaluatedSubTaskExecution);
            taskDispatcher.dispatch(evaluatedSubTaskExecution);
        } else {
            loopTaskExecution.setEndTime(new Date());
            loopTaskExecution.setExecutionTime(
                loopTaskExecution.getEndTime().getTime() - loopTaskExecution.getStartTime().getTime()
            );

            taskCompletionHandler.handle(loopTaskExecution);
        }
    }
}
