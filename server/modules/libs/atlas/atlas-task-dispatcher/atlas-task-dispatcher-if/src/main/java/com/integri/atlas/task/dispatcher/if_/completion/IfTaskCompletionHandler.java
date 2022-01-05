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

package com.integri.atlas.task.dispatcher.if_.completion;

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
import com.integri.atlas.task.dispatcher.if_.util.IfTaskUtil;
import java.util.Date;
import java.util.List;

/**
 * @author Matija Petanjek
 */
public class IfTaskCompletionHandler implements TaskCompletionHandler {

    private final TaskExecutionRepository taskExecutionRepo;
    private final TaskCompletionHandler taskCompletionHandler;
    private final TaskDispatcher taskDispatcher;
    private final ContextRepository contextRepository;
    private final TaskEvaluator taskEvaluator;
    private final IfTaskUtil ifTaskHelper;

    public IfTaskCompletionHandler(
        TaskExecutionRepository aTaskExecutionRepo,
        TaskCompletionHandler aTaskCompletionHandler,
        TaskDispatcher aTaskDispatcher,
        ContextRepository aContextRepository,
        TaskEvaluator aTaskEvaluator,
        IfTaskUtil aIfTaskHelper
    ) {
        taskExecutionRepo = aTaskExecutionRepo;
        taskCompletionHandler = aTaskCompletionHandler;
        taskDispatcher = aTaskDispatcher;
        contextRepository = aContextRepository;
        taskEvaluator = aTaskEvaluator;
        ifTaskHelper = aIfTaskHelper;
    }

    @Override
    public void handle(TaskExecution aTaskExecution) {
        SimpleTaskExecution completedTask = SimpleTaskExecution.of(aTaskExecution);
        completedTask.setStatus(TaskStatus.COMPLETED);
        taskExecutionRepo.merge(completedTask);

        SimpleTaskExecution ifTask = SimpleTaskExecution.of(taskExecutionRepo.findOne(aTaskExecution.getParentId()));

        if (aTaskExecution.getOutput() != null && aTaskExecution.getName() != null) {
            Context context = contextRepository.peek(ifTask.getId());
            MapContext newContext = new MapContext(context.asMap());
            newContext.put(aTaskExecution.getName(), aTaskExecution.getOutput());
            contextRepository.push(ifTask.getId(), newContext);
        }

        List<MapObject> tasks;

        if (ifTaskHelper.resolveCase(taskEvaluator, ifTask)) {
            tasks = ifTask.getList("caseTrue", MapObject.class);
        } else {
            tasks = ifTask.getList("caseFalse", MapObject.class);
        }

        if (aTaskExecution.getTaskNumber() < tasks.size()) {
            MapObject task = tasks.get(aTaskExecution.getTaskNumber());
            SimpleTaskExecution taskExecution = SimpleTaskExecution.of(task);
            taskExecution.setId(UUIDGenerator.generate());
            taskExecution.setStatus(TaskStatus.CREATED);
            taskExecution.setCreateTime(new Date());
            taskExecution.setTaskNumber(aTaskExecution.getTaskNumber() + 1);
            taskExecution.setJobId(ifTask.getJobId());
            taskExecution.setParentId(ifTask.getId());
            taskExecution.setPriority(ifTask.getPriority());
            MapContext context = new MapContext(contextRepository.peek(ifTask.getId()));
            contextRepository.push(taskExecution.getId(), context);
            TaskExecution evaluatedExecution = taskEvaluator.evaluate(taskExecution, context);
            taskExecutionRepo.create(evaluatedExecution);
            taskDispatcher.dispatch(evaluatedExecution);
        }
        // no more tasks to execute -- complete the If
        else {
            Context parentContext;

            // If is root level, get the job's context
            if (ifTask.getParentId() == null) {
                parentContext = contextRepository.peek(ifTask.getJobId());
            }
            // otherwise get its parent's context
            else {
                parentContext = contextRepository.peek(ifTask.getParentId());
            }

            Context thisContext = contextRepository.peek(ifTask.getId());
            MapContext newContext = new MapContext(parentContext);
            newContext.putAll(thisContext.asMap());
            contextRepository.push(aTaskExecution.getJobId(), newContext);
            ifTask.setEndTime(new Date());
            ifTask.setExecutionTime(ifTask.getEndTime().getTime() - ifTask.getStartTime().getTime());
            taskCompletionHandler.handle(ifTask);
        }
    }

    @Override
    public boolean canHandle(TaskExecution aTaskExecution) {
        String parentId = aTaskExecution.getParentId();
        if (parentId != null) {
            TaskExecution parentExecution = taskExecutionRepo.findOne(parentId);
            return parentExecution.getType().equals(DSL.IF);
        }
        return false;
    }
}
