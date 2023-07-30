
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

package com.bytechef.helios.configuration.connection;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.task.WorkflowTask;
import com.bytechef.commons.util.MapValueUtils;
import com.bytechef.hermes.configuration.trigger.WorkflowTrigger;
import org.springframework.core.convert.converter.Converter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
public class WorkflowConnection {

    public static final String CONNECTIONS = "connections";
    public static final String ID = "id";

    private final String componentName; // required if a component supports multiple connections
    private final Integer componentVersion; // required if a component supports multiple connections
    private final Long id;
    private final String key;
    private final String operationName; // task/trigger name used in the workflow

    private WorkflowConnection(
        String componentName, Integer componentVersion, String operationName, String key, Long id) {

        this.componentName = componentName;
        this.componentVersion = componentVersion;
        this.id = id;
        this.key = key;
        this.operationName = operationName;
    }

    @SuppressWarnings("unchecked")
    public static List<WorkflowConnection> of(WorkflowTask workflowTask) {
        return toList(workflowTask.getExtension(CONNECTIONS, Map.class, Map.of()), workflowTask.getName());
    }

    @SuppressWarnings("unchecked")
    public static List<WorkflowConnection> of(WorkflowTrigger workflowTrigger) {
        return toList(workflowTrigger.getExtension(CONNECTIONS, Map.class, Map.of()), workflowTrigger.getName());
    }

    public static List<WorkflowConnection> of(Workflow workflow) {
        List<WorkflowConnection> workflowConnections = new ArrayList<>();

        WorkflowTrigger.of(workflow)
            .stream()
            .map(WorkflowConnection::of)
            .forEach(workflowConnections::addAll);

        workflow.getTasks()
            .stream()
            .map(WorkflowConnection::of)
            .forEach(workflowConnections::addAll);

        return workflowConnections;
    }

    private static List<WorkflowConnection> toList(Map<String, Map<String, Object>> source, String operationName) {
        return source.entrySet()
            .stream()
            .map(entry -> new WorkflowConnection(
                MapValueUtils.getString(entry.getValue(), "componentName"),
                MapValueUtils.getInteger(entry.getValue(), "componentVersion"), operationName, entry.getKey(),
                MapValueUtils.getLong(entry.getValue(), ID)))
            .toList();
    }

    public Optional<String> getComponentName() {
        return Optional.ofNullable(componentName);
    }

    public Optional<Long> getId() {
        return Optional.ofNullable(id);
    }

    public Optional<Integer> getComponentVersion() {
        return Optional.ofNullable(componentVersion);
    }

    public String getKey() {
        return key;
    }

    public String getOperationName() {
        return operationName;
    }

    @Override
    public String toString() {
        return "WorkflowConnection{" +
            "componentName='" + componentName + '\'' +
            ", componentVersion=" + componentVersion +
            ", key='" + key + '\'' +
            ", operationName='" + operationName + '\'' +
            ", id=" + id +
            '}';
    }

    @SuppressWarnings("rawtypes")
    public static class WorkflowConnectionConverter implements Converter<Map, WorkflowConnection> {

        @Override
        @SuppressWarnings("unchecked")
        public WorkflowConnection convert(Map source) {
            return new WorkflowConnection(
                MapValueUtils.getString(source, "componentName"), MapValueUtils.getInteger(source, "componentVersion"),
                MapValueUtils.getString(source, "operationName"), MapValueUtils.getString(source, "key"),
                MapValueUtils.getLong(source, "id"));
        }
    }
}
