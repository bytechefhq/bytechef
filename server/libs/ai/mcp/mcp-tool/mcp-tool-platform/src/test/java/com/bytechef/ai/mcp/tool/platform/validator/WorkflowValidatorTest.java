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

package com.bytechef.ai.mcp.tool.platform.validator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.bytechef.ai.mcp.tool.platform.util.ToolUtils;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class WorkflowValidatorTest {
    private static final ToolUtils.PropertyInfo trigger1 =
        new ToolUtils.PropertyInfo("propString", "STRING", null, false, true, null, null);
    private static final ToolUtils.PropertyInfo action1 =
        new ToolUtils.PropertyInfo("propBool", "BOOLEAN", null, false, true, null, null);
    private static final ToolUtils.PropertyInfo action2 =
        new ToolUtils.PropertyInfo("propNumber", "NUMBER", null, false, true, null, null);
    private static final ToolUtils.PropertyInfo action3 =
        new ToolUtils.PropertyInfo("propInteger", "INTEGER", null, false, true, null, null);
    private static final ToolUtils.PropertyInfo action4 =
        new ToolUtils.PropertyInfo("propDateTime", "DATE_TIME", null, false, true, null, null);
    private static final ToolUtils.PropertyInfo actionObj = new ToolUtils.PropertyInfo(
        "element", "OBJECT", null, false, true, null, List.of(
            action1, action2, action3));
    private static final ToolUtils.PropertyInfo actionArr = new ToolUtils.PropertyInfo(
        "elements", "ARRAY", null, false, true, null, List.of(
            new ToolUtils.PropertyInfo(null, "OBJECT", null, false, true, null, List.of(
                action1, action2, action3))));

    @Test
    void validateWorkflowStructure_validWorkflow_noErrors() {
        String validWorkflow = """
            {
                "label": "workflowName",
                "description": "workflowDescription",
                "inputs": [],
                "triggers": [
                    {
                        "label": "Manual",
                        "name": "trigger_1",
                        "type": "manual/v1/manual"
                    }
                ],
                "tasks": []
            }
            """;

        StringBuilder errors = new StringBuilder();
        WorkflowValidator.validateWorkflowStructure(validWorkflow, errors);

        assertEquals("", errors.toString());
    }

    @Test
    void validateWorkflowStructure_missingLabel_addsError() {
        String invalidWorkflow = """
            {
                "description": "workflowDescription",
                "inputs": [],
                "triggers": [
                    {
                        "label": "Manual",
                        "name": "trigger_1",
                        "type": "manual/v1/manual"
                    }
                ],
                "tasks": []
            }
            """;

        StringBuilder errors = new StringBuilder();
        WorkflowValidator.validateWorkflowStructure(invalidWorkflow, errors);

        assertEquals("Missing required field: label", errors.toString());
    }

    @Test
    void validateWorkflowStructure_nonStringLabel_addsError() {
        String invalidWorkflow = """
            {
                "label": 123,
                "description": "workflowDescription",
                "inputs": [],
                "triggers": [
                    {
                        "label": "Manual",
                        "name": "trigger_1",
                        "type": "manual/v1/manual"
                    }
                ],
                "tasks": []
            }
            """;

        StringBuilder errors = new StringBuilder();
        WorkflowValidator.validateWorkflowStructure(invalidWorkflow, errors);

        assertEquals("Field 'label' must be a string", errors.toString());
    }

    @Test
    void validateWorkflowStructure_missingDescription_addsError() {
        String invalidWorkflow = """
            {
                "label": "workflowName",
                "inputs": [],
                "triggers": [
                    {
                        "label": "Manual",
                        "name": "trigger_1",
                        "type": "manual/v1/manual"
                    }
                ],
                "tasks": []
            }
            """;

        StringBuilder errors = new StringBuilder();
        WorkflowValidator.validateWorkflowStructure(invalidWorkflow, errors);

        assertEquals("Missing required field: description", errors.toString());
    }

    @Test
    void validateWorkflowStructure_nonStringDescription_addsError() {
        String invalidWorkflow = """
            {
                "label": "workflowName",
                "description": true,
                "inputs": [],
                "triggers": [
                    {
                        "label": "Manual",
                        "name": "trigger_1",
                        "type": "manual/v1/manual"
                    }
                ],
                "tasks": []
            }
            """;

        StringBuilder errors = new StringBuilder();
        WorkflowValidator.validateWorkflowStructure(invalidWorkflow, errors);

        assertEquals("Field 'description' must be a string", errors.toString());
    }

    @Test
    void validateWorkflowStructure_missingTriggers_addsError() {
        String invalidWorkflow = """
            {
                "label": "workflowName",
                "description": "workflowDescription",
                "inputs": [],
                "tasks": []
            }
            """;

        StringBuilder errors = new StringBuilder();
        WorkflowValidator.validateWorkflowStructure(invalidWorkflow, errors);

        assertEquals("Missing required field: triggers", errors.toString());
    }

    @Test
    void validateWorkflowStructure_nonArrayTriggers_addsError() {
        String invalidWorkflow = """
            {
                "label": "workflowName",
                "description": "workflowDescription",
                "inputs": [],
                "triggers": "not an array",
                "tasks": []
            }
            """;

        StringBuilder errors = new StringBuilder();
        WorkflowValidator.validateWorkflowStructure(invalidWorkflow, errors);

        assertEquals("Field 'triggers' must be an array", errors.toString());
    }

    @Test
    void validateWorkflowStructure_multipleTriggers_addsError() {
        String invalidWorkflow = """
            {
                "label": "workflowName",
                "description": "workflowDescription",
                "inputs": [],
                "triggers": [
                    {
                        "label": "Manual",
                        "name": "trigger_1",
                        "type": "manual/v1/manual"
                    },
                    {
                        "label": "Manual",
                        "name": "trigger_2",
                        "type": "manual/v1/manual"
                    }
                ],
                "tasks": []
            }
            """;

        StringBuilder errors = new StringBuilder();
        WorkflowValidator.validateWorkflowStructure(invalidWorkflow, errors);

        assertEquals("Field 'triggers' must contain one or less objects", errors.toString());
    }

    @Test
    void validateWorkflowStructure_nonObjectTrigger_addsError() {
        String invalidWorkflow = """
            {
                "label": "workflowName",
                "description": "workflowDescription",
                "inputs": [],
                "triggers": [
                   "manual/v1/manual"
                ],
                "tasks": []
            }
            """;

        StringBuilder errors = new StringBuilder();
        WorkflowValidator.validateWorkflowStructure(invalidWorkflow, errors);

        assertEquals("Trigger must be an object", errors.toString());
    }

    @Test
    void validateWorkflowStructure_missingTasks_addsError() {
        String invalidWorkflow = """
            {
                "label": "workflowName",
                "description": "workflowDescription",
                "inputs": [],
                "triggers": [
                    {
                        "label": "Manual",
                        "name": "trigger_1",
                        "type": "manual/v1/manual"
                    }
                ]
            }
            """;

        StringBuilder errors = new StringBuilder();
        WorkflowValidator.validateWorkflowStructure(invalidWorkflow, errors);

        assertEquals("Missing required field: tasks", errors.toString());
    }

    @Test
    void validateWorkflowStructure_nonArrayTasks_addsError() {
        String invalidWorkflow = """
            {
                "label": "workflowName",
                "description": "workflowDescription",
                "inputs": [],
                "triggers": [
                    {
                        "label": "Manual",
                        "name": "trigger_1",
                        "type": "manual/v1/manual"
                    }
                ],
                "tasks": "not an array"
            }
            """;

        StringBuilder errors = new StringBuilder();
        WorkflowValidator.validateWorkflowStructure(invalidWorkflow, errors);

        assertEquals("Field 'tasks' must be an array", errors.toString());
    }

    @Test
    void validateWorkflowStructure_nonObject_addsError() {
        String invalidWorkflow = "\"not an object\"";

        StringBuilder errors = new StringBuilder();
        WorkflowValidator.validateWorkflowStructure(invalidWorkflow, errors);

        assertTrue(errors.toString()
            .contains("Workflow must be an object"));
    }

    @Test
    void validateWorkflowStructure_invalidJson_addsError() {
        String invalidWorkflow = "{invalid json}";

        StringBuilder errors = new StringBuilder();
        WorkflowValidator.validateWorkflowStructure(invalidWorkflow, errors);

        assertTrue(errors.toString()
            .contains("Invalid JSON format:"), errors.toString());
    }

    @Test
    void validateTaskStructure_validTask_noErrors() {
        String validTask = """
            {
                "label": "Test Task",
                "name": "testTask",
                "type": "component/v1/action",
                "parameters": {}
            }
            """;

        StringBuilder errors = new StringBuilder();
        WorkflowValidator.validateTaskStructure(validTask, errors);

        assertEquals("", errors.toString());
    }

    @Test
    void validateTaskStructure_missingLabel_addsError() {
        String invalidTask = """
            {
                "name": "testTask",
                "type": "component/v1/action",
                "parameters": {}
            }
            """;

        StringBuilder errors = new StringBuilder();
        WorkflowValidator.validateTaskStructure(invalidTask, errors);

        assertEquals("Missing required field: label", errors.toString());
    }

    @Test
    void validateTaskStructure_nonStringLabel_addsError() {
        String invalidTask = """
            {
                "label": 123,
                "name": "testTask",
                "type": "component/v1/action",
                "parameters": {}
            }
            """;

        StringBuilder errors = new StringBuilder();
        WorkflowValidator.validateTaskStructure(invalidTask, errors);

        assertEquals("Field 'label' must be a string", errors.toString());
    }

    @Test
    void validateTaskStructure_missingName_addsError() {
        String invalidTask = """
            {
                "label": "Test Task",
                "type": "component/v1/action",
                "parameters": {}
            }
            """;

        StringBuilder errors = new StringBuilder();
        WorkflowValidator.validateTaskStructure(invalidTask, errors);

        assertEquals("Missing required field: name", errors.toString());
    }

    @Test
    void validateTaskStructure_nonStringName_addsError() {
        String invalidTask = """
            {
                "label": "Test Task",
                "name": 123,
                "type": "component/v1/action",
                "parameters": {}
            }
            """;

        StringBuilder errors = new StringBuilder();
        WorkflowValidator.validateTaskStructure(invalidTask, errors);

        assertEquals("Field 'name' must be a string", errors.toString());
    }

    @Test
    void validateTaskStructure_missingType_addsError() {
        String invalidTask = """
            {
                "label": "Test Task",
                "name": "testTask",
                "parameters": {}
            }
            """;

        StringBuilder errors = new StringBuilder();
        WorkflowValidator.validateTaskStructure(invalidTask, errors);

        assertEquals("Missing required field: type", errors.toString());
    }

    @Test
    void validateTaskStructure_nonStringType_addsError() {
        String invalidTask = """
            {
                "label": "Test Task",
                "name": "testTask",
                "type": 123,
                "parameters": {}
            }
            """;

        StringBuilder errors = new StringBuilder();
        WorkflowValidator.validateTaskStructure(invalidTask, errors);

        assertEquals("Field 'type' must be a string", errors.toString());
    }

    @Test
    void validateTaskStructure_invalidTypePattern_addsError() {
        String invalidTask = """
            {
                "label": "Test Task",
                "name": "testTask",
                "type": "component/v1/",
                "parameters": {}
            }
            """;

        StringBuilder errors = new StringBuilder();
        WorkflowValidator.validateTaskStructure(invalidTask, errors);

        assertTrue(errors.toString()
            .contains("Field 'type' must match pattern:"), errors.toString());
    }

    @Test
    void validateTaskStructure_validTypePatterns_noErrors() {
        String[] validTypes = {
            "component/v1",
            "component/v1/action",
            "comp123/v456/act789"
        };

        for (String type : validTypes) {
            String validTask = String.format("""
                {
                    "label": "Test Task",
                    "name": "testTask",
                    "type": "%s",
                    "parameters": {}
                }
                """, type);

            StringBuilder errors = new StringBuilder();
            WorkflowValidator.validateTaskStructure(validTask, errors);

            assertEquals("", errors.toString(), "Type should be valid: " + type);
        }
    }

    @Test
    void validateTaskStructure_missingParameters_addsError() {
        String invalidTask = """
            {
                "label": "Test Task",
                "name": "testTask",
                "type": "component/v1/action"
            }
            """;

        StringBuilder errors = new StringBuilder();
        WorkflowValidator.validateTaskStructure(invalidTask, errors);

        assertEquals("Missing required field: parameters", errors.toString());
    }

    @Test
    void validateTaskStructure_nonObjectParameters_addsError() {
        String invalidTask = """
            {
                "label": "Test Task",
                "name": "testTask",
                "type": "component/v1/action",
                "parameters": "not an object"
            }
            """;

        StringBuilder errors = new StringBuilder();
        WorkflowValidator.validateTaskStructure(invalidTask, errors);

        assertEquals("Field 'parameters' must be an object", errors.toString());
    }

    @Test
    void validateTaskStructure_nonObject_addsError() {
        String invalidTask = "\"not an object\"";

        StringBuilder errors = new StringBuilder();
        WorkflowValidator.validateTaskStructure(invalidTask, errors);

        assertEquals("Task must be an object", errors.toString());
    }

    @Test
    void validateTaskStructure_invalidJson_addsError() {
        String invalidTask = "{invalid json}";

        StringBuilder errors = new StringBuilder();
        WorkflowValidator.validateTaskStructure(invalidTask, errors);

        assertTrue(errors.toString()
            .contains("Invalid JSON format:"), errors.toString());
    }

    @Test
    void validateTaskParameters_validParameters_noErrors() {
        String currentTaskParameters = """
            {
                "name": "John",
                "age": 30,
                "active": true
            }
            """;

        List<ToolUtils.PropertyInfo> taskDefinition = List.of(
            new ToolUtils.PropertyInfo("name", "STRING", null, true, true, null, null),
            new ToolUtils.PropertyInfo("age", "NUMBER", null, false, true, null, null),
            new ToolUtils.PropertyInfo("active", "BOOLEAN", null, false, true, null, null));

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();
        WorkflowValidator.validateTaskParameters(currentTaskParameters, taskDefinition, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateTaskParameters_extraProperty_addsWarning() {
        String currentTaskParameters = """
            {
                "name": "John",
                "extraField": "not allowed"
            }
            """;

        List<ToolUtils.PropertyInfo> taskDefinition = List.of(
            new ToolUtils.PropertyInfo("name", "STRING", null, true, true, null, null));

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();
        WorkflowValidator.validateTaskParameters(currentTaskParameters, taskDefinition, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("Property 'extraField' is not defined in task definition", warnings.toString());
    }

    @Test
    void validateTaskParameters_missingRequiredProperty_addsError() {
        String currentTaskParameters = """
            {
                "age": 30
            }
            """;

        List<ToolUtils.PropertyInfo> taskDefinition = List.of(
            new ToolUtils.PropertyInfo("name", "STRING", null, true, true, null, null),
            new ToolUtils.PropertyInfo("age", "NUMBER", null, false, true, null, null));

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();
        WorkflowValidator.validateTaskParameters(currentTaskParameters, taskDefinition, errors, warnings);

        assertEquals("Missing required property: name", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateTaskParameters_typeMatching() {
        String currentTaskParameters = """
            {
                "string": "test",
                "integer": 123,
                "boolean": true,
                "number": 123.456,
                "array": [1, 2, 3],
                "object": {},
                "null": null,
                "date": "2025-01-01",
                "time": "09:46:38",
                "date_time": "2025-09-23T11:46:19"
            }
            """;

        List<ToolUtils.PropertyInfo> taskDefinition = List.of(
            new ToolUtils.PropertyInfo("string", "STRING", null, true, true, null, null),
            new ToolUtils.PropertyInfo("integer", "INTEGER", null, true, true, null, null),
            new ToolUtils.PropertyInfo("boolean", "BOOLEAN", null, true, true, null, null),
            new ToolUtils.PropertyInfo("number", "NUMBER", null, true, true, null, null),
            new ToolUtils.PropertyInfo("array", "ARRAY", null, true, true, null, null),
            new ToolUtils.PropertyInfo("object", "OBJECT", null, true, true, null, null),
            new ToolUtils.PropertyInfo("null", "NULL", null, true, true, null, null),
            new ToolUtils.PropertyInfo("date", "DATE", null, true, true, null, null),
            new ToolUtils.PropertyInfo("time", "TIME", null, true, true, null, null),
            new ToolUtils.PropertyInfo("date_time", "DATE_TIME", null, true, true, null, null));

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();
        WorkflowValidator.validateTaskParameters(currentTaskParameters, taskDefinition, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateTaskParameters_nullTypeMatching() {
        String currentTaskParameters = """
            {
                "string": null,
                "integer": null,
                "boolean": null,
                "number": null,
                "array": null,
                "object": null,
                "null": null,
                "date": null,
                "time": null,
                "date_time": null
            }
            """;

        List<ToolUtils.PropertyInfo> taskDefinition = List.of(
            new ToolUtils.PropertyInfo("string", "STRING", null, true, true, null, null),
            new ToolUtils.PropertyInfo("integer", "INTEGER", null, true, true, null, null),
            new ToolUtils.PropertyInfo("boolean", "BOOLEAN", null, true, true, null, null),
            new ToolUtils.PropertyInfo("number", "NUMBER", null, true, true, null, null),
            new ToolUtils.PropertyInfo("array", "ARRAY", null, true, true, null, null),
            new ToolUtils.PropertyInfo("object", "OBJECT", null, true, true, null, null),
            new ToolUtils.PropertyInfo("null", "NULL", null, true, true, null, null),
            new ToolUtils.PropertyInfo("date", "DATE", null, true, true, null, null),
            new ToolUtils.PropertyInfo("time", "TIME", null, true, true, null, null),
            new ToolUtils.PropertyInfo("date_time", "DATE_TIME", null, true, true, null, null));

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();
        WorkflowValidator.validateTaskParameters(currentTaskParameters, taskDefinition, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateTaskParameters_wrongType_addsError() {
        String currentTaskParameters = """
            {
                "name": 123,
                "age": "thirty",
                "active": "yes"
            }
            """;

        List<ToolUtils.PropertyInfo> taskDefinition = List.of(
            new ToolUtils.PropertyInfo("name", "STRING", null, true, true, null, null),
            new ToolUtils.PropertyInfo("age", "INTEGER", null, false, true, null, null),
            new ToolUtils.PropertyInfo("active", "BOOLEAN", null, false, true, null, null));

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();
        WorkflowValidator.validateTaskParameters(currentTaskParameters, taskDefinition, errors, warnings);

        assertEquals("""
            Property 'name' has incorrect type. Expected: string, but got: integer
            Property 'age' has incorrect type. Expected: integer, but got: string
            Property 'active' has incorrect type. Expected: boolean, but got: string""", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateTaskParameters_wrongTypeFormat_addsError() {
        String currentTaskParameters = """
            {
                "day": "2025-1-1",
                "night": "2025-02-30",
                "from": "45:45:73",
                "to": "2:5:5",
                "specific_date": "2025-09-23F11:46:12"
            }
            """;

        List<ToolUtils.PropertyInfo> taskDefinition = List.of(
            new ToolUtils.PropertyInfo("day", "DATE", null, true, true, null, null),
            new ToolUtils.PropertyInfo("night", "DATE", null, true, true, null, null),
            new ToolUtils.PropertyInfo("from", "TIME", null, true, true, null, null),
            new ToolUtils.PropertyInfo("to", "TIME", null, true, true, null, null),
            new ToolUtils.PropertyInfo("specific_date", "DATE_TIME", null, true, true, null, null));

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();
        WorkflowValidator.validateTaskParameters(currentTaskParameters, taskDefinition, errors, warnings);

        assertEquals("""
            Property 'day' is in incorrect date format. Format should be in: 'yyyy-MM-dd'
            Property 'night' is in incorrect date format. Impossible date: 2025-02-30
            Property 'from' is in incorrect time format. Impossible time: 45:45:73
            Property 'to' is in incorrect time format. Format should be in: 'hh:mm:ss'
            Property 'specific_date' has incorrect type. Format should be in: 'yyyy-MM-ddThh:mm:ss'""",
            errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateTaskParameters_wrongTypeInArrayAndObject_addsError() {
        String currentTaskParameters = """
            {
                "obj": {
                    "name": 123,
                    "age": "thirty",
                    "active": "yes",
                    "items": ["John", 36, "Porky"]
                }
            }
            """;

        List<ToolUtils.PropertyInfo> taskDefinition = List.of(
            new ToolUtils.PropertyInfo("obj", "OBJECT", null, false, true, null, List.of(
                new ToolUtils.PropertyInfo("name", "STRING", null, true, true, null, null),
                new ToolUtils.PropertyInfo("age", "INTEGER", null, false, true, null, null),
                new ToolUtils.PropertyInfo("active", "BOOLEAN", null, false, true, null, null),
                new ToolUtils.PropertyInfo("items", "ARRAY", null, false, true, null, List.of(
                    new ToolUtils.PropertyInfo(null, "INTEGER", null, false, true, null, null))))));

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();
        WorkflowValidator.validateTaskParameters(currentTaskParameters, taskDefinition, errors, warnings);

        assertEquals("""
            Property 'obj.name' has incorrect type. Expected: string, but got: integer
            Property 'obj.age' has incorrect type. Expected: integer, but got: string
            Property 'obj.active' has incorrect type. Expected: boolean, but got: string
            Value 'John' has incorrect type in property 'items'. Expected: integer, but got: string
            Value 'Porky' has incorrect type in property 'items'. Expected: integer, but got: string""",
            errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateTaskParameters_arrayAndObjectTypes_noErrors() {
        String currentTaskParameters = """
            {
                "items": [],
                "config": {}
            }
            """;

        List<ToolUtils.PropertyInfo> taskDefinition = List.of(
            new ToolUtils.PropertyInfo("items", "ARRAY", null, false, true, null, List.of(
                new ToolUtils.PropertyInfo("name", "STRING", null, false, true, null, null),
                new ToolUtils.PropertyInfo("age", "INTEGER", null, false, true, null, null))),
            new ToolUtils.PropertyInfo("config", "OBJECT", null, false, true, null, null));

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();
        WorkflowValidator.validateTaskParameters(currentTaskParameters, taskDefinition, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateTaskParameters_arrayMultipleValues_noErrors() {
        String currentTaskParameters = """
            {
                "items": [
                    "John",
                    30,
                    45,
                    "Porky"
                ]
            }
            """;

        List<ToolUtils.PropertyInfo> taskDefinition = List.of(
            new ToolUtils.PropertyInfo("items", "ARRAY", null, false, true, null, List.of(
                new ToolUtils.PropertyInfo("name", "STRING", null, false, true, null, null),
                new ToolUtils.PropertyInfo("age", "INTEGER", null, false, true, null, null))));

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();
        WorkflowValidator.validateTaskParameters(currentTaskParameters, taskDefinition, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateTaskParameters_arrayMultipleValues_objectWrongAttribute() {
        String currentTaskParameters = """
            {
                "items": [
                    {
                        "name": "John",
                        "age": 30
                    },
                    {
                        "name": "Porky",
                        "age": 45
                    },
                    {
                        "name": "Jane",
                        "page": 25
                    }
                ]
            }
            """;

        List<ToolUtils.PropertyInfo> taskDefinition = List.of(
            new ToolUtils.PropertyInfo("items", "ARRAY", null, false, true, null, List.of(
                new ToolUtils.PropertyInfo("ignoredName", "OBJECT", null, false, true, null, List.of(
                    new ToolUtils.PropertyInfo("name", "STRING", null, true, true, null, null),
                    new ToolUtils.PropertyInfo("age", "INTEGER", null, true, true, null, null))))));

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();
        WorkflowValidator.validateTaskParameters(currentTaskParameters, taskDefinition, errors, warnings);

        assertEquals("Missing required property: items[2].age", errors.toString());
        assertEquals("Property 'items[index].page' is not defined in task definition", warnings.toString());
    }

    @Test
    void validateTaskParameters_arrayMultipleValues_indexDisplayCondition() {
        String currentTaskParameters = """
            {
                "items": [
                    {
                        "name": "John",
                        "age": 30,
                        "adult": true
                    },
                    {
                        "name": "Porky",
                        "age": 18
                    },
                    {
                        "name": "Jane",
                        "age": 15,
                        "adult": true
                    }
                ]
            }
            """;

        List<ToolUtils.PropertyInfo> taskDefinition = List.of(
            new ToolUtils.PropertyInfo("items", "ARRAY", null, false, true, null, List.of(
                new ToolUtils.PropertyInfo("name", "STRING", null, true, true, null, null),
                new ToolUtils.PropertyInfo("age", "INTEGER", null, true, true, null, null),
                new ToolUtils.PropertyInfo("adult", "BOOLEAN", null, true, true, "items[index].age >= 18", null))));

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();
        WorkflowValidator.validateTaskParameters(currentTaskParameters, taskDefinition, errors, warnings);

        assertEquals("Missing required property: items[1].adult", errors.toString());
        assertEquals("Property 'items[2].adult' is not defined in task definition", warnings.toString());
    }

    @Test
    void validateTaskParameters_typesInArrayAndObject_noErrors() {
        String currentTaskParameters = """
            {
                "items": ["string"],
                "config": {
                    "key": "string (required)"
                }
            }
            """;

        List<ToolUtils.PropertyInfo> taskDefinition = List.of(
            new ToolUtils.PropertyInfo("items", "ARRAY", null, true, true, null, null),
            new ToolUtils.PropertyInfo("config", "OBJECT", null, false, true, null, null));

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();
        WorkflowValidator.validateTaskParameters(currentTaskParameters, taskDefinition, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("Property 'config.key' is not defined in task definition", warnings.toString());
    }

    @Test
    void validateTaskParameters_nonObjectCurrentParameters_addsError() {
        String currentTaskParameters = "\"not an object\"";

        List<ToolUtils.PropertyInfo> taskDefinition = List.of();

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();
        WorkflowValidator.validateTaskParameters(currentTaskParameters, taskDefinition, errors, warnings);

        assertEquals("Current task parameters must be an object", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateTaskParameters_nonObjectTaskDefinition_addsError() {
        String currentTaskParameters = """
            {
                "name": "John"
            }
            """;

        ToolUtils.PropertyInfo taskDefinition = new ToolUtils.PropertyInfo(
            "testTask", "STRING", null, false, true, null, null);

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();
        WorkflowValidator.validateTaskParameters(currentTaskParameters, taskDefinition, errors, warnings);

        assertEquals("Task definition must be an object", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateTaskParameters_invalidJsonCurrentParameters_addsError() {
        String currentTaskParameters = "{invalid json}";

        List<ToolUtils.PropertyInfo> taskDefinition = List.of();

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();
        WorkflowValidator.validateTaskParameters(currentTaskParameters, taskDefinition, errors, warnings);

        assertTrue(errors.toString()
            .contains("Invalid JSON format:"), errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateTaskParameters_nullValue_correctType() {
        String currentTaskParameters = """
            {
                "nullable": null
            }
            """;

        List<ToolUtils.PropertyInfo> taskDefinition = List.of(
            new ToolUtils.PropertyInfo("nullable", "STRING", null, false, true, null, null));

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();
        WorkflowValidator.validateTaskParameters(currentTaskParameters, taskDefinition, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateTaskParameters_mixedValidationScenario() {
        String currentTaskParameters = """
            {
                "validString": "hello",
                "invalidNumber": "not an integer",
                "extraProperty": "can be here"
            }
            """;

        List<ToolUtils.PropertyInfo> taskDefinition = List.of(
            new ToolUtils.PropertyInfo("validString", "STRING", null, true, true, null, null),
            new ToolUtils.PropertyInfo("invalidNumber", "INTEGER", null, true, true, null, null),
            new ToolUtils.PropertyInfo("missingRequired", "BOOLEAN", null, true, true, null, null));

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();
        WorkflowValidator.validateTaskParameters(currentTaskParameters, taskDefinition, errors, warnings);

        assertEquals("""
            Property 'invalidNumber' has incorrect type. Expected: integer, but got: string
            Missing required property: missingRequired""", errors.toString());
        assertEquals("Property 'extraProperty' is not defined in task definition", warnings.toString());
    }

    @Test
    void validateTaskParameters_emptyParametersDefinition_allowsAnyParameters() {
        String currentTaskParameters = """
            {
                "anyProperty": "any value"
            }
            """;

        List<ToolUtils.PropertyInfo> taskDefinition = List.of();

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();
        WorkflowValidator.validateTaskParameters(currentTaskParameters, taskDefinition, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("Property 'anyProperty' is not defined in task definition", warnings.toString());
    }

    @Test
    void validateTaskParameters_displayConditionTrue_includesConditionalProperty() {
        String currentTaskParameters = """
            {
                "enableFeature": true,
                "featureConfig": {
                    "setting1": "value1",
                    "setting2": "value2"
                }
            }
            """;

        List<ToolUtils.PropertyInfo> taskDefinition = List.of(
            new ToolUtils.PropertyInfo("enableFeature", "BOOLEAN", null, true, true, null, null),
            new ToolUtils.PropertyInfo("featureConfig", "OBJECT", null, false, true, "enableFeature == true", List.of(
                new ToolUtils.PropertyInfo("setting1", "STRING", null, true, true, null, null),
                new ToolUtils.PropertyInfo("setting2", "STRING", null, false, true, null, null))));

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();
        WorkflowValidator.validateTaskParameters(currentTaskParameters, taskDefinition, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateTaskParameters_displayConditionTrue_includesConditionalPropertyReverseOrder() {
        String currentTaskParameters = """
            {
                "enableFeature": true,
                "featureConfig": {
                    "setting1": "value1",
                    "setting2": "value2"
                }
            }
            """;

        List<ToolUtils.PropertyInfo> taskDefinition = List.of(
            new ToolUtils.PropertyInfo("enableFeature", "BOOLEAN", null, true, true, null, null),
            new ToolUtils.PropertyInfo("featureConfig", "OBJECT", null, false, true, "true == enableFeature", List.of(
                new ToolUtils.PropertyInfo("setting1", "STRING", null, true, true, null, null),
                new ToolUtils.PropertyInfo("setting2", "STRING", null, false, true, null, null))));

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();
        WorkflowValidator.validateTaskParameters(currentTaskParameters, taskDefinition, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateTaskParameters_displayConditionTrue_includesConditionalPropertyString() {
        String currentTaskParameters = """
            {
                "enableFeature": "true",
                "featureConfig": {
                    "setting1": "value1",
                    "setting2": "value2"
                }
            }
            """;

        List<ToolUtils.PropertyInfo> taskDefinition = List.of(
            new ToolUtils.PropertyInfo("enableFeature", "STRING", null, true, true, null, null),
            new ToolUtils.PropertyInfo("featureConfig", "OBJECT", null, false, true, "enableFeature == 'true'", List.of(
                new ToolUtils.PropertyInfo("setting1", "STRING", null, true, true, null, null),
                new ToolUtils.PropertyInfo("setting2", "STRING", null, false, true, null, null))));

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();
        WorkflowValidator.validateTaskParameters(currentTaskParameters, taskDefinition, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateTaskParameters_displayConditionTrue_includesConditionalPropertyContains() {
        String currentTaskParameters = """
            {
                "enableFeature": "true",
                "featureConfig": {
                    "setting1": "value1",
                    "setting2": "value2"
                }
            }
            """;

        List<ToolUtils.PropertyInfo> taskDefinition = List.of(
            new ToolUtils.PropertyInfo("enableFeature", "STRING", null, true, true, null, null),
            new ToolUtils.PropertyInfo("featureConfig", "OBJECT", null, false, true,
                "contains({'true','True','1'}, enableFeature)", List.of(
                    new ToolUtils.PropertyInfo("setting1", "STRING", null, true, true, null, null),
                    new ToolUtils.PropertyInfo("setting2", "STRING", null, false, true, null, null))));

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();
        WorkflowValidator.validateTaskParameters(currentTaskParameters, taskDefinition, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateTaskParameters_displayConditionFalse_includesConditionalPropertyContains() {
        String currentTaskParameters = """
            {
                "enableFeature": "false",
                "featureConfig": {
                    "setting1": "value1",
                    "setting2": "value2"
                }
            }
            """;

        List<ToolUtils.PropertyInfo> taskDefinition = List.of(
            new ToolUtils.PropertyInfo("enableFeature", "STRING", null, true, true, null, null),
            new ToolUtils.PropertyInfo("featureConfig", "OBJECT", null, false, true,
                "contains({'true','True','1'}, enableFeature)", List.of(
                    new ToolUtils.PropertyInfo("setting1", "STRING", null, true, true, null, null),
                    new ToolUtils.PropertyInfo("setting2", "STRING", null, false, true, null, null))));

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();
        WorkflowValidator.validateTaskParameters(currentTaskParameters, taskDefinition, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("""
            Property 'featureConfig' is not defined in task definition
            Property 'featureConfig.setting1' is not defined in task definition
            Property 'featureConfig.setting2' is not defined in task definition""", warnings.toString());
    }

    @Test
    void validateTaskParameters_displayConditionTrue_invalidDisplayConditionOperation() {
        String currentTaskParameters = """
            {
                "enableFeature": "true",
                "featureConfig": {
                    "setting1": "value1",
                    "setting2": "value2"
                }
            }
            """;

        List<ToolUtils.PropertyInfo> taskDefinition = List.of(
            new ToolUtils.PropertyInfo("enableFeature", "STRING", null, true, true, null, null),
            new ToolUtils.PropertyInfo("featureConfig", "OBJECT", null, false, true, "gndfknskgn / sflakdjdkf 3",
                List.of(
                    new ToolUtils.PropertyInfo("setting1", "STRING", null, true, true, null, null),
                    new ToolUtils.PropertyInfo("setting2", "STRING", null, false, true, null, null))));

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();
        WorkflowValidator.validateTaskParameters(currentTaskParameters, taskDefinition, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("""
            Property 'featureConfig' is not defined in task definition
            Property 'featureConfig.setting1' is not defined in task definition
            Property 'featureConfig.setting2' is not defined in task definition
            Invalid logic for display condition: 'gndfknskgn / sflakdjdkf 3'""", warnings.toString());
    }

    @Test
    void validateTaskParameters_displayConditionFalseFloat_errorWhenConditionalPropertyProvided() {
        String currentTaskParameters = """
            {
                "enableFeature": 4,
                "featureConfig": {
                    "setting2": "value1"
                }
            }
            """;

        List<ToolUtils.PropertyInfo> taskDefinition = List.of(
            new ToolUtils.PropertyInfo("enableFeature", "INTEGER", null, true, true, null, null),
            new ToolUtils.PropertyInfo("featureConfig", "OBJECT", null, false, true, "enableFeature <= 5.87546",
                List.of(
                    new ToolUtils.PropertyInfo("setting1", "STRING", null, true, true, null, null),
                    new ToolUtils.PropertyInfo("setting2", "STRING", null, false, true, null, null))));

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();
        WorkflowValidator.validateTaskParameters(currentTaskParameters, taskDefinition, errors, warnings);

        assertEquals("Missing required property: featureConfig.setting1", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateTaskParameters_displayConditionTrueIntegerInverted_errorWhenConditionalPropertyProvided() {
        String currentTaskParameters = """
            {
                "enableFeature": 4.1,
                "featureConfig": {
                    "setting2": "value1"
                }
            }
            """;

        List<ToolUtils.PropertyInfo> taskDefinition = List.of(
            new ToolUtils.PropertyInfo("enableFeature", "FLOAT", null, true, true, null, null),
            new ToolUtils.PropertyInfo("featureConfig", "OBJECT", null, false, true, "50 > enableFeature", List.of(
                new ToolUtils.PropertyInfo("setting1", "STRING", null, true, true, null, null),
                new ToolUtils.PropertyInfo("setting2", "STRING", null, false, true, null, null))));

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();
        WorkflowValidator.validateTaskParameters(currentTaskParameters, taskDefinition, errors, warnings);

        assertEquals("Missing required property: featureConfig.setting1", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateTaskParameters_multipleDisplayConditions_filtersByCondition_incorrectCondition() {
        String currentTaskParameters = """
            {
                "mode": "advanced",
                "basicConfig": {
                    "name": "basic"
                }
            }
            """;

        List<ToolUtils.PropertyInfo> taskDefinition = List.of(
            new ToolUtils.PropertyInfo("mode", "STRING", null, true, true, null, null),
            new ToolUtils.PropertyInfo("basicConfig", "OBJECT", null, false, true, "mode == 'basic'", List.of(
                new ToolUtils.PropertyInfo("name", "STRING", null, true, true, null, null))),
            new ToolUtils.PropertyInfo("advancedConfig", "OBJECT", null, false, true, "mode == 'advanced'", List.of(
                new ToolUtils.PropertyInfo("name", "STRING", null, true, true, null, null),
                new ToolUtils.PropertyInfo("extra", "STRING", null, false, true, null, null))));

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();
        WorkflowValidator.validateTaskParameters(currentTaskParameters, taskDefinition, errors, warnings);

        assertEquals("Missing required property: advancedConfig.name", errors.toString());
        assertEquals("""
            Property 'basicConfig' is not defined in task definition
            Property 'basicConfig.name' is not defined in task definition""", warnings.toString());
    }

    @Test
    void validateTaskParameters_duplicatePropertiesWithDifferentConditions_includesCorrectOne() {
        String currentTaskParameters = """
            {
                "bodyContentType": true,
                "bodyContent": {
                    "extension": "txt",
                    "mimeType": "text/plain",
                    "name": "file.txt",
                    "url": "https://example.com/file.txt"
                }
            }
            """;

        List<ToolUtils.PropertyInfo> taskDefinition = List.of(
            new ToolUtils.PropertyInfo("bodyContentType", "BOOLEAN", null, false, true, null, null),
            new ToolUtils.PropertyInfo("bodyContent", "OBJECT", null, false, true, "bodyContentType == true", List.of(
                new ToolUtils.PropertyInfo("extension", "STRING", null, true, true, null, null),
                new ToolUtils.PropertyInfo("mimeType", "STRING", null, true, true, null, null),
                new ToolUtils.PropertyInfo("name", "STRING", null, true, true, null, null),
                new ToolUtils.PropertyInfo("url", "STRING", null, true, true, null, null))),
            new ToolUtils.PropertyInfo("bodyContent", "OBJECT", null, false, true, "bodyContentType == false",
                List.of()));

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();
        WorkflowValidator.validateTaskParameters(currentTaskParameters, taskDefinition, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateTaskParameters_duplicatePropertiesWithDifferentConditions_excludesIncorrectOne() {
        String currentTaskParameters = """
            {
                "bodyContentType": false,
                "bodyContent": {
                    "extension": "txt"
                }
            }
            """;

        List<ToolUtils.PropertyInfo> taskDefinition = List.of(
            new ToolUtils.PropertyInfo("bodyContentType", "BOOLEAN", null, false, true, null, null),
            new ToolUtils.PropertyInfo("bodyContent", "OBJECT", null, false, true, "bodyContentType == true", List.of(
                new ToolUtils.PropertyInfo("extension", "STRING", null, true, true, null, null),
                new ToolUtils.PropertyInfo("mimeType", "STRING", null, true, true, null, null),
                new ToolUtils.PropertyInfo("name", "STRING", null, true, true, null, null),
                new ToolUtils.PropertyInfo("url", "STRING", null, true, true, null, null))),
            new ToolUtils.PropertyInfo("bodyContent", "OBJECT", null, false, true, "bodyContentType == false", List.of(
                new ToolUtils.PropertyInfo("simpleProperty", "STRING", null, false, true, null, null))));

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();
        WorkflowValidator.validateTaskParameters(currentTaskParameters, taskDefinition, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("Property 'bodyContent.extension' is not defined in task definition", warnings.toString());
    }

    @Test
    void validateTaskParameters_duplicatePropertiesWithDifferentConditions_includesIncorrect() {
        String currentTaskParameters = """
            {
                "bodyContentType": true,
                "bodyContent": {
                    "extension": "txt"
                }
            }
            """;

        List<ToolUtils.PropertyInfo> taskDefinition = List.of(
            new ToolUtils.PropertyInfo("bodyContentType", "BOOLEAN", null, false, true, null, null),
            new ToolUtils.PropertyInfo("bodyContent", "OBJECT", null, false, true, "bodyContentType == true", List.of(
                new ToolUtils.PropertyInfo("extension", "STRING", null, true, true, null, null),
                new ToolUtils.PropertyInfo("mimeType", "STRING", null, true, true, null, null),
                new ToolUtils.PropertyInfo("name", "STRING", null, true, true, null, null),
                new ToolUtils.PropertyInfo("url", "STRING", null, true, true, null, null))),
            new ToolUtils.PropertyInfo("bodyContent", "OBJECT", null, false, true, "bodyContentType == false", List.of(
                new ToolUtils.PropertyInfo("simpleProperty", "STRING", null, false, true, null, null))));

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();
        WorkflowValidator.validateTaskParameters(currentTaskParameters, taskDefinition, errors, warnings);

        assertEquals("""
            Missing required property: bodyContent.mimeType
            Missing required property: bodyContent.name
            Missing required property: bodyContent.url""", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateTaskParameters_nestedDisplayConditions_validatesCorrectly() {
        String currentTaskParameters = """
            {
                "parentEnabled": true,
                "parent": {
                    "childEnabled": true,
                    "child": {
                        "value": "test"
                    }
                }
            }
            """;

        List<ToolUtils.PropertyInfo> taskDefinition = List.of(
            new ToolUtils.PropertyInfo("parentEnabled", "BOOLEAN", null, true, true, null, null),
            new ToolUtils.PropertyInfo("parent", "OBJECT", null, false, true, "parentEnabled == true", List.of(
                new ToolUtils.PropertyInfo("childEnabled", "BOOLEAN", null, true, true, null, null),
                new ToolUtils.PropertyInfo("child", "OBJECT", null, false, true, "childEnabled == true", List.of(
                    new ToolUtils.PropertyInfo("value", "STRING", null, true, true, null, null))))));

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();
        WorkflowValidator.validateTaskParameters(currentTaskParameters, taskDefinition, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateTaskParameters_stringValueCondition_worksCorrectly() {
        String currentTaskParameters = """
            {
                "format": "json",
                "jsonConfig": {
                    "indent": 2
                }
            }
            """;

        List<ToolUtils.PropertyInfo> taskDefinition = List.of(
            new ToolUtils.PropertyInfo("format", "STRING", null, true, true, null, null),
            new ToolUtils.PropertyInfo("jsonConfig", "OBJECT", null, false, true, "format == 'json'", List.of(
                new ToolUtils.PropertyInfo("indent", "INTEGER", null, false, true, null, null))),
            new ToolUtils.PropertyInfo("xmlConfig", "OBJECT", null, false, true, "format == 'xml'", List.of(
                new ToolUtils.PropertyInfo("schema", "STRING", null, false, true, null, null))));

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();
        WorkflowValidator.validateTaskParameters(currentTaskParameters, taskDefinition, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateTaskParameters_missingFieldForCondition_excludesConditionalProperty() {
        String currentTaskParameters = """
            {
                "name": "test"
            }
            """;

        List<ToolUtils.PropertyInfo> taskDefinition = List.of(
            new ToolUtils.PropertyInfo("name", "STRING", null, true, true, null, null),
            new ToolUtils.PropertyInfo("advancedConfig", "OBJECT", null, false, true, "enableAdvanced == true", List.of(
                new ToolUtils.PropertyInfo("setting", "STRING", null, true, true, null, null))));

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();
        WorkflowValidator.validateTaskParameters(currentTaskParameters, taskDefinition, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateTaskParameters_complexNestedConditions_validatesAllLevels() {
        String currentTaskParameters = """
            {
                "level1": "enabled",
                "config1": {
                    "level2": "active",
                    "config2": {
                        "level3": true,
                        "config3": {
                            "finalValue": "success"
                        }
                    }
                }
            }
            """;

        List<ToolUtils.PropertyInfo> taskDefinition = List.of(
            new ToolUtils.PropertyInfo("level1", "STRING", null, true, true, null, null),
            new ToolUtils.PropertyInfo("config1", "OBJECT", null, false, true, "level1 == 'enabled'", List.of(
                new ToolUtils.PropertyInfo("level2", "STRING", null, true, true, null, null),
                new ToolUtils.PropertyInfo("config2", "OBJECT", null, false, true, "level2 == 'active'", List.of(
                    new ToolUtils.PropertyInfo("level3", "BOOLEAN", null, true, true, null, null),
                    new ToolUtils.PropertyInfo("config3", "OBJECT", null, false, true, "level3 == true", List.of(
                        new ToolUtils.PropertyInfo("finalValue", "STRING", null, true, true, null, null))))))));

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();
        WorkflowValidator.validateTaskParameters(currentTaskParameters, taskDefinition, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateTaskParameters_complexNestedConditions_deepMissingError() {
        String currentTaskParameters = """
            {
                "level1": "enabled",
                "config1": {
                    "level2": "active",
                    "config2": {
                        "level3": true,
                        "config3": {
                            "randomValue": "success"
                        }
                    }
                }
            }
            """;

        List<ToolUtils.PropertyInfo> taskDefinition = List.of(
            new ToolUtils.PropertyInfo("level1", "STRING", null, true, true, null, null),
            new ToolUtils.PropertyInfo("config1", "OBJECT", null, false, true, null, List.of(
                new ToolUtils.PropertyInfo("level2", "STRING", null, true, true, null, null),
                new ToolUtils.PropertyInfo("config2", "OBJECT", null, false, true, "level2 == 'active'", List.of(
                    new ToolUtils.PropertyInfo("level3", "BOOLEAN", null, true, true, null, null),
                    new ToolUtils.PropertyInfo("config3", "OBJECT", null, false, true, "level3 == true", List.of(
                        new ToolUtils.PropertyInfo("finalValue", "STRING", null, true, true, null, null))))))));

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();
        WorkflowValidator.validateTaskParameters(currentTaskParameters, taskDefinition, errors, warnings);

        assertEquals("Missing required property: config1.config2.config3.finalValue", errors.toString());
        assertEquals("Property 'config1.config2.config3.randomValue' is not defined in task definition",
            warnings.toString());
    }

    @Test
    void validateTaskParameters_complexNestedConditions_deepWarning() {
        String currentTaskParameters = """
            {
                "level1": "enabled",
                "config1": {
                    "level2": "active",
                    "config2": {
                        "level3": false,
                        "config3": {
                            "finalValue": "success"
                        }
                    }
                }
            }
            """;

        List<ToolUtils.PropertyInfo> taskDefinition = List.of(
            new ToolUtils.PropertyInfo("level1", "STRING", null, true, true, null, null),
            new ToolUtils.PropertyInfo("config1", "OBJECT", "", false, true, null, List.of(
                new ToolUtils.PropertyInfo("level2", "STRING", null, true, true, null, null),
                new ToolUtils.PropertyInfo("config2", "OBJECT", null, false, true, "level2 == 'active'", List.of(
                    new ToolUtils.PropertyInfo("level3", "BOOLEAN", null, true, true, null, null),
                    new ToolUtils.PropertyInfo("config3", "OBJECT", null, false, true, "level3 == true", List.of(
                        new ToolUtils.PropertyInfo("finalValue", "STRING", null, true, true, null, null))))))));

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();
        WorkflowValidator.validateTaskParameters(currentTaskParameters, taskDefinition, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("""
            Property 'config1.config2.config3' is not defined in task definition
            Property 'config1.config2.config3.finalValue' is not defined in task definition""", warnings.toString());
    }

    @Test
    void validateTaskParameters_inlineCondition_excludesRequiredProperty() {
        String currentTaskParameters = """
            {
                "name": "test",
                "enableAdvanced": false
            }
            """;

        List<ToolUtils.PropertyInfo> taskDefinition = List.of(
            new ToolUtils.PropertyInfo("name", "STRING", null, true, true, null, null),
            new ToolUtils.PropertyInfo("enableAdvanced", "BOOLEAN", null, true, true, null, null),
            new ToolUtils.PropertyInfo("advancedConfig", "STRING", null, true, true, "enableAdvanced == true", null));

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();
        WorkflowValidator.validateTaskParameters(currentTaskParameters, taskDefinition, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateTaskParameters_inlineConditionReversed_excludesRequiredProperty() {
        String currentTaskParameters = """
            {
                "name": "test",
                "enableAdvanced": true,
                "advancedConfig": "value"
            }
            """;

        List<ToolUtils.PropertyInfo> taskDefinition = List.of(
            new ToolUtils.PropertyInfo("name", "STRING", null, true, true, null, null),
            new ToolUtils.PropertyInfo("enableAdvanced", "BOOLEAN", null, true, true, null, null),
            new ToolUtils.PropertyInfo("advancedConfig", "STRING", null, true, true, "true == enableAdvanced", null));

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();
        WorkflowValidator.validateTaskParameters(currentTaskParameters, taskDefinition, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateTaskParameters_inlineConditionDoesntIncludeRequiredProperty_missingError() {
        String currentTaskParameters = """
            {
                "name": "test",
                "enableAdvanced": true
            }
            """;

        List<ToolUtils.PropertyInfo> taskDefinition = List.of(
            new ToolUtils.PropertyInfo("name", "STRING", null, true, true, null, null),
            new ToolUtils.PropertyInfo("enableAdvanced", "BOOLEAN", null, true, true, null, null),
            new ToolUtils.PropertyInfo("advancedConfig", "STRING", null, true, true, "enableAdvanced == true", null));

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();
        WorkflowValidator.validateTaskParameters(currentTaskParameters, taskDefinition, errors, warnings);

        assertEquals("Missing required property: advancedConfig", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateTaskParameters_inlineConditionDoesntIncludeRequiredPropertyInteger_missingError() {
        String currentTaskParameters = """
            {
                "name": "test",
                "enableAdvanced": 4.0
            }
            """;

        List<ToolUtils.PropertyInfo> taskDefinition = List.of(
            new ToolUtils.PropertyInfo("name", "STRING", null, true, true, null, null),
            new ToolUtils.PropertyInfo("enableAdvanced", "NUMBER", null, true, true, null, null),
            new ToolUtils.PropertyInfo("advancedConfig", "STRING", null, true, true, "enableAdvanced >= 4", null));

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();
        WorkflowValidator.validateTaskParameters(currentTaskParameters, taskDefinition, errors, warnings);

        assertEquals("Missing required property: advancedConfig", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateTaskParameters_inlineConditionDoesntIncludeRequiredPropertyReversedFloat_noErrors() {
        String currentTaskParameters = """
            {
                "name": "test",
                "enableAdvanced": 4
            }
            """;

        List<ToolUtils.PropertyInfo> taskDefinition = List.of(
            new ToolUtils.PropertyInfo("name", "STRING", null, true, true, null, null),
            new ToolUtils.PropertyInfo("enableAdvanced", "INTEGER", null, true, true, null, null),
            new ToolUtils.PropertyInfo("advancedConfig", "STRING", null, true, true, "4.1 < enableAdvanced", null));

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();
        WorkflowValidator.validateTaskParameters(currentTaskParameters, taskDefinition, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateTaskParameters_inlineConditionDoesntIncludeRequiredPropertyContains_noErrors() {
        String currentTaskParameters = """
            {
                "name": "test",
                "enableAdvanced": "true",
                "advancedConfig": "Barabara Strisen"
            }
            """;

        List<ToolUtils.PropertyInfo> taskDefinition = List.of(
            new ToolUtils.PropertyInfo("name", "STRING", null, true, true, null, null),
            new ToolUtils.PropertyInfo("enableAdvanced", "STRING", null, true, true, null, null),
            new ToolUtils.PropertyInfo("advancedConfig", "STRING", null, true, true,
                "contains({'Donald_B2rbara','True','true'}, enableAdvanced)", null));

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();
        WorkflowValidator.validateTaskParameters(currentTaskParameters, taskDefinition, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateTaskParameters_complexNestedInlineConditions_validatesAllLevels() {
        String currentTaskParameters = """
            {
                "level1": "enabled",
                "config1": {
                    "level2": "active",
                    "config2": {
                        "level3": true,
                        "config3": {
                            "finalValue": "success"
                        }
                    }
                }
            }
            """;

        List<ToolUtils.PropertyInfo> taskDefinition = List.of(
            new ToolUtils.PropertyInfo("level1", "STRING", null, true, true, null, null),
            new ToolUtils.PropertyInfo("config1", "OBJECT", null, false, true, null, List.of(
                new ToolUtils.PropertyInfo("level2", "STRING", null, true, true, "level1 == 'enabled'", null),
                new ToolUtils.PropertyInfo("config2", "OBJECT", null, false, true, null, List.of(
                    new ToolUtils.PropertyInfo("level3", "BOOLEAN", null, true, true, "'active' == level2", null),
                    new ToolUtils.PropertyInfo("config3", "OBJECT", null, false, true, null, List.of(
                        new ToolUtils.PropertyInfo("finalValue", "STRING", null, true, true, "level3 == true",
                            null))))))));

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();
        WorkflowValidator.validateTaskParameters(currentTaskParameters, taskDefinition, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateTaskParameters_nestedInlineCondition_missingError() {
        String currentTaskParameters = """
            {
                "level1": "enabled",
                "config1": {
                    "config2": {
                        "config3": {
                        }
                    }
                }
            }
            """;

        List<ToolUtils.PropertyInfo> taskDefinition = List.of(
            new ToolUtils.PropertyInfo("level1", "STRING", null, true, true, null, null),
            new ToolUtils.PropertyInfo("config1", "OBJECT", null, false, true, null, List.of(
                new ToolUtils.PropertyInfo("level2", "STRING", null, true, true, "level1 == 'enabled'", null),
                new ToolUtils.PropertyInfo("config2", "OBJECT", null, false, true, null, List.of(
                    new ToolUtils.PropertyInfo("level3", "BOOLEAN", null, true, true, "'enabled' == level1", null),
                    new ToolUtils.PropertyInfo("config3", "OBJECT", null, false, true, null, List.of(
                        new ToolUtils.PropertyInfo("finalValue", "STRING", null, true, true, "level1 == 'enabled'",
                            null))))))));

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();
        WorkflowValidator.validateTaskParameters(currentTaskParameters, taskDefinition, errors, warnings);

        assertEquals("""
            Missing required property: config1.level2
            Missing required property: config1.config2.level3
            Missing required property: config1.config2.config3.finalValue""", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateTaskParameters_nestedInlineCondition_notMatchingCondition() {
        String currentTaskParameters = """
            {
                "level1": "not_enabled",
                "config1": {
                    "config2": {
                        "config3": { }
                    }
                }
            }
            """;

        List<ToolUtils.PropertyInfo> taskDefinition = List.of(
            new ToolUtils.PropertyInfo("level1", "STRING", null, true, true, null, null),
            new ToolUtils.PropertyInfo("config1", "OBJECT", null, false, true, null, List.of(
                new ToolUtils.PropertyInfo("level2", "STRING", null, true, true, "level1 == 'enabled'", null),
                new ToolUtils.PropertyInfo("config2", "OBJECT", null, false, true, null, List.of(
                    new ToolUtils.PropertyInfo("level3", "BOOLEAN", null, true, true, "'enabled' == level1", null),
                    new ToolUtils.PropertyInfo("config3", "OBJECT", null, false, true, null, List.of(
                        new ToolUtils.PropertyInfo("finalValue", "STRING", null, true, true, "level1 == 'enabled'",
                            null))))))));

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();
        WorkflowValidator.validateTaskParameters(currentTaskParameters, taskDefinition, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateTaskParameters_httpClientPost() {
        String currentTaskParameters = """
            {
                "uri": "https://api.example.com/v1/users",
                "allowUnauthorizedCerts": false,
                "responseType": "JSON",
                "headers": {
                    "Authorization": "Bearer some_fake_token",
                    "Content-Type": "application/json"
                },
                "queryParameters": {
                    "debug": "true"
                },
                "body": {
                    "bodyContentType": "RAW",
                    "bodyContentMimeType": "application/json",
                    "bodyContent": "John Doe"
                },
                "fullResponse": true,
                "followAllRedirects": false,
                "followRedirect": true,
                "ignoreResponseCode": false,
                "proxy": "",
                "timeout": 30000
            }
            """;

        List<ToolUtils.PropertyInfo> taskDefinition = List.of(
            new ToolUtils.PropertyInfo("uri", "STRING", null, true, true, null, null),
            new ToolUtils.PropertyInfo("allowUnauthorizedCerts", "BOOLEAN", null, false, true, null, null),
            new ToolUtils.PropertyInfo("responseType", "STRING", null, false, true, null, null),
            new ToolUtils.PropertyInfo("responseFilename", "STRING", null, false, true, "responseType == 'BINARY'",
                null),
            new ToolUtils.PropertyInfo("headers", "OBJECT", "", false, true, null, null),
            new ToolUtils.PropertyInfo("queryParameters", "OBJECT", "", false, true, null, null),
            new ToolUtils.PropertyInfo("body", "OBJECT", "", false, true, null, List.of(
                new ToolUtils.PropertyInfo("bodyContentType", "STRING", null, false, true, null, null),
                new ToolUtils.PropertyInfo("bodyContent", "OBJECT", null, false, true, "body.bodyContentType == 'JSON'",
                    null),
                new ToolUtils.PropertyInfo("bodyContent", "OBJECT", null, false, true, "body.bodyContentType == 'XML'",
                    null),
                new ToolUtils.PropertyInfo("bodyContent", "OBJECT", null, false, true,
                    "body.bodyContentType == 'FORM_DATA'", null),
                new ToolUtils.PropertyInfo("bodyContent", "OBJECT", null, false, true,
                    "body.bodyContentType == 'FORM_URL_ENCODED'", null),
                new ToolUtils.PropertyInfo("bodyContent", "STRING", null, false, true, "body.bodyContentType == 'RAW'",
                    null),
                new ToolUtils.PropertyInfo("bodyContent", "OBJECT", null, false, true,
                    "body.bodyContentType == 'BINARY'", List.of(
                        new ToolUtils.PropertyInfo("extension", "STRING", null, true, true, null, null),
                        new ToolUtils.PropertyInfo("mimeType", "STRING", null, true, true, null, null),
                        new ToolUtils.PropertyInfo("name", "STRING", null, true, true, null, null),
                        new ToolUtils.PropertyInfo("url", "STRING", null, true, true, null, null))),
                new ToolUtils.PropertyInfo("bodyContentMimeType", "STRING", null, false, true,
                    "'BINARY' == body.bodyContentType", null),
                new ToolUtils.PropertyInfo("bodyContentMimeType", "STRING", null, false, true,
                    "'RAW' == body.bodyContentType", null))),
            new ToolUtils.PropertyInfo("fullResponse", "BOOLEAN", null, false, true, null, null),
            new ToolUtils.PropertyInfo("followAllRedirects", "BOOLEAN", null, false, true, null, null),
            new ToolUtils.PropertyInfo("followRedirect", "BOOLEAN", null, false, true, null, null),
            new ToolUtils.PropertyInfo("ignoreResponseCode", "BOOLEAN", null, false, true, null, null),
            new ToolUtils.PropertyInfo("proxy", "STRING", null, false, true, null, null),
            new ToolUtils.PropertyInfo("timeout", "INTEGER", null, false, true, null, null));

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();
        WorkflowValidator.validateTaskParameters(currentTaskParameters, taskDefinition, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("""
            Property 'headers.Authorization' is not defined in task definition
            Property 'headers.Content-Type' is not defined in task definition
            Property 'queryParameters.debug' is not defined in task definition""", warnings.toString());
    }

    @Test
    void validateTaskParameters_conditionFlow_rawExpressionTrue() {
        String currentTaskParameters = """
            {
                "rawExpression": true,
                "expression": "TODO",
                "caseTrue": [],
                "caseFalse": []
            }
            """;

        List<ToolUtils.PropertyInfo> taskDefinition = List.of(
            new ToolUtils.PropertyInfo("rawExpression", "BOOLEAN", null, false, true, null, null),
            new ToolUtils.PropertyInfo("conditions", "ARRAY", null, false, true, "rawExpression == false", List.of(
                new ToolUtils.PropertyInfo(null, "ARRAY", null, false, false, null, List.of(
                    new ToolUtils.PropertyInfo("boolean", "OBJECT", null, false, false, null, List.of(
                        new ToolUtils.PropertyInfo("type", "STRING", null, false, true, null, null),
                        new ToolUtils.PropertyInfo("value1", "BOOLEAN", null, true, true, null, null),
                        new ToolUtils.PropertyInfo("operation", "STRING", null, true, true, null, null),
                        new ToolUtils.PropertyInfo("value2", "BOOLEAN", null, true, true, null, null))),
                    new ToolUtils.PropertyInfo("dateTime", "OBJECT", null, false, false, null, List.of(
                        new ToolUtils.PropertyInfo("type", "STRING", null, false, true, null, null),
                        new ToolUtils.PropertyInfo("value1", "DATE_TIME", null, true, true, null, null),
                        new ToolUtils.PropertyInfo("operation", "STRING", null, true, true, null, null),
                        new ToolUtils.PropertyInfo("value2", "DATE_TIME", null, true, true, null, null))),
                    new ToolUtils.PropertyInfo("number", "OBJECT", null, false, false, null, List.of(
                        new ToolUtils.PropertyInfo("type", "STRING", null, false, true, null, null),
                        new ToolUtils.PropertyInfo("value1", "NUMBER", null, true, true, null, null),
                        new ToolUtils.PropertyInfo("operation", "STRING", null, true, true, null, null),
                        new ToolUtils.PropertyInfo("value2", "NUMBER", null, true, true,
                            "conditions[index][index].operation != 'EMPTY'", null))),
                    new ToolUtils.PropertyInfo("string", "OBJECT", null, false, false, null, List.of(
                        new ToolUtils.PropertyInfo("type", "STRING", null, false, true, null, null),
                        new ToolUtils.PropertyInfo("value1", "STRING", null, true, true, null, null),
                        new ToolUtils.PropertyInfo("operation", "STRING", null, true, true, null, null),
                        new ToolUtils.PropertyInfo("value2", "STRING", null, true, true,
                            "!contains({'EMPTY','REGEX'}, conditions[index][index].operation)", null))))))),
            new ToolUtils.PropertyInfo("expression", "STRING", null, false, true, "rawExpression == true", null),
            new ToolUtils.PropertyInfo("caseTrue", "ARRAY", null, false, true, null, List.of(
                new ToolUtils.PropertyInfo(null, "TASK", null, false, true, null, null))),
            new ToolUtils.PropertyInfo("caseFalse", "ARRAY", null, false, true, null, List.of(
                new ToolUtils.PropertyInfo(null, "TASK", null, false, true, null, null))));

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();
        WorkflowValidator.validateTaskParameters(currentTaskParameters, taskDefinition, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateTaskParameters_conditionFlow_rawExpressionTrueWithTask() {
        String currentTaskParameters = """
            {
                "rawExpression": true,
                "expression": "TODO",
                "caseTrue": [
                    {
                        "label": "Task 1",
                        "name": "task1",
                        "type": "component/v1/action1",
                            "parameters": {
                            "name": "John"
                        }
                    }
                ],
                "caseFalse": []
            }
            """;

        List<ToolUtils.PropertyInfo> taskDefinition = List.of(
            new ToolUtils.PropertyInfo("rawExpression", "BOOLEAN", null, false, true, null, null),
            new ToolUtils.PropertyInfo("conditions", "ARRAY", null, false, true, "rawExpression == false", List.of(
                new ToolUtils.PropertyInfo(null, "ARRAY", null, false, false, null, List.of(
                    new ToolUtils.PropertyInfo("boolean", "OBJECT", null, false, false, null, List.of(
                        new ToolUtils.PropertyInfo("type", "STRING", null, false, true, null, null),
                        new ToolUtils.PropertyInfo("value1", "BOOLEAN", null, true, true, null, null),
                        new ToolUtils.PropertyInfo("operation", "STRING", null, true, true, null, null),
                        new ToolUtils.PropertyInfo("value2", "BOOLEAN", null, true, true, null, null))),
                    new ToolUtils.PropertyInfo("dateTime", "OBJECT", null, false, false, null, List.of(
                        new ToolUtils.PropertyInfo("type", "STRING", null, false, true, null, null),
                        new ToolUtils.PropertyInfo("value1", "DATE_TIME", null, true, true, null, null),
                        new ToolUtils.PropertyInfo("operation", "STRING", null, true, true, null, null),
                        new ToolUtils.PropertyInfo("value2", "DATE_TIME", null, true, true, null, null))),
                    new ToolUtils.PropertyInfo("number", "OBJECT", null, false, false, null, List.of(
                        new ToolUtils.PropertyInfo("type", "STRING", null, false, true, null, null),
                        new ToolUtils.PropertyInfo("value1", "NUMBER", null, true, true, null, null),
                        new ToolUtils.PropertyInfo("operation", "STRING", null, true, true, null, null),
                        new ToolUtils.PropertyInfo("value2", "NUMBER", null, true, true,
                            "conditions[index][index].operation != 'EMPTY'", null))),
                    new ToolUtils.PropertyInfo("string", "OBJECT", null, false, false, null, List.of(
                        new ToolUtils.PropertyInfo("type", "STRING", null, false, true, null, null),
                        new ToolUtils.PropertyInfo("value1", "STRING", null, true, true, null, null),
                        new ToolUtils.PropertyInfo("operation", "STRING", null, true, true, null, null),
                        new ToolUtils.PropertyInfo("value2", "STRING", null, true, true,
                            "!contains({'EMPTY','REGEX'}, conditions[index][index].operation)", null))))))),
            new ToolUtils.PropertyInfo("expression", "STRING", null, false, true, "rawExpression == true", null),
            new ToolUtils.PropertyInfo("caseTrue", "ARRAY", null, false, true, null, List.of(
                new ToolUtils.PropertyInfo(null, "TASK", null, false, true, null, null))),
            new ToolUtils.PropertyInfo("caseFalse", "ARRAY", null, false, true, null, List.of(
                new ToolUtils.PropertyInfo(null, "TASK", null, false, true, null, null))));

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();
        WorkflowValidator.validateTaskParameters(currentTaskParameters, taskDefinition, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateTaskParameters_conditionFlow_rawExpressionFalse() {
        String currentTaskParameters = """
            {
                "rawExpression": false,
                "conditions": [
                    [
                        {
                            "type": "boolean",
                            "value1": true,
                            "operation": "EQUALS",
                            "value2": true
                        }
                    ],
                    [
                        {
                            "type": "number",
                            "value1": 6.5,
                            "operation": "GREATER_THAN",
                            "value2": 4.5
                        },
                        {
                            "type": "string",
                            "value1": "true",
                            "operation": "EQUALS",
                            "value2": "fingers"
                        }
                    ]
                ],
                "caseTrue": [],
                "caseFalse": []
            }
            """;

        List<ToolUtils.PropertyInfo> taskDefinition = List.of(
            new ToolUtils.PropertyInfo("rawExpression", "BOOLEAN", null, false, true, null, null),
            new ToolUtils.PropertyInfo("conditions", "ARRAY", null, false, true, "rawExpression == false", List.of(
                new ToolUtils.PropertyInfo(null, "ARRAY", null, false, false, null, List.of(
                    new ToolUtils.PropertyInfo("boolean", "OBJECT", null, false, false, null, List.of(
                        new ToolUtils.PropertyInfo("type", "STRING", null, false, true, null, null),
                        new ToolUtils.PropertyInfo("value1", "BOOLEAN", null, true, true, null, null),
                        new ToolUtils.PropertyInfo("operation", "STRING", null, true, true, null, null),
                        new ToolUtils.PropertyInfo("value2", "BOOLEAN", null, true, true, null, null))),
                    new ToolUtils.PropertyInfo("dateTime", "OBJECT", null, false, false, null, List.of(
                        new ToolUtils.PropertyInfo("type", "STRING", null, false, true, null, null),
                        new ToolUtils.PropertyInfo("value1", "DATE_TIME", null, true, true, null, null),
                        new ToolUtils.PropertyInfo("operation", "STRING", null, true, true, null, null),
                        new ToolUtils.PropertyInfo("value2", "DATE_TIME", null, true, true, null, null))),
                    new ToolUtils.PropertyInfo("number", "OBJECT", null, false, false, null, List.of(
                        new ToolUtils.PropertyInfo("type", "STRING", null, false, true, null, null),
                        new ToolUtils.PropertyInfo("value1", "NUMBER", null, true, true, null, null),
                        new ToolUtils.PropertyInfo("operation", "STRING", null, true, true, null, null),
                        new ToolUtils.PropertyInfo("value2", "NUMBER", null, true, true,
                            "conditions[index][index].operation != 'EMPTY'", null))),
                    new ToolUtils.PropertyInfo("string", "OBJECT", null, false, false, null, List.of(
                        new ToolUtils.PropertyInfo("type", "STRING", null, false, true, null, null),
                        new ToolUtils.PropertyInfo("value1", "STRING", null, true, true, null, null),
                        new ToolUtils.PropertyInfo("operation", "STRING", null, true, true, null, null),
                        new ToolUtils.PropertyInfo("value2", "STRING", null, true, true,
                            "!contains({'EMPTY','REGEX'}, conditions[index][index].operation)", null))))))),
            new ToolUtils.PropertyInfo("expression", "STRING", null, false, true, "rawExpression == true", null),
            new ToolUtils.PropertyInfo("caseTrue", "ARRAY", null, false, true, null, List.of(
                new ToolUtils.PropertyInfo(null, "TASK", null, false, true, null, null))),
            new ToolUtils.PropertyInfo("caseFalse", "ARRAY", null, false, true, null, List.of(
                new ToolUtils.PropertyInfo(null, "TASK", null, false, true, null, null))));

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();
        WorkflowValidator.validateTaskParameters(currentTaskParameters, taskDefinition, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateWorkflowTasks_validTasks_noErrors() {
        String tasksJson = """
            [
                {
                    "label": "Test Task 1",
                    "name": "testTask1",
                    "type": "component/v1/trigger1",
                    "parameters": {
                        "name": "John",
                        "age": 30
                    }
                },
                {
                    "label": "Test Task 2",
                    "name": "testTask2",
                    "type": "component/v1/action1",
                    "parameters": {
                        "active": true,
                        "name": "${testTask1.propString}"
                    }
                }
            ]
            """;

        Map<String, List<ToolUtils.PropertyInfo>> taskDefinitions = Map.of(
            "component/v1/trigger1", List.of(
                new ToolUtils.PropertyInfo("name", "STRING", null, true, true, null, null),
                new ToolUtils.PropertyInfo("age", "INTEGER", null, false, true, null, null)),
            "component/v1/action1", List.of(
                new ToolUtils.PropertyInfo("active", "BOOLEAN", null, true, true, null, null),
                new ToolUtils.PropertyInfo("name", "STRING", null, true, true, null, null)));

        Map<String, ToolUtils.PropertyInfo> taskOutputs = Map.of(
            "component/v1/trigger1", trigger1, "component/v1/action2", action1);

        try {
            JsonNode tasksNode = WorkflowParser.parseJsonString(tasksJson);
            List<JsonNode> tasks = new ArrayList<>();
            for (JsonNode taskNode : tasksNode) {
                tasks.add(taskNode);
            }
            StringBuilder errors = new StringBuilder();
            StringBuilder warnings = new StringBuilder();

            WorkflowValidator.validateWorkflowTasks(tasks, taskDefinitions, taskOutputs, errors, warnings);

            assertEquals("", errors.toString());
            assertEquals("", warnings.toString());
        } catch (Exception e) {
            fail("Should not throw exception for valid tasks: " + e.getMessage());
        }
    }

    @Test
    void validateWorkflowTasks_invalidTaskStructure_addsError() {
        String tasksJson = """
            [
                {
                    "name": "testTask1",
                    "type": "component/v1/trigger1",
                    "parameters": {}
                }
            ]
            """;

        Map<String, List<ToolUtils.PropertyInfo>> taskDefinitions = Map.of(
            "component/v1/trigger1", List.of());

        Map<String, ToolUtils.PropertyInfo> taskOutputs = Map.of(
            "component/v1/trigger1",
            new ToolUtils.PropertyInfo("testTask1", "component/v1/action", "", true, true, null, null));

        try {
            JsonNode tasksNode = WorkflowParser.parseJsonString(tasksJson);
            List<JsonNode> tasks = new ArrayList<>();
            for (JsonNode taskNode : tasksNode) {
                tasks.add(taskNode);
            }
            StringBuilder errors = new StringBuilder();
            StringBuilder warnings = new StringBuilder();

            WorkflowValidator.validateWorkflowTasks(tasks, taskDefinitions, taskOutputs, errors, warnings);

            assertEquals("Missing required field: label", errors.toString());
            assertEquals("", warnings.toString());
        } catch (Exception e) {
            fail("Should not throw exception: " + e.getMessage());
        }
    }

    @Test
    void validateWorkflowTasks_invalidTaskParameters_addsError() {
        String tasksJson = """
            [
                {
                    "label": "Test Task",
                    "name": "testTask1",
                    "type": "component/v1/trigger1",
                    "parameters": {
                        "name": 123
                    }
                }
            ]
            """;

        Map<String, List<ToolUtils.PropertyInfo>> taskDefinitions = Map.of(
            "component/v1/trigger1", List.of(
                new ToolUtils.PropertyInfo("name", "STRING", null, true, true, null, null),
                new ToolUtils.PropertyInfo("age", "INTEGER", null, true, true, null, null)));

        Map<String, ToolUtils.PropertyInfo> taskOutputs = Map.of(
            "component/v1/trigger1", trigger1);

        try {
            JsonNode tasksNode = WorkflowParser.parseJsonString(tasksJson);
            List<JsonNode> tasks = new ArrayList<>();
            for (JsonNode taskNode : tasksNode) {
                tasks.add(taskNode);
            }
            StringBuilder errors = new StringBuilder();
            StringBuilder warnings = new StringBuilder();

            WorkflowValidator.validateWorkflowTasks(tasks, taskDefinitions, taskOutputs, errors, warnings);

            assertEquals("""
                Property 'name' has incorrect type. Expected: string, but got: integer
                Missing required property: age""", errors.toString());
            assertEquals("", warnings.toString());
        } catch (Exception e) {
            fail("Should not throw exception: " + e.getMessage());
        }
    }

    @Test
    void validateWorkflowTasks_noSuchAttributeOutput_addsError() {
        String tasksJson = """
            [
                {
                    "label": "Test Task 1",
                    "name": "testTask1",
                    "type": "component/v1/trigger1",
                    "parameters": {
                        "name": "John",
                        "age": 30
                    }
                },
                {
                    "label": "Test Task 2",
                    "name": "testTask2",
                    "type": "component/v1/action1",
                    "parameters": {
                        "active": "${testTask1.propBool}",
                        "name": "John"
                    }
                }
            ]
            """;

        Map<String, List<ToolUtils.PropertyInfo>> taskDefinitions = Map.of(
            "component/v1/trigger1", List.of(
                new ToolUtils.PropertyInfo("name", "STRING", null, true, true, null, null),
                new ToolUtils.PropertyInfo("age", "INTEGER", null, false, true, null, null)),
            "component/v1/action1", List.of(
                new ToolUtils.PropertyInfo("active", "BOOLEAN", null, true, true, null, null),
                new ToolUtils.PropertyInfo("name", "STRING", null, true, true, null, null)));

        Map<String, ToolUtils.PropertyInfo> taskOutputs = Map.of(
            "component/v1/trigger1", trigger1, "component/v1/action2", action1);

        try {
            JsonNode tasksNode = WorkflowParser.parseJsonString(tasksJson);
            List<JsonNode> tasks = new ArrayList<>();
            for (JsonNode taskNode : tasksNode) {
                tasks.add(taskNode);
            }
            StringBuilder errors = new StringBuilder();
            StringBuilder warnings = new StringBuilder();

            WorkflowValidator.validateWorkflowTasks(tasks, taskDefinitions, taskOutputs, errors, warnings);

            assertEquals("", errors.toString());
            assertEquals("Property 'testTask1.propBool' might not exist in the output of 'component/v1/trigger1'",
                warnings.toString());
        } catch (Exception e) {
            fail("Should not throw exception for valid tasks: " + e.getMessage());
        }
    }

    @Test
    void validateWorkflowTasks_wrongDataPillType_addsError() {
        String tasksJson = """
            [
                {
                    "label": "Test Task 1",
                    "name": "testTask1",
                    "type": "component/v1/trigger1",
                    "parameters": {
                        "name": "John",
                        "age": 30
                    }
                },
                {
                    "label": "Test Task 2",
                    "name": "testTask2",
                    "type": "component/v1/action1",
                    "parameters": {
                        "active": "${testTask1.propString}",
                        "name": "John"
                    }
                }
            ]
            """;

        Map<String, List<ToolUtils.PropertyInfo>> taskDefinitions = Map.of(
            "component/v1/trigger1", List.of(
                new ToolUtils.PropertyInfo("name", "STRING", null, true, true, null, null),
                new ToolUtils.PropertyInfo("age", "INTEGER", null, false, true, null, null)),
            "component/v1/action1", List.of(
                new ToolUtils.PropertyInfo("active", "BOOLEAN", null, true, true, null, null),
                new ToolUtils.PropertyInfo("name", "STRING", null, true, true, null, null)));

        Map<String, ToolUtils.PropertyInfo> taskOutputs = Map.of(
            "component/v1/trigger1", trigger1, "component/v1/action2", action1);

        try {
            JsonNode tasksNode = WorkflowParser.parseJsonString(tasksJson);
            List<JsonNode> tasks = new ArrayList<>();
            for (JsonNode taskNode : tasksNode) {
                tasks.add(taskNode);
            }
            StringBuilder errors = new StringBuilder();
            StringBuilder warnings = new StringBuilder();

            WorkflowValidator.validateWorkflowTasks(tasks, taskDefinitions, taskOutputs, errors, warnings);

            assertEquals(
                "Property 'testTask1.propString' in output of 'component/v1/trigger1' is of type string, not boolean",
                errors.toString());
            assertEquals("", warnings.toString());
        } catch (Exception e) {
            fail("Should not throw exception for valid tasks: " + e.getMessage());
        }
    }

    @Test
    void validateWorkflowTasks_incorrectOutputOrder_addsError() {
        String tasksJson = """
            [
                {
                    "label": "Test Task 1",
                    "name": "testTask1",
                    "type": "component/v1/trigger1",
                    "parameters": {
                        "name": "John",
                        "age": 30
                    }
                },
                {
                    "label": "Test Task 2",
                    "name": "testTask2",
                    "type": "component/v1/action1",
                    "parameters": {
                        "active": "${testTask3.prepNumber}",
                        "name": "${testTask1.prepString}"
                    }
                },
                {
                    "label": "Test Task 3",
                    "name": "testTask3",
                    "type": "component/v1/action2",
                    "parameters": {
                        "active": "${testTask2.propBool}",
                        "name": "${testTask1.propBool}"
                    }
                }
            ]
            """;

        Map<String, List<ToolUtils.PropertyInfo>> taskDefinitions = Map.of(
            "component/v1/trigger1", List.of(
                new ToolUtils.PropertyInfo("name", "STRING", null, true, true, null, null),
                new ToolUtils.PropertyInfo("age", "INTEGER", null, false, true, null, null)),
            "component/v1/action1", List.of(
                new ToolUtils.PropertyInfo("active", "BOOLEAN", null, true, true, null, null),
                new ToolUtils.PropertyInfo("name", "STRING", null, true, true, null, null)),
            "component/v1/action2", List.of(
                new ToolUtils.PropertyInfo("active", "BOOLEAN", null, true, true, null, null),
                new ToolUtils.PropertyInfo("name", "STRING", null, true, true, null, null)));

        Map<String, ToolUtils.PropertyInfo> taskOutputs = Map.of(
            "component/v1/trigger1", trigger1, "component/v1/action2", action3, "component/v1/action1", action1);

        try {
            JsonNode tasksNode = WorkflowParser.parseJsonString(tasksJson);
            List<JsonNode> tasks = new ArrayList<>();
            for (JsonNode taskNode : tasksNode) {
                tasks.add(taskNode);
            }
            StringBuilder errors = new StringBuilder();
            StringBuilder warnings = new StringBuilder();

            WorkflowValidator.validateWorkflowTasks(tasks, taskDefinitions, taskOutputs, errors, warnings);

            assertEquals("Wrong task order: You can't reference 'testTask3.prepNumber' in testTask2",
                errors.toString());
            assertEquals("Property 'testTask1.propBool' might not exist in the output of 'component/v1/trigger1'",
                warnings.toString());
        } catch (Exception e) {
            fail("Should not throw exception for valid tasks: " + e.getMessage());
        }
    }

    @Test
    void validateWorkflowTasks_taskWithMissingParametersField_handlesCorrectly() {
        String tasksJson = """
            [
                {
                    "label": "Test Task",
                    "name": "testTask1",
                    "type": "component/v1/trigger1"
                }
            ]
            """;

        Map<String, List<ToolUtils.PropertyInfo>> taskDefinitions = Map.of(
            "component/v1/trigger1", List.of());

        Map<String, ToolUtils.PropertyInfo> taskOutputs = Map.of(
            "component/v1/trigger1", trigger1);

        try {
            JsonNode tasksNode = WorkflowParser.parseJsonString(tasksJson);
            List<JsonNode> tasks = new ArrayList<>();
            for (JsonNode taskNode : tasksNode) {
                tasks.add(taskNode);
            }
            StringBuilder errors = new StringBuilder();
            StringBuilder warnings = new StringBuilder();

            WorkflowValidator.validateWorkflowTasks(tasks, taskDefinitions, taskOutputs, errors, warnings);

            assertTrue(errors.toString()
                .contains("Missing required field: parameters") ||
                errors.toString()
                    .contains("Task definition must have a 'parameters' object"));
        } catch (Exception e) {
            fail("Should handle missing parameters field: " + e.getMessage());
        }
    }

    @Test
    void validateWorkflowTasks_multipleTasks_validatesAll() {
        String tasksJson = """
            [
                {
                    "label": "Valid Task",
                    "name": "validTask",
                    "type": "component/v1/trigger1",
                    "parameters": {
                        "name": "John"
                    }
                },
                {
                    "name": "invalidTask",
                    "type": "component/v1/action1",
                    "parameters": {
                        "age": "thirty"
                    }
                }
            ]
            """;

        Map<String, List<ToolUtils.PropertyInfo>> taskDefinitions = Map.of(
            "component/v1/trigger1", List.of(
                new ToolUtils.PropertyInfo("name", "STRING", null, true, true, null, null)),
            "component/v1/action1", List.of(
                new ToolUtils.PropertyInfo("age", "INTEGER", null, true, true, null, null)));

        Map<String, ToolUtils.PropertyInfo> taskOutputs = Map.of(
            "component/v1/trigger1", trigger1,
            "component/v1/action1", action1);

        try {
            JsonNode tasksNode = WorkflowParser.parseJsonString(tasksJson);
            List<JsonNode> tasks = new ArrayList<>();
            for (JsonNode taskNode : tasksNode) {
                tasks.add(taskNode);
            }
            StringBuilder errors = new StringBuilder();
            StringBuilder warnings = new StringBuilder();

            WorkflowValidator.validateWorkflowTasks(tasks, taskDefinitions, taskOutputs, errors, warnings);

            assertEquals("""
                Missing required field: label
                Property 'age' has incorrect type. Expected: integer, but got: string""", errors.toString());
            assertEquals("", warnings.toString());
        } catch (Exception e) {
            fail("Should not throw exception: " + e.getMessage());
        }
    }

    @Test
    void validateTaskDataPills_arrayParameters_validatesCorrectly() {
        String tasksJson = """
            [
                {
                    "label": "Task 1",
                    "name": "task1",
                    "type": "component/v1/trigger1",
                    "parameters": {
                        "name": "John"
                    }
                },
                {
                    "label": "Task 2",
                    "name": "task2",
                    "type": "component/v1/action1",
                    "parameters": {
                        "items": ["${task1.propString}", "literal value"]
                    }
                }
            ]
            """;

        Map<String, List<ToolUtils.PropertyInfo>> taskDefinitions = Map.of(
            "component/v1/trigger1", List.of(
                new ToolUtils.PropertyInfo("name", "STRING", null, false, true, null, null)),
            "component/v1/action1", List.of(
                new ToolUtils.PropertyInfo("items", "ARRAY", null, false, true, null, null)));

        Map<String, ToolUtils.PropertyInfo> taskOutputs = Map.of(
            "component/v1/trigger1", trigger1);

        try {
            JsonNode tasksNode = WorkflowParser.parseJsonString(tasksJson);
            List<JsonNode> tasks = new ArrayList<>();
            for (JsonNode taskNode : tasksNode) {
                tasks.add(taskNode);
            }
            StringBuilder errors = new StringBuilder();
            StringBuilder warnings = new StringBuilder();

            WorkflowValidator.validateWorkflowTasks(tasks, taskDefinitions, taskOutputs, errors, warnings);

            assertEquals("", errors.toString());
            assertEquals("", warnings.toString());
        } catch (Exception e) {
            fail("Should not throw exception: " + e.getMessage());
        }
    }

    @Test
    void validateTaskDataPills_multipleDataPillsInSameValue_validatesAll() {
        String tasksJson = """
            [
                {
                    "label": "Task 1",
                    "name": "task1",
                    "type": "component/v1/trigger1",
                    "parameters": {
                        "name": "John"
                    }
                },
                {
                    "label": "Task 2",
                    "name": "task2",
                    "type": "component/v1/action1",
                    "parameters": {
                        "date": "2025-09-23T11:46:19"
                    }
                },
                {
                    "label": "Task 3",
                    "name": "task3",
                    "type": "component/v1/action1",
                    "parameters": {
                        "name": "Name: ${task1.propString}, Other: ${task2.propDateTime}",
                        "date": "${task2.propDateTime}"
                    }
                }
            ]
            """;

        Map<String, List<ToolUtils.PropertyInfo>> taskDefinitions = Map.of(
            "component/v1/trigger1", List.of(
                new ToolUtils.PropertyInfo("name", "STRING", null, false, true, null, null)),
            "component/v1/action1", List.of(
                new ToolUtils.PropertyInfo("name", "STRING", null, false, true, null, null),
                new ToolUtils.PropertyInfo("date", "DATE_TIME", null, false, true, null, null)));

        Map<String, ToolUtils.PropertyInfo> taskOutputs = Map.of(
            "component/v1/trigger1", trigger1,
            "component/v1/action1", action4);

        try {
            JsonNode tasksNode = WorkflowParser.parseJsonString(tasksJson);
            List<JsonNode> tasks = new ArrayList<>();
            for (JsonNode taskNode : tasksNode) {
                tasks.add(taskNode);
            }
            StringBuilder errors = new StringBuilder();
            StringBuilder warnings = new StringBuilder();

            WorkflowValidator.validateWorkflowTasks(tasks, taskDefinitions, taskOutputs, errors, warnings);

            // this is an exception, every value can be converted to string
            assertEquals("", errors.toString());
            assertEquals("", warnings.toString());
        } catch (Exception e) {
            fail("Should not throw exception: " + e.getMessage());
        }
    }

    @Test
    void validateTaskDataPills_nonExistentTask_ignoresGracefully() {
        String tasksJson = """
            [
                {
                    "label": "Task 1",
                    "name": "task1",
                    "type": "component/v1/trigger1",
                    "parameters": {
                        "name": "John"
                    }
                },
                {
                    "label": "Task 2",
                    "name": "task2",
                    "type": "component/v1/action1",
                    "parameters": {
                        "name": "${invalidformat}",
                        "active": "${task1}"
                    }
                }
            ]
            """;

        Map<String, List<ToolUtils.PropertyInfo>> taskDefinitions = Map.of(
            "component/v1/trigger1", List.of(
                new ToolUtils.PropertyInfo("name", "STRING", null, false, true, null, null)),
            "component/v1/action1", List.of(
                new ToolUtils.PropertyInfo("name", "STRING", null, false, true, null, null),
                new ToolUtils.PropertyInfo("active", "BOOLEAN", null, false, true, null, null)));

        Map<String, ToolUtils.PropertyInfo> taskOutputs = Map.of(
            "component/v1/trigger1", trigger1);

        try {
            JsonNode tasksNode = WorkflowParser.parseJsonString(tasksJson);
            List<JsonNode> tasks = new ArrayList<>();
            for (JsonNode taskNode : tasksNode) {
                tasks.add(taskNode);
            }
            StringBuilder errors = new StringBuilder();
            StringBuilder warnings = new StringBuilder();

            WorkflowValidator.validateWorkflowTasks(tasks, taskDefinitions, taskOutputs, errors, warnings);

            assertEquals("Task 'invalidformat' doesn't exits.", errors.toString());
            assertEquals("", warnings.toString());
        } catch (Exception e) {
            fail("Should not throw exception: " + e.getMessage());
        }
    }

    @Test
    void validateTaskDataPills_missingTaskOutputInfo_handlesGracefully() {
        String tasksJson = """
            [
                {
                    "label": "Task 1",
                    "name": "task1",
                    "type": "component/v1/trigger1",
                    "parameters": {
                        "name": "John"
                    }
                },
                {
                    "label": "Task 2",
                    "name": "task2",
                    "type": "component/v1/action1",
                    "parameters": {
                        "name": "${task1.propString}"
                    }
                }
            ]
            """;

        Map<String, List<ToolUtils.PropertyInfo>> taskDefinitions = Map.of(
            "component/v1/trigger1", List.of(
                new ToolUtils.PropertyInfo("name", "STRING", null, false, true, null, null)),
            "component/v1/action1", List.of(
                new ToolUtils.PropertyInfo("name", "STRING", null, false, true, null, null)));

        Map<String, ToolUtils.PropertyInfo> taskOutputs = Map.of();

        try {
            JsonNode tasksNode = WorkflowParser.parseJsonString(tasksJson);
            List<JsonNode> tasks = new ArrayList<>();
            for (JsonNode taskNode : tasksNode) {
                tasks.add(taskNode);
            }
            StringBuilder errors = new StringBuilder();
            StringBuilder warnings = new StringBuilder();

            WorkflowValidator.validateWorkflowTasks(tasks, taskDefinitions, taskOutputs, errors, warnings);

            assertEquals("", errors.toString());
            assertEquals("Property 'task1.propString' might not exist in the output of 'component/v1/trigger1'",
                warnings.toString());
        } catch (Exception e) {
            fail("Should not throw exception: " + e.getMessage());
        }
    }

    @Test
    void validateTaskDataPills_objectTypeValidation_validatesCorrectly() {
        String tasksJson = """
            [
                {
                    "label": "Task 1",
                    "name": "task1",
                    "type": "component/v1/trigger1",
                    "parameters": {
                        "name": "John"
                    }
                },
                {
                    "label": "Task 2",
                    "name": "task2",
                    "type": "component/v1/action1",
                    "parameters": {
                        "active": "${task1.element.propBool}",
                        "height": "${task1.element.propNumber}",
                        "age": "${task1.propInteger}"
                    }
                },
                {
                    "label": "Task 3",
                    "name": "task3",
                    "type": "component/v1/action2",
                    "parameters": {
                        "activeString": "${task1.element.propBool}",
                        "heightString": "${task1.element.propNumber}",
                        "ageString": "${task1.propInteger}"
                    }
                }
            ]
            """;

        Map<String, List<ToolUtils.PropertyInfo>> taskDefinitions = Map.of(
            "component/v1/trigger1", List.of(
                new ToolUtils.PropertyInfo("name", "STRING", null, false, true, null, null)),
            "component/v1/action1", List.of(
                new ToolUtils.PropertyInfo("active", "BOOLEAN", null, false, true, null, null),
                new ToolUtils.PropertyInfo("height", "NUMBER", null, false, true, null, null),
                new ToolUtils.PropertyInfo("age", "INTEGER", null, false, true, null, null)),
            "component/v1/action2", List.of(
                new ToolUtils.PropertyInfo("activeString", "STRING", null, false, true, null, null),
                new ToolUtils.PropertyInfo("heightString", "STRING", null, false, true, null, null),
                new ToolUtils.PropertyInfo("ageString", "STRING", null, false, true, null, null)));

        Map<String, ToolUtils.PropertyInfo> taskOutputs = Map.of(
            "component/v1/trigger1", actionObj);

        try {
            JsonNode tasksNode = WorkflowParser.parseJsonString(tasksJson);
            List<JsonNode> tasks = new ArrayList<>();
            for (JsonNode taskNode : tasksNode) {
                tasks.add(taskNode);
            }
            StringBuilder errors = new StringBuilder();
            StringBuilder warnings = new StringBuilder();

            WorkflowValidator.validateWorkflowTasks(tasks, taskDefinitions, taskOutputs, errors, warnings);

            assertEquals("", errors.toString());
            assertEquals("", warnings.toString());
        } catch (Exception e) {
            fail("Should not throw exception: " + e.getMessage());
        }
    }

    @Test
    void validateTaskDataPills_arrayTypeValidation_validatesCorrectly() {
        String tasksJson = """
            [
                {
                    "label": "Task 1",
                    "name": "task1",
                    "type": "component/v1/trigger1",
                    "parameters": {
                        "name": "John"
                    }
                },
                {
                    "label": "Task 2",
                    "name": "task2",
                    "type": "component/v1/action1",
                    "parameters": {
                        "active": "${task1.elements[0].propBool}",
                        "height": "${task1.elements[0].propNumber}",
                        "age": "${task1.elements[0].propInteger}"
                    }
                }
            ]
            """;

        Map<String, List<ToolUtils.PropertyInfo>> taskDefinitions = Map.of(
            "component/v1/trigger1", List.of(
                new ToolUtils.PropertyInfo("name", "STRING", null, false, true, null, null)),
            "component/v1/action1", List.of(
                new ToolUtils.PropertyInfo("active", "BOOLEAN", null, false, true, null, null),
                new ToolUtils.PropertyInfo("height", "NUMBER", null, false, true, null, null),
                new ToolUtils.PropertyInfo("age", "INTEGER", null, false, true, null, null)));

        Map<String, ToolUtils.PropertyInfo> taskOutputs = Map.of(
            "component/v1/trigger1", actionArr);

        try {
            JsonNode tasksNode = WorkflowParser.parseJsonString(tasksJson);
            List<JsonNode> tasks = new ArrayList<>();
            for (JsonNode taskNode : tasksNode) {
                tasks.add(taskNode);
            }
            StringBuilder errors = new StringBuilder();
            StringBuilder warnings = new StringBuilder();

            WorkflowValidator.validateWorkflowTasks(tasks, taskDefinitions, taskOutputs, errors, warnings);

            assertEquals("", errors.toString());
            assertEquals("", warnings.toString());
        } catch (Exception e) {
            fail("Should not throw exception: " + e.getMessage());
        }
    }

    @Test
    void validateTaskDataPills_complexNestedArrayWithDataPills_validatesCorrectly() {
        String tasksJson = """
            [
                {
                    "label": "Task 1",
                    "name": "task1",
                    "type": "component/v1/trigger1",
                    "parameters": {
                        "name": "John"
                    }
                },
                {
                    "label": "Task 2",
                    "name": "task2",
                    "type": "component/v1/action1",
                    "parameters": {
                        "configs": [
                            {
                                "setting": "${task1.propInvalid}",
                                "enabled": true
                            },
                            {
                                "setting": "static value",
                                "enabled": false
                            }
                        ]
                    }
                }
            ]
            """;

        Map<String, List<ToolUtils.PropertyInfo>> taskDefinitions = Map.of(
            "component/v1/trigger1", List.of(
                new ToolUtils.PropertyInfo("name", "STRING", null, false, true, null, null)),
            "component/v1/action1", List.of(
                new ToolUtils.PropertyInfo("configs", "ARRAY", null, false, true, null, List.of(
                    new ToolUtils.PropertyInfo("setting", "STRING", null, true, true, null, null),
                    new ToolUtils.PropertyInfo("enabled", "BOOLEAN", null, false, true, null, null)))));

        Map<String, ToolUtils.PropertyInfo> taskOutputs = Map.of(
            "component/v1/trigger1", trigger1);

        try {
            JsonNode tasksNode = WorkflowParser.parseJsonString(tasksJson);
            List<JsonNode> tasks = new ArrayList<>();
            for (JsonNode taskNode : tasksNode) {
                tasks.add(taskNode);
            }
            StringBuilder errors = new StringBuilder();
            StringBuilder warnings = new StringBuilder();

            WorkflowValidator.validateWorkflowTasks(tasks, taskDefinitions, taskOutputs, errors, warnings);

            assertEquals("", errors.toString());
            assertEquals("Property 'task1.propInvalid' might not exist in the output of 'component/v1/trigger1'",
                warnings.toString());
        } catch (Exception e) {
            fail("Should not throw exception: " + e.getMessage());
        }
    }

    @Test
    void validateTaskDataPills_malformedDataPillFormat_ignoresGracefully() {
        String tasksJson = """
            [
                {
                    "label": "Task 1",
                    "name": "task1",
                    "type": "component/v1/trigger1",
                    "parameters": {
                        "name": "John"
                    }
                },
                {
                    "label": "Task 2",
                    "name": "task2",
                    "type": "component/v1/action1",
                    "parameters": {
                        "name": "${incomplete",
                        "active": "${}",
                        "value": "normal text"
                    }
                }
            ]
            """;

        Map<String, List<ToolUtils.PropertyInfo>> taskDefinitions = Map.of(
            "component/v1/trigger1", List.of(
                new ToolUtils.PropertyInfo("name", "STRING", null, false, true, null, null)),
            "component/v1/action1", List.of(
                new ToolUtils.PropertyInfo("name", "STRING", null, false, true, null, null),
                new ToolUtils.PropertyInfo("active", "BOOLEAN", null, false, true, null, null),
                new ToolUtils.PropertyInfo("value", "STRING", null, false, true, null, null)));

        Map<String, ToolUtils.PropertyInfo> taskOutputs = Map.of(
            "component/v1/trigger1", trigger1);

        try {
            JsonNode tasksNode = WorkflowParser.parseJsonString(tasksJson);
            List<JsonNode> tasks = new ArrayList<>();
            for (JsonNode taskNode : tasksNode) {
                tasks.add(taskNode);
            }
            StringBuilder errors = new StringBuilder();
            StringBuilder warnings = new StringBuilder();

            WorkflowValidator.validateWorkflowTasks(tasks, taskDefinitions, taskOutputs, errors, warnings);

            assertEquals("Property 'active' has incorrect type. Expected: boolean, but got: string", errors.toString());
            assertEquals("", warnings.toString());
        } catch (Exception e) {
            fail("Should not throw exception: " + e.getMessage());
        }
    }

    @Test
    void validateWorkflowTasks_flowLoop_wrongTypes() {
        String tasksJson = """
            [
                {
                    "label": "Task 1",
                    "name": "task1",
                    "type": "component/v1/trigger1",
                    "parameters": {
                        "name": "John"
                    }
                },
                {
                     "label": "Loop",
                     "name": "loop1",
                     "type": "loop/v1",
                     "parameters": {
                         "items": [
                            "Marko",
                            13.2,
                            {
                                "name": "Hello"
                            }
                         ],
                         "loopForever": false,
                         "iteratee": [
                            {
                                "label": "Task 3",
                                "name": "task3",
                                "type": "component/v1/action1",
                                "parameters": {
                                    "name": "${loop1.item}"
                                }
                            },
                            {
                                "label": "Task 2",
                                "name": "task2",
                                "type": "component/v1/action2",
                                "parameters": {
                                    "age": "${loop1.item}"
                                }
                            }
                         ]
                     }
                }
            ]
            """;

        Map<String, List<ToolUtils.PropertyInfo>> taskDefinitions = Map.of(
            "component/v1/trigger1", List.of(
                new ToolUtils.PropertyInfo("name", "STRING", null, false, true, null, null)),
            "loop/v1", List.of(
                new ToolUtils.PropertyInfo("items", "ARRAY", null, false, true, null, List.of()),
                new ToolUtils.PropertyInfo("loopForever", "BOOLEAN", null, false, true, null, null),
                new ToolUtils.PropertyInfo("iteratee", "ARRAY", null, false, true, null, List.of(
                    new ToolUtils.PropertyInfo(null, "TASK", null, false, true, null, null)))),
            "component/v1/action1", List.of(
                new ToolUtils.PropertyInfo("name", "STRING", null, false, true, null, null)),
            "component/v1/action2", List.of(
                new ToolUtils.PropertyInfo("age", "NUMBER", null, false, true, null, null)));

        Map<String, ToolUtils.PropertyInfo> taskOutputs = Map.of(
            "component/v1/trigger1", actionArr,
            "component/v1/action1", action1);

        try {
            JsonNode tasksNode = WorkflowParser.parseJsonString(tasksJson);
            List<JsonNode> tasks = new ArrayList<>();
            for (JsonNode taskNode : tasksNode) {
                tasks.add(taskNode);
            }
            StringBuilder errors = new StringBuilder();
            StringBuilder warnings = new StringBuilder();

            WorkflowValidator.validateWorkflowTasks(tasks, taskDefinitions, taskOutputs, errors, warnings);

            assertEquals("""
                Property 'loop1.item[0]' in output of 'loop/v1' is of type string, not number
                Property 'loop1.item[2]' in output of 'loop/v1' is of type object, not number""", errors.toString());
            assertEquals("", warnings.toString());
        } catch (Exception e) {
            fail("Should not throw exception: " + e.getMessage());
        }
    }

    @Test
    void validateWorkflowTasks_flowLoop_wrongParameters() {
        String tasksJson = """
            [
                {
                    "label": "Task 1",
                    "name": "task1",
                    "type": "component/v1/trigger1",
                    "parameters": {
                        "name": "John"
                    }
                },
                {
                     "label": "Loop",
                     "name": "loop1",
                     "type": "loop/v1",
                     "parameters": {
                         "items": "${task1.elements}",
                         "loopForever": false,
                         "iteratee": [
                            {
                                "label": "Task 3",
                                "name": "task3",
                                "type": "component/v1/action1",
                                "parameters": {
                                    "name": "${loop1.item}"
                                }
                            },
                            {
                                "label": "Task 2",
                                "name": "task2",
                                "type": "component/v1/action2",
                                "parameters": {
                                    "age": "${loop1.item}"
                                }
                            }
                         ]
                     }
                }
            ]
            """;

        Map<String, List<ToolUtils.PropertyInfo>> taskDefinitions = Map.of(
            "component/v1/trigger1", List.of(
                new ToolUtils.PropertyInfo("name", "STRING", null, false, true, null, null)),
            "loop/v1", List.of(
                new ToolUtils.PropertyInfo("items", "ARRAY", null, false, true, null, List.of()),
                new ToolUtils.PropertyInfo("loopForever", "BOOLEAN", null, false, true, null, null),
                new ToolUtils.PropertyInfo("iteratee", "ARRAY", null, false, true, null, List.of(
                    new ToolUtils.PropertyInfo(null, "TASK", null, false, true, null, null)))),
            "component/v1/action1", List.of(
                new ToolUtils.PropertyInfo("name", "STRING", null, false, true, null, null)),
            "component/v1/action2", List.of(
                new ToolUtils.PropertyInfo("age", "NUMBER", null, false, true, null, null)));

        Map<String, ToolUtils.PropertyInfo> taskOutputs = Map.of(
            "component/v1/trigger1", actionArr,
            "component/v1/action1", action1);

        try {
            JsonNode tasksNode = WorkflowParser.parseJsonString(tasksJson);
            List<JsonNode> tasks = new ArrayList<>();
            for (JsonNode taskNode : tasksNode) {
                tasks.add(taskNode);
            }
            StringBuilder errors = new StringBuilder();
            StringBuilder warnings = new StringBuilder();

            WorkflowValidator.validateWorkflowTasks(tasks, taskDefinitions, taskOutputs, errors, warnings);

            assertEquals("Property 'loop1.item[0]' in output of 'loop/v1' is of type boolean, not number",
                errors.toString());
            assertEquals("", warnings.toString());
        } catch (Exception e) {
            fail("Should not throw exception: " + e.getMessage());
        }
    }

    @Test
    void validateWorkflowTasks_flowCondition_noErrors() {
        String tasksJson = """
            [
                {
                    "label": "Task 1",
                    "name": "task1",
                    "type": "component/v1/trigger1",
                    "parameters": {
                        "name": "John"
                    }
                },
                {
                     "label": "Condition",
                     "name": "condition_1",
                     "type": "condition/v1",
                     "parameters": {
                         "rawExpression": false,
                         "conditions": [
                            [
                                {
                                    "type": "string",
                                    "value1": "${task1.propString}",
                                    "operation": "EQUALS",
                                    "value2": "Mario"
                                }
                            ]
                         ],
                         "caseTrue": [
                            {
                                "label": "Task 3",
                                "name": "task3",
                                "type": "component/v1/action1",
                                "parameters": {
                                    "name": "${task1.propString}"
                                }
                            }
                         ],
                         "caseFalse": [
                            {
                                "label": "Task 2",
                                "name": "task2",
                                "type": "component/v1/action1",
                                "parameters": {
                                    "name": "${task1.propString}"
                                }
                            }
                         ]
                     }
                }
            ]
            """;

        Map<String, List<ToolUtils.PropertyInfo>> taskDefinitions = Map.of(
            "component/v1/trigger1", List.of(
                new ToolUtils.PropertyInfo("name", "STRING", null, false, true, null, null)),
            "condition/v1", List.of(
                new ToolUtils.PropertyInfo("rawExpression", "BOOLEAN", null, false, true, null, null),
                new ToolUtils.PropertyInfo("conditions", "ARRAY", null, false, true, "rawExpression == false", List.of(
                    new ToolUtils.PropertyInfo(null, "ARRAY", null, false, false, null, List.of(
                        new ToolUtils.PropertyInfo("boolean", "OBJECT", null, false, false, null, List.of(
                            new ToolUtils.PropertyInfo("type", "STRING", null, false, true, null, null),
                            new ToolUtils.PropertyInfo("value1", "BOOLEAN", null, true, true, null, null),
                            new ToolUtils.PropertyInfo("operation", "STRING", null, true, true, null, null),
                            new ToolUtils.PropertyInfo("value2", "BOOLEAN", null, true, true, null, null))),
                        new ToolUtils.PropertyInfo("dateTime", "OBJECT", null, false, false, null, List.of(
                            new ToolUtils.PropertyInfo("type", "STRING", null, false, true, null, null),
                            new ToolUtils.PropertyInfo("value1", "DATE_TIME", null, true, true, null, null),
                            new ToolUtils.PropertyInfo("operation", "STRING", null, true, true, null, null),
                            new ToolUtils.PropertyInfo("value2", "DATE_TIME", null, true, true, null, null))),
                        new ToolUtils.PropertyInfo("number", "OBJECT", null, false, false, null, List.of(
                            new ToolUtils.PropertyInfo("type", "STRING", null, false, true, null, null),
                            new ToolUtils.PropertyInfo("value1", "NUMBER", null, true, true, null, null),
                            new ToolUtils.PropertyInfo("operation", "STRING", null, true, true, null, null),
                            new ToolUtils.PropertyInfo("value2", "NUMBER", null, true, true,
                                "conditions[index][index].operation != 'EMPTY'", null))),
                        new ToolUtils.PropertyInfo("string", "OBJECT", null, false, false, null, List.of(
                            new ToolUtils.PropertyInfo("type", "STRING", null, false, true, null, null),
                            new ToolUtils.PropertyInfo("value1", "STRING", null, true, true, null, null),
                            new ToolUtils.PropertyInfo("operation", "STRING", null, true, true, null, null),
                            new ToolUtils.PropertyInfo("value2", "STRING", null, true, true,
                                "!contains({'EMPTY','REGEX'}, conditions[index][index].operation)", null))))))),
                new ToolUtils.PropertyInfo("expression", "STRING", null, false, true, "rawExpression == true", null),
                new ToolUtils.PropertyInfo("caseTrue", "ARRAY", null, false, true, null, List.of(
                    new ToolUtils.PropertyInfo(null, "TASK", null, false, false, null, null))),
                new ToolUtils.PropertyInfo("caseFalse", "ARRAY", null, false, true, null, List.of(
                    new ToolUtils.PropertyInfo(null, "TASK", null, false, false, null, null)))),
            "component/v1/action1", List.of(
                new ToolUtils.PropertyInfo("name", "STRING", null, false, true, null, null)));

        Map<String, ToolUtils.PropertyInfo> taskOutputs = Map.of(
            "component/v1/trigger1", trigger1,
            "component/v1/action1", action1);

        try {
            JsonNode tasksNode = WorkflowParser.parseJsonString(tasksJson);
            List<JsonNode> tasks = new ArrayList<>();
            for (JsonNode taskNode : tasksNode) {
                tasks.add(taskNode);
            }
            StringBuilder errors = new StringBuilder();
            StringBuilder warnings = new StringBuilder();

            WorkflowValidator.validateWorkflowTasks(tasks, taskDefinitions, taskOutputs, errors, warnings);

            assertEquals("", errors.toString());
            assertEquals("", warnings.toString());
        } catch (Exception e) {
            fail("Should not throw exception: " + e.getMessage());
        }
    }

    @Test
    void validateWorkflowTasks_flowCondition_withErrors() {
        String tasksJson = """
            [
                {
                    "label": "Task 1",
                    "name": "task1",
                    "type": "component/v1/trigger1",
                    "parameters": {
                        "name": "John"
                    }
                },
                {
                     "label": "Condition",
                     "name": "condition_1",
                     "type": "condition/v1",
                     "parameters": {
                         "rawExpression": false,
                         "conditions": [
                            [
                                {
                                    "type": "string",
                                    "value1": "${task1.propString}",
                                    "operation": "EQUALS",
                                    "value2": "Mario"
                                },
                                {
                                    "type": "number",
                                    "value1": "${task1.propNumber}",
                                    "operation": "GREATER_THAN",
                                    "value2": 10
                                }
                            ]
                         ],
                         "caseTrue": [
                            {
                                "label": "Task 3",
                                "name": "task3",
                                "type": "component/v1/action1",
                                "parameters": {
                                    "name": "${task1.propString}"
                                }
                            }
                         ],
                         "caseFalse": [
                            {
                                "label": "Task 2",
                                "name": "task2",
                                "type": "component/v1/action1",
                                "parameters": {
                                    "age": "${task1.propString}"
                                }
                            }
                         ]
                     }
                }
            ]
            """;

        Map<String, List<ToolUtils.PropertyInfo>> taskDefinitions = Map.of(
            "component/v1/trigger1", List.of(
                new ToolUtils.PropertyInfo("name", "STRING", null, false, true, null, null)),
            "condition/v1", List.of(
                new ToolUtils.PropertyInfo("rawExpression", "BOOLEAN", null, false, true, null, null),
                new ToolUtils.PropertyInfo("conditions", "ARRAY", null, false, true, "rawExpression == false", List.of(
                    new ToolUtils.PropertyInfo(null, "ARRAY", null, false, false, null, List.of(
                        new ToolUtils.PropertyInfo("boolean", "OBJECT", null, false, false, null, List.of(
                            new ToolUtils.PropertyInfo("type", "STRING", null, false, true, null, null),
                            new ToolUtils.PropertyInfo("value1", "BOOLEAN", null, true, true, null, null),
                            new ToolUtils.PropertyInfo("operation", "STRING", null, true, true, null, null),
                            new ToolUtils.PropertyInfo("value2", "BOOLEAN", null, true, true, null, null))),
                        new ToolUtils.PropertyInfo("dateTime", "OBJECT", null, false, false, null, List.of(
                            new ToolUtils.PropertyInfo("type", "STRING", null, false, true, null, null),
                            new ToolUtils.PropertyInfo("value1", "DATE_TIME", null, true, true, null, null),
                            new ToolUtils.PropertyInfo("operation", "STRING", null, true, true, null, null),
                            new ToolUtils.PropertyInfo("value2", "DATE_TIME", null, true, true, null, null))),
                        new ToolUtils.PropertyInfo("number", "OBJECT", null, false, false, null, List.of(
                            new ToolUtils.PropertyInfo("type", "STRING", null, false, true, null, null),
                            new ToolUtils.PropertyInfo("value1", "NUMBER", null, true, true, null, null),
                            new ToolUtils.PropertyInfo("operation", "STRING", null, true, true, null, null),
                            new ToolUtils.PropertyInfo("value2", "NUMBER", null, true, true,
                                "conditions[index][index].operation != 'EMPTY'", null))),
                        new ToolUtils.PropertyInfo("string", "OBJECT", null, false, false, null, List.of(
                            new ToolUtils.PropertyInfo("type", "STRING", null, false, true, null, null),
                            new ToolUtils.PropertyInfo("value1", "STRING", null, true, true, null, null),
                            new ToolUtils.PropertyInfo("operation", "STRING", null, true, true, null, null),
                            new ToolUtils.PropertyInfo("value2", "STRING", null, true, true,
                                "!contains({'EMPTY','REGEX'}, conditions[index][index].operation)", null))))))),
                new ToolUtils.PropertyInfo("expression", "STRING", null, false, true, "rawExpression == true", null),
                new ToolUtils.PropertyInfo("caseTrue", "ARRAY", null, false, true, null, List.of(
                    new ToolUtils.PropertyInfo(null, "TASK", null, false, false, null, null))),
                new ToolUtils.PropertyInfo("caseFalse", "ARRAY", null, false, true, null, List.of(
                    new ToolUtils.PropertyInfo(null, "TASK", null, false, false, null, null)))),
            "component/v1/action1", List.of(
                new ToolUtils.PropertyInfo("name", "STRING", null, true, true, null, null)));

        Map<String, ToolUtils.PropertyInfo> taskOutputs = Map.of(
            "component/v1/trigger1", trigger1,
            "component/v1/action1", action1);

        try {
            JsonNode tasksNode = WorkflowParser.parseJsonString(tasksJson);
            List<JsonNode> tasks = new ArrayList<>();
            for (JsonNode taskNode : tasksNode) {
                tasks.add(taskNode);
            }
            StringBuilder errors = new StringBuilder();
            StringBuilder warnings = new StringBuilder();

            WorkflowValidator.validateWorkflowTasks(tasks, taskDefinitions, taskOutputs, errors, warnings);

            assertEquals("Missing required property: name", errors.toString());
            assertEquals("""
                Property 'age' is not defined in task definition
                Property 'task1.propNumber' might not exist in the output of 'component/v1/trigger1'""",
                warnings.toString());
        } catch (Exception e) {
            fail("Should not throw exception: " + e.getMessage());
        }
    }

    @Test
    void validateWorkflowTasks_flowConditionAndLoop_noErrors() {
        String tasksJson = """
            [
                {
                    "label": "Task 1",
                    "name": "task1",
                    "type": "component/v1/trigger1",
                    "parameters": {
                        "name": "John"
                    }
                },
                {
                     "label": "Condition",
                     "name": "condition_1",
                     "type": "condition/v1",
                     "parameters": {
                         "rawExpression": false,
                         "conditions": [
                            [
                                {
                                    "type": "string",
                                    "value1": "${task1.elements[0]}",
                                    "operation": "EQUALS",
                                    "value2": "Mario"
                                }
                            ]
                         ],
                         "caseTrue": [
                             {
                                "label": "Loop",
                                "name": "loop1",
                                "type": "loop/v1",
                                "parameters": {
                                    "items": "${task1.elements}",
                                    "loopForever": false,
                                    "iteratee": [
                                        {
                                           "label": "Task 3",
                                           "name": "task3",
                                           "type": "component/v1/action1",
                                           "parameters": {
                                               "name": "${loop1.item}"
                                           }
                                        },
                                        {
                                           "label": "Task 4",
                                           "name": "task4",
                                           "type": "component/v1/action2",
                                           "parameters": {
                                               "age": "${loop1.item}"
                                           }
                                        }
                                    ]
                                }
                             }
                         ],
                         "caseFalse": [
                            {
                                "label": "Task 2",
                                "name": "task2",
                                "type": "component/v1/action1",
                                "parameters": {
                                    "name": "${task1.propString}"
                                }
                            }
                         ]
                     }
                }
            ]
            """;

        Map<String, List<ToolUtils.PropertyInfo>> taskDefinitions = Map.of(
            "component/v1/trigger1", List.of(
                new ToolUtils.PropertyInfo("name", "STRING", null, false, true, null, null)),
            "condition/v1", List.of(
                new ToolUtils.PropertyInfo("rawExpression", "BOOLEAN", null, false, true, null, null),
                new ToolUtils.PropertyInfo("conditions", "ARRAY", null, false, true, "rawExpression == false", List.of(
                    new ToolUtils.PropertyInfo(null, "ARRAY", null, false, false, null, List.of(
                        new ToolUtils.PropertyInfo("boolean", "OBJECT", null, false, false, null, List.of(
                            new ToolUtils.PropertyInfo("type", "STRING", null, false, true, null, null),
                            new ToolUtils.PropertyInfo("value1", "BOOLEAN", null, true, true, null, null),
                            new ToolUtils.PropertyInfo("operation", "STRING", null, true, true, null, null),
                            new ToolUtils.PropertyInfo("value2", "BOOLEAN", null, true, true, null, null))),
                        new ToolUtils.PropertyInfo("dateTime", "OBJECT", null, false, false, null, List.of(
                            new ToolUtils.PropertyInfo("type", "STRING", null, false, true, null, null),
                            new ToolUtils.PropertyInfo("value1", "DATE_TIME", null, true, true, null, null),
                            new ToolUtils.PropertyInfo("operation", "STRING", null, true, true, null, null),
                            new ToolUtils.PropertyInfo("value2", "DATE_TIME", null, true, true, null, null))),
                        new ToolUtils.PropertyInfo("number", "OBJECT", null, false, false, null, List.of(
                            new ToolUtils.PropertyInfo("type", "STRING", null, false, true, null, null),
                            new ToolUtils.PropertyInfo("value1", "NUMBER", null, true, true, null, null),
                            new ToolUtils.PropertyInfo("operation", "STRING", null, true, true, null, null),
                            new ToolUtils.PropertyInfo("value2", "NUMBER", null, true, true,
                                "conditions[index][index].operation != 'EMPTY'", null))),
                        new ToolUtils.PropertyInfo("string", "OBJECT", null, false, false, null, List.of(
                            new ToolUtils.PropertyInfo("type", "STRING", null, false, true, null, null),
                            new ToolUtils.PropertyInfo("value1", "STRING", null, true, true, null, null),
                            new ToolUtils.PropertyInfo("operation", "STRING", null, true, true, null, null),
                            new ToolUtils.PropertyInfo("value2", "STRING", null, true, true,
                                "!contains({'EMPTY','REGEX'}, conditions[index][index].operation)", null))))))),
                new ToolUtils.PropertyInfo("expression", "STRING", null, false, true, "rawExpression == true", null),
                new ToolUtils.PropertyInfo("caseTrue", "ARRAY", null, false, true, null, List.of(
                    new ToolUtils.PropertyInfo(null, "TASK", null, false, false, null, null))),
                new ToolUtils.PropertyInfo("caseFalse", "ARRAY", null, false, true, null, List.of(
                    new ToolUtils.PropertyInfo(null, "TASK", null, false, false, null, null)))),
            "loop/v1", List.of(
                new ToolUtils.PropertyInfo("items", "ARRAY", null, false, true, null, List.of()),
                new ToolUtils.PropertyInfo("loopForever", "BOOLEAN", null, false, true, null, null),
                new ToolUtils.PropertyInfo("iteratee", "ARRAY", null, false, true, null, List.of(
                    new ToolUtils.PropertyInfo(null, "TASK", null, false, true, null, null)))),
            "component/v1/action1", List.of(
                new ToolUtils.PropertyInfo("name", "STRING", null, false, true, null, null)),
            "component/v1/action2", List.of(
                new ToolUtils.PropertyInfo("age", "NUMBER", null, false, true, null, null)));

        Map<String, ToolUtils.PropertyInfo> taskOutputs = Map.of(
            "component/v1/trigger1", actionArr,
            "component/v1/action1", action1);

        try {
            JsonNode tasksNode = WorkflowParser.parseJsonString(tasksJson);
            List<JsonNode> tasks = new ArrayList<>();
            for (JsonNode taskNode : tasksNode) {
                tasks.add(taskNode);
            }
            StringBuilder errors = new StringBuilder();
            StringBuilder warnings = new StringBuilder();

            WorkflowValidator.validateWorkflowTasks(tasks, taskDefinitions, taskOutputs, errors, warnings);

            assertEquals("Property 'loop1.item[0]' in output of 'loop/v1' is of type boolean, not number",
                errors.toString());
            assertEquals("Property 'task1.propString' might not exist in the output of 'component/v1/trigger1'",
                warnings.toString());
        } catch (Exception e) {
            fail("Should not throw exception: " + e.getMessage());
        }
    }

    @Test
    void validateCompleteWorkflow_validWorkflow_noErrors() {
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
                            "name": "test"
                        }
                    }
                ]
            }
            """;

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        // Mock task definition and output providers
        WorkflowValidator.TaskDefinitionProvider taskDefProvider = (taskType, kind) -> List.of(
            new ToolUtils.PropertyInfo("name", "STRING", null, false, true, null, null));

        WorkflowValidator.TaskOutputProvider taskOutputProvider =
            (taskType, kind, warningsBuilder) -> new ToolUtils.PropertyInfo("result", "STRING", null, false, false,
                null, null);

        WorkflowValidator.validateCompleteWorkflow(workflow, taskDefProvider, taskOutputProvider, new HashMap<>(),
            new HashMap<>(), errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateCompleteWorkflow_invalidStructure_hasErrors() {
        String workflow = """
            {
                "description": "Missing label",
                "triggers": [],
                "tasks": []
            }
            """;

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        WorkflowValidator.TaskDefinitionProvider taskDefProvider = (taskType, kind) -> List.of();
        WorkflowValidator.TaskOutputProvider taskOutputProvider = (taskType, kind, warningsBuilder) -> null;

        WorkflowValidator.validateCompleteWorkflow(workflow, taskDefProvider, taskOutputProvider, new HashMap<>(),
            new HashMap<>(), errors, warnings);

        assertEquals("Missing required field: label", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateCompleteWorkflow_duplicateTaskNames_hasErrors() {
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
                        "label": "Test Task 1",
                        "name": "duplicate_name",
                        "type": "component/v1/action1",
                        "parameters": {}
                    },
                    {
                        "label": "Test Task 2",
                        "name": "duplicate_name",
                        "type": "component/v1/action2",
                        "parameters": {}
                    }
                ]
            }
            """;

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        WorkflowValidator.TaskDefinitionProvider taskDefProvider = (taskType, kind) -> List.of();
        WorkflowValidator.TaskOutputProvider taskOutputProvider = (taskType, kind, warningsBuilder) -> null;

        WorkflowValidator.validateCompleteWorkflow(workflow, taskDefProvider, taskOutputProvider, new HashMap<>(),
            new HashMap<>(), errors, warnings);

        assertEquals("Tasks cannot have repeating names: duplicate_name", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateWorkflowTasks_addsNestedTasksToTaskDefinitionsCondition_noErrors() {
        String workflow = """
            {
                "label": "Test Workflow",
                "description": "Test workflow description",
                "triggers": [
                    {
                        "label": "Task 1",
                        "name": "task1",
                        "type": "component/v1/trigger1",
                        "parameters": {
                            "name": "John"
                        }
                    }
                ],
                "tasks": [
                    {
                         "label": "Condition",
                         "name": "condition_1",
                         "type": "condition/v1",
                         "parameters": {
                             "rawExpression": false,
                             "conditions": [
                                [
                                    {
                                        "type": "string",
                                        "value1": "${task1.propString}",
                                        "operation": "EQUALS",
                                        "value2": "Mario"
                                    }
                                ]
                             ],
                             "caseTrue": [
                                {
                                    "label": "Task 3",
                                    "name": "task3",
                                    "type": "component/v1/action1",
                                    "parameters": {
                                        "name": "${task1.propString}"
                                    }
                                }
                             ],
                             "caseFalse": [
                                {
                                    "label": "Task 2",
                                    "name": "task2",
                                    "type": "component/v1/action1",
                                    "parameters": {
                                        "name": "${task1.propString}"
                                    }
                                }
                             ]
                         }
                    }
                ]
            }
            """;

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        WorkflowValidator.TaskDefinitionProvider taskDefProvider = (taskType, kind) -> List.of();
        WorkflowValidator.TaskOutputProvider taskOutputProvider = (taskType, kind, warningsBuilder) -> null;
        Map<String, List<ToolUtils.PropertyInfo>> taskDefinitions = new HashMap<>();
        Map<String, ToolUtils.PropertyInfo> taskOutputs = new HashMap<>();

        WorkflowValidator.validateCompleteWorkflow(workflow, taskDefProvider, taskOutputProvider, taskDefinitions,
            taskOutputs, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("", warnings.toString());
        assertEquals("{component/v1/action1=[], component/v1/trigger1=[], condition/v1=[]}",
            taskDefinitions.toString());
        assertEquals("{component/v1/action1=null, component/v1/trigger1=null, condition/v1=null}",
            taskOutputs.toString());
    }

    @Test
    void validateWorkflowTasks_addsNestedTasksToTaskDefinitionsLoop_noErrors() {
        String workflow = """
            {
                "label": "Test Workflow",
                "description": "Test workflow description",
                "triggers": [
                    {
                        "label": "Task 1",
                        "name": "task1",
                        "type": "component/v1/trigger1",
                        "parameters": {
                            "name": "John"
                        }
                    }
                ],
                "tasks": [
                    {
                         "label": "Loop",
                         "name": "loop1",
                         "type": "loop/v1",
                         "parameters": {
                             "items": "${task1.elements}",
                             "loopForever": false,
                             "iteratee": [
                                {
                                    "label": "Task 3",
                                    "name": "task3",
                                    "type": "component/v1/action1",
                                    "parameters": {
                                        "name": "${loop1.item.propNumber}"
                                    }
                                },
                                {
                                    "label": "Task 2",
                                    "name": "task2",
                                    "type": "component/v1/action2",
                                    "parameters": {
                                        "age": "${loop1.item.propBool}"
                                    }
                                }
                             ]
                         }
                    }
                ]
            }
            """;

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        WorkflowValidator.TaskDefinitionProvider taskDefProvider = (taskType, kind) -> List.of();
        WorkflowValidator.TaskOutputProvider taskOutputProvider = (taskType, kind, warningsBuilder) -> null;
        Map<String, List<ToolUtils.PropertyInfo>> taskDefinitions = new HashMap<>();
        Map<String, ToolUtils.PropertyInfo> taskOutputs = new HashMap<>();

        WorkflowValidator.validateCompleteWorkflow(workflow, taskDefProvider, taskOutputProvider, taskDefinitions,
            taskOutputs, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("", warnings.toString());
        assertEquals("{component/v1/action2=[], component/v1/action1=[], component/v1/trigger1=[], loop/v1=[]}",
            taskDefinitions.toString());
        assertEquals("{component/v1/action2=null, component/v1/action1=null, component/v1/trigger1=null, loop/v1=null}",
            taskOutputs.toString());
    }

    @Test
    void validateWorkflowTasks_addsNestedTasksToTaskDefinitionsMultipleFlows_noErrors() {
        String workflow = """
            {
                "label": "Test Workflow",
                "description": "Test workflow description",
                "triggers": [
                    {
                        "label": "Task 1",
                        "name": "task1",
                        "type": "component/v1/trigger1",
                        "parameters": {
                            "name": "John"
                        }
                    }
                ],
                "tasks": [
                    {
                         "label": "Loop",
                         "name": "loop1",
                         "type": "loop/v1",
                         "parameters": {
                             "items": "${task1.elements}",
                             "loopForever": false,
                             "iteratee": [
                                {
                                     "label": "Condition",
                                     "name": "condition_1",
                                     "type": "condition/v1",
                                     "parameters": {
                                         "rawExpression": false,
                                         "conditions": [
                                            [
                                                {
                                                    "type": "string",
                                                    "value1": "${task1.propString}",
                                                    "operation": "EQUALS",
                                                    "value2": "Mario"
                                                }
                                            ]
                                         ],
                                         "caseTrue": [
                                            {
                                                "label": "Task 3",
                                                "name": "task3",
                                                "type": "component/v1/action1",
                                                "parameters": {
                                                    "name": "${task1.propString}"
                                                }
                                            }
                                         ],
                                         "caseFalse": [
                                            {
                                                "label": "Task 2",
                                                "name": "task2",
                                                "type": "component/v1/action1",
                                                "parameters": {
                                                    "name": "${task1.propString}"
                                                }
                                            }
                                         ]
                                     }
                                },
                                {
                                    "label": "Task 3",
                                    "name": "task3",
                                    "type": "component/v1/action1",
                                    "parameters": {
                                        "name": "${loop1.item}"
                                    }
                                },
                                {
                                    "label": "Task 2",
                                    "name": "task2",
                                    "type": "component/v1/action2",
                                    "parameters": {
                                        "age": "${loop1.item}"
                                    }
                                }
                             ]
                         }
                    }
                ]
            }
            """;

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        WorkflowValidator.TaskDefinitionProvider taskDefProvider = (taskType, kind) -> List.of();
        WorkflowValidator.TaskOutputProvider taskOutputProvider = (taskType, kind, warningsBuilder) -> null;
        Map<String, List<ToolUtils.PropertyInfo>> taskDefinitions = new HashMap<>();
        Map<String, ToolUtils.PropertyInfo> taskOutputs = new HashMap<>();

        WorkflowValidator.validateCompleteWorkflow(workflow, taskDefProvider, taskOutputProvider, taskDefinitions,
            taskOutputs, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("", warnings.toString());
        assertEquals(
            "{component/v1/action2=[], component/v1/action1=[], component/v1/trigger1=[], loop/v1=[], condition/v1=[]}",
            taskDefinitions.toString());
        assertEquals(
            "{component/v1/action2=null, component/v1/action1=null, component/v1/trigger1=null, loop/v1=null, condition/v1=null}",
            taskOutputs.toString());
    }

    @Test
    void validateSingleTask_validTask_noErrors() {
        String task = """
            {
                "label": "Test Task",
                "name": "test_task",
                "type": "component/v1/action1",
                "parameters": {
                    "name": "test",
                    "age": 25
                }
            }
            """;

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        WorkflowValidator.TaskDefinitionProvider taskDefProvider = (taskType, kind) -> List.of(
            new ToolUtils.PropertyInfo("name", "STRING", null, true, true, null, null),
            new ToolUtils.PropertyInfo("age", "INTEGER", null, false, true, null, null));

        WorkflowValidator.validateSingleTask(task, taskDefProvider, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateSingleTask_invalidStructure_hasErrors() {
        String task = """
            {
                "name": "test_task",
                "type": "component/v1/action1",
                "parameters": {}
            }
            """;

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        WorkflowValidator.TaskDefinitionProvider taskDefProvider = (taskType, kind) -> List.of();

        WorkflowValidator.validateSingleTask(task, taskDefProvider, errors, warnings);

        assertEquals("Missing required field: label", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateSingleTask_missingRequiredParameter_hasErrors() {
        String task = """
            {
                "label": "Test Task",
                "name": "test_task",
                "type": "component/v1/action1",
                "parameters": {
                    "age": 25
                }
            }
            """;

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        WorkflowValidator.TaskDefinitionProvider taskDefProvider = (taskType, kind) -> List.of(
            new ToolUtils.PropertyInfo("name", "STRING", null, true, true, null, null),
            new ToolUtils.PropertyInfo("age", "INTEGER", null, false, true, null, null));

        WorkflowValidator.validateSingleTask(task, taskDefProvider, errors, warnings);

        assertEquals("Missing required property: name", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void taskDefinitionProvider_functionalInterface_worksCorrectly() {
        WorkflowValidator.TaskDefinitionProvider provider = (taskType, kind) -> {
            if ("test/v1".equals(taskType)) {
                return List.of(new ToolUtils.PropertyInfo("test", "STRING", null, false, true, null, null));
            }
            return List.of();
        };

        List<ToolUtils.PropertyInfo> result = provider.getTaskProperties("test/v1", "action");
        assertEquals(1, result.size());
        assertEquals("test", result.get(0)
            .name());

        List<ToolUtils.PropertyInfo> emptyResult = provider.getTaskProperties("unknown/v1", "action");
        assertTrue(emptyResult.isEmpty());
    }

    @Test
    void taskOutputProvider_functionalInterface_worksCorrectly() {
        WorkflowValidator.TaskOutputProvider provider = (taskType, kind, warnings) -> {
            if ("test/v1".equals(taskType)) {
                return new ToolUtils.PropertyInfo("output", "STRING", null, false, false, null, null);
            }
            warnings.append("Unknown task type: ")
                .append(taskType);
            return null;
        };

        StringBuilder warnings = new StringBuilder();
        ToolUtils.PropertyInfo result = provider.getTaskOutputProperty("test/v1", "action", warnings);
        assertNotNull(result);
        assertEquals("output", result.name());
        assertEquals("", warnings.toString());

        ToolUtils.PropertyInfo nullResult = provider.getTaskOutputProperty("unknown/v1", "action", warnings);
        assertNull(nullResult);
        assertTrue(warnings.toString()
            .contains("Unknown task type: unknown/v1"));
    }
}
