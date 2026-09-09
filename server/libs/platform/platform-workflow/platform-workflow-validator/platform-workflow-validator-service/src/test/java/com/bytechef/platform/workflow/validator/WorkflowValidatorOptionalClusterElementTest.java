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
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class WorkflowValidatorOptionalClusterElementTest {

    private static final Map<String, List<PropertyInfo>> TASK_DEFINITION_MAP = Map.of(
        "aiAgent/v1/chat", List.of(
            new PropertyInfo("userPrompt", "STRING", null, false, true, null, null)),
        "anthropic/v1/model", List.of(
            new PropertyInfo("model", "STRING", null, true, true, null, null)));

    private static final WorkflowValidator.ClusterTypesProvider CLUSTER_TYPES_PROVIDER =
        new WorkflowValidator.ClusterTypesProvider() {

            @Override
            @Nullable
            public List<String> getClusterElementTypes(String taskType) {
                return "aiAgent/v1/chat".equals(taskType)
                    ? List.of("model", "chatMemory", "rag", "guardrails", "tools") : null;
            }

            @Override
            @Nullable
            public List<String> getRequiredClusterElementTypes(String taskType) {
                return "aiAgent/v1/chat".equals(taskType) ? List.of("model") : null;
            }
        };

    @Test
    void validateWorkflowReportsOnlyTheRequiredClusterElementAsMissing() {
        String workflow = agentWorkflow("{}");

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        validate(workflow, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("[aiAgent_1] Cluster element 'model' is missing from task aiAgent_1", warnings.toString());
    }

    @Test
    void validateWorkflowStillReportsAClusterElementTheComponentDoesNotDefine() {
        String workflow = agentWorkflow("""
            {
                "model": {
                    "label": "Anthropic",
                    "name": "anthropic_1",
                    "type": "anthropic/v1/model",
                    "parameters": {
                        "model": "claude-sonnet-4-6"
                    }
                },
                "widgets": {
                    "label": "Widget",
                    "name": "widget_1",
                    "type": "acme/v1/widget",
                    "parameters": {}
                }
            }
            """);

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        validate(workflow, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("[aiAgent_1] Cluster element 'widgets' are not defined in task aiAgent_1", warnings.toString());
    }

    private static void validate(String workflow, StringBuilder errors, StringBuilder warnings) {
        WorkflowValidator.TaskDefinitionProvider taskDefinitionProvider =
            (taskType, kind) -> TASK_DEFINITION_MAP.get(taskType);
        WorkflowValidator.TaskOutputProvider taskOutputProvider = (taskType, kind, warningsBuilder) -> null;

        WorkflowValidator.validateWorkflow(
            workflow, taskDefinitionProvider, taskOutputProvider, CLUSTER_TYPES_PROVIDER, new HashMap<>(),
            new HashMap<>(), new HashMap<>(), errors, warnings);
    }

    private static String agentWorkflow(String clusterElements) {
        return """
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
                        "clusterElements": {CLUSTER_ELEMENTS}
                    }
                ]
            }
            """.replace("{CLUSTER_ELEMENTS}", clusterElements);
    }
}
