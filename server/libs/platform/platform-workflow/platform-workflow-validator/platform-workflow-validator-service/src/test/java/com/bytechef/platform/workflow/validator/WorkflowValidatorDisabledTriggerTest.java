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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.commons.util.JsonUtils;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author Ivica Cardic
 */
class WorkflowValidatorDisabledTriggerTest {

    @BeforeAll
    public static void beforeAll() {
        JsonUtils.setObjectMapper(
            JsonMapper.builder()
                .build());
    }

    @Test
    void testNoEnabledTriggerDetectsAllDisabled() {
        String workflow = """
            {"label": "t", "triggers": [
                {"name": "trigger_1", "type": "webhook/v1/webhook", "disabled": true},
                {"name": "trigger_2", "type": "webhook/v1/webhook", "disabled": true}
            ], "tasks": []}
            """;

        assertTrue(WorkflowValidator.hasNoEnabledTrigger(workflow));
    }

    @Test
    void testNoEnabledTriggerTreatsTextualTrueAsDisabled() {
        String workflow = """
            {"label": "t", "triggers": [
                {"name": "trigger_1", "type": "webhook/v1/webhook", "disabled": "true"},
                {"name": "trigger_2", "type": "webhook/v1/webhook", "disabled": "TRUE"}
            ], "tasks": []}
            """;

        assertTrue(WorkflowValidator.hasNoEnabledTrigger(workflow));
    }

    @Test
    void testNoEnabledTriggerAllowsOneEnabled() {
        String workflow = """
            {"label": "t", "triggers": [
                {"name": "trigger_1", "type": "webhook/v1/webhook", "disabled": true},
                {"name": "trigger_2", "type": "webhook/v1/webhook"}
            ], "tasks": []}
            """;

        assertFalse(WorkflowValidator.hasNoEnabledTrigger(workflow));
    }

    @Test
    void testNoEnabledTriggerAllowsDisabledManual() {
        String workflow = """
            {"label": "t", "triggers": [
                {"name": "trigger_1", "type": "manual/v1/manual", "disabled": true}
            ], "tasks": []}
            """;

        assertFalse(WorkflowValidator.hasNoEnabledTrigger(workflow));
    }

    @Test
    void testNoEnabledTriggerAllowsWorkflowWithoutTriggers() {
        assertFalse(WorkflowValidator.hasNoEnabledTrigger("{\"label\": \"t\", \"tasks\": []}"));
        assertFalse(WorkflowValidator.hasNoEnabledTrigger("{\"label\": \"t\", \"triggers\": [], \"tasks\": []}"));
    }

    @Test
    void testNoEnabledTriggerFailsOpenOnUnparsableJson() {
        assertFalse(WorkflowValidator.hasNoEnabledTrigger("not json"));
    }

    @Test
    void testValidateWorkflowReportsNoEnabledTriggerError() {
        String workflow = """
            {"label": "t", "triggers": [
                {"label": "Webhook", "name": "trigger_1", "type": "webhook/v1/webhook", "disabled": true}
            ], "tasks": []}
            """;

        String errors = validateErrors(workflow);

        assertTrue(errors.contains("A workflow must have at least one enabled trigger."), errors);

        List<WorkflowValidatorFacade.NodeValidationIssue> nodeValidationIssues = NodeValidationIssueParser.parse(
            List.of(errors.split("\n")), List.of());

        assertTrue(
            nodeValidationIssues.stream()
                .anyMatch(
                    issue -> issue.severity() == WorkflowValidatorFacade.WorkflowIssueSeverity.ERROR &&
                        issue.kind() == WorkflowValidatorFacade.WorkflowIssueKind.OTHER &&
                        issue.message()
                            .contains("A workflow must have at least one enabled trigger.")),
            nodeValidationIssues.toString());
    }

    @Test
    void testValidateWorkflowWarnsOnDisabledManualTrigger() {
        String workflow = """
            {"label": "t", "triggers": [
                {"label": "Manual", "name": "trigger_1", "type": "manual/v1/manual", "disabled": true}
            ], "tasks": []}
            """;

        String warnings = validate(workflow);

        assertTrue(warnings.contains("Manual triggers cannot be disabled; the disabled flag is ignored."), warnings);
        assertFalse(validateErrors(workflow).contains("at least one enabled trigger"));
    }

    private static String validate(String workflow) {
        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        WorkflowValidator.validateWorkflow(
            workflow, (taskType, kind) -> null, (taskType, kind, warningsBuilder) -> null, taskType -> null,
            new HashMap<>(), new HashMap<>(), new HashMap<>(), errors, warnings);

        return warnings.toString();
    }

    private static String validateErrors(String workflow) {
        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        WorkflowValidator.validateWorkflow(
            workflow, (taskType, kind) -> null, (taskType, kind, warningsBuilder) -> null, taskType -> null,
            new HashMap<>(), new HashMap<>(), new HashMap<>(), errors, warnings);

        return errors.toString();
    }
}
