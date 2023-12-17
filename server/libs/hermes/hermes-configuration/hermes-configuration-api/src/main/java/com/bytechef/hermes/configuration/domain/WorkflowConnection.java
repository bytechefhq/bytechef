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

package com.bytechef.hermes.configuration.domain;

import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.hermes.component.registry.OperationType;
import com.bytechef.hermes.configuration.constant.MetadataConstants;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
public class WorkflowConnection {

    public static final String AUTHORIZATION_REQUIRED = "authorizationRequired";
    public static final String COMPONENT_NAME = "componentName";
    public static final String COMPONENT_VERSION = "componentVersion";
    public static final String ID = "id";

    private final String componentName;
    private final int componentVersion;
    private final Long id;
    private final String key;
    private final String operationName; // task/trigger name used in the workflow
    private final boolean required;

    private WorkflowConnection(
        String componentName, int componentVersion, String operationName, String key, Long id, boolean required) {

        this.componentName = componentName;
        this.componentVersion = componentVersion;
        this.id = id;
        this.key = key;
        this.operationName = operationName;
        this.required = required;
    }

    public static List<WorkflowConnection> of(WorkflowTask workflowTask, boolean connectionRequired) {
        List<WorkflowConnection> workflowConnections;
        OperationType operationType = OperationType.ofType(workflowTask.getType());

        if (MapUtils.containsKey(workflowTask.getMetadata(), MetadataConstants.CONNECTIONS)) {
            workflowConnections = toList(
                MapUtils.getMap(
                    workflowTask.getExtensions(), MetadataConstants.CONNECTIONS, new TypeReference<>() {},
                    Map.of()),
                operationType.componentName(), operationType.componentVersion(), workflowTask.getName(),
                connectionRequired);
        } else {
            workflowConnections = getWorkflowConnections(workflowTask.getName(), operationType, connectionRequired);
        }

        return workflowConnections;
    }

    public static List<WorkflowConnection> of(WorkflowTrigger workflowTrigger, boolean connectionRequired) {
        List<WorkflowConnection> workflowConnections;
        OperationType operationType = OperationType.ofType(workflowTrigger.getType());

        if (MapUtils.containsKey(workflowTrigger.getMetadata(), MetadataConstants.CONNECTIONS)) {
            workflowConnections = toList(
                MapUtils.getMap(
                    workflowTrigger.getMetadata(), MetadataConstants.CONNECTIONS, new TypeReference<>() {},
                    Map.of()),
                operationType.componentName(), operationType.componentVersion(), workflowTrigger.getName(),
                connectionRequired);
        } else {
            workflowConnections = getWorkflowConnections(workflowTrigger.getName(), operationType, connectionRequired);
        }

        return workflowConnections;
    }

    private static List<WorkflowConnection> getWorkflowConnections(
        String operationName, OperationType operationType, boolean connectionRequired) {

        return List.of(
            new WorkflowConnection(
                operationType.componentName(), operationType.componentVersion(), operationName,
                operationType.componentName(), null,
                connectionRequired));
    }

    private static List<WorkflowConnection> toList(
        Map<String, Map<String, Object>> connections, String componentName, int componentVersion,
        String operationName, boolean connectionRequired) {

        return CollectionUtils.map(
            connections.entrySet(),
            entry -> {
                Map<String, Object> connectionMap = entry.getValue();

                if (!connectionMap.containsKey(ID) &&
                    (!connectionMap.containsKey(COMPONENT_NAME) || !connectionMap.containsKey(COMPONENT_VERSION))) {

                    throw new IllegalStateException(
                        "%s and %s must be set".formatted(COMPONENT_NAME, COMPONENT_VERSION));
                }

                return new WorkflowConnection(
                    MapUtils.getString(connectionMap, COMPONENT_NAME, componentName),
                    MapUtils.getInteger(
                        connectionMap, COMPONENT_VERSION, componentVersion),
                    operationName, entry.getKey(),
                    MapUtils.getLong(connectionMap, ID),
                    MapUtils.getBoolean(connectionMap, AUTHORIZATION_REQUIRED, false) || connectionRequired);
            });
    }

    public String getComponentName() {
        return componentName;
    }

    public Optional<Long> getId() {
        return Optional.ofNullable(id);
    }

    public int getComponentVersion() {
        return componentVersion;
    }

    public String getKey() {
        return key;
    }

    public String getOperationName() {
        return operationName;
    }

    public boolean isRequired() {
        return required;
    }

    @Override
    public String toString() {
        return "WorkflowConnection{" +
            "componentName='" + componentName + '\'' +
            ", componentVersion=" + componentVersion +
            ", key='" + key + '\'' +
            ", operationName='" + operationName + '\'' +
            ", id=" + id +
            ", required=" + required +
            '}';
    }
}
