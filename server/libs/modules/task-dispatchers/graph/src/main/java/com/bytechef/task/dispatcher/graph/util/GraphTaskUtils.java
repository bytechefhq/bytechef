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

package com.bytechef.task.dispatcher.graph.util;

import static com.bytechef.task.dispatcher.graph.constant.GraphTaskDispatcherConstants.NAME;
import static com.bytechef.task.dispatcher.graph.constant.GraphTaskDispatcherConstants.NEXT;
import static com.bytechef.task.dispatcher.graph.constant.GraphTaskDispatcherConstants.NODE;
import static com.bytechef.task.dispatcher.graph.constant.GraphTaskDispatcherConstants.NODES;
import static com.bytechef.task.dispatcher.graph.constant.GraphTaskDispatcherConstants.ROUTER_NODE;
import static com.bytechef.task.dispatcher.graph.constant.GraphTaskDispatcherConstants.TASKS;

import com.bytechef.atlas.configuration.constant.WorkflowConstants;
import com.bytechef.atlas.configuration.domain.Task;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcher;
import com.bytechef.atlas.execution.domain.Context;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.evaluator.Evaluator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.apache.commons.lang3.Validate;
import tools.jackson.core.type.TypeReference;

/**
 * Shared helpers for stamping, node lookup, {@code next} expression evaluation, and node-task dispatch used by both
 * {@link com.bytechef.task.dispatcher.graph.GraphTaskDispatcher} and
 * {@link com.bytechef.task.dispatcher.graph.completion.GraphTaskCompletionHandler} -- transition resolution and node
 * dispatch live in exactly one place so the two classes cannot drift apart.
 *
 * @author Ivica Cardic
 */
public class GraphTaskUtils {

    private GraphTaskUtils() {
    }

    /**
     * Stamps {@code parameters.__node} onto a copy of the given workflow task map, so a completion handler can find its
     * way back to the owning graph node. Overwrites any existing {@code __node} value; every other parameter is
     * preserved.
     */
    public static WorkflowTask stampNode(Map<String, ?> workflowTaskMap, String nodeName) {
        return new WorkflowTask(
            MapUtils.append(workflowTaskMap, WorkflowConstants.PARAMETERS, Map.of(NODE, nodeName)));
    }

    /**
     * Stamps {@code parameters.__routerNode} (a distinct key from {@link #stampNode}'s {@code __node}) onto a copy of
     * the given workflow task map, for {@code GraphTaskDispatcher}'s router hand-off of an empty start node. Using a
     * separate key -- rather than reusing {@code __node} -- means the router hand-off never clobbers an outer graph's
     * {@code __node} stamp when this graph is itself nested inside another graph's node; every other parameter,
     * including any existing {@code __node}, is preserved.
     */
    public static WorkflowTask stampRouterNode(Map<String, ?> workflowTaskMap, String nodeName) {
        return new WorkflowTask(
            MapUtils.append(workflowTaskMap, WorkflowConstants.PARAMETERS, Map.of(ROUTER_NODE, nodeName)));
    }

    /**
     * Removes {@code parameters.__routerNode} from a copy of the given workflow task map, leaving every other parameter
     * (including any {@code __node}) untouched. A no-op copy when {@code __routerNode} is already absent.
     *
     * <p>
     * Used by {@code GraphTaskCompletionHandler#completeGraph} before persisting a terminal resolution that was reached
     * directly from the router hand-off entry point (an all-router graph, e.g. two empty nodes chained straight to
     * terminal, that never dispatches a single real task). In that shape {@code completeGraph} is called with the SAME
     * in-memory task execution {@code GraphTaskDispatcher#dispatchRouterNode} stamped -- without stripping the stamp
     * first, persisting and re-entering the completion chain on that same, still-stamped execution would make
     * {@code isRouterHandOff} match again on re-entry, recursing back into transition resolution instead of completing
     * (and eventually exhausting the transition budget instead of finishing cleanly).
     * </p>
     */
    public static WorkflowTask stripRouterNode(Map<String, ?> workflowTaskMap) {
        Map<String, ?> parameters = MapUtils.getMap(workflowTaskMap, WorkflowConstants.PARAMETERS, Map.of());

        if (!parameters.containsKey(ROUTER_NODE)) {
            return new WorkflowTask(workflowTaskMap);
        }

        Map<String, Object> newParameters = new LinkedHashMap<>(parameters);

        newParameters.remove(ROUTER_NODE);

        Map<String, Object> newWorkflowTaskMap = new LinkedHashMap<>(workflowTaskMap);

        newWorkflowTaskMap.put(WorkflowConstants.PARAMETERS, newParameters);

        return new WorkflowTask(newWorkflowTaskMap);
    }

    /**
     * Reads the {@code nodes} list out of a graph task's (already resolved) parameters, without validation -- the
     * dispatcher is the only place non-empty/unique-name validation is enforced, at dispatch time.
     */
    public static List<Map<String, ?>> getNodes(Map<String, ?> parameters) {
        return MapUtils.getList(parameters, NODES, new TypeReference<Map<String, ?>>() {}, List.of());
    }

    public static Optional<Map<String, ?>> findNode(List<Map<String, ?>> nodes, String name) {
        return nodes.stream()
            .filter(node -> Objects.equals(MapUtils.getString(node, NAME), name))
            .findFirst();
    }

    public static List<WorkflowTask> getNodeWorkflowTasks(Map<String, ?> node) {
        return MapUtils.getList(node, TASKS, new TypeReference<Map<String, ?>>() {}, List.of())
            .stream()
            .map(WorkflowTask::new)
            .toList();
    }

    /**
     * Evaluates a node's {@code next} expression against the accumulated context, through the same injected
     * {@link Evaluator} used to evaluate ordinary task parameters (the {@code branch} dispatcher's {@code resolveCase}
     * is the precedent for evaluating a single raw expression value this way, rather than re-deriving a bespoke SpEL
     * parser like {@code ConditionTaskUtils} does for boolean conditions).
     *
     * @return the resolved node name, or {@code null} when {@code next} is absent, blank, or evaluates to a null/blank
     *         value -- all of which mean "terminal".
     */
    public static String resolveNext(Evaluator evaluator, Map<String, ?> node, Map<String, ?> context) {
        String nextExpression = MapUtils.getString(node, NEXT);

        if (nextExpression == null || nextExpression.isBlank()) {
            return null;
        }

        Map<String, ?> evaluated = evaluator.evaluate(Map.of(NEXT, nextExpression), context);

        Object nextValue = evaluated.get(NEXT);

        if (nextValue == null) {
            return null;
        }

        String next = String.valueOf(nextValue)
            .trim();

        return next.isEmpty() ? null : next;
    }

    /**
     * Creates, evaluates, persists, and dispatches the given node's task as {@code taskNumber} within {@code nodeName},
     * stamped with a fresh {@code __node} value -- the single dispatch path shared by the dispatcher's start-node
     * dispatch, mid-node advancement, and post-transition dispatch of a target node's first task.
     */
    public static void dispatchNodeTask(
        ContextService contextService, Evaluator evaluator, TaskDispatcher<? super Task> taskDispatcher,
        TaskExecutionService taskExecutionService, TaskFileStorage taskFileStorage,
        TaskExecution graphTaskExecution, WorkflowTask workflowTask, int taskNumber, String nodeName) {

        long graphTaskExecutionId = Validate.notNull(graphTaskExecution.getId(), "id");

        TaskExecution subTaskExecution = TaskExecution.builder()
            .jobId(graphTaskExecution.getJobId())
            .maxRetries(workflowTask.getMaxRetries())
            .parentId(graphTaskExecutionId)
            .priority(graphTaskExecution.getPriority())
            .taskNumber(taskNumber)
            .workflowTask(stampNode(workflowTask.toMap(), nodeName))
            .build();

        Map<String, ?> context = taskFileStorage.readContextValue(
            contextService.peek(graphTaskExecutionId, Context.Classname.TASK_EXECUTION));

        subTaskExecution.evaluate(context, evaluator);

        subTaskExecution = taskExecutionService.create(subTaskExecution);

        long subTaskExecutionId = Validate.notNull(subTaskExecution.getId(), "id");

        contextService.push(
            subTaskExecutionId, Context.Classname.TASK_EXECUTION,
            taskFileStorage.storeContextValue(subTaskExecutionId, Context.Classname.TASK_EXECUTION, context));

        taskDispatcher.dispatch(subTaskExecution);
    }
}
