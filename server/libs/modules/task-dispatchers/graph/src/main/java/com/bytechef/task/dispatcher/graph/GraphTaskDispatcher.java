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
import static com.bytechef.task.dispatcher.graph.constant.GraphTaskDispatcherConstants.START_NODE;

import com.bytechef.atlas.configuration.domain.Task;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
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
import java.util.Objects;
import java.util.Set;
import org.apache.commons.lang3.Validate;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Dispatches the start node of a {@code graph/v1} state-machine container.
 *
 * <p>
 * A graph is a plain list of task nodes ({@code nodes}, one task per node, node name == task name) wired together by a
 * sibling {@code transitions} edge list, evaluated at node completion (owned by {@code GraphTaskCompletionHandler}, not
 * this class). Cycles are legal; a total transition budget (seeded here via {@link CounterService}) bounds them.
 * </p>
 *
 * <p>
 * On dispatch, the start node's task is created with {@code parameters.__node} stamped to the start node's name so the
 * completion handler can find its way back to the owning node.
 * </p>
 *
 * @author Ivica Cardic
 */
public class GraphTaskDispatcher extends ErrorHandlingTaskDispatcher implements TaskDispatcherResolver {

    private final ContextService contextService;
    private final CounterService counterService;
    private final Evaluator evaluator;
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
        this.taskDispatcher = taskDispatcher;
        this.taskExecutionService = taskExecutionService;
        this.taskFileStorage = taskFileStorage;
    }

    @Override
    public void doDispatch(TaskExecution taskExecution) {
        List<WorkflowTask> nodes = GraphTaskUtils.getNodes(taskExecution.getParameters());

        Validate.isTrue(!nodes.isEmpty(), "graph must define at least one node");

        validateUniqueNodeNames(nodes);

        WorkflowTask startNode = resolveStartNode(taskExecution, nodes);

        taskExecution.setStartDate(Instant.now());
        taskExecution.setStatus(TaskExecution.Status.STARTED);

        taskExecution = taskExecutionService.update(taskExecution);

        long taskExecutionId = Validate.notNull(taskExecution.getId(), "id");

        int maxTransitions = MapUtils.getInteger(
            taskExecution.getParameters(), MAX_TRANSITIONS, DEFAULT_MAX_TRANSITIONS);

        counterService.set(taskExecutionId, maxTransitions);

        GraphTaskUtils.dispatchNodeTask(
            contextService, evaluator, taskDispatcher, taskExecutionService, taskFileStorage, taskExecution,
            startNode, startNode.getName());
    }

    @Override
    public TaskDispatcher<? extends Task> resolve(Task task) {
        if (Objects.equals(task.getType(), GRAPH + "/v1")) {
            return this;
        }

        return null;
    }

    private static void validateUniqueNodeNames(List<WorkflowTask> nodes) {
        Set<String> nodeNames = new HashSet<>();

        for (WorkflowTask node : nodes) {
            String name = Validate.notBlank(node.getName(), "graph node name");

            if (!nodeNames.add(name)) {
                throw new IllegalArgumentException("Duplicate graph node name: '" + name + "'");
            }
        }
    }

    private static WorkflowTask resolveStartNode(TaskExecution taskExecution, List<WorkflowTask> nodes) {
        String startNodeName = MapUtils.getString(taskExecution.getParameters(), START_NODE);

        if (startNodeName == null || startNodeName.isBlank()) {
            return nodes.getFirst();
        }

        return GraphTaskUtils.findNode(nodes, startNodeName)
            .orElseThrow(() -> new IllegalArgumentException("Unknown graph start node: '" + startNodeName + "'"));
    }
}
