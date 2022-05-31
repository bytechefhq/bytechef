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

package com.integri.atlas.task.dispatcher.sequence;

import com.integri.atlas.engine.Constants;
import com.integri.atlas.engine.MapObject;
import com.integri.atlas.engine.context.MapContext;
import com.integri.atlas.engine.context.service.ContextService;
import com.integri.atlas.engine.message.broker.MessageBroker;
import com.integri.atlas.engine.message.broker.Queues;
import com.integri.atlas.engine.task.Task;
import com.integri.atlas.engine.task.dispatcher.TaskDispatcher;
import com.integri.atlas.engine.task.dispatcher.TaskDispatcherResolver;
import com.integri.atlas.engine.task.execution.SimpleTaskExecution;
import com.integri.atlas.engine.task.execution.TaskExecution;
import com.integri.atlas.engine.task.execution.TaskStatus;
import com.integri.atlas.engine.task.execution.evaluator.TaskEvaluator;
import com.integri.atlas.engine.task.execution.service.TaskExecutionService;
import com.integri.atlas.engine.uuid.UUIDGenerator;
import java.util.Date;
import java.util.List;

/**
 * @author Ivica Cardic
 * @author Matija Petanjek
 */
public class SequenceTaskDispatcher implements TaskDispatcher<TaskExecution>, TaskDispatcherResolver {

    private final ContextService contextService;
    private final MessageBroker messageBroker;
    private final TaskDispatcher taskDispatcher;
    private final TaskEvaluator taskEvaluator;
    private final TaskExecutionService taskExecutionService;

    public SequenceTaskDispatcher(
        ContextService contextService,
        MessageBroker messageBroker,
        TaskDispatcher taskDispatcher,
        TaskEvaluator taskEvaluator,
        TaskExecutionService taskExecutionService
    ) {
        this.contextService = contextService;
        this.messageBroker = messageBroker;
        this.taskDispatcher = taskDispatcher;
        this.taskEvaluator = taskEvaluator;
        this.taskExecutionService = taskExecutionService;
    }

    @Override
    public void dispatch(TaskExecution taskExecution) {
        SimpleTaskExecution sequenceTaskExecution = SimpleTaskExecution.of(taskExecution);

        sequenceTaskExecution.setStartTime(new Date());
        sequenceTaskExecution.setStatus(TaskStatus.STARTED);

        taskExecutionService.merge(sequenceTaskExecution);

        List<MapObject> subtaskDefinitions = sequenceTaskExecution.getList("tasks", MapObject.class);

        if (subtaskDefinitions.size() > 0) {
            MapObject taskDefinition = subtaskDefinitions.get(0);

            SimpleTaskExecution subTaskExecution = SimpleTaskExecution.of(taskDefinition);

            subTaskExecution.setId(UUIDGenerator.generate());
            subTaskExecution.setStatus(TaskStatus.CREATED);
            subTaskExecution.setCreateTime(new Date());
            subTaskExecution.setTaskNumber(1);
            subTaskExecution.setJobId(sequenceTaskExecution.getJobId());
            subTaskExecution.setParentId(sequenceTaskExecution.getId());
            subTaskExecution.setPriority(sequenceTaskExecution.getPriority());

            MapContext context = new MapContext(contextService.peek(sequenceTaskExecution.getId()));

            contextService.push(subTaskExecution.getId(), context);

            TaskExecution evaluatedSubTaskExecution = taskEvaluator.evaluate(subTaskExecution, context);

            taskExecutionService.create(evaluatedSubTaskExecution);
            taskDispatcher.dispatch(evaluatedSubTaskExecution);
        } else {
            SimpleTaskExecution completionTaskExecution = SimpleTaskExecution.of(taskExecution);

            completionTaskExecution.setStartTime(new Date());
            completionTaskExecution.setEndTime(new Date());
            completionTaskExecution.setExecutionTime(0);

            messageBroker.send(Queues.COMPLETIONS, completionTaskExecution);
        }
    }

    @Override
    public TaskDispatcher resolve(Task task) {
        if (task.getType().equals(Constants.SEQUENCE)) {
            return this;
        }

        return null;
    }
}
