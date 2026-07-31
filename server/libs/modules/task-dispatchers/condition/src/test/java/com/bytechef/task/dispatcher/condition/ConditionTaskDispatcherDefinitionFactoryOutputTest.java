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

package com.bytechef.task.dispatcher.condition;

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
public class ConditionTaskDispatcherDefinitionFactoryOutputTest {

    private static final TaskListOutputDataSource DATA_SOURCE =
        (workflowId, lastTaskName, lastTaskType, environmentId) -> Map.of(
            "var_1", OutputResponse.of(string(), "yes"),
            "var_2", OutputResponse.of(string(), "no"))
            .get(lastTaskName);

    @Test
    public void testOutputIsTheCaseTrueOutputWhenTheExpressionHolds() {
        OutputResponse outputResponse = ConditionTaskDispatcherDefinitionFactory.output(
            rawExpressionParameters("2 > 1", List.of(task("var_1")), List.of(task("var_2"))), DATA_SOURCE);

        Assertions.assertEquals("yes", outputResponse.getSampleOutput());
    }

    @Test
    public void testOutputIsTheCaseFalseOutputWhenTheExpressionDoesNotHold() {
        OutputResponse outputResponse = ConditionTaskDispatcherDefinitionFactory.output(
            rawExpressionParameters("1 > 2", List.of(task("var_1")), List.of(task("var_2"))), DATA_SOURCE);

        Assertions.assertEquals("no", outputResponse.getSampleOutput());
    }

    @Test
    public void testOutputEvaluatesStructuredConditions() {
        Map<String, Object> inputParameters = Map.of(
            WORKFLOW_ID, "test-workflow",
            "conditions", List.of(List.of(
                Map.of("type", "number", "value1", 100, "operation", "GREATER", "value2", 200))),
            "caseTrue", List.of(task("var_1")),
            "caseFalse", List.of(task("var_2")));

        OutputResponse outputResponse = ConditionTaskDispatcherDefinitionFactory.output(inputParameters, DATA_SOURCE);

        Assertions.assertEquals("no", outputResponse.getSampleOutput());
    }

    @Test
    public void testOutputPreviewsCaseTrueWhenTheConditionIsUnresolved() {
        OutputResponse outputResponse = ConditionTaskDispatcherDefinitionFactory.output(
            rawExpressionParameters(
                "${trigger_1.count} > 1", List.of(task("var_1")), List.of(task("var_2"))),
            DATA_SOURCE);

        Assertions.assertEquals("yes", outputResponse.getSampleOutput());
    }

    @Test
    public void testOutputPreviewsCaseFalseWhenTheConditionIsUnresolvedAndCaseTrueIsEmpty() {
        OutputResponse outputResponse = ConditionTaskDispatcherDefinitionFactory.output(
            rawExpressionParameters("${trigger_1.count} > 1", List.of(), List.of(task("var_2"))), DATA_SOURCE);

        Assertions.assertEquals("no", outputResponse.getSampleOutput());
    }

    @Test
    public void testOutputSkipsDisabledTrailingTasks() {
        OutputResponse outputResponse = ConditionTaskDispatcherDefinitionFactory.output(
            rawExpressionParameters(
                "true",
                List.of(task("var_1"), Map.of("name", "var_2", "type", "var/v1/set", "disabled", true)),
                List.of()),
            DATA_SOURCE);

        Assertions.assertEquals("yes", outputResponse.getSampleOutput());
    }

    private static Map<String, Object> rawExpressionParameters(
        String expression, List<Map<String, Object>> caseTrueTasks, List<Map<String, Object>> caseFalseTasks) {

        return Map.of(
            WORKFLOW_ID, "test-workflow",
            ENVIRONMENT_ID, 1L,
            "rawExpression", true,
            "expression", expression,
            "caseTrue", caseTrueTasks,
            "caseFalse", caseFalseTasks);
    }

    private static Map<String, Object> task(String name) {
        return Map.of("name", name, "type", "var/v1/set");
    }
}
