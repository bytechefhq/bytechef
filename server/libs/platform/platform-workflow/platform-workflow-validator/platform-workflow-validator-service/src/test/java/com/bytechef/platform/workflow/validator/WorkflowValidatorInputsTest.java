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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author Marko Kriskovic
 */
class WorkflowValidatorInputsTest {

    @BeforeAll
    public static void beforeAll() {
        JsonUtils.setObjectMapper(
            JsonMapper.builder()
                .build());
    }

    @Test
    void validateWorkflowValidWorkflowNoErrors() {
        String workflow = """
            {
                "label": "Test Workflow",
                "description": "Test workflow description",
                "inputs": [
                    {
                        "name": "any Name",
                        "label": "any Label",
                        "required": true,
                        "type": "string"
                    }
                ],
                "triggers": [
                    {
                        "label": "Manual Trigger",
                        "name": "trigger_1",
                        "type": "manual/v1/manual",
                        "parameters": {}
                    }
                ],
                "tasks": [
                    {
                        "label": "Test Task",
                        "name": "task_1",
                        "type": "component/v1/action1",
                        "parameters": {
                            "name": "${trigger_1.triggerResult}",
                            "surname": "${any Name}"
                        }
                    }
                ]
            }
            """;

        Map<String, List<PropertyInfo>> taskDefinitionMap = Map.of(
            "component/v1/action1", List.of(
                new PropertyInfo("name", "STRING", null, false, true, null, null),
                new PropertyInfo("surname", "STRING", null, false, true, null, null)),
            "manual/v1/manual", List.of(new PropertyInfo("name", "STRING", null, false, true, null, null)));

        Map<String, PropertyInfo> taskOutputMap = Map.of(
            "component/v1/action1", new PropertyInfo("actionResult", "STRING", null, false, false, null, null),
            "manual/v1/manual", new PropertyInfo("triggerResult", "STRING", null, false, false, null, null));

        Map<String, List<String>> clusterTypesMap = Map.of();

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        WorkflowValidator.TaskDefinitionProvider taskDefProvider = (taskType, kind) -> taskDefinitionMap.get(taskType);
        WorkflowValidator.TaskOutputProvider taskOutputProvider =
            (taskType, kind, warningsBuilder) -> taskOutputMap.get(taskType);
        WorkflowValidator.ClusterTypesProvider clusterTypesProvider = clusterTypesMap::get;

        WorkflowValidator.validateWorkflow(workflow, taskDefProvider, taskOutputProvider, clusterTypesProvider,
            new HashMap<>(), new HashMap<>(), new HashMap<>(), errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateWorkflowAllValidInputTypesNoErrors() {
        String workflow = """
            {
                "label": "Test Workflow",
                "description": "Test workflow description",
                "inputs": [
                    {
                        "name": "flag",
                        "label": "Flag",
                        "type": "boolean",
                        "required": false
                    },
                    {
                        "name": "day",
                        "label": "Day",
                        "required": true,
                        "type": "date"
                    },
                    {
                        "name": "startedAt",
                        "label": "Started At",
                        "required": true,
                        "type": "date_time"
                    },
                    {
                        "name": "startTime",
                        "label": "Start Time",
                        "required": false,
                        "type": "time"
                    },
                    {
                        "name": "count",
                        "label": "Count",
                        "required": true,
                        "type": "integer"
                    },
                    {
                        "name": "price",
                        "label": "Price",
                        "required": true,
                        "type": "number"
                    },
                    {
                        "name": "note",
                        "label": "Note",
                        "required": true,
                        "type": "string"
                    }
                ],
                "triggers": [
                    {
                        "label": "Manual Trigger",
                        "name": "trigger_1",
                        "type": "manual/v1/manual",
                        "parameters": {}
                    }
                ],
                "tasks": [
                    {
                        "label": "Test Task",
                        "name": "task_1",
                        "type": "component/v1/action1",
                        "parameters": {
                            "flag": "${flag}",
                            "day": "${day}",
                            "startedAt": "${startedAt}",
                            "startTime": "${startTime}",
                            "count": "${count}",
                            "price": "${price}",
                            "note": "${note}"
                        }
                    }
                ]
            }
            """;

        Map<String, List<PropertyInfo>> taskDefinitionMap = Map.of(
            "component/v1/action1", List.of(
                new PropertyInfo("flag", "BOOLEAN", null, false, true, null, null),
                new PropertyInfo("day", "DATE", null, false, true, null, null),
                new PropertyInfo("startedAt", "DATE_TIME", null, false, true, null, null),
                new PropertyInfo("startTime", "TIME", null, false, true, null, null),
                new PropertyInfo("count", "INTEGER", null, false, true, null, null),
                new PropertyInfo("price", "NUMBER", null, false, true, null, null),
                new PropertyInfo("note", "STRING", null, false, true, null, null)));

        Map<String, PropertyInfo> taskOutputMap = Map.of();
        Map<String, List<String>> clusterTypesMap = Map.of();

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        WorkflowValidator.TaskDefinitionProvider taskDefProvider = (taskType, kind) -> taskDefinitionMap.get(taskType);
        WorkflowValidator.TaskOutputProvider taskOutputProvider =
            (taskType, kind, warningsBuilder) -> taskOutputMap.get(taskType);
        WorkflowValidator.ClusterTypesProvider clusterTypesProvider = clusterTypesMap::get;

        WorkflowValidator.validateWorkflow(workflow, taskDefProvider, taskOutputProvider, clusterTypesProvider,
            new HashMap<>(), new HashMap<>(), new HashMap<>(), errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateWorkflowInputBareReferenceWrongTypeAddsError() {
        String workflow = """
            {
                "label": "Test Workflow",
                "description": "Test workflow description",
                "inputs": [
                    {
                        "name": "flag",
                        "label": "Flag",
                        "required": true,
                        "type": "boolean"
                    }
                ],
                "triggers": [
                    {
                        "label": "Manual Trigger",
                        "name": "trigger_1",
                        "type": "manual/v1/manual",
                        "parameters": {}
                    }
                ],
                "tasks": [
                    {
                        "label": "Test Task",
                        "name": "task_1",
                        "type": "component/v1/action1",
                        "parameters": {
                            "age": "${flag}"
                        }
                    }
                ]
            }
            """;

        Map<String, List<PropertyInfo>> taskDefinitionMap = Map.of(
            "component/v1/action1", List.of(
                new PropertyInfo("age", "INTEGER", null, false, true, null, null)));

        Map<String, PropertyInfo> taskOutputMap = Map.of();
        Map<String, List<String>> clusterTypesMap = Map.of();

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        WorkflowValidator.TaskDefinitionProvider taskDefProvider = (taskType, kind) -> taskDefinitionMap.get(taskType);
        WorkflowValidator.TaskOutputProvider taskOutputProvider =
            (taskType, kind, warningsBuilder) -> taskOutputMap.get(taskType);
        WorkflowValidator.ClusterTypesProvider clusterTypesProvider = clusterTypesMap::get;

        WorkflowValidator.validateWorkflow(workflow, taskDefProvider, taskOutputProvider, clusterTypesProvider,
            new HashMap<>(), new HashMap<>(), new HashMap<>(), errors, warnings);

        assertEquals(
            "[task_1] Input property 'flag' is of type boolean, not integer", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateWorkflowInvalidInputTypeHasErrors() {
        String workflow = """
            {
                "label": "Test Workflow",
                "description": "Test workflow description",
                "inputs": [
                    {
                        "name": "count",
                        "label": "Count",
                        "type": "float"
                    }
                ],
                "triggers": [],
                "tasks": []
            }
            """;

        Map<String, List<PropertyInfo>> taskDefinitionMap = Map.of();
        Map<String, PropertyInfo> taskOutputMap = Map.of();
        Map<String, List<String>> clusterTypesMap = Map.of();

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        WorkflowValidator.TaskDefinitionProvider taskDefProvider = (taskType, kind) -> taskDefinitionMap.get(taskType);
        WorkflowValidator.TaskOutputProvider taskOutputProvider =
            (taskType, kind, warningsBuilder) -> taskOutputMap.get(taskType);
        WorkflowValidator.ClusterTypesProvider clusterTypesProvider = clusterTypesMap::get;

        WorkflowValidator.validateWorkflow(workflow, taskDefProvider, taskOutputProvider, clusterTypesProvider,
            new HashMap<>(), new HashMap<>(), new HashMap<>(), errors, warnings);

        assertEquals(
            "[count] Field 'type' must be one of: boolean, date, date_time, time, integer, number, string",
            errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateWorkflowMissingInputNameHasErrors() {
        String workflow = """
            {
                "label": "Test Workflow",
                "description": "Test workflow description",
                "inputs": [
                    {
                        "label": "Count",
                        "type": "integer"
                    }
                ],
                "triggers": [],
                "tasks": []
            }
            """;

        Map<String, List<PropertyInfo>> taskDefinitionMap = Map.of();
        Map<String, PropertyInfo> taskOutputMap = Map.of();
        Map<String, List<String>> clusterTypesMap = Map.of();

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        WorkflowValidator.TaskDefinitionProvider taskDefProvider = (taskType, kind) -> taskDefinitionMap.get(taskType);
        WorkflowValidator.TaskOutputProvider taskOutputProvider =
            (taskType, kind, warningsBuilder) -> taskOutputMap.get(taskType);
        WorkflowValidator.ClusterTypesProvider clusterTypesProvider = clusterTypesMap::get;

        WorkflowValidator.validateWorkflow(workflow, taskDefProvider, taskOutputProvider, clusterTypesProvider,
            new HashMap<>(), new HashMap<>(), new HashMap<>(), errors, warnings);

        assertEquals("Missing required field: name", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateWorkflowMissingInputLabelHasErrors() {
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

        Map<String, List<PropertyInfo>> taskDefinitionMap = Map.of();
        Map<String, PropertyInfo> taskOutputMap = Map.of();
        Map<String, List<String>> clusterTypesMap = Map.of();

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        WorkflowValidator.TaskDefinitionProvider taskDefProvider = (taskType, kind) -> taskDefinitionMap.get(taskType);
        WorkflowValidator.TaskOutputProvider taskOutputProvider =
            (taskType, kind, warningsBuilder) -> taskOutputMap.get(taskType);
        WorkflowValidator.ClusterTypesProvider clusterTypesProvider = clusterTypesMap::get;

        WorkflowValidator.validateWorkflow(workflow, taskDefProvider, taskOutputProvider, clusterTypesProvider,
            new HashMap<>(), new HashMap<>(), new HashMap<>(), errors, warnings);

        assertEquals("Missing required field: label", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateWorkflowMissingInputTypeHasErrors() {
        String workflow = """
            {
                "label": "Test Workflow",
                "description": "Test workflow description",
                "inputs": [
                    {
                        "name": "count",
                        "label": "Count"
                    }
                ],
                "triggers": [],
                "tasks": []
            }
            """;

        Map<String, List<PropertyInfo>> taskDefinitionMap = Map.of();
        Map<String, PropertyInfo> taskOutputMap = Map.of();
        Map<String, List<String>> clusterTypesMap = Map.of();

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        WorkflowValidator.TaskDefinitionProvider taskDefProvider = (taskType, kind) -> taskDefinitionMap.get(taskType);
        WorkflowValidator.TaskOutputProvider taskOutputProvider =
            (taskType, kind, warningsBuilder) -> taskOutputMap.get(taskType);
        WorkflowValidator.ClusterTypesProvider clusterTypesProvider = clusterTypesMap::get;

        WorkflowValidator.validateWorkflow(workflow, taskDefProvider, taskOutputProvider, clusterTypesProvider,
            new HashMap<>(), new HashMap<>(), new HashMap<>(), errors, warnings);

        assertEquals("[count] Missing required field: type", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateWorkflowInputsNotArrayHasErrors() {
        String workflow = """
            {
                "label": "Test Workflow",
                "description": "Test workflow description",
                "inputs": {},
                "triggers": [],
                "tasks": []
            }
            """;

        Map<String, List<PropertyInfo>> taskDefinitionMap = Map.of();
        Map<String, PropertyInfo> taskOutputMap = Map.of();
        Map<String, List<String>> clusterTypesMap = Map.of();

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        WorkflowValidator.TaskDefinitionProvider taskDefProvider = (taskType, kind) -> taskDefinitionMap.get(taskType);
        WorkflowValidator.TaskOutputProvider taskOutputProvider =
            (taskType, kind, warningsBuilder) -> taskOutputMap.get(taskType);
        WorkflowValidator.ClusterTypesProvider clusterTypesProvider = clusterTypesMap::get;

        WorkflowValidator.validateWorkflow(workflow, taskDefProvider, taskOutputProvider, clusterTypesProvider,
            new HashMap<>(), new HashMap<>(), new HashMap<>(), errors, warnings);

        assertEquals("Field 'inputs' must be an array", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateWorkflowInputElementNotObjectHasErrors() {
        String workflow = """
            {
                "label": "Test Workflow",
                "description": "Test workflow description",
                "inputs": ["count"],
                "triggers": [],
                "tasks": []
            }
            """;

        Map<String, List<PropertyInfo>> taskDefinitionMap = Map.of();
        Map<String, PropertyInfo> taskOutputMap = Map.of();
        Map<String, List<String>> clusterTypesMap = Map.of();

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        WorkflowValidator.TaskDefinitionProvider taskDefProvider = (taskType, kind) -> taskDefinitionMap.get(taskType);
        WorkflowValidator.TaskOutputProvider taskOutputProvider =
            (taskType, kind, warningsBuilder) -> taskOutputMap.get(taskType);
        WorkflowValidator.ClusterTypesProvider clusterTypesProvider = clusterTypesMap::get;

        WorkflowValidator.validateWorkflow(workflow, taskDefProvider, taskOutputProvider, clusterTypesProvider,
            new HashMap<>(), new HashMap<>(), new HashMap<>(), errors, warnings);

        assertEquals("Input must be an object", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateWorkflowUndefinedBareReferenceHasErrors() {
        String workflow = """
            {
                "label": "Test Workflow",
                "description": "Test workflow description",
                "triggers": [
                    {
                        "label": "Manual Trigger",
                        "name": "trigger_1",
                        "type": "manual/v1/manual",
                        "parameters": {}
                    }
                ],
                "tasks": [
                    {
                        "label": "Test Task",
                        "name": "task_1",
                        "type": "component/v1/action1",
                        "parameters": {
                            "surname": "${doesNotExist}"
                        }
                    }
                ]
            }
            """;

        Map<String, List<PropertyInfo>> taskDefinitionMap = Map.of(
            "component/v1/action1", List.of(
                new PropertyInfo("surname", "STRING", null, false, true, null, null)));

        Map<String, PropertyInfo> taskOutputMap = Map.of();
        Map<String, List<String>> clusterTypesMap = Map.of();

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        WorkflowValidator.TaskDefinitionProvider taskDefProvider = (taskType, kind) -> taskDefinitionMap.get(taskType);
        WorkflowValidator.TaskOutputProvider taskOutputProvider =
            (taskType, kind, warningsBuilder) -> taskOutputMap.get(taskType);
        WorkflowValidator.ClusterTypesProvider clusterTypesProvider = clusterTypesMap::get;

        WorkflowValidator.validateWorkflow(workflow, taskDefProvider, taskOutputProvider, clusterTypesProvider,
            new HashMap<>(), new HashMap<>(), new HashMap<>(), errors, warnings);

        assertEquals("[task_1] Task 'doesNotExist' doesn't exits.", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateWorkflowInputDotPropertyReferenceHasWarning() {
        String workflow = """
            {
                "label": "Test Workflow",
                "description": "Test workflow description",
                "inputs": [
                    {
                        "name": "any Name",
                        "label": "any Label",
                        "type": "string"
                    }
                ],
                "triggers": [
                    {
                        "label": "Manual Trigger",
                        "name": "trigger_1",
                        "type": "manual/v1/manual",
                        "parameters": {}
                    }
                ],
                "tasks": [
                    {
                        "label": "Test Task",
                        "name": "task_1",
                        "type": "component/v1/action1",
                        "parameters": {
                            "surname": "${any Name.foo}"
                        }
                    }
                ]
            }
            """;

        Map<String, List<PropertyInfo>> taskDefinitionMap = Map.of(
            "component/v1/action1", List.of(
                new PropertyInfo("surname", "STRING", null, false, true, null, null)));

        Map<String, PropertyInfo> taskOutputMap = Map.of();
        Map<String, List<String>> clusterTypesMap = Map.of();

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        WorkflowValidator.TaskDefinitionProvider taskDefProvider = (taskType, kind) -> taskDefinitionMap.get(taskType);
        WorkflowValidator.TaskOutputProvider taskOutputProvider =
            (taskType, kind, warningsBuilder) -> taskOutputMap.get(taskType);
        WorkflowValidator.ClusterTypesProvider clusterTypesProvider = clusterTypesMap::get;

        WorkflowValidator.validateWorkflow(workflow, taskDefProvider, taskOutputProvider, clusterTypesProvider,
            new HashMap<>(), new HashMap<>(), new HashMap<>(), errors, warnings);

        assertEquals("", errors.toString());
        assertEquals(
            "[task_1] Property 'any Name.foo' might not exist in the output of 'string'", warnings.toString());
    }

    @Test
    void validateWorkflowInputBareReferenceResolvesDisplayConditionedPropertyNoErrors() {
        String workflow = """
            {
                "label": "error-handler",
                "description": "",
                "inputs": [
                    {
                        "name": "any Name",
                        "label": "any Label",
                        "type": "boolean"
                    }
                ],
                "triggers": [
                    {
                        "description": "",
                        "label": "Manual",
                        "name": "trigger_1",
                        "parameters": {},
                        "type": "manual/v1/manual"
                    }
                ],
                "tasks": [
                    {
                        "label": "Var",
                        "name": "var_1",
                        "parameters": {
                            "type": "STRING",
                            "value": "${any Name}"
                        },
                        "type": "var/v1/set"
                    }
                ]
            }
            """;

        Map<String, List<PropertyInfo>> taskDefinitionMap = Map.of(
            "manual/v1/manual", List.of(),
            "var/v1/set", List.of(
                new PropertyInfo("type", "STRING", null, false, true, null, null),
                new PropertyInfo("value", "ARRAY", null, true, true, "type == 'ARRAY'", null),
                new PropertyInfo("value", "BOOLEAN", null, true, true, "type == 'BOOLEAN'", null),
                new PropertyInfo("value", "DATE", null, true, true, "type == 'DATE'", null),
                new PropertyInfo("value", "DATE_TIME", null, true, true, "type == 'DATE_TIME'", null),
                new PropertyInfo("value", "INTEGER", null, true, true, "type == 'INTEGER'", null),
                new PropertyInfo("value", "NUMBER", null, true, true, "type == 'NUMBER'", null),
                new PropertyInfo("value", "OBJECT", null, true, true, "type == 'OBJECT'", null),
                new PropertyInfo("value", "STRING", null, true, true, "type == 'STRING'", null),
                new PropertyInfo("value", "TIME", null, true, true, "type == 'TIME'", null)));

        Map<String, PropertyInfo> taskOutputMap = Map.of();
        Map<String, List<String>> clusterTypesMap = Map.of();

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        WorkflowValidator.TaskDefinitionProvider taskDefProvider = (taskType, kind) -> taskDefinitionMap.get(taskType);
        WorkflowValidator.TaskOutputProvider taskOutputProvider =
            (taskType, kind, warningsBuilder) -> taskOutputMap.get(taskType);
        WorkflowValidator.ClusterTypesProvider clusterTypesProvider = clusterTypesMap::get;

        WorkflowValidator.validateWorkflow(workflow, taskDefProvider, taskOutputProvider, clusterTypesProvider,
            new HashMap<>(), new HashMap<>(), new HashMap<>(), errors, warnings);

        // 'value' is a discriminated union (one PropertyInfo per "type"); the expected type must resolve to the
        // STRING candidate whose displayCondition matches the task's actual "type": "STRING", not the first "value"
        // entry in definition order (ARRAY). A boolean input bound into a STRING field is fine - it gets stringified.
        assertEquals("", errors.toString());
        assertEquals("", warnings.toString());
    }
}
