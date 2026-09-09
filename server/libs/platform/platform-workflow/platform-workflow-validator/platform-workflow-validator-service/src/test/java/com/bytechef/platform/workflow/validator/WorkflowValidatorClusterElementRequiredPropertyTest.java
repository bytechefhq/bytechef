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
class WorkflowValidatorClusterElementRequiredPropertyTest {

    private static final Map<String, List<PropertyInfo>> TASK_DEFINITION_MAP = Map.of(
        "aiAgent/v1/chat", List.of(
            new PropertyInfo("userPrompt", "STRING", null, false, true, null, null)),
        "googleMail/v1/getEmail", List.of(
            new PropertyInfo("id", "STRING", null, true, true, null, null)),
        "anthropic/v1/model", List.of(
            new PropertyInfo("model", "STRING", null, true, true, null, null),
            new PropertyInfo("maxTokens", "INTEGER", null, true, true, null, null)));

    @Test
    void validateWorkflowReportsAMissingRequiredPropertyOfATool() {
        String workflow = """
            {
                "label": "Agent",
                "description": "",
                "inputs": [],
                "triggers": [],
                "tasks": [
                    {
                        "label": "AI Agent",
                        "name": "aiAgent_1",
                        "type": "aiAgent/v1/chat",
                        "parameters": {
                            "userPrompt": "hello"
                        },
                        "clusterElements": {
                            "tools": [
                                {
                                    "label": "Gmail",
                                    "name": "googleMail_1",
                                    "type": "googleMail/v1/getEmail",
                                    "parameters": {}
                                }
                            ]
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
        WorkflowValidator.ClusterTypesProvider clusterTypesProvider =
            taskType -> "aiAgent/v1/chat".equals(taskType) ? List.of("tools") : null;

        WorkflowValidator.validateWorkflow(
            workflow, taskDefinitionProvider, taskOutputProvider, clusterTypesProvider, new HashMap<>(),
            new HashMap<>(), new HashMap<>(), errors, warnings);

        assertEquals("[aiAgent_1] Missing required property: googleMail_1.id", errors.toString());
    }

    @Test
    void validateWorkflowReportsAMissingRequiredPropertyOfAModel() {
        String workflow = """
            {
                "label": "Agent",
                "description": "",
                "inputs": [],
                "triggers": [],
                "tasks": [
                    {
                        "label": "AI Agent",
                        "name": "aiAgent_1",
                        "type": "aiAgent/v1/chat",
                        "parameters": {
                            "userPrompt": "hello"
                        },
                        "clusterElements": {
                            "model": {
                                "label": "Anthropic",
                                "name": "anthropic_1",
                                "type": "anthropic/v1/model",
                                "parameters": {
                                    "model": "claude-sonnet-4-6"
                                }
                            }
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
        WorkflowValidator.ClusterTypesProvider clusterTypesProvider =
            taskType -> "aiAgent/v1/chat".equals(taskType) ? List.of("model") : null;

        WorkflowValidator.validateWorkflow(
            workflow, taskDefinitionProvider, taskOutputProvider, clusterTypesProvider, new HashMap<>(),
            new HashMap<>(), new HashMap<>(), errors, warnings);

        assertEquals("[aiAgent_1] Missing required property: anthropic_1.maxTokens", errors.toString());
    }
}
