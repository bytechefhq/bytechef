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

package com.bytechef.platform.configuration.workflow.connection;

import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.domain.ClusterElement;
import com.bytechef.platform.configuration.domain.ClusterElementMap;
import com.bytechef.platform.configuration.domain.ComponentConnection;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
@Order(1)
class ClusterRootComponentConnectionFactory
    implements ComponentConnectionFactory, ComponentConnectionFactoryResolver {

    private static final Logger log = LoggerFactory.getLogger(ClusterRootComponentConnectionFactory.class);

    private final ComponentDefinitionService componentDefinitionService;

    public ClusterRootComponentConnectionFactory(ComponentDefinitionService componentDefinitionService) {
        this.componentDefinitionService = componentDefinitionService;
    }

    @Override
    public List<ComponentConnection> create(
        String workflowNodeName, Map<String, ?> extensions, ComponentDefinition componentDefinition) {

        List<ComponentConnection> componentConnections = new ArrayList<>();

        if (componentDefinition.getConnection() != null) {
            componentConnections.add(
                ComponentConnection.of(
                    workflowNodeName, componentDefinition.getName(), componentDefinition.getName(),
                    componentDefinition.getVersion(), componentDefinition.isConnectionRequired()));
        }

        componentConnections.addAll(getComponentConnections(ClusterElementMap.of(extensions), workflowNodeName));

        return componentConnections;
    }

    @Override
    public Optional<ComponentConnectionFactory> resolve(ComponentDefinition componentDefinition) {
        return Optional.ofNullable(componentDefinition.isClusterRoot() ? this : null);
    }

    private List<ComponentConnection> getComponentConnections(
        ClusterElementMap clusterElementMap, String workflowNodeName) {

        Set<ComponentConnection> componentConnections = new HashSet<>();

        for (Map.Entry<String, Object> entry : clusterElementMap.entrySet()) {
            List<ClusterElement> clusterElements = new ArrayList<>();

            if (entry.getValue() instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof ClusterElement clusterElement) {
                        clusterElements.add(clusterElement);
                    } else {
                        throw new IllegalArgumentException("Invalid cluster element entry");
                    }
                }
            } else {
                clusterElements.add((ClusterElement) entry.getValue());
            }

            for (ClusterElement clusterElement : clusterElements) {
                try {
                    ComponentConnection componentConnection = getComponentConnection(
                        workflowNodeName, clusterElement.getWorkflowNodeName(), clusterElement.getComponentName(),
                        clusterElement.getComponentVersion());

                    if (componentConnection != null) {
                        componentConnections.add(componentConnection);
                    }
                } catch (IllegalArgumentException e) {
                    if (log.isDebugEnabled()) {
                        log.debug(e.getMessage());
                    }
                }

                componentConnections.addAll(
                    getComponentConnections(ClusterElementMap.of(clusterElement.getExtensions()), workflowNodeName));
            }
        }

        return new ArrayList<>(componentConnections);
    }

    private ComponentConnection getComponentConnection(
        String workflowNodeName, String workflowConnectionKey, String componentName, int componentVersion) {

        ComponentConnection componentConnection = null;

        ComponentDefinition componentDefinition = componentDefinitionService.getComponentDefinition(
            componentName, componentVersion);

        if (componentDefinition.getConnection() != null) {
            componentConnection = ComponentConnection.of(
                workflowNodeName, workflowConnectionKey, componentDefinition.getName(),
                componentDefinition.getVersion(), componentDefinition.isConnectionRequired());
        }

        return componentConnection;
    }
}
