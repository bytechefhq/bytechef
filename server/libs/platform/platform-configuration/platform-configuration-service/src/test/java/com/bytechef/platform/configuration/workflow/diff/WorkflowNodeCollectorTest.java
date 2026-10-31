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
}
