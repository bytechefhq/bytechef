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

package com.bytechef.platform.configuration.workflow.diff;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.platform.configuration.dto.WorkflowDefinitionChange;
import com.bytechef.platform.configuration.dto.WorkflowDefinitionValueChange;
import com.bytechef.platform.configuration.dto.WorkflowNodeChangeKind;
import com.bytechef.platform.configuration.dto.WorkflowNodeChangeTarget;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class WorkflowStructuralDifferTest {

    @Test
    void testWorkflowLabelAndDescriptionChange() {
        List<WorkflowDefinitionChange> changes = WorkflowStructuralDiffer.diff(
            Map.of("label", "A", "description", "x"), Map.of("label", "B", "description", "y"));

        assertThat(changes).hasSize(1);

        WorkflowDefinitionChange change = changes.get(0);

        assertThat(change.target()).isEqualTo(WorkflowNodeChangeTarget.WORKFLOW);
        assertThat(change.kind()).isEqualTo(WorkflowNodeChangeKind.MODIFIED);
        assertThat(change.name()).isNull();
        assertThat(change.valueChanges()).extracting(WorkflowDefinitionValueChange::path)
            .containsExactly("label", "description");
    }

    @Test
    void testTriggerTypeChange() {
        List<WorkflowDefinitionChange> changes = WorkflowStructuralDiffer.diff(
            Map.of("triggers", List.of(Map.of("name", "trigger_1", "type", "a/v1/x"))),
            Map.of("triggers", List.of(Map.of("name", "trigger_1", "type", "b/v1/y"))));

        assertThat(changes).hasSize(1);
        assertThat(changes.get(0)
            .target()).isEqualTo(WorkflowNodeChangeTarget.TRIGGER);
        assertThat(changes.get(0)
            .kind()).isEqualTo(WorkflowNodeChangeKind.MODIFIED);
        assertThat(changes.get(0)
            .valueChanges()).containsExactly(
                new WorkflowDefinitionValueChange("type", "\"a/v1/x\"", "\"b/v1/y\""));
    }

    @Test
    void testTriggerAddedAndRemoved() {
        List<WorkflowDefinitionChange> added = WorkflowStructuralDiffer.diff(
            Map.of(), Map.of("triggers", List.of(Map.of("name", "trigger_1", "type", "a/v1/x"))));
        List<WorkflowDefinitionChange> removed = WorkflowStructuralDiffer.diff(
            Map.of("triggers", List.of(Map.of("name", "trigger_1", "type", "a/v1/x"))), Map.of());

        assertThat(added).hasSize(1);
        assertThat(added.get(0)
            .kind()).isEqualTo(WorkflowNodeChangeKind.ADDED);
        assertThat(added.get(0)
            .target()).isEqualTo(WorkflowNodeChangeTarget.TRIGGER);
        assertThat(removed).hasSize(1);
        assertThat(removed.get(0)
            .kind()).isEqualTo(WorkflowNodeChangeKind.REMOVED);
    }

    @Test
    void testTaskAdded() {
        List<WorkflowDefinitionChange> changes = WorkflowStructuralDiffer.diff(
            Map.of("tasks", List.of()),
            Map.of("tasks", List.of(Map.of("name", "httpClient_2", "type", "httpClient/v1/get"))));

        assertThat(changes).hasSize(1);

        WorkflowDefinitionChange change = changes.get(0);

        assertThat(change.kind()).isEqualTo(WorkflowNodeChangeKind.ADDED);
        assertThat(change.target()).isEqualTo(WorkflowNodeChangeTarget.TASK);
        assertThat(change.name()).isEqualTo("httpClient_2");
        assertThat(change.type()).isEqualTo("httpClient/v1/get");
        assertThat(change.valueChanges()).isEmpty();
    }

    @Test
    void testTaskRemoved() {
        List<WorkflowDefinitionChange> changes = WorkflowStructuralDiffer.diff(
            Map.of("tasks", List.of(Map.of("name", "t1", "type", "a/v1/b"))), Map.of("tasks", List.of()));

        assertThat(changes).hasSize(1);
        assertThat(changes.get(0)
            .kind()).isEqualTo(WorkflowNodeChangeKind.REMOVED);
        assertThat(changes.get(0)
            .name()).isEqualTo("t1");
    }

    @Test
    void testTaskParameterChange() {
        List<WorkflowDefinitionChange> changes = WorkflowStructuralDiffer.diff(
            tasks(Map.of("name", "t1", "parameters", Map.of("url", "a", "n", 1))),
            tasks(Map.of("name", "t1", "parameters", Map.of("url", "b", "n", 1))));

        assertThat(changes).hasSize(1);
        assertThat(changes.get(0)
            .valueChanges()).containsExactly(new WorkflowDefinitionValueChange("parameters.url", "\"a\"", "\"b\""));
    }

    @Test
    void testTaskNestedParameterListChange() {
        List<WorkflowDefinitionChange> changes = WorkflowStructuralDiffer.diff(
            tasks(Map.of("name", "t1", "parameters", Map.of("headers", List.of(Map.of("value", 1))))),
            tasks(Map.of("name", "t1", "parameters", Map.of("headers", List.of(Map.of("value", 2))))));

        assertThat(changes.get(0)
            .valueChanges()).containsExactly(
                new WorkflowDefinitionValueChange("parameters.headers[0].value", "1", "2"));
    }

    @Test
    void testRenamedTaskIsRemovedAndAdded() {
        List<WorkflowDefinitionChange> changes = WorkflowStructuralDiffer.diff(
            tasks(Map.of("name", "old", "type", "a/v1/b")), tasks(Map.of("name", "new", "type", "a/v1/b")));

        assertThat(changes).extracting(WorkflowDefinitionChange::kind)
            .containsExactly(WorkflowNodeChangeKind.ADDED, WorkflowNodeChangeKind.REMOVED);
        assertThat(changes).extracting(WorkflowDefinitionChange::name)
            .containsExactly("new", "old");
    }

    @Test
    void testInputAddedRemovedModified() {
        List<WorkflowDefinitionChange> changes = WorkflowStructuralDiffer.diff(
            Map.of("inputs", List.of(Map.of("name", "keep", "required", false), Map.of("name", "gone"))),
            Map.of("inputs", List.of(Map.of("name", "keep", "required", true), Map.of("name", "fresh"))));

        assertThat(changes).extracting(WorkflowDefinitionChange::kind)
            .containsExactly(WorkflowNodeChangeKind.MODIFIED, WorkflowNodeChangeKind.ADDED,
                WorkflowNodeChangeKind.REMOVED);
        assertThat(changes).extracting(WorkflowDefinitionChange::target)
            .containsOnly(WorkflowNodeChangeTarget.INPUT);
        assertThat(changes.get(0)
            .valueChanges()).containsExactly(new WorkflowDefinitionValueChange("required", "false", "true"));
    }

    @Test
    void testOutputValueChange() {
        List<WorkflowDefinitionChange> changes = WorkflowStructuralDiffer.diff(
            Map.of("outputs", List.of(Map.of("name", "o", "value", "a"))),
            Map.of("outputs", List.of(Map.of("name", "o", "value", "b"))));

        assertThat(changes).hasSize(1);
        assertThat(changes.get(0)
            .target()).isEqualTo(WorkflowNodeChangeTarget.OUTPUT);
        assertThat(changes.get(0)
            .valueChanges()).containsExactly(new WorkflowDefinitionValueChange("value", "\"a\"", "\"b\""));
    }

    @Test
    void testChangeOrderIsWorkflowThenNewOrderThenRemoved() {
        List<WorkflowDefinitionChange> changes = WorkflowStructuralDiffer.diff(
            Map.of("label", "A", "tasks", List.of(Map.of("name", "gone"), Map.of("name", "a", "label", "1"))),
            Map.of("label", "B", "tasks", List.of(Map.of("name", "b"), Map.of("name", "a", "label", "2"))));

        assertThat(changes).extracting(WorkflowDefinitionChange::target)
            .containsExactly(WorkflowNodeChangeTarget.WORKFLOW, WorkflowNodeChangeTarget.TASK,
                WorkflowNodeChangeTarget.TASK, WorkflowNodeChangeTarget.TASK);
        assertThat(changes).extracting(WorkflowDefinitionChange::name)
            .containsExactly(null, "b", "a", "gone");
    }

    @Test
    void testMaskedValueChangeShowsMaskOnBothSides() {
        List<WorkflowDefinitionChange> changes = WorkflowStructuralDiffer.diff(
            maskedTask("one"), maskedTask("two"));

        String quotedMask = "\"" + SensitiveValueMasker.MASK + "\"";

        assertThat(changes).hasSize(1);
        assertThat(changes.get(0)
            .valueChanges()).containsExactly(
                new WorkflowDefinitionValueChange("parameters.password", quotedMask, quotedMask));
    }

    @Test
    void testNoValueChangeContainsMaskDigest() {
        List<WorkflowDefinitionChange> changes = WorkflowStructuralDiffer.diff(
            maskedTask("one"), maskedTask("two"));

        for (WorkflowDefinitionChange change : changes) {
            for (WorkflowDefinitionValueChange valueChange : change.valueChanges()) {
                assertThat(valueChange.oldValue()).doesNotContain(SensitiveValueMasker.MASK + ":");
                assertThat(valueChange.newValue()).doesNotContain(SensitiveValueMasker.MASK + ":");
            }
        }
    }

    private static Map<String, Object> maskedTask(String secret) {
        return SensitiveValueMasker.mask(
            tasks(Map.of("name", "t1", "parameters", Map.of("password", secret))));
    }

    private static Map<String, Object> tasks(Map<String, Object> task) {
        return Map.of("tasks", List.of(task));
    }

    @Test
    void testNestedTaskAddedInsideCondition() {
        List<WorkflowDefinitionChange> changes = WorkflowStructuralDiffer.diff(
            Map.of("tasks", List.of(condition(List.of(), List.of()))),
            Map.of("tasks", List.of(condition(List.of(nested("a", "x")), List.of()))));

        assertThat(changes).hasSize(1);
        assertThat(changes.get(0)
            .kind()).isEqualTo(WorkflowNodeChangeKind.ADDED);
        assertThat(changes.get(0)
            .name()).isEqualTo("a");
        assertThat(changes.get(0)
            .parentPath()).isEqualTo("condition_1.caseTrue");
    }

    @Test
    void testNestedTaskParameterChangeInsideLoop() {
        List<WorkflowDefinitionChange> changes = WorkflowStructuralDiffer.diff(
            Map.of("tasks", List.of(loop(List.of(nested("a", "x"))))),
            Map.of("tasks", List.of(loop(List.of(nested("a", "y"))))));

        assertThat(changes).hasSize(1);
        assertThat(changes.get(0)
            .kind()).isEqualTo(WorkflowNodeChangeKind.MODIFIED);
        assertThat(changes.get(0)
            .name()).isEqualTo("a");
        assertThat(changes.get(0)
            .valueChanges()).containsExactly(new WorkflowDefinitionValueChange("parameters.p", "\"x\"", "\"y\""));
    }

    @Test
    void testTaskMovedBetweenConditionBranches() {
        List<WorkflowDefinitionChange> changes = WorkflowStructuralDiffer.diff(
            Map.of("tasks", List.of(condition(List.of(nested("a", "x")), List.of()))),
            Map.of("tasks", List.of(condition(List.of(), List.of(nested("a", "x"))))));

        assertThat(changes).hasSize(1);
        assertThat(changes.get(0)
            .kind()).isEqualTo(WorkflowNodeChangeKind.MOVED);
        assertThat(changes.get(0)
            .parentPath()).isEqualTo("condition_1.caseFalse");
    }

    @Test
    void testTaskMovedToTopLevel() {
        List<WorkflowDefinitionChange> changes = WorkflowStructuralDiffer.diff(
            Map.of("tasks", List.of(condition(List.of(nested("a", "x")), List.of()))),
            Map.of("tasks", List.of(condition(List.of(), List.of()), nested("a", "x"))));

        assertThat(changes).hasSize(1);
        assertThat(changes.get(0)
            .kind()).isEqualTo(WorkflowNodeChangeKind.MOVED);
        assertThat(changes.get(0)
            .parentPath()).isNull();
    }

    @Test
    void testReorderedSiblingsAreMoved() {
        List<WorkflowDefinitionChange> changes = WorkflowStructuralDiffer.diff(
            Map.of("tasks", List.of(nested("a", "x"), nested("b", "x"), nested("c", "x"))),
            Map.of("tasks", List.of(nested("a", "x"), nested("c", "x"), nested("b", "x"))));

        assertThat(changes).extracting(WorkflowDefinitionChange::name)
            .containsExactly("c");
        assertThat(changes).extracting(WorkflowDefinitionChange::kind)
            .containsOnly(WorkflowNodeChangeKind.MOVED);
    }

    @Test
    void testTopLevelReorderProducesMovedChanges() {
        List<WorkflowDefinitionChange> changes = WorkflowStructuralDiffer.diff(
            Map.of("tasks", List.of(nested("a", "x"), nested("b", "x"))),
            Map.of("tasks", List.of(nested("b", "x"), nested("a", "x"))));

        assertThat(changes).extracting(WorkflowDefinitionChange::name)
            .containsExactly("b");
        assertThat(changes).extracting(WorkflowDefinitionChange::kind)
            .containsOnly(WorkflowNodeChangeKind.MOVED);
    }

    @Test
    void testMovingOneTaskToTheEndMovesOnlyThatTask() {
        List<WorkflowDefinitionChange> changes = WorkflowStructuralDiffer.diff(
            Map.of("tasks", List.of(nested("a", "x"), nested("b", "x"), nested("c", "x"), nested("d", "x"))),
            Map.of("tasks", List.of(nested("b", "x"), nested("c", "x"), nested("d", "x"), nested("a", "x"))));

        assertThat(changes).extracting(WorkflowDefinitionChange::name)
            .containsExactly("a");
        assertThat(changes).extracting(WorkflowDefinitionChange::kind)
            .containsOnly(WorkflowNodeChangeKind.MOVED);
    }

    @Test
    void testInsertingSiblingDoesNotMoveOthers() {
        List<WorkflowDefinitionChange> changes = WorkflowStructuralDiffer.diff(
            Map.of("tasks", List.of(nested("a", "x"), nested("c", "x"))),
            Map.of("tasks", List.of(nested("a", "x"), nested("b", "x"), nested("c", "x"))));

        assertThat(changes).hasSize(1);
        assertThat(changes.get(0)
            .kind()).isEqualTo(WorkflowNodeChangeKind.ADDED);
        assertThat(changes.get(0)
            .name()).isEqualTo("b");
    }

    @Test
    void testClusterElementAddedRemovedModified() {
        Map<String, Object> oldTask = Map.of(
            "name", "agent_1", "type", "aiAgent/v1",
            "clusterElements", Map.of(
                "model", Map.of("name", "m1", "type", "a/v1/m", "parameters", Map.of("p", "x")),
                "tools", List.of(Map.of("name", "t1", "type", "a/v1/t"))));
        Map<String, Object> newTask = Map.of(
            "name", "agent_1", "type", "aiAgent/v1",
            "clusterElements", Map.of(
                "model", Map.of("name", "m1", "type", "a/v1/m", "parameters", Map.of("p", "y")),
                "tools", List.of(Map.of("name", "t2", "type", "a/v1/t"))));

        List<WorkflowDefinitionChange> changes = WorkflowStructuralDiffer.diff(
            Map.of("tasks", List.of(oldTask)), Map.of("tasks", List.of(newTask)));

        assertThat(changes).hasSize(3);
        assertThat(changes).extracting(WorkflowDefinitionChange::target)
            .containsOnly(WorkflowNodeChangeTarget.CLUSTER_ELEMENT);
        assertThat(changes).extracting(WorkflowDefinitionChange::kind)
            .containsExactlyInAnyOrder(
                WorkflowNodeChangeKind.MODIFIED, WorkflowNodeChangeKind.ADDED, WorkflowNodeChangeKind.REMOVED);
        assertThat(changes).extracting(WorkflowDefinitionChange::parentPath)
            .containsOnly("agent_1.clusterElements.model", "agent_1.clusterElements.tools");
    }

    @Test
    void testMovedTaskWithParameterChangeStaysMoved() {
        List<WorkflowDefinitionChange> changes = WorkflowStructuralDiffer.diff(
            Map.of("tasks", List.of(condition(List.of(nested("a", "x")), List.of()))),
            Map.of("tasks", List.of(condition(List.of(), List.of(nested("a", "y"))))));

        assertThat(changes).hasSize(1);
        assertThat(changes.get(0)
            .kind()).isEqualTo(WorkflowNodeChangeKind.MOVED);
        assertThat(changes.get(0)
            .valueChanges()).hasSize(1);
    }

    @Test
    void testOtherTopLevelWorkflowKeyChangeIsReportedOnWorkflowEntry() {
        List<WorkflowDefinitionChange> changes = WorkflowStructuralDiffer.diff(
            Map.of("label", "A", "settings", Map.of("retry", 1)), Map.of("label", "A", "settings", Map.of("retry", 2)));

        assertThat(changes).hasSize(1);
        assertThat(changes.get(0)
            .target()).isEqualTo(WorkflowNodeChangeTarget.WORKFLOW);
        assertThat(changes.get(0)
            .valueChanges()).containsExactly(new WorkflowDefinitionValueChange("settings.retry", "1", "2"));
    }

    @Test
    void testDuplicateNodeNamesArePairedByOccurrence() {
        List<WorkflowDefinitionChange> changes = WorkflowStructuralDiffer.diff(
            Map.of("tasks", List.of(nested("a", "first"), nested("a", "second"))),
            Map.of("tasks", List.of(nested("a", "first"), nested("a", "changed"))));

        assertThat(changes).hasSize(1);

        WorkflowDefinitionChange change = changes.get(0);

        assertThat(change.kind()).isEqualTo(WorkflowNodeChangeKind.MODIFIED);
        assertThat(change.name()).isEqualTo("a");
        assertThat(change.valueChanges()).containsExactly(
            new WorkflowDefinitionValueChange("parameters.p", "\"second\"", "\"changed\""));
    }

    @Test
    void testRemovedDuplicateNodeNameIsReportedOnce() {
        List<WorkflowDefinitionChange> changes = WorkflowStructuralDiffer.diff(
            Map.of("tasks", List.of(nested("a", "first"), nested("a", "second"))),
            Map.of("tasks", List.of(nested("a", "first"))));

        assertThat(changes).hasSize(1);
        assertThat(changes.get(0)
            .kind()).isEqualTo(WorkflowNodeChangeKind.REMOVED);
        assertThat(changes.get(0)
            .name()).isEqualTo("a");
    }

    private static Map<String, Object> nested(String name, String parameterValue) {
        return Map.of("name", name, "type", "a/v1/b", "parameters", Map.of("p", parameterValue));
    }

    private static Map<String, Object> condition(List<Object> caseTrue, List<Object> caseFalse) {
        return Map.of(
            "name", "condition_1", "type", "condition/v1",
            "parameters", Map.of("rawExpression", "x", "caseTrue", caseTrue, "caseFalse", caseFalse));
    }

    private static Map<String, Object> loop(List<Object> iteratee) {
        return Map.of("name", "loop_1", "type", "loop/v1", "parameters", Map.of("iteratee", iteratee));
    }

    @Test
    void testEmptyListVersusMissingKeyIsReported() {
        List<WorkflowDefinitionChange> changes = WorkflowStructuralDiffer.diff(
            Map.of("tasks",
                List.of(Map.of("name", "t1", "type", "a/v1/b", "parameters", Map.of("headers", List.of())))),
            Map.of("tasks", List.of(Map.of("name", "t1", "type", "a/v1/b", "parameters", Map.of()))));

        assertThat(changes).hasSize(1);
        assertThat(changes.get(0)
            .kind()).isEqualTo(WorkflowNodeChangeKind.MODIFIED);
        assertThat(changes.get(0)
            .valueChanges()).extracting(WorkflowDefinitionValueChange::path)
                .containsExactly("parameters.headers");
    }
}
