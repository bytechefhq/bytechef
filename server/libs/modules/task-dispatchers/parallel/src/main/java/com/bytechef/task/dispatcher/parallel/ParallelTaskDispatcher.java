
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

import static com.bytechef.task.dispatcher.parallel.constants.ParallelTaskDispatcherConstants.PARALLEL;
import static com.bytechef.task.dispatcher.parallel.constants.ParallelTaskDispatcherConstants.TASKS;

import com.bytechef.atlas.execution.domain.Context;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.coordinator.event.TaskExecutionCompleteEvent;
import com.bytechef.atlas.file.storage.facade.WorkflowFileStorageFacade;
import com.bytechef.atlas.execution.service.RemoteContextService;
import com.bytechef.atlas.execution.service.RemoteCounterService;
import com.bytechef.atlas.execution.service.RemoteTaskExecutionService;
import com.bytechef.atlas.configuration.task.Task;
import com.bytechef.atlas.configuration.task.WorkflowTask;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcher;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcherResolver;
import com.bytechef.commons.util.MapUtils;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.context.ApplicationEventPublisher;
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

    private final ApplicationEventPublisher eventPublisher;
    private final RemoteContextService contextService;
    private final RemoteCounterService counterService;
    private final TaskDispatcher<? super Task> taskDispatcher;
    private final RemoteTaskExecutionService taskExecutionService;
    private final WorkflowFileStorageFacade workflowFileStorageFacade;

    @SuppressFBWarnings("EI")
    public ParallelTaskDispatcher(
        ApplicationEventPublisher eventPublisher, RemoteContextService contextService,
        RemoteCounterService counterService, TaskDispatcher<? super Task> taskDispatcher,
        RemoteTaskExecutionService taskExecutionService, WorkflowFileStorageFacade workflowFileStorageFacade) {

        this.eventPublisher = eventPublisher;
        this.contextService = contextService;
        this.counterService = counterService;
        this.taskDispatcher = taskDispatcher;
        this.taskExecutionService = taskExecutionService;
        this.workflowFileStorageFacade = workflowFileStorageFacade;
    }

    @Override
    @SuppressFBWarnings("NP")
    public void dispatch(TaskExecution taskExecution) {
        List<WorkflowTask> workflowTasks = MapUtils.getList(
            taskExecution.getParameters(), TASKS, WorkflowTask.class, Collections.emptyList());

        Assert.notNull(workflowTasks, "'tasks' property can't be null");

        if (workflowTasks.isEmpty()) {
            taskExecution.setStartDate(LocalDateTime.now());
            taskExecution.setEndDate(LocalDateTime.now());
            taskExecution.setExecutionTime(0);

            eventPublisher.publishEvent(new TaskExecutionCompleteEvent(taskExecution));
        } else {
            counterService.set(taskExecution.getId(), workflowTasks.size());

            for (WorkflowTask workflowTask : workflowTasks) {
                TaskExecution parallelTaskExecution = TaskExecution.builder()
                    .jobId(taskExecution.getJobId())
                    .parentId(taskExecution.getId())
                    .priority(taskExecution.getPriority())
                    .workflowTask(workflowTask)
                    .build();

                parallelTaskExecution = taskExecutionService.create(parallelTaskExecution);

                Map<String, ?> context = workflowFileStorageFacade.readContextValue(
                    contextService.peek(
                        taskExecution.getId(), Context.Classname.TASK_EXECUTION));

                contextService.push(
                    Objects.requireNonNull(parallelTaskExecution.getId()), Context.Classname.TASK_EXECUTION,
                    workflowFileStorageFacade.storeContextValue(
                        parallelTaskExecution.getId(), Context.Classname.TASK_EXECUTION, context));
                taskDispatcher.dispatch(parallelTaskExecution);
            }
        }
    }

    @Override
    public TaskDispatcher<? extends Task> resolve(Task task) {
        if (Objects.equals(task.getType(), PARALLEL + "/v1")) {
            return this;
        }
        return null;
    }
}
