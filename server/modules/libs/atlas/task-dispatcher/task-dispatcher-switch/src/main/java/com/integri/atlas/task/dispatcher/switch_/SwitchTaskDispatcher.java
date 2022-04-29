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

package com.integri.atlas.task.dispatcher.switch_;

import com.integri.atlas.engine.core.Accessor;
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
import com.integri.atlas.task.dispatcher.switch_.completion.SwitchTaskCompletionHandler;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.springframework.util.Assert;

/**
 * @author Arik Cohen
 * @author Ivica Cardic
 * @since Jun 3, 2017
 * @see SwitchTaskCompletionHandler
 */
public class SwitchTaskDispatcher implements TaskDispatcher<TaskExecution>, TaskDispatcherResolver {

    private final ContextRepository contextRepository;
    private final MessageBroker messageBroker;
    private final TaskDispatcher taskDispatcher;
    private final TaskEvaluator taskEvaluator;
    private final TaskExecutionRepository taskExecutionRepository;

    public SwitchTaskDispatcher(
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
        SimpleTaskExecution switchTaskExecution = SimpleTaskExecution.of(taskExecution);

        switchTaskExecution.setStartTime(new Date());
        switchTaskExecution.setStatus(TaskStatus.STARTED);

        taskExecutionRepository.merge(switchTaskExecution);

        Accessor selectedCase = resolveCase(taskExecution);

        if (selectedCase.containsKey("tasks")) {
            List<MapObject> tasks = selectedCase.getList("tasks", MapObject.class, Collections.emptyList());

            if (tasks.size() > 0) {
                MapObject taskDefinition = tasks.get(0);

                SimpleTaskExecution subTaskExecution = SimpleTaskExecution.of(taskDefinition);

                subTaskExecution.setId(UUIDGenerator.generate());
                subTaskExecution.setStatus(TaskStatus.CREATED);
                subTaskExecution.setCreateTime(new Date());
                subTaskExecution.setTaskNumber(1);
                subTaskExecution.setJobId(switchTaskExecution.getJobId());
                subTaskExecution.setParentId(switchTaskExecution.getId());
                subTaskExecution.setPriority(switchTaskExecution.getPriority());

                MapContext context = new MapContext(contextRepository.peek(switchTaskExecution.getId()));

                contextRepository.push(subTaskExecution.getId(), context);

                TaskExecution evaluatedExecution = taskEvaluator.evaluate(subTaskExecution, context);

                taskExecutionRepository.create(evaluatedExecution);
                taskDispatcher.dispatch(evaluatedExecution);
            } else {
                SimpleTaskExecution completionTaskExecution = SimpleTaskExecution.of(taskExecution);

                completionTaskExecution.setStartTime(new Date());
                completionTaskExecution.setEndTime(new Date());
                completionTaskExecution.setExecutionTime(0);

                messageBroker.send(Queues.COMPLETIONS, completionTaskExecution);
            }
        } else {
            SimpleTaskExecution completionTaskExecution = SimpleTaskExecution.of(taskExecution);

            completionTaskExecution.setStartTime(new Date());
            completionTaskExecution.setEndTime(new Date());
            completionTaskExecution.setExecutionTime(0);
            completionTaskExecution.setOutput(selectedCase.get("value"));

            messageBroker.send(Queues.COMPLETIONS, completionTaskExecution);
        }
    }

    @Override
    public TaskDispatcher resolve(Task task) {
        if (task.getType().equals(DSL.SWITCH)) {
            return this;
        }
        return null;
    }

    private Accessor resolveCase(TaskExecution taskExecution) {
        Object expression = taskExecution.getRequired("expression");
        List<MapObject> cases = taskExecution.getList("cases", MapObject.class);

        Assert.notNull(cases, "you must specify 'cases' in a switch statement");

        for (MapObject oneCase : cases) {
            Object key = oneCase.getRequired("key");
            if (key.equals(expression)) {
                return oneCase;
            }
        }

        return new MapObject(taskExecution.getMap("default", Collections.emptyMap()));
    }
}
