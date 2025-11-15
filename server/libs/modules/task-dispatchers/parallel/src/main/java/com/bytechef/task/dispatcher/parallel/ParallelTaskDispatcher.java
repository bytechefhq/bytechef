/*
 * Copyright 2016-2020 the original author or authors.
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
 * Modifications copyright (C) 2025 ByteChef
 */

package com.bytechef.task.dispatcher.parallel;

import static com.bytechef.task.dispatcher.parallel.constants.ParallelTaskDispatcherConstants.PARALLEL;
import static com.bytechef.task.dispatcher.parallel.constants.ParallelTaskDispatcherConstants.TASKS;

import com.bytechef.atlas.configuration.domain.Task;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.coordinator.event.TaskExecutionCompleteEvent;
import com.bytechef.atlas.coordinator.event.TaskExecutionErrorEvent;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcher;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcherResolver;
import com.bytechef.atlas.execution.domain.Context;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.CounterService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.error.ExecutionError;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.context.ApplicationEventPublisher;

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
    private final ApplicationEventPublisher eventPublisher;
    private final TaskDispatcher<? super Task> taskDispatcher;
    private final TaskExecutionService taskExecutionService;
    private final TaskFileStorage taskFileStorage;

    @SuppressFBWarnings("EI")
    public ParallelTaskDispatcher(
        ContextService contextService, CounterService counterService, ApplicationEventPublisher eventPublisher,
        TaskDispatcher<? super Task> taskDispatcher, TaskExecutionService taskExecutionService,
        TaskFileStorage taskFileStorage) {

        this.contextService = contextService;
        this.counterService = counterService;
        this.eventPublisher = eventPublisher;
        this.taskDispatcher = taskDispatcher;
        this.taskExecutionService = taskExecutionService;
        this.taskFileStorage = taskFileStorage;
    }

    @Override
    public void dispatch(TaskExecution taskExecution) {
        List<WorkflowTask> workflowTasks = Validate.notNull(
            MapUtils.getList(taskExecution.getParameters(), TASKS, WorkflowTask.class, Collections.emptyList()),
            "'workflowTasks' property must not be null");

        if (workflowTasks.isEmpty()) {
            taskExecution.setStartDate(Instant.now());
            taskExecution.setEndDate(Instant.now());
            taskExecution.setExecutionTime(0);

            eventPublisher.publishEvent(new TaskExecutionCompleteEvent(taskExecution));
        } else {
            try {
                counterService.set(Validate.notNull(taskExecution.getId(), "id"), workflowTasks.size());

                for (WorkflowTask workflowTask : workflowTasks) {
                    TaskExecution parallelTaskExecution = TaskExecution.builder()
                        .jobId(taskExecution.getJobId())
                        .parentId(taskExecution.getId())
                        .priority(taskExecution.getPriority())
                        .workflowTask(workflowTask)
                        .build();

                    parallelTaskExecution = taskExecutionService.create(parallelTaskExecution);

                    Map<String, ?> context = taskFileStorage.readContextValue(
                        contextService.peek(
                            Validate.notNull(taskExecution.getId(), "id"), Context.Classname.TASK_EXECUTION));

                    contextService.push(
                        Validate.notNull(parallelTaskExecution.getId(), "id"), Context.Classname.TASK_EXECUTION,
                        taskFileStorage.storeContextValue(
                            Validate.notNull(parallelTaskExecution.getId(), "id"), Context.Classname.TASK_EXECUTION,
                            context));
                    taskDispatcher.dispatch(parallelTaskExecution);
                }
            } catch (Exception e) {
                taskExecution
                    .setError(new ExecutionError(e.getMessage(), Arrays.asList(ExceptionUtils.getStackFrames(e))));

                eventPublisher.publishEvent(new TaskExecutionErrorEvent(taskExecution));
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
