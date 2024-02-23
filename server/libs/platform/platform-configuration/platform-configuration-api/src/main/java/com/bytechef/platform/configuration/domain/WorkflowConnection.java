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

import com.bytechef.commons.util.MapUtils;
import com.bytechef.platform.component.registry.domain.ComponentDefinition;
import com.bytechef.platform.configuration.constant.WorkflowExtConstants;
import com.bytechef.platform.definition.WorkflowNodeType;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.List;
import java.util.Map;

/**
 * @param workflowNodeName action task/trigger name used in the workflow
 *
 * @author Ivica Cardic
 */
public record WorkflowConnection(
    String componentName, int componentVersion, String workflowNodeName, String key, boolean required) {

    public static List<WorkflowConnection> of(Map<String, ?> extensions, String workflowNodeName) {
        Map<String, Map<String, ?>> connections = MapUtils.getMap(
            extensions, WorkflowExtConstants.CONNECTIONS, new TypeReference<>() {}, Map.of());

        return connections
            .entrySet()
            .stream()
            .map(entry -> {
                Map<String, ?> connectionMap = entry.getValue();

                return new WorkflowConnection(
                    MapUtils.getRequiredString(connectionMap, WorkflowExtConstants.COMPONENT_NAME),
                    MapUtils.getRequiredInteger(connectionMap, WorkflowExtConstants.COMPONENT_VERSION),
                    workflowNodeName, entry.getKey(),
                    MapUtils.getBoolean(connectionMap, WorkflowExtConstants.AUTHORIZATION_REQUIRED, false));
            })
            .toList();
    }

    public static WorkflowConnection of(
        String workflowNodeName, String workflowConnectionKey, ComponentDefinition componentDefinition) {

        return new WorkflowConnection(
            componentDefinition.getName(), componentDefinition.getVersion(), workflowNodeName,
            workflowConnectionKey, componentDefinition.isConnectionRequired());
    }

    public static WorkflowConnection of(
        String workflowNodeName, WorkflowNodeType workflowNodeType, ComponentDefinition componentDefinition) {

        return new WorkflowConnection(
            workflowNodeType.componentName(), workflowNodeType.componentVersion(), workflowNodeName,
            componentDefinition.getName(), componentDefinition.isConnectionRequired());
    }
}
