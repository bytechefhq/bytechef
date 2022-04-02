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

package com.integri.atlas.task.dispatcher.if_;

import com.integri.atlas.engine.core.DSL;
import com.integri.atlas.engine.core.MapObject;
import com.integri.atlas.engine.core.context.MapContext;
import com.integri.atlas.engine.core.context.repository.ContextRepository;
import com.integri.atlas.engine.core.message.broker.MessageBroker;
import com.integri.atlas.engine.core.message.broker.Queues;
import com.integri.atlas.engine.core.task.SimpleTaskExecution;
import com.integri.atlas.engine.core.task.Task;
import com.integri.atlas.engine.core.task.TaskExecution;
import com.integri.atlas.engine.core.task.TaskStatus;
import com.integri.atlas.engine.core.task.dispatcher.TaskDispatcher;
import com.integri.atlas.engine.core.task.dispatcher.TaskDispatcherResolver;
import com.integri.atlas.engine.core.task.evaluator.TaskEvaluator;
import com.integri.atlas.engine.core.task.repository.TaskExecutionRepository;
import com.integri.atlas.engine.core.uuid.UUIDGenerator;
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
    private final TaskExecutionRepository taskExecutionRepository;
    private final TaskEvaluator taskEvaluator;

    public IfTaskDispatcher(
        ContextRepository contextRepository,
        MessageBroker messageBroker,
        TaskDispatcher taskDispatcher,
        TaskExecutionRepository taskExecutionRepository,
        TaskEvaluator taskEvaluator
    ) {
        this.contextRepository = contextRepository;
        this.messageBroker = messageBroker;
        this.taskDispatcher = taskDispatcher;
        this.taskExecutionRepository = taskExecutionRepository;
        this.taskEvaluator = taskEvaluator;
    }

    @Override
    public void dispatch(TaskExecution taskExecution) {
        SimpleTaskExecution ifTaskExecution = SimpleTaskExecution.of(taskExecution);

        ifTaskExecution.setStartTime(new Date());
        ifTaskExecution.setStatus(TaskStatus.STARTED);

        taskExecutionRepository.merge(ifTaskExecution);

        List<MapObject> subtaskDefinitions;

        if (IfTaskUtil.resolveCase(taskEvaluator, ifTaskExecution)) {
            subtaskDefinitions = ifTaskExecution.getList("caseTrue", MapObject.class);
        } else {
            subtaskDefinitions = ifTaskExecution.getList("caseFalse", MapObject.class);
        }

        if (subtaskDefinitions.size() > 0) {
            MapObject taskDefinition = subtaskDefinitions.get(0);

            SimpleTaskExecution subtaskExecution = SimpleTaskExecution.of(taskDefinition);

            subtaskExecution.setId(UUIDGenerator.generate());
            subtaskExecution.setStatus(TaskStatus.CREATED);
            subtaskExecution.setCreateTime(new Date());
            subtaskExecution.setTaskNumber(1);
            subtaskExecution.setJobId(ifTaskExecution.getJobId());
            subtaskExecution.setParentId(ifTaskExecution.getId());
            subtaskExecution.setPriority(ifTaskExecution.getPriority());

            MapContext context = new MapContext(contextRepository.peek(ifTaskExecution.getId()));

            contextRepository.push(subtaskExecution.getId(), context);

            taskExecutionRepository.create(subtaskExecution);

            taskDispatcher.dispatch(subtaskExecution);
        } else {
            SimpleTaskExecution completion = SimpleTaskExecution.of(taskExecution);

            completion.setStartTime(new Date());
            completion.setEndTime(new Date());
            completion.setExecutionTime(0);

            messageBroker.send(Queues.COMPLETIONS, completion);
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
