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

package com.bytechef.platform.configuration.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class VersionWorkflowChangeFactoryTest {

    private static final String WORKFLOW_UUID = "workflow-uuid";

    @Test
    void testSummaryUsesNewLabelAndChangeCount() {
        Workflow oldWorkflow = workflow("Old label");
        Workflow newWorkflow = workflow("New label");
        WorkflowDefinitionDiff diff = new WorkflowDefinitionDiff(
            WorkflowChangeType.MODIFIED, List.of(nodeChange(), nodeChange()), "old", "new");

        VersionWorkflowChangeSummary summary = VersionWorkflowChangeFactory.summary(
            WORKFLOW_UUID, oldWorkflow, newWorkflow, diff);

        assertEquals(WORKFLOW_UUID, summary.workflowUuid());
        assertEquals("New label", summary.label());
        assertEquals(WorkflowChangeType.MODIFIED, summary.changeType());
        assertEquals(2, summary.changeCount());
    }

    @Test
    void testSummaryUsesOldLabelWhenRemoved() {
        Workflow oldWorkflow = workflow("Old label");
        WorkflowDefinitionDiff diff = new WorkflowDefinitionDiff(
            WorkflowChangeType.REMOVED, List.of(), "old", null);

        VersionWorkflowChangeSummary summary = VersionWorkflowChangeFactory.summary(
            WORKFLOW_UUID, oldWorkflow, null, diff);

        assertEquals("Old label", summary.label());
        assertEquals(WorkflowChangeType.REMOVED, summary.changeType());
        assertEquals(0, summary.changeCount());
    }

    @Test
    void testChangeCarriesDefinitionsAndVersions() {
        Workflow oldWorkflow = workflow("Old label");
        Workflow newWorkflow = workflow("New label");
        List<WorkflowDefinitionChange> changes = List.of(nodeChange());
        WorkflowDefinitionDiff diff = new WorkflowDefinitionDiff(
            WorkflowChangeType.MODIFIED, changes, "old", "new");

        VersionWorkflowChange versionChange = VersionWorkflowChangeFactory.change(
            WORKFLOW_UUID, 1, 2, oldWorkflow, newWorkflow, diff);

        assertEquals(WORKFLOW_UUID, versionChange.workflowUuid());
        assertEquals("New label", versionChange.label());
        assertEquals(WorkflowChangeType.MODIFIED, versionChange.changeType());
        assertEquals(1, versionChange.baseVersion());
        assertEquals(2, versionChange.version());
        assertEquals(changes, versionChange.changes());
        assertEquals("old", versionChange.oldDefinition());
        assertEquals("new", versionChange.newDefinition());
    }

    @Test
    void testChangeAllowsMissingBaseVersionAndWorkflows() {
        WorkflowDefinitionDiff diff = new WorkflowDefinitionDiff(
            WorkflowChangeType.ADDED, List.of(), null, "new");

        VersionWorkflowChange versionChange = VersionWorkflowChangeFactory.change(
            WORKFLOW_UUID, null, 1, null, null, diff);

        assertNull(versionChange.label());
        assertNull(versionChange.baseVersion());
        assertNull(versionChange.oldDefinition());
    }

    private static Workflow workflow(String label) {
        return new Workflow("{\"label\": \"" + label + "\", \"tasks\": []}", Workflow.Format.JSON);
    }

    private static WorkflowDefinitionChange nodeChange() {
        return new WorkflowDefinitionChange(
            WorkflowNodeChangeKind.MODIFIED, WorkflowNodeChangeTarget.TASK, "task1", "type", null, null, List.of());
    }
}
