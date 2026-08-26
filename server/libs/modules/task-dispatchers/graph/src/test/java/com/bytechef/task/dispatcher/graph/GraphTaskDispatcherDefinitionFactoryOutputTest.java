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

import static com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDsl.string;
import static com.bytechef.platform.workflow.task.dispatcher.output.TaskListOutputDataSource.ENVIRONMENT_ID;
import static com.bytechef.platform.workflow.task.dispatcher.output.TaskListOutputDataSource.WORKFLOW_ID;
import static com.bytechef.task.dispatcher.graph.constant.GraphTaskDispatcherConstants.FROM;
import static com.bytechef.task.dispatcher.graph.constant.GraphTaskDispatcherConstants.NAME;
import static com.bytechef.task.dispatcher.graph.constant.GraphTaskDispatcherConstants.NODES;
import static com.bytechef.task.dispatcher.graph.constant.GraphTaskDispatcherConstants.TO;
import static com.bytechef.task.dispatcher.graph.constant.GraphTaskDispatcherConstants.TRANSITIONS;

import com.bytechef.definition.BaseOutputDefinition.OutputResponse;
import com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDsl.ModifiableStringProperty;
import com.bytechef.platform.workflow.task.dispatcher.output.TaskListOutputDataSource;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Pins {@link GraphTaskDispatcherDefinitionFactory#output(Map, TaskListOutputDataSource)} and its private
 * {@code findFirstTerminalNode} helper against real {@code nodes}/{@code transitions} input, as opposed to
 * {@link GraphTaskDispatcherDefinitionFactoryTest}, whose stub data source always returns {@code null} and so never
 * exercises this logic.
 *
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
public class GraphTaskDispatcherDefinitionFactoryOutputTest {

    private static final String TEST_WORKFLOW_ID = "test-workflow";
    private static final long TEST_ENVIRONMENT_ID = 42L;

    @Test
    public void testOutputWhenTwoNodesHaveNoOutgoingTransitionChoosesFirstDeclared() {
        Map<String, Object> inputParameters = inputParameters(
            List.of(node("classify", "print"), node("review", "print"), node("archive", "print")),
            List.of(transition("classify", "review")));

        RecordingTaskListOutputDataSource dataSource = new RecordingTaskListOutputDataSource(
            OutputResponse.of(string("out")));

        output(inputParameters, dataSource);

        Assertions.assertEquals("review", dataSource.queriedTaskName);
        Assertions.assertEquals("print", dataSource.queriedTaskType);
    }

    @Test
    public void testOutputWhenEveryNodeHasOutgoingTransitionReturnsNull() {
        Map<String, Object> inputParameters = inputParameters(
            List.of(node("a", "print"), node("b", "print")),
            List.of(transition("a", "b"), transition("b", "a")));

        RecordingTaskListOutputDataSource dataSource = new RecordingTaskListOutputDataSource(
            OutputResponse.of(string("out")));

        OutputResponse outputResponse = output(inputParameters, dataSource);

        Assertions.assertNull(outputResponse);
        Assertions.assertFalse(dataSource.invoked, "the data source must not be queried when no node is terminal");
    }

    @Test
    public void testOutputWhenChosenTerminalNodeHasNoTypeReturnsNull() {
        Map<String, Object> inputParameters = inputParameters(
            List.of(node("unset", null), node("b", "print")),
            List.of(transition("b", "unset")));

        RecordingTaskListOutputDataSource dataSource = new RecordingTaskListOutputDataSource(
            OutputResponse.of(string("out")));

        OutputResponse outputResponse = output(inputParameters, dataSource);

        Assertions.assertNull(outputResponse);
        Assertions.assertFalse(
            dataSource.invoked, "the data source must not be queried for a terminal node without a type");
    }

    @Test
    public void testOutputWhenDataSourceYieldsNoOutputReturnsNull() {
        Map<String, Object> inputParameters = inputParameters(List.of(node("a", "print")), List.of());

        RecordingTaskListOutputDataSource dataSource = new RecordingTaskListOutputDataSource(null);

        OutputResponse outputResponse = output(inputParameters, dataSource);

        Assertions.assertNull(outputResponse);
        Assertions.assertEquals("a", dataSource.queriedTaskName);
    }

    @Test
    public void testOutputCarriesSampleOutputWhenDataSourceSuppliesOne() {
        Map<String, Object> inputParameters = inputParameters(List.of(node("a", "print")), List.of());

        ModifiableStringProperty schema = string("out");
        Map<String, ?> sampleOutput = Map.of("key", "value");

        RecordingTaskListOutputDataSource dataSource = new RecordingTaskListOutputDataSource(
            OutputResponse.of(schema, sampleOutput));

        OutputResponse outputResponse = output(inputParameters, dataSource);

        Assertions.assertEquals(OutputResponse.of(schema, sampleOutput), outputResponse);
        Assertions.assertEquals(TEST_WORKFLOW_ID, dataSource.queriedWorkflowId);
        Assertions.assertEquals(TEST_ENVIRONMENT_ID, dataSource.queriedEnvironmentId);
    }

    @Test
    public void testOutputCarriesOnlySchemaWhenDataSourceSuppliesNoSample() {
        Map<String, Object> inputParameters = inputParameters(List.of(node("a", "print")), List.of());

        ModifiableStringProperty schema = string("out");

        RecordingTaskListOutputDataSource dataSource = new RecordingTaskListOutputDataSource(
            OutputResponse.of(schema));

        OutputResponse outputResponse = output(inputParameters, dataSource);

        Assertions.assertEquals(OutputResponse.of(schema), outputResponse);
    }

    private static OutputResponse output(
        Map<String, ?> inputParameters, TaskListOutputDataSource taskListOutputDataSource) {

        return GraphTaskDispatcherDefinitionFactory.output(inputParameters, taskListOutputDataSource);
    }

    private static Map<String, Object> inputParameters(
        List<Map<String, ?>> nodes, List<Map<String, ?>> transitions) {

        Map<String, Object> inputParameters = new LinkedHashMap<>();

        inputParameters.put(NODES, nodes);
        inputParameters.put(TRANSITIONS, transitions);
        inputParameters.put(WORKFLOW_ID, TEST_WORKFLOW_ID);
        inputParameters.put(ENVIRONMENT_ID, TEST_ENVIRONMENT_ID);

        return inputParameters;
    }

    private static Map<String, ?> node(String name, @Nullable String type) {
        Map<String, Object> node = new LinkedHashMap<>();

        node.put(NAME, name);

        if (type != null) {
            node.put("type", type);
        }

        return node;
    }

    private static Map<String, ?> transition(String from, String to) {
        return Map.of(FROM, from, TO, to);
    }

    private static final class RecordingTaskListOutputDataSource implements TaskListOutputDataSource {

        private final OutputResponse response;

        private boolean invoked;
        private String queriedTaskName;
        private String queriedTaskType;
        private String queriedWorkflowId;
        private long queriedEnvironmentId;

        private RecordingTaskListOutputDataSource(OutputResponse response) {
            this.response = response;
        }

        @Override
        public OutputResponse getLastTaskOutput(
            String workflowId, String lastTaskName, String lastTaskType, long environmentId) {

            invoked = true;
            queriedWorkflowId = workflowId;
            queriedTaskName = lastTaskName;
            queriedTaskType = lastTaskType;
            queriedEnvironmentId = environmentId;

            return response;
        }
    }
}
