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
    private final TaskExecutionRepository taskExecutionRepo;
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
        taskExecutionRepo = taskExecutionRepository;
        this.taskEvaluator = taskEvaluator;
    }

    @Override
    public void dispatch(TaskExecution aTask) {
        SimpleTaskExecution ifTask = SimpleTaskExecution.of(aTask);
        ifTask.setStartTime(new Date());
        ifTask.setStatus(TaskStatus.STARTED);
        taskExecutionRepo.merge(ifTask);

        List<MapObject> tasks;

        if (IfTaskUtil.resolveCase(taskEvaluator, ifTask)) {
            tasks = ifTask.getList("caseTrue", MapObject.class);
        } else {
            tasks = ifTask.getList("caseFalse", MapObject.class);
        }

        if (tasks.size() > 0) {
            MapObject task = tasks.get(0);
            SimpleTaskExecution execution = SimpleTaskExecution.of(task);
            execution.setId(UUIDGenerator.generate());
            execution.setStatus(TaskStatus.CREATED);
            execution.setCreateTime(new Date());
            execution.setTaskNumber(1);
            execution.setJobId(ifTask.getJobId());
            execution.setParentId(ifTask.getId());
            execution.setPriority(ifTask.getPriority());
            MapContext context = new MapContext(contextRepository.peek(ifTask.getId()));
            contextRepository.push(execution.getId(), context);
            taskExecutionRepo.create(execution);
            taskDispatcher.dispatch(execution);
        } else {
            SimpleTaskExecution completion = SimpleTaskExecution.of(aTask);
            completion.setStartTime(new Date());
            completion.setEndTime(new Date());
            completion.setExecutionTime(0);
            messageBroker.send(Queues.COMPLETIONS, completion);
        }
    }

    @Override
    public TaskDispatcher resolve(Task aTask) {
        if (aTask.getType().equals(DSL.IF)) {
            return this;
        }
        return null;
    }
}
