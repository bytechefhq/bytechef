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
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.platform.workflow.validator.model.PropertyInfo;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author Ivica Cardic
 */
class WorkflowValidatorConfigAwareOutputTest {

    private static final String WORKFLOW = """
        {
            "label": "Summarize email",
            "description": "Summarize a new Gmail email and post to Slack",
            "triggers": [
                {
                    "label": "New Email",
                    "name": "newEmail_1",
                    "type": "googleMail/v1/newEmail",
                    "parameters": {
                        "format": "FULL"
                    }
                }
            ],
            "tasks": [
                {
                    "label": "Ask",
                    "name": "openAi_1",
                    "type": "openAi/v1/ask",
                    "parameters": {
                        "userPrompt": "${newEmail_1.bodyPlain}"
                    }
                }
            ]
        }
        """;

    private static final String ARRAY_VARIABLE_WORKFLOW = """
        {
            "label": "Array variable",
            "description": "",
            "triggers": [
                {
                    "label": "Manual",
                    "name": "trigger_1",
                    "type": "manual/v1/manual"
                }
            ],
            "tasks": [
                {
                    "label": "Var",
                    "name": "var_1",
                    "type": "var/v1/set",
                    "parameters": {
                        "type": "ARRAY",
                        "value": [
                            {
                                "wqe": "qwe",
                                "wqeqwe": "qweq"
                            }
                        ]
                    }
                },
                {
                    "label": "Logger",
                    "name": "logger_1",
                    "type": "logger/v1/info",
                    "parameters": {
                        "text": "TEXT_PLACEHOLDER"
                    }
                }
            ]
        }
        """;

    private static final PropertyInfo ARRAY_VARIABLE_OUTPUT = new PropertyInfo(
        null, "ARRAY", null, false, false, null, List.of(
            new PropertyInfo(null, "OBJECT", null, false, false, null, List.of(
                new PropertyInfo("wqe", "STRING", null, false, false, null, null),
                new PropertyInfo("wqeqwe", "STRING", null, false, false, null, null)))));

    private static final Map<String, List<PropertyInfo>> TASK_DEFINITION_MAP = Map.of(
        "googleMail/v1/newEmail", List.of(
            new PropertyInfo("format", "STRING", null, false, false, null, null)),
        "openAi/v1/ask", List.of(
            new PropertyInfo("userPrompt", "STRING", null, true, true, null, null)),
        "manual/v1/manual", List.of(),
        "var/v1/set", List.of(
            new PropertyInfo("type", "STRING", null, false, true, null, null),
            new PropertyInfo("value", "ARRAY", null, true, true, "type == 'ARRAY'", null)),
        "logger/v1/info", List.of(
            new PropertyInfo("text", "STRING", null, true, true, null, null)));

    private static final WorkflowValidator.TaskDefinitionProvider TASK_DEFINITION_PROVIDER =
        (taskType, kind) -> TASK_DEFINITION_MAP.get(taskType);
    private static final WorkflowValidator.TaskOutputProvider EMPTY_TASK_OUTPUT_PROVIDER =
        (taskType, kind, warnings) -> null;
    private static final WorkflowValidator.ClusterTypesProvider EMPTY_CLUSTER_TYPES_PROVIDER = taskType -> null;

    @BeforeAll
    static void beforeAll() {
        JsonUtils.setObjectMapper(
            JsonMapper.builder()
                .build());
    }

    @Test
    void testMissingPropertyInConfigResolvedOutputIsError() {
        // format=FULL output: from/subject present, bodyPlain absent.
        PropertyInfo fullOutput = output(
            new PropertyInfo("from", "STRING", null, false, false, null, null),
            new PropertyInfo("subject", "STRING", null, false, false, null, null));

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        WorkflowValidator.validateWorkflow(
            WORKFLOW, TASK_DEFINITION_PROVIDER, EMPTY_TASK_OUTPUT_PROVIDER, EMPTY_CLUSTER_TYPES_PROVIDER,
            new HashMap<>(), new HashMap<>(), Map.of("newEmail_1", fullOutput), new HashMap<>(), errors, warnings);

        assertTrue(
            errors.toString()
                .contains("Property 'newEmail_1.bodyPlain' does not exist in the output of 'googleMail/v1/newEmail'"),
            "Expected a config-aware missing-property error, got: " + errors);
    }

    @Test
    void testPresentPropertyInConfigResolvedOutputIsValid() {
        // A simplified output that does expose bodyPlain.
        PropertyInfo simpleOutput = output(
            new PropertyInfo("from", "STRING", null, false, false, null, null),
            new PropertyInfo("subject", "STRING", null, false, false, null, null),
            new PropertyInfo("bodyPlain", "STRING", null, false, false, null, null));

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        WorkflowValidator.validateWorkflow(
            WORKFLOW, TASK_DEFINITION_PROVIDER, EMPTY_TASK_OUTPUT_PROVIDER, EMPTY_CLUSTER_TYPES_PROVIDER,
            new HashMap<>(), new HashMap<>(), Map.of("newEmail_1", simpleOutput), new HashMap<>(), errors, warnings);

        assertEquals("", errors.toString());
    }

    @Test
    void testIndexedReferenceToArrayOutputIsValid() {
        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        validateArrayVariableWorkflow("${var_1[0]}", errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void testIndexedPropertyReferenceToArrayOutputIsValid() {
        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        validateArrayVariableWorkflow("${var_1[0].wqe}", errors, warnings);

        assertEquals("", errors.toString());
        assertEquals("", warnings.toString());
    }

    @Test
    void testMissingIndexedPropertyInArrayOutputIsError() {
        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        validateArrayVariableWorkflow("${var_1[0].missing}", errors, warnings);

        assertEquals(
            "[logger_1] Property 'var_1[0].missing' does not exist in the output of 'var/v1/set'", errors.toString());
        assertEquals("", warnings.toString());
    }

    private static void validateArrayVariableWorkflow(String text, StringBuilder errors, StringBuilder warnings) {
        WorkflowValidator.validateWorkflow(
            ARRAY_VARIABLE_WORKFLOW.replace("TEXT_PLACEHOLDER", text), TASK_DEFINITION_PROVIDER,
            EMPTY_TASK_OUTPUT_PROVIDER, EMPTY_CLUSTER_TYPES_PROVIDER, new HashMap<>(), new HashMap<>(),
            Map.of("var_1", ARRAY_VARIABLE_OUTPUT), new HashMap<>(), errors, warnings);
    }

    private static PropertyInfo output(PropertyInfo... nestedProperties) {
        return new PropertyInfo("output", "OBJECT", null, false, false, null, null, List.of(nestedProperties));
    }
}
