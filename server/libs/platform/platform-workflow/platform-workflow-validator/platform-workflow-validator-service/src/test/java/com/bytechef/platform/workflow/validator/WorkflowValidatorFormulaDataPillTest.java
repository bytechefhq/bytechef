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
class WorkflowValidatorFormulaDataPillTest {

    private static final Map<String, List<PropertyInfo>> TASK_DEFINITION_MAP = Map.of(
        "var/v1/set", List.of(
            new PropertyInfo("type", "STRING", null, false, true, null, null),
            new PropertyInfo("value", "OBJECT", null, false, true, null, null)),
        "loop/v1", List.of(
            new PropertyInfo("items", "ARRAY", null, false, true, null, List.of()),
            new PropertyInfo("loopForever", "BOOLEAN", null, false, true, null, null),
            new PropertyInfo("iteratee", "ARRAY", null, false, true, null, List.of(
                new PropertyInfo(null, "TASK", null, false, true, null, null)))),
        "number/v1/set", List.of(
            new PropertyInfo("value", "NUMBER", null, false, true, null, null)));

    private static final Map<String, PropertyInfo> NODE_OUTPUT_MAP = Map.of(
        "config", new PropertyInfo(
            null, "OBJECT", null, false, false, null, null, List.of(
                new PropertyInfo("batches", "INTEGER", null, false, false, null, null),
                new PropertyInfo("batchSize", "INTEGER", null, false, false, null, null)),
            null));

    @Test
    void validateWorkflowTasksAcceptsDataPillUsedAsFormulaOperand() {
        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        validate(
            """
                [
                    {
                        "label": "Run configuration",
                        "name": "config",
                        "type": "var/v1/set",
                        "parameters": {"type": "OBJECT", "value": {"batches": 150, "batchSize": 50}}
                    },
                    {
                        "label": "Loop over batches",
                        "name": "loop_batches",
                        "type": "loop/v1",
                        "parameters": {
                            "items": "=range(1, ${config.batches})",
                            "loopForever": false,
                            "iteratee": []
                        }
                    }
                ]
                """,
            errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateWorkflowTasksStillReportsTypeMismatchForSingleDataPillFormula() {
        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        validate(
            """
                [
                    {
                        "label": "Run configuration",
                        "name": "config",
                        "type": "var/v1/set",
                        "parameters": {"type": "OBJECT", "value": {"batches": 150, "batchSize": 50}}
                    },
                    {
                        "label": "Loop over batches",
                        "name": "loop_batches",
                        "type": "loop/v1",
                        "parameters": {
                            "items": "=${config.batches}",
                            "loopForever": false,
                            "iteratee": []
                        }
                    }
                ]
                """,
            errors, warnings);

        assertEquals(
            "[loop_batches] Property 'config.batches' in output of 'var/v1/set' is of type integer, not array",
            errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateWorkflowTasksAcceptsLoopItemUsedAsFormulaOperand() {
        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        validate(
            """
                [
                    {
                        "label": "Loop over names",
                        "name": "loop_names",
                        "type": "loop/v1",
                        "parameters": {
                            "items": ["Marko", "Ivica"],
                            "loopForever": false,
                            "iteratee": [
                                {
                                    "label": "Name length",
                                    "name": "length",
                                    "type": "number/v1/set",
                                    "parameters": {"value": "=length(${loop_names.item})"}
                                }
                            ]
                        }
                    }
                ]
                """,
            errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("", warnings.toString());
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
