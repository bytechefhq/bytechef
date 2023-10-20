
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

package com.bytechef.task.dispatcher.if_;

import static com.bytechef.hermes.task.dispatcher.constants.TaskDispatcherConstants.Versions.VERSION_1;
import static com.bytechef.task.dispatcher.if_.constants.IfTaskDispatcherConstants.CASE_FALSE;
import static com.bytechef.task.dispatcher.if_.constants.IfTaskDispatcherConstants.CASE_TRUE;
import static com.bytechef.task.dispatcher.if_.constants.IfTaskDispatcherConstants.IF;

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
import com.bytechef.commons.utils.MapUtils;
import com.bytechef.task.dispatcher.if_.util.IfTaskUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author Ivica Cardic
 * @author Matija Petanjek
 */
public class IfTaskDispatcher implements TaskDispatcher<TaskExecution>, TaskDispatcherResolver {

    private final ContextService contextService;
    private final MessageBroker messageBroker;
    private final TaskDispatcher<? super Task> taskDispatcher;
    private final TaskEvaluator taskEvaluator;
    private final TaskExecutionService taskExecutionService;

    public IfTaskDispatcher(
        ContextService contextService,
        MessageBroker messageBroker,
        TaskDispatcher<? super Task> taskDispatcher,
        TaskEvaluator taskEvaluator,
        TaskExecutionService taskExecutionService) {
        this.contextService = contextService;
        this.messageBroker = messageBroker;
        this.taskDispatcher = taskDispatcher;
        this.taskEvaluator = taskEvaluator;
        this.taskExecutionService = taskExecutionService;
    }

    @Override
    @SuppressFBWarnings("NP")
    public void dispatch(TaskExecution taskExecution) {
        taskExecutionService.updateStatus(taskExecution.getId(), TaskStatus.STARTED, LocalDateTime.now(), null);

        List<WorkflowTask> subWorkflowTasks;

        if (IfTaskUtils.resolveCase(taskExecution)) {
            subWorkflowTasks = MapUtils
                .getList(taskExecution.getParameters(), CASE_TRUE, Map.class, Collections.emptyList())
                .stream()
                .map(WorkflowTask::new)
                .toList();
        } else {
            subWorkflowTasks = MapUtils
                .getList(taskExecution.getParameters(), CASE_FALSE, Map.class, Collections.emptyList())
                .stream()
                .map(WorkflowTask::new)
                .toList();
        }

        if (subWorkflowTasks.size() > 0) {
            WorkflowTask subWorkflowTask = subWorkflowTasks.get(0);

            TaskExecution subTaskExecution = TaskExecution.of(
                taskExecution.getJobId(), taskExecution.getId(), taskExecution.getPriority(), 1, subWorkflowTask);

            subTaskExecution = taskExecutionService.create(subTaskExecution);

            Map<String, Object> context = contextService.peek(taskExecution.getId(), Context.Classname.TASK_EXECUTION);

            contextService.push(subTaskExecution.getId(), Context.Classname.TASK_EXECUTION, context);

            subTaskExecution.evaluate(taskEvaluator, context);

            taskDispatcher.dispatch(subTaskExecution);
        } else {
            taskExecution.setStartTime(LocalDateTime.now());
            taskExecution.setEndTime(LocalDateTime.now());
            taskExecution.setExecutionTime(0);

            messageBroker.send(Queues.COMPLETIONS, taskExecution);
        }
    }

    @Override
    public TaskDispatcher<? extends Task> resolve(Task task) {
        if (Objects.equals(task.getType(), IF + "/v" + VERSION_1)) {
            return this;
        }

        return null;
    }
}
