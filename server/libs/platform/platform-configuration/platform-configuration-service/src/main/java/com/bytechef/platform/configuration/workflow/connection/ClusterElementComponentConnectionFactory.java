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

package com.bytechef.platform.configuration.workflow.connection;

import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.domain.ClusterElements;
import com.bytechef.platform.configuration.domain.ClusterElements.ClusterElement;
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
public class ClusterElementComponentConnectionFactory
    implements ComponentConnectionFactory, ComponentConnectionFactoryResolver {

    private static final Logger log = LoggerFactory.getLogger(ClusterElementComponentConnectionFactory.class);

    private final ComponentDefinitionService componentDefinitionService;

    public ClusterElementComponentConnectionFactory(ComponentDefinitionService componentDefinitionService) {
        this.componentDefinitionService = componentDefinitionService;
    }

    @Override
    public List<ComponentConnection> create(
        String workflowNodeName, Map<String, ?> extensions, ComponentDefinition componentDefinition) {

        return getWorkflowConnections(ClusterElements.of(extensions), workflowNodeName);
    }

    @Override
    public Optional<ComponentConnectionFactory> resolve(ComponentDefinition componentDefinition) {
        return Optional.ofNullable(componentDefinition.isClusterRoot() ? this : null);
    }

    private List<ComponentConnection> getWorkflowConnections(
        ClusterElements clusterElements, String workflowNodeName) {

        Set<ComponentConnection> componentConnections = new HashSet<>();

        for (Map.Entry<String, List<ClusterElement>> clusterElementEntriesMap : clusterElements.entrySet()) {
            List<ClusterElement> clusterElementList = clusterElementEntriesMap.getValue();

            for (ClusterElement clusterElement : clusterElementList) {
                try {
                    ComponentConnection componentConnection = getWorkflowConnection(
                        workflowNodeName, clusterElement.getComponentName(),
                        clusterElement.getComponentVersion());

                    if (componentConnection != null) {
                        componentConnections.add(componentConnection);
                    }
                } catch (IllegalArgumentException e) {
                    if (log.isDebugEnabled()) {
                        log.debug(e.getMessage());
                    }
                }
            }
        }

        return new ArrayList<>(componentConnections);
    }

    private ComponentConnection getWorkflowConnection(
        String workflowNodeName, String componentName, int componentVersion) {

        ComponentConnection componentConnection = null;

        ComponentDefinition componentDefinition = componentDefinitionService.getComponentDefinition(
            componentName, componentVersion);

        if (componentDefinition.getConnection() != null) {
            componentConnection = ComponentConnection.of(
                workflowNodeName, componentName, componentDefinition.getName(), componentDefinition.getVersion(),
                componentDefinition.isConnectionRequired());
        }

        return componentConnection;
    }
}
