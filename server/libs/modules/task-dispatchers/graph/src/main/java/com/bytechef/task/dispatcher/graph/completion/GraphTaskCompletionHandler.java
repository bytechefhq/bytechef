/*
 * Copyright 2025 ByteChef
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

package com.bytechef.task.dispatcher.graph.completion;

import static com.bytechef.task.dispatcher.graph.constant.GraphTaskDispatcherConstants.DEFAULT_MAX_TRANSITIONS;
import static com.bytechef.task.dispatcher.graph.constant.GraphTaskDispatcherConstants.GRAPH;
import static com.bytechef.task.dispatcher.graph.constant.GraphTaskDispatcherConstants.MAX_TRANSITIONS;
import static com.bytechef.task.dispatcher.graph.constant.GraphTaskDispatcherConstants.NODE;

import com.bytechef.atlas.configuration.domain.Task;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.coordinator.task.completion.TaskCompletionHandler;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcher;
import com.bytechef.atlas.execution.domain.Context;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.CounterService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.task.dispatcher.graph.util.GraphTaskUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Completes a {@code graph/v1} node: pushes the node's output to the accumulated context, resolves its transition per
 * the conditional-then-default rule ({@link GraphTaskUtils#resolveTransition}), enforces the transition budget, and
 * either dispatches the target node's task or completes the graph — releasing the graph's transition-budget counter row
 * as it does.
 *
 * @author Ivica Cardic
 */
public class GraphTaskCompletionHandler implements TaskCompletionHandler {

    private final ContextService contextService;
    private final CounterService counterService;
    private final Evaluator evaluator;
    private final TaskCompletionHandler taskCompletionHandler;
    private final TaskDispatcher<? super Task> taskDispatcher;
    private final TaskExecutionService taskExecutionService;
    private final TaskFileStorage taskFileStorage;

    @SuppressFBWarnings("EI")
    public GraphTaskCompletionHandler(
        ContextService contextService, CounterService counterService, Evaluator evaluator,
        TaskCompletionHandler taskCompletionHandler, TaskDispatcher<? super Task> taskDispatcher,
        TaskExecutionService taskExecutionService, TaskFileStorage taskFileStorage) {

        this.contextService = contextService;
        this.counterService = counterService;
        this.evaluator = evaluator;
        this.taskCompletionHandler = taskCompletionHandler;
        this.taskDispatcher = taskDispatcher;
        this.taskExecutionService = taskExecutionService;
        this.taskFileStorage = taskFileStorage;
    }

    @Override
    public boolean canHandle(TaskExecution taskExecution) {
        if (MapUtils.getString(taskExecution.getParameters(), NODE) == null) {
            return false;
        }

        Long parentId = taskExecution.getParentId();

        if (parentId == null) {
            return false;
        }

        TaskExecution parentTaskExecution = taskExecutionService.getTaskExecution(parentId);

        return Objects.equals(parentTaskExecution.getType(), GRAPH + "/v1");
    }

    @Override
    public void handle(TaskExecution taskExecution) {
        taskExecution.setStatus(TaskExecution.Status.COMPLETED);

        taskExecution = taskExecutionService.update(taskExecution);

        TaskExecution graphTaskExecution = taskExecutionService.getTaskExecution(
            Objects.requireNonNull(taskExecution.getParentId()));

        long graphTaskExecutionId = Objects.requireNonNull(graphTaskExecution.getId());
        String nodeName = MapUtils.getRequiredString(taskExecution.getParameters(), NODE);

        pushCompletedNodeOutputToContext(taskExecution, graphTaskExecutionId);

        Map<String, ?> context = taskFileStorage.readContextValue(
            contextService.peek(graphTaskExecutionId, Context.Classname.TASK_EXECUTION));

        Map<String, ?> graphParameters = graphTaskExecution.getParameters();

        Optional<String> targetNodeName = GraphTaskUtils.resolveTransition(
            evaluator, GraphTaskUtils.getTransitions(graphParameters), nodeName, context);

        if (targetNodeName.isEmpty()) {
            completeGraph(graphTaskExecution, taskExecution);

            return;
        }

        String resolvedTargetNodeName = targetNodeName.get();

        WorkflowTask targetNode =
            GraphTaskUtils.findNode(GraphTaskUtils.getNodes(graphParameters), resolvedTargetNodeName)
                .orElseThrow(() -> new IllegalArgumentException(
                    "Unknown graph transition target node: '" + resolvedTargetNodeName + "' resolved from node '"
                        + nodeName + "'"));

        decrementTransitionBudget(graphTaskExecution, graphTaskExecutionId);

        GraphTaskUtils.dispatchNodeTask(
            contextService, evaluator, taskDispatcher, taskExecutionService, taskFileStorage, graphTaskExecution,
            targetNode, resolvedTargetNodeName);
    }

    private void pushCompletedNodeOutputToContext(TaskExecution taskExecution, long graphTaskExecutionId) {
        if (taskExecution.getName() == null) {
            return;
        }

        Map<String, Object> newContext = new HashMap<>(
            taskFileStorage.readContextValue(
                contextService.peek(graphTaskExecutionId, Context.Classname.TASK_EXECUTION)));

        if (taskExecution.getOutput() != null) {
            newContext.put(
                taskExecution.getName(), taskFileStorage.readTaskExecutionOutput(taskExecution.getOutput()));
        } else {
            newContext.put(taskExecution.getName(), null);
        }

        contextService.push(
            graphTaskExecutionId, Context.Classname.TASK_EXECUTION,
            taskFileStorage.storeContextValue(graphTaskExecutionId, Context.Classname.TASK_EXECUTION, newContext));
    }

    private void decrementTransitionBudget(TaskExecution graphTaskExecution, long graphTaskExecutionId) {
        long remainingTransitions = counterService.decrement(graphTaskExecutionId);

        if (remainingTransitions < 0) {
            int maxTransitions = MapUtils.getInteger(
                graphTaskExecution.getParameters(), MAX_TRANSITIONS, DEFAULT_MAX_TRANSITIONS);

            throw new IllegalStateException(
                "graph transition budget exhausted (maxTransitions=" + maxTransitions + ")");
        }
    }

    private void completeGraph(TaskExecution graphTaskExecution, TaskExecution completedTaskExecution) {
        if (completedTaskExecution != null && completedTaskExecution.getOutput() != null) {
            long jobId = Objects.requireNonNull(graphTaskExecution.getJobId());
            long graphTaskExecutionId = Objects.requireNonNull(graphTaskExecution.getId());

            Object outputValue = taskFileStorage.readTaskExecutionOutput(completedTaskExecution.getOutput());

            graphTaskExecution.setOutput(
                taskFileStorage.storeTaskExecutionOutput(jobId, graphTaskExecutionId, outputValue));
        }

        graphTaskExecution.setEndDate(Instant.now());

        graphTaskExecution = taskExecutionService.update(graphTaskExecution);

        // The transition budget is a counter row keyed by this graph's own execution id, set when
        // GraphTaskDispatcher dispatched the graph and decremented on every transition. Nothing
        // else deletes it, so it is released here, once, as the graph finishes — before the parent
        // chain is re-entered: completing a NESTED graph advances the OUTER graph through that
        // chain, and the outer graph's budget is a different row, so nothing downstream can still
        // need this one. A graph that does NOT finish — budget exhausted, or a node task failing —
        // never reaches here and leaves its row behind, as each and parallel do; the key is a task
        // execution id that never recurs, so nothing can mis-read it.
        counterService.delete(Objects.requireNonNull(graphTaskExecution.getId()));

        taskCompletionHandler.handle(graphTaskExecution);
    }
}
