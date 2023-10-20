
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

import static com.bytechef.hermes.task.dispatcher.constants.Versions.VERSION_1;
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
    private final TaskDispatcher taskDispatcher;
    private final TaskEvaluator taskEvaluator;
    private final TaskExecutionService taskExecutionService;

    public ForkJoinTaskDispatcher(
        ContextService contextService,
        CounterService counterService,
        MessageBroker messageBroker,
        TaskDispatcher taskDispatcher,
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
        @SuppressWarnings("unchecked")
        List<List<WorkflowTask>> branchesWorkflowTasks = MapUtils
            .getList(taskExecution.getParameters(), BRANCHES, List.class)
            .stream()
            .map(curList -> ((List<Map<String, Object>>) curList)
                .stream()
                .map(WorkflowTask::new)
                .toList())
            .toList();

        Assert.notNull(branchesWorkflowTasks, "'branches' property can't be null");

        TaskExecution forkTaskExecution = new TaskExecution(taskExecution);

        forkTaskExecution.setStartTime(LocalDateTime.now());
        forkTaskExecution.setStatus(TaskStatus.STARTED);

        taskExecutionService.update(forkTaskExecution);

        if (branchesWorkflowTasks.size() > 0) {
            counterService.set(taskExecution.getId(), branchesWorkflowTasks.size());

            for (int i = 0; i < branchesWorkflowTasks.size(); i++) {
                List<WorkflowTask> branchWorkflowTask = branchesWorkflowTasks.get(i);

                Assert.isTrue(branchWorkflowTask.size() > 0, "branch " + i + " does not contain any tasks");

                TaskExecution branchTaskExecution = new TaskExecution(branchWorkflowTask.get(0), Map.of("branch", i));

                branchTaskExecution.setId(UUIDUtils.generate());
                branchTaskExecution.setJobId(taskExecution.getJobId());
                branchTaskExecution.setParentId(taskExecution.getId());
                branchTaskExecution.setPriority(taskExecution.getPriority());
                branchTaskExecution.setStatus(TaskStatus.CREATED);
                branchTaskExecution.setTaskNumber(1);

                Context context = new Context(contextService.peek(taskExecution.getId()));

                contextService.push(taskExecution.getId() + "/" + i, context);
                contextService.push(branchTaskExecution.getId(), context);

                TaskExecution evaluatedTaskExecution = taskEvaluator.evaluate(branchTaskExecution, context);

                evaluatedTaskExecution = taskExecutionService.add(evaluatedTaskExecution);

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
    public TaskDispatcher resolve(Task task) {
        if (task.getType()
            .equals(FORK_JOIN + "/v" + VERSION_1)) {
            return this;
        }

        return null;
    }
}
