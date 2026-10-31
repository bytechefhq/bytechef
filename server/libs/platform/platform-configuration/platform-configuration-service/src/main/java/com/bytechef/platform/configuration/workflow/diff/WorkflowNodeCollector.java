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

import com.bytechef.platform.configuration.dto.WorkflowNodeChangeTarget;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class WorkflowNodeCollector {

    private static final String CLUSTER_ELEMENTS = "clusterElements";

    private WorkflowNodeCollector() {
    }

    public static List<WorkflowNode> collect(Map<String, Object> normalizedDefinition) {
        List<WorkflowNode> nodes = new ArrayList<>();

        collectList(normalizedDefinition, "triggers", WorkflowNodeChangeTarget.TRIGGER, nodes);
        collectList(normalizedDefinition, "tasks", WorkflowNodeChangeTarget.TASK, nodes);
        collectList(normalizedDefinition, "inputs", WorkflowNodeChangeTarget.INPUT, nodes);
        collectList(normalizedDefinition, "outputs", WorkflowNodeChangeTarget.OUTPUT, nodes);

        return nodes;
    }

    private static void collectList(
        Map<String, Object> definition, String key, WorkflowNodeChangeTarget target, List<WorkflowNode> nodes) {

        if (!(definition.get(key) instanceof List<?> items)) {
            return;
        }

        for (int index = 0; index < items.size(); index++) {
            if (items.get(index) instanceof Map<?, ?> item) {
                nodes.add(toNode(target, item, index));
            }
        }
    }

    private static WorkflowNode toNode(WorkflowNodeChangeTarget target, Map<?, ?> item, int index) {
        Map<String, Object> attributes = new LinkedHashMap<>();

        for (Map.Entry<?, ?> entry : item.entrySet()) {
            String attributeKey = String.valueOf(entry.getKey());

            if (!CLUSTER_ELEMENTS.equals(attributeKey)) {
                attributes.put(attributeKey, entry.getValue());
            }
        }

        Object name = item.get("name");
        Object type = item.get("type");
        Object label = item.get("label");

        return new WorkflowNode(
            target, name == null ? "" : String.valueOf(name), type == null ? null : String.valueOf(type),
            label == null ? null : String.valueOf(label), null, index, attributes);
    }
}
