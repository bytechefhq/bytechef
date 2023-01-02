
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

package com.bytechef.task.dispatcher.fork;

import static com.bytechef.hermes.task.dispatcher.constants.TaskDispatcherConstants.Versions.VERSION_1;
import static com.bytechef.task.dispatcher.fork.constants.ForkJoinTaskDispatcherConstants.BRANCH;
import static com.bytechef.task.dispatcher.fork.constants.ForkJoinTaskDispatcherConstants.BRANCHES;
import static com.bytechef.task.dispatcher.fork.constants.ForkJoinTaskDispatcherConstants.FORK_JOIN;

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
import com.bytechef.atlas.task.evaluator.TaskEvaluator;
import com.bytechef.atlas.task.execution.TaskStatus;
import com.bytechef.commons.utils.MapUtils;
import com.bytechef.commons.utils.UUIDUtils;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
 *           label: Generate a random number
 *           type: randomInt
 *           startInclusive: 0
 *           endInclusive: 5000
 *
 *         - type: sleep
 *           millis: ${randomNumber}
 *
 *       - - name: randomNumber
 *           label: Generate a random number
 *           type: randomInt
 *           startInclusive: 0
 *           endInclusive: 5000
 *
 *         - type: sleep
 *           millis: ${randomNumber}
 * </pre>
 *
 * @author Arik Cohen
 * @author Ivica Cardic
 * @since May 11, 2017
 * @see com.bytechef.task.dispatcher.fork.completion.ForkJoinTaskCompletionHandler
 */
public class ForkJoinTaskDispatcher implements TaskDispatcher<TaskExecution>, TaskDispatcherResolver {

    private final ContextService contextService;
    private final CounterService counterService;
    private final MessageBroker messageBroker;
    private final TaskDispatcher<? super Task> taskDispatcher;
    private final TaskEvaluator taskEvaluator;
    private final TaskExecutionService taskExecutionService;

    public ForkJoinTaskDispatcher(
        ContextService contextService,
        CounterService counterService,
        MessageBroker messageBroker,
        TaskDispatcher<? super Task> taskDispatcher,
        TaskEvaluator taskEvaluator,
        TaskExecutionService taskExecutionService) {
        this.contextService = contextService;
        this.counterService = counterService;
        this.messageBroker = messageBroker;
        this.taskDispatcher = taskDispatcher;
        this.taskEvaluator = taskEvaluator;
        this.taskExecutionService = taskExecutionService;
    }

    @Override
    public void dispatch(TaskExecution taskExecution) {
        List<List<Map<String, Object>>> branches = MapUtils.getRequiredList(
            taskExecution.getParameters(), BRANCHES, new ParameterizedTypeReference<>() {});

        List<List<WorkflowTask>> branchesWorkflowTasks = branches.stream()
            .map(curList -> curList
                .stream()
                .map(WorkflowTask::new)
                .toList())
            .toList();

        taskExecutionService.updateStatus(taskExecution.getId(), TaskStatus.STARTED, LocalDateTime.now(), null);

        if (branchesWorkflowTasks.isEmpty()) {
            taskExecution.setStartTime(LocalDateTime.now());
            taskExecution.setEndTime(LocalDateTime.now());
            taskExecution.setExecutionTime(0);

            messageBroker.send(Queues.COMPLETIONS, taskExecution);
        } else {
            counterService.set(taskExecution.getId(), branchesWorkflowTasks.size());

            for (int i = 0; i < branchesWorkflowTasks.size(); i++) {
                List<WorkflowTask> branchWorkflowTask = branchesWorkflowTasks.get(i);

                Assert.isTrue(branchWorkflowTask.size() > 0, "branch " + i + " does not contain any tasks");

                TaskExecution branchTaskExecution = new TaskExecution(
                    WorkflowTask.of(Map.of(BRANCH, i), branchWorkflowTask.get(0)));

                branchTaskExecution.setId(UUIDUtils.generate());
                branchTaskExecution.setJobId(taskExecution.getJobId());
                branchTaskExecution.setParentId(taskExecution.getId());
                branchTaskExecution.setPriority(taskExecution.getPriority());
                branchTaskExecution.setStatus(TaskStatus.CREATED);
                branchTaskExecution.setTaskNumber(1);

                Context context = contextService.peek(taskExecution.getId());

                contextService.push(taskExecution.getId() + "/" + i, context);
                contextService.push(branchTaskExecution.getId(), context);

                branchTaskExecution.evaluate(taskEvaluator, context);

                branchTaskExecution = taskExecutionService.create(branchTaskExecution);

                taskDispatcher.dispatch(branchTaskExecution);
            }
        }
    }

    @Override
    public TaskDispatcher<? extends Task> resolve(Task task) {
        if (Objects.equals(task.getType(), FORK_JOIN + "/v" + VERSION_1)) {
            return this;
        }

        return null;
    }
}
