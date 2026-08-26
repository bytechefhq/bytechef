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

package com.bytechef.platform.workflow.validator;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.bytechef.platform.workflow.validator.model.PropertyInfo;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class WorkflowValidatorOptionalParametersTest {

    private static final Map<String, List<PropertyInfo>> TASK_DEFINITION_MAP = Map.of(
        "component/v1/optionalPropertyAction", List.of(
            new PropertyInfo("name", "STRING", null, false, true, null, null)),
        "component/v1/requiredPropertyAction", List.of(
            new PropertyInfo("name", "STRING", null, true, true, null, null)));

    @Test
    void validateWorkflowTriggerWithoutParametersHasNoErrors() {
        String workflow = """
            {
                "label": "DateTime condition bug",
                "description": "",
                "inputs": [],
                "triggers": [
                    {
                        "description": "",
                        "label": "Manual",
                        "name": "trigger_1",
                        "type": "manual/v1/manual"
                    }
                ],
                "tasks": []
            }
            """;

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        validateWorkflow(workflow, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateWorkflowTaskWithoutParametersHasNoErrorsWhenNoPropertyIsRequired() {
        String workflow = """
            {
                "label": "Test Workflow",
                "description": "Test workflow description",
                "inputs": [],
                "triggers": [],
                "tasks": [
                    {
                        "label": "Test Task",
                        "name": "testTask",
                        "type": "component/v1/optionalPropertyAction"
                    }
                ]
            }
            """;

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        validateWorkflow(workflow, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateWorkflowTaskWithoutParametersReportsMissingRequiredPropertyByName() {
        String workflow = """
            {
                "label": "Test Workflow",
                "description": "Test workflow description",
                "inputs": [],
                "triggers": [],
                "tasks": [
                    {
                        "label": "Test Task",
                        "name": "testTask",
                        "type": "component/v1/requiredPropertyAction"
                    }
                ]
            }
            """;

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        validateWorkflow(workflow, errors, warnings);

        assertEquals("[testTask] Missing required property: name", errors.toString());
    }

    @Test
    void validateTaskStructureNonObjectParametersStillAddsError() {
        String task = """
            {
                "label": "Test Task",
                "name": "testTask",
                "type": "component/v1/optionalPropertyAction",
                "parameters": "notAnObject"
            }
            """;

        StringBuilder errors = new StringBuilder();

        TaskValidator.validateTaskStructure(task, errors, new StringBuilder());

        assertEquals("[testTask] Field 'parameters' must be an object", errors.toString());
    }

    private static void validateWorkflow(String workflow, StringBuilder errors, StringBuilder warnings) {
        WorkflowValidator.TaskDefinitionProvider taskDefinitionProvider =
            (taskType, kind) -> TASK_DEFINITION_MAP.get(taskType);
        WorkflowValidator.TaskOutputProvider taskOutputProvider =
            (taskType, kind, warningsBuilder) -> null;
        WorkflowValidator.ClusterTypesProvider clusterTypesProvider = taskType -> null;

        WorkflowValidator.validateWorkflow(
            workflow, taskDefinitionProvider, taskOutputProvider, clusterTypesProvider, new HashMap<>(),
            new HashMap<>(), new HashMap<>(), errors, warnings);
    }
}
