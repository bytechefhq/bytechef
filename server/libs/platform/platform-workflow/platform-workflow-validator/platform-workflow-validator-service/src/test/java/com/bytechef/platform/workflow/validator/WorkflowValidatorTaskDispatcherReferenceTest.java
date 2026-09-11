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
class WorkflowValidatorTaskDispatcherReferenceTest {

    private static final PropertyInfo ITEM_VARIABLE_OUTPUT = new PropertyInfo(
        null, "OBJECT", null, false, false, null, null, List.of(
            new PropertyInfo("item", "STRING", null, false, false, null, null),
            new PropertyInfo("index", "INTEGER", null, false, false, null, null)),
        null);

    private static final PropertyInfo SUBFLOW_OUTPUT = new PropertyInfo(
        null, "OBJECT", null, false, false, null, null, List.of(
            new PropertyInfo("message", "STRING", null, false, false, null, null)),
        null);

    private static final Map<String, List<PropertyInfo>> TASK_DEFINITION_MAP = Map.of(
        "manual/v1/manual", List.of(),
        "map/v1", List.of(
            new PropertyInfo("items", "ARRAY", null, false, true, null, List.of()),
            new PropertyInfo("iteratee", "TASK", null, false, false, null, null)),
        "each/v1", List.of(
            new PropertyInfo("items", "ARRAY", null, false, true, null, List.of()),
            new PropertyInfo("iteratee", "TASK", null, false, false, null, null)),
        "var/v1/set", List.of(
            new PropertyInfo("type", "STRING", null, false, true, null, null),
            new PropertyInfo("value", "STRING", null, false, true, null, null)),
        "number/v1/set", List.of(
            new PropertyInfo("value", "NUMBER", null, false, true, null, null)),
        "subflow/v1", List.of(
            new PropertyInfo("workflowUuid", "STRING", null, false, true, null, null),
            new PropertyInfo("inputs", "OBJECT", null, false, true, null, null)),
        "logger/v1/info", List.of(
            new PropertyInfo("text", "STRING", null, false, true, null, null)),
        "condition/v1", List.of(
            new PropertyInfo("rawExpression", "BOOLEAN", null, false, true, null, null),
            new PropertyInfo("expression", "STRING", null, false, true, null, null),
            new PropertyInfo("caseTrue", "ARRAY", null, false, false, null, List.of(
                new PropertyInfo(null, "TASK", null, false, false, null, null))),
            new PropertyInfo("caseFalse", "ARRAY", null, false, false, null, List.of(
                new PropertyInfo(null, "TASK", null, false, false, null, null)))));

    private static final String MAP_WORKFLOW = """
        {
            "label": "workflow1",
            "description": "",
            "triggers": [
                {"label": "Manual", "name": "trigger_1", "type": "manual/v1/manual"}
            ],
            "tasks": [
                {
                    "label": "Map",
                    "name": "map_1",
                    "type": "map/v1",
                    "parameters": {
                        "items": ["ee", "eeeeee"],
                        "iteratee": [
                            {
                                "label": "Var",
                                "name": "var_1",
                                "type": "var/v1/set",
                                "parameters": {"type": "STRING", "value": "${map_1.item}"}
                            }
                        ]
                    }
                }
            ]
        }
        """;

    private static final String DEEP_MAP_WORKFLOW = """
        {
            "label": "workflow1",
            "description": "",
            "triggers": [
                {"label": "Manual", "name": "trigger_1", "type": "manual/v1/manual"}
            ],
            "tasks": [
                {
                    "label": "Map",
                    "name": "map_1",
                    "type": "map/v1",
                    "parameters": {
                        "items": ["ee"],
                        "iteratee": [
                            {
                                "label": "Condition",
                                "name": "condition_1",
                                "type": "condition/v1",
                                "parameters": {
                                    "rawExpression": true,
                                    "expression": "true",
                                    "caseTrue": [
                                        {
                                            "label": "Var",
                                            "name": "var_1",
                                            "type": "var/v1/set",
                                            "parameters": {"type": "STRING", "value": "${map_1.item}"}
                                        }
                                    ],
                                    "caseFalse": []
                                }
                            }
                        ]
                    }
                }
            ]
        }
        """;

    private static final String MAP_THEN_LOGGER_WORKFLOW = """
        {
            "label": "workflow1",
            "description": "",
            "triggers": [
                {"label": "Manual", "name": "trigger_1", "type": "manual/v1/manual"}
            ],
            "tasks": [
                {
                    "label": "Map",
                    "name": "map_1",
                    "type": "map/v1",
                    "parameters": {"items": ["ee"], "iteratee": []}
                },
                {
                    "label": "Logger",
                    "name": "logger_1",
                    "type": "logger/v1/info",
                    "parameters": {"text": "${map_1.item}"}
                }
            ]
        }
        """;

    private static final String LOGGER_THEN_MAP_WORKFLOW = """
        {
            "label": "workflow1",
            "description": "",
            "triggers": [
                {"label": "Manual", "name": "trigger_1", "type": "manual/v1/manual"}
            ],
            "tasks": [
                {
                    "label": "Logger",
                    "name": "logger_1",
                    "type": "logger/v1/info",
                    "parameters": {"text": "${map_1.item}"}
                },
                {
                    "label": "Map",
                    "name": "map_1",
                    "type": "map/v1",
                    "parameters": {"items": ["ee"], "iteratee": []}
                }
            ]
        }
        """;

    private static final String EACH_WORKFLOW = """
        {
            "label": "workflow1",
            "description": "",
            "triggers": [
                {"label": "Manual", "name": "trigger_1", "type": "manual/v1/manual"}
            ],
            "tasks": [
                {
                    "label": "Each",
                    "name": "each_1",
                    "type": "each/v1",
                    "parameters": {
                        "items": ["ee"],
                        "iteratee": {
                            "label": "Var",
                            "name": "var_1",
                            "type": "var/v1/set",
                            "parameters": {"type": "STRING", "value": "${each_1.item}"}
                        }
                    }
                }
            ]
        }
        """;

    private static final String SUBFLOW_WORKFLOW = """
        {
            "label": "workflow1",
            "description": "",
            "triggers": [
                {"label": "Manual", "name": "trigger_1", "type": "manual/v1/manual"}
            ],
            "tasks": [
                {
                    "label": "Subflow",
                    "name": "subflow_1",
                    "type": "subflow/v1",
                    "parameters": {"workflowUuid": "25327c41", "inputs": {"message": "message1"}}
                },
                {
                    "label": "Logger",
                    "name": "logger_1",
                    "type": "logger/v1/info",
                    "parameters": {"text": "${subflow_1.message}"}
                }
            ]
        }
        """;

    @Test
    void mapIterateeCanReferenceTheMapItem() {
        Result result = validate(MAP_WORKFLOW, Map.of(), Map.of("map_1", ITEM_VARIABLE_OUTPUT));

        assertEquals("", result.errors());
        assertEquals("", result.warnings());
    }

    @Test
    void mapIterateeReferenceIsNotReportedAsAWrongTaskOrder() {
        Result result = validate(MAP_WORKFLOW, Map.of(), Map.of());

        assertEquals("", result.errors());
        assertEquals("[var_1] Property 'map_1.item' might not exist in the output of 'map/v1'", result.warnings());
    }

    @Test
    void mapItemTypeIsCheckedAgainstTheVariableOutput() {
        String workflow = MAP_WORKFLOW
            .replace("\"type\": \"var/v1/set\"", "\"type\": \"number/v1/set\"")
            .replace("{\"type\": \"STRING\", \"value\": \"${map_1.item}\"}", "{\"value\": \"${map_1.item}\"}");

        Result result = validate(workflow, Map.of(), Map.of("map_1", ITEM_VARIABLE_OUTPUT));

        assertEquals(
            "[var_1] Property 'map_1.item' in output of 'map/v1' is of type string, not number", result.errors());
        assertEquals("", result.warnings());
    }

    @Test
    void aTaskNestedDeeperInsideTheMapCanReferenceTheMapItem() {
        Result result = validate(DEEP_MAP_WORKFLOW, Map.of(), Map.of("map_1", ITEM_VARIABLE_OUTPUT));

        assertEquals("", result.errors());
        assertEquals("", result.warnings());
    }

    @Test
    void aTaskAfterTheMapCannotReferenceTheMapItem() {
        Result result = validate(MAP_THEN_LOGGER_WORKFLOW, Map.of(), Map.of("map_1", ITEM_VARIABLE_OUTPUT));

        assertEquals("", result.errors());
        assertEquals("[logger_1] Property 'map_1.item' might not exist in the output of 'map/v1'", result.warnings());
    }

    @Test
    void aTaskBeforeTheMapCannotReferenceTheMapItem() {
        Result result = validate(LOGGER_THEN_MAP_WORKFLOW, Map.of(), Map.of("map_1", ITEM_VARIABLE_OUTPUT));

        assertEquals("[logger_1] Wrong task order: You can't reference 'map_1.item' in logger_1", result.errors());
    }

    @Test
    void eachIterateeSingleTaskCanReferenceTheEachItem() {
        Result result = validate(EACH_WORKFLOW, Map.of(), Map.of("each_1", ITEM_VARIABLE_OUTPUT));

        assertEquals("", result.errors());
        assertEquals("", result.warnings());
    }

    @Test
    void subflowOutputResolvedPerNodeSatisfiesTheReference() {
        Result result = validate(SUBFLOW_WORKFLOW, Map.of("subflow_1", SUBFLOW_OUTPUT), Map.of());

        assertEquals("", result.errors());
        assertEquals("", result.warnings());
    }

    @Test
    void subflowOutputUnknownIsReportedAsAWarning() {
        Result result = validate(SUBFLOW_WORKFLOW, Map.of(), Map.of());

        assertEquals("", result.errors());
        assertEquals(
            "[logger_1] Property 'subflow_1.message' might not exist in the output of 'subflow/v1'",
            result.warnings());
    }

    private static Result validate(
        String workflow, Map<String, PropertyInfo> nodeOutputMap, Map<String, PropertyInfo> nodeVariableOutputMap) {

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        WorkflowValidator.TaskDefinitionProvider taskDefinitionProvider =
            (taskType, kind) -> TASK_DEFINITION_MAP.getOrDefault(taskType, List.of());
        WorkflowValidator.TaskOutputProvider taskOutputProvider = (taskType, kind, warningsBuilder) -> null;
        WorkflowValidator.ClusterTypesProvider clusterTypesProvider = taskType -> null;

        WorkflowValidator.validateWorkflow(
            workflow, taskDefinitionProvider, taskOutputProvider, clusterTypesProvider,
            WorkflowValidator.NO_RESOURCE_REFERENCE_PROVIDER, new HashMap<>(), new HashMap<>(), nodeOutputMap,
            nodeVariableOutputMap, new HashMap<>(), errors, warnings);

        return new Result(errors.toString(), warnings.toString());
    }

    private record Result(String errors, String warnings) {
    }
}
