
/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.hermes.connection;

import com.bytechef.atlas.domain.Job;
import com.bytechef.atlas.domain.Workflow;
import com.bytechef.atlas.task.WorkflowTask;
import com.bytechef.commons.util.MapValueUtils;
import com.bytechef.hermes.trigger.WorkflowTrigger;
import org.springframework.core.ParameterizedTypeReference;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Ivica Cardic
 */
public class WorkflowConnection {

    public static final String CONNECTIONS = "connections";
    public static final String ID = "id";

    private final String componentName;
    private final Long connectionId;
    private final Integer componentVersion;
    private final String key;
    private final String name;
    private final String taskName;

    private WorkflowConnection(
        String componentName, Long connectionId, Integer componentVersion, String key, String name, String taskName) {

        this.componentName = componentName;
        this.connectionId = connectionId;
        this.componentVersion = componentVersion;
        this.key = key;
        this.name = name;
        this.taskName = taskName;
    }

    public static Map<String, Map<String, WorkflowConnection>> of(Job job) {
        return MapValueUtils.get(
            job.getMetadata(), CONNECTIONS,
            new ParameterizedTypeReference<Map<String, Map<String, Map<String, Object>>>>() {}, Map.of())
            .entrySet()
            .stream()
            .collect(Collectors.toMap(Map.Entry::getKey, entry -> toMap(entry.getValue(), entry.getKey())));
    }

    public static Map<String, WorkflowConnection> of(WorkflowTask workflowTask) {
        return toMap(
            workflowTask.getExtension(CONNECTIONS, new ParameterizedTypeReference<>() {}, Map.of()),
            workflowTask.getName());
    }

    public static Map<String, WorkflowConnection> of(WorkflowTrigger workflowTrigger) {
        return toMap(
            workflowTrigger.getExtension(CONNECTIONS, new ParameterizedTypeReference<>() {}, Map.of()),
            workflowTrigger.getName());
    }

    public static List<WorkflowConnection> of(Workflow workflow) {
        List<WorkflowConnection> workflowConnections = new ArrayList<>();

        WorkflowTrigger.of(workflow)
            .stream()
            .map(WorkflowConnection::of)
            .forEach(workflowConnectionMap -> workflowConnections.addAll(workflowConnectionMap.values()));

        workflow.getTasks()
            .stream()
            .map(WorkflowConnection::of)
            .forEach(workflowConnectionMap -> workflowConnections.addAll(workflowConnectionMap.values()));

        return workflowConnections;
    }

    public Optional<String> getComponentName() {
        return Optional.ofNullable(componentName);
    }

    public Optional<Long> getConnectionId() {
        return Optional.ofNullable(connectionId);
    }

    public Optional<Integer> getComponentVersion() {
        return Optional.ofNullable(componentVersion);
    }

    public String getKey() {
        return key;
    }

    public String getName() {
        return name;
    }

    public String getTaskName() {
        return taskName;
    }

    private static Map<String, WorkflowConnection> toMap(Map<String, Map<String, Object>> source, String taskName) {
        return source.entrySet()
            .stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> new WorkflowConnection(
                    MapValueUtils.getString(entry.getValue(), "componentName"),
                    MapValueUtils.getLong(entry.getValue(), ID),
                    MapValueUtils.getInteger(entry.getValue(), "componentVersion"),
                    entry.getKey(), MapValueUtils.getString(entry.getValue(), "name"), taskName)));
    }

    @Override
    public String toString() {
        return "WorkflowConnection{" +
            "componentName='" + componentName + '\'' +
            ", connectionId=" + connectionId +
            ", connectionVersion=" + componentVersion +
            ", key='" + key + '\'' +
            ", name='" + name + '\'' +
            ", taskName='" + taskName + '\'' +
            '}';
    }
}
