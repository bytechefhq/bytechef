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

import com.bytechef.platform.configuration.dto.WorkflowNodeChangeTarget;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class WorkflowNodeCollectorTest {

    @Test
    void testCollectsTriggerThenTasksThenInputsAndOutputs() {
        Map<String, Object> definition = Map.of(
            "outputs", List.of(Map.of("name", "o1", "value", "v")),
            "inputs", List.of(Map.of("name", "i1")),
            "tasks", List.of(Map.of("name", "t1", "type", "a/v1/b", "label", "T")),
            "triggers", List.of(Map.of("name", "trigger_1", "type", "c/v1/d")));

        List<WorkflowNode> nodes = WorkflowNodeCollector.collect(definition);

        assertThat(nodes).extracting(WorkflowNode::target)
            .containsExactly(
                WorkflowNodeChangeTarget.TRIGGER, WorkflowNodeChangeTarget.TASK, WorkflowNodeChangeTarget.INPUT,
                WorkflowNodeChangeTarget.OUTPUT);
        assertThat(nodes).extracting(WorkflowNode::name)
            .containsExactly("trigger_1", "t1", "i1", "o1");
        assertThat(nodes.get(1)
            .label()).isEqualTo("T");
        assertThat(nodes.get(1)
            .type()).isEqualTo("a/v1/b");
    }

    @Test
    void testTaskAttributesExcludeClusterElements() {
        Map<String, Object> definition = Map.of(
            "tasks", List.of(Map.of("name", "t1", "clusterElements", Map.of("model", Map.of("name", "m")))));

        List<WorkflowNode> nodes = WorkflowNodeCollector.collect(definition);

        assertThat(nodes.get(0)
            .attributes()).containsOnlyKeys("name");
    }

    @Test
    void testTaskIndexFollowsListPosition() {
        Map<String, Object> definition = Map.of(
            "tasks", List.of(Map.of("name", "a"), Map.of("name", "b")));

        List<WorkflowNode> nodes = WorkflowNodeCollector.collect(definition);

        assertThat(nodes).extracting(WorkflowNode::index)
            .containsExactly(0, 1);
    }

    @Test
    void testCollectsConditionCaseTrueAndCaseFalseTasks() {
        List<WorkflowNode> nodes = WorkflowNodeCollector.collect(
            Map.of(
                "tasks", List.of(
                    Map.of(
                        "name", "condition_1", "type", "condition/v1",
                        "parameters", Map.of(
                            "rawExpression", "x", "caseTrue", List.of(task("a")), "caseFalse", List.of(task("b")))))));

        assertThat(nodes).extracting(WorkflowNode::name)
            .containsExactly("condition_1", "a", "b");
        assertThat(nodes).extracting(WorkflowNode::parentPath)
            .containsOnly(null, "condition_1.caseTrue", "condition_1.caseFalse");
        assertThat(nodes.get(1)
            .parentPath()).isEqualTo("condition_1.caseTrue");
        assertThat(nodes.get(2)
            .parentPath()).isEqualTo("condition_1.caseFalse");
        assertThat(nodes.get(1)
            .target()).isEqualTo(WorkflowNodeChangeTarget.TASK);
    }

    @Test
    void testCollectsLoopIterateeTasks() {
        List<WorkflowNode> nodes = WorkflowNodeCollector.collect(
            Map.of(
                "tasks", List.of(
                    Map.of(
                        "name", "loop_1", "type", "loop/v1",
                        "parameters", Map.of("iteratee", List.of(task("a")))))));

        assertThat(nodes.get(1)
            .parentPath()).isEqualTo("loop_1.iteratee");
    }

    @Test
    void testCollectsBranchCasesAndDefaultTasks() {
        List<WorkflowNode> nodes = WorkflowNodeCollector.collect(
            Map.of(
                "tasks", List.of(
                    Map.of(
                        "name", "branch_1", "type", "branch/v1",
                        "parameters", Map.of(
                            "cases", List.of(
                                Map.of("key", "one", "tasks", List.of(task("a"))),
                                Map.of("key", "two", "tasks", List.of(task("b")))),
                            "default", List.of(task("c")))))));

        assertThat(nodes).extracting(WorkflowNode::name)
            .containsExactlyInAnyOrder("branch_1", "a", "b", "c");
        assertThat(nodes).extracting(WorkflowNode::parentPath)
            .containsExactlyInAnyOrder(
                null, "branch_1.cases[0].tasks", "branch_1.cases[1].tasks", "branch_1.default");
        assertThat(nodes.get(0)
            .attributes()).isEqualTo(
                Map.of(
                    "name", "branch_1", "type", "branch/v1",
                    "parameters",
                    Map.of(
                        "cases",
                        List.of(Map.of("key", "one", "tasks", List.of()), Map.of("key", "two", "tasks", List.of())),
                        "default", List.of())));
    }

    @Test
    void testCollectsForkJoinBranchesListOfLists() {
        List<WorkflowNode> nodes = WorkflowNodeCollector.collect(
            Map.of(
                "tasks", List.of(
                    Map.of(
                        "name", "forkJoin_1", "type", "fork-join/v1",
                        "parameters", Map.of(
                            "branches", List.of(List.of(task("a")), List.of(task("b"), task("c"))))))));

        assertThat(nodes).extracting(WorkflowNode::name)
            .containsExactly("forkJoin_1", "a", "b", "c");
        assertThat(nodes.get(1)
            .parentPath()).isEqualTo("forkJoin_1.branches[0]");
        assertThat(nodes.get(3)
            .parentPath()).isEqualTo("forkJoin_1.branches[1]");
        assertThat(nodes.get(3)
            .index()).isEqualTo(1);
    }

    @Test
    void testCollectsParallelTasks() {
        List<WorkflowNode> nodes = WorkflowNodeCollector.collect(
            Map.of(
                "tasks", List.of(
                    Map.of(
                        "name", "parallel_1", "type", "parallel/v1",
                        "parameters", Map.of("tasks", List.of(task("a")))))));

        assertThat(nodes.get(1)
            .parentPath()).isEqualTo("parallel_1.tasks");
    }

    @Test
    void testNestedTaskAttributesExcludeChildTaskLists() {
        List<WorkflowNode> nodes = WorkflowNodeCollector.collect(
            Map.of(
                "tasks", List.of(
                    Map.of(
                        "name", "condition_1", "type", "condition/v1",
                        "parameters", Map.of(
                            "rawExpression", "x", "caseTrue", List.of(task("a")), "caseFalse", List.of())))));

        assertThat(nodes.get(0)
            .attributes()).isEqualTo(
                Map.of(
                    "name", "condition_1", "type", "condition/v1", "parameters",
                    Map.of("rawExpression", "x", "caseTrue", List.of(), "caseFalse", List.of())));
    }

    @Test
    void testCollectsClusterElementsSingleAndList() {
        List<WorkflowNode> nodes = WorkflowNodeCollector.collect(
            Map.of(
                "tasks", List.of(
                    Map.of(
                        "name", "aiAgent_1", "type", "aiAgent/v1",
                        "clusterElements", Map.of(
                            "model", Map.of("name", "openai_1", "type", "openai/v1/model"),
                            "tools", List.of(
                                Map.of("name", "tool_1", "type", "t/v1/a"),
                                Map.of("name", "tool_2", "type", "t/v1/b")))))));

        assertThat(nodes).extracting(WorkflowNode::name)
            .containsExactlyInAnyOrder("aiAgent_1", "openai_1", "tool_1", "tool_2");

        WorkflowNode model = nodes.stream()
            .filter(node -> node.name()
                .equals("openai_1"))
            .findFirst()
            .orElseThrow();
        WorkflowNode tool = nodes.stream()
            .filter(node -> node.name()
                .equals("tool_2"))
            .findFirst()
            .orElseThrow();

        assertThat(model.target()).isEqualTo(WorkflowNodeChangeTarget.CLUSTER_ELEMENT);
        assertThat(model.parentPath()).isEqualTo("aiAgent_1.clusterElements.model");
        assertThat(tool.parentPath()).isEqualTo("aiAgent_1.clusterElements.tools");
        assertThat(tool.index()).isEqualTo(1);
    }

    @Test
    void testCollectsNestedClusterElements() {
        List<WorkflowNode> nodes = WorkflowNodeCollector.collect(
            Map.of(
                "tasks", List.of(
                    Map.of(
                        "name", "aiAgent_1", "type", "aiAgent/v1",
                        "clusterElements", Map.of(
                            "tools", List.of(
                                Map.of(
                                    "name", "tool_1", "type", "t/v1/a", "parameters", Map.of("x", 1),
                                    "clusterElements", Map.of(
                                        "model", Map.of("name", "inner_1", "type", "m/v1/m")))))))));

        WorkflowNode inner = nodes.stream()
            .filter(node -> node.name()
                .equals("inner_1"))
            .findFirst()
            .orElseThrow();
        WorkflowNode tool = nodes.stream()
            .filter(node -> node.name()
                .equals("tool_1"))
            .findFirst()
            .orElseThrow();

        assertThat(inner.parentPath()).isEqualTo("tool_1.clusterElements.model");
        assertThat(tool.attributes()).containsOnlyKeys("name", "type", "parameters");
    }

    private static Map<String, Object> task(String name) {
        return Map.of("name", name, "type", "a/v1/b");
    }

    @Test
    void testEmptyListParameterIsKeptInAttributes() {
        List<WorkflowNode> nodes = WorkflowNodeCollector.collect(
            Map.of(
                "tasks", List.of(
                    Map.of(
                        "name", "t1", "type", "a/v1/b",
                        "parameters", Map.of("headers", List.of(), "body", Map.of("values", List.of()))))));

        assertThat(nodes.get(0)
            .attributes()).isEqualTo(
                Map.of(
                    "name", "t1", "type", "a/v1/b", "parameters",
                    Map.of("headers", List.of(), "body", Map.of("values", List.of()))));
    }
}
