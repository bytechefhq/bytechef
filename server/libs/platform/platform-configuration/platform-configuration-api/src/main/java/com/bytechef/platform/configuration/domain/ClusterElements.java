/*
 * Copyright 2023-present ByteChef Inc.
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

import static com.bytechef.platform.configuration.domain.ClusterElements.ClusterElement;

import com.bytechef.atlas.configuration.constant.WorkflowConstants;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.platform.configuration.constant.WorkflowExtConstants;
import com.bytechef.platform.definition.WorkflowNodeType;
import com.fasterxml.jackson.core.type.TypeReference;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public class ClusterElements extends AbstractMap<String, List<ClusterElement>> {

    private static final Logger log = LoggerFactory.getLogger(ClusterElements.class);

    private final Set<Map.Entry<String, List<ClusterElement>>> entrySet;

    public ClusterElements(Set<Entry<String, List<ClusterElement>>> entrySet) {
        this.entrySet = entrySet;
    }

    public static ClusterElements of(Map<String, ?> extensions) {
        Set<Map.Entry<String, List<ClusterElement>>> result = new HashSet<>();

        if (extensions.containsKey(WorkflowExtConstants.CLUSTER_ELEMENTS)) {
            Map<String, Map<String, Map<String, ?>>> clusterElementEntriesMap = MapUtils.get(
                extensions, WorkflowExtConstants.CLUSTER_ELEMENTS, new TypeReference<>() {}, Map.of());

            for (Map.Entry<String, Map<String, Map<String, ?>>> clusterElementsEntry : clusterElementEntriesMap
                .entrySet()) {

                Map<String, Map<String, ?>> clusterElementEntryMap = clusterElementsEntry.getValue();
                List<ClusterElement> clusterElements = new ArrayList<>();

                for (Map.Entry<String, Map<String, ?>> entry : clusterElementEntryMap.entrySet()) {
                    Map<String, ?> elementValue = entry.getValue();

                    try {
                        ClusterElement clusterElement = new ClusterElement(
                            entry.getKey(),
                            MapUtils.getRequiredString(elementValue, WorkflowConstants.TYPE),
                            MapUtils.getString(elementValue, WorkflowConstants.LABEL),
                            MapUtils.getString(elementValue, WorkflowConstants.DESCRIPTION),
                            MapUtils.getMap(
                                elementValue, WorkflowConstants.PARAMETERS, new TypeReference<>() {}, Map.of()));

                        clusterElements.add(clusterElement);
                    } catch (Exception e) {
                        if (log.isDebugEnabled()) {
                            log.debug(e.getMessage());
                        }
                    }
                }

                result.add(Map.entry(clusterElementsEntry.getKey(), clusterElements));
            }
        }

        return new ClusterElements(result);
    }

    @Override
    public Set<Entry<String, List<ClusterElement>>> entrySet() {
        return entrySet;
    }

    public Optional<ClusterElement> fetchFirst(String clusterElementTypeName) {
        return Optional.ofNullable(get(clusterElementTypeName))
            .orElse(List.of())
            .stream()
            .findFirst();
    }

    public ClusterElement getFirst(String clusterElementTypeName) {
        return get(clusterElementTypeName)
            .stream()
            .findFirst()
            .orElseThrow(
                () -> new IllegalArgumentException(
                    "Cluster element type %s not found".formatted(clusterElementTypeName)));
    }

    public static final class ClusterElement {

        private final String componentName;
        private final String componentOperation;
        private final int componentVersion;
        private final String name;
        private final String type;
        private final String label;
        private final String description;
        private final Map<String, ?> parameters;

        public ClusterElement(
            String name, String type, String label, String description, Map<String, ?> parameters) {
            this.name = name;
            this.type = type;
            this.label = label;
            this.description = description;
            this.parameters = parameters;

            WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(type);

            this.componentName = workflowNodeType.name();
            this.componentOperation = workflowNodeType.operation();
            this.componentVersion = workflowNodeType.version();
        }

        public String getComponentName() {
            return componentName;
        }

        public String getComponentOperation() {
            return componentOperation;
        }

        public int getComponentVersion() {
            return componentVersion;
        }

        public String getName() {
            return name;
        }

        public String getType() {
            return type;
        }

        public String getLabel() {
            return label;
        }

        public String getDescription() {
            return description;
        }

        public Map<String, ?> getParameters() {
            return parameters;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }

            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }

            var that = (ClusterElement) obj;

            return Objects.equals(this.name, that.name) && Objects.equals(this.type, that.type) &&
                Objects.equals(this.label, that.label) && Objects.equals(this.description, that.description) &&
                Objects.equals(this.parameters, that.parameters);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, type, label, description, parameters);
        }

        @Override
        public String toString() {
            return "ClusterElement[" +
                "name=" + name + ", " +
                "type=" + type + ", " +
                "label=" + label + ", " +
                "description=" + description + ", " +
                "parameters=" + parameters + ']';
        }
    }
}
