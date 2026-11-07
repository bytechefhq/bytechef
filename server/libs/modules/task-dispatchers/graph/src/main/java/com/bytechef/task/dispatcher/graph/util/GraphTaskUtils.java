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
import static com.bytechef.task.dispatcher.graph.constant.GraphTaskDispatcherConstants.NODES;
import static com.bytechef.task.dispatcher.graph.constant.GraphTaskDispatcherConstants.TO;
import static com.bytechef.task.dispatcher.graph.constant.GraphTaskDispatcherConstants.TRANSITIONS;

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
 * @author Ivica Cardic
 */
public class GraphTaskUtils {

    private static final String FORMULA_PREFIX = "=";

    private GraphTaskUtils() {
    }

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

    public static Optional<String> resolveTransition(
        Evaluator evaluator, List<Map<String, ?>> transitions, String fromNodeName, Map<String, ?> context) {

        List<Map<String, ?>> outgoingTransitions = transitions.stream()
            .filter(transition -> Objects.equals(MapUtils.getString(transition, FROM), fromNodeName))
            .toList();

        for (Map<String, ?> transition : outgoingTransitions) {
            if (!isConditional(transition)) {
                continue;
            }

            if (evaluatesTrue(evaluator, fromNodeName, MapUtils.getRequiredString(transition, CONDITION), context)) {
                return Optional.of(resolveTarget(evaluator, fromNodeName, transition, context));
            }
        }

        for (Map<String, ?> transition : outgoingTransitions) {
            if (!isConditional(transition)) {
                return Optional.of(resolveTarget(evaluator, fromNodeName, transition, context));
            }
        }

        return Optional.empty();
    }

    public static void dispatchNodeTask(
        ContextService contextService, Evaluator evaluator, TaskDispatcher<? super Task> taskDispatcher,
        TaskExecutionService taskExecutionService, TaskFileStorage taskFileStorage,
        TaskExecution graphTaskExecution, WorkflowTask nodeTask) {

        long graphTaskExecutionId = Validate.notNull(graphTaskExecution.getId(), "id");

        TaskExecution subTaskExecution = TaskExecution.builder()
            .jobId(graphTaskExecution.getJobId())
            .maxRetries(nodeTask.getMaxRetries())
            .parentId(graphTaskExecutionId)
            .priority(graphTaskExecution.getPriority())
            .taskNumber(1)
            .workflowTask(nodeTask)
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

    public static boolean isConditional(Map<String, ?> transition) {
        String condition = MapUtils.getString(transition, CONDITION);

        if (condition == null) {
            return false;
        }

        String trimmedCondition = condition.trim();

        return !trimmedCondition.isEmpty() && !Objects.equals(trimmedCondition, FORMULA_PREFIX);
    }

    private static boolean evaluatesTrue(
        Evaluator evaluator, String fromNodeName, String conditionExpression, Map<String, ?> context) {

        Map<String, ?> evaluated = evaluator.evaluate(Map.of(CONDITION, conditionExpression), context);

        if (evaluated.get(CONDITION) instanceof Boolean booleanValue) {
            return booleanValue;
        }

        throw new IllegalArgumentException(
            "Transition from node '" + fromNodeName + "' has a condition that did not evaluate to true or false: " +
                conditionExpression);
    }

    private static String resolveTarget(
        Evaluator evaluator, String fromNodeName, Map<String, ?> transition, Map<String, ?> context) {

        String toExpression = MapUtils.getString(transition, TO);

        if (toExpression == null || toExpression.isBlank()) {
            throw new IllegalArgumentException("Transition from node '" + fromNodeName + "' has no target node");
        }

        Map<String, ?> evaluated = evaluator.evaluate(Map.of(TO, toExpression), context);

        Object toValue = evaluated.get(TO);

        String target = toValue == null ? "" : String.valueOf(toValue);

        target = target.trim();

        if (target.isEmpty()) {
            throw new IllegalArgumentException(
                "Transition from node '" + fromNodeName + "' has a target that did not resolve to a node name: " +
                    toExpression);
        }

        return target;
    }
}
