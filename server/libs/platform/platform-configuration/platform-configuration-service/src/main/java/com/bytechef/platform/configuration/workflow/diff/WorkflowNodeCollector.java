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
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
public class WorkflowNodeCollector {

    private static final String CLUSTER_ELEMENTS = "clusterElements";
    private static final Set<String> NON_CONTAINER_KEYS = Set.of("name", "type", "label", "description", "metadata");
    private static final String PARAMETERS = "parameters";

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
                collectNode(target, item, null, index, nodes);
            }
        }
    }

    private static void collectNode(
        WorkflowNodeChangeTarget target, Map<?, ?> item, @Nullable String parentPath, int index,
        List<WorkflowNode> nodes) {

        Object name = item.get("name");
        Object type = item.get("type");
        Object label = item.get("label");

        String nodeName = name == null ? "" : String.valueOf(name);

        Map<String, Object> attributes = new LinkedHashMap<>();
        List<WorkflowNode> childNodes = new ArrayList<>();

        for (Map.Entry<?, ?> entry : item.entrySet()) {
            String attributeKey = String.valueOf(entry.getKey());

            if (CLUSTER_ELEMENTS.equals(attributeKey)) {
                continue;
            }

            Object value = entry.getValue();

            if (target != WorkflowNodeChangeTarget.CLUSTER_ELEMENT && !NON_CONTAINER_KEYS.contains(attributeKey)) {
                String valuePath = PARAMETERS.equals(attributeKey) ? nodeName : nodeName + "." + attributeKey;

                value = prune(value, valuePath, childNodes);
            }

            attributes.put(attributeKey, value);
        }

        nodes.add(
            new WorkflowNode(
                target, nodeName, type == null ? null : String.valueOf(type),
                label == null ? null : String.valueOf(label), parentPath, index, attributes));
        nodes.addAll(childNodes);

        if (item.get(CLUSTER_ELEMENTS) instanceof Map<?, ?> clusterElements) {
            collectClusterElements(nodeName, clusterElements, nodes);
        }
    }

    private static void collectClusterElements(String ownerName, Map<?, ?> clusterElements, List<WorkflowNode> nodes) {
        for (Map.Entry<?, ?> entry : clusterElements.entrySet()) {
            String parentPath = ownerName + "." + CLUSTER_ELEMENTS + "." + entry.getKey();
            Object value = entry.getValue();

            if (value instanceof Map<?, ?> element) {
                collectNode(WorkflowNodeChangeTarget.CLUSTER_ELEMENT, element, parentPath, 0, nodes);
            } else if (value instanceof List<?> elements) {
                for (int index = 0; index < elements.size(); index++) {
                    if (elements.get(index) instanceof Map<?, ?> element) {
                        collectNode(WorkflowNodeChangeTarget.CLUSTER_ELEMENT, element, parentPath, index, nodes);
                    }
                }
            }
        }
    }

    private static Object prune(Object value, String path, List<WorkflowNode> childNodes) {
        if (value instanceof List<?> list) {
            List<Object> remaining = new ArrayList<>();

            for (int index = 0; index < list.size(); index++) {
                Object item = list.get(index);

                if (isTask(item)) {
                    collectNode(WorkflowNodeChangeTarget.TASK, (Map<?, ?>) item, path, index, childNodes);
                } else {
                    remaining.add(prune(item, path + "[" + index + "]", childNodes));
                }
            }

            return remaining;
        }

        if (value instanceof Map<?, ?> map) {
            Map<String, Object> remaining = new LinkedHashMap<>();

            for (Map.Entry<?, ?> entry : map.entrySet()) {
                remaining.put(
                    String.valueOf(entry.getKey()), prune(entry.getValue(), path + "." + entry.getKey(), childNodes));
            }

            return remaining;
        }

        return value;
    }

    private static boolean isTask(Object item) {
        return item instanceof Map<?, ?> map && map.get("name") instanceof String && map.get("type") instanceof String;
    }
}
