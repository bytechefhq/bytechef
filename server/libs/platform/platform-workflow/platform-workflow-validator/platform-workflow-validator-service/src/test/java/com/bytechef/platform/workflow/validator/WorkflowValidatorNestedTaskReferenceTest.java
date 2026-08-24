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
class WorkflowValidatorNestedTaskReferenceTest {

    private static final PropertyInfo ACTION_OUTPUT =
        new PropertyInfo("propString", "STRING", null, false, true, null, null);

    private static final Map<String, List<PropertyInfo>> TASK_DEFINITION_MAP = Map.of(
        "component/v1/action1", List.of(
            new PropertyInfo("name", "STRING", null, false, true, null, null)),
        "condition/v1", List.of(
            new PropertyInfo("rawExpression", "BOOLEAN", null, false, true, null, null),
            new PropertyInfo("expression", "STRING", null, false, true, "rawExpression == true", null),
            new PropertyInfo("caseTrue", "ARRAY", null, false, true, null, List.of(
                new PropertyInfo(null, "TASK", null, false, false, null, null))),
            new PropertyInfo("caseFalse", "ARRAY", null, false, true, null, List.of(
                new PropertyInfo(null, "TASK", null, false, false, null, null)))),
        "branch/v1", List.of(
            new PropertyInfo("expression", "STRING", null, false, true, null, null),
            new PropertyInfo("cases", "ARRAY", null, false, true, null, List.of(
                new PropertyInfo(null, "OBJECT", null, false, false, null, List.of(
                    new PropertyInfo("key", "STRING", null, false, true, null, null),
                    new PropertyInfo("tasks", "ARRAY", null, false, true, null, List.of(
                        new PropertyInfo(null, "TASK", null, false, false, null, null))))))),
            new PropertyInfo("default", "ARRAY", null, false, true, null, List.of(
                new PropertyInfo(null, "TASK", null, false, false, null, null)))));

    private static final Map<String, PropertyInfo> TASK_OUTPUT_MAP = Map.of(
        "component/v1/action1", ACTION_OUTPUT);

    @BeforeAll
    public static void beforeAll() {
        JsonUtils.setObjectMapper(
            JsonMapper.builder()
                .build());
    }

    @Test
    void validateWorkflowTasksResolvesBareReferenceToTaskNestedInCondition() {
        String tasksJson = """
            [
                {
                    "label": "Condition",
                    "name": "condition_1",
                    "type": "condition/v1",
                    "parameters": {
                        "rawExpression": true,
                        "expression": "true",
                        "caseTrue": [
                            {
                                "label": "Task 1",
                                "name": "task1",
                                "type": "component/v1/action1",
                                "parameters": {
                                    "name": "John"
                                }
                            },
                            {
                                "label": "Task 2",
                                "name": "task2",
                                "type": "component/v1/action1",
                                "parameters": {
                                    "name": "${task1}"
                                }
                            }
                        ],
                        "caseFalse": []
                    }
                }
            ]
            """;

        assertNoErrors(tasksJson);
    }

    @Test
    void validateWorkflowTasksResolvesBareReferenceToTaskNestedInBranchCase() {
        String tasksJson = """
            [
                {
                    "label": "Branch",
                    "name": "branch_1",
                    "type": "branch/v1",
                    "parameters": {
                        "expression": "BOT",
                        "cases": [
                            {
                                "key": "BOT",
                                "tasks": [
                                    {
                                        "label": "Task 1",
                                        "name": "task1",
                                        "type": "component/v1/action1",
                                        "parameters": {
                                            "name": "John"
                                        }
                                    },
                                    {
                                        "label": "Task 2",
                                        "name": "task2",
                                        "type": "component/v1/action1",
                                        "parameters": {
                                            "name": "${task1}"
                                        }
                                    }
                                ]
                            }
                        ],
                        "default": []
                    }
                }
            ]
            """;

        assertNoErrors(tasksJson);
    }

    @Test
    void validateWorkflowTasksValidatesStructureOfTaskNestedInBranchCase() {
        String tasksJson = """
            [
                {
                    "label": "Branch",
                    "name": "branch_1",
                    "type": "branch/v1",
                    "parameters": {
                        "expression": "BOT",
                        "cases": [
                            {
                                "key": "BOT",
                                "tasks": [
                                    {
                                        "name": "task1",
                                        "type": "component/v1/action1",
                                        "parameters": {
                                            "name": "John"
                                        }
                                    }
                                ]
                            }
                        ],
                        "default": []
                    }
                }
            ]
            """;

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        validate(tasksJson, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("[branch_1] Missing recommended field: label", warnings.toString());
    }

    private static void assertNoErrors(String tasksJson) {
        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        validate(tasksJson, errors, warnings);

        assertEquals("", errors.toString());
    }

    private static void validate(String tasksJson, StringBuilder errors, StringBuilder warnings) {
        JsonNode tasksJsonNode = JsonUtils.readTree(tasksJson);
        List<JsonNode> taskJsonNodes = new ArrayList<>();

        for (JsonNode taskJsonNode : tasksJsonNode) {
            taskJsonNodes.add(taskJsonNode);
        }

        WorkflowValidator.validateWorkflowTasks(
            taskJsonNodes, TASK_DEFINITION_MAP, TASK_OUTPUT_MAP, new HashMap<>(), errors, warnings);
    }
}
