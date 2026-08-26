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

import static com.bytechef.task.dispatcher.graph.constant.GraphTaskDispatcherConstants.CONDITION;
import static com.bytechef.task.dispatcher.graph.constant.GraphTaskDispatcherConstants.FROM;
import static com.bytechef.task.dispatcher.graph.constant.GraphTaskDispatcherConstants.NODE;
import static com.bytechef.task.dispatcher.graph.constant.GraphTaskDispatcherConstants.NODES;
import static com.bytechef.task.dispatcher.graph.constant.GraphTaskDispatcherConstants.TO;
import static com.bytechef.task.dispatcher.graph.constant.GraphTaskDispatcherConstants.TRANSITIONS;

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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.apache.commons.lang3.Validate;
import tools.jackson.core.type.TypeReference;

/**
 * Shared helpers for stamping, node lookup, transition resolution and node dispatch used by both
 * {@link com.bytechef.task.dispatcher.graph.GraphTaskDispatcher} and
 * {@link com.bytechef.task.dispatcher.graph.completion.GraphTaskCompletionHandler} -- transition resolution and node
 * dispatch live in exactly one place so the two classes cannot drift apart.
 *
 * @author Ivica Cardic
 */
public class GraphTaskUtils {

    /**
     * What the property editor writes for a formula field with nothing in it -- the mode's prefix and no expression.
     */
    private static final String FORMULA_PREFIX = "=";

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
     * Reads the {@code nodes} task list out of a graph task's (already resolved) parameters, without validation -- the
     * dispatcher is the only place non-empty/unique-name validation is enforced, at dispatch time.
     */
    public static List<WorkflowTask> getNodes(Map<String, ?> parameters) {
        return MapUtils.getList(parameters, NODES, new TypeReference<Map<String, ?>>() {}, List.of())
            .stream()
            .map(WorkflowTask::new)
            .toList();
    }

    public static Optional<WorkflowTask> findNode(List<WorkflowTask> nodes, String name) {
        return nodes.stream()
            .filter(node -> Objects.equals(node.getName(), name))
            .findFirst();
    }

    public static List<Map<String, ?>> getTransitions(Map<String, ?> parameters) {
        return MapUtils.getList(parameters, TRANSITIONS, new TypeReference<Map<String, ?>>() {}, List.of());
    }

    /**
     * Resolves which node, if any, {@code fromNodeName} transitions to: its CONDITIONAL transitions in declared order,
     * the first whose {@code condition} evaluates truthy wins; if none matched, its first UNCONDITIONAL transition; if
     * there is none the node is terminal. A {@code to} that is an expression is evaluated against the context; one that
     * resolves to null/blank counts as "did not match" and evaluation continues with the next candidate. The returned
     * name is NOT validated against the node list -- the completion handler does that so the error can name the source.
     */
    public static Optional<String> resolveTransition(
        Evaluator evaluator, List<Map<String, ?>> transitions, String fromNodeName, Map<String, ?> context) {

        List<Map<String, ?>> outgoingTransitions = transitions.stream()
            .filter(transition -> Objects.equals(MapUtils.getString(transition, FROM), fromNodeName))
            .toList();

        for (Map<String, ?> transition : outgoingTransitions) {
            if (!isConditional(transition)) {
                continue;
            }

            if (evaluatesTruthy(evaluator, fromNodeName, MapUtils.getRequiredString(transition, CONDITION), context)) {
                String target = resolveTarget(evaluator, transition, context);

                if (target != null) {
                    return Optional.of(target);
                }
            }
        }

        for (Map<String, ?> transition : outgoingTransitions) {
            if (isConditional(transition)) {
                continue;
            }

            String target = resolveTarget(evaluator, transition, context);

            if (target != null) {
                return Optional.of(target);
            }
        }

        return Optional.empty();
    }

    /**
     * Creates, evaluates, persists, and dispatches {@code nodeTask} as the single task of {@code nodeName}, stamped
     * with {@code __node} -- the one dispatch path shared by the dispatcher's start-node dispatch and the completion
     * handler's post-transition dispatch.
     */
    public static void dispatchNodeTask(
        ContextService contextService, Evaluator evaluator, TaskDispatcher<? super Task> taskDispatcher,
        TaskExecutionService taskExecutionService, TaskFileStorage taskFileStorage,
        TaskExecution graphTaskExecution, WorkflowTask nodeTask, String nodeName) {

        long graphTaskExecutionId = Validate.notNull(graphTaskExecution.getId(), "id");

        TaskExecution subTaskExecution = TaskExecution.builder()
            .jobId(graphTaskExecution.getJobId())
            .maxRetries(nodeTask.getMaxRetries())
            .parentId(graphTaskExecutionId)
            .priority(graphTaskExecution.getPriority())
            .taskNumber(1)
            .workflowTask(stampNode(nodeTask.toMap(), nodeName))
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

    /**
     * Whether {@code transition} carries a condition at all.
     *
     * A bare formula prefix does not. The editor saves a cleared formula field as {@code "="} rather than as an empty
     * string, and that is not blank -- so it counted as conditional, evaluated falsy (there is no expression to be
     * true), and was then skipped by the unconditional pass as well. A node whose every outgoing transition looked like
     * that simply stopped the graph, with nothing to show why.
     */
    private static boolean isConditional(Map<String, ?> transition) {
        String condition = MapUtils.getString(transition, CONDITION);

        if (condition == null) {
            return false;
        }

        String trimmedCondition = condition.trim();

        return !trimmedCondition.isEmpty() && !Objects.equals(trimmedCondition, FORMULA_PREFIX);
    }

    /**
     * Whether {@code conditionExpression} evaluates to true.
     *
     * A condition that does not evaluate to a boolean at all is an error, not a false. The evaluator hands back the
     * expression unchanged when it cannot parse it, and reading that as "false" made a broken condition
     * indistinguishable from one that simply did not match -- so a node whose only outgoing transition carried a
     * half-typed expression ended the run with nothing said. Failing here names the condition instead.
     */
    private static boolean evaluatesTruthy(
        Evaluator evaluator, String fromNodeName, String conditionExpression, Map<String, ?> context) {

        Map<String, ?> evaluated = evaluator.evaluate(Map.of(CONDITION, conditionExpression), context);

        Object value = evaluated.get(CONDITION);

        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }

        String stringValue = value == null ? null : String.valueOf(value);

        if (stringValue != null &&
            (stringValue.equalsIgnoreCase("true") || stringValue.equalsIgnoreCase("false"))) {

            return Boolean.parseBoolean(stringValue);
        }

        throw new IllegalArgumentException(
            "Transition from node '" + fromNodeName + "' has a condition that did not evaluate to true or false: " +
                conditionExpression);
    }

    private static String resolveTarget(Evaluator evaluator, Map<String, ?> transition, Map<String, ?> context) {
        String toExpression = MapUtils.getString(transition, TO);

        if (toExpression == null || toExpression.isBlank()) {
            return null;
        }

        Map<String, ?> evaluated = evaluator.evaluate(Map.of(TO, toExpression), context);

        Object toValue = evaluated.get(TO);

        if (toValue == null) {
            return null;
        }

        String target = String.valueOf(toValue)
            .trim();

        return target.isEmpty() ? null : target;
    }
}
