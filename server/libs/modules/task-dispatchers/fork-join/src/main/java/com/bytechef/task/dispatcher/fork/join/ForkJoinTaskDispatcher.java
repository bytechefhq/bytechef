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

package com.bytechef.task.dispatcher.fork.join;

import com.bytechef.atlas.configuration.constant.WorkflowConstants;
import com.bytechef.atlas.configuration.domain.Task;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.coordinator.event.TaskExecutionCompleteEvent;
import com.bytechef.atlas.coordinator.event.TaskExecutionErrorEvent;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcher;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcherResolver;
import com.bytechef.atlas.execution.domain.Context.Classname;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.CounterService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.error.ExecutionError;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.task.dispatcher.fork.join.constant.ForkJoinTaskDispatcherConstants;
import com.fasterxml.jackson.core.type.TypeReference;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Implements a Fork/Join construct.
 *
 * <p>
 * Fork/Join tasks are expected to have a "branches" property which contains a list of task list.
 * </p>
 *
 * <p>
 * Each branch executes in isolation, in parallel to the other branches in the fork and has its own context namespace.
 * </p>
 *
 * <pre>
 *   - type: fork
 *     branches:
 *       - - name: randomNumber
 *       label: Generate a random number
 *       type: randomInt
 *       startInclusive: 0
 *       endInclusive: 5000
 *
 *     - type: sleep
 *       millis: ${randomNumber}
 *
 *       - - name: randomNumber
 *       label: Generate a random number
 *       type: randomInt
 *       startInclusive: 0
 *       endInclusive: 5000
 *
 *     - type: sleep
 *       millis: ${randomNumber}
 * </pre>
 *
 * @author Arik Cohen
 * @author Ivica Cardic
 * @since May 11, 2017
 * @see ForkJoinTaskCompletionHandler
 */
public class ForkJoinTaskDispatcher implements TaskDispatcher<TaskExecution>, TaskDispatcherResolver {

    private final ContextService contextService;
    private final CounterService counterService;
    private final Evaluator evaluator;
    private final ApplicationEventPublisher eventPublisher;
    private final TaskDispatcher<? super Task> taskDispatcher;
    private final TaskExecutionService taskExecutionService;
    private final TaskFileStorage taskFileStorage;

    @SuppressFBWarnings("EI")
    public ForkJoinTaskDispatcher(
        ContextService contextService, CounterService counterService, Evaluator evaluator,
        ApplicationEventPublisher eventPublisher, TaskDispatcher<? super Task> taskDispatcher,
        TaskExecutionService taskExecutionService, TaskFileStorage taskFileStorage) {

        this.contextService = contextService;
        this.counterService = counterService;
        this.evaluator = evaluator;
        this.eventPublisher = eventPublisher;
        this.taskDispatcher = taskDispatcher;
        this.taskExecutionService = taskExecutionService;
        this.taskFileStorage = taskFileStorage;
    }

    @Override
    public void dispatch(TaskExecution taskExecution) {
        List<List<WorkflowTask>> branchesWorkflowTasks = MapUtils.getRequiredList(
            taskExecution.getParameters(), ForkJoinTaskDispatcherConstants.BRANCHES, new TypeReference<>() {});

        taskExecution.setStartDate(Instant.now());
        taskExecution.setStatus(TaskExecution.Status.STARTED);

        taskExecution = taskExecutionService.update(taskExecution);

        try {
            if (branchesWorkflowTasks.isEmpty()) {
                taskExecution.setStartDate(Instant.now());
                taskExecution.setEndDate(Instant.now());
                taskExecution.setExecutionTime(0);

                eventPublisher.publishEvent(new TaskExecutionCompleteEvent(taskExecution));
            } else {
                long taskExecutionId = Validate.notNull(taskExecution.getId(), "id");

                counterService.set(taskExecutionId, branchesWorkflowTasks.size());

                long taskExecutionJobId = Validate.notNull(
                    taskExecution.getJobId(), "'taskExecution.jobId' must not be null");

                for (int i = 0; i < branchesWorkflowTasks.size(); i++) {
                    List<WorkflowTask> branchWorkflowTasks = branchesWorkflowTasks.get(i);

                    Validate.isTrue(!branchWorkflowTasks.isEmpty(), "branch " + i + " does not contain any tasks");

                    WorkflowTask branchWorkflowTask = branchWorkflowTasks.getFirst();

                    TaskExecution branchTaskExecution = TaskExecution.builder()
                        .jobId(taskExecutionJobId)
                        .parentId(taskExecution.getId())
                        .priority(taskExecution.getPriority())
                        .taskNumber(1)
                        .workflowTask(
                            new WorkflowTask(
                                MapUtils.append(
                                    branchWorkflowTask.toMap(), WorkflowConstants.PARAMETERS,
                                    Map.of(ForkJoinTaskDispatcherConstants.BRANCH, i))))
                        .build();

                    Map<String, ?> context = taskFileStorage.readContextValue(
                        contextService.peek(taskExecutionId, Classname.TASK_EXECUTION));

                    branchTaskExecution.evaluate(context, evaluator);

                    branchTaskExecution = taskExecutionService.create(branchTaskExecution);

                    long branchTaskExecutionId = Validate.notNull(branchTaskExecution.getId(), "id");

                    contextService.push(
                        branchTaskExecutionId, Classname.TASK_EXECUTION,
                        taskFileStorage.storeContextValue(branchTaskExecutionId, Classname.TASK_EXECUTION, context));

                    contextService.push(
                        taskExecutionId, i, Classname.TASK_EXECUTION,
                        taskFileStorage.storeContextValue(taskExecutionId, i, Classname.TASK_EXECUTION, context));
                    taskDispatcher.dispatch(branchTaskExecution);
                }
            }
        } catch (Exception e) {
            taskExecution.setError(new ExecutionError(e.getMessage(), Arrays.asList(ExceptionUtils.getStackFrames(e))));

            eventPublisher.publishEvent(new TaskExecutionErrorEvent(taskExecution));
        }
    }

    @Override
    public TaskDispatcher<? extends Task> resolve(Task task) {
        if (Objects.equals(task.getType(), ForkJoinTaskDispatcherConstants.FORK_JOIN + "/v1")) {
            return this;
        }

        return null;
    }
}
