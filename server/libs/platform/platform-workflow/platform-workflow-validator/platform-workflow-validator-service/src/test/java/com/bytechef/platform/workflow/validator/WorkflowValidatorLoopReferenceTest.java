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
class WorkflowValidatorLoopReferenceTest {

    private static final PropertyInfo LOGGER_OUTPUT = new PropertyInfo(
        null, "OBJECT", null, false, false, null, null, List.of(
            new PropertyInfo("text", "STRING", null, false, false, null, null)),
        null);

    private static final Map<String, List<PropertyInfo>> TASK_DEFINITION_MAP = Map.of(
        "manual/v1/manual", List.of(),
        "logger/v1/info", List.of(
            new PropertyInfo("text", "STRING", null, false, true, null, null)),
        "loop/v1", List.of(
            new PropertyInfo("items", "ARRAY", null, false, false, null, null),
            new PropertyInfo("iteratee", "ARRAY", null, false, false, null, List.of(
                new PropertyInfo(null, "TASK", null, false, false, null, null)))),
        "map/v1", List.of(
            new PropertyInfo("items", "ARRAY", null, false, false, null, null),
            new PropertyInfo("iteratee", "ARRAY", null, false, false, null, List.of(
                new PropertyInfo(null, "TASK", null, false, false, null, null)))),
        "each/v1", List.of(
            new PropertyInfo("items", "ARRAY", null, false, false, null, null),
            new PropertyInfo("iteratee", "TASK", null, false, false, null, null)));

    private static final String EACH_WORKFLOW = """
        {
            "label": "workflow1",
            "description": "",
            "triggers": [
                {"label": "Manual", "name": "trigger_1", "type": "manual/v1/manual"}
            ],
            "tasks": [
                {
                    "label": "Each",
                    "name": "each_1",
                    "type": "each/v1",
                    "parameters": {
                        "items": [1, 2, 3],
                        "iteratee": {
                            "label": "Logger",
                            "name": "logger_1",
                            "type": "logger/v1/info",
                            "parameters": {"text": "=getOrDefault(${logger_1.text}, '')"}
                        }
                    }
                }
            ]
        }
        """;

    private static final String LOOP_WORKFLOW = """
        {
            "label": "workflow1",
            "description": "",
            "triggers": [
                {"label": "Manual", "name": "trigger_1", "type": "manual/v1/manual"}
            ],
            "tasks": [
                {
                    "label": "Logger",
                    "name": "logger_0",
                    "type": "logger/v1/info",
                    "parameters": {"text": "BEFORE_TEXT"}
                },
                {
                    "label": "Loop",
                    "name": "loop_1",
                    "type": "LOOP_TYPE",
                    "parameters": {
                        "items": [1, 2, 3],
                        "iteratee": [
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
    void loopBodyTaskCanReferenceItsOwnOutputFromThePreviousIteration() {
        Result result = validate(
            workflow("hello", "=getOrDefault(${logger_1.text}, '')", "hello"), Map.of("logger_1", LOGGER_OUTPUT));

        assertEquals("", result.errors());
    }

    @Test
    void loopBodyTaskCanReferenceALaterTaskOfTheSameBody() {
        Result result = validate(workflow("hello", "${logger_2.text}", "hello"), Map.of("logger_2", LOGGER_OUTPUT));

        assertEquals("", result.errors());
    }

    @Test
    void taskBeforeTheLoopCannotReferenceATaskOfTheBody() {
        Result result = validate(workflow("${logger_1.text}", "hello", "hello"), Map.of("logger_1", LOGGER_OUTPUT));

        assertEquals("[logger_0] Wrong task order: You can't reference 'logger_1.text' in logger_0", result.errors());
    }

    @Test
    void mapBodyTaskCannotReferenceItsOwnOutput() {
        Result result = validate(
            workflow("map/v1", "hello", "=getOrDefault(${logger_1.text}, '')", "hello"),
            Map.of("logger_1", LOGGER_OUTPUT));

        assertEquals("[logger_1] Wrong task order: You can't reference 'logger_1.text' in logger_1", result.errors());
    }

    @Test
    void mapBodyTaskCannotReferenceALaterTaskOfTheSameBody() {
        Result result = validate(
            workflow("map/v1", "hello", "${logger_2.text}", "hello"), Map.of("logger_2", LOGGER_OUTPUT));

        assertEquals("[logger_1] Wrong task order: You can't reference 'logger_2.text' in logger_1", result.errors());
    }

    @Test
    void eachIterateeTaskCannotReferenceItsOwnOutput() {
        Result result = validate(EACH_WORKFLOW, Map.of("logger_1", LOGGER_OUTPUT));

        assertEquals("[logger_1] Wrong task order: You can't reference 'logger_1.text' in logger_1", result.errors());
    }

    private static String workflow(String beforeText, String firstText, String secondText) {
        return workflow("loop/v1", beforeText, firstText, secondText);
    }

    private static String workflow(String loopType, String beforeText, String firstText, String secondText) {
        return LOOP_WORKFLOW
            .replace("LOOP_TYPE", loopType)
            .replace("BEFORE_TEXT", beforeText)
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
