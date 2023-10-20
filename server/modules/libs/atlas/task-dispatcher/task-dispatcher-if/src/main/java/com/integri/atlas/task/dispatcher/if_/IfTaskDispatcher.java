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

package com.integri.atlas.task.dispatcher.if_;

import static com.integri.atlas.task.dispatcher.if_.IfTaskConstants.PROPERTY_CASE_FALSE;
import static com.integri.atlas.task.dispatcher.if_.IfTaskConstants.PROPERTY_CASE_TRUE;

import com.integri.atlas.engine.DSL;
import com.integri.atlas.engine.MapObject;
import com.integri.atlas.engine.context.MapContext;
import com.integri.atlas.engine.context.repository.ContextRepository;
import com.integri.atlas.engine.message.broker.MessageBroker;
import com.integri.atlas.engine.message.broker.Queues;
import com.integri.atlas.engine.task.Task;
import com.integri.atlas.engine.task.dispatcher.TaskDispatcher;
import com.integri.atlas.engine.task.dispatcher.TaskDispatcherResolver;
import com.integri.atlas.engine.task.execution.SimpleTaskExecution;
import com.integri.atlas.engine.task.execution.TaskExecution;
import com.integri.atlas.engine.task.execution.TaskStatus;
import com.integri.atlas.engine.task.execution.evaluator.TaskEvaluator;
import com.integri.atlas.engine.task.execution.repository.TaskExecutionRepository;
import com.integri.atlas.engine.uuid.UUIDGenerator;
import com.integri.atlas.task.dispatcher.if_.util.IfTaskUtil;
import java.util.Date;
import java.util.List;

/**
 * @author Ivica Cardic
 * @author Matija Petanjek
 */
public class IfTaskDispatcher implements TaskDispatcher<TaskExecution>, TaskDispatcherResolver {

    private final ContextRepository contextRepository;
    private final MessageBroker messageBroker;
    private final TaskDispatcher taskDispatcher;
    private final TaskEvaluator taskEvaluator;
    private final TaskExecutionRepository taskExecutionRepository;

    public IfTaskDispatcher(
        ContextRepository contextRepository,
        MessageBroker messageBroker,
        TaskDispatcher taskDispatcher,
        TaskEvaluator taskEvaluator,
        TaskExecutionRepository taskExecutionRepository
    ) {
        this.contextRepository = contextRepository;
        this.messageBroker = messageBroker;
        this.taskDispatcher = taskDispatcher;
        this.taskEvaluator = taskEvaluator;
        this.taskExecutionRepository = taskExecutionRepository;
    }

    @Override
    public void dispatch(TaskExecution taskExecution) {
        SimpleTaskExecution ifTaskExecution = SimpleTaskExecution.of(taskExecution);

        ifTaskExecution.setStartTime(new Date());
        ifTaskExecution.setStatus(TaskStatus.STARTED);

        taskExecutionRepository.merge(ifTaskExecution);

        List<MapObject> subtaskDefinitions;

        if (IfTaskUtil.resolveCase(ifTaskExecution)) {
            subtaskDefinitions = ifTaskExecution.getList(PROPERTY_CASE_TRUE, MapObject.class);
        } else {
            subtaskDefinitions = ifTaskExecution.getList(PROPERTY_CASE_FALSE, MapObject.class);
        }

        if (subtaskDefinitions.size() > 0) {
            MapObject taskDefinition = subtaskDefinitions.get(0);

            SimpleTaskExecution subTaskExecution = SimpleTaskExecution.of(taskDefinition);

            subTaskExecution.setCreateTime(new Date());
            subTaskExecution.setId(UUIDGenerator.generate());
            subTaskExecution.setJobId(ifTaskExecution.getJobId());
            subTaskExecution.setParentId(ifTaskExecution.getId());
            subTaskExecution.setPriority(ifTaskExecution.getPriority());
            subTaskExecution.setStatus(TaskStatus.CREATED);
            subTaskExecution.setTaskNumber(1);

            MapContext context = new MapContext(contextRepository.peek(ifTaskExecution.getId()));

            contextRepository.push(subTaskExecution.getId(), context);

            TaskExecution evaluatedSubTaskExecution = taskEvaluator.evaluate(subTaskExecution, context);

            taskExecutionRepository.create(evaluatedSubTaskExecution);
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
        if (task.getType().equals(DSL.IF)) {
            return this;
        }

        return null;
    }
}
