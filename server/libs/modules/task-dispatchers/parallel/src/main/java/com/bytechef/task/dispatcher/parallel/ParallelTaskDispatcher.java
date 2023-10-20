
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

package com.bytechef.task.dispatcher.parallel;

import static com.bytechef.hermes.task.dispatcher.constants.TaskDispatcherConstants.Versions.VERSION_1;
import static com.bytechef.task.dispatcher.parallel.constants.ParallelTaskDispatcherConstants.PARALLEL;
import static com.bytechef.task.dispatcher.parallel.constants.ParallelTaskDispatcherConstants.TASKS;

import com.bytechef.atlas.domain.Context;
import com.bytechef.atlas.domain.TaskExecution;
import com.bytechef.atlas.message.broker.MessageBroker;
import com.bytechef.atlas.message.broker.Queues;
import com.bytechef.atlas.service.ContextService;
import com.bytechef.atlas.service.CounterService;
import com.bytechef.atlas.service.TaskExecutionService;
import com.bytechef.atlas.task.Task;
import com.bytechef.atlas.task.WorkflowTask;
import com.bytechef.atlas.task.dispatcher.TaskDispatcher;
import com.bytechef.atlas.task.dispatcher.TaskDispatcherResolver;
import com.bytechef.commons.utils.MapUtils;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.springframework.util.Assert;

/**
 * A {@link TaskDispatcher} implementation which implements the parallel construct. Providing a list of
 * <code>tasks</code> the dispatcher will execute these in parallel. As each task is complete it will be caught by the
 * {@link com.bytechef.task.dispatcher.parallel.completion.ParallelTaskCompletionHandler}.
 *
 * @author Arik Cohen
 * @since May 12, 2017
 */
public class ParallelTaskDispatcher implements TaskDispatcher<TaskExecution>, TaskDispatcherResolver {

    private final ContextService contextService;
    private final CounterService counterService;
    private final MessageBroker messageBroker;
    private final TaskDispatcher taskDispatcher;
    private final TaskExecutionService taskExecutionService;

    public ParallelTaskDispatcher(
        ContextService contextService,
        CounterService counterService,
        MessageBroker messageBroker,
        TaskDispatcher taskDispatcher,
        TaskExecutionService taskExecutionService) {
        this.contextService = contextService;
        this.counterService = counterService;
        this.messageBroker = messageBroker;
        this.taskDispatcher = taskDispatcher;
        this.taskExecutionService = taskExecutionService;
    }

    @Override
    public void dispatch(TaskExecution taskExecution) {
        List<WorkflowTask> workflowTasks = MapUtils
            .getList(taskExecution.getParameters(), TASKS, Map.class, Collections.emptyList())
            .stream()
            .map(WorkflowTask::new)
            .toList();

        Assert.notNull(workflowTasks, "'tasks' property can't be null");

        if (workflowTasks.size() > 0) {
            counterService.set(taskExecution.getId(), workflowTasks.size());

            for (WorkflowTask workflowTask : workflowTasks) {
                TaskExecution parallelTaskExecution = new TaskExecution(
                    workflowTask, taskExecution.getJobId(), taskExecution.getId(), taskExecution.getPriority());

                Context context = new Context(contextService.peek(taskExecution.getId()));

                TaskExecution evaluatedTaskExecution = taskExecutionService.add(parallelTaskExecution);

                contextService.push(evaluatedTaskExecution.getId(), context);
                taskDispatcher.dispatch(evaluatedTaskExecution);
            }
        } else {
            TaskExecution completionTaskExecution = new TaskExecution(taskExecution);

            completionTaskExecution.setStartTime(LocalDateTime.now());
            completionTaskExecution.setEndTime(LocalDateTime.now());
            completionTaskExecution.setExecutionTime(0);

            messageBroker.send(Queues.COMPLETIONS, completionTaskExecution);
        }
    }

    @Override
    public TaskDispatcher resolve(Task aTask) {
        if (aTask.getType()
            .equals(PARALLEL + "/v" + VERSION_1)) {
            return this;
        }
        return null;
    }
}
