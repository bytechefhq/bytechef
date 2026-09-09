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
import static org.junit.jupiter.api.Assertions.assertFalse;

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
class ResourceReferenceValidatorTest {

    private static final PropertyInfo TABLE_PROPERTY =
        new PropertyInfo("table", "STRING", null, true, true, null, null, null, "DATA_TABLE");
    private static final PropertyInfo NAME_PROPERTY =
        new PropertyInfo("name", "STRING", null, false, true, null, null, null, null);

    @BeforeAll
    static void beforeAll() {
        JsonUtils.setObjectMapper(JsonMapper.builder()
            .build());
    }

    @Test
    void resolvedReferenceProducesNothing() {
        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        ResourceReferenceValidator.validate(
            JsonUtils.readTree("{\"table\":\"conversations\"}"), List.of(TABLE_PROPERTY), "",
            (resourceType, reference) -> null, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void missingReferenceProducesError() {
        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        ResourceReferenceValidator.validate(
            JsonUtils.readTree("{\"table\":\"conversations\"}"), List.of(TABLE_PROPERTY), "",
            (resourceType, reference) -> "Data table '" + reference + "' does not exist in this environment",
            errors, warnings);

        assertEquals(
            "Resource referenced by property 'table' is not available: Data table 'conversations' does not exist " +
                "in this environment",
            errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void expressionValueIsSkipped() {
        StringBuilder errors = new StringBuilder();

        ResourceReferenceValidator.validate(
            JsonUtils.readTree("{\"table\":\"${trigger_1.tableName}\"}"), List.of(TABLE_PROPERTY), "",
            (resourceType, reference) -> "must not be called", errors, new StringBuilder());

        assertEquals("", errors.toString());
    }

    @Test
    void propertyWithoutResourceTypeIsSkipped() {
        StringBuilder errors = new StringBuilder();

        ResourceReferenceValidator.validate(
            JsonUtils.readTree("{\"name\":\"x\"}"), List.of(NAME_PROPERTY), "",
            (resourceType, reference) -> "must not be called", errors, new StringBuilder());

        assertEquals("", errors.toString());
    }

    @Test
    void resolverExceptionProducesWarning() {
        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        ResourceReferenceValidator.validate(
            JsonUtils.readTree("{\"table\":\"conversations\"}"), List.of(TABLE_PROPERTY), "",
            (resourceType, reference) -> {
                throw new IllegalStateException("database unavailable");
            }, errors, warnings);

        assertEquals("", errors.toString());
        assertEquals(
            "Could not verify the resource referenced by property 'table': database unavailable",
            warnings.toString());
    }

    @Test
    void nestedObjectPropertyIsChecked() {
        PropertyInfo settings = new PropertyInfo(
            "settings", "OBJECT", null, false, true, null, null, List.of(TABLE_PROPERTY), null);
        StringBuilder errors = new StringBuilder();

        ResourceReferenceValidator.validate(
            JsonUtils.readTree("{\"settings\":{\"table\":\"orders\"}}"), List.of(settings), "",
            (resourceType, reference) -> "missing", errors, new StringBuilder());

        assertEquals("Resource referenced by property 'settings.table' is not available: missing", errors.toString());
    }

    @Test
    void workflowValidationPrefixesResourceErrorWithTaskName() {
        String workflow = """
            {
                "label": "Test Workflow",
                "description": "workflowDescription",
                "triggers": [
                    {"label": "Manual", "name": "trigger_1", "type": "manual/v1/manual", "parameters": {}}
                ],
                "tasks": [
                    {
                        "label": "Update Record",
                        "name": "dataTable_1",
                        "type": "dataTable/v1/updateRecord",
                        "parameters": {"table": "conversations"}
                    }
                ]
            }
            """;

        Map<String, List<PropertyInfo>> taskDefinitionMap = Map.of(
            "manual/v1/manual", List.of(),
            "dataTable/v1/updateRecord", List.of(TABLE_PROPERTY));
        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        WorkflowValidator.validateWorkflow(
            workflow, (taskType, kind) -> taskDefinitionMap.get(taskType), (taskType, kind, w) -> null,
            taskType -> null,
            (resourceType, reference) -> "DATA_TABLE".equals(resourceType) ? "Data table '" + reference +
                "' does not exist in this environment" : null,
            new HashMap<>(), new HashMap<>(), Map.of(), new HashMap<>(), errors, warnings);

        assertEquals(
            "[dataTable_1] Resource referenced by property 'table' is not available: Data table 'conversations' " +
                "does not exist in this environment",
            errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateWorkflowTasksClusterElementResourceReferenceMissing() {
        String tasksJson = """
            [
                {
                    "clusterElements": {
                        "tool": {
                            "label": "Knowledge Base Search",
                            "name": "knowledgeBaseSearchTool_1",
                            "parameters": {"knowledgeBase": "42"},
                            "type": "knowledgeBase/v1/searchTool"
                        }
                    },
                    "name": "aiAgent_1",
                    "label": "AI Agent",
                    "parameters": {"userPrompt": "hi"},
                    "type": "aiAgent/v1/chat"
                }
            ]
            """;

        Map<String, List<PropertyInfo>> taskDefinitionMap = Map.of(
            "aiAgent/v1/chat", List.of(
                new PropertyInfo("userPrompt", "STRING", null, true, true, null, null)),
            "knowledgeBase/v1/searchTool", List.of(
                new PropertyInfo("knowledgeBase", "STRING", null, true, true, null, null, null, "KNOWLEDGE_BASE")));

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        JsonNode tasksJsonNode = JsonUtils.readTree(tasksJson);
        List<JsonNode> taskJsonNodes = new ArrayList<>();

        for (JsonNode taskJsonNode : tasksJsonNode) {
            taskJsonNodes.add(taskJsonNode);
        }

        ValidationContext context = ValidationContext.of(
            taskJsonNodes, List.of(), taskDefinitionMap, Map.of(), Map.of(), Map.of(),
            (resourceType, reference) -> "KNOWLEDGE_BASE".equals(resourceType)
                ? "Knowledge base '" + reference + "' does not exist in this environment" : null,
            errors, warnings);

        TaskValidator.validateAllTasks(context);

        assertEquals(
            "[aiAgent_1] Resource referenced by property 'knowledgeBase' is not available: Knowledge base '42' " +
                "does not exist in this environment",
            errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void validateWorkflowReportsNestedTaskResourceReferenceOnceAgainstNestedTask() {
        String workflow = """
            {
                "label": "Test Workflow",
                "description": "workflowDescription",
                "triggers": [
                    {"label": "Manual", "name": "trigger_1", "type": "manual/v1/manual", "parameters": {}}
                ],
                "tasks": [
                    {
                        "label": "Condition",
                        "name": "condition_1",
                        "type": "condition/v1",
                        "parameters": {
                            "caseTrue": [
                                {
                                    "label": "Update Record",
                                    "name": "dataTable_1",
                                    "type": "dataTable/v1/updateRecord",
                                    "parameters": {"table": "conversations"}
                                }
                            ],
                            "caseFalse": []
                        }
                    }
                ]
            }
            """;

        Map<String, List<PropertyInfo>> taskDefinitionMap = Map.of(
            "manual/v1/manual", List.of(),
            "condition/v1", List.of(
                new PropertyInfo("caseTrue", "ARRAY", null, false, true, null, List.of(
                    new PropertyInfo(null, "TASK", null, false, false, null, null))),
                new PropertyInfo("caseFalse", "ARRAY", null, false, true, null, List.of(
                    new PropertyInfo(null, "TASK", null, false, false, null, null)))),
            "dataTable/v1/updateRecord", List.of(TABLE_PROPERTY));

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        WorkflowValidator.validateWorkflow(
            workflow, (taskType, kind) -> taskDefinitionMap.get(taskType), (taskType, kind, w) -> null,
            taskType -> null,
            (resourceType, reference) -> "DATA_TABLE".equals(resourceType) ? "Data table '" + reference +
                "' does not exist in this environment" : null,
            new HashMap<>(), new HashMap<>(), Map.of(), new HashMap<>(), errors, warnings);

        String errorsString = errors.toString();

        assertEquals(
            "[dataTable_1] Resource referenced by property 'table' is not available: Data table 'conversations' " +
                "does not exist in this environment",
            errorsString);

        int occurrences = errorsString.split("Resource referenced by property 'table'", -1).length - 1;

        assertEquals(1, occurrences);
        assertFalse(errorsString.contains("[condition_1]"));
        assertEquals("", warnings.toString());
    }
}
