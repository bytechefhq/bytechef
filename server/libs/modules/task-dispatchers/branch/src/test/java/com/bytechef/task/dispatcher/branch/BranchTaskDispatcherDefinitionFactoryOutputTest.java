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

package com.bytechef.task.dispatcher.branch;

import static com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDsl.string;
import static com.bytechef.platform.workflow.task.dispatcher.output.TaskListOutputDataSource.ENVIRONMENT_ID;
import static com.bytechef.platform.workflow.task.dispatcher.output.TaskListOutputDataSource.WORKFLOW_ID;

import com.bytechef.definition.BaseOutputDefinition.OutputResponse;
import com.bytechef.platform.workflow.task.dispatcher.output.TaskListOutputDataSource;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
public class BranchTaskDispatcherDefinitionFactoryOutputTest {

    private static final TaskListOutputDataSource DATA_SOURCE =
        (workflowId, lastTaskName, lastTaskType, environmentId) -> Map.of(
            "var_1", OutputResponse.of(string(), "first"),
            "var_2", OutputResponse.of(string(), "second"),
            "var_3", OutputResponse.of(string(), "fallback"))
            .get(lastTaskName);

    @Test
    public void testOutputIsTheLastTaskOutputOfTheMatchingCase() {
        OutputResponse outputResponse = BranchTaskDispatcherDefinitionFactory.output(
            branchParameters("k2", List.of(task("var_3"))), DATA_SOURCE);

        Assertions.assertEquals("second", outputResponse.getSampleOutput());
    }

    @Test
    public void testOutputFallsBackToTheDefaultWhenNoCaseMatches() {
        OutputResponse outputResponse = BranchTaskDispatcherDefinitionFactory.output(
            branchParameters("k9", List.of(task("var_3"))), DATA_SOURCE);

        Assertions.assertEquals("fallback", outputResponse.getSampleOutput());
    }

    @Test
    public void testOutputPreviewsTheFirstCaseWithTasksWhenTheExpressionIsUnresolved() {
        OutputResponse outputResponse = BranchTaskDispatcherDefinitionFactory.output(
            branchParameters("${trigger_1.status}", List.of(task("var_3"))), DATA_SOURCE);

        Assertions.assertEquals("first", outputResponse.getSampleOutput());
    }

    @Test
    public void testOutputIsEmptyWhenTheSelectedCaseHasNoTasks() {
        Map<String, Object> inputParameters = Map.of(
            WORKFLOW_ID, "test-workflow",
            "expression", "k1",
            "cases", List.of(Map.of("key", "k1", "tasks", List.of())),
            "default", List.of(task("var_3")));

        TaskListOutputDataSource dataSource = (workflowId, lastTaskName, lastTaskType, environmentId) -> {
            throw new AssertionError("the data source must not be queried for a case without tasks");
        };

        Assertions.assertNull(BranchTaskDispatcherDefinitionFactory.output(inputParameters, dataSource));
    }

    @Test
    public void testOutputSkipsDisabledTrailingTasks() {
        Map<String, Object> inputParameters = Map.of(
            WORKFLOW_ID, "test-workflow",
            "expression", "k1",
            "cases", List.of(
                Map.of("key", "k1", "tasks", List.of(task("var_1"), Map.of(
                    "name", "var_2", "type", "var/v1/set", "disabled", true)))),
            "default", List.of());

        OutputResponse outputResponse = BranchTaskDispatcherDefinitionFactory.output(inputParameters, DATA_SOURCE);

        Assertions.assertEquals("first", outputResponse.getSampleOutput());
    }

    private static Map<String, Object> branchParameters(Object expression, List<Map<String, Object>> defaultTasks) {
        return Map.of(
            WORKFLOW_ID, "test-workflow",
            ENVIRONMENT_ID, 1L,
            "expression", expression,
            "cases", List.of(caseOf("k1", "var_1"), caseOf("k2", "var_2")),
            "default", defaultTasks);
    }

    private static Map<String, Object> caseOf(String key, String taskName) {
        return Map.of("key", key, "tasks", List.of(task(taskName)));
    }

    private static Map<String, Object> task(String name) {
        return Map.of("name", name, "type", "var/v1/set");
    }
}
