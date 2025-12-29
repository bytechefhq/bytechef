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

import com.bytechef.commons.util.MapUtils;
import com.bytechef.platform.configuration.constant.WorkflowExtConstants;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import tools.jackson.core.type.TypeReference;

/**
 * @param workflowNodeName action task/trigger name used in the workflow
 *
 * @author Ivica Cardic
 */
public record ComponentConnection(
    String componentName, int componentVersion, String workflowNodeName, String key, boolean required) {

    public static int count(Map<String, ?> extensions) {
        Map<String, Map<String, ?>> connections = MapUtils.getMap(
            extensions, WorkflowExtConstants.CONNECTIONS, new TypeReference<>() {}, Map.of());

        return connections.size();
    }

    public static ComponentConnection of(
        String workflowNodeName, String workflowConnectionKey, String componentName, int componentVersion,
        boolean connectionRequired) {

        return new ComponentConnection(
            componentName, componentVersion, workflowNodeName, workflowConnectionKey, connectionRequired);
    }

    public static ComponentConnection of(
        String workflowNodeName, String componentName, int componentVersion, boolean connectionRequired) {

        return new ComponentConnection(
            componentName, componentVersion, workflowNodeName, componentName, connectionRequired);
    }

    public static List<ComponentConnection> of(
        Map<String, ?> extensions, String workflowNodeName,
        BiFunction<String, Integer, Boolean> connectionRequiredFunction) {

        Map<String, Map<String, ?>> connections = MapUtils.getMap(
            extensions, WorkflowExtConstants.CONNECTIONS, new TypeReference<>() {}, Map.of());

        return connections.entrySet()
            .stream()
            .map(entry -> {
                Map<String, ?> connectionMap = entry.getValue();

                String name = MapUtils.getRequiredString(connectionMap, WorkflowExtConstants.COMPONENT_NAME);
                int version = MapUtils.getRequiredInteger(connectionMap, WorkflowExtConstants.COMPONENT_VERSION);

                boolean required =
                    MapUtils.getBoolean(connectionMap, WorkflowExtConstants.AUTHORIZATION_REQUIRED, false) ||
                        connectionRequiredFunction.apply(name, version);

                return new ComponentConnection(name, version, workflowNodeName, entry.getKey(), required);
            })
            .toList();
    }
}
