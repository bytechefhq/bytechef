/*
 * Copyright 2016-2018 the original author or authors.
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
 *
 * Modifications copyright (C) 2021 <your company/name>
 */

package com.integri.atlas.task.dispatcher.switch_.completion;

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
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.springframework.util.Assert;

/**
 * @author Arik Cohen
 * @author Ivica Cardic
 * @since Jun 3, 2017
 */
public class SwitchTaskCompletionHandler implements TaskCompletionHandler {

    private final ContextRepository contextRepository;
    private final TaskExecutionRepository taskExecutionRepository;
    private final TaskCompletionHandler taskCompletionHandler;
    private final TaskDispatcher taskDispatcher;
    private final TaskEvaluator taskEvaluator;

    public SwitchTaskCompletionHandler(
        ContextRepository contextRepository,
        TaskExecutionRepository taskExecutionRepository,
        TaskCompletionHandler taskCompletionHandler,
        TaskDispatcher taskDispatcher,
        TaskEvaluator taskEvaluator
    ) {
        this.contextRepository = contextRepository;
        this.taskExecutionRepository = taskExecutionRepository;
        this.taskCompletionHandler = taskCompletionHandler;
        this.taskDispatcher = taskDispatcher;
        this.taskEvaluator = taskEvaluator;
    }

    @Override
    public boolean canHandle(TaskExecution taskExecution) {
        String parentId = taskExecution.getParentId();

        if (parentId != null) {
            TaskExecution parentExecution = taskExecutionRepository.findOne(parentId);

            return parentExecution.getType().equals(DSL.SWITCH);
        }

        return false;
    }

    @Override
    public void handle(TaskExecution taskExecution) {
        SimpleTaskExecution completedSubTaskExecution = SimpleTaskExecution.of(taskExecution);

        completedSubTaskExecution.setStatus(TaskStatus.COMPLETED);

        taskExecutionRepository.merge(completedSubTaskExecution);

        SimpleTaskExecution switchTaskExecution = SimpleTaskExecution.of(
            taskExecutionRepository.findOne(taskExecution.getParentId())
        );

        if (taskExecution.getOutput() != null && taskExecution.getName() != null) {
            Context context = contextRepository.peek(switchTaskExecution.getId());
            MapContext newContext = new MapContext(context.asMap());

            newContext.put(taskExecution.getName(), taskExecution.getOutput());

            contextRepository.push(switchTaskExecution.getId(), newContext);
        }

        List<MapObject> tasks = resolveCase(switchTaskExecution);

        if (taskExecution.getTaskNumber() < tasks.size()) {
            MapObject task = tasks.get(taskExecution.getTaskNumber());

            SimpleTaskExecution subTaskExecution = SimpleTaskExecution.of(task);

            subTaskExecution.setId(UUIDGenerator.generate());
            subTaskExecution.setStatus(TaskStatus.CREATED);
            subTaskExecution.setCreateTime(new Date());
            subTaskExecution.setTaskNumber(taskExecution.getTaskNumber() + 1);
            subTaskExecution.setJobId(switchTaskExecution.getJobId());
            subTaskExecution.setParentId(switchTaskExecution.getId());
            subTaskExecution.setPriority(switchTaskExecution.getPriority());

            MapContext context = new MapContext(contextRepository.peek(switchTaskExecution.getId()));

            contextRepository.push(subTaskExecution.getId(), context);

            TaskExecution evaluatedSubTaskExecution = taskEvaluator.evaluate(subTaskExecution, context);

            taskExecutionRepository.create(evaluatedSubTaskExecution);
            taskDispatcher.dispatch(evaluatedSubTaskExecution);
        }
        // no more tasks to execute -- complete the switch
        else {
            switchTaskExecution.setEndTime(new Date());
            switchTaskExecution.setExecutionTime(
                switchTaskExecution.getEndTime().getTime() - switchTaskExecution.getStartTime().getTime()
            );

            taskCompletionHandler.handle(switchTaskExecution);
        }
    }

    private List<MapObject> resolveCase(TaskExecution taskExecution) {
        Object expression = taskExecution.getRequired("expression");
        List<MapObject> cases = taskExecution.getList("cases", MapObject.class);

        Assert.notNull(cases, "you must specify 'cases' in a switch statement");

        for (MapObject oneCase : cases) {
            Object key = oneCase.getRequired("key");
            List<MapObject> tasks = oneCase.getList("tasks", MapObject.class);

            if (key.equals(expression)) {
                return tasks;
            }
        }

        return taskExecution.getList("default", MapObject.class, Collections.emptyList());
    }
}
