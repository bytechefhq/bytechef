
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

    private static final String CONNECTIONS = "connections";

    private final String componentName;
    private final int connectionVersion;
    private final String name;
    private final String title;

    private WorkflowConnection(String componentName, int connectionVersion, String name, String title) {
        this.componentName = componentName;
        this.connectionVersion = connectionVersion;
        this.name = name;
        this.title = title;
    }

    public static Map<String, WorkflowConnection> of(WorkflowTask workflowTask) {
        return toMap(workflowTask.getExtension(CONNECTIONS, new ParameterizedTypeReference<>() {}));
    }

    public static Optional<WorkflowConnection> of(WorkflowTrigger workflowTrigger) {
        return toMap(workflowTrigger.getExtension(CONNECTIONS, new ParameterizedTypeReference<>() {}))
            .values()
            .stream()
            .findFirst();
    }

    public static List<WorkflowConnection> of(Workflow workflow) {
        List<WorkflowConnection> workflowConnections = new ArrayList<>();

        WorkflowTrigger.of(workflow)
            .stream()
            .map(WorkflowConnection::of)
            .forEach(optional -> optional.ifPresent(workflowConnections::add));

        workflow.getTasks()
            .stream()
            .map(WorkflowConnection::of)
            .forEach(workflowConnectionMap -> workflowConnections.addAll(workflowConnectionMap.values()));

        return workflowConnections;
    }

    public String getComponentName() {
        return componentName;
    }

    public int getConnectionVersion() {
        return connectionVersion;
    }

    public String getName() {
        return name;
    }

    public String getTitle() {
        return title;
    }

    private static Map<String, WorkflowConnection> toMap(Map<String, Map<String, Object>> source) {
        return source.entrySet()
            .stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> new WorkflowConnection(
                    MapValueUtils.getRequiredString(entry.getValue(), "componentName"),
                    MapValueUtils.getRequiredInteger(entry.getValue(), "connectionVersion"),
                    entry.getKey(), MapValueUtils.getString(entry.getValue(), "title"))));
    }
}
