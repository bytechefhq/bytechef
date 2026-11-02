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

package com.bytechef.task.dispatcher.fork.join;

import static com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDsl.string;
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
public class ForkJoinTaskDispatcherDefinitionFactoryOutputTest {

    @Test
    public void testOutputCarriesEachBranchSampleOutput() {
        Map<String, Object> inputParameters = Map.of(
            WORKFLOW_ID, "test-workflow",
            "branches", List.of(List.of(task("var_1")), List.of(task("var_2")), List.of(task("var_3"))));

        TaskListOutputDataSource dataSource = (workflowId, lastTaskName, lastTaskType, environmentId) -> Map.of(
            "var_1", OutputResponse.of(string(), "first"),
            "var_3", OutputResponse.of(string(), "third"))
            .get(lastTaskName);

        OutputResponse outputResponse = ForkJoinTaskDispatcherDefinitionFactory.output(inputParameters, dataSource);

        Assertions.assertEquals(Map.of("branch_0", "first", "branch_2", "third"), outputResponse.getSampleOutput());
    }

    @Test
    public void testOutputCarriesOnlySchemaWhenNoBranchHasSampleOutput() {
        Map<String, Object> inputParameters = Map.of(
            WORKFLOW_ID, "test-workflow",
            "branches", List.of(List.of(task("var_1"))));

        TaskListOutputDataSource dataSource = (workflowId, lastTaskName, lastTaskType, environmentId) -> OutputResponse
            .of(string());

        OutputResponse outputResponse = ForkJoinTaskDispatcherDefinitionFactory.output(inputParameters, dataSource);

        Assertions.assertNotNull(outputResponse.getOutputSchema());
        Assertions.assertNull(outputResponse.getSampleOutput());
    }

    @Test
    public void testOutputSkipsDisabledTrailingTasks() {
        Map<String, Object> inputParameters = Map.of(
            WORKFLOW_ID, "test-workflow",
            "branches", List.of(List.of(task("var_1"), disabledTask("var_2"))));

        TaskListOutputDataSource dataSource = (workflowId, lastTaskName, lastTaskType, environmentId) -> Map.of(
            "var_1", OutputResponse.of(string(), "enabled"),
            "var_2", OutputResponse.of(string(), "disabled"))
            .get(lastTaskName);

        OutputResponse outputResponse = ForkJoinTaskDispatcherDefinitionFactory.output(inputParameters, dataSource);

        Assertions.assertEquals(Map.of("branch_0", "enabled"), outputResponse.getSampleOutput());
    }

    private static Map<String, Object> disabledTask(String name) {
        return Map.of("name", name, "type", "var/v1/set", "disabled", true);
    }

    private static Map<String, Object> task(String name) {
        return Map.of("name", name, "type", "var/v1/set");
    }
}
