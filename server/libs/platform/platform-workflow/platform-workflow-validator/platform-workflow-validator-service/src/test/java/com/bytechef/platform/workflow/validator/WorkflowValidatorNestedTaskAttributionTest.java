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
class WorkflowValidatorNestedTaskAttributionTest {

    private static final Map<String, List<PropertyInfo>> TASK_DEFINITION_MAP = Map.of(
        "condition/v1", List.of(
            new PropertyInfo("rawExpression", "BOOLEAN", null, false, true, null, null),
            new PropertyInfo("expression", "STRING", null, false, true, null, null),
            new PropertyInfo(
                "caseTrue", "ARRAY", null, false, false, null,
                List.of(new PropertyInfo(null, "TASK", null, false, false, null, null))),
            new PropertyInfo(
                "caseFalse", "ARRAY", null, false, false, null,
                List.of(new PropertyInfo(null, "TASK", null, false, false, null, null)))),
        "dataStorage/v1/setValue", List.of(
            new PropertyInfo("key", "STRING", null, true, true, null, null),
            new PropertyInfo("value", "BOOLEAN", null, true, true, null, null)));

    @Test
    void validateWorkflowReportsANestedTaskProblemOnceAgainstTheNestedTask() {
        String workflow = """
            {
                "label": "Nested",
                "description": "",
                "inputs": [],
                "triggers": [],
                "tasks": [
                    {
                        "label": "Outer",
                        "name": "condition_1",
                        "type": "condition/v1",
                        "parameters": {
                            "rawExpression": true,
                            "expression": "true",
                            "caseTrue": [
                                {
                                    "label": "Inner",
                                    "name": "condition_2",
                                    "type": "condition/v1",
                                    "parameters": {
                                        "rawExpression": true,
                                        "expression": "true",
                                        "caseTrue": [
                                            {
                                                "label": "Set",
                                                "name": "dataStorage_1",
                                                "type": "dataStorage/v1/setValue",
                                                "parameters": {
                                                    "key": "flag",
                                                    "value": "not a boolean"
                                                }
                                            }
                                        ],
                                        "caseFalse": []
                                    }
                                }
                            ],
                            "caseFalse": []
                        }
                    }
                ]
            }
            """;

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        WorkflowValidator.TaskDefinitionProvider taskDefinitionProvider =
            (taskType, kind) -> TASK_DEFINITION_MAP.get(taskType);
        WorkflowValidator.TaskOutputProvider taskOutputProvider = (taskType, kind, warningsBuilder) -> null;
        WorkflowValidator.ClusterTypesProvider clusterTypesProvider = taskType -> null;

        WorkflowValidator.validateWorkflow(
            workflow, taskDefinitionProvider, taskOutputProvider, clusterTypesProvider, new HashMap<>(),
            new HashMap<>(), new HashMap<>(), errors, warnings);

        assertEquals(
            "[dataStorage_1] Property 'value' has incorrect type. Expected: boolean, but got: string",
            errors.toString());
        assertEquals("", warnings.toString());
    }
}
