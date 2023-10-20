
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

import static com.bytechef.hermes.task.dispatcher.constant.TaskDispatcherConstants.Versions.VERSION_1;
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
    private final TaskDispatcher<? super Task> taskDispatcher;
    private final TaskExecutionService taskExecutionService;

    public ParallelTaskDispatcher(
        ContextService contextService,
        CounterService counterService,
        MessageBroker messageBroker,
        TaskDispatcher<? super Task> taskDispatcher,
        TaskExecutionService taskExecutionService) {
        this.contextService = contextService;
        this.counterService = counterService;
        this.messageBroker = messageBroker;
        this.taskDispatcher = taskDispatcher;
        this.taskExecutionService = taskExecutionService;
    }

    @Override
    @SuppressFBWarnings("NP")
    public void dispatch(TaskExecution taskExecution) {
        List<WorkflowTask> workflowTasks = MapValueUtils
            .getList(taskExecution.getParameters(), TASKS, Map.class, Collections.emptyList())
            .stream()
            .map(WorkflowTask::of)
            .toList();

        Assert.notNull(workflowTasks, "'tasks' property can't be null");

        if (workflowTasks.isEmpty()) {
            taskExecution.setStartDate(LocalDateTime.now());
            taskExecution.setEndDate(LocalDateTime.now());
            taskExecution.setExecutionTime(0);

            messageBroker.send(Queues.COMPLETIONS, taskExecution);
        } else {
            counterService.set(taskExecution.getId(), workflowTasks.size());

            for (WorkflowTask workflowTask : workflowTasks) {
                TaskExecution parallelTaskExecution = TaskExecution.of(
                    taskExecution.getJobId(), taskExecution.getId(), taskExecution.getPriority(), workflowTask);

                parallelTaskExecution = taskExecutionService.create(parallelTaskExecution);

                Map<String, Object> context = contextService.peek(
                    taskExecution.getId(), Context.Classname.TASK_EXECUTION);

                contextService.push(parallelTaskExecution.getId(), Context.Classname.TASK_EXECUTION, context);
                taskDispatcher.dispatch(parallelTaskExecution);
            }
        }
    }

    @Override
    public TaskDispatcher<? extends Task> resolve(Task task) {
        if (Objects.equals(task.getType(), PARALLEL + "/v" + VERSION_1)) {
            return this;
        }
        return null;
    }
}
