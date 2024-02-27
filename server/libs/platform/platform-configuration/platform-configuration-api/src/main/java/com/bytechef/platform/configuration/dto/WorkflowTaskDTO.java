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

package com.bytechef.platform.configuration.dto;

import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.platform.configuration.domain.DataStream;
import com.bytechef.platform.configuration.domain.DataStream.DataStreamComponent;
import com.bytechef.platform.configuration.domain.WorkflowConnection;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public final class WorkflowTaskDTO {

    private final List<WorkflowConnection> connections;
    private final DataStreamComponent destination;
    private final List<WorkflowTask> finalize;
    private final String label;
    private final int maxRetries;
    private final String name;
    private final String node;
    private final Map<String, ?> parameters;
    private final List<WorkflowTask> post;
    private final List<WorkflowTask> pre;
    private final DataStreamComponent source;
    private final int taskNumber;
    private final String timeout;
    private final String type;

    /**
     *
     */
    public WorkflowTaskDTO(
        List<WorkflowConnection> connections, DataStreamComponent destination, List<WorkflowTask> finalize,
        String label, int maxRetries, String name, String node, Map<String, ?> parameters, List<WorkflowTask> post,
        List<WorkflowTask> pre, DataStreamComponent source, int taskNumber, String timeout, String type) {

        this.connections = connections;
        this.destination = destination;
        this.finalize = finalize;
        this.label = label;
        this.maxRetries = maxRetries;
        this.name = name;
        this.node = node;
        this.parameters = parameters;
        this.post = post;
        this.pre = pre;
        this.source = source;
        this.taskNumber = taskNumber;
        this.timeout = timeout;
        this.type = type;
    }

    public WorkflowTaskDTO(WorkflowTask workflowTask, List<WorkflowConnection> connections, DataStream dataStream) {
        this(
            connections, dataStream == null ? null : dataStream.destination(), workflowTask.getFinalize(),
            workflowTask.getLabel(), workflowTask.getMaxRetries(), workflowTask.getName(), workflowTask.getNode(),
            workflowTask.getParameters(), workflowTask.getPost(), workflowTask.getPre(),
            dataStream == null ? null : dataStream.source(), workflowTask.getTaskNumber(), workflowTask.getTimeout(),
            workflowTask.getType());
    }

    public List<WorkflowConnection> getConnections() {
        return connections;
    }

    public DataStreamComponent getDestination() {
        return destination;
    }

    public List<WorkflowTask> getFinalize() {
        return finalize;
    }

    public String getLabel() {
        return label;
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

    public DataStreamComponent getSource() {
        return source;
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

        return Objects.equals(this.connections, that.connections) &&
            Objects.equals(this.destination, that.destination) &&
            Objects.equals(this.finalize, that.finalize) &&
            Objects.equals(this.label, that.label) &&
            this.maxRetries == that.maxRetries &&
            Objects.equals(this.name, that.name) &&
            Objects.equals(this.node, that.node) &&
            Objects.equals(this.parameters, that.parameters) &&
            Objects.equals(this.post, that.post) &&
            Objects.equals(this.pre, that.pre) &&
            Objects.equals(this.source, that.source) &&
            this.taskNumber == that.taskNumber &&
            Objects.equals(this.timeout, that.timeout) &&
            Objects.equals(this.type, that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            connections, destination, finalize, label, maxRetries, name, node, parameters, post, pre, source,
            taskNumber, timeout, type);
    }

    @Override
    public String toString() {
        return "WorkflowTaskDTO[" +
            "connections=" + connections + ", " +
            "destination=" + destination + ", " +
            "finalize=" + finalize + ", " +
            "label=" + label + ", " +
            "maxRetries=" + maxRetries + ", " +
            "name=" + name + ", " +
            "node=" + node + ", " +
            "parameters=" + parameters + ", " +
            "post=" + post + ", " +
            "pre=" + pre + ", " +
            "source=" + source + ", " +
            "taskNumber=" + taskNumber + ", " +
            "timeout=" + timeout + ", " +
            "type=" + type + ']';
    }
}
