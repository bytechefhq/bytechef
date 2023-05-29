
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

package com.bytechef.task.dispatcher.branch;

import static com.bytechef.task.dispatcher.branch.constant.BranchTaskDispatcherConstants.CASES;
import static com.bytechef.task.dispatcher.branch.constant.BranchTaskDispatcherConstants.DEFAULT;
import static com.bytechef.task.dispatcher.branch.constant.BranchTaskDispatcherConstants.EXPRESSION;
import static com.bytechef.task.dispatcher.branch.constant.BranchTaskDispatcherConstants.KEY;
import static com.bytechef.task.dispatcher.branch.constant.BranchTaskDispatcherConstants.BRANCH;
import static com.bytechef.task.dispatcher.branch.constant.BranchTaskDispatcherConstants.TASKS;

import com.bytechef.atlas.domain.Context;
import com.bytechef.atlas.domain.Context.Classname;
import com.bytechef.atlas.domain.TaskExecution;
import com.bytechef.atlas.message.broker.TaskMessageRoute;
import com.bytechef.message.broker.MessageBroker;
import com.bytechef.atlas.service.ContextService;
import com.bytechef.atlas.service.TaskExecutionService;
import com.bytechef.atlas.task.Task;
import com.bytechef.atlas.task.WorkflowTask;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcher;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcherResolver;
import com.bytechef.commons.util.MapValueUtils;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.util.Assert;

/**
 * @author Arik Cohen
 * @author Ivica Cardic
 * @since Jun 3, 2017
 * @see BranchTaskCompletionHandler
 */
public class BranchTaskDispatcher implements TaskDispatcher<TaskExecution>, TaskDispatcherResolver {

    private final ContextService contextService;
    private final MessageBroker messageBroker;
    private final TaskDispatcher<? super Task> taskDispatcher;
    private final TaskExecutionService taskExecutionService;

    @SuppressFBWarnings("EI")
    public BranchTaskDispatcher(
        ContextService contextService, MessageBroker messageBroker, TaskDispatcher<? super Task> taskDispatcher,
        TaskExecutionService taskExecutionService) {

        this.contextService = contextService;
        this.messageBroker = messageBroker;
        this.taskDispatcher = taskDispatcher;
        this.taskExecutionService = taskExecutionService;
    }

    @Override
    @SuppressFBWarnings("NP")
    public void dispatch(TaskExecution taskExecution) {
        taskExecution.setStartDate(LocalDateTime.now());
        taskExecution.setStatus(TaskExecution.Status.STARTED);

        taskExecution = taskExecutionService.update(taskExecution);

        Map<String, ?> selectedCase = resolveCase(taskExecution);

        if (selectedCase.containsKey(TASKS)) {
            List<WorkflowTask> subWorkflowTasks = MapValueUtils.getList(
                selectedCase, TASKS, WorkflowTask.class, Collections.emptyList());

            if (subWorkflowTasks.isEmpty()) {
                taskExecution.setStartDate(LocalDateTime.now());
                taskExecution.setEndDate(LocalDateTime.now());
                taskExecution.setExecutionTime(0);

                messageBroker.send(TaskMessageRoute.TASKS_COMPLETIONS, taskExecution);
            } else {
                WorkflowTask subWorkflowTask = subWorkflowTasks.get(0);

                TaskExecution subTaskExecution = TaskExecution.builder()
                    .jobId(taskExecution.getJobId())
                    .parentId(taskExecution.getId())
                    .priority(taskExecution.getPriority())
                    .taskNumber(1)
                    .workflowTask(subWorkflowTask)
                    .build();

                Map<String, Object> context = contextService.peek(
                    Objects.requireNonNull(taskExecution.getId()), Classname.TASK_EXECUTION);

                subTaskExecution.evaluate(context);

                subTaskExecution = taskExecutionService.create(subTaskExecution);

                contextService.push(
                    Objects.requireNonNull(subTaskExecution.getId()), Classname.TASK_EXECUTION, context);

                taskDispatcher.dispatch(subTaskExecution);
            }
        } else {
            taskExecution.setStartDate(LocalDateTime.now());
            taskExecution.setEndDate(LocalDateTime.now());
            taskExecution.setExecutionTime(0);
            // TODO check, it seems wrong
            taskExecution.setOutput(selectedCase.get("value"));

            messageBroker.send(TaskMessageRoute.TASKS_COMPLETIONS, taskExecution);
        }
    }

    @Override
    public TaskDispatcher<? extends Task> resolve(Task task) {
        if (Objects.equals(task.getType(), BRANCH + "/v1")) {
            return this;
        }

        return null;
    }

    @SuppressWarnings({
        "rawtypes", "unchecked"
    })
    private Map<String, ?> resolveCase(TaskExecution taskExecution) {
        Object expression = MapValueUtils.getRequired(taskExecution.getParameters(), EXPRESSION);
        List<Map<String, Object>> cases = (List) MapValueUtils.getList(taskExecution.getParameters(), CASES, Map.class);

        Assert.notNull(cases, "you must specify 'cases' in a branch statement");

        for (Map<String, Object> oneCase : cases) {
            Object key = MapValueUtils.getRequired(oneCase, KEY);

            if (key.equals(expression)) {
                return oneCase;
            }
        }

        return MapValueUtils.getMap(taskExecution.getParameters(), DEFAULT, Collections.emptyMap());
    }
}
