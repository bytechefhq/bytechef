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
class WorkflowValidatorDynamicPropertiesTest {

    private static final Map<String, List<PropertyInfo>> TASK_DEFINITION_MAP = Map.of(
        "httpClient/v1/get", List.of(
            new PropertyInfo("uri", "STRING", null, false, true, null, null),
            new PropertyInfo(
                "queryParameters", "OBJECT", null, false, true, null,
                List.of(new PropertyInfo("locale", "STRING", null, false, true, null, null))),
            new PropertyInfo("responseType", "STRING", null, false, true, null, null),
            new PropertyInfo(
                "responseContentType", "STRING", null, false, true, "responseType == 'BINARY'", null)));

    @Test
    void validateWorkflowDoesNotWarnAboutPropertiesRecordedAsDynamic() {
        String workflow = """
            {
                "label": "Test Workflow",
                "description": "",
                "inputs": [],
                "triggers": [],
                "tasks": [
                    {
                        "label": "Get rate",
                        "name": "httpClient_1",
                        "type": "httpClient/v1/get",
                        "metadata": {
                            "ui": {
                                "dynamicPropertyTypes": {
                                    "queryParameters.valuta": "ARRAY"
                                }
                            }
                        },
                        "parameters": {
                            "uri": "https://example.com",
                            "queryParameters": {
                                "valuta": ["USD"]
                            }
                        }
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
    void validateWorkflowWarnsAboutAPropertyThatWasNotRecordedAsDynamic() {
        String workflow = """
            {
                "label": "Test Workflow",
                "description": "",
                "inputs": [],
                "triggers": [],
                "tasks": [
                    {
                        "label": "Get rate",
                        "name": "httpClient_1",
                        "type": "httpClient/v1/get",
                        "metadata": {
                            "ui": {
                                "dynamicPropertyTypes": {
                                    "queryParameters.valuta": "ARRAY"
                                }
                            }
                        },
                        "parameters": {
                            "uri": "https://example.com",
                            "queryParameters": {
                                "valuta": ["USD"],
                                "datum": "2024-01-01"
                            }
                        }
                    }
                ]
            }
            """;

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        validateWorkflow(workflow, errors, warnings);

        assertEquals(
            "[httpClient_1] Property 'queryParameters.datum' is not defined in task definition",
            warnings.toString());
    }

    @Test
    void validateWorkflowDoesNotWarnAboutADefinedPropertyItsDisplayConditionCurrentlyHides() {
        String workflow = """
            {
                "label": "Test Workflow",
                "description": "",
                "inputs": [],
                "triggers": [],
                "tasks": [
                    {
                        "label": "Get",
                        "name": "httpClient_2",
                        "type": "httpClient/v1/get",
                        "parameters": {
                            "uri": "https://example.com",
                            "responseType": "JSON",
                            "responseContentType": "application/octet-stream"
                        }
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
