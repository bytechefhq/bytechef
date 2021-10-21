/*
 * Copyright 2016-2018 the original author or authors.
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
 *
 * Modifications copyright (C) 2021 <your company/name>
 */

package com.integri.atlas.engine.coordinator;

import com.integri.atlas.engine.core.DSL;
import com.integri.atlas.engine.core.MapObject;
import com.integri.atlas.engine.core.context.Context;
import com.integri.atlas.engine.core.context.ContextRepository;
import com.integri.atlas.engine.core.context.MapContext;
import com.integri.atlas.engine.core.task.SimpleTaskExecution;
import com.integri.atlas.engine.core.task.TaskDispatcher;
import com.integri.atlas.engine.core.task.TaskEvaluator;
import com.integri.atlas.engine.core.task.TaskExecution;
import com.integri.atlas.engine.core.task.TaskExecutionRepository;
import com.integri.atlas.engine.core.task.TaskStatus;
import com.integri.atlas.engine.core.uuid.UUIDGenerator;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.springframework.util.Assert;

/**
 * @author Arik Cohen
 * @since Jun 3, 2017
 */
public class SwitchTaskCompletionHandler implements TaskCompletionHandler {

    private final TaskExecutionRepository taskExecutionRepo;
    private final TaskCompletionHandler taskCompletionHandler;
    private final TaskDispatcher taskDispatcher;
    private final ContextRepository contextRepository;
    private final TaskEvaluator taskEvaluator;

    public SwitchTaskCompletionHandler(
        TaskExecutionRepository aTaskExecutionRepo,
        TaskCompletionHandler aTaskCompletionHandler,
        TaskDispatcher aTaskDispatcher,
        ContextRepository aContextRepository,
        TaskEvaluator aTaskEvaluator
    ) {
        taskExecutionRepo = aTaskExecutionRepo;
        taskCompletionHandler = aTaskCompletionHandler;
        taskDispatcher = aTaskDispatcher;
        contextRepository = aContextRepository;
        taskEvaluator = aTaskEvaluator;
    }

    @Override
    public void handle(TaskExecution aTaskExecution) {
        SimpleTaskExecution mtask = SimpleTaskExecution.of(aTaskExecution);
        mtask.setStatus(TaskStatus.COMPLETED);
        taskExecutionRepo.merge(mtask);

        SimpleTaskExecution switchTask = SimpleTaskExecution.of(
            taskExecutionRepo.findOne(aTaskExecution.getParentId())
        );
        if (aTaskExecution.getOutput() != null && aTaskExecution.getName() != null) {
            Context context = contextRepository.peek(switchTask.getId());
            MapContext newContext = new MapContext(context.asMap());
            newContext.put(aTaskExecution.getName(), aTaskExecution.getOutput());
            contextRepository.push(switchTask.getId(), newContext);
        }

        List<MapObject> tasks = resolveCase(switchTask);
        if (aTaskExecution.getTaskNumber() < tasks.size()) {
            MapObject task = tasks.get(aTaskExecution.getTaskNumber());
            SimpleTaskExecution execution = SimpleTaskExecution.of(task);
            execution.setId(UUIDGenerator.generate());
            execution.setStatus(TaskStatus.CREATED);
            execution.setCreateTime(new Date());
            execution.setTaskNumber(aTaskExecution.getTaskNumber() + 1);
            execution.setJobId(switchTask.getJobId());
            execution.setParentId(switchTask.getId());
            execution.setPriority(switchTask.getPriority());
            MapContext context = new MapContext(contextRepository.peek(switchTask.getId()));
            contextRepository.push(execution.getId(), context);
            TaskExecution evaluatedExecution = taskEvaluator.evaluate(execution, context);
            taskExecutionRepo.create(evaluatedExecution);
            taskDispatcher.dispatch(evaluatedExecution);
        }
        // no more tasks to execute -- complete the switch
        else {
            // if switch is root level, update the job's context
            if (switchTask.getParentId() == null) {
                Context parentContext = contextRepository.peek(switchTask.getJobId());
                Context thisContext = contextRepository.peek(switchTask.getId());
                MapContext newContext = new MapContext(parentContext);
                newContext.putAll(thisContext.asMap());
                contextRepository.push(aTaskExecution.getJobId(), newContext);
            }
            // otherwise update the its parent's context
            else {
                Context parentContext = contextRepository.peek(switchTask.getParentId());
                Context thisContext = contextRepository.peek(switchTask.getId());
                MapContext newContext = new MapContext(parentContext);
                newContext.putAll(thisContext.asMap());
                contextRepository.push(switchTask.getParentId(), newContext);
            }
            switchTask.setEndTime(new Date());
            switchTask.setExecutionTime(switchTask.getEndTime().getTime() - switchTask.getStartTime().getTime());
            taskCompletionHandler.handle(switchTask);
        }
    }

    private List<MapObject> resolveCase(TaskExecution aSwitch) {
        Object expression = aSwitch.getRequired("expression");
        List<MapObject> cases = aSwitch.getList("cases", MapObject.class);
        Assert.notNull(cases, "you must specify 'cases' in a switch statement");
        for (MapObject oneCase : cases) {
            Object key = oneCase.getRequired("key");
            List<MapObject> tasks = oneCase.getList("tasks", MapObject.class);
            if (key.equals(expression)) {
                return tasks;
            }
        }
        return aSwitch.getList("default", MapObject.class, Collections.emptyList());
    }

    @Override
    public boolean canHandle(TaskExecution aTaskExecution) {
        String parentId = aTaskExecution.getParentId();
        if (parentId != null) {
            TaskExecution parentExecution = taskExecutionRepo.findOne(parentId);
            return parentExecution.getType().equals(DSL.SWITCH);
        }
        return false;
    }
}
