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
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class WorkflowValidatorClusterElementTypeTest {

    private static final Map<String, List<PropertyInfo>> TASK_DEFINITION_MAP = Map.of(
        "aiAgent/v1/chat", List.of(
            new PropertyInfo("userPrompt", "STRING", null, false, true, null, null)),
        "anthropic/v1/model", List.of(
            new PropertyInfo("model", "STRING", null, true, true, null, null)),
        "mathHelper/v1/addition", List.of());

    private static final Map<String, Set<String>> SLOT_KEYS = Map.of(
        "anthropic/v1/model", Set.of("model"),
        "mathHelper/v1/addition", Set.of("tools"),
        "script/v1/javaScript", Set.of("tools", "processor"));

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

            @Override
            public Set<String> getClusterElementSlotKeys(String clusterElementType) {
                return SLOT_KEYS.getOrDefault(clusterElementType, Set.of());
            }
        };

    @Test
    void validateWorkflowAcceptsKnownClusterElementsInTheirSlots() {
        StringBuilder errors = new StringBuilder();

        validate(
            agentWorkflow(model("anthropic/v1/model") + ", " + tools("mathHelper/v1/addition", "script/v1/javaScript")),
            errors, new StringBuilder());

        assertEquals("", errors.toString());
    }

    @Test
    void validateWorkflowReportsAClusterElementTypeThatDoesNotExist() {
        StringBuilder errors = new StringBuilder();

        validate(
            agentWorkflow(model("anthropic/v1/claude-3-5-sonnet") + ", " + tools("math/v1")), errors,
            new StringBuilder());

        assertEquals(
            """
                [aiAgent_1] Cluster element 'model_1' has unknown type anthropic/v1/claude-3-5-sonnet; the type must be \
                componentName/vN/clusterElementName of a defined cluster element
                [aiAgent_1] Cluster element 'tool_1' has unknown type math/v1; the type must be \
                componentName/vN/clusterElementName of a defined cluster element""",
            errors.toString());
    }

    @Test
    void validateWorkflowReportsAClusterElementPlacedInTheWrongSlot() {
        StringBuilder errors = new StringBuilder();

        validate(
            agentWorkflow(model("anthropic/v1/model") + ", " + tools("anthropic/v1/model")), errors,
            new StringBuilder());

        assertEquals(
            "[aiAgent_1] Cluster element 'tool_1' of type anthropic/v1/model cannot be placed under 'tools'; it " +
                "belongs under model\n[aiAgent_1] Missing required property: tool_1.model",
            errors.toString());
    }

    @Test
    void validateWorkflowReportsAClusterElementUnderASlotTheTaskDoesNotHave() {
        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        validate(
            agentWorkflow(
                model("anthropic/v1/model") + ", " + """
                    "vectorStore": {"label": "Store", "name": "vectorStore_1", "type": "mathHelper/v1/addition", \
                    "parameters": {}}"""),
            errors, warnings);

        assertEquals(
            "[aiAgent_1] Cluster element 'vectorStore' is not a slot of aiAgent/v1/chat; its slots are " +
                "chatMemory, guardrails, model, rag, tools\n[aiAgent_1] Cluster element 'vectorStore_1' of type " +
                "mathHelper/v1/addition cannot be placed under 'vectorStore'; it belongs under tools",
            errors.toString());
        assertEquals("", warnings.toString());
    }

    private static void validate(String workflow, StringBuilder errors, StringBuilder warnings) {
        WorkflowValidator.TaskDefinitionProvider taskDefinitionProvider =
            (taskType, kind) -> TASK_DEFINITION_MAP.get(taskType);
        WorkflowValidator.TaskOutputProvider taskOutputProvider = (taskType, kind, warningsBuilder) -> null;

        WorkflowValidator.validateWorkflow(
            workflow, taskDefinitionProvider, taskOutputProvider, CLUSTER_TYPES_PROVIDER, new HashMap<>(),
            new HashMap<>(), new HashMap<>(), errors, warnings);
    }

    private static String model(String type) {
        return """
            "model": {"label": "Model", "name": "model_1", "type": "%s", "parameters": {"model": "claude"}}"""
            .formatted(type);
    }

    private static String tools(String... types) {
        StringBuilder tools = new StringBuilder();

        for (int index = 0; index < types.length; index++) {
            if (index > 0) {
                tools.append(", ");
            }

            tools.append("""
                {"label": "Tool", "name": "tool_%d", "type": "%s", "parameters": {}}""".formatted(index + 1,
                types[index]));
        }

        return "\"tools\": [" + tools + "]";
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
            """.replace("{CLUSTER_ELEMENTS}", "{" + clusterElements + "}");
    }
}
