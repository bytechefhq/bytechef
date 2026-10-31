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

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.platform.configuration.dto.WorkflowDefinitionChange;
import com.bytechef.platform.configuration.dto.WorkflowDefinitionValueChange;
import com.bytechef.platform.configuration.dto.WorkflowNodeChangeKind;
import com.bytechef.platform.configuration.dto.WorkflowNodeChangeTarget;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
public class WorkflowStructuralDiffer {

    private static final Set<String> STRUCTURAL_KEYS = Set.of("tasks", "triggers", "inputs", "outputs");

    private WorkflowStructuralDiffer() {
    }

    public static List<WorkflowDefinitionChange> diff(
        Map<String, Object> oldDefinition, Map<String, Object> newDefinition) {

        List<WorkflowDefinitionChange> changes = new ArrayList<>();

        List<WorkflowDefinitionValueChange> workflowValueChanges = new ArrayList<>();

        Set<String> workflowKeys = new LinkedHashSet<>(List.of("label", "description"));

        workflowKeys.addAll(new TreeSet<>(oldDefinition.keySet()));
        workflowKeys.addAll(new TreeSet<>(newDefinition.keySet()));
        workflowKeys.removeAll(STRUCTURAL_KEYS);

        for (String key : workflowKeys) {
            compareValues(key, oldDefinition.get(key), newDefinition.get(key), workflowValueChanges);
        }

        if (!workflowValueChanges.isEmpty()) {
            changes.add(
                new WorkflowDefinitionChange(
                    WorkflowNodeChangeKind.MODIFIED, WorkflowNodeChangeTarget.WORKFLOW, null, null, null, null,
                    workflowValueChanges));
        }

        Map<String, WorkflowNode> oldNodes = index(WorkflowNodeCollector.collect(oldDefinition));
        Map<String, WorkflowNode> newNodes = index(WorkflowNodeCollector.collect(newDefinition));

        List<Set<WorkflowNodeChangeTarget>> groups = List.of(
            Set.of(
                WorkflowNodeChangeTarget.TRIGGER, WorkflowNodeChangeTarget.TASK,
                WorkflowNodeChangeTarget.CLUSTER_ELEMENT),
            Set.of(WorkflowNodeChangeTarget.INPUT), Set.of(WorkflowNodeChangeTarget.OUTPUT));

        Set<String> movedKeys = findMovedKeys(oldNodes, newNodes);

        for (Set<WorkflowNodeChangeTarget> group : groups) {
            for (Map.Entry<String, WorkflowNode> newEntry : newNodes.entrySet()) {
                String nodeKey = newEntry.getKey();
                WorkflowNode newNode = newEntry.getValue();

                if (!group.contains(newNode.target())) {
                    continue;
                }

                WorkflowNode oldNode = oldNodes.get(nodeKey);

                if (oldNode == null) {
                    changes.add(toChange(WorkflowNodeChangeKind.ADDED, newNode, List.of()));
                } else {
                    List<WorkflowDefinitionValueChange> valueChanges = new ArrayList<>();

                    compareValues("", oldNode.attributes(), newNode.attributes(), valueChanges);

                    if (movedKeys.contains(nodeKey)) {
                        changes.add(toChange(WorkflowNodeChangeKind.MOVED, newNode, valueChanges));
                    } else if (!valueChanges.isEmpty()) {
                        changes.add(toChange(WorkflowNodeChangeKind.MODIFIED, newNode, valueChanges));
                    }
                }
            }

            for (Map.Entry<String, WorkflowNode> oldEntry : oldNodes.entrySet()) {
                WorkflowNode oldNode = oldEntry.getValue();

                if (group.contains(oldNode.target()) && !newNodes.containsKey(oldEntry.getKey())) {
                    changes.add(toChange(WorkflowNodeChangeKind.REMOVED, oldNode, List.of()));
                }
            }
        }

        return changes;
    }

    private static Set<String> findMovedKeys(
        Map<String, WorkflowNode> oldNodes, Map<String, WorkflowNode> newNodes) {

        Set<String> movedKeys = new HashSet<>();
        Map<String, List<String>> oldSiblings = new LinkedHashMap<>();
        Map<String, List<String>> newSiblings = new LinkedHashMap<>();

        for (Map.Entry<String, WorkflowNode> entry : oldNodes.entrySet()) {
            WorkflowNode oldNode = entry.getValue();
            WorkflowNode newNode = newNodes.get(entry.getKey());

            if (newNode == null) {
                continue;
            }

            if (Objects.equals(oldNode.parentPath(), newNode.parentPath())) {
                oldSiblings.computeIfAbsent(siblingGroup(oldNode), groupKey -> new ArrayList<>())
                    .add(entry.getKey());
            } else {
                movedKeys.add(entry.getKey());
            }
        }

        for (Map.Entry<String, WorkflowNode> entry : newNodes.entrySet()) {
            WorkflowNode newNode = entry.getValue();
            WorkflowNode oldNode = oldNodes.get(entry.getKey());

            if (oldNode != null && Objects.equals(oldNode.parentPath(), newNode.parentPath())) {
                newSiblings.computeIfAbsent(siblingGroup(newNode), groupKey -> new ArrayList<>())
                    .add(entry.getKey());
            }
        }

        for (Map.Entry<String, List<String>> entry : oldSiblings.entrySet()) {
            List<String> oldOrder = entry.getValue();
            List<String> newOrder = newSiblings.get(entry.getKey());

            Map<String, Integer> oldPositions = new HashMap<>();

            for (int index = 0; index < oldOrder.size(); index++) {
                oldPositions.put(oldOrder.get(index), index);
            }

            int[] oldPositionsInNewOrder = new int[newOrder.size()];

            for (int index = 0; index < newOrder.size(); index++) {
                oldPositionsInNewOrder[index] = oldPositions.get(newOrder.get(index));
            }

            Set<Integer> stableIndexes = findLongestIncreasingSubsequence(oldPositionsInNewOrder);

            for (int index = 0; index < newOrder.size(); index++) {
                if (!stableIndexes.contains(index)) {
                    movedKeys.add(newOrder.get(index));
                }
            }
        }

        return movedKeys;
    }

    /**
     * Returns the indexes of one longest strictly increasing subsequence of the given values. Siblings on that
     * subsequence kept their relative order; only the others are reported as moved.
     */
    private static Set<Integer> findLongestIncreasingSubsequence(int[] values) {
        int[] tailIndexes = new int[values.length];
        int[] previousIndexes = new int[values.length];
        int length = 0;

        for (int index = 0; index < values.length; index++) {
            int low = 0;
            int high = length;

            while (low < high) {
                int middle = (low + high) >>> 1;

                if (values[tailIndexes[middle]] < values[index]) {
                    low = middle + 1;
                } else {
                    high = middle;
                }
            }

            previousIndexes[index] = low > 0 ? tailIndexes[low - 1] : -1;
            tailIndexes[low] = index;

            if (low == length) {
                length++;
            }
        }

        Set<Integer> stableIndexes = new HashSet<>();

        for (int index = length > 0 ? tailIndexes[length - 1] : -1; index >= 0; index = previousIndexes[index]) {
            stableIndexes.add(index);
        }

        return stableIndexes;
    }

    private static String siblingGroup(WorkflowNode node) {
        return node.target() + ":" + node.parentPath();
    }

    private static WorkflowDefinitionChange toChange(
        WorkflowNodeChangeKind kind, WorkflowNode node, List<WorkflowDefinitionValueChange> valueChanges) {

        return new WorkflowDefinitionChange(
            kind, node.target(), node.name(), node.type(), node.label(), node.parentPath(), valueChanges);
    }

    private static Map<String, WorkflowNode> index(List<WorkflowNode> nodes) {
        Map<String, WorkflowNode> indexedNodes = new LinkedHashMap<>();
        Map<String, Integer> occurrences = new HashMap<>();

        for (WorkflowNode node : nodes) {
            String nameKey = node.target() + ":" + node.name();

            int occurrence = occurrences.merge(nameKey, 1, Integer::sum) - 1;

            indexedNodes.put(occurrence == 0 ? nameKey : nameKey + "#" + occurrence, node);
        }

        return indexedNodes;
    }

    private static void compareValues(
        String path, @Nullable Object oldValue, @Nullable Object newValue,
        List<WorkflowDefinitionValueChange> valueChanges) {

        if (oldValue instanceof Map<?, ?> oldMap && newValue instanceof Map<?, ?> newMap) {
            Set<Object> keys = new LinkedHashSet<>(oldMap.keySet());

            keys.addAll(newMap.keySet());

            for (Object key : keys) {
                String childPath = path.isEmpty() ? String.valueOf(key) : path + "." + key;

                compareValues(childPath, oldMap.get(key), newMap.get(key), valueChanges);
            }

            return;
        }

        if (oldValue instanceof List<?> oldList && newValue instanceof List<?> newList) {
            int size = Math.max(oldList.size(), newList.size());

            for (int index = 0; index < size; index++) {
                compareValues(
                    path + "[" + index + "]", index < oldList.size() ? oldList.get(index) : null,
                    index < newList.size() ? newList.get(index) : null, valueChanges);
            }

            return;
        }

        if (!Objects.equals(oldValue, newValue)) {
            valueChanges.add(new WorkflowDefinitionValueChange(path, write(oldValue), write(newValue)));
        }
    }

    private static @Nullable String write(@Nullable Object value) {
        return value == null ? null : JsonUtils.write(SensitiveValueMasker.plainMasks(value));
    }
}
