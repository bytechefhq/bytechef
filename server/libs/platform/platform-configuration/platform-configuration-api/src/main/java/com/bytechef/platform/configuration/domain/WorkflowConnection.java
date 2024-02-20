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

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.platform.component.registry.domain.ComponentDefinition;
import com.bytechef.platform.configuration.constant.WorkflowExtConstants;
import com.bytechef.platform.definition.WorkflowNodeType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @param workflowNodeName action task/trigger name used in the workflow
 *
 * @author Ivica Cardic
 */
public record WorkflowConnection(
    String componentName, int componentVersion, String workflowNodeName, String key, boolean required) {

    public static List<WorkflowConnection> of(
        Map<String, Map<String, Object>> connections, String componentName, int componentVersion,
        String workflowNodeName) {

        return connections
            .entrySet()
            .stream()
            .map(entry -> {
                Map<String, Object> connectionMap = entry.getValue();

                if ((!connectionMap.containsKey(WorkflowExtConstants.COMPONENT_NAME) ||
                    !connectionMap.containsKey(WorkflowExtConstants.COMPONENT_VERSION))) {

                    throw new IllegalStateException(
                        "%s and %s must be set".formatted(
                            WorkflowExtConstants.COMPONENT_NAME, WorkflowExtConstants.COMPONENT_VERSION));
                }

                return new WorkflowConnection(
                    MapUtils.getString(connectionMap, WorkflowExtConstants.COMPONENT_NAME, componentName),
                    MapUtils.getInteger(connectionMap, WorkflowExtConstants.COMPONENT_VERSION, componentVersion),
                    workflowNodeName, entry.getKey(),
                    MapUtils.getBoolean(connectionMap, WorkflowExtConstants.AUTHORIZATION_REQUIRED, false));
            })
            .toList();
    }

    public static WorkflowConnection of(String workflowNodeName, ComponentDefinition componentDefinition) {
        return new WorkflowConnection(
            componentDefinition.getName(), componentDefinition.getVersion(), workflowNodeName,
            componentDefinition.getName(), componentDefinition.isConnectionRequired());
    }

    public static List<WorkflowConnection> of(
        String workflowNodeName, DataStream.ComponentType componentType,
        List<ComponentDefinition> componentDefinitions) {

        List<WorkflowConnection> workflowConnections = new ArrayList<>();

        ComponentDefinition componentDefinition = CollectionUtils.getFirst(
            componentDefinitions,
            curComponentDefinition -> Objects.equals(
                curComponentDefinition.getName(), componentType.componentName()));

        if (componentDefinition.getConnection() != null) {
            workflowConnections.add(of(workflowNodeName, componentDefinition));
        }

        return workflowConnections;
    }

    public static WorkflowConnection of(
        String workflowNodeName, WorkflowNodeType workflowNodeType, ComponentDefinition componentDefinition) {

        return new WorkflowConnection(
            workflowNodeType.componentName(), workflowNodeType.componentVersion(), workflowNodeName,
            componentDefinition.getName(), componentDefinition.isConnectionRequired());
    }
}
