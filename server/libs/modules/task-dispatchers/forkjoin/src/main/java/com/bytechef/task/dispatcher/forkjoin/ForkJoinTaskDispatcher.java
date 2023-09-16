
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

package com.bytechef.task.dispatcher.forkjoin;

import static com.bytechef.task.dispatcher.forkjoin.constant.ForkJoinTaskDispatcherConstants.BRANCH;
import static com.bytechef.task.dispatcher.forkjoin.constant.ForkJoinTaskDispatcherConstants.BRANCHES;
import static com.bytechef.task.dispatcher.forkjoin.constant.ForkJoinTaskDispatcherConstants.FORK_JOIN;

import com.bytechef.atlas.configuration.constant.WorkflowConstants;
import com.bytechef.atlas.execution.domain.Context;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.file.storage.facade.WorkflowFileStorageFacade;
import com.bytechef.atlas.execution.message.broker.TaskMessageRoute;
import com.bytechef.message.broker.MessageBroker;
import com.bytechef.atlas.execution.service.RemoteContextService;
import com.bytechef.atlas.execution.service.RemoteCounterService;
import com.bytechef.atlas.execution.service.RemoteTaskExecutionService;
import com.bytechef.atlas.configuration.task.Task;
import com.bytechef.atlas.configuration.task.WorkflowTask;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcher;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcherResolver;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.MapUtils;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.util.Assert;

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
 * @see com.bytechef.task.dispatcher.forkjoin.completion.ForkJoinTaskCompletionHandler
 */
public class ForkJoinTaskDispatcher implements TaskDispatcher<TaskExecution>, TaskDispatcherResolver {

    private final RemoteContextService contextService;
    private final RemoteCounterService counterService;
    private final MessageBroker messageBroker;
    private final TaskDispatcher<? super Task> taskDispatcher;
    private final RemoteTaskExecutionService taskExecutionService;
    private final WorkflowFileStorageFacade workflowFileStorageFacade;

    @SuppressFBWarnings("EI")
    public ForkJoinTaskDispatcher(
        RemoteContextService contextService, RemoteCounterService counterService, MessageBroker messageBroker,
        TaskDispatcher<? super Task> taskDispatcher, RemoteTaskExecutionService taskExecutionService,
        WorkflowFileStorageFacade workflowFileStorageFacade) {

        this.contextService = contextService;
        this.counterService = counterService;
        this.messageBroker = messageBroker;
        this.taskDispatcher = taskDispatcher;
        this.taskExecutionService = taskExecutionService;
        this.workflowFileStorageFacade = workflowFileStorageFacade;
    }

    @Override
    @SuppressFBWarnings("NP")
    public void dispatch(TaskExecution taskExecution) {
        List<List<Map<String, Object>>> branches = MapUtils.getRequiredList(
            taskExecution.getParameters(), BRANCHES, new ParameterizedTypeReference<>() {});

        List<List<WorkflowTask>> branchesWorkflowTasks = branches.stream()
            .map(source -> CollectionUtils.map(source, WorkflowTask::of))
            .toList();

        taskExecution.setStartDate(LocalDateTime.now());
        taskExecution.setStatus(TaskExecution.Status.STARTED);

        taskExecution = taskExecutionService.update(taskExecution);

        if (branchesWorkflowTasks.isEmpty()) {
            taskExecution.setStartDate(LocalDateTime.now());
            taskExecution.setEndDate(LocalDateTime.now());
            taskExecution.setExecutionTime(0);

            messageBroker.send(TaskMessageRoute.TASKS_COMPLETE, taskExecution);
        } else {
            Assert.notNull(taskExecution.getId(), "'taskExecution.id' must not be null");

            counterService.set(taskExecution.getId(), branchesWorkflowTasks.size());

            for (int i = 0; i < branchesWorkflowTasks.size(); i++) {
                List<WorkflowTask> branchWorkflowTasks = branchesWorkflowTasks.get(i);

                Assert.isTrue(branchWorkflowTasks.size() > 0, "branch " + i + " does not contain any tasks");

                WorkflowTask branchWorkflowTask = branchWorkflowTasks.get(0);

                Assert.notNull(taskExecution.getJobId(), "'taskExecution.jobId' must not be null");

                TaskExecution branchTaskExecution = TaskExecution.builder()
                    .jobId(taskExecution.getJobId())
                    .parentId(taskExecution.getId())
                    .priority(taskExecution.getPriority())
                    .taskNumber(1)
                    .workflowTask(
                        WorkflowTask.of(
                            MapUtils.append(
                                branchWorkflowTask.toMap(), WorkflowConstants.PARAMETERS, Map.of(BRANCH, i))))
                    .build();

                Map<String, ?> context = workflowFileStorageFacade.readContextValue(
                    contextService.peek(taskExecution.getId(), Context.Classname.TASK_EXECUTION));

                branchTaskExecution.evaluate(context);

                branchTaskExecution = taskExecutionService.create(branchTaskExecution);

                Assert.notNull(branchTaskExecution.getId(), "'branchTaskExecution.getId' must not be null");

                contextService.push(
                    branchTaskExecution.getId(), Context.Classname.TASK_EXECUTION,
                    workflowFileStorageFacade.storeContextValue(
                        branchTaskExecution.getId(), Context.Classname.TASK_EXECUTION, context));
                contextService.push(
                    taskExecution.getId(), i, Context.Classname.TASK_EXECUTION,
                    workflowFileStorageFacade.storeContextValue(taskExecution.getId(), i,
                        Context.Classname.TASK_EXECUTION,
                        context));

                taskDispatcher.dispatch(branchTaskExecution);
            }
        }
    }

    @Override
    public TaskDispatcher<? extends Task> resolve(Task task) {
        if (Objects.equals(task.getType(), FORK_JOIN + "/v1")) {
            return this;
        }

        return null;
    }
}
