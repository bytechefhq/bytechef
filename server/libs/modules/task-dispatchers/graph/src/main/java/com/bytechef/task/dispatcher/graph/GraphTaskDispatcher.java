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

package com.bytechef.task.dispatcher.graph;

import static com.bytechef.task.dispatcher.graph.constant.GraphTaskDispatcherConstants.DEFAULT_MAX_TRANSITIONS;
import static com.bytechef.task.dispatcher.graph.constant.GraphTaskDispatcherConstants.GRAPH;
import static com.bytechef.task.dispatcher.graph.constant.GraphTaskDispatcherConstants.MAX_TRANSITIONS;
import static com.bytechef.task.dispatcher.graph.constant.GraphTaskDispatcherConstants.NAME;
import static com.bytechef.task.dispatcher.graph.constant.GraphTaskDispatcherConstants.NODES;
import static com.bytechef.task.dispatcher.graph.constant.GraphTaskDispatcherConstants.START_NODE;
import static com.bytechef.task.dispatcher.graph.constant.GraphTaskDispatcherConstants.TASKS;

import com.bytechef.atlas.configuration.domain.Task;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.coordinator.event.TaskExecutionCompleteEvent;
import com.bytechef.atlas.coordinator.task.dispatcher.ErrorHandlingTaskDispatcher;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcher;
import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcherResolver;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.apache.commons.lang3.Validate;
import org.springframework.context.ApplicationEventPublisher;
import tools.jackson.core.type.TypeReference;

/**
 * Dispatches the start node of a {@code graph/v1} state-machine container.
 *
 * <p>
 * A graph is a set of named nodes, each holding an ordinary task list, wired together by per-node {@code next}
 * expressions evaluated at node exhaustion (owned by {@code GraphTaskCompletionHandler}, not this class). Cycles are
 * legal; a total transition budget (seeded here via {@link CounterService}) bounds them.
 * </p>
 *
 * <p>
 * On dispatch, the start node's first task is created with {@code parameters.__node} stamped to the start node's name
 * so the completion handler can find its way back to the owning node. A start node with an empty task list acts as a
 * pure router: nothing is dispatched, and this class completes itself immediately (still after seeding the transition
 * budget) with a distinct {@code parameters.__routerNode} stamp (not {@code __node}) -- so the router hand-off never
 * clobbers an outer graph's own {@code __node} stamp when this graph is itself nested inside another graph's node --
 * handing router-chaining off entirely to the completion handler, which keys its router-hand-off detection on
 * {@code __routerNode} alone.
 * </p>
 *
 * @author Ivica Cardic
 */
public class GraphTaskDispatcher extends ErrorHandlingTaskDispatcher implements TaskDispatcherResolver {

    private final ContextService contextService;
    private final CounterService counterService;
    private final Evaluator evaluator;
    private final ApplicationEventPublisher eventPublisher;
    private final TaskDispatcher<? super Task> taskDispatcher;
    private final TaskExecutionService taskExecutionService;
    private final TaskFileStorage taskFileStorage;

    @SuppressFBWarnings("EI")
    public GraphTaskDispatcher(
        ContextService contextService, CounterService counterService, Evaluator evaluator,
        ApplicationEventPublisher eventPublisher, TaskDispatcher<? super Task> taskDispatcher,
        TaskExecutionService taskExecutionService, TaskFileStorage taskFileStorage) {

        super(eventPublisher);

        this.contextService = contextService;
        this.counterService = counterService;
        this.evaluator = evaluator;
        this.eventPublisher = eventPublisher;
        this.taskDispatcher = taskDispatcher;
        this.taskExecutionService = taskExecutionService;
        this.taskFileStorage = taskFileStorage;
    }

    @Override
    public void doDispatch(TaskExecution taskExecution) {
        List<Map<String, ?>> nodes = getNodes(taskExecution);

        validateUniqueNodeNames(nodes);

        Map<String, ?> startNode = resolveStartNode(taskExecution, nodes);
        String startNodeName = MapUtils.getRequiredString(startNode, NAME);

        taskExecution.setStartDate(Instant.now());
        taskExecution.setStatus(TaskExecution.Status.STARTED);

        taskExecution = taskExecutionService.update(taskExecution);

        long taskExecutionId = Validate.notNull(taskExecution.getId(), "id");

        int maxTransitions = MapUtils.getInteger(
            taskExecution.getParameters(), MAX_TRANSITIONS, DEFAULT_MAX_TRANSITIONS);

        counterService.set(taskExecutionId, maxTransitions);

        List<WorkflowTask> nodeWorkflowTasks = getNodeWorkflowTasks(startNode);

        if (nodeWorkflowTasks.isEmpty()) {
            dispatchRouterNode(taskExecution, startNodeName);
        } else {
            GraphTaskUtils.dispatchNodeTask(
                contextService, evaluator, taskDispatcher, taskExecutionService, taskFileStorage, taskExecution,
                nodeWorkflowTasks.getFirst(), 1, startNodeName);
        }
    }

    @Override
    public TaskDispatcher<? extends Task> resolve(Task task) {
        if (Objects.equals(task.getType(), GRAPH + "/v1")) {
            return this;
        }

        return null;
    }

    private void dispatchRouterNode(TaskExecution taskExecution, String startNodeName) {
        taskExecution.setWorkflowTask(
            GraphTaskUtils.stampRouterNode(
                taskExecution.getWorkflowTask()
                    .toMap(),
                startNodeName));

        taskExecution.setStartDate(Instant.now());
        taskExecution.setEndDate(Instant.now());
        taskExecution.setExecutionTime(0);

        eventPublisher.publishEvent(new TaskExecutionCompleteEvent(taskExecution));
    }

    private static List<Map<String, ?>> getNodes(TaskExecution taskExecution) {
        List<Map<String, ?>> nodes = MapUtils.getList(
            taskExecution.getParameters(), NODES, new TypeReference<Map<String, ?>>() {}, List.of());

        Validate.isTrue(!nodes.isEmpty(), "graph must define at least one node");

        return nodes;
    }

    private static void validateUniqueNodeNames(List<Map<String, ?>> nodes) {
        Set<String> nodeNames = new HashSet<>();

        for (Map<String, ?> node : nodes) {
            String name = MapUtils.getRequiredString(node, NAME);

            if (!nodeNames.add(name)) {
                throw new IllegalArgumentException("Duplicate graph node name: '" + name + "'");
            }
        }
    }

    private static Map<String, ?> resolveStartNode(TaskExecution taskExecution, List<Map<String, ?>> nodes) {
        String startNodeName = MapUtils.getString(taskExecution.getParameters(), START_NODE);

        if (startNodeName == null) {
            return nodes.getFirst();
        }

        return nodes.stream()
            .filter(node -> Objects.equals(MapUtils.getString(node, NAME), startNodeName))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown graph start node: '" + startNodeName + "'"));
    }

    private static List<WorkflowTask> getNodeWorkflowTasks(Map<String, ?> node) {
        return MapUtils.getList(node, TASKS, new TypeReference<Map<String, ?>>() {}, List.of())
            .stream()
            .map(WorkflowTask::new)
            .toList();
    }
}
