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

package com.bytechef.platform.configuration.dto;

import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.platform.configuration.domain.ClusterElementMap;
import com.bytechef.platform.configuration.domain.ComponentConnection;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author Ivica Cardic
 */
public final class WorkflowTaskDTO {

    private final List<ComponentConnection> connections;
    private final ClusterElementMap clusterElements;
    private final boolean clusterRoot;
    private final String description;
    private final List<WorkflowTask> finalize;
    private final String label;
    private final int maxRetries;
    private final Map<String, ?> metadata;
    private final String name;
    private final String node;
    private final Map<String, ?> parameters;
    private final List<WorkflowTask> post;
    private final List<WorkflowTask> pre;
    private final int taskNumber;
    private final String timeout;
    private final String type;

    @SuppressFBWarnings("EI")
    public WorkflowTaskDTO(
        ClusterElementMap clusterElements, boolean clusterRoot, List<ComponentConnection> connections,
        String description, List<WorkflowTask> finalize, String label, int maxRetries, Map<String, ?> metadata,
        String name, String node, Map<String, ?> parameters, List<WorkflowTask> post, List<WorkflowTask> pre,
        int taskNumber, String timeout, String type) {

        this.clusterElements = clusterElements;
        this.clusterRoot = clusterRoot;
        this.connections = Collections.unmodifiableList(connections);
        this.description = description;
        this.finalize = Collections.unmodifiableList(finalize);
        this.label = label;
        this.maxRetries = maxRetries;
        this.metadata = Collections.unmodifiableMap(metadata);
        this.name = name;
        this.node = node;
        this.parameters = Collections.unmodifiableMap(parameters);
        this.post = Collections.unmodifiableList(post);
        this.pre = Collections.unmodifiableList(pre);
        this.taskNumber = taskNumber;
        this.timeout = timeout;
        this.type = type;
    }

    public WorkflowTaskDTO(
        WorkflowTask workflowTask, boolean clusterRoot, ClusterElementMap clusterElements,
        List<ComponentConnection> connections) {

        this(
            clusterElements, clusterRoot, connections, workflowTask.getDescription(), workflowTask.getFinalize(),
            workflowTask.getLabel(), workflowTask.getMaxRetries(), workflowTask.getMetadata(), workflowTask.getName(),
            workflowTask.getNode(), workflowTask.getParameters(), workflowTask.getPost(), workflowTask.getPre(),
            workflowTask.getTaskNumber(), workflowTask.getTimeout(), workflowTask.getType());
    }

    @SuppressFBWarnings("EI")
    public ClusterElementMap getClusterElements() {
        return clusterElements;
    }

    public boolean isClusterRoot() {
        return clusterRoot;
    }

    public List<ComponentConnection> getConnections() {
        return connections;
    }

    public String getDescription() {
        return description;
    }

    public List<WorkflowTask> getFinalize() {
        return finalize;
    }

    public String getLabel() {
        return label;
    }

    public Map<String, ?> getMetadata() {
        return metadata;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public String getName() {
        return name;
    }

    public String getNode() {
        return node;
    }

    public Map<String, ?> getParameters() {
        return parameters;
    }

    public List<WorkflowTask> getPost() {
        return post;
    }

    public List<WorkflowTask> getPre() {
        return pre;
    }

    public int getTaskNumber() {
        return taskNumber;
    }

    public String getTimeout() {
        return timeout;
    }

    public String getType() {
        return type;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }

        WorkflowTaskDTO that = (WorkflowTaskDTO) obj;

        return Objects.equals(this.clusterElements, that.clusterElements) && clusterRoot == that.clusterRoot &&
            Objects.equals(this.connections, that.connections) && Objects.equals(this.description, that.description) &&
            Objects.equals(this.finalize, that.finalize) && Objects.equals(this.label, that.label) &&
            this.maxRetries == that.maxRetries && Objects.equals(this.name, that.name) &&
            Objects.equals(this.metadata, that.metadata) && Objects.equals(this.node, that.node) &&
            Objects.equals(this.parameters, that.parameters) && Objects.equals(this.post, that.post) &&
            Objects.equals(this.pre, that.pre) && this.taskNumber == that.taskNumber &&
            Objects.equals(this.timeout, that.timeout) && Objects.equals(this.type, that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            clusterElements, clusterRoot, connections, description, finalize, label, maxRetries, metadata, name, node,
            parameters, post, pre, taskNumber, timeout, type);
    }

    @Override
    public String toString() {
        return "WorkflowTaskDTO[" +
            "connections=" + connections + ", " +
            "description=" + description + ", " +
            "finalize=" + finalize + ", " +
            "label=" + label + ", " +
            "maxRetries=" + maxRetries + ", " +
            "name=" + name + ", " +
            "node=" + node + ", " +
            "parameters=" + parameters + ", " +
            "clusterRoot=" + clusterRoot + ", " +
            "clusterElements=" + clusterElements + ", " +
            "post=" + post + ", " +
            "pre=" + pre + ", " +
            "taskNumber=" + taskNumber + ", " +
            "timeout=" + timeout + ", " +
            "type=" + type + ", " +
            "metadata=" + metadata + ']';
    }
}
