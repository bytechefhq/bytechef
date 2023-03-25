
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

package com.bytechef.task.dispatcher.switch_;

import static com.bytechef.hermes.task.dispatcher.constant.TaskDispatcherConstants.Versions.VERSION_1;
import static com.bytechef.task.dispatcher.switch_.constant.SwitchTaskDispatcherConstants.CASES;
import static com.bytechef.task.dispatcher.switch_.constant.SwitchTaskDispatcherConstants.DEFAULT;
import static com.bytechef.task.dispatcher.switch_.constant.SwitchTaskDispatcherConstants.EXPRESSION;
import static com.bytechef.task.dispatcher.switch_.constant.SwitchTaskDispatcherConstants.KEY;
import static com.bytechef.task.dispatcher.switch_.constant.SwitchTaskDispatcherConstants.SWITCH;
import static com.bytechef.task.dispatcher.switch_.constant.SwitchTaskDispatcherConstants.TASKS;

import com.bytechef.atlas.domain.Context;
import com.bytechef.atlas.domain.TaskExecution;
import com.bytechef.atlas.message.broker.MessageBroker;
import com.bytechef.atlas.message.broker.Queues;
import com.bytechef.atlas.service.ContextService;
import com.bytechef.atlas.service.TaskExecutionService;
import com.bytechef.atlas.task.Task;
import com.bytechef.atlas.task.WorkflowTask;
import com.bytechef.atlas.task.dispatcher.TaskDispatcher;
import com.bytechef.atlas.task.dispatcher.TaskDispatcherResolver;
import com.bytechef.atlas.task.evaluator.TaskEvaluator;
import com.bytechef.atlas.task.execution.TaskStatus;
import com.bytechef.commons.util.MapValueUtils;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.util.Assert;

/**
 * @author Arik Cohen
 * @author Ivica Cardic
 * @since Jun 3, 2017
 * @see com.bytechef.task.dispatcher.switch_.completion.SwitchTaskCompletionHandler
 */
public class SwitchTaskDispatcher implements TaskDispatcher<TaskExecution>, TaskDispatcherResolver {

    private final ContextService contextService;
    private final MessageBroker messageBroker;
    private final TaskDispatcher<? super Task> taskDispatcher;
    private final TaskEvaluator taskEvaluator;
    private final TaskExecutionService taskExecutionService;

    public SwitchTaskDispatcher(
        ContextService contextService,
        MessageBroker messageBroker,
        TaskDispatcher<? super Task> taskDispatcher,
        TaskExecutionService taskExecutionService,
        TaskEvaluator taskEvaluator) {
        this.contextService = contextService;
        this.messageBroker = messageBroker;
        this.taskDispatcher = taskDispatcher;
        this.taskExecutionService = taskExecutionService;
        this.taskEvaluator = taskEvaluator;
    }

    @Override
    @SuppressFBWarnings("NP")
    public void dispatch(TaskExecution taskExecution) {
        taskExecution.setStartDate(LocalDateTime.now());
        taskExecution.setStatus(TaskStatus.STARTED);

        taskExecution = taskExecutionService.update(taskExecution);

        Map<String, Object> selectedCase = resolveCase(taskExecution);

        if (selectedCase.containsKey(TASKS)) {
            List<WorkflowTask> subWorkflowTasks = MapValueUtils.getList(selectedCase, TASKS, WorkflowTask.class,
                Collections.emptyList());

            if (subWorkflowTasks.isEmpty()) {
                taskExecution.setStartDate(LocalDateTime.now());
                taskExecution.setEndDate(LocalDateTime.now());
                taskExecution.setExecutionTime(0);

                messageBroker.send(Queues.COMPLETIONS, taskExecution);
            } else {
                WorkflowTask subWorkflowTask = subWorkflowTasks.get(0);

                TaskExecution subTaskExecution = TaskExecution.of(
                    taskExecution.getJobId(), taskExecution.getId(), taskExecution.getPriority(), 1, subWorkflowTask);

                Map<String, Object> context = contextService.peek(
                    taskExecution.getId(), Context.Classname.TASK_EXECUTION);

                subTaskExecution = taskEvaluator.evaluate(subTaskExecution, context);

                subTaskExecution = taskExecutionService.create(subTaskExecution);

                contextService.push(subTaskExecution.getId(), Context.Classname.TASK_EXECUTION, context);

                taskDispatcher.dispatch(subTaskExecution);
            }
        } else {
            taskExecution.setStartDate(LocalDateTime.now());
            taskExecution.setEndDate(LocalDateTime.now());
            taskExecution.setExecutionTime(0);
            // TODO check, it seems wrong
            taskExecution.setOutput(selectedCase.get("value"));

            messageBroker.send(Queues.COMPLETIONS, taskExecution);
        }
    }

    @Override
    public TaskDispatcher<? extends Task> resolve(Task task) {
        if (Objects.equals(task.getType(), SWITCH + "/v" + VERSION_1)) {
            return this;
        }

        return null;
    }

    private Map<String, Object> resolveCase(TaskExecution taskExecution) {
        Object expression = MapValueUtils.getRequired(taskExecution.getParameters(), EXPRESSION);
        List<Map<String, Object>> cases = MapValueUtils.getList(
            taskExecution.getParameters(), CASES, new ParameterizedTypeReference<>() {});

        Assert.notNull(cases, "you must specify 'cases' in a switch statement");

        for (Map<String, Object> oneCase : cases) {
            Object key = MapValueUtils.getRequired(oneCase, KEY);

            if (key.equals(expression)) {
                return oneCase;
            }
        }

        return MapValueUtils.getMap(taskExecution.getParameters(), DEFAULT, Collections.emptyMap());
    }
}
