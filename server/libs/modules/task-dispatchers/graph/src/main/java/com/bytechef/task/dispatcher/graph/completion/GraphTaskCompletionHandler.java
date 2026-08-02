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
import static com.bytechef.task.dispatcher.graph.constant.GraphTaskDispatcherConstants.ROUTER_NODE;

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
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Advances a {@code graph/v1} container: mid-node task advancement, {@code next}-expression transition resolution (with
 * router-chain hopping and transition-budget enforcement), and terminal output/completion.
 *
 * <p>
 * Two distinct completions land here (see {@link #canHandle(TaskExecution)}):
 * </p>
 * <ol>
 * <li>a real sub-task (or nested container) finishing under a graph node -- its parent is the graph's own task
 * execution, and it carries the node's {@code __node} stamp on its own parameters;</li>
 * <li>the router hand-off {@code GraphTaskDispatcher} publishes for an empty start node -- the completed execution IS
 * the graph's own task execution, self-stamped with the distinct {@code __routerNode} sentinel (never {@code __node}
 * itself, so a graph nested inside another graph's node keeps its outer {@code __node} stamp intact when its own start
 * node is a router).</li>
 * </ol>
 *
 * <p>
 * In both cases the relevant stamp is read off the event-carried {@link TaskExecution}'s in-memory parameters, never
 * re-fetched from the graph's own persisted row -- the router hand-off never persists its self-stamp (see
 * {@code GraphTaskDispatcher#dispatchRouterNode}).
 * </p>
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
        return isRouterHandOff(taskExecution) || isNodeSubTaskCompletion(taskExecution);
    }

    @Override
    public void handle(TaskExecution taskExecution) {
        if (isRouterHandOff(taskExecution)) {
            String nodeName = MapUtils.getRequiredString(taskExecution.getParameters(), ROUTER_NODE);

            transition(taskExecution, nodeName, null);

            // The router hand-off event's own task execution is the graph's own row (parentId == null for a
            // top-level graph), so without this it would ALSO satisfy DefaultTaskCompletionHandler#canHandle
            // (parentId == null && !isHandled()) and be mistaken for "the job's root task finished". Two
            // sub-cases, both needing this:
            // - transition() dispatched further work (a real task, or another node's own sub-tree) and simply
            // returned -- nothing else has marked this event handled, so without this line the chain would
            // fall through to DefaultTaskCompletionHandler on the SAME still-unhandled event and complete
            // the job while that work is still in flight.
            // - transition() resolved straight to terminal (completeGraph already ran, synchronously, on this
            // SAME task execution instance -- see completeGraph's own __routerNode-stripping guard against
            // re-triggering isRouterHandOff on that inner re-entry). completeGraph's inner
            // taskCompletionHandler.handle(...) call already drove the genuine completion signal before this
            // line runs, so setting handled here is a harmless no-op for THIS outer chain, not the mechanism
            // that made completion happen.
            // Condition's analogous empty-case self-publish avoids the whole problem by having its OWN
            // canHandle reject parentId == null (an empty case is unconditionally terminal for condition), but
            // an empty graph start/router node is explicitly NOT terminal -- it must be intercepted here for
            // transition resolution, so the same event needs this explicit opt-out instead.
            taskExecution.setHandled(true);

            return;
        }

        handleNodeSubTaskCompletion(taskExecution);
    }

    private void handleNodeSubTaskCompletion(TaskExecution taskExecution) {
        taskExecution.setStatus(TaskExecution.Status.COMPLETED);

        taskExecution = taskExecutionService.update(taskExecution);

        TaskExecution graphTaskExecution = taskExecutionService.getTaskExecution(
            Objects.requireNonNull(taskExecution.getParentId()));

        long graphTaskExecutionId = Objects.requireNonNull(graphTaskExecution.getId());
        String nodeName = MapUtils.getRequiredString(taskExecution.getParameters(), NODE);

        pushCompletedTaskOutputToContext(taskExecution, graphTaskExecutionId);

        Map<String, ?> node = requireNode(GraphTaskUtils.getNodes(graphTaskExecution.getParameters()), nodeName);

        List<WorkflowTask> nodeWorkflowTasks = GraphTaskUtils.getNodeWorkflowTasks(node);

        if (taskExecution.getTaskNumber() < nodeWorkflowTasks.size()) {
            WorkflowTask nextWorkflowTask = nodeWorkflowTasks.get(taskExecution.getTaskNumber());

            GraphTaskUtils.dispatchNodeTask(
                contextService, evaluator, taskDispatcher, taskExecutionService, taskFileStorage,
                graphTaskExecution, nextWorkflowTask, taskExecution.getTaskNumber() + 1, nodeName);
        }
        // no more tasks in this node -- resolve its `next` transition
        else {
            transition(graphTaskExecution, nodeName, taskExecution);
        }
    }

    private void pushCompletedTaskOutputToContext(TaskExecution taskExecution, long graphTaskExecutionId) {
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

    /**
     * Resolves {@code currentNodeName}'s {@code next} transition, hopping through any chain of empty router nodes IN
     * THIS LOOP (never recursing back through {@code taskDispatcher.dispatch}), consuming one transition-budget unit
     * per hop, until either a node with tasks is reached (its first task is dispatched, stamped with a fresh
     * {@code __node}) or a node resolves no further {@code next} (terminal).
     *
     * @param completedTaskExecution the real completed child whose output feeds the terminal output copy when
     *                               {@code currentNodeName} itself is terminal (i.e. zero hops); {@code null} for the
     *                               router hand-off case, and reset to {@code null} on every hop since a router node
     *                               has no task output of its own.
     */
    private void transition(
        TaskExecution graphTaskExecution, String currentNodeName, TaskExecution completedTaskExecution) {

        long graphTaskExecutionId = Objects.requireNonNull(graphTaskExecution.getId());

        Map<String, ?> context = taskFileStorage.readContextValue(
            contextService.peek(graphTaskExecutionId, Context.Classname.TASK_EXECUTION));

        List<Map<String, ?>> nodes = GraphTaskUtils.getNodes(graphTaskExecution.getParameters());

        Map<String, ?> node = requireNode(nodes, currentNodeName);

        String fromNodeName = currentNodeName;
        String nextNodeName = GraphTaskUtils.resolveNext(evaluator, node, context);

        while (nextNodeName != null) {
            String resolvedNodeName = nextNodeName;
            String transitioningFromNodeName = fromNodeName;

            Map<String, ?> targetNode = GraphTaskUtils.findNode(nodes, resolvedNodeName)
                .orElseThrow(() -> new IllegalArgumentException(
                    "Unknown graph transition target node: '" + resolvedNodeName + "' resolved from node '"
                        + transitioningFromNodeName + "'"));

            decrementTransitionBudget(graphTaskExecution, graphTaskExecutionId);

            List<WorkflowTask> targetNodeWorkflowTasks = GraphTaskUtils.getNodeWorkflowTasks(targetNode);

            if (!targetNodeWorkflowTasks.isEmpty()) {
                GraphTaskUtils.dispatchNodeTask(
                    contextService, evaluator, taskDispatcher, taskExecutionService, taskFileStorage,
                    graphTaskExecution, targetNodeWorkflowTasks.getFirst(), 1, nextNodeName);

                return;
            }

            // router chain: an empty target node evaluates its own `next` immediately, looping in place
            node = targetNode;
            fromNodeName = nextNodeName;
            completedTaskExecution = null;
            nextNodeName = GraphTaskUtils.resolveNext(evaluator, node, context);
        }

        completeGraph(graphTaskExecution, completedTaskExecution);
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

        // A terminal resolution reached directly from the router hand-off entry point (an all-router graph,
        // e.g. two empty nodes chained straight to terminal, never dispatching a single real task) is called
        // with the SAME in-memory task execution GraphTaskDispatcher#dispatchRouterNode stamped with
        // __routerNode. Strip it before persisting: otherwise the row below would be saved (and re-handed to
        // taskCompletionHandler) still carrying the stamp, so canHandle's isRouterHandOff would match AGAIN on
        // this same re-entry, recursing back into transition() instead of completing -- eventually exhausting
        // the transition budget rather than finishing cleanly. The node-subtask-completion path never hits
        // this: there, graphTaskExecution is always a fresh fetch from the persisted store, which never carries
        // the hand-off's deliberately-unpersisted stamp in the first place.
        if (hasRouterNodeStamp(graphTaskExecution)) {
            graphTaskExecution.setWorkflowTask(
                GraphTaskUtils.stripRouterNode(
                    graphTaskExecution.getWorkflowTask()
                        .toMap()));
        }

        graphTaskExecution.setEndDate(Instant.now());

        graphTaskExecution = taskExecutionService.update(graphTaskExecution);

        taskCompletionHandler.handle(graphTaskExecution);
    }

    private static Map<String, ?> requireNode(List<Map<String, ?>> nodes, String nodeName) {
        return GraphTaskUtils.findNode(nodes, nodeName)
            .orElseThrow(() -> new IllegalStateException("Unknown graph node: '" + nodeName + "'"));
    }

    private boolean isNodeSubTaskCompletion(TaskExecution taskExecution) {
        if (!hasNodeStamp(taskExecution)) {
            return false;
        }

        Long parentId = taskExecution.getParentId();

        if (parentId == null) {
            return false;
        }

        TaskExecution parentTaskExecution = taskExecutionService.getTaskExecution(parentId);

        return isGraphType(parentTaskExecution);
    }

    /**
     * The router hand-off is keyed on {@code __routerNode} alone -- a distinct sentinel from {@code __node} that only
     * {@code GraphTaskDispatcher#dispatchRouterNode} ever stamps, so this needs no "parent is not a graph" heuristic:
     * unlike {@code __node} (which an outer graph also stamps onto this same task execution when it is nested inside
     * one of the outer's node), {@code __routerNode} can never be set by anything other than this graph's own router
     * hand-off, regardless of whether the graph is nested.
     */
    private boolean isRouterHandOff(TaskExecution taskExecution) {
        return isGraphType(taskExecution) && hasRouterNodeStamp(taskExecution);
    }

    private static boolean hasNodeStamp(TaskExecution taskExecution) {
        return MapUtils.getString(taskExecution.getParameters(), NODE) != null;
    }

    private static boolean hasRouterNodeStamp(TaskExecution taskExecution) {
        return MapUtils.getString(taskExecution.getParameters(), ROUTER_NODE) != null;
    }

    private static boolean isGraphType(TaskExecution taskExecution) {
        return Objects.equals(taskExecution.getType(), GRAPH + "/v1");
    }
}
