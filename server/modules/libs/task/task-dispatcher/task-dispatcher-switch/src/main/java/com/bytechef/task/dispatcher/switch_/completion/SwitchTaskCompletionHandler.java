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

package com.bytechef.task.dispatcher.switch_.completion;

import com.bytechef.atlas.Constants;
import com.bytechef.atlas.MapObject;
import com.bytechef.atlas.context.domain.Context;
import com.bytechef.atlas.context.domain.MapContext;
import com.bytechef.atlas.coordinator.task.completion.TaskCompletionHandler;
import com.bytechef.atlas.service.context.ContextService;
import com.bytechef.atlas.service.task.execution.TaskExecutionService;
import com.bytechef.atlas.task.dispatcher.TaskDispatcher;
import com.bytechef.atlas.task.execution.TaskStatus;
import com.bytechef.atlas.task.execution.domain.SimpleTaskExecution;
import com.bytechef.atlas.task.execution.domain.TaskExecution;
import com.bytechef.atlas.task.execution.evaluator.TaskEvaluator;
import com.bytechef.atlas.uuid.UUIDGenerator;
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

    private final ContextService contextService;
    private final TaskExecutionService taskExecutionService;
    private final TaskCompletionHandler taskCompletionHandler;
    private final TaskDispatcher taskDispatcher;
    private final TaskEvaluator taskEvaluator;

    public SwitchTaskCompletionHandler(
            ContextService contextService,
            TaskExecutionService taskExecutionService,
            TaskCompletionHandler taskCompletionHandler,
            TaskDispatcher taskDispatcher,
            TaskEvaluator taskEvaluator) {
        this.contextService = contextService;
        this.taskExecutionService = taskExecutionService;
        this.taskCompletionHandler = taskCompletionHandler;
        this.taskDispatcher = taskDispatcher;
        this.taskEvaluator = taskEvaluator;
    }

    @Override
    public boolean canHandle(TaskExecution taskExecution) {
        String parentId = taskExecution.getParentId();

        if (parentId != null) {
            TaskExecution parentExecution = taskExecutionService.getTaskExecution(parentId);

            return parentExecution.getType().equals(Constants.SWITCH);
        }

        return false;
    }

    @Override
    public void handle(TaskExecution taskExecution) {
        SimpleTaskExecution completedSubTaskExecution = SimpleTaskExecution.of(taskExecution);

        completedSubTaskExecution.setStatus(TaskStatus.COMPLETED);

        taskExecutionService.merge(completedSubTaskExecution);

        SimpleTaskExecution switchTaskExecution =
                SimpleTaskExecution.of(taskExecutionService.getTaskExecution(taskExecution.getParentId()));

        if (taskExecution.getOutput() != null && taskExecution.getName() != null) {
            Context context = contextService.peek(switchTaskExecution.getId());
            MapContext newContext = new MapContext(context.asMap());

            newContext.put(taskExecution.getName(), taskExecution.getOutput());

            contextService.push(switchTaskExecution.getId(), newContext);
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

            MapContext context = new MapContext(contextService.peek(switchTaskExecution.getId()));

            contextService.push(subTaskExecution.getId(), context);

            TaskExecution evaluatedSubTaskExecution = taskEvaluator.evaluate(subTaskExecution, context);

            taskExecutionService.create(evaluatedSubTaskExecution);
            taskDispatcher.dispatch(evaluatedSubTaskExecution);
        }
        // no more tasks to execute -- complete the switch
        else {
            switchTaskExecution.setEndTime(new Date());
            switchTaskExecution.setExecutionTime(
                    switchTaskExecution.getEndTime().getTime()
                            - switchTaskExecution.getStartTime().getTime());

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
