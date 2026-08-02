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

import static com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDsl.array;
import static com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDsl.integer;
import static com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDsl.object;
import static com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDsl.string;
import static com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDsl.task;
import static com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDsl.taskDispatcher;
import static com.bytechef.platform.workflow.task.dispatcher.output.TaskListOutputDataSource.ENVIRONMENT_ID;
import static com.bytechef.platform.workflow.task.dispatcher.output.TaskListOutputDataSource.WORKFLOW_ID;
import static com.bytechef.task.dispatcher.graph.constant.GraphTaskDispatcherConstants.DEFAULT_MAX_TRANSITIONS;
import static com.bytechef.task.dispatcher.graph.constant.GraphTaskDispatcherConstants.GRAPH;
import static com.bytechef.task.dispatcher.graph.constant.GraphTaskDispatcherConstants.MAX_TRANSITIONS;
import static com.bytechef.task.dispatcher.graph.constant.GraphTaskDispatcherConstants.NAME;
import static com.bytechef.task.dispatcher.graph.constant.GraphTaskDispatcherConstants.NEXT;
import static com.bytechef.task.dispatcher.graph.constant.GraphTaskDispatcherConstants.NODES;
import static com.bytechef.task.dispatcher.graph.constant.GraphTaskDispatcherConstants.START_NODE;
import static com.bytechef.task.dispatcher.graph.constant.GraphTaskDispatcherConstants.TASKS;

import com.bytechef.commons.util.MapUtils;
import com.bytechef.definition.BaseOutputDefinition.OutputResponse;
import com.bytechef.platform.workflow.task.dispatcher.TaskDispatcherDefinitionFactory;
import com.bytechef.platform.workflow.task.dispatcher.definition.Property;
import com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDefinition;
import com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDsl.ModifiableObjectProperty;
import com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDsl.ModifiableValueProperty;
import com.bytechef.platform.workflow.task.dispatcher.output.TaskListOutputDataSource;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;

/**
 * @author Ivica Cardic
 */
@Component
public class GraphTaskDispatcherDefinitionFactory implements TaskDispatcherDefinitionFactory {

    private final TaskDispatcherDefinition taskDispatcherDefinition;

    public GraphTaskDispatcherDefinitionFactory(Optional<TaskListOutputDataSource> taskListOutputDataSource) {
        this.taskDispatcherDefinition = taskDispatcher(GRAPH)
            .title("Graph")
            .description(
                "Directs execution across a set of named nodes connected by dynamic transitions, executing each node's tasks in sequence until a node with no next transition is reached.")
            .icon("path:assets/graph.svg")
            .properties(
                string(START_NODE)
                    .label("Start Node")
                    .description(
                        "The name of the node execution begins from. Defaults to the first declared node when left empty."),
                integer(MAX_TRANSITIONS)
                    .label("Max Transitions")
                    .description(
                        "The maximum number of node-to-node transitions allowed before the graph is halted, to guard against infinite loops.")
                    .defaultValue(DEFAULT_MAX_TRANSITIONS))
            .output(inputParameters -> taskListOutputDataSource
                .map(dataSource -> output(inputParameters, dataSource))
                .orElse(null))
            .taskProperties(
                array(NODES)
                    .description("The list of nodes that make up the graph.")
                    .items(nodeProperty()));
    }

    @Override
    public TaskDispatcherDefinition getDefinition() {
        return taskDispatcherDefinition;
    }

    private static ModifiableObjectProperty nodeProperty() {
        return object()
            .properties(
                string(NAME)
                    .label("Name")
                    .description("The unique name of this node within the graph.")
                    .required(true),
                string(NEXT)
                    .label("Next")
                    .description(
                        "An expression resolving to the name of the node to transition to next. Absent or blank marks this node as terminal.")
                    .controlType(Property.ControlType.FORMULA_MODE),
                array(TASKS)
                    .description("The list of tasks to execute sequentially for this node.")
                    .items(task()));
    }

    protected static OutputResponse output(
        Map<String, ?> inputParameters, TaskListOutputDataSource taskListOutputDataSource) {

        String workflowId = MapUtils.getString(inputParameters, WORKFLOW_ID);
        long environmentId = MapUtils.getLong(inputParameters, ENVIRONMENT_ID, 0L);

        List<Map<String, ?>> nodes = MapUtils.getList(
            inputParameters, NODES, new TypeReference<>() {}, List.of());

        Map<String, ?> terminalNode = findFirstTerminalNode(nodes);

        if (terminalNode == null) {
            return null;
        }

        List<Map<String, ?>> terminalNodeTasks = MapUtils.getList(
            terminalNode, TASKS, new TypeReference<>() {}, List.of());

        if (terminalNodeTasks.isEmpty()) {
            return null;
        }

        Map<String, ?> lastTask = terminalNodeTasks.getLast();

        String lastTaskName = MapUtils.getString(lastTask, "name");
        String lastTaskType = MapUtils.getString(lastTask, "type");

        if (lastTaskType == null) {
            return null;
        }

        OutputResponse lastTaskOutput = taskListOutputDataSource.getLastTaskOutput(
            workflowId, lastTaskName, lastTaskType, environmentId);

        if (lastTaskOutput == null) {
            return null;
        }

        ModifiableValueProperty<?, ?> lastTaskSchema = (ModifiableValueProperty<?, ?>) lastTaskOutput.getOutputSchema();

        Object lastTaskSampleOutput = lastTaskOutput.getSampleOutput();

        if (lastTaskSampleOutput != null) {
            return OutputResponse.of(lastTaskSchema, lastTaskSampleOutput);
        }

        return OutputResponse.of(lastTaskSchema);
    }

    /**
     * Returns the first node whose {@code next} is absent or blank -- the first node declared, in list order, as
     * terminal -- or {@code null} when every node declares a transition.
     */
    private static Map<String, ?> findFirstTerminalNode(List<Map<String, ?>> nodes) {
        for (Map<String, ?> node : nodes) {
            String next = MapUtils.getString(node, NEXT);

            if (next == null || next.isBlank()) {
                return node;
            }
        }

        return null;
    }
}
