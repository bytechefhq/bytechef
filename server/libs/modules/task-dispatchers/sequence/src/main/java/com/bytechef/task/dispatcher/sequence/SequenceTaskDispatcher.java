
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

package com.bytechef.task.dispatcher.sequence;

import static com.bytechef.hermes.task.dispatcher.constants.TaskDispatcherConstants.Versions.VERSION_1;
import static com.bytechef.task.dispatcher.sequence.constants.SequenceTaskDispatcherConstants.SEQUENCE;

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
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * @author Ivica Cardic
 * @author Matija Petanjek
 */
public class SequenceTaskDispatcher implements TaskDispatcher<TaskExecution>, TaskDispatcherResolver {

    public static final String TASKS = "tasks";
    private final ContextService contextService;
    private final MessageBroker messageBroker;
    private final TaskDispatcher taskDispatcher;
    private final TaskEvaluator taskEvaluator;
    private final TaskExecutionService taskExecutionService;

    public SequenceTaskDispatcher(
        ContextService contextService,
        MessageBroker messageBroker,
        TaskDispatcher taskDispatcher,
        TaskEvaluator taskEvaluator,
        TaskExecutionService taskExecutionService) {
        this.contextService = contextService;
        this.messageBroker = messageBroker;
        this.taskDispatcher = taskDispatcher;
        this.taskEvaluator = taskEvaluator;
        this.taskExecutionService = taskExecutionService;
    }

    @Override
    public void dispatch(TaskExecution taskExecution) {
        TaskExecution sequenceTaskExecution = new TaskExecution(taskExecution);

        sequenceTaskExecution.setStartTime(LocalDateTime.now());
        sequenceTaskExecution.setStatus(TaskStatus.STARTED);

        taskExecutionService.update(sequenceTaskExecution);

        List<WorkflowTask> subWorkflowTasks = MapUtils.getList(
            sequenceTaskExecution.getParameters(), TASKS, WorkflowTask.class, Collections.emptyList());

        if (subWorkflowTasks.size() > 0) {
            WorkflowTask subWorkflowTask = subWorkflowTasks.get(0);

            TaskExecution subTaskExecution = new TaskExecution(
                subWorkflowTask,
                sequenceTaskExecution.getJobId(),
                sequenceTaskExecution.getId(),
                sequenceTaskExecution.getPriority(),
                1);

            Context context = new Context(contextService.peek(sequenceTaskExecution.getId()));

            contextService.push(subTaskExecution.getId(), context);

            TaskExecution evaluatedTaskExecution = taskEvaluator.evaluate(subTaskExecution, context);

            evaluatedTaskExecution = taskExecutionService.create(evaluatedTaskExecution);

            taskDispatcher.dispatch(evaluatedTaskExecution);
        } else {
            TaskExecution completionTaskExecution = new TaskExecution(taskExecution);

            completionTaskExecution.setStartTime(LocalDateTime.now());
            completionTaskExecution.setEndTime(LocalDateTime.now());
            completionTaskExecution.setExecutionTime(0);

            messageBroker.send(Queues.COMPLETIONS, completionTaskExecution);
        }
    }

    @Override
    public TaskDispatcher resolve(Task task) {
        if (task.getType()
            .equals(SEQUENCE + "/v" + VERSION_1)) {
            return this;
        }

        return null;
    }
}
