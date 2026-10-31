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

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.platform.configuration.service.WorkflowDefinitionDiffService;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class VersionWorkflowChangeCalculatorTest {

    private static final WorkflowDefinitionDiffService DIFF_SERVICE = (oldWorkflow, newWorkflow) -> {
        if (oldWorkflow == null) {
            return new WorkflowDefinitionDiff(WorkflowChangeType.ADDED, List.of(), null, "new");
        }

        if (newWorkflow == null) {
            return new WorkflowDefinitionDiff(WorkflowChangeType.REMOVED, List.of(), "old", null);
        }

        WorkflowChangeType changeType = oldWorkflow.getDefinition()
            .equals(newWorkflow.getDefinition()) ? WorkflowChangeType.UNCHANGED : WorkflowChangeType.MODIFIED;

        return new WorkflowDefinitionDiff(changeType, List.of(), "old", "new");
    };

    @Test
    void testResolveBaseVersionSkipsDeletedVersions() {
        assertThat(VersionWorkflowChangeCalculator.resolveBaseVersion(List.of(1, 3), 3)).isEqualTo(1);
        assertThat(VersionWorkflowChangeCalculator.resolveBaseVersion(List.of(1, 2, 3), 3)).isEqualTo(2);
    }

    @Test
    void testResolveBaseVersionIsNullWhenNoneBelow() {
        assertThat(VersionWorkflowChangeCalculator.resolveBaseVersion(List.of(2, 3), 2)).isNull();
    }

    @Test
    void testSummarizeOrdersByChangeTypeThenCaseInsensitiveLabelNullsLast() {
        Map<String, Workflow> oldWorkflows = new LinkedHashMap<>();

        oldWorkflows.put("m-zebra", workflow("Zebra", "a"));
        oldWorkflows.put("m-apple", workflow("apple", "a"));
        oldWorkflows.put("m-null", workflow(null, "a"));
        oldWorkflows.put("unchanged", workflow("Same", "a"));
        oldWorkflows.put("removed", workflow("Gone", "a"));

        Map<String, Workflow> newWorkflows = new LinkedHashMap<>();

        newWorkflows.put("m-zebra", workflow("Zebra", "b"));
        newWorkflows.put("m-apple", workflow("apple", "b"));
        newWorkflows.put("m-null", workflow(null, "b"));
        newWorkflows.put("unchanged", workflow("Same", "a"));
        newWorkflows.put("added", workflow("New", "a"));

        VersionWorkflowChanges changes = VersionWorkflowChangeCalculator.summarize(
            1, 2, oldWorkflows, newWorkflows, DIFF_SERVICE);

        assertThat(changes.baseVersion()).isEqualTo(1);
        assertThat(changes.version()).isEqualTo(2);
        assertThat(changes.workflows())
            .extracting(VersionWorkflowChangeSummary::workflowUuid)
            .containsExactly("added", "m-apple", "m-zebra", "m-null", "removed");
    }

    @Test
    void testFetchChangeIsEmptyForUnknownUuid() {
        Optional<VersionWorkflowChange> change = VersionWorkflowChangeCalculator.fetchChange(
            1, 2, "missing", Map.of("known", workflow("A", "a")), Map.of(), DIFF_SERVICE);

        assertThat(change).isEmpty();
    }

    @Test
    void testFetchChangeReturnsRemovedChange() {
        Optional<VersionWorkflowChange> change = VersionWorkflowChangeCalculator.fetchChange(
            1, 2, "known", Map.of("known", workflow("A", "a")), Map.of(), DIFF_SERVICE);

        assertThat(change).isPresent();
        assertThat(change.get()
            .changeType()).isEqualTo(WorkflowChangeType.REMOVED);
    }

    private static Workflow workflow(String label, String marker) {
        String labelJson = label == null ? "" : "\"label\":\"" + label + "\",";

        return new Workflow(
            "id-" + marker, "{" + labelJson + "\"tasks\":[],\"description\":\"" + marker + "\"}", Workflow.Format.JSON);
    }
}
