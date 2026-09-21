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
class WorkflowValidatorForkJoinReferenceTest {

    private static final PropertyInfo LOGGER_OUTPUT = new PropertyInfo(
        null, "OBJECT", null, false, false, null, null, List.of(
            new PropertyInfo("text", "STRING", null, false, false, null, null)),
        null);

    private static final Map<String, List<PropertyInfo>> TASK_DEFINITION_MAP = Map.of(
        "manual/v1/manual", List.of(),
        "logger/v1/info", List.of(
            new PropertyInfo("text", "STRING", null, false, true, null, null)),
        "fork-join/v1", List.of(
            new PropertyInfo("branches", "ARRAY", null, false, false, null, List.of(
                new PropertyInfo(null, "ARRAY", null, false, false, null, List.of(
                    new PropertyInfo(null, "TASK", null, false, false, null, null)))))),
        "branch/v1", List.of(
            new PropertyInfo("expression", "STRING", null, false, true, null, null),
            new PropertyInfo("cases", "ARRAY", null, false, false, null, List.of(
                new PropertyInfo(null, "OBJECT", null, false, false, null, List.of(
                    new PropertyInfo("key", "STRING", null, false, true, null, null),
                    new PropertyInfo("tasks", "ARRAY", null, false, false, null, List.of(
                        new PropertyInfo(null, "TASK", null, false, false, null, null))))))),
            new PropertyInfo("default", "ARRAY", null, false, false, null, List.of(
                new PropertyInfo(null, "TASK", null, false, false, null, null)))));

    private static final String NESTED_TASKS = """
        [
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
        """;

    private static final String FORK_JOIN_WORKFLOW = """
        {
            "label": "workflow1",
            "description": "",
            "triggers": [
                {"label": "Manual", "name": "trigger_1", "type": "manual/v1/manual"}
            ],
            "tasks": [
                {
                    "label": "Fork/Join",
                    "name": "fork-join_1",
                    "type": "fork-join/v1",
                    "parameters": {"branches": [NESTED_TASKS]}
                }
            ]
        }
        """;

    private static final String BRANCH_WORKFLOW = """
        {
            "label": "workflow1",
            "description": "",
            "triggers": [
                {"label": "Manual", "name": "trigger_1", "type": "manual/v1/manual"}
            ],
            "tasks": [
                {
                    "label": "Branch",
                    "name": "branch_1",
                    "type": "branch/v1",
                    "parameters": {"expression": "a", "cases": [{"key": "a", "tasks": NESTED_TASKS}]}
                }
            ]
        }
        """;

    @Test
    void forkJoinBranchTaskCanReferenceAPrecedingTaskInTheSameBranch() {
        Result result = validate(
            workflow(FORK_JOIN_WORKFLOW, "hello", "${logger_1.text}"), Map.of("logger_1", LOGGER_OUTPUT));

        assertEquals("", result.errors());
    }

    @Test
    void forkJoinBranchTaskCannotReferenceAFollowingTaskInTheSameBranch() {
        Result result = validate(
            workflow(FORK_JOIN_WORKFLOW, "${logger_2.text}", "hello"), Map.of("logger_2", LOGGER_OUTPUT));

        assertEquals("[logger_1] Wrong task order: You can't reference 'logger_2.text' in logger_1", result.errors());
    }

    @Test
    void branchCaseTaskCanReferenceAPrecedingTaskInTheSameCase() {
        Result result = validate(
            workflow(BRANCH_WORKFLOW, "hello", "${logger_1.text}"), Map.of("logger_1", LOGGER_OUTPUT));

        assertEquals("", result.errors());
    }

    private static String workflow(String template, String firstText, String secondText) {
        String nestedTasks = NESTED_TASKS
            .replace("FIRST_TEXT", firstText)
            .replace("SECOND_TEXT", secondText);

        return template.replace("NESTED_TASKS", nestedTasks);
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
