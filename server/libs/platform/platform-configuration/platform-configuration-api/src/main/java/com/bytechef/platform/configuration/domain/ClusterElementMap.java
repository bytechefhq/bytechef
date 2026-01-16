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

package com.bytechef.platform.configuration.domain;

import com.bytechef.atlas.configuration.constant.WorkflowConstants;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.component.definition.ClusterElementDefinition.ClusterElementType;
import com.bytechef.platform.configuration.constant.WorkflowExtConstants;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.type.TypeReference;

/**
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public class ClusterElementMap extends AbstractMap<String, Object> {

    private static final Logger log = LoggerFactory.getLogger(ClusterElementMap.class);

    private static final List<String> CLUSTER_ELEMENT_KEYS = List.of(
        WorkflowConstants.NAME, WorkflowConstants.TYPE, WorkflowConstants.LABEL,
        WorkflowConstants.DESCRIPTION, WorkflowConstants.PARAMETERS);

    private final Set<Map.Entry<String, Object>> entrySet;

    private ClusterElementMap(Set<Entry<String, Object>> entrySet) {
        this.entrySet = entrySet;
    }

    public static ClusterElementMap of(Map<String, ?> extensions) {
        return of(extensions, List.of());
    }

    @SuppressWarnings("unchecked")
    public static ClusterElementMap of(Map<String, ?> extensions, List<ComponentConnection> connections) {
        Set<Map.Entry<String, Object>> clusterElementSet = new HashSet<>();

        if (extensions.containsKey(WorkflowExtConstants.CLUSTER_ELEMENTS)) {
            Map<String, Object> clusterElementEntriesMap = MapUtils.get(
                extensions, WorkflowExtConstants.CLUSTER_ELEMENTS, new TypeReference<>() {}, Map.of());

            for (Map.Entry<String, Object> clusterElementsEntry : clusterElementEntriesMap.entrySet()) {
                Object clusterElementEntryValue = clusterElementsEntry.getValue();
                List<ClusterElement> clusterElements = new ArrayList<>();

                if (clusterElementEntryValue instanceof List<?> list) {
                    for (Object item : list) {
                        if (item instanceof Map<?, ?> map) {
                            add((Map<String, ?>) map, clusterElements, connections);
                        }
                    }

                    clusterElementSet.add(Map.entry(clusterElementsEntry.getKey(), clusterElements));
                } else {
                    if (clusterElementEntryValue == null) {
                        continue;
                    }

                    clusterElementSet.add(
                        Map.entry(
                            clusterElementsEntry.getKey(),
                            toClusterElement((Map<String, ?>) clusterElementEntryValue, connections)));
                }
            }
        }

        return new ClusterElementMap(clusterElementSet);
    }

    @Override
    @NonNull
    public Set<Entry<String, Object>> entrySet() {
        return entrySet;
    }

    public Optional<ClusterElement> fetchClusterElement(ClusterElementType clusterElementType) {
        return Optional.ofNullable(super.get(clusterElementType.key()))
            .map(value -> (ClusterElement) value);
    }

    public ClusterElement getClusterElement(ClusterElementType clusterElementType) {
        Object value = super.get(clusterElementType.key());

        if (value == null) {
            throw new IllegalArgumentException("Cluster element type %s not found".formatted(clusterElementType));
        }

        return (ClusterElement) value;
    }

    public ClusterElement getClusterElement(
        ClusterElementType clusterElementType, String clusterElementWorkflowNodeName) {

        if (clusterElementType.multipleElements()) {
            return getClusterElements(clusterElementType)
                .stream()
                .filter(curClusterElement -> Objects.equals(
                    curClusterElement.getWorkflowNodeName(), clusterElementWorkflowNodeName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                    "Cluster element %s not found".formatted(clusterElementWorkflowNodeName)));
        } else {
            ClusterElement clusterElement = getClusterElement(clusterElementType);

            if (!Objects.equals(clusterElement.getWorkflowNodeName(), clusterElementWorkflowNodeName)) {
                throw new IllegalArgumentException("Cluster element type %s not found".formatted(clusterElementType));
            }

            return clusterElement;
        }
    }

    @SuppressWarnings("unchecked")
    public List<ClusterElement> getClusterElements(ClusterElementType clusterElementType) {
        List<ClusterElement> clusterElements = (List<ClusterElement>) super.get(clusterElementType.key());

        return clusterElements == null ? List.of() : clusterElements;
    }

    private static void add(
        Map<String, ?> clusterElementMap, List<ClusterElement> clusterElements, List<ComponentConnection> connections) {

        try {
            clusterElements.add(toClusterElement(clusterElementMap, connections));
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.debug(e.getMessage());
            }
        }
    }

    private static ClusterElement toClusterElement(
        Map<String, ?> clusterElementMap, List<ComponentConnection> componentConnections) {

        Map<String, Object> extensions = new HashMap<>();

        for (Map.Entry<String, ?> entry : clusterElementMap.entrySet()) {
            if (!CLUSTER_ELEMENT_KEYS.contains(entry.getKey())) {
                extensions.put(entry.getKey(), entry.getValue());
            }
        }

        String name = MapUtils.getRequiredString(clusterElementMap, WorkflowConstants.NAME);

        ComponentConnection firstComponentConnection = componentConnections.stream()
            .filter(componentConnection -> Objects.equals(componentConnection.key(), name))
            .findFirst()
            .orElse(null);

        return new ClusterElement(
            firstComponentConnection, MapUtils.getString(clusterElementMap, WorkflowConstants.DESCRIPTION), extensions,
            MapUtils.getString(clusterElementMap, WorkflowConstants.LABEL),
            MapUtils.getRequiredString(clusterElementMap, WorkflowConstants.TYPE),
            MapUtils.getMap(clusterElementMap, WorkflowConstants.PARAMETERS, new TypeReference<>() {}, Map.of()),
            name);
    }
}
