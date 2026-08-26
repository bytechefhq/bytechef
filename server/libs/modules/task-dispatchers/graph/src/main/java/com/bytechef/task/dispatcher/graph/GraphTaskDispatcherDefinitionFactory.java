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
import static com.bytechef.task.dispatcher.graph.constant.GraphTaskDispatcherConstants.CONDITION;
import static com.bytechef.task.dispatcher.graph.constant.GraphTaskDispatcherConstants.DEFAULT_MAX_TRANSITIONS;
import static com.bytechef.task.dispatcher.graph.constant.GraphTaskDispatcherConstants.FROM;
import static com.bytechef.task.dispatcher.graph.constant.GraphTaskDispatcherConstants.GRAPH;
import static com.bytechef.task.dispatcher.graph.constant.GraphTaskDispatcherConstants.MAX_TRANSITIONS;
import static com.bytechef.task.dispatcher.graph.constant.GraphTaskDispatcherConstants.NAME;
import static com.bytechef.task.dispatcher.graph.constant.GraphTaskDispatcherConstants.NODES;
import static com.bytechef.task.dispatcher.graph.constant.GraphTaskDispatcherConstants.START_NODE;
import static com.bytechef.task.dispatcher.graph.constant.GraphTaskDispatcherConstants.TO;
import static com.bytechef.task.dispatcher.graph.constant.GraphTaskDispatcherConstants.TRANSITIONS;

import com.bytechef.commons.util.MapUtils;
import com.bytechef.definition.BaseOutputDefinition.OutputResponse;
import com.bytechef.platform.workflow.task.dispatcher.TaskDispatcherDefinitionFactory;
import com.bytechef.platform.workflow.task.dispatcher.definition.Property;
import com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDefinition;
import com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDsl.ModifiableValueProperty;
import com.bytechef.platform.workflow.task.dispatcher.output.TaskListOutputDataSource;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
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
                "Runs a set of task nodes wired by transitions: after a node completes, its conditional transitions are checked in order, then its default one; a node with no matching transition ends the graph. Cycles are allowed and bounded by a transition budget.")
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
                    .defaultValue(DEFAULT_MAX_TRANSITIONS),
                array(TRANSITIONS)
                    .label("Transitions")
                    .description(
                        "The edges between nodes. Conditional transitions are checked in declared order, then the unconditional one.")
                    .items(
                        object()
                            .properties(
                                string(FROM)
                                    .label("From")
                                    .description("The name of the node this transition leaves.")
                                    .required(true),
                                string(TO)
                                    .label("To")
                                    .description(
                                        "The name of the node to transition to, or an expression resolving to one.")
                                    .required(true),
                                string(CONDITION)
                                    .label("Condition")
                                    .description(
                                        "An expression that must evaluate to true for this transition to be taken. Absent or blank means unconditional.")
                                    .controlType(Property.ControlType.FORMULA_MODE))))
            .output(inputParameters -> taskListOutputDataSource
                .map(dataSource -> output(inputParameters, dataSource))
                .orElse(null))
            .taskProperties(
                array(NODES)
                    .description("The task nodes that make up the graph; each entry is one task.")
                    .items(task()));
    }

    @Override
    public TaskDispatcherDefinition getDefinition() {
        return taskDispatcherDefinition;
    }

    protected static OutputResponse output(
        Map<String, ?> inputParameters, TaskListOutputDataSource taskListOutputDataSource) {

        String workflowId = MapUtils.getString(inputParameters, WORKFLOW_ID);
        long environmentId = MapUtils.getLong(inputParameters, ENVIRONMENT_ID, 0L);

        List<Map<String, ?>> nodes = MapUtils.getList(inputParameters, NODES, new TypeReference<>() {}, List.of());
        List<Map<String, ?>> transitions = MapUtils.getList(
            inputParameters, TRANSITIONS, new TypeReference<>() {}, List.of());

        Map<String, ?> terminalNode = findFirstTerminalNode(nodes, transitions);

        if (terminalNode == null) {
            return null;
        }

        String terminalNodeName = MapUtils.getString(terminalNode, NAME);
        String terminalNodeType = MapUtils.getString(terminalNode, "type");

        if (terminalNodeType == null) {
            return null;
        }

        OutputResponse terminalNodeOutput = taskListOutputDataSource.getLastTaskOutput(
            workflowId, terminalNodeName, terminalNodeType, environmentId);

        if (terminalNodeOutput == null) {
            return null;
        }

        ModifiableValueProperty<?, ?> terminalNodeSchema =
            (ModifiableValueProperty<?, ?>) terminalNodeOutput.getOutputSchema();

        Object terminalNodeSampleOutput = terminalNodeOutput.getSampleOutput();

        if (terminalNodeSampleOutput != null) {
            return OutputResponse.of(terminalNodeSchema, terminalNodeSampleOutput);
        }

        return OutputResponse.of(terminalNodeSchema);
    }

    /**
     * Returns the first declared node that has no outgoing transition (no {@code transitions[].from} equal to its
     * name), or {@code null} when every node has one -- a documented approximation, since which node actually ends a
     * run is undecidable statically.
     */
    private static Map<String, ?> findFirstTerminalNode(
        List<Map<String, ?>> nodes, List<Map<String, ?>> transitions) {

        Set<String> sourceNodeNames = transitions.stream()
            .map(transition -> MapUtils.getString(transition, FROM))
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        for (Map<String, ?> node : nodes) {
            String nodeName = MapUtils.getString(node, NAME);

            if (nodeName != null && !sourceNodeNames.contains(nodeName)) {
                return node;
            }
        }

        return null;
    }
}
