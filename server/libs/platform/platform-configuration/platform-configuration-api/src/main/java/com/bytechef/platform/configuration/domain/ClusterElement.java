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

import com.bytechef.platform.definition.WorkflowNodeType;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 *
 * @author Ivica Cardic
 */
public class ClusterElement {

    private final String clusterElementName;
    private final String componentName;
    private final int componentVersion;
    private final Map<String, ?> extensions;
    private final String nodeName;
    private final String type;
    private final String label;
    private final String description;
    private final Map<String, ?> parameters;

    public ClusterElement(
        String nodeName, String type, String label, String description, Map<String, ?> parameters,
        Map<String, ?> extensions) {

        this.extensions = Collections.unmodifiableMap(extensions);
        this.description = description;
        this.label = label;
        this.nodeName = nodeName;
        this.parameters = Collections.unmodifiableMap(parameters);
        this.type = type;

        WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(type);

        this.clusterElementName = workflowNodeType.operation();
        this.componentName = workflowNodeType.name();
        this.componentVersion = workflowNodeType.version();
    }

    public String getComponentName() {
        return componentName;
    }

    public String getClusterElementName() {
        return clusterElementName;
    }

    public int getComponentVersion() {
        return componentVersion;
    }

    public Map<String, ?> getExtensions() {
        return Collections.unmodifiableMap(extensions);
    }

    public String getNodeName() {
        return nodeName;
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
        return Collections.unmodifiableMap(parameters);
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

        return Objects.equals(this.nodeName, that.nodeName) && Objects.equals(this.type, that.type) &&
            Objects.equals(this.label, that.label) && Objects.equals(this.description, that.description) &&
            Objects.equals(this.parameters, that.parameters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodeName, type, label, description, parameters);
    }

    @Override
    public String toString() {
        return "ClusterElement[" +
            "name=" + nodeName + ", " +
            "type=" + type + ", " +
            "label=" + label + ", " +
            "description=" + description + ", " +
            "parameters=" + parameters + ']';
    }
}
