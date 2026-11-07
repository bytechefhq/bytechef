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
class GraphReferenceUtilsTest {

    private static final PropertyInfo LOGGER_OUTPUT = new PropertyInfo(
        null, "OBJECT", null, false, false, null, null, List.of(
            new PropertyInfo("text", "STRING", null, false, false, null, null)),
        null);

    private static final Map<String, List<PropertyInfo>> TASK_DEFINITION_MAP = Map.of(
        "manual/v1/manual", List.of(),
        "logger/v1/info", List.of(
            new PropertyInfo("text", "STRING", null, false, true, null, null)),
        "condition/v1", List.of(
            new PropertyInfo("rawExpression", "BOOLEAN", null, false, false, null, null),
            new PropertyInfo("expression", "STRING", null, false, true, null, null),
            new PropertyInfo("caseTrue", "ARRAY", null, false, false, null, List.of(
                new PropertyInfo(null, "TASK", null, false, false, null, null))),
            new PropertyInfo("caseFalse", "ARRAY", null, false, false, null, List.of(
                new PropertyInfo(null, "TASK", null, false, false, null, null)))),
        "graph/v1", List.of(
            new PropertyInfo("startNode", "STRING", null, false, true, null, null),
            new PropertyInfo("transitions", "ARRAY", null, false, false, null, List.of(
                new PropertyInfo(null, "OBJECT", null, false, false, null, null))),
            new PropertyInfo("nodes", "ARRAY", null, false, false, null, List.of(
                new PropertyInfo(null, "TASK", null, false, false, null, null)))));

    private static final String NESTED_MEMBER_WORKFLOW = """
        {
            "label": "workflow1",
            "description": "",
            "triggers": [
                {"label": "Manual", "name": "trigger_1", "type": "manual/v1/manual"}
            ],
            "tasks": [
                {
                    "label": "Graph",
                    "name": "graph_1",
                    "type": "graph/v1",
                    "parameters": {
                        "startNode": "logger_2",
                        "transitions": TRANSITIONS,
                        "nodes": [
                            {
                                "label": "Condition",
                                "name": "condition_1",
                                "type": "condition/v1",
                                "parameters": {
                                    "rawExpression": true,
                                    "expression": "=true",
                                    "caseTrue": [
                                        {
                                            "label": "Logger",
                                            "name": "logger_1",
                                            "type": "logger/v1/info",
                                            "parameters": {"text": "NESTED_TEXT"}
                                        }
                                    ],
                                    "caseFalse": []
                                }
                            },
                            {
                                "label": "Logger",
                                "name": "logger_2",
                                "type": "logger/v1/info",
                                "parameters": {"text": "hello"}
                            }
                        ]
                    }
                }
            ]
        }
        """;

    private static final String GRAPH_WORKFLOW = """
        {
            "label": "workflow1",
            "description": "",
            "triggers": [
                {"label": "Manual", "name": "trigger_1", "type": "manual/v1/manual"}
            ],
            "tasks": [
                {
                    "label": "Graph",
                    "name": "graph_1",
                    "type": "graph/v1",
                    "parameters": {
                        "startNode": "START_NODE",
                        "transitions": TRANSITIONS,
                        "nodes": [
                            {
                                "label": "Logger",
                                "name": "logger_1",
                                "type": "logger/v1/info",
                                "parameters": {"text": "FIRST_TEXT"}
                            },
                            {
                                "label": "Logger",
                                "name": "logger_2",
                                "type": "logger/v1/info",
                                "parameters": {"text": "SECOND_TEXT"}
                            }
                        ]
                    }
                }
            ]
        }
        """;

    @Test
    void graphMemberCanReferenceAPrecedingMemberOfTheSameGraph() {
        Result result = validate(workflow("hello", "${logger_1.text}"), Map.of("logger_1", LOGGER_OUTPUT));

        assertEquals("", result.errors());
    }

    @Test
    void graphMemberCannotReferenceAFollowingMemberOfTheSameGraph() {
        Result result = validate(workflow("${logger_2.text}", "hello"), Map.of("logger_2", LOGGER_OUTPUT));

        assertEquals("[logger_1] Wrong task order: You can't reference 'logger_2.text' in logger_1", result.errors());
    }

    @Test
    void graphMemberCanReferenceAFollowingMemberThatLoopsBackToIt() {
        Result result = validate(
            workflow(
                "logger_1", """
                    [{"from": "logger_1", "to": "logger_2"}, {"from": "logger_2", "to": "logger_1"}]""",
                "${logger_2.text}", "hello"),
            Map.of("logger_2", LOGGER_OUTPUT));

        assertEquals("", result.errors());
    }

    @Test
    void graphMemberCanReferenceAMemberDeclaredAfterItThatRunsFirst() {
        Result result = validate(
            workflow("logger_2", """
                [{"from": "logger_2", "to": "logger_1"}]""", "${logger_2.text}", "hello"),
            Map.of("logger_2", LOGGER_OUTPUT));

        assertEquals("", result.errors());
    }

    @Test
    void graphMemberCannotReferenceAMemberDeclaredBeforeItThatRunsAfter() {
        Result result = validate(
            workflow("logger_2", """
                [{"from": "logger_2", "to": "logger_1"}]""", "hello", "${logger_1.text}"),
            Map.of("logger_1", LOGGER_OUTPUT));

        assertEquals("[logger_2] Wrong task order: You can't reference 'logger_1.text' in logger_2", result.errors());
    }

    @Test
    void graphMemberCanReferenceAnyMemberWhenATransitionTargetIsDynamic() {
        Result result = validate(
            workflow("logger_1", """
                [{"from": "logger_1", "to": "=${logger_1.text}"}]""", "${logger_2.text}", "hello"),
            Map.of("logger_1", LOGGER_OUTPUT, "logger_2", LOGGER_OUTPUT));

        assertEquals("", result.errors());
    }

    @Test
    void graphTransitionConditionCanReferenceAMemberOfTheGraph() {
        Result result = validate(
            workflow("logger_1", """
                [{"from": "logger_1", "to": "logger_2", "condition": "=${logger_1.text} == 'x'"}]""", "hello",
                "hello"),
            Map.of("logger_1", LOGGER_OUTPUT));

        assertEquals("", result.errors());
    }

    @Test
    void taskNestedInAGraphMemberCanReferenceAMemberThatRunsBeforeItsEnclosingMember() {
        Result result = validate(
            nestedMemberWorkflow("""
                [{"from": "logger_2", "to": "condition_1"}]""", "${logger_2.text}"),
            Map.of("logger_2", LOGGER_OUTPUT));

        assertEquals("", result.errors());
    }

    @Test
    void taskNestedInAGraphMemberCannotReferenceAMemberThatRunsAfterItsEnclosingMember() {
        Result result = validate(
            nestedMemberWorkflow("""
                [{"from": "condition_1", "to": "logger_2"}]""", "${logger_2.text}"),
            Map.of("logger_2", LOGGER_OUTPUT));

        assertEquals("[logger_1] Wrong task order: You can't reference 'logger_2.text' in logger_1", result.errors());
    }

    private static String nestedMemberWorkflow(String transitions, String nestedText) {
        return NESTED_MEMBER_WORKFLOW
            .replace("TRANSITIONS", transitions)
            .replace("NESTED_TEXT", nestedText);
    }

    private static String workflow(String firstText, String secondText) {
        return workflow("logger_1", """
            [{"from": "logger_1", "to": "logger_2"}]""", firstText, secondText);
    }

    private static String workflow(String startNode, String transitions, String firstText, String secondText) {
        return GRAPH_WORKFLOW
            .replace("START_NODE", startNode)
            .replace("TRANSITIONS", transitions)
            .replace("FIRST_TEXT", firstText)
            .replace("SECOND_TEXT", secondText);
    }

    private static Result validate(String workflow, Map<String, PropertyInfo> nodeOutputMap) {
        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        WorkflowValidator.TaskDefinitionProvider taskDefinitionProvider =
            (taskType, kind) -> TASK_DEFINITION_MAP.getOrDefault(taskType, List.of());
        WorkflowValidator.TaskOutputProvider taskOutputProvider = (taskType, kind, warningsBuilder) -> null;
        WorkflowValidator.ClusterTypesProvider clusterTypesProvider = taskType -> null;

        WorkflowValidator.validateWorkflow(
            workflow, taskDefinitionProvider, taskOutputProvider, clusterTypesProvider,
            WorkflowValidator.NO_RESOURCE_REFERENCE_PROVIDER, new HashMap<>(), new HashMap<>(), nodeOutputMap,
            new HashMap<>(), new HashMap<>(), errors, warnings);

        return new Result(errors.toString(), warnings.toString());
    }

    private record Result(String errors, String warnings) {
    }
}
