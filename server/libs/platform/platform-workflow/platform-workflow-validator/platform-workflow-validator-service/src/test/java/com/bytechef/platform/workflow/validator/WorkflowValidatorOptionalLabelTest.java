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

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.platform.workflow.validator.model.PropertyInfo;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author Ivica Cardic
 */
class WorkflowValidatorOptionalLabelTest {

    private static final Map<String, List<PropertyInfo>> TASK_DEFINITION_MAP = Map.of(
        "component/v1/action1", List.of(
            new PropertyInfo("name", "STRING", null, false, true, null, null)));

    @BeforeAll
    public static void beforeAll() {
        JsonUtils.setObjectMapper(
            JsonMapper.builder()
                .build());
    }

    @Test
    void validateWorkflowTasksMissingTaskLabelAddsWarningNotError() {
        String tasksJson = """
            [
                {
                    "name": "testTask",
                    "type": "component/v1/action1",
                    "parameters": {
                        "name": "John"
                    }
                }
            ]
            """;

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        validateTasks(tasksJson, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("[testTask] Missing recommended field: label", warnings.toString());
    }

    @Test
    void validateWorkflowTasksNonStringTaskLabelStillAddsError() {
        String tasksJson = """
            [
                {
                    "label": 42,
                    "name": "testTask",
                    "type": "component/v1/action1",
                    "parameters": {
                        "name": "John"
                    }
                }
            ]
            """;

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        validateTasks(tasksJson, errors, warnings);

        assertEquals("[testTask] Field 'label' must be a string", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateWorkflowMissingWorkflowLabelAddsWarningNotError() {
        String workflow = """
            {
                "description": "Test workflow description",
                "inputs": [],
                "triggers": [],
                "tasks": []
            }
            """;

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        validateWorkflow(workflow, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("Missing recommended field: label", warnings.toString());
    }

    @Test
    void validateWorkflowMissingInputLabelAddsWarningNotError() {
        String workflow = """
            {
                "label": "Test Workflow",
                "description": "Test workflow description",
                "inputs": [
                    {
                        "name": "count",
                        "type": "integer"
                    }
                ],
                "triggers": [],
                "tasks": []
            }
            """;

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        validateWorkflow(workflow, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("Missing recommended field: label", warnings.toString());
    }

    @Test
    void validateWorkflowMissingTriggerLabelAddsWarningNotError() {
        String workflow = """
            {
                "label": "Test Workflow",
                "description": "Test workflow description",
                "inputs": [],
                "triggers": [
                    {
                        "name": "trigger_1",
                        "type": "manual/v1/manual",
                        "parameters": {}
                    }
                ],
                "tasks": []
            }
            """;

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        validateWorkflow(workflow, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("[trigger_1] Missing recommended field: label", warnings.toString());
    }

    private static void validateTasks(String tasksJson, StringBuilder errors, StringBuilder warnings) {
        JsonNode tasksJsonNode = JsonUtils.readTree(tasksJson);
        List<JsonNode> taskJsonNodes = new ArrayList<>();

        for (JsonNode taskJsonNode : tasksJsonNode) {
            taskJsonNodes.add(taskJsonNode);
        }

        WorkflowValidator.validateWorkflowTasks(
            taskJsonNodes, TASK_DEFINITION_MAP, Map.of(), new HashMap<>(), errors, warnings);
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
