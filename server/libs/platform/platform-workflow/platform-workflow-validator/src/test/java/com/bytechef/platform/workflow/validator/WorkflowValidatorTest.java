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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.platform.workflow.validator.model.PropertyInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * @author Marko Kriskovic
 */
class WorkflowValidatorTest {

    private static final PropertyInfo trigger1 =
        new PropertyInfo("propString", "STRING", null, false, true, null, null);
    private static final PropertyInfo action1 =
        new PropertyInfo("propBool", "BOOLEAN", null, false, true, null, null);
    private static final PropertyInfo action2 =
        new PropertyInfo("propNumber", "NUMBER", null, false, true, null, null);
    private static final PropertyInfo action3 =
        new PropertyInfo("propInteger", "INTEGER", null, false, true, null, null);
    private static final PropertyInfo action4 =
        new PropertyInfo("propDateTime", "DATE_TIME", null, false, true, null, null);
    private static final PropertyInfo actionObj = new PropertyInfo(
        "element", "OBJECT", null, false, true, null, List.of(action1, action2, action3));
    private static final PropertyInfo actionArr = new PropertyInfo(
        "elements", "ARRAY", null, false, true, null, List.of(
            new PropertyInfo(null, "OBJECT", null, false, true, null, List.of(action1, action2, action3))));

    @BeforeAll
    public static void beforeAll() {
        JsonUtils.setObjectMapper(new ObjectMapper());
    }

    @Test
    void validateTaskDataPillsArrayParametersValidatesCorrectly() {
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

        Map<String, List<PropertyInfo>> taskDefinitionMap = Map.of(
            "component/v1/trigger1", List.of(
                new PropertyInfo("name", "STRING", null, false, true, null, null)),
            "component/v1/action1", List.of(
                new PropertyInfo("items", "ARRAY", null, false, true, null, null)));

        Map<String, PropertyInfo> taskOutputMap = Map.of(
            "component/v1/trigger1", trigger1);

        try {
            JsonNode tasksJsonNode = JsonUtils.readTree(tasksJson);
            List<JsonNode> taskJsonNodes = new ArrayList<>();

            for (JsonNode taskJsonNode : tasksJsonNode) {
                taskJsonNodes.add(taskJsonNode);
            }
            StringBuilder errors = new StringBuilder();
            StringBuilder warnings = new StringBuilder();

            WorkflowValidator.validateWorkflowTasks(taskJsonNodes, taskDefinitionMap, taskOutputMap, errors, warnings);

            assertEquals("", errors.toString());
            assertEquals("", warnings.toString());
        } catch (Exception e) {
            fail("Should not throw exception: " + e.getMessage());
        }
    }

    @Test
    void validateTaskDataPillsMalformedDataPillFormatIgnoresGracefully() {
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

        Map<String, List<PropertyInfo>> taskDefinitionMap = Map.of(
            "component/v1/trigger1", List.of(
                new PropertyInfo("name", "STRING", null, false, true, null, null)),
            "component/v1/action1", List.of(
                new PropertyInfo("name", "STRING", null, false, true, null, null),
                new PropertyInfo("active", "BOOLEAN", null, false, true, null, null),
                new PropertyInfo("value", "STRING", null, false, true, null, null)));

        Map<String, PropertyInfo> taskOutputMap = Map.of(
            "component/v1/trigger1", trigger1);

        try {
            JsonNode tasksJsonNode = JsonUtils.readTree(tasksJson);
            List<JsonNode> taskJsonNodes = new ArrayList<>();

            for (JsonNode taskJsonNode : tasksJsonNode) {
                taskJsonNodes.add(taskJsonNode);
            }

            StringBuilder errors = new StringBuilder();
            StringBuilder warnings = new StringBuilder();

            WorkflowValidator.validateWorkflowTasks(taskJsonNodes, taskDefinitionMap, taskOutputMap, errors, warnings);

            assertEquals("Property 'active' has incorrect type. Expected: boolean, but got: string", errors.toString());
            assertEquals("", warnings.toString());
        } catch (Exception e) {
            fail("Should not throw exception: " + e.getMessage());
        }
    }

    @Test
    void validateTaskDataPillsMultipleDataPillsInSameValueValidatesAll() {
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

        Map<String, List<PropertyInfo>> taskDefinitionMap = Map.of(
            "component/v1/trigger1", List.of(
                new PropertyInfo("name", "STRING", null, false, true, null, null)),
            "component/v1/action1", List.of(
                new PropertyInfo("name", "STRING", null, false, true, null, null),
                new PropertyInfo("date", "DATE_TIME", null, false, true, null, null)));

        Map<String, PropertyInfo> taskOutputMap = Map.of(
            "component/v1/trigger1", trigger1,
            "component/v1/action1", action4);

        try {
            JsonNode tasksJsonNode = JsonUtils.readTree(tasksJson);
            List<JsonNode> taskJsonNodes = new ArrayList<>();

            for (JsonNode taskJsonNode : tasksJsonNode) {
                taskJsonNodes.add(taskJsonNode);
            }

            StringBuilder errors = new StringBuilder();
            StringBuilder warnings = new StringBuilder();

            WorkflowValidator.validateWorkflowTasks(taskJsonNodes, taskDefinitionMap, taskOutputMap, errors, warnings);

            // this is an exception, every value can be converted to string
            assertEquals("", errors.toString());
            assertEquals("", warnings.toString());
        } catch (Exception e) {
            fail("Should not throw exception: " + e.getMessage());
        }
    }

    @Test
    void validateTaskDataPillsNonExistentTaskIgnoresGracefully() {
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

        Map<String, List<PropertyInfo>> taskDefinitionMap = Map.of(
            "component/v1/trigger1", List.of(
                new PropertyInfo("name", "STRING", null, false, true, null, null)),
            "component/v1/action1", List.of(
                new PropertyInfo("name", "STRING", null, false, true, null, null),
                new PropertyInfo("active", "BOOLEAN", null, false, true, null, null)));

        Map<String, PropertyInfo> taskOutputMap = Map.of(
            "component/v1/trigger1", trigger1);

        try {
            JsonNode tasksJsonNode = JsonUtils.readTree(tasksJson);
            List<JsonNode> taskJsonNodes = new ArrayList<>();

            for (JsonNode taskNode : tasksJsonNode) {
                taskJsonNodes.add(taskNode);
            }

            StringBuilder errors = new StringBuilder();
            StringBuilder warnings = new StringBuilder();

            WorkflowValidator.validateWorkflowTasks(taskJsonNodes, taskDefinitionMap, taskOutputMap, errors, warnings);

            assertEquals("Task 'invalidformat' doesn't exits.", errors.toString());
            assertEquals("", warnings.toString());
        } catch (Exception e) {
            fail("Should not throw exception: " + e.getMessage());
        }
    }

    @Test
    void validateTaskDataPillsMissingTaskOutputInfoHandlesGracefully() {
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

        Map<String, List<PropertyInfo>> taskDefinitionMap = Map.of(
            "component/v1/trigger1", List.of(
                new PropertyInfo("name", "STRING", null, false, true, null, null)),
            "component/v1/action1", List.of(
                new PropertyInfo("name", "STRING", null, false, true, null, null)));

        Map<String, PropertyInfo> taskOutputMap = Map.of();

        try {
            JsonNode tasksJsonNode = JsonUtils.readTree(tasksJson);
            List<JsonNode> taskJsonNodes = new ArrayList<>();

            for (JsonNode taskJsonNode : tasksJsonNode) {
                taskJsonNodes.add(taskJsonNode);
            }

            StringBuilder errors = new StringBuilder();
            StringBuilder warnings = new StringBuilder();

            WorkflowValidator.validateWorkflowTasks(taskJsonNodes, taskDefinitionMap, taskOutputMap, errors, warnings);

            assertEquals("", errors.toString());
            assertEquals("Property 'task1.propString' might not exist in the output of 'component/v1/trigger1'",
                warnings.toString());
        } catch (Exception e) {
            fail("Should not throw exception: " + e.getMessage());
        }
    }

    @Test
    void validateTaskDataPillsObjectTypeValidationValidatesCorrectly() {
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

        Map<String, List<PropertyInfo>> taskDefinitionMap = Map.of(
            "component/v1/trigger1", List.of(
                new PropertyInfo("name", "STRING", null, false, true, null, null)),
            "component/v1/action1", List.of(
                new PropertyInfo("active", "BOOLEAN", null, false, true, null, null),
                new PropertyInfo("height", "NUMBER", null, false, true, null, null),
                new PropertyInfo("age", "INTEGER", null, false, true, null, null)),
            "component/v1/action2", List.of(
                new PropertyInfo("activeString", "STRING", null, false, true, null, null),
                new PropertyInfo("heightString", "STRING", null, false, true, null, null),
                new PropertyInfo("ageString", "STRING", null, false, true, null, null)));

        Map<String, PropertyInfo> taskOutputMap = Map.of(
            "component/v1/trigger1", actionObj);

        try {
            JsonNode tasksJsonNode = JsonUtils.readTree(tasksJson);
            List<JsonNode> taskJsonNodes = new ArrayList<>();

            for (JsonNode taskJsonNode : tasksJsonNode) {
                taskJsonNodes.add(taskJsonNode);
            }

            StringBuilder errors = new StringBuilder();
            StringBuilder warnings = new StringBuilder();

            WorkflowValidator.validateWorkflowTasks(taskJsonNodes, taskDefinitionMap, taskOutputMap, errors, warnings);

            assertEquals("", errors.toString());
            assertEquals("", warnings.toString());
        } catch (Exception e) {
            fail("Should not throw exception: " + e.getMessage());
        }
    }

    @Test
    void validateTaskDataPillsArrayTypeValidationValidatesCorrectly() {
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

        Map<String, List<PropertyInfo>> taskDefinitionMap = Map.of(
            "component/v1/trigger1", List.of(
                new PropertyInfo("name", "STRING", null, false, true, null, null)),
            "component/v1/action1", List.of(
                new PropertyInfo("active", "BOOLEAN", null, false, true, null, null),
                new PropertyInfo("height", "NUMBER", null, false, true, null, null),
                new PropertyInfo("age", "INTEGER", null, false, true, null, null)));

        Map<String, PropertyInfo> taskOutputMap = Map.of(
            "component/v1/trigger1", actionArr);

        try {
            JsonNode tasksJsonNode = JsonUtils.readTree(tasksJson);
            List<JsonNode> taskJsonNodes = new ArrayList<>();

            for (JsonNode taskJsonNode : tasksJsonNode) {
                taskJsonNodes.add(taskJsonNode);
            }

            StringBuilder errors = new StringBuilder();
            StringBuilder warnings = new StringBuilder();

            WorkflowValidator.validateWorkflowTasks(taskJsonNodes, taskDefinitionMap, taskOutputMap, errors, warnings);

            assertEquals("", errors.toString());
            assertEquals("", warnings.toString());
        } catch (Exception e) {
            fail("Should not throw exception: " + e.getMessage());
        }
    }

    @Test
    void validateTaskDataPillsComplexNestedArrayWithDataPillsValidatesCorrectly() {
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

        Map<String, List<PropertyInfo>> taskDefinitionMap = Map.of(
            "component/v1/trigger1", List.of(
                new PropertyInfo("name", "STRING", null, false, true, null, null)),
            "component/v1/action1", List.of(
                new PropertyInfo("configs", "ARRAY", null, false, true, null, List.of(
                    new PropertyInfo("ignored", "OBJECT", null, false, true, null, List.of(
                        new PropertyInfo("setting", "STRING", null, true, true, null, null),
                        new PropertyInfo("enabled", "BOOLEAN", null, false, true, null, null)))))));

        Map<String, PropertyInfo> taskOutputMap = Map.of(
            "component/v1/trigger1", trigger1);

        try {
            JsonNode tasksJsonNode = JsonUtils.readTree(tasksJson);
            List<JsonNode> taskJsonNodes = new ArrayList<>();

            for (JsonNode taskJsonNode : tasksJsonNode) {
                taskJsonNodes.add(taskJsonNode);
            }

            StringBuilder errors = new StringBuilder();
            StringBuilder warnings = new StringBuilder();

            WorkflowValidator.validateWorkflowTasks(taskJsonNodes, taskDefinitionMap, taskOutputMap, errors, warnings);

            assertEquals("", errors.toString());
            assertEquals(
                "Property 'task1.propInvalid' might not exist in the output of 'component/v1/trigger1'",
                warnings.toString());
        } catch (Exception e) {
            fail("Should not throw exception: " + e.getMessage());
        }
    }

    @Test
    void validateTaskStructureInvalidJsonAddsError() {
        String invalidTask = "{invalid json}";

        StringBuilder errors = new StringBuilder();

        TaskValidator.validateTaskStructure(invalidTask, errors);

        String string = errors.toString();

        assertTrue(string.contains("Invalid JSON format:"), errors.toString());
    }

    @Test
    void validateTaskStructureInvalidTypePatternAddsError() {
        String invalidTask = """
            {
                "label": "Test Task",
                "name": "testTask",
                "type": "component/v1/",
                "parameters": {}
            }
            """;

        StringBuilder errors = new StringBuilder();

        TaskValidator.validateTaskStructure(invalidTask, errors);

        String string = errors.toString();

        assertTrue(string.contains("Field 'type' must match pattern:"), string);
    }

    @Test
    void validateTaskStructureMissingLabelAddsError() {
        String invalidTask = """
            {
                "name": "testTask",
                "type": "component/v1/action",
                "parameters": {}
            }
            """;

        StringBuilder errors = new StringBuilder();

        TaskValidator.validateTaskStructure(invalidTask, errors);

        assertEquals("Missing required field: label", errors.toString());
    }

    @Test
    void validateTaskStructureMissingTypeAddsError() {
        String invalidTask = """
            {
                "label": "Test Task",
                "name": "testTask",
                "parameters": {}
            }
            """;

        StringBuilder errors = new StringBuilder();

        TaskValidator.validateTaskStructure(invalidTask, errors);

        assertEquals("Missing required field: type", errors.toString());
    }

    @Test
    void validateTaskStructureMissingNameAddsError() {
        String invalidTask = """
            {
                "label": "Test Task",
                "type": "component/v1/action",
                "parameters": {}
            }
            """;

        StringBuilder errors = new StringBuilder();

        TaskValidator.validateTaskStructure(invalidTask, errors);

        assertEquals("Missing required field: name", errors.toString());
    }

    @Test
    void validateTaskStructureNonStringLabelAddsError() {
        String invalidTask = """
            {
                "label": 123,
                "name": "testTask",
                "type": "component/v1/action",
                "parameters": {}
            }
            """;

        StringBuilder errors = new StringBuilder();

        TaskValidator.validateTaskStructure(invalidTask, errors);

        assertEquals("Field 'label' must be a string", errors.toString());
    }

    @Test
    void validateTaskStructureNonStringNameAddsError() {
        String invalidTask = """
            {
                "label": "Test Task",
                "name": 123,
                "type": "component/v1/action",
                "parameters": {}
            }
            """;

        StringBuilder errors = new StringBuilder();

        TaskValidator.validateTaskStructure(invalidTask, errors);

        assertEquals("Field 'name' must be a string", errors.toString());
    }

    @Test
    void validateTaskStructureNonStringTypeAddsError() {
        String invalidTask = """
            {
                "label": "Test Task",
                "name": "testTask",
                "type": 123,
                "parameters": {}
            }
            """;

        StringBuilder errors = new StringBuilder();

        TaskValidator.validateTaskStructure(invalidTask, errors);

        assertEquals("Field 'type' must be a string", errors.toString());
    }

    @Test
    void validateTaskStructureValidTaskNoErrors() {
        String validTask = """
            {
                "label": "Test Task",
                "name": "testTask",
                "type": "component/v1/action",
                "parameters": {}
            }
            """;

        StringBuilder errors = new StringBuilder();

        TaskValidator.validateTaskStructure(validTask, errors);

        assertEquals("", errors.toString());
    }

    @Test
    void validateTaskStructureDifferentOrderNoErrors() {
        String validTask = """
            {
                "parameters": {},
                "name": "testTask",
                "type": "component/v1/action",
                "label": "Test Task"
            }
            """;

        StringBuilder errors = new StringBuilder();

        TaskValidator.validateTaskStructure(validTask, errors);

        assertEquals("", errors.toString());
    }

    @Test
    void validateTaskStructureValidTypePatternsNoErrors() {
        String[] validTypes = {
            "component/v1",
            "component/v1/action",
            "comp123/v456/act789"
        };

        for (String type : validTypes) {
            String validTask = String.format(
                "{%n" +
                    "    \"label\": \"Test Task\",%n" +
                    "    \"name\": \"testTask\",%n" +
                    "    \"type\": \"%s\",%n" +
                    "    \"parameters\": {}%n" +
                    "}",
                type);

            StringBuilder errors = new StringBuilder();

            TaskValidator.validateTaskStructure(validTask, errors);

            assertEquals("", errors.toString(), "Type should be valid: " + type);
        }
    }

    @Test
    void validateTaskStructureMissingParametersAddsError() {
        String invalidTask = """
            {
                "label": "Test Task",
                "name": "testTask",
                "type": "component/v1/action"
            }
            """;

        StringBuilder errors = new StringBuilder();

        TaskValidator.validateTaskStructure(invalidTask, errors);

        assertEquals("Missing required field: parameters", errors.toString());
    }

    @Test
    void validateTaskStructureNonObjectParametersAddsError() {
        String invalidTask = """
            {
                "label": "Test Task",
                "name": "testTask",
                "type": "component/v1/action",
                "parameters": "not an object"
            }
            """;

        StringBuilder errors = new StringBuilder();

        TaskValidator.validateTaskStructure(invalidTask, errors);

        assertEquals("Field 'parameters' must be an object", errors.toString());
    }

    @Test
    void validateTaskStructureNonObjectAddsError() {
        String invalidTask = "\"not an object\"";

        StringBuilder errors = new StringBuilder();

        TaskValidator.validateTaskStructure(invalidTask, errors);

        assertEquals("Task must be an object", errors.toString());
    }

    @Test
    void validateTaskParametersValidParametersNoErrors() {
        String taskParameters = """
            {
                "name": "John",
                "age": 30,
                "active": true
            }
            """;

        List<PropertyInfo> taskDefinition = List.of(
            new PropertyInfo("name", "STRING", null, true, true, null, null),
            new PropertyInfo("age", "NUMBER", null, false, true, null, null),
            new PropertyInfo("active", "BOOLEAN", null, false, true, null, null));

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        TaskValidator.validateTaskParameters(taskParameters, taskDefinition, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateTaskParametersDifferentOrderNoErrors() {
        String taskParameters = """
            {
                "active": true,
                "age": 30,
                "name": "John"
            }
            """;

        List<PropertyInfo> taskDefinition = List.of(
            new PropertyInfo("name", "STRING", null, true, true, null, null),
            new PropertyInfo("age", "NUMBER", null, false, true, null, null),
            new PropertyInfo("active", "BOOLEAN", null, false, true, null, null));

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        TaskValidator.validateTaskParameters(taskParameters, taskDefinition, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateTaskParametersExtraPropertyAddsWarning() {
        String taskParameters = """
            {
                "name": "John",
                "extraField": "not allowed"
            }
            """;

        List<PropertyInfo> taskDefinition = List.of(
            new PropertyInfo("name", "STRING", null, true, true, null, null));

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        TaskValidator.validateTaskParameters(taskParameters, taskDefinition, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("Property 'extraField' is not defined in task definition", warnings.toString());
    }

    @Test
    void validateTaskParametersMissingRequiredPropertyAddsError() {
        String taskParameters = """
            {
                "age": 30
            }
            """;

        List<PropertyInfo> taskDefinition = List.of(
            new PropertyInfo("name", "STRING", null, true, true, null, null),
            new PropertyInfo("age", "NUMBER", null, false, true, null, null));

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        TaskValidator.validateTaskParameters(taskParameters, taskDefinition, errors, warnings);

        assertEquals("Missing required property: name", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateTaskParametersTypeMatching() {
        String taskParameters = """
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

        List<PropertyInfo> taskDefinition = List.of(
            new PropertyInfo("string", "STRING", null, true, true, null, null),
            new PropertyInfo("integer", "INTEGER", null, true, true, null, null),
            new PropertyInfo("boolean", "BOOLEAN", null, true, true, null, null),
            new PropertyInfo("number", "NUMBER", null, true, true, null, null),
            new PropertyInfo("array", "ARRAY", null, true, true, null, null),
            new PropertyInfo("object", "OBJECT", null, true, true, null, null),
            new PropertyInfo("null", "NULL", null, true, true, null, null),
            new PropertyInfo("date", "DATE", null, true, true, null, null),
            new PropertyInfo("time", "TIME", null, true, true, null, null),
            new PropertyInfo("date_time", "DATE_TIME", null, true, true, null, null));

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        TaskValidator.validateTaskParameters(taskParameters, taskDefinition, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateTaskParametersNullTypeMatching() {
        String taskParameters = """
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

        List<PropertyInfo> taskDefinition = List.of(
            new PropertyInfo("string", "STRING", null, true, true, null, null),
            new PropertyInfo("integer", "INTEGER", null, true, true, null, null),
            new PropertyInfo("boolean", "BOOLEAN", null, true, true, null, null),
            new PropertyInfo("number", "NUMBER", null, true, true, null, null),
            new PropertyInfo("array", "ARRAY", null, true, true, null, null),
            new PropertyInfo("object", "OBJECT", null, true, true, null, null),
            new PropertyInfo("null", "NULL", null, true, true, null, null),
            new PropertyInfo("date", "DATE", null, true, true, null, null),
            new PropertyInfo("time", "TIME", null, true, true, null, null),
            new PropertyInfo("date_time", "DATE_TIME", null, true, true, null, null));

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        TaskValidator.validateTaskParameters(taskParameters, taskDefinition, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateTaskParametersWrongTypeAddsError() {
        String taskParameters = """
            {
                "name": 123,
                "age": "thirty",
                "active": "yes"
            }
            """;

        List<PropertyInfo> taskDefinition = List.of(
            new PropertyInfo("name", "STRING", null, true, true, null, null),
            new PropertyInfo("age", "INTEGER", null, false, true, null, null),
            new PropertyInfo("active", "BOOLEAN", null, false, true, null, null));

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        TaskValidator.validateTaskParameters(taskParameters, taskDefinition, errors, warnings);

        assertEquals("""
            Property 'name' has incorrect type. Expected: string, but got: integer
            Property 'age' has incorrect type. Expected: integer, but got: string
            Property 'active' has incorrect type. Expected: boolean, but got: string""", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateTaskParametersWrongTypeFormatAddsError() {
        String taskParameters = """
            {
                "day": "2025-1-1",
                "night": "2025-02-30",
                "from": "45:45:73",
                "to": "2:5:5",
                "specific_date": "2025-09-23F11:46:12"
            }
            """;

        List<PropertyInfo> taskDefinition = List.of(
            new PropertyInfo("day", "DATE", null, true, true, null, null),
            new PropertyInfo("night", "DATE", null, true, true, null, null),
            new PropertyInfo("from", "TIME", null, true, true, null, null),
            new PropertyInfo("to", "TIME", null, true, true, null, null),
            new PropertyInfo("specific_date", "DATE_TIME", null, true, true, null, null));

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        TaskValidator.validateTaskParameters(taskParameters, taskDefinition, errors, warnings);

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
    void validateTaskParametersWrongTypeInArrayAndObjectAddsError() {
        String taskParameters = """
            {
                "obj": {
                    "name": 123,
                    "age": "thirty",
                    "active": "yes",
                    "items": ["John", 36, "Porky"]
                }
            }
            """;

        List<PropertyInfo> taskDefinition = List.of(
            new PropertyInfo(
                "obj", "OBJECT", null, false, true, null,
                List.of(
                    new PropertyInfo("name", "STRING", null, true, true, null, null),
                    new PropertyInfo("age", "INTEGER", null, false, true, null, null),
                    new PropertyInfo("active", "BOOLEAN", null, false, true, null, null),
                    new PropertyInfo(
                        "items", "ARRAY", null, false, true, null,
                        List.of(new PropertyInfo(null, "INTEGER", null, false, true, null, null))))));

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        TaskValidator.validateTaskParameters(taskParameters, taskDefinition, errors, warnings);

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
    void validateTaskParametersArrayAndObjectTypesNoErrors() {
        String taskParameters = """
            {
                "items": [],
                "config": {}
            }
            """;

        List<PropertyInfo> taskDefinition = List.of(
            new PropertyInfo(
                "items", "ARRAY", null, false, true, null, List.of(
                    new PropertyInfo(null, "OBJECT", null, false, true, null, List.of(
                        new PropertyInfo("name", "STRING", null, false, true, null, null),
                        new PropertyInfo("age", "INTEGER", null, false, true, null, null))))),
            new PropertyInfo("config", "OBJECT", null, false, true, null, null));

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        TaskValidator.validateTaskParameters(taskParameters, taskDefinition, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateTaskParametersArrayMultipleValuesNoErrors() {
        String taskParameters = """
            {
                "items": [
                    "John",
                    30,
                    45,
                    "Porky"
                ]
            }
            """;

        List<PropertyInfo> taskDefinition = List.of(
            new PropertyInfo(
                "items", "ARRAY", null, false, true, null,
                List.of(
                    new PropertyInfo("name", "STRING", null, false, true, null, null),
                    new PropertyInfo("age", "INTEGER", null, false, true, null, null))));

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        TaskValidator.validateTaskParameters(taskParameters, taskDefinition, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateTaskParametersArrayMultipleValuesObjectWrongAttribute() {
        String taskParameters = """
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

        List<PropertyInfo> taskDefinition = List.of(
            new PropertyInfo(
                "items", "ARRAY", null, false, true, null,
                List.of(
                    new PropertyInfo("ignoredName", "OBJECT", null, false, true, null,
                        List.of(
                            new PropertyInfo("name", "STRING", null, true, true, null, null),
                            new PropertyInfo("age", "INTEGER", null, true, true, null, null))))));

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        TaskValidator.validateTaskParameters(taskParameters, taskDefinition, errors, warnings);

        assertEquals("Missing required property: items[2].age", errors.toString());
        assertEquals("Property 'items[index].page' is not defined in task definition", warnings.toString());
    }

    @Test
    void validateTaskParametersArrayMultipleValuesIndexDisplayCondition() {
        String taskParameters = """
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

        List<PropertyInfo> taskDefinition = List.of(
            new PropertyInfo("items", "ARRAY", null, false, true, null, List.of(
                new PropertyInfo("name", "OBJECT", null, true, true, null, List.of(
                    new PropertyInfo("name", "STRING", null, true, true, null, null),
                    new PropertyInfo("age", "INTEGER", null, true, true, null, null),
                    new PropertyInfo("adult", "BOOLEAN", null, true, true, "items[index].age >= 18", null))))));

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        TaskValidator.validateTaskParameters(taskParameters, taskDefinition, errors, warnings);

        assertEquals("Missing required property: items[1].adult", errors.toString());
        assertEquals("Property 'items[2].adult' is not defined in task definition", warnings.toString());
    }

    @Test
    void validateTaskParametersTypesInArrayAndObjectNoErrors() {
        String taskParameters = """
            {
                "items": ["string"],
                "config": {
                    "key": "string (required)"
                }
            }
            """;

        List<PropertyInfo> taskDefinition = List.of(
            new PropertyInfo("items", "ARRAY", null, true, true, null, null),
            new PropertyInfo("config", "OBJECT", null, false, true, null, null));

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        TaskValidator.validateTaskParameters(taskParameters, taskDefinition, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("Property 'config.key' is not defined in task definition", warnings.toString());
    }

    @Test
    void validateTaskParametersNonObjectCurrentParametersAddsError() {
        String taskParameters = "\"not an object\"";

        List<PropertyInfo> taskDefinition = List.of();

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        TaskValidator.validateTaskParameters(taskParameters, taskDefinition, errors, warnings);

        assertEquals("Current task parameters must be an object", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateTaskParametersNonObjectTaskDefinitionAddsError() {
        String taskParameters = """
            {
                "name": "John"
            }
            """;

        PropertyInfo taskDefinitionPropertyInfo = new PropertyInfo(
            "testTask", "STRING", null, false, true, null, null);

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        WorkflowValidator.validateTaskParameters(taskParameters, taskDefinitionPropertyInfo, errors, warnings);

        assertEquals("Task definition must be an object", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateTaskParametersInvalidJsonCurrentParametersAddsError() {
        String taskParameters = "{invalid json}";

        List<PropertyInfo> taskDefinition = List.of();

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        TaskValidator.validateTaskParameters(taskParameters, taskDefinition, errors, warnings);

        assertTrue(errors.toString()
            .contains("Invalid JSON format:"), errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateTaskParametersNullValueCorrectType() {
        String taskParameters = """
            {
                "nullable": null
            }
            """;

        List<PropertyInfo> taskDefinition = List.of(
            new PropertyInfo("nullable", "STRING", null, false, true, null, null));

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        TaskValidator.validateTaskParameters(taskParameters, taskDefinition, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateTaskParametersMixedValidationScenario() {
        String taskParameters = """
            {
                "validString": "hello",
                "invalidNumber": "not an integer",
                "extraProperty": "can be here"
            }
            """;

        List<PropertyInfo> taskDefinition = List.of(
            new PropertyInfo("validString", "STRING", null, true, true, null, null),
            new PropertyInfo("invalidNumber", "INTEGER", null, true, true, null, null),
            new PropertyInfo("missingRequired", "BOOLEAN", null, true, true, null, null));

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        TaskValidator.validateTaskParameters(taskParameters, taskDefinition, errors, warnings);

        assertEquals("""
            Property 'invalidNumber' has incorrect type. Expected: integer, but got: string
            Missing required property: missingRequired""", errors.toString());
        assertEquals("Property 'extraProperty' is not defined in task definition", warnings.toString());
    }

    @Test
    void validateTaskParametersEmptyParametersDefinitionAllowsAnyParameters() {
        String taskParameters = """
            {
                "anyProperty": "any value"
            }
            """;

        List<PropertyInfo> taskDefinition = List.of();

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        TaskValidator.validateTaskParameters(taskParameters, taskDefinition, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("Property 'anyProperty' is not defined in task definition", warnings.toString());
    }

    @Test
    void validateTaskParametersDisplayConditionTrueIncludesConditionalProperty() {
        String taskParameters = """
            {
                "enableFeature": true,
                "featureConfig": {
                    "setting1": "value1",
                    "setting2": "value2"
                }
            }
            """;

        List<PropertyInfo> taskDefinition = List.of(
            new PropertyInfo("enableFeature", "BOOLEAN", null, true, true, null, null),
            new PropertyInfo(
                "featureConfig", "OBJECT", null, false, true, "enableFeature == true",
                List.of(
                    new PropertyInfo("setting1", "STRING", null, true, true, null, null),
                    new PropertyInfo("setting2", "STRING", null, false, true, null, null))));

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        TaskValidator.validateTaskParameters(taskParameters, taskDefinition, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateTaskParametersDisplayConditionTrueIncludesConditionalPropertyReverseOrder() {
        String taskParameters = """
            {
                "enableFeature": true,
                "featureConfig": {
                    "setting1": "value1",
                    "setting2": "value2"
                }
            }
            """;

        List<PropertyInfo> taskDefinition = List.of(
            new PropertyInfo("enableFeature", "BOOLEAN", null, true, true, null, null),
            new PropertyInfo(
                "featureConfig", "OBJECT", null, false, true, "true == enableFeature",
                List.of(
                    new PropertyInfo("setting1", "STRING", null, true, true, null, null),
                    new PropertyInfo("setting2", "STRING", null, false, true, null, null))));

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        TaskValidator.validateTaskParameters(taskParameters, taskDefinition, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateTaskParametersDisplayConditionTrueIncludesConditionalPropertyString() {
        String taskParameters = """
            {
                "enableFeature": "true",
                "featureConfig": {
                    "setting1": "value1",
                    "setting2": "value2"
                }
            }
            """;

        List<PropertyInfo> taskDefinition = List.of(
            new PropertyInfo("enableFeature", "STRING", null, true, true, null, null),
            new PropertyInfo(
                "featureConfig", "OBJECT", null, false, true, "enableFeature == 'true'",
                List.of(
                    new PropertyInfo("setting1", "STRING", null, true, true, null, null),
                    new PropertyInfo("setting2", "STRING", null, false, true, null, null))));

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        TaskValidator.validateTaskParameters(taskParameters, taskDefinition, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateTaskParametersDisplayConditionTrueIncludesConditionalPropertyContains() {
        String taskParameters = """
            {
                "enableFeature": "true",
                "featureConfig": {
                    "setting1": "value1",
                    "setting2": "value2"
                }
            }
            """;

        List<PropertyInfo> taskDefinition = List.of(
            new PropertyInfo("enableFeature", "STRING", null, true, true, null, null),
            new PropertyInfo(
                "featureConfig", "OBJECT", null, false, true,
                "contains({'true','True','1'}, enableFeature)",
                List.of(
                    new PropertyInfo("setting1", "STRING", null, true, true, null, null),
                    new PropertyInfo("setting2", "STRING", null, false, true, null, null))));

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        TaskValidator.validateTaskParameters(taskParameters, taskDefinition, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateTaskParametersDisplayConditionFalseIncludesConditionalPropertyContains() {
        String taskParameters = """
            {
                "enableFeature": "false",
                "featureConfig": {
                    "setting1": "value1",
                    "setting2": "value2"
                }
            }
            """;

        List<PropertyInfo> taskDefinition = List.of(
            new PropertyInfo("enableFeature", "STRING", null, true, true, null, null),
            new PropertyInfo(
                "featureConfig", "OBJECT", null, false, true, "contains({'true','True','1'}, enableFeature)",
                List.of(
                    new PropertyInfo("setting1", "STRING", null, true, true, null, null),
                    new PropertyInfo("setting2", "STRING", null, false, true, null, null))));

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        TaskValidator.validateTaskParameters(taskParameters, taskDefinition, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("""
            Property 'featureConfig' is not defined in task definition
            Property 'featureConfig.setting1' is not defined in task definition
            Property 'featureConfig.setting2' is not defined in task definition""", warnings.toString());
    }

    @Test
    void validateTaskParametersDisplayConditionTrueInvalidDisplayConditionOperation() {
        String taskParameters = """
            {
                "enableFeature": "true",
                "featureConfig": {
                    "setting1": "value1",
                    "setting2": "value2"
                }
            }
            """;

        List<PropertyInfo> taskDefinition = List.of(
            new PropertyInfo("enableFeature", "STRING", null, true, true, null, null),
            new PropertyInfo(
                "featureConfig", "OBJECT", null, false, true, "gndfknskgn / sflakdjdkf 3",
                List.of(
                    new PropertyInfo("setting1", "STRING", null, true, true, null, null),
                    new PropertyInfo("setting2", "STRING", null, false, true, null, null))));

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        TaskValidator.validateTaskParameters(taskParameters, taskDefinition, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("""
            Property 'featureConfig' is not defined in task definition
            Property 'featureConfig.setting1' is not defined in task definition
            Property 'featureConfig.setting2' is not defined in task definition
            Invalid logic for display condition: 'gndfknskgn / sflakdjdkf 3'""", warnings.toString());
    }

    @Test
    void validateTaskParametersDisplayConditionFalseFloatErrorWhenConditionalPropertyProvided() {
        String taskParameters = """
            {
                "enableFeature": 4,
                "featureConfig": {
                    "setting2": "value1"
                }
            }
            """;

        List<PropertyInfo> taskDefinition = List.of(
            new PropertyInfo("enableFeature", "INTEGER", null, true, true, null, null),
            new PropertyInfo(
                "featureConfig", "OBJECT", null, false, true, "enableFeature <= 5.87546",
                List.of(
                    new PropertyInfo("setting1", "STRING", null, true, true, null, null),
                    new PropertyInfo("setting2", "STRING", null, false, true, null, null))));

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        TaskValidator.validateTaskParameters(taskParameters, taskDefinition, errors, warnings);

        assertEquals("Missing required property: featureConfig.setting1", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateTaskParametersDisplayConditionTrueIntegerInvertedErrorWhenConditionalPropertyProvided() {
        String taskParameters = """
            {
                "enableFeature": 4.1,
                "featureConfig": {
                    "setting2": "value1"
                }
            }
            """;

        List<PropertyInfo> taskDefinition = List.of(
            new PropertyInfo("enableFeature", "FLOAT", null, true, true, null, null),
            new PropertyInfo(
                "featureConfig", "OBJECT", null, true, true, "50 > enableFeature",
                List.of(
                    new PropertyInfo("setting1", "STRING", null, true, true, null, null),
                    new PropertyInfo("setting2", "STRING", null, false, true, null, null))));

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        TaskValidator.validateTaskParameters(taskParameters, taskDefinition, errors, warnings);

        assertEquals("Missing required property: featureConfig.setting1", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateTaskParametersMultipleDisplayConditionsFiltersByConditionIncorrectCondition() {
        String taskParameters = """
            {
                "mode": "advanced",
                "basicConfig": {
                    "name": "basic"
                }
            }
            """;

        List<PropertyInfo> taskDefinition = List.of(
            new PropertyInfo("mode", "STRING", null, true, true, null, null),
            new PropertyInfo("basicConfig", "OBJECT", null, false, true, "mode == 'basic'",
                List.of(new PropertyInfo("name", "STRING", null, true, true, null, null))),
            new PropertyInfo(
                "advancedConfig", "OBJECT", null, true, true, "mode == 'advanced'",
                List.of(
                    new PropertyInfo("mandatory", "OBJECT", null, true, true, null, List.of(
                        new PropertyInfo("name", "STRING", null, true, true, null, null))),
                    new PropertyInfo("extra", "STRING", null, false, true, null, List.of(
                        new PropertyInfo("name", "STRING", null, true, true, null, null))))));

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        TaskValidator.validateTaskParameters(taskParameters, taskDefinition, errors, warnings);

        assertEquals("""
            Missing required property: advancedConfig
            Missing required property: advancedConfig.mandatory
            Missing required property: advancedConfig.mandatory.name""", errors.toString());
        assertEquals("""
            Property 'basicConfig' is not defined in task definition
            Property 'basicConfig.name' is not defined in task definition""", warnings.toString());
    }

    @Test
    void validateTaskParametersDuplicatePropertiesWithDifferentConditionsIncludesCorrectOne() {
        String taskParameters = """
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

        List<PropertyInfo> taskDefinition = List.of(
            new PropertyInfo("bodyContentType", "BOOLEAN", null, false, true, null, null),
            new PropertyInfo(
                "bodyContent", "OBJECT", null, false, true, "bodyContentType == true",
                List.of(
                    new PropertyInfo("extension", "STRING", null, true, true, null, null),
                    new PropertyInfo("mimeType", "STRING", null, true, true, null, null),
                    new PropertyInfo("name", "STRING", null, true, true, null, null),
                    new PropertyInfo("url", "STRING", null, true, true, null, null))),
            new PropertyInfo("bodyContent", "OBJECT", null, false, true, "bodyContentType == false",
                List.of()));

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        TaskValidator.validateTaskParameters(taskParameters, taskDefinition, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateTaskParametersDuplicatePropertiesWithDifferentConditionsExcludesIncorrectOne() {
        String taskParameters = """
            {
                "bodyContentType": false,
                "bodyContent": {
                    "extension": "txt"
                }
            }
            """;

        List<PropertyInfo> taskDefinition = List.of(
            new PropertyInfo("bodyContentType", "BOOLEAN", null, false, true, null, null),
            new PropertyInfo(
                "bodyContent", "OBJECT", null, false, true, "bodyContentType == true",
                List.of(
                    new PropertyInfo("extension", "STRING", null, true, true, null, null),
                    new PropertyInfo("mimeType", "STRING", null, true, true, null, null),
                    new PropertyInfo("name", "STRING", null, true, true, null, null),
                    new PropertyInfo("url", "STRING", null, true, true, null, null))),
            new PropertyInfo(
                "bodyContent", "OBJECT", null, false, true, "bodyContentType == false",
                List.of(
                    new PropertyInfo("simpleProperty", "STRING", null, false, true, null, null))));

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        TaskValidator.validateTaskParameters(taskParameters, taskDefinition, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("Property 'bodyContent.extension' is not defined in task definition", warnings.toString());
    }

    @Test
    void validateTaskParametersDuplicatePropertiesWithDifferentConditionsIncludesIncorrect() {
        String taskParameters = """
            {
                "bodyContentType": true,
                "bodyContent": {
                    "extension": "txt"
                }
            }
            """;

        List<PropertyInfo> taskDefinition = List.of(
            new PropertyInfo("bodyContentType", "BOOLEAN", null, false, true, null, null),
            new PropertyInfo(
                "bodyContent", "OBJECT", null, false, true, "bodyContentType == true",
                List.of(
                    new PropertyInfo("extension", "STRING", null, true, true, null, null),
                    new PropertyInfo("mimeType", "STRING", null, true, true, null, null),
                    new PropertyInfo("name", "STRING", null, true, true, null, null),
                    new PropertyInfo("url", "STRING", null, true, true, null, null))),
            new PropertyInfo(
                "bodyContent", "OBJECT", null, false, true, "bodyContentType == false",
                List.of(new PropertyInfo("simpleProperty", "STRING", null, false, true, null, null))));

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        TaskValidator.validateTaskParameters(taskParameters, taskDefinition, errors, warnings);

        assertEquals("""
            Missing required property: bodyContent.mimeType
            Missing required property: bodyContent.name
            Missing required property: bodyContent.url""", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateTaskParametersNestedDisplayConditionsValidatesCorrectly() {
        String taskParameters = """
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

        List<PropertyInfo> taskDefinition = List.of(
            new PropertyInfo("parentEnabled", "BOOLEAN", null, true, true, null, null),
            new PropertyInfo(
                "parent", "OBJECT", null, false, true, "parentEnabled == true",
                List.of(
                    new PropertyInfo("childEnabled", "BOOLEAN", null, true, true, null, null),
                    new PropertyInfo(
                        "child", "OBJECT", null, false, true, "parent.childEnabled == true",
                        List.of(
                            new PropertyInfo("value", "STRING", null, true, true, null, null))))));

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        TaskValidator.validateTaskParameters(taskParameters, taskDefinition, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateTaskParametersStringValueConditionWorksCorrectly() {
        // here
        String taskParameters = """
            {
                "format": "json",
                "jsonConfig": {
                    "indent": 2
                }
            }
            """;

        List<PropertyInfo> taskDefinition = List.of(
            new PropertyInfo("format", "STRING", null, true, true, null, null),
            new PropertyInfo("jsonConfig", "OBJECT", null, false, true, "format == 'json'",
                List.of(new PropertyInfo("indent", "INTEGER", null, false, true, null, null))),
            new PropertyInfo("xmlConfig", "OBJECT", null, false, true, "format == 'xml'",
                List.of(new PropertyInfo("schema", "STRING", null, false, true, null, null))));

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        TaskValidator.validateTaskParameters(taskParameters, taskDefinition, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateTaskParametersMissingFieldForConditionExcludesConditionalProperty() {
        String taskParameters = """
            {
                "name": "test"
            }
            """;

        List<PropertyInfo> taskDefinition = List.of(
            new PropertyInfo("name", "STRING", null, true, true, null, null),
            new PropertyInfo(
                "advancedConfig", "OBJECT", null, false, true, "enableAdvanced == true",
                List.of(new PropertyInfo("setting", "STRING", null, true, true, null, null))));

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        TaskValidator.validateTaskParameters(taskParameters, taskDefinition, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("Invalid logic for display condition: 'enableAdvanced == true'", warnings.toString());
    }

    @Test
    void validateTaskParametersComplexNestedConditionsValidatesAllLevels() {
        String taskParameters = """
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

        List<PropertyInfo> taskDefinition = List.of(
            new PropertyInfo("level1", "STRING", null, true, true, null, null),
            new PropertyInfo(
                "config1", "OBJECT", null, false, true, "level1 == 'enabled'",
                List.of(
                    new PropertyInfo("level2", "STRING", null, true, true, null, null),
                    new PropertyInfo(
                        "config2", "OBJECT", null, false, true, "config1.level2 == 'active'",
                        List.of(
                            new PropertyInfo("level3", "BOOLEAN", null, true, true, null, null),
                            new PropertyInfo(
                                "config3", "OBJECT", null, false, true, "config1.config2.level3 == true",
                                List.of(new PropertyInfo("finalValue", "STRING", null, true, true, null, null))))))));

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        TaskValidator.validateTaskParameters(taskParameters, taskDefinition, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateTaskParametersComplexNestedConditionsDeepMissingError() {
        String taskParameters = """
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

        List<PropertyInfo> taskDefinition = List.of(
            new PropertyInfo("level1", "STRING", null, true, true, null, null),
            new PropertyInfo(
                "config1", "OBJECT", null, false, true, null,
                List.of(
                    new PropertyInfo("level2", "STRING", null, true, true, null, null),
                    new PropertyInfo(
                        "config2", "OBJECT", null, false, true, "config1.level2 == 'active'",
                        List.of(
                            new PropertyInfo("level3", "BOOLEAN", null, true, true, null, null),
                            new PropertyInfo(
                                "config3", "OBJECT", null, false, true, "config1.config2.level3 == true",
                                List.of(
                                    new PropertyInfo("finalValue", "STRING", null, true, true, null, null))))))));

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        TaskValidator.validateTaskParameters(taskParameters, taskDefinition, errors, warnings);

        assertEquals("Missing required property: config1.config2.config3.finalValue", errors.toString());
        assertEquals("Property 'config1.config2.config3.randomValue' is not defined in task definition",
            warnings.toString());
    }

    @Test
    void validateTaskParametersComplexNestedConditionsDeepWarning() {
        String taskParameters = """
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

        List<PropertyInfo> taskDefinition = List.of(
            new PropertyInfo("level1", "STRING", null, true, true, null, null),
            new PropertyInfo(
                "config1", "OBJECT", "", false, true, null,
                List.of(
                    new PropertyInfo("level2", "STRING", null, true, true, null, null),
                    new PropertyInfo(
                        "config2", "OBJECT", null, false, true, "config1.level2 == 'active'",
                        List.of(
                            new PropertyInfo("level3", "BOOLEAN", null, true, true, null, null),
                            new PropertyInfo(
                                "config3", "OBJECT", null, false, true, "config1.config2.level3 == true",
                                List.of(new PropertyInfo("finalValue", "STRING", null, true, true, null, null))))))));

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        TaskValidator.validateTaskParameters(taskParameters, taskDefinition, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("""
            Property 'config1.config2.config3' is not defined in task definition
            Property 'config1.config2.config3.finalValue' is not defined in task definition""", warnings.toString());
    }

    @Test
    void validateTaskParametersInlineConditionExcludesRequiredProperty() {
        String taskParameters = """
            {
                "name": "test",
                "enableAdvanced": false
            }
            """;

        List<PropertyInfo> taskDefinition = List.of(
            new PropertyInfo("name", "STRING", null, true, true, null, null),
            new PropertyInfo("enableAdvanced", "BOOLEAN", null, true, true, null, null),
            new PropertyInfo("advancedConfig", "STRING", null, true, true, "enableAdvanced == true", null));

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        TaskValidator.validateTaskParameters(taskParameters, taskDefinition, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateTaskParametersInlineConditionReversedExcludesRequiredProperty() {
        String taskParameters = """
            {
                "name": "test",
                "enableAdvanced": true,
                "advancedConfig": "value"
            }
            """;

        List<PropertyInfo> taskDefinition = List.of(
            new PropertyInfo("name", "STRING", null, true, true, null, null),
            new PropertyInfo("enableAdvanced", "BOOLEAN", null, true, true, null, null),
            new PropertyInfo("advancedConfig", "STRING", null, true, true, "true == enableAdvanced", null));

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        TaskValidator.validateTaskParameters(taskParameters, taskDefinition, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateTaskParametersInlineConditionDoesntIncludeRequiredPropertyMissingError() {
        String taskParameters = """
            {
                "name": "test",
                "enableAdvanced": true
            }
            """;

        List<PropertyInfo> taskDefinition = List.of(
            new PropertyInfo("name", "STRING", null, true, true, null, null),
            new PropertyInfo("enableAdvanced", "BOOLEAN", null, true, true, null, null),
            new PropertyInfo("advancedConfig", "STRING", null, true, true, "enableAdvanced == true", null));

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        TaskValidator.validateTaskParameters(taskParameters, taskDefinition, errors, warnings);

        assertEquals("Missing required property: advancedConfig", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateTaskParametersInlineConditionDoesntIncludeRequiredPropertyIntegerMissingError() {
        String taskParameters = """
            {
                "name": "test",
                "enableAdvanced": 4.0
            }
            """;

        List<PropertyInfo> taskDefinition = List.of(
            new PropertyInfo("name", "STRING", null, true, true, null, null),
            new PropertyInfo("enableAdvanced", "NUMBER", null, true, true, null, null),
            new PropertyInfo("advancedConfig", "STRING", null, true, true, "enableAdvanced >= 4", null));

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        TaskValidator.validateTaskParameters(taskParameters, taskDefinition, errors, warnings);

        assertEquals("Missing required property: advancedConfig", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateTaskParametersInlineConditionDoesntIncludeRequiredPropertyReversedFloatNoErrors() {
        String taskParameters = """
            {
                "name": "test",
                "enableAdvanced": 4
            }
            """;

        List<PropertyInfo> taskDefinition = List.of(
            new PropertyInfo("name", "STRING", null, true, true, null, null),
            new PropertyInfo("enableAdvanced", "INTEGER", null, true, true, null, null),
            new PropertyInfo("advancedConfig", "STRING", null, true, true, "4.1 < enableAdvanced", null));

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        TaskValidator.validateTaskParameters(taskParameters, taskDefinition, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateTaskParametersInlineConditionDoesntIncludeRequiredPropertyContainsNoErrors() {
        String taskParameters = """
            {
                "name": "test",
                "enableAdvanced": "true",
                "advancedConfig": "Barabara Strisen"
            }
            """;

        List<PropertyInfo> taskDefinition = List.of(
            new PropertyInfo("name", "STRING", null, true, true, null, null),
            new PropertyInfo("enableAdvanced", "STRING", null, true, true, null, null),
            new PropertyInfo("advancedConfig", "STRING", null, true, true,
                "contains({'Donald_B2rbara','True','true'}, enableAdvanced)", null));

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        TaskValidator.validateTaskParameters(taskParameters, taskDefinition, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateTaskParametersComplexNestedInlineConditionsValidatesAllLevels() {
        String taskParameters = """
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

        List<PropertyInfo> taskDefinition = List.of(
            new PropertyInfo("level1", "STRING", null, true, true, null, null),
            new PropertyInfo(
                "config1", "OBJECT", null, false, true, null,
                List.of(
                    new PropertyInfo("level2", "STRING", null, true, true, "level1 == 'enabled'", null),
                    new PropertyInfo(
                        "config2", "OBJECT", null, false, true, null,
                        List.of(
                            new PropertyInfo("level3", "BOOLEAN", null, true, true, "'active' == config1.level2", null),
                            new PropertyInfo("config3", "OBJECT", null, false, true, null, List.of(
                                new PropertyInfo(
                                    "finalValue", "STRING", null, true, true, "config1.config2.level3 == true",
                                    null))))))));

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        TaskValidator.validateTaskParameters(taskParameters, taskDefinition, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateTaskParametersNestedInlineConditionMissingError() {
        String taskParameters = """
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

        List<PropertyInfo> taskDefinition = List.of(
            new PropertyInfo("level1", "STRING", null, true, true, null, null),
            new PropertyInfo(
                "config1", "OBJECT", null, false, true, null,
                List.of(
                    new PropertyInfo("level2", "STRING", null, true, true, "level1 == 'enabled'", null),
                    new PropertyInfo(
                        "config2", "OBJECT", null, false, true, null,
                        List.of(
                            new PropertyInfo("level3", "BOOLEAN", null, true, true, "'enabled' == level1", null),
                            new PropertyInfo("config3", "OBJECT", null, false, true, null,
                                List.of(
                                    new PropertyInfo(
                                        "finalValue", "STRING", null, true, true, "level1 == 'enabled'", null))))))));

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        TaskValidator.validateTaskParameters(taskParameters, taskDefinition, errors, warnings);

        assertEquals("""
            Missing required property: config1.level2
            Missing required property: config1.config2.level3
            Missing required property: config1.config2.config3.finalValue""", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateTaskParametersNestedInlineConditionNotMatchingCondition() {
        String taskParameters = """
            {
                "level1": "not_enabled",
                "config1": {
                    "config2": {
                        "config3": { }
                    }
                }
            }
            """;

        List<PropertyInfo> taskDefinition = List.of(
            new PropertyInfo("level1", "STRING", null, true, true, null, null),
            new PropertyInfo(
                "config1", "OBJECT", null, false, true, null,
                List.of(
                    new PropertyInfo("level2", "STRING", null, true, true, "level1 == 'enabled'", null),
                    new PropertyInfo(
                        "config2", "OBJECT", null, false, true, null,
                        List.of(
                            new PropertyInfo("level3", "BOOLEAN", null, true, true, "'enabled' == level1", null),
                            new PropertyInfo(
                                "config3", "OBJECT", null, false, true, null,
                                List.of(
                                    new PropertyInfo(
                                        "finalValue", "STRING", null, true, true, "level1 == 'enabled'", null))))))));

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        TaskValidator.validateTaskParameters(taskParameters, taskDefinition, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateTaskParametersHttpClientPost() {
        String taskParameters = """
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

        List<PropertyInfo> taskDefinition = List.of(
            new PropertyInfo("uri", "STRING", null, true, true, null, null),
            new PropertyInfo("allowUnauthorizedCerts", "BOOLEAN", null, false, true, null, null),
            new PropertyInfo("responseType", "STRING", null, false, true, null, null),
            new PropertyInfo("responseFilename", "STRING", null, false, true, "responseType == 'BINARY'",
                null),
            new PropertyInfo("headers", "OBJECT", "", false, true, null, null),
            new PropertyInfo("queryParameters", "OBJECT", "", false, true, null, null),
            new PropertyInfo(
                "body", "OBJECT", "", false, true, null,
                List.of(
                    new PropertyInfo("bodyContentType", "STRING", null, false, true, null, null),
                    new PropertyInfo(
                        "bodyContent", "OBJECT", null, false, true, "body.bodyContentType == 'JSON'", null),
                    new PropertyInfo("bodyContent", "OBJECT", null, false, true, "body.bodyContentType == 'XML'", null),
                    new PropertyInfo(
                        "bodyContent", "OBJECT", null, false, true, "body.bodyContentType == 'FORM_DATA'", null),
                    new PropertyInfo(
                        "bodyContent", "OBJECT", null, false, true, "body.bodyContentType == 'FORM_URL_ENCODED'", null),
                    new PropertyInfo("bodyContent", "STRING", null, false, true, "body.bodyContentType == 'RAW'", null),
                    new PropertyInfo(
                        "bodyContent", "OBJECT", null, false, true, "body.bodyContentType == 'BINARY'",
                        List.of(
                            new PropertyInfo("extension", "STRING", null, true, true, null, null),
                            new PropertyInfo("mimeType", "STRING", null, true, true, null, null),
                            new PropertyInfo("name", "STRING", null, true, true, null, null),
                            new PropertyInfo("url", "STRING", null, true, true, null, null))),
                    new PropertyInfo("bodyContentMimeType", "STRING", null, false, true,
                        "'BINARY' == body.bodyContentType", null),
                    new PropertyInfo("bodyContentMimeType", "STRING", null, false, true,
                        "'RAW' == body.bodyContentType", null))),
            new PropertyInfo("fullResponse", "BOOLEAN", null, false, true, null, null),
            new PropertyInfo("followAllRedirects", "BOOLEAN", null, false, true, null, null),
            new PropertyInfo("followRedirect", "BOOLEAN", null, false, true, null, null),
            new PropertyInfo("ignoreResponseCode", "BOOLEAN", null, false, true, null, null),
            new PropertyInfo("proxy", "STRING", null, false, true, null, null),
            new PropertyInfo("timeout", "INTEGER", null, false, true, null, null));

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        TaskValidator.validateTaskParameters(taskParameters, taskDefinition, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("""
            Property 'headers.Authorization' is not defined in task definition
            Property 'headers.Content-Type' is not defined in task definition
            Property 'queryParameters.debug' is not defined in task definition""", warnings.toString());
    }

    @Test
    void validateTaskParametersConditionFlowRawExpressionTrue() {
        String taskParameters = """
            {
                "rawExpression": true,
                "expression": "TODO",
                "caseTrue": [],
                "caseFalse": []
            }
            """;

        List<PropertyInfo> taskDefinition = List.of(
            new PropertyInfo("rawExpression", "BOOLEAN", null, false, true, null, null),
            new PropertyInfo(
                "conditions", "ARRAY", null, false, true, "rawExpression == false",
                List.of(
                    new PropertyInfo(
                        null, "ARRAY", null, false, false, null,
                        List.of(
                            new PropertyInfo(
                                "boolean", "OBJECT", null, false, false, null,
                                List.of(
                                    new PropertyInfo("type", "STRING", null, false, true, null, null),
                                    new PropertyInfo("value1", "BOOLEAN", null, true, true, null, null),
                                    new PropertyInfo("operation", "STRING", null, true, true, null, null),
                                    new PropertyInfo("value2", "BOOLEAN", null, true, true, null, null))),
                            new PropertyInfo(
                                "dateTime", "OBJECT", null, false, false, null,
                                List.of(
                                    new PropertyInfo("type", "STRING", null, false, true, null, null),
                                    new PropertyInfo("value1", "DATE_TIME", null, true, true, null, null),
                                    new PropertyInfo("operation", "STRING", null, true, true, null, null),
                                    new PropertyInfo("value2", "DATE_TIME", null, true, true, null, null))),
                            new PropertyInfo(
                                "number", "OBJECT", null, false, false, null,
                                List.of(
                                    new PropertyInfo("type", "STRING", null, false, true, null, null),
                                    new PropertyInfo("value1", "NUMBER", null, true, true, null, null),
                                    new PropertyInfo("operation", "STRING", null, true, true, null, null),
                                    new PropertyInfo("value2", "NUMBER", null, true, true,
                                        "conditions[index][index].operation != 'EMPTY'", null))),
                            new PropertyInfo(
                                "string", "OBJECT", null, false, false, null,
                                List.of(
                                    new PropertyInfo("type", "STRING", null, false, true, null, null),
                                    new PropertyInfo("value1", "STRING", null, true, true, null, null),
                                    new PropertyInfo("operation", "STRING", null, true, true, null, null),
                                    new PropertyInfo("value2", "STRING", null, true, true,
                                        "!contains({'EMPTY','REGEX'}, conditions[index][index].operation)", null))))))),
            new PropertyInfo("expression", "STRING", null, false, true, "rawExpression == true", null),
            new PropertyInfo(
                "caseTrue", "ARRAY", null, false, true, null,
                List.of(new PropertyInfo(null, "TASK", null, false, true, null, null))),
            new PropertyInfo(
                "caseFalse", "ARRAY", null, false, true, null,
                List.of(new PropertyInfo(null, "TASK", null, false, true, null, null))));

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        TaskValidator.validateTaskParameters(taskParameters, taskDefinition, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateTaskParametersConditionFlowRawExpressionTrueWithTask() {
        String taskParameters = """
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

        List<PropertyInfo> taskDefinition = List.of(
            new PropertyInfo("rawExpression", "BOOLEAN", null, false, true, null, null),
            new PropertyInfo(
                "conditions", "ARRAY", null, false, true, "rawExpression == false",
                List.of(
                    new PropertyInfo(
                        null, "ARRAY", null, false, false, null,
                        List.of(
                            new PropertyInfo(
                                "boolean", "OBJECT", null, false, false, null,
                                List.of(
                                    new PropertyInfo("type", "STRING", null, false, true, null, null),
                                    new PropertyInfo("value1", "BOOLEAN", null, true, true, null, null),
                                    new PropertyInfo("operation", "STRING", null, true, true, null, null),
                                    new PropertyInfo("value2", "BOOLEAN", null, true, true, null, null))),
                            new PropertyInfo(
                                "dateTime", "OBJECT", null, false, false, null,
                                List.of(
                                    new PropertyInfo("type", "STRING", null, false, true, null, null),
                                    new PropertyInfo("value1", "DATE_TIME", null, true, true, null, null),
                                    new PropertyInfo("operation", "STRING", null, true, true, null, null),
                                    new PropertyInfo("value2", "DATE_TIME", null, true, true, null, null))),
                            new PropertyInfo(
                                "number", "OBJECT", null, false, false, null,
                                List.of(
                                    new PropertyInfo("type", "STRING", null, false, true, null, null),
                                    new PropertyInfo("value1", "NUMBER", null, true, true, null, null),
                                    new PropertyInfo("operation", "STRING", null, true, true, null, null),
                                    new PropertyInfo(
                                        "value2", "NUMBER", null, true, true,
                                        "conditions[index][index].operation != 'EMPTY'", null))),
                            new PropertyInfo(
                                "string", "OBJECT", null, false, false, null,
                                List.of(
                                    new PropertyInfo("type", "STRING", null, false, true, null, null),
                                    new PropertyInfo("value1", "STRING", null, true, true, null, null),
                                    new PropertyInfo("operation", "STRING", null, true, true, null, null),
                                    new PropertyInfo("value2", "STRING", null, true, true,
                                        "!contains({'EMPTY','REGEX'}, conditions[index][index].operation)", null))))))),
            new PropertyInfo("expression", "STRING", null, false, true, "rawExpression == true", null),
            new PropertyInfo(
                "caseTrue", "ARRAY", null, false, true, null,
                List.of(new PropertyInfo(null, "TASK", null, false, true, null, null))),
            new PropertyInfo(
                "caseFalse", "ARRAY", null, false, true, null,
                List.of(new PropertyInfo(null, "TASK", null, false, true, null, null))));

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        TaskValidator.validateTaskParameters(taskParameters, taskDefinition, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateTaskParametersConditionFlowRawExpressionFalse() {
        String taskParameters = """
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

        List<PropertyInfo> taskDefinition = List.of(
            new PropertyInfo("rawExpression", "BOOLEAN", null, false, true, null, null),
            new PropertyInfo(
                "conditions", "ARRAY", null, false, true, "rawExpression == false",
                List.of(
                    new PropertyInfo(
                        null, "ARRAY", null, false, false, null,
                        List.of(
                            new PropertyInfo("boolean", "OBJECT", null, false, false, null,
                                List.of(
                                    new PropertyInfo("type", "STRING", null, false, true, null, null),
                                    new PropertyInfo("value1", "BOOLEAN", null, true, true, null, null),
                                    new PropertyInfo("operation", "STRING", null, true, true, null, null),
                                    new PropertyInfo("value2", "BOOLEAN", null, true, true, null, null))),
                            new PropertyInfo(
                                "dateTime", "OBJECT", null, false, false, null,
                                List.of(
                                    new PropertyInfo("type", "STRING", null, false, true, null, null),
                                    new PropertyInfo("value1", "DATE_TIME", null, true, true, null, null),
                                    new PropertyInfo("operation", "STRING", null, true, true, null, null),
                                    new PropertyInfo("value2", "DATE_TIME", null, true, true, null, null))),
                            new PropertyInfo(
                                "number", "OBJECT", null, false, false, null,
                                List.of(
                                    new PropertyInfo("type", "STRING", null, false, true, null, null),
                                    new PropertyInfo("value1", "NUMBER", null, true, true, null, null),
                                    new PropertyInfo("operation", "STRING", null, true, true, null, null),
                                    new PropertyInfo(
                                        "value2", "NUMBER", null, true, true,
                                        "conditions[index][index].operation != 'EMPTY'",
                                        null))),
                            new PropertyInfo("string", "OBJECT", null, false, false, null,
                                List.of(
                                    new PropertyInfo("type", "STRING", null, false, true, null, null),
                                    new PropertyInfo("value1", "STRING", null, true, true, null, null),
                                    new PropertyInfo("operation", "STRING", null, true, true, null, null),
                                    new PropertyInfo(
                                        "value2", "STRING", null, true, true,
                                        "!contains({'EMPTY','REGEX'}, conditions[index][index].operation)", null))))))),
            new PropertyInfo("expression", "STRING", null, false, true, "rawExpression == true", null),
            new PropertyInfo(
                "caseTrue", "ARRAY", null, false, true, null,
                List.of(new PropertyInfo(null, "TASK", null, false, true, null, null))),
            new PropertyInfo(
                "caseFalse", "ARRAY", null, false, true, null,
                List.of(new PropertyInfo(null, "TASK", null, false, true, null, null))));

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        TaskValidator.validateTaskParameters(taskParameters, taskDefinition, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateWorkflowStructureValidWorkflowNoErrors() {
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
    void validateWorkflowStructureDifferentOrderNoErrors() {
        String validWorkflow = """
            {
                "tasks": [],
                "description": "workflowDescription",
                "label": "workflowName",
                "triggers": [
                    {
                        "type": "manual/v1/manual",
                        "name": "trigger_1",
                        "label": "Manual"
                    }
                ],
                "inputs": []
            }
            """;

        StringBuilder errors = new StringBuilder();

        WorkflowValidator.validateWorkflowStructure(validWorkflow, errors);

        assertEquals("", errors.toString());
    }

    @Test
    void validateWorkflowStructureMissingLabelAddsError() {
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
    void validateWorkflowStructureNonStringLabelAddsError() {
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
    void validateWorkflowStructureMissingDescriptionAddsError() {
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
    void validateWorkflowStructureNonStringDescriptionAddsError() {
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
    void validateWorkflowStructureMissingTriggersAddsError() {
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
    void validateWorkflowStructureNonArrayTriggersAddsError() {
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
    void validateWorkflowStructureMultipleTriggersAddsError() {
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
    void validateWorkflowStructureNonObjectTriggerAddsError() {
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
    void validateWorkflowStructureMissingTasksAddsError() {
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
    void validateWorkflowStructureNonArrayTasksAddsError() {
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
    void validateWorkflowStructureNonObjectAddsError() {
        String invalidWorkflow = "\"not an object\"";

        StringBuilder errors = new StringBuilder();
        WorkflowValidator.validateWorkflowStructure(invalidWorkflow, errors);

        assertTrue(errors.toString()
            .contains("Workflow must be an object"));
    }

    @Test
    void validateWorkflowStructureInvalidJsonAddsError() {
        String invalidWorkflow = "{invalid json}";

        StringBuilder errors = new StringBuilder();
        WorkflowValidator.validateWorkflowStructure(invalidWorkflow, errors);

        String string = errors.toString();

        assertTrue(string.contains("Invalid JSON format:"), string);
    }

    @Test
    void validateWorkflowTasksValidTasksNoErrors() {
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

        Map<String, List<PropertyInfo>> taskDefinitionMap = Map.of(
            "component/v1/trigger1", List.of(
                new PropertyInfo("name", "STRING", null, true, true, null, null),
                new PropertyInfo("age", "INTEGER", null, false, true, null, null)),
            "component/v1/action1", List.of(
                new PropertyInfo("active", "BOOLEAN", null, true, true, null, null),
                new PropertyInfo("name", "STRING", null, true, true, null, null)));

        Map<String, PropertyInfo> taskOutputMap = Map.of(
            "component/v1/trigger1", trigger1, "component/v1/action2", action1);

        try {
            JsonNode tasksJsonNode = JsonUtils.readTree(tasksJson);
            List<JsonNode> taskJsonNodes = new ArrayList<>();

            for (JsonNode taskJsonNode : tasksJsonNode) {
                taskJsonNodes.add(taskJsonNode);
            }

            StringBuilder errors = new StringBuilder();
            StringBuilder warnings = new StringBuilder();

            WorkflowValidator.validateWorkflowTasks(taskJsonNodes, taskDefinitionMap, taskOutputMap, errors, warnings);

            assertEquals("", errors.toString());
            assertEquals("", warnings.toString());
        } catch (Exception e) {
            fail("Should not throw exception for valid tasks: " + e.getMessage());
        }
    }

    @Test
    void validateWorkflowTasksInvalidTaskStructureAddsError() {
        String tasksJson = """
            [
                {
                    "name": "testTask1",
                    "type": "component/v1/trigger1",
                    "parameters": {}
                }
            ]
            """;

        Map<String, List<PropertyInfo>> taskDefinitionMap = Map.of(
            "component/v1/trigger1", List.of());

        Map<String, PropertyInfo> taskOutputMap = Map.of(
            "component/v1/trigger1",
            new PropertyInfo("testTask1", "component/v1/action", "", true, true, null, null));

        try {
            JsonNode tasksJsonNode = JsonUtils.readTree(tasksJson);
            List<JsonNode> taskJsonNodes = new ArrayList<>();
            for (JsonNode taskJsonNode : tasksJsonNode) {
                taskJsonNodes.add(taskJsonNode);
            }
            StringBuilder errors = new StringBuilder();
            StringBuilder warnings = new StringBuilder();

            WorkflowValidator.validateWorkflowTasks(taskJsonNodes, taskDefinitionMap, taskOutputMap, errors, warnings);

            assertEquals("Missing required field: label", errors.toString());
            assertEquals("", warnings.toString());
        } catch (Exception e) {
            fail("Should not throw exception: " + e.getMessage());
        }
    }

    @Test
    void validateWorkflowTasksInvalidTaskParametersAddsError() {
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

        Map<String, List<PropertyInfo>> taskDefinitionMap = Map.of(
            "component/v1/trigger1", List.of(
                new PropertyInfo("name", "STRING", null, true, true, null, null),
                new PropertyInfo("age", "INTEGER", null, true, true, null, null)));

        Map<String, PropertyInfo> taskOutputMap = Map.of(
            "component/v1/trigger1", trigger1);

        try {
            JsonNode tasksJsonNode = JsonUtils.readTree(tasksJson);
            List<JsonNode> taskJsonNodes = new ArrayList<>();

            for (JsonNode taskJsonNode : tasksJsonNode) {
                taskJsonNodes.add(taskJsonNode);
            }

            StringBuilder errors = new StringBuilder();
            StringBuilder warnings = new StringBuilder();

            WorkflowValidator.validateWorkflowTasks(taskJsonNodes, taskDefinitionMap, taskOutputMap, errors, warnings);

            assertEquals("""
                Property 'name' has incorrect type. Expected: string, but got: integer
                Missing required property: age""", errors.toString());
            assertEquals("", warnings.toString());
        } catch (Exception e) {
            fail("Should not throw exception: " + e.getMessage());
        }
    }

    @Test
    void validateWorkflowTasksNoSuchAttributeOutputAddsError() {
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

        Map<String, List<PropertyInfo>> taskDefinitionMap = Map.of(
            "component/v1/trigger1", List.of(
                new PropertyInfo("name", "STRING", null, true, true, null, null),
                new PropertyInfo("age", "INTEGER", null, false, true, null, null)),
            "component/v1/action1", List.of(
                new PropertyInfo("active", "BOOLEAN", null, true, true, null, null),
                new PropertyInfo("name", "STRING", null, true, true, null, null)));

        Map<String, PropertyInfo> taskOutputMap = Map.of(
            "component/v1/trigger1", trigger1, "component/v1/action2", action1);

        try {
            JsonNode tasksJsonNode = JsonUtils.readTree(tasksJson);
            List<JsonNode> taskJsonNodes = new ArrayList<>();

            for (JsonNode taskNode : tasksJsonNode) {
                taskJsonNodes.add(taskNode);
            }

            StringBuilder errors = new StringBuilder();
            StringBuilder warnings = new StringBuilder();

            WorkflowValidator.validateWorkflowTasks(taskJsonNodes, taskDefinitionMap, taskOutputMap, errors, warnings);

            assertEquals("", errors.toString());
            assertEquals(
                "Property 'testTask1.propBool' might not exist in the output of 'component/v1/trigger1'",
                warnings.toString());
        } catch (Exception e) {
            fail("Should not throw exception for valid tasks: " + e.getMessage());
        }
    }

    @Test
    void validateWorkflowTasksWrongDataPillTypeAddsError() {
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

        Map<String, List<PropertyInfo>> taskDefinitionMap = Map.of(
            "component/v1/trigger1", List.of(
                new PropertyInfo("name", "STRING", null, true, true, null, null),
                new PropertyInfo("age", "INTEGER", null, false, true, null, null)),
            "component/v1/action1", List.of(
                new PropertyInfo("active", "BOOLEAN", null, true, true, null, null),
                new PropertyInfo("name", "STRING", null, true, true, null, null)));

        Map<String, PropertyInfo> taskOutputMap = Map.of(
            "component/v1/trigger1", trigger1, "component/v1/action2", action1);

        try {
            JsonNode tasksJsonNode = JsonUtils.readTree(tasksJson);
            List<JsonNode> taskJsonNodes = new ArrayList<>();

            for (JsonNode taskJsonNode : tasksJsonNode) {
                taskJsonNodes.add(taskJsonNode);
            }

            StringBuilder errors = new StringBuilder();
            StringBuilder warnings = new StringBuilder();

            WorkflowValidator.validateWorkflowTasks(taskJsonNodes, taskDefinitionMap, taskOutputMap, errors, warnings);

            assertEquals(
                "Property 'testTask1.propString' in output of 'component/v1/trigger1' is of type string, not boolean",
                errors.toString());
            assertEquals("", warnings.toString());
        } catch (Exception e) {
            fail("Should not throw exception for valid tasks: " + e.getMessage());
        }
    }

    @Test
    void validateWorkflowTasksIncorrectOutputOrderAddsError() {
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

        Map<String, List<PropertyInfo>> taskDefinitionMap = Map.of(
            "component/v1/trigger1", List.of(
                new PropertyInfo("name", "STRING", null, true, true, null, null),
                new PropertyInfo("age", "INTEGER", null, false, true, null, null)),
            "component/v1/action1", List.of(
                new PropertyInfo("active", "BOOLEAN", null, true, true, null, null),
                new PropertyInfo("name", "STRING", null, true, true, null, null)),
            "component/v1/action2", List.of(
                new PropertyInfo("active", "BOOLEAN", null, true, true, null, null),
                new PropertyInfo("name", "STRING", null, true, true, null, null)));

        Map<String, PropertyInfo> taskOutputMap = Map.of(
            "component/v1/trigger1", trigger1, "component/v1/action2", action3, "component/v1/action1", action1);

        try {
            JsonNode tasksJsonNode = JsonUtils.readTree(tasksJson);
            List<JsonNode> taskJsonNodes = new ArrayList<>();

            for (JsonNode taskJsonNode : tasksJsonNode) {
                taskJsonNodes.add(taskJsonNode);
            }

            StringBuilder errors = new StringBuilder();
            StringBuilder warnings = new StringBuilder();

            WorkflowValidator.validateWorkflowTasks(taskJsonNodes, taskDefinitionMap, taskOutputMap, errors, warnings);

            assertEquals("Wrong task order: You can't reference 'testTask3.prepNumber' in testTask2",
                errors.toString());
            assertEquals("Property 'testTask1.propBool' might not exist in the output of 'component/v1/trigger1'",
                warnings.toString());
        } catch (Exception e) {
            fail("Should not throw exception for valid tasks: " + e.getMessage());
        }
    }

    @Test
    void validateWorkflowTasksTaskWithMissingParametersFieldHandlesCorrectly() {
        String tasksJson = """
            [
                {
                    "label": "Test Task",
                    "name": "testTask1",
                    "type": "component/v1/trigger1"
                }
            ]
            """;

        Map<String, List<PropertyInfo>> taskDefinitionMap = Map.of(
            "component/v1/trigger1", List.of());

        Map<String, PropertyInfo> taskOutputMap = Map.of(
            "component/v1/trigger1", trigger1);

        try {
            JsonNode tasksJsonNode = JsonUtils.readTree(tasksJson);
            List<JsonNode> taskJsonNodes = new ArrayList<>();

            for (JsonNode taskJsonNode : tasksJsonNode) {
                taskJsonNodes.add(taskJsonNode);
            }

            StringBuilder errors = new StringBuilder();
            StringBuilder warnings = new StringBuilder();

            WorkflowValidator.validateWorkflowTasks(taskJsonNodes, taskDefinitionMap, taskOutputMap, errors, warnings);

            String string = errors.toString();

            assertTrue(
                string.contains("Missing required field: parameters") ||
                    string.contains("Task definition must have a 'parameters' object"));
        } catch (Exception e) {
            fail("Should handle missing parameters field: " + e.getMessage());
        }
    }

    @Test
    void validateWorkflowTasksMultipleTasksValidatesAll() {
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

        Map<String, List<PropertyInfo>> taskDefinitionMap = Map.of(
            "component/v1/trigger1", List.of(
                new PropertyInfo("name", "STRING", null, true, true, null, null)),
            "component/v1/action1", List.of(
                new PropertyInfo("age", "INTEGER", null, true, true, null, null)));

        Map<String, PropertyInfo> taskOutputMap = Map.of(
            "component/v1/trigger1", trigger1,
            "component/v1/action1", action1);

        try {
            JsonNode tasksJsonNode = JsonUtils.readTree(tasksJson);
            List<JsonNode> taskJsonNodes = new ArrayList<>();

            for (JsonNode taskJsonNode : tasksJsonNode) {
                taskJsonNodes.add(taskJsonNode);
            }

            StringBuilder errors = new StringBuilder();
            StringBuilder warnings = new StringBuilder();

            WorkflowValidator.validateWorkflowTasks(taskJsonNodes, taskDefinitionMap, taskOutputMap, errors, warnings);

            assertEquals("""
                Missing required field: label
                Property 'age' has incorrect type. Expected: integer, but got: string""", errors.toString());
            assertEquals("", warnings.toString());
        } catch (Exception e) {
            fail("Should not throw exception: " + e.getMessage());
        }
    }

    @Test
    void validateWorkflowTasksFlowLoopWrongTypes() {
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

        Map<String, List<PropertyInfo>> taskDefinitionMap = Map.of(
            "component/v1/trigger1", List.of(
                new PropertyInfo("name", "STRING", null, false, true, null, null)),
            "loop/v1", List.of(
                new PropertyInfo("items", "ARRAY", null, false, true, null, List.of()),
                new PropertyInfo("loopForever", "BOOLEAN", null, false, true, null, null),
                new PropertyInfo("iteratee", "ARRAY", null, false, true, null, List.of(
                    new PropertyInfo(null, "TASK", null, false, true, null, null)))),
            "component/v1/action1", List.of(
                new PropertyInfo("name", "STRING", null, false, true, null, null)),
            "component/v1/action2", List.of(
                new PropertyInfo("age", "NUMBER", null, false, true, null, null)));

        Map<String, PropertyInfo> taskOutputMap = Map.of(
            "component/v1/trigger1", actionArr,
            "component/v1/action1", action1);

        try {
            JsonNode tasksJsonNode = JsonUtils.readTree(tasksJson);
            List<JsonNode> taskJsonNodes = new ArrayList<>();

            for (JsonNode taskJsonNode : tasksJsonNode) {
                taskJsonNodes.add(taskJsonNode);
            }

            StringBuilder errors = new StringBuilder();
            StringBuilder warnings = new StringBuilder();

            WorkflowValidator.validateWorkflowTasks(taskJsonNodes, taskDefinitionMap, taskOutputMap, errors, warnings);

            assertEquals("""
                Property 'loop1.item[0]' in output of 'loop/v1' is of type string, not number
                Property 'loop1.item[2]' in output of 'loop/v1' is of type object, not number""", errors.toString());
            assertEquals("", warnings.toString());
        } catch (Exception e) {
            fail("Should not throw exception: " + e.getMessage());
        }
    }

    @Test
    void validateWorkflowTasksFlowLoopWrongParameters() {
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

        Map<String, List<PropertyInfo>> taskDefinitionMap = Map.of(
            "component/v1/trigger1", List.of(
                new PropertyInfo("name", "STRING", null, false, true, null, null)),
            "loop/v1", List.of(
                new PropertyInfo("items", "ARRAY", null, false, true, null, List.of()),
                new PropertyInfo("loopForever", "BOOLEAN", null, false, true, null, null),
                new PropertyInfo("iteratee", "ARRAY", null, false, true, null, List.of(
                    new PropertyInfo(null, "TASK", null, false, true, null, null)))),
            "component/v1/action1", List.of(
                new PropertyInfo("name", "STRING", null, false, true, null, null)),
            "component/v1/action2", List.of(
                new PropertyInfo("age", "NUMBER", null, false, true, null, null)));

        Map<String, PropertyInfo> taskOutputMap = Map.of(
            "component/v1/trigger1", actionArr,
            "component/v1/action1", action1);

        try {
            JsonNode tasksJsonNode = JsonUtils.readTree(tasksJson);
            List<JsonNode> taskJsonNodes = new ArrayList<>();

            for (JsonNode taskJsonNode : tasksJsonNode) {
                taskJsonNodes.add(taskJsonNode);
            }

            StringBuilder errors = new StringBuilder();
            StringBuilder warnings = new StringBuilder();

            WorkflowValidator.validateWorkflowTasks(taskJsonNodes, taskDefinitionMap, taskOutputMap, errors, warnings);

            assertEquals(
                "Property 'loop1.item[0]' in output of 'loop/v1' is of type boolean, not number",
                errors.toString());
            assertEquals("", warnings.toString());
        } catch (Exception e) {
            fail("Should not throw exception: " + e.getMessage());
        }
    }

    @Test
    void validateWorkflowTasksFlowConditionNoErrors() {
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

        Map<String, List<PropertyInfo>> taskDefinitionMap = Map.of(
            "component/v1/trigger1", List.of(
                new PropertyInfo("name", "STRING", null, false, true, null, null)),
            "condition/v1", List.of(
                new PropertyInfo("rawExpression", "BOOLEAN", null, false, true, null, null),
                new PropertyInfo("conditions", "ARRAY", null, false, true, "rawExpression == false", List.of(
                    new PropertyInfo(null, "ARRAY", null, false, false, null, List.of(
                        new PropertyInfo("boolean", "OBJECT", null, false, false, null, List.of(
                            new PropertyInfo("type", "STRING", null, false, true, null, null),
                            new PropertyInfo("value1", "BOOLEAN", null, true, true, null, null),
                            new PropertyInfo("operation", "STRING", null, true, true, null, null),
                            new PropertyInfo("value2", "BOOLEAN", null, true, true, null, null))),
                        new PropertyInfo("dateTime", "OBJECT", null, false, false, null, List.of(
                            new PropertyInfo("type", "STRING", null, false, true, null, null),
                            new PropertyInfo("value1", "DATE_TIME", null, true, true, null, null),
                            new PropertyInfo("operation", "STRING", null, true, true, null, null),
                            new PropertyInfo("value2", "DATE_TIME", null, true, true, null, null))),
                        new PropertyInfo("number", "OBJECT", null, false, false, null, List.of(
                            new PropertyInfo("type", "STRING", null, false, true, null, null),
                            new PropertyInfo("value1", "NUMBER", null, true, true, null, null),
                            new PropertyInfo("operation", "STRING", null, true, true, null, null),
                            new PropertyInfo("value2", "NUMBER", null, true, true,
                                "conditions[index][index].operation != 'EMPTY'", null))),
                        new PropertyInfo("string", "OBJECT", null, false, false, null, List.of(
                            new PropertyInfo("type", "STRING", null, false, true, null, null),
                            new PropertyInfo("value1", "STRING", null, true, true, null, null),
                            new PropertyInfo("operation", "STRING", null, true, true, null, null),
                            new PropertyInfo("value2", "STRING", null, true, true,
                                "!contains({'EMPTY','REGEX'}, conditions[index][index].operation)", null))))))),
                new PropertyInfo("expression", "STRING", null, false, true, "rawExpression == true", null),
                new PropertyInfo("caseTrue", "ARRAY", null, false, true, null, List.of(
                    new PropertyInfo(null, "TASK", null, false, false, null, null))),
                new PropertyInfo("caseFalse", "ARRAY", null, false, true, null, List.of(
                    new PropertyInfo(null, "TASK", null, false, false, null, null)))),
            "component/v1/action1", List.of(
                new PropertyInfo("name", "STRING", null, false, true, null, null)));

        Map<String, PropertyInfo> taskOutputMap = Map.of(
            "component/v1/trigger1", trigger1,
            "component/v1/action1", action1);

        try {
            JsonNode tasksJsonNode = JsonUtils.readTree(tasksJson);
            List<JsonNode> taskJsonNodes = new ArrayList<>();

            for (JsonNode taskJsonNode : tasksJsonNode) {
                taskJsonNodes.add(taskJsonNode);
            }

            StringBuilder errors = new StringBuilder();
            StringBuilder warnings = new StringBuilder();

            WorkflowValidator.validateWorkflowTasks(taskJsonNodes, taskDefinitionMap, taskOutputMap, errors, warnings);

            assertEquals("", errors.toString());
            assertEquals("", warnings.toString());
        } catch (Exception e) {
            fail("Should not throw exception: " + e.getMessage());
        }
    }

    @Test
    void validateWorkflowTasksFlowConditionWithErrors() {
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

        Map<String, List<PropertyInfo>> taskDefinitionMap = Map.of(
            "component/v1/trigger1", List.of(
                new PropertyInfo("name", "STRING", null, false, true, null, null)),
            "condition/v1", List.of(
                new PropertyInfo("rawExpression", "BOOLEAN", null, false, true, null, null),
                new PropertyInfo("conditions", "ARRAY", null, false, true, "rawExpression == false", List.of(
                    new PropertyInfo(null, "ARRAY", null, false, false, null, List.of(
                        new PropertyInfo("boolean", "OBJECT", null, false, false, null, List.of(
                            new PropertyInfo("type", "STRING", null, false, true, null, null),
                            new PropertyInfo("value1", "BOOLEAN", null, true, true, null, null),
                            new PropertyInfo("operation", "STRING", null, true, true, null, null),
                            new PropertyInfo("value2", "BOOLEAN", null, true, true, null, null))),
                        new PropertyInfo("dateTime", "OBJECT", null, false, false, null, List.of(
                            new PropertyInfo("type", "STRING", null, false, true, null, null),
                            new PropertyInfo("value1", "DATE_TIME", null, true, true, null, null),
                            new PropertyInfo("operation", "STRING", null, true, true, null, null),
                            new PropertyInfo("value2", "DATE_TIME", null, true, true, null, null))),
                        new PropertyInfo("number", "OBJECT", null, false, false, null, List.of(
                            new PropertyInfo("type", "STRING", null, false, true, null, null),
                            new PropertyInfo("value1", "NUMBER", null, true, true, null, null),
                            new PropertyInfo("operation", "STRING", null, true, true, null, null),
                            new PropertyInfo("value2", "NUMBER", null, true, true,
                                "conditions[index][index].operation != 'EMPTY'", null))),
                        new PropertyInfo("string", "OBJECT", null, false, false, null, List.of(
                            new PropertyInfo("type", "STRING", null, false, true, null, null),
                            new PropertyInfo("value1", "STRING", null, true, true, null, null),
                            new PropertyInfo("operation", "STRING", null, true, true, null, null),
                            new PropertyInfo("value2", "STRING", null, true, true,
                                "!contains({'EMPTY','REGEX'}, conditions[index][index].operation)", null))))))),
                new PropertyInfo("expression", "STRING", null, false, true, "rawExpression == true", null),
                new PropertyInfo("caseTrue", "ARRAY", null, false, true, null, List.of(
                    new PropertyInfo(null, "TASK", null, false, false, null, null))),
                new PropertyInfo("caseFalse", "ARRAY", null, false, true, null, List.of(
                    new PropertyInfo(null, "TASK", null, false, false, null, null)))),
            "component/v1/action1", List.of(
                new PropertyInfo("name", "STRING", null, true, true, null, null)));

        Map<String, PropertyInfo> taskOutputMap = Map.of(
            "component/v1/trigger1", trigger1,
            "component/v1/action1", action1);

        try {
            JsonNode tasksJsonNode = JsonUtils.readTree(tasksJson);
            List<JsonNode> taskJsonNodes = new ArrayList<>();

            for (JsonNode taskJsonNode : tasksJsonNode) {
                taskJsonNodes.add(taskJsonNode);
            }

            StringBuilder errors = new StringBuilder();
            StringBuilder warnings = new StringBuilder();

            WorkflowValidator.validateWorkflowTasks(taskJsonNodes, taskDefinitionMap, taskOutputMap, errors, warnings);

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
    void validateWorkflowTasksFlowConditionAndLoopNoErrors() {
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

        Map<String, List<PropertyInfo>> taskDefinitionMap = Map.of(
            "component/v1/trigger1", List.of(
                new PropertyInfo("name", "STRING", null, false, true, null, null)),
            "condition/v1", List.of(
                new PropertyInfo("rawExpression", "BOOLEAN", null, true, true, null, null),
                new PropertyInfo("conditions", "ARRAY", null, false, true, "rawExpression == false", List.of(
                    new PropertyInfo(null, "ARRAY", null, false, false, null, List.of(
                        new PropertyInfo("boolean", "OBJECT", null, false, false, null, List.of(
                            new PropertyInfo("type", "STRING", null, false, true, null, null),
                            new PropertyInfo("value1", "BOOLEAN", null, true, true, null, null),
                            new PropertyInfo("operation", "STRING", null, true, true, null, null),
                            new PropertyInfo("value2", "BOOLEAN", null, true, true, null, null))),
                        new PropertyInfo("dateTime", "OBJECT", null, false, false, null, List.of(
                            new PropertyInfo("type", "STRING", null, false, true, null, null),
                            new PropertyInfo("value1", "DATE_TIME", null, true, true, null, null),
                            new PropertyInfo("operation", "STRING", null, true, true, null, null),
                            new PropertyInfo("value2", "DATE_TIME", null, true, true, null, null))),
                        new PropertyInfo("number", "OBJECT", null, false, false, null, List.of(
                            new PropertyInfo("type", "STRING", null, false, true, null, null),
                            new PropertyInfo("value1", "NUMBER", null, true, true, null, null),
                            new PropertyInfo("operation", "STRING", null, true, true, null, null),
                            new PropertyInfo("value2", "NUMBER", null, true, true,
                                "conditions[index][index].operation != 'EMPTY'", null))),
                        new PropertyInfo("string", "OBJECT", null, false, false, null, List.of(
                            new PropertyInfo("type", "STRING", null, false, true, null, null),
                            new PropertyInfo("value1", "STRING", null, true, true, null, null),
                            new PropertyInfo("operation", "STRING", null, true, true, null, null),
                            new PropertyInfo("value2", "STRING", null, true, true,
                                "!contains({'EMPTY','REGEX'}, conditions[index][index].operation)", null))))))),
                new PropertyInfo("expression", "STRING", null, false, true, "rawExpression == true", null),
                new PropertyInfo("caseTrue", "ARRAY", null, false, true, null, List.of(
                    new PropertyInfo(null, "TASK", null, false, false, null, null))),
                new PropertyInfo("caseFalse", "ARRAY", null, false, true, null, List.of(
                    new PropertyInfo(null, "TASK", null, false, false, null, null)))),
            "loop/v1", List.of(
                new PropertyInfo("items", "ARRAY", null, false, true, null, List.of()),
                new PropertyInfo("loopForever", "BOOLEAN", null, false, true, null, null),
                new PropertyInfo("iteratee", "ARRAY", null, false, true, null, List.of(
                    new PropertyInfo(null, "TASK", null, false, true, null, null)))),
            "component/v1/action1", List.of(
                new PropertyInfo("name", "STRING", null, false, true, null, null)),
            "component/v1/action2", List.of(
                new PropertyInfo("age", "NUMBER", null, false, true, null, null)));

        Map<String, PropertyInfo> taskOutputMap = Map.of(
            "component/v1/trigger1", actionArr,
            "component/v1/action1", action1);

        try {
            JsonNode tasksJsonNode = JsonUtils.readTree(tasksJson);
            List<JsonNode> taskJsonNodes = new ArrayList<>();

            for (JsonNode taskJsonNode : tasksJsonNode) {
                taskJsonNodes.add(taskJsonNode);
            }
            StringBuilder errors = new StringBuilder();
            StringBuilder warnings = new StringBuilder();

            WorkflowValidator.validateWorkflowTasks(taskJsonNodes, taskDefinitionMap, taskOutputMap, errors, warnings);

            assertEquals("Property 'loop1.item[0]' in output of 'loop/v1' is of type boolean, not number",
                errors.toString());
            assertEquals("Property 'task1.propString' might not exist in the output of 'component/v1/trigger1'",
                warnings.toString());
        } catch (Exception e) {
            fail("Should not throw exception: " + e.getMessage());
        }
    }

    @Test
    void validateWorkflowValidWorkflowNoErrors() {
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
            new PropertyInfo("name", "STRING", null, false, true, null, null));

        WorkflowValidator.TaskOutputProvider taskOutputProvider =
            (taskType, kind, warningsBuilder) -> new PropertyInfo("result", "STRING", null, false, false,
                null, null);

        WorkflowValidator.validateWorkflow(workflow, taskDefProvider, taskOutputProvider, new HashMap<>(),
            new HashMap<>(), errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateWorkflowInvalidStructureHasErrors() {
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

        WorkflowValidator.validateWorkflow(workflow, taskDefProvider, taskOutputProvider, new HashMap<>(),
            new HashMap<>(), errors, warnings);

        assertEquals("Missing required field: label", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateWorkflowDuplicateTaskNamesHasErrors() {
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

        WorkflowValidator.validateWorkflow(workflow, taskDefProvider, taskOutputProvider, new HashMap<>(),
            new HashMap<>(), errors, warnings);

        assertEquals("Tasks cannot have repeating names: duplicate_name", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateWorkflowStringFormatNoErrors() {
        String workflow =
            "{\n  \"label\": \"Productboard Feature Update to Box Document Sync\",\n  \"description\": \"When a feature is updated on Productboard, this workflow lists 10 published documents with Coda, fetches their content, processes them with OpenAI to match Productboard attributes, and uploads the updated documents to Box.\",\n  \"inputs\": [],\n  \"triggers\": [\n    {\n      \"label\": \"Productboard Feature Updated\",\n      \"name\": \"productboard_1\",\n      \"type\": \"productboard/v1/updatedFeature\",\n      \"parameters\": {}\n    }\n  ],\n  \"tasks\": [\n    {\n      \"label\": \"List Published Coda Documents\",\n      \"name\": \"coda_1\",\n      \"type\": \"coda/v1/listDocs\",\n      \"parameters\": {\n        \"isPublished\": true,\n        \"limit\": 10\n      }\n    },\n    {\n      \"label\": \"Loop Through Documents\",\n      \"name\": \"loop_1\",\n      \"type\": \"loop/v1\",\n      \"parameters\": {\n        \"items\": \"${coda_1.items}\",\n        \"iteratee\": [\n          {\n            \"label\": \"Fetch Document Content\",\n            \"name\": \"httpClient_1\",\n            \"type\": \"httpClient/v1/get\",\n            \"parameters\": {\n              \"uri\": \"${loop_1.item.href}\"\n            }\n          },\n          {\n            \"label\": \"Process Document with OpenAI\",\n            \"name\": \"openAi_1\", \n            \"type\": \"openAi/v1/ask\",\n            \"parameters\": {\n              \"model\": \"gpt-4o\",\n              \"format\": \"SIMPLE\",\n              \"userPrompt\": \"Please analyze the following document content and the Productboard feature attributes, then update the document attributes to match the Productboard attributes where applicable. Document content: ${httpClient_1.body}. Productboard updated attributes: ${productboard_1.updatedAttributes}. Productboard feature ID: ${productboard_1.id}. Please return an updated version of the document with matching attributes.\",\n              \"systemPrompt\": \"You are an expert at analyzing documents and matching attributes between systems. Your task is to update document attributes to align with Productboard feature data while preserving the original document structure and content where possible.\"\n            }\n          },\n          {\n            \"label\": \"Upload Updated Document to Box\",\n            \"name\": \"box_1\",\n            \"type\": \"box/v1/uploadFile\", \n            \"parameters\": {\n              \"id\": \"0\",\n              \"file\": {\n                \"name\": \"${loop_1.item.name}_updated_${productboard_1.id}.txt\",\n                \"extension\": \"txt\",\n                \"mimeType\": \"text/plain\",\n                \"url\": \"data:text/plain;charset=utf-8,${openAi_1}\"\n              }\n            }\n          }\n        ]\n      }\n    }\n  ]\n}";

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        WorkflowValidator.TaskDefinitionProvider taskDefProvider = (taskType, kind) -> List.of();
        WorkflowValidator.TaskOutputProvider taskOutputProvider = (taskType, kind, warningsBuilder) -> null;

        WorkflowValidator.validateWorkflow(workflow, taskDefProvider, taskOutputProvider, new HashMap<>(),
            new HashMap<>(), errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateWorkflowTasksAddsNestedTasksToTaskDefinitionsConditionNoErrors() {
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
        Map<String, List<PropertyInfo>> taskDefinitionMap = new HashMap<>();
        Map<String, PropertyInfo> taskOutputMap = new HashMap<>();

        WorkflowValidator.validateWorkflow(workflow, taskDefProvider, taskOutputProvider, taskDefinitionMap,
            taskOutputMap, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("", warnings.toString());
        assertEquals("{component/v1/action1=[], component/v1/trigger1=[], condition/v1=[]}",
            taskDefinitionMap.toString());
        assertEquals("{component/v1/action1=null, component/v1/trigger1=null, condition/v1=null}",
            taskOutputMap.toString());
    }

    @Test
    void validateWorkflowTasksAddsNestedTasksToTaskDefinitionsLoopNoErrors() {
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
        Map<String, List<PropertyInfo>> taskDefinitionMap = new HashMap<>();
        Map<String, PropertyInfo> taskOutputMap = new HashMap<>();

        WorkflowValidator.validateWorkflow(workflow, taskDefProvider, taskOutputProvider, taskDefinitionMap,
            taskOutputMap, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("", warnings.toString());
        assertEquals("{component/v1/action2=[], component/v1/action1=[], component/v1/trigger1=[], loop/v1=[]}",
            taskDefinitionMap.toString());
        assertEquals("{component/v1/action2=null, component/v1/action1=null, component/v1/trigger1=null, loop/v1=null}",
            taskOutputMap.toString());
    }

    @Test
    void validateWorkflowTasksAddsNestedTasksToTaskDefinitionMapMultipleFlowsNoErrors() {
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
        Map<String, List<PropertyInfo>> taskDefinitionMap = new HashMap<>();
        Map<String, PropertyInfo> taskOutputMap = new HashMap<>();

        WorkflowValidator.validateWorkflow(workflow, taskDefProvider, taskOutputProvider, taskDefinitionMap,
            taskOutputMap, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("", warnings.toString());
        assertEquals(
            "{component/v1/action2=[], component/v1/action1=[], component/v1/trigger1=[], loop/v1=[], condition/v1=[]}",
            taskDefinitionMap.toString());
        assertEquals(
            "{component/v1/action2=null, component/v1/action1=null, component/v1/trigger1=null, loop/v1=null, condition/v1=null}",
            taskOutputMap.toString());
    }

    @Test
    void validateSingleTaskValidTaskNoErrors() {
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
            new PropertyInfo("name", "STRING", null, true, true, null, null),
            new PropertyInfo("age", "INTEGER", null, false, true, null, null));

        WorkflowValidator.validateSingleTask(task, taskDefProvider, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateSingleTaskInvalidStructureHasErrors() {
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
    void validateSingleTaskMissingRequiredParameterHasErrors() {
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
            new PropertyInfo("name", "STRING", null, true, true, null, null),
            new PropertyInfo("age", "INTEGER", null, false, true, null, null));

        WorkflowValidator.validateSingleTask(task, taskDefProvider, errors, warnings);

        assertEquals("Missing required property: name", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void taskDefinitionProviderFunctionalInterfaceWorksCorrectly() {
        WorkflowValidator.TaskDefinitionProvider provider = (taskType, kind) -> {
            if ("test/v1".equals(taskType)) {
                return List.of(new PropertyInfo("test", "STRING", null, false, true, null, null));
            }
            return List.of();
        };

        List<PropertyInfo> result = provider.getTaskProperties("test/v1", "action");

        assertEquals(1, result.size());

        PropertyInfo propertyInfo = result.getFirst();

        assertEquals("test", propertyInfo.name());

        List<PropertyInfo> emptyResult = provider.getTaskProperties("unknown/v1", "action");

        assertTrue(emptyResult.isEmpty());
    }

    @Test
    void taskOutputProviderFunctionalInterfaceThrowsError() {
        WorkflowValidator.TaskOutputProvider provider = (taskType, kind, warnings) -> {
            if ("test/v1".equals(taskType)) {
                return new PropertyInfo("output", "STRING", null, false, false, null, null);
            }

            warnings.append("Unknown task type: ");
            warnings.append(taskType);

            return null;
        };

        StringBuilder warnings = new StringBuilder();

        PropertyInfo resultPropertyInfo = provider.getTaskOutputProperty("test/v1", "action", warnings);

        assertNotNull(resultPropertyInfo);
        assertEquals("output", resultPropertyInfo.name());
        assertEquals("", warnings.toString());

        PropertyInfo nullResultPropertyInfo = provider.getTaskOutputProperty("unknown/v1", "action", warnings);

        assertNull(nullResultPropertyInfo);

        String warningsString = warnings.toString();

        assertTrue(warningsString.contains("Unknown task type: unknown/v1"));
    }
}
