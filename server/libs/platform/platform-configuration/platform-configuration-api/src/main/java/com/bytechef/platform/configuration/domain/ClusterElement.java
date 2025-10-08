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

import com.bytechef.platform.definition.WorkflowNodeType;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 *
 * @author Ivica Cardic
 */
public class ClusterElement {

    private final ComponentConnection componentConnection;
    private final String clusterElementName;
    private final String componentName;
    private final int componentVersion;
    private final Map<String, ?> extensions;
    private final String type;
    private final String label;
    private final String description;
    private final Map<String, ?> parameters;
    private final String workflowNodeName;

    public ClusterElement(
        ComponentConnection componentConnection, String description, Map<String, ?> extensions, String label,
        String type, Map<String, ?> parameters, String workflowNodeName) {

        this.componentConnection = componentConnection;
        this.extensions = Collections.unmodifiableMap(extensions);
        this.description = description;
        this.label = label;
        this.workflowNodeName = workflowNodeName;
        this.parameters = Collections.unmodifiableMap(parameters);
        this.type = type;

        WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(type);

        this.clusterElementName = workflowNodeType.operation();
        this.componentName = workflowNodeType.name();
        this.componentVersion = workflowNodeType.version();
    }

    public ComponentConnection getConnection() {
        return componentConnection;
    }

    public String getClusterElementName() {
        return clusterElementName;
    }

    public String getComponentName() {
        return componentName;
    }

    public int getComponentVersion() {
        return componentVersion;
    }

    public Map<String, ?> getExtensions() {
        return extensions;
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

    public String getWorkflowNodeName() {
        return workflowNodeName;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ClusterElement that)) {
            return false;
        }

        return componentVersion == that.componentVersion
            && Objects.equals(componentConnection, that.componentConnection) &&
            Objects.equals(clusterElementName, that.clusterElementName) &&
            Objects.equals(componentName, that.componentName) && Objects.equals(extensions, that.extensions) &&
            Objects.equals(type, that.type) && Objects.equals(label, that.label) &&
            Objects.equals(description, that.description) && Objects.equals(parameters, that.parameters) &&
            Objects.equals(workflowNodeName, that.workflowNodeName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            componentConnection, clusterElementName, componentName, componentVersion, extensions, type, label,
            description,
            parameters, workflowNodeName);
    }

    @Override
    public String toString() {
        return "ClusterElement{" +
            "workflowNodeName='" + workflowNodeName + '\'' +
            ", componentName='" + componentName + '\'' +
            ", componentVersion=" + componentVersion +
            ", clusterElementName='" + clusterElementName + '\'' +
            ", connection=" + componentConnection +
            ", type='" + type + '\'' +
            ", label='" + label + '\'' +
            ", description='" + description + '\'' +
            ", parameters=" + parameters +
            ", extensions=" + extensions +
            '}';
    }
}
