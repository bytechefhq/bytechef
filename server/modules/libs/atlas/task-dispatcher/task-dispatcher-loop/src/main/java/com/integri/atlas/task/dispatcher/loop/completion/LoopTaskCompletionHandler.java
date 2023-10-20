/*
 * Copyright 2021 <your company/name>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.integri.atlas.task.dispatcher.loop.completion;

import com.integri.atlas.engine.coordinator.task.completion.TaskCompletionHandler;
import com.integri.atlas.engine.core.DSL;
import com.integri.atlas.engine.core.context.Context;
import com.integri.atlas.engine.core.context.MapContext;
import com.integri.atlas.engine.core.context.repository.ContextRepository;
import com.integri.atlas.engine.core.task.SimpleTaskExecution;
import com.integri.atlas.engine.core.task.TaskExecution;
import com.integri.atlas.engine.core.task.TaskStatus;
import com.integri.atlas.engine.core.task.dispatcher.TaskDispatcher;
import com.integri.atlas.engine.core.task.evaluator.TaskEvaluator;
import com.integri.atlas.engine.core.task.repository.TaskExecutionRepository;
import com.integri.atlas.engine.core.uuid.UUIDGenerator;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class LoopTaskCompletionHandler implements TaskCompletionHandler {

    private final ContextRepository contextRepository;
    private final TaskDispatcher taskDispatcher;
    private final TaskEvaluator taskEvaluator;
    private final TaskExecutionRepository taskExecutionRepository;
    private final TaskCompletionHandler taskCompletionHandler;

    public LoopTaskCompletionHandler(
        ContextRepository contextRepository,
        TaskCompletionHandler taskCompletionHandler,
        TaskDispatcher taskDispatcher,
        TaskEvaluator taskEvaluator,
        TaskExecutionRepository taskExecutionRepository
    ) {
        this.contextRepository = contextRepository;
        this.taskCompletionHandler = taskCompletionHandler;
        this.taskDispatcher = taskDispatcher;
        this.taskEvaluator = taskEvaluator;
        this.taskExecutionRepository = taskExecutionRepository;
    }

    @Override
    public boolean canHandle(TaskExecution taskExecution) {
        String parentId = taskExecution.getParentId();

        if (parentId != null) {
            TaskExecution parentExecution = taskExecutionRepository.findOne(parentId);

            return parentExecution.getType().equals(DSL.LOOP);
        }

        return false;
    }

    @Override
    public void handle(TaskExecution taskExecution) {
        SimpleTaskExecution completedSubtaskExecution = SimpleTaskExecution.of(taskExecution);

        completedSubtaskExecution.setStatus(TaskStatus.COMPLETED);

        taskExecutionRepository.merge(completedSubtaskExecution);

        SimpleTaskExecution loopTaskExecution = SimpleTaskExecution.of(
            taskExecutionRepository.findOne(taskExecution.getParentId())
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

            MapContext context = new MapContext(contextRepository.peek(loopTaskExecution.getId()));

            if (list != null) {
                Object item = list.get(taskExecution.getTaskNumber());

                context.set(loopTaskExecution.getString("itemVar", "item"), item);
            }

            context.set(loopTaskExecution.getString("itemIndex", "itemIndex"), taskExecution.getTaskNumber());

            contextRepository.push(subTaskExecution.getId(), context);

            TaskExecution evaluatedSubTaskExecution = taskEvaluator.evaluate(subTaskExecution, context);

            taskExecutionRepository.create(evaluatedSubTaskExecution);
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
