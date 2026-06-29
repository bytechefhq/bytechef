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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.workflow.validator.exception.WorkflowValidatorErrorType;
import com.bytechef.platform.workflow.validator.model.PropertyInfo;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/**
 * Covers duplicate node-name detection across the task-dispatcher nesting shapes, complementing the top-level checks in
 * {@link WorkflowValidatorTest}.
 */
class WorkflowValidatorDuplicateNodeNamesTest {

    @BeforeAll
    public static void beforeAll() {
        JsonUtils.setObjectMapper(
            JsonMapper.builder()
                .build());
    }

    @Test
    void detectsDuplicateNameNestedInConditionBranches() {
        String workflow = """
            {
                "label": "Test Workflow",
                "description": "Test workflow description",
                "triggers": [
                    {"label": "Manual Trigger", "name": "trigger_1", "type": "manual/v1/manual", "parameters": {}}
                ],
                "tasks": [
                    {
                        "label": "Condition A",
                        "name": "condition_1",
                        "type": "condition/v1",
                        "parameters": {
                            "caseTrue": [],
                            "caseFalse": [
                                {"label": "Email", "name": "email_1", "type": "email/v1/send", "parameters": {}}
                            ]
                        }
                    },
                    {
                        "label": "Condition B",
                        "name": "condition_2",
                        "type": "condition/v1",
                        "parameters": {
                            "caseTrue": [],
                            "caseFalse": [
                                {"label": "Email", "name": "email_1", "type": "email/v1/send", "parameters": {}}
                            ]
                        }
                    }
                ]
            }
            """;

        assertEquals("Node names must be unique. Duplicate node name: email_1", validate(workflow));
    }

    @Test
    void detectsDuplicateNameNestedInLoopIteratee() {
        String workflow = """
            {
                "label": "Test Workflow",
                "description": "Test workflow description",
                "triggers": [],
                "tasks": [
                    {
                        "label": "Loop",
                        "name": "loop_1",
                        "type": "loop/v1",
                        "parameters": {
                            "iteratee": [
                                {"label": "Log", "name": "logger_1", "type": "logger/v1/info", "parameters": {}}
                            ]
                        }
                    },
                    {"label": "Log", "name": "logger_1", "type": "logger/v1/info", "parameters": {}}
                ]
            }
            """;

        assertEquals("Node names must be unique. Duplicate node name: logger_1", validate(workflow));
    }

    @Test
    void detectsCollisionBetweenTriggerAndTaskNames() {
        String workflow = """
            {
                "label": "Test Workflow",
                "description": "Test workflow description",
                "triggers": [
                    {"label": "Manual Trigger", "name": "node_1", "type": "manual/v1/manual", "parameters": {}}
                ],
                "tasks": [
                    {"label": "Task", "name": "node_1", "type": "logger/v1/info", "parameters": {}}
                ]
            }
            """;

        assertEquals("Node names must be unique. Duplicate node name: node_1", validate(workflow));
    }

    @Test
    void reportsEachDuplicatedNameOnce() {
        String workflow = """
            {
                "label": "Test Workflow",
                "description": "Test workflow description",
                "triggers": [],
                "tasks": [
                    {"label": "Task", "name": "task_1", "type": "logger/v1/info", "parameters": {}},
                    {"label": "Task", "name": "task_1", "type": "logger/v1/info", "parameters": {}},
                    {"label": "Task", "name": "task_1", "type": "logger/v1/info", "parameters": {}}
                ]
            }
            """;

        assertEquals("Node names must be unique. Duplicate node name: task_1", validate(workflow));
    }

    @Test
    void allowsUniqueNamesAcrossNestedTasks() {
        String workflow = """
            {
                "label": "Test Workflow",
                "description": "Test workflow description",
                "triggers": [
                    {"label": "Manual Trigger", "name": "trigger_1", "type": "manual/v1/manual", "parameters": {}}
                ],
                "tasks": [
                    {
                        "label": "Condition",
                        "name": "condition_1",
                        "type": "condition/v1",
                        "parameters": {
                            "caseTrue": [
                                {"label": "Email", "name": "email_1", "type": "email/v1/send", "parameters": {}}
                            ],
                            "caseFalse": [
                                {"label": "Email", "name": "email_2", "type": "email/v1/send", "parameters": {}}
                            ]
                        }
                    }
                ]
            }
            """;

        assertEquals("", validate(workflow));
    }

    @Test
    void validateNoDuplicateNodeNamesThrowsWithErrorKeyOnDuplicate() {
        String workflow = """
            {
                "label": "L",
                "description": "D",
                "triggers": [],
                "tasks": [
                    {"label": "T", "name": "task_1", "type": "logger/v1/info", "parameters": {}},
                    {"label": "T", "name": "task_1", "type": "logger/v1/info", "parameters": {}}
                ]
            }
            """;

        ConfigurationException exception = assertThrows(
            ConfigurationException.class, () -> duplicateNodeNamesFacade().validateNoDuplicateNodeNames(workflow));

        assertEquals(WorkflowValidatorErrorType.DUPLICATE_NODE_NAMES.getErrorKey(), exception.getErrorKey());
    }

    @Test
    void validateNoDuplicateNodeNamesPassesWhenUnique() {
        String workflow = """
            {
                "label": "L",
                "description": "D",
                "triggers": [],
                "tasks": [
                    {"label": "T", "name": "task_1", "type": "logger/v1/info", "parameters": {}}
                ]
            }
            """;

        assertDoesNotThrow(() -> duplicateNodeNamesFacade().validateNoDuplicateNodeNames(workflow));
    }

    /**
     * A minimal {@link WorkflowValidatorFacade} that exercises the real {@code validateNoDuplicateNodeNames} default
     * method against the real duplicate detection, without resolving component/trigger definitions.
     */
    private static WorkflowValidatorFacade duplicateNodeNamesFacade() {
        return new WorkflowValidatorFacade() {

            @Override
            public WorkflowValidationResult validateWorkflow(String workflow) {
                return new WorkflowValidationResult(List.of(), List.of());
            }

            @Override
            public WorkflowValidationResult validateWorkflowById(String workflowId) {
                return new WorkflowValidationResult(List.of(), List.of());
            }

            @Override
            public List<String> getDuplicateNodeNames(String workflow) {
                return WorkflowValidator.getDuplicateNodeNames(workflow);
            }
        };
    }

    private static String validate(String workflow) {
        Map<String, List<PropertyInfo>> taskDefinitionMap = new HashMap<>();
        Map<String, PropertyInfo> taskOutputMap = new HashMap<>();
        Map<String, List<String>> clusterTypesMap = new HashMap<>();

        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        WorkflowValidator.TaskDefinitionProvider taskDefinitionProvider = (taskType, kind) -> List.of();
        WorkflowValidator.TaskOutputProvider taskOutputProvider = (taskType, kind, warningsBuilder) -> null;
        WorkflowValidator.ClusterTypesProvider clusterTypesProvider = (taskType) -> null;

        WorkflowValidator.validateWorkflow(
            workflow, taskDefinitionProvider, taskOutputProvider, clusterTypesProvider, taskDefinitionMap,
            taskOutputMap, clusterTypesMap, errors, warnings);

        return errors.toString();
    }
}
