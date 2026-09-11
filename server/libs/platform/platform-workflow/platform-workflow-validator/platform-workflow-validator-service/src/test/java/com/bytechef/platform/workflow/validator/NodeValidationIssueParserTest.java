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
import static org.junit.jupiter.api.Assertions.assertNull;

import com.bytechef.platform.workflow.validator.WorkflowValidatorFacade.NodeValidationIssue;
import com.bytechef.platform.workflow.validator.WorkflowValidatorFacade.WorkflowIssueKind;
import com.bytechef.platform.workflow.validator.WorkflowValidatorFacade.WorkflowIssueSeverity;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class NodeValidationIssueParserTest {

    @Test
    void classifiesAMissingConnectionWithoutAPropertyPath() {
        List<NodeValidationIssue> issues = NodeValidationIssueParser.parse(
            List.of("[affinity_1] Missing required connection: Affinity"), List.of());

        assertIssue(
            issues.getFirst(), "affinity_1", null, WorkflowIssueKind.MISSING_CONNECTION, WorkflowIssueSeverity.ERROR);
    }

    @Test
    void classifiesPrefixedMessagesByTemplate() {
        List<NodeValidationIssue> issues = NodeValidationIssueParser.parse(
            List.of(
                "[dataTable_1] Missing required property: table",
                "[dataTable_1] Resource referenced by property 'table' is not available: gone",
                "[condition_1] Property 'python_1.diff' does not exist in the output of 'python/v1/script'",
                "[condition_1] Wrong task order: You can't reference 'logger_1.x' in condition_1",
                "[http_1] Property 'timeout' has incorrect type. Expected: INTEGER, but got: STRING",
                "[loop_1] Property 'loop_1.item[0].propBool' in output of 'loop/v1' is of type boolean, not number",
                "[agent_1] Cluster element 'model' is missing from task agent_1",
                "[x_1] Something the parser has never seen"),
            List.of(
                "[dataTable_1] Could not verify the resource referenced by property 'table': db down",
                "[condition_1] Property 'python_1.diff' might not exist in the output of 'python/v1/script'"));

        assertEquals(10, issues.size());
        assertIssue(issues.get(0), "dataTable_1", "table", WorkflowIssueKind.MISSING_REQUIRED,
            WorkflowIssueSeverity.ERROR);
        assertIssue(issues.get(1), "dataTable_1", "table", WorkflowIssueKind.MISSING_RESOURCE,
            WorkflowIssueSeverity.ERROR);
        assertIssue(issues.get(2), "condition_1", "python_1.diff", WorkflowIssueKind.BROKEN_REFERENCE,
            WorkflowIssueSeverity.ERROR);
        assertIssue(issues.get(3), "condition_1", "logger_1.x", WorkflowIssueKind.TASK_ORDER,
            WorkflowIssueSeverity.ERROR);
        assertIssue(issues.get(4), "http_1", "timeout", WorkflowIssueKind.TYPE_MISMATCH, WorkflowIssueSeverity.ERROR);
        assertIssue(issues.get(5), "loop_1", "loop_1.item[0].propBool", WorkflowIssueKind.TYPE_MISMATCH,
            WorkflowIssueSeverity.ERROR);
        assertIssue(issues.get(6), "agent_1", "model", WorkflowIssueKind.MISSING_CLUSTER_ELEMENT,
            WorkflowIssueSeverity.ERROR);
        assertIssue(issues.get(7), "x_1", null, WorkflowIssueKind.OTHER, WorkflowIssueSeverity.ERROR);
        assertIssue(issues.get(8), "dataTable_1", "table", WorkflowIssueKind.MISSING_RESOURCE,
            WorkflowIssueSeverity.WARNING);
        assertIssue(issues.get(9), "condition_1", "python_1.diff", WorkflowIssueKind.BROKEN_REFERENCE,
            WorkflowIssueSeverity.WARNING);
        assertEquals("Something the parser has never seen", issues.get(7)
            .message());
    }

    @Test
    void classifiesFieldTaskDateAndDisplayConditionMessages() {
        List<NodeValidationIssue> issues = NodeValidationIssueParser.parse(
            List.of(
                "[testTask] Field 'label' must be a string",
                "[task_2] Task 'ghost_1.value' doesn't exist.",
                "[task_1] Input property 'flag' is of type boolean, not integer",
                "[testTask] Property 'startDate' is in incorrect date format. Format should be in: 'yyyy-MM-dd'",
                "[testTask] Property 'items' does not match any of the expected union types: STRING, NUMBER"),
            List.of(
                "[testTask] Missing recommended field: label",
                "[testTask] Invalid logic for display condition: 'foo =='"));

        assertEquals(7, issues.size());
        assertIssue(issues.get(0), "testTask", "label", WorkflowIssueKind.TYPE_MISMATCH, WorkflowIssueSeverity.ERROR);
        assertIssue(issues.get(1), "task_2", "ghost_1.value", WorkflowIssueKind.BROKEN_REFERENCE,
            WorkflowIssueSeverity.ERROR);
        assertIssue(issues.get(2), "task_1", "flag", WorkflowIssueKind.TYPE_MISMATCH, WorkflowIssueSeverity.ERROR);
        assertIssue(issues.get(3), "testTask", "startDate", WorkflowIssueKind.TYPE_MISMATCH,
            WorkflowIssueSeverity.ERROR);
        assertIssue(issues.get(4), "testTask", "items", WorkflowIssueKind.TYPE_MISMATCH, WorkflowIssueSeverity.ERROR);
        assertIssue(issues.get(5), "testTask", "label", WorkflowIssueKind.MISSING_REQUIRED,
            WorkflowIssueSeverity.WARNING);
        assertIssue(issues.get(6), "testTask", "'foo =='", WorkflowIssueKind.OTHER, WorkflowIssueSeverity.WARNING);
    }

    @Test
    void duplicateNodeNameIsAttributedToTheDuplicatedName() {
        List<NodeValidationIssue> issues = NodeValidationIssueParser.parse(
            List.of("Node names must be unique. Duplicate node name: logger_1"), List.of());

        assertEquals(1, issues.size());
        assertIssue(issues.get(0), "logger_1", null, WorkflowIssueKind.DUPLICATE_NODE_NAME,
            WorkflowIssueSeverity.ERROR);
    }

    @Test
    void workflowLevelMessagesWithoutNodeAreDropped() {
        List<NodeValidationIssue> issues = NodeValidationIssueParser.parse(
            List.of("Failed to validate workflow: boom", "Missing required field: triggers"), List.of());

        assertEquals(0, issues.size());
    }

    private static void assertIssue(
        NodeValidationIssue issue, String nodeName, String propertyPath, WorkflowIssueKind kind,
        WorkflowIssueSeverity severity) {

        assertEquals(nodeName, issue.nodeName());

        if (propertyPath == null) {
            assertNull(issue.propertyPath());
        } else {
            assertEquals(propertyPath, issue.propertyPath());
        }

        assertEquals(kind, issue.kind());
        assertEquals(severity, issue.severity());
    }
}
