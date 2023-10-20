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

package com.integri.atlas.task.dispatcher.sequence.completion;

import com.integri.atlas.engine.coordinator.task.completion.TaskCompletionHandler;
import com.integri.atlas.engine.core.DSL;
import com.integri.atlas.engine.core.MapObject;
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

/**
 * @author Ivica Cardic
 * @author Matija Petanjek
 */
public class SequenceTaskCompletionHandler implements TaskCompletionHandler {

    private final TaskExecutionRepository taskExecutionRepository;
    private final TaskCompletionHandler taskCompletionHandler;
    private final TaskDispatcher taskDispatcher;
    private final ContextRepository contextRepository;
    private final TaskEvaluator taskEvaluator;

    public SequenceTaskCompletionHandler(
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
    public boolean canHandle(TaskExecution aTaskExecution) {
        String parentId = aTaskExecution.getParentId();

        if (parentId != null) {
            TaskExecution parentTaskExecution = taskExecutionRepository.findOne(parentId);

            return parentTaskExecution.getType().equals(DSL.SEQUENCE);
        }

        return false;
    }

    @Override
    public void handle(TaskExecution taskExecution) {
        SimpleTaskExecution completedSubtaskExecution = SimpleTaskExecution.of(taskExecution);

        completedSubtaskExecution.setStatus(TaskStatus.COMPLETED);

        taskExecutionRepository.merge(completedSubtaskExecution);

        SimpleTaskExecution sequenceTaskExecution = SimpleTaskExecution.of(
            taskExecutionRepository.findOne(taskExecution.getParentId())
        );

        if (taskExecution.getOutput() != null && taskExecution.getName() != null) {
            Context context = contextRepository.peek(sequenceTaskExecution.getId());

            MapContext newContext = new MapContext(context.asMap());

            newContext.put(taskExecution.getName(), taskExecution.getOutput());

            contextRepository.push(sequenceTaskExecution.getId(), newContext);
        }

        List<MapObject> subtaskDefinitions = sequenceTaskExecution.getList("tasks", MapObject.class);

        if (taskExecution.getTaskNumber() < subtaskDefinitions.size()) {
            MapObject subtaskDefinition = subtaskDefinitions.get(taskExecution.getTaskNumber());

            SimpleTaskExecution subTaskExecution = SimpleTaskExecution.of(subtaskDefinition);

            subTaskExecution.setCreateTime(new Date());
            subTaskExecution.setId(UUIDGenerator.generate());
            subTaskExecution.setJobId(sequenceTaskExecution.getJobId());
            subTaskExecution.setParentId(sequenceTaskExecution.getId());
            subTaskExecution.setPriority(sequenceTaskExecution.getPriority());
            subTaskExecution.setStatus(TaskStatus.CREATED);
            subTaskExecution.setTaskNumber(taskExecution.getTaskNumber() + 1);

            MapContext context = new MapContext(contextRepository.peek(sequenceTaskExecution.getId()));

            contextRepository.push(subTaskExecution.getId(), context);

            TaskExecution evaluatedSubTaskExecution = taskEvaluator.evaluate(subTaskExecution, context);

            taskExecutionRepository.create(evaluatedSubTaskExecution);
            taskDispatcher.dispatch(evaluatedSubTaskExecution);
        }
        // no more tasks to execute -- complete the sequence
        else {
            Context parentContext;

            // If is root level, get the job's context
            if (sequenceTaskExecution.getParentId() == null) {
                parentContext = contextRepository.peek(sequenceTaskExecution.getJobId());
            }
            // otherwise get its parent's context
            else {
                parentContext = contextRepository.peek(sequenceTaskExecution.getParentId());
            }

            Context thisContext = contextRepository.peek(sequenceTaskExecution.getId());
            MapContext newContext = new MapContext(parentContext);

            newContext.putAll(thisContext.asMap());

            contextRepository.push(taskExecution.getJobId(), newContext);

            sequenceTaskExecution.setEndTime(new Date());
            sequenceTaskExecution.setExecutionTime(
                sequenceTaskExecution.getEndTime().getTime() - sequenceTaskExecution.getStartTime().getTime()
            );

            List<TaskExecution> children = taskExecutionRepository.findByParentId(taskExecution.getParentId());

            if (!children.isEmpty()) {
                TaskExecution lastTaskExecution = children.get(children.size() - 1);

                sequenceTaskExecution.setOutput(lastTaskExecution.getOutput());
            }

            taskCompletionHandler.handle(sequenceTaskExecution);
        }
    }
}
