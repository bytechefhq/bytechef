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
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import tools.jackson.databind.JsonNode;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class WorkflowValidatorInvalidExpressionTest {

    private static final Map<String, List<PropertyInfo>> TASK_DEFINITION_MAP = Map.of(
        "var/v1/set", List.of(
            new PropertyInfo("type", "STRING", null, false, true, null, null),
            new PropertyInfo("value", "OBJECT", null, false, true, null, null)),
        "loop/v1", List.of(
            new PropertyInfo("items", "ARRAY", null, false, true, null, List.of()),
            new PropertyInfo("loopForever", "BOOLEAN", null, false, true, null, null),
            new PropertyInfo("iteratee", "ARRAY", null, false, true, null, List.of(
                new PropertyInfo(null, "TASK", null, false, true, null, null)))),
        "logger/v1/info", List.of(
            new PropertyInfo("text", "STRING", null, false, true, null, null)));

    private static final Map<String, PropertyInfo> NODE_OUTPUT_MAP = Map.of(
        "config", new PropertyInfo(
            null, "OBJECT", null, false, false, null, null, List.of(
                new PropertyInfo("linesPerRow", "INTEGER", null, false, false, null, null),
                new PropertyInfo("names", "ARRAY", null, false, false, null, List.of(
                    new PropertyInfo(null, "STRING", null, false, false, null, null)))),
            null));

    @Test
    void validateWorkflowTasksReportsADataPillNestedInsideAnotherDataPill() {
        StringBuilder errors = new StringBuilder();

        validate(loopItems("${range(1, ${config.linesPerRow)}"), errors, new StringBuilder());

        assertEquals(
            "[loop_lines] Property 'items' has an invalid expression: ${range(1, ${config.linesPerRow)}",
            errors.toString());
    }

    @Test
    void validateWorkflowTasksReportsAFunctionCallInsideADataPill() {
        StringBuilder errors = new StringBuilder();

        validate(loopItems("${range(1, config.linesPerRow)}"), errors, new StringBuilder());

        assertEquals(
            "[loop_lines] Property 'items' has an invalid expression: ${range(1, config.linesPerRow)}",
            errors.toString());
    }

    @Test
    void validateWorkflowTasksReportsAnUnclosedDataPillInsideAFormula() {
        StringBuilder errors = new StringBuilder();

        validate(loopItems("=range(1, ${config.linesPerRow)"), errors, new StringBuilder());

        assertEquals(
            "[loop_lines] Property 'items' has an invalid expression: =range(1, ${config.linesPerRow)",
            errors.toString());
    }

    @Test
    void validateWorkflowTasksAcceptsPlainAndIndexedDataPillsInText() {
        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        validate(
            """
                [
                    {
                        "label": "Run configuration",
                        "name": "config",
                        "type": "var/v1/set",
                        "parameters": {"type": "OBJECT", "value": {"linesPerRow": 5}}
                    },
                    {
                        "label": "Log",
                        "name": "logger_1",
                        "type": "logger/v1/info",
                        "parameters": {
                            "text": "Rows: ${config.linesPerRow}, first: ${config.names[0]}"
                        }
                    }
                ]
                """,
            errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("", warnings.toString());
    }

    private static String loopItems(String items) {
        return """
            [
                {
                    "label": "Run configuration",
                    "name": "config",
                    "type": "var/v1/set",
                    "parameters": {"type": "OBJECT", "value": {"linesPerRow": 5}}
                },
                {
                    "label": "Loop over lines",
                    "name": "loop_lines",
                    "type": "loop/v1",
                    "parameters": {"items": "%s", "loopForever": false, "iteratee": []}
                }
            ]
            """.formatted(items.replace("\\", "\\\\"));
    }

    private static void validate(String tasksJson, StringBuilder errors, StringBuilder warnings) {
        List<JsonNode> taskJsonNodes = new ArrayList<>();

        for (JsonNode taskJsonNode : JsonUtils.readTree(tasksJson)) {
            taskJsonNodes.add(taskJsonNode);
        }

        WorkflowValidator.validateWorkflowTasks(
            taskJsonNodes, TASK_DEFINITION_MAP, Map.of(), NODE_OUTPUT_MAP, Map.of(), errors, warnings);
    }
}
